package com.syzygy.events.ui.entrant;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.GeoPoint;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInstance;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.EventAssociation;
import com.syzygy.events.database.Image;
import com.syzygy.events.databinding.FragEntrantEventPageBinding;
import com.syzygy.events.ui.EntrantActivity;

import java.util.Locale;
import java.util.Objects;

public class EntrantEventPageFragment extends Fragment implements Database.UpdateListener {

    private FragEntrantEventPageBinding binding;
    private Event event;
    private EventAssociation association;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragEntrantEventPageBinding.inflate(inflater, container, false);

        EntrantActivity activity = (EntrantActivity)getActivity();
        SyzygyApplication app = (SyzygyApplication)getActivity().getApplication();

        app.getDatabase().<Event>getInstance(Database.Collections.EVENTS, activity.getEventID(), (instance, success) -> {
            if (!success) {
                activity.eventError();
                return;
            }

            event = instance;
            event.addListener(this);

            binding.eventTitle.setText(event.getTitle());
            binding.eventPriceText.setText(String.format(Locale.getDefault(), "$ %3.2f", event.getPrice()));
            String start = app.formatTimestamp(event.getStartDate());
            String start_end = String.format ("%s - %s", start, app.formatTimestamp(event.getEndDate()));
            binding.eventStartEndText.setText(event.getEventDates() == Event.Dates.NO_REPEAT ? start : start_end);
            binding.eventWeekdaysTimeText.setText(event.getFormattedEventDates());
            binding.eventGeoRequiredText.setVisibility(event.getRequiresLocation() ? View.VISIBLE : View.GONE);
            binding.eventDescriptionText.setText(event.getDescription());
            binding.capacityInfoText.setText("Capacity: "+ event.getCapacity());
            if(event.getWaitlistCapacity() > 0){
                binding.waitlistCapacityInfoText.setText("Waitlist Limit: " + event.getWaitlistCapacity());
                binding.waitlistCapacityInfoText.setVisibility(View.VISIBLE);
            }

            TextView facility_name = binding.getRoot().findViewById(R.id.card_facility_name_text);
            facility_name.setText(event.getFacility().getName());
            TextView facility_address = binding.getRoot().findViewById(R.id.card_facility_location_text);
            facility_address.setText(event.getFacility().getAddress());
            ImageView facility_image = binding.getRoot().findViewById(R.id.card_facility_image_img);
            Image.getFormatedAssociatedImage(event.getFacility(), Image.Options.Square(Image.Options.Sizes.MEDIUM)).into(facility_image);

            binding.eventImg.setOnClickListener(view -> {
                app.displayImage(event);
            });

            View facility_card = binding.getRoot().findViewById(R.id.facility_card);
            facility_card.setOnClickListener(view -> {
                activity.openFacility();
            });

            binding.eventExitWaitlistButton.setOnClickListener(view -> {
                deleteAssociation();
            });

            binding.buttonReject.setOnClickListener(view -> {
                deleteAssociation();
            });

            binding.buttonAccept.setOnClickListener(view -> {
                event.acceptInvite(app.getUser(), (e, a, s) -> {});
            });

            binding.eventJoinWaitlistButton.setOnClickListener(view -> {
                if (event.getRequiresLocation()) {
                    Dialog requiresGeoWarning = new AlertDialog.Builder(getContext())
                            .setTitle("TODO")
                            .setNegativeButton("cancel", null)
                            .setPositiveButton("agree", (dialogInterface, i) -> {
                                app.getLocation((l) -> {
                                    if (l != null) {
                                        GeoPoint geo = new GeoPoint(l.getLatitude(), l.getLongitude());
                                        event.addUserToWaitlist(app.getUser(), geo, (e, a, s) -> {
                                            updateView();
                                        });
                                    } else {
                                        Dialog geoFailedDialog = new AlertDialog.Builder(getContext())
                                                .setMessage(R.string.failed_get_location).create();
                                        geoFailedDialog.show();
                                    }
                                });
                            })
                            .create();
                    requiresGeoWarning.show();
                } else {
                    event.addUserToWaitlist(app.getUser(), null, (e, a, s) -> {
                        updateView();
                    });
                }
            });

            updateView();
        });

        return binding.getRoot();


    }


    @Override
    public void onDestroyView() {
        if (association != null) {
            association.dissolve(this);
        }
        if (event != null) {
            event.dissolve(this);
        }
        super.onDestroyView();
        binding = null;
    }


    private void updateView() {

        Image.getFormatedAssociatedImage(event, Image.Options.Square(Image.Options.Sizes.MEDIUM)).into(binding.eventImg);
        SyzygyApplication app = (SyzygyApplication) getActivity().getApplication();

        binding.inWaitlistLayout.setVisibility(View.GONE);
        binding.inInvitedLayout.setVisibility(View.GONE);
        binding.inEnrolledLayout.setVisibility(View.GONE);
        binding.joinWaitlistLayout.setVisibility(View.GONE);
        binding.waitlistFullLayout.setVisibility(View.GONE);

        if (association == null) {
            event.getUserAssociation(app.getUser(), (e, a, success) -> {
                if (success && a.size()>0) {
                    association = a.result.get(0);
                    association.addListener(this);
                    updateView();
                }
            });
        }
        event.refreshData((e, success) -> {
            if (association != null && !Objects.equals(association.getStatus(), getString(R.string.event_assoc_status_cancelled))) {
                if (Objects.equals(association.getStatus(), getString(R.string.event_assoc_status_waitlist))) {
                    binding.inWaitlistLayout.setVisibility(View.VISIBLE);
                } else if (Objects.equals(association.getStatus(), getString(R.string.event_assoc_status_invited))) {
                    binding.inInvitedLayout.setVisibility(View.VISIBLE);
                } else if (Objects.equals(association.getStatus(), getString(R.string.event_assoc_status_enrolled))) {
                    binding.inEnrolledLayout.setVisibility(View.VISIBLE);
                }
            }
            else if (event.isRegistrationOpen()) {
                if (event.getWaitlistCapacity() < 0 || event.getCurrentWaitlist() < event.getWaitlistCapacity()) {
                    binding.joinWaitlistLayout.setVisibility(View.VISIBLE);
                } else {
                    binding.waitlistFullLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        if (event.isBeforeRegistration()) {
            binding.registrationDateInfoText.setText(
                    getString(R.string.before_reg_text, app.formatTimestamp(event.getOpenRegistrationDate())));
        } else if (event.isRegistrationOpen()) {
            binding.registrationDateInfoText.setText(
                    getString(R.string.reg_open_text, app.formatTimestamp(event.getCloseRegistrationDate())));
        } else {
            binding.registrationDateInfoText.setText(getString(R.string.after_reg_text));
        }
    }

    private void deleteAssociation(){
        association.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, success -> {
            if(!success){
                ((EntrantActivity)getActivity()).eventError();
            }
        });
    }

    @Override
    public <T extends DatabaseInstance<T>> void onUpdate(DatabaseInstance<T> instance, Type type) {
        if (!event.isLegalState()) {
            EntrantActivity activity = (EntrantActivity)getActivity();
            activity.eventError();
            return;
        }
        if(!association.isLegalState()){
            association = null;
        }
        updateView();
    }


}