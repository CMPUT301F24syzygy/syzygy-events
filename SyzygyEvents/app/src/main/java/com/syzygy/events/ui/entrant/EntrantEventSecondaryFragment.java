package com.syzygy.events.ui.entrant;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInstance;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.EventAssociation;
import com.syzygy.events.database.Image;
import com.syzygy.events.database.User;
import com.syzygy.events.databinding.SecondaryEntrantEventBinding;
import com.syzygy.events.ui.EntrantActivity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class EntrantEventSecondaryFragment extends Fragment {

    private SecondaryEntrantEventBinding binding;
    Event event;
    EventAssociation association;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = SecondaryEntrantEventBinding.inflate(inflater, container, false);

        EntrantActivity activity = (EntrantActivity)getActivity();
        SyzygyApplication app = (SyzygyApplication)getActivity().getApplication();
        app.getDatabase().<Event>getInstance(Database.Collections.EVENTS, activity.getEventID(), (instance, success) -> {
            if (!success) {
                Toast.makeText(getContext(), "event does not exist", Toast.LENGTH_SHORT).show();
                activity.navigateUp();
            }
            event = instance;
            event.addListener(new Database.UpdateListener() {
                @Override
                public <T extends DatabaseInstance<T>> void onUpdate(DatabaseInstance<T> instance, Type type) {
                    if (!event.isLegalState()) {
                        EntrantActivity activity = (EntrantActivity)getActivity();
                        Toast.makeText(getContext(), "event does not exist", Toast.LENGTH_SHORT).show();
                        activity.navigateUp();
                    }
                    Image.getFormatedAssociatedImage(event, Image.Options.AsIs()).into(binding.eventImg);
                }
            });

            Image.getFormatedAssociatedImage(event, Image.Options.AsIs()).into(binding.eventImg);
            binding.eventTitle.setText(event.getTitle());
            binding.eventPriceText.setText("$ " + event.getPrice().toString());
            binding.eventStartEndText.setText("");
            binding.eventWeekdaysTimeText.setText(event.getFormattedEventDates());
            if (event.getRequiresLocation()) {
                binding.eventGeoRequiredText.setVisibility(View.VISIBLE);
            }
            binding.eventDescriptionText.setText(event.getDescription());

            TextView facility_name = binding.getRoot().findViewById(R.id.card_facility_name);
            facility_name.setText(event.getFacility().getName());
            TextView facility_address = binding.getRoot().findViewById(R.id.card_facility_address);
            facility_address.setText(event.getFacility().getAddress());
            ImageView facility_image = binding.getRoot().findViewById(R.id.facility_image);
            Image.getFormatedAssociatedImage(event.getFacility(), Image.Options.AsIs()).into(facility_image);

            binding.eventImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SyzygyApplication app = (SyzygyApplication)getActivity().getApplication();
                    app.displayImage(event);
                }
            });
            binding.eventFacilityCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EntrantActivity activity = (EntrantActivity)getActivity();
                    activity.openFacility();
                }
            });

            updateAssociation();
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        if (association != null) {
            association.dissolve();
        }
        event.dissolve();
        super.onDestroyView();
        binding = null;
    }


    private void updateAssociation() {

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

                    association.addListener(new Database.UpdateListener() {
                        @Override
                        public <T extends DatabaseInstance<T>> void onUpdate(DatabaseInstance<T> instance, Type type) {
                            updateAssociation();
                        }
                    });

                }
            });
        }


        if (association != null && !Objects.equals(association.getStatus(), getString(R.string.event_assoc_status_cancelled))) {
            if (Objects.equals(association.getStatus(), getString(R.string.event_assoc_status_waitlist))) {
                binding.inWaitlistLayout.setVisibility(View.VISIBLE);
                binding.eventExitWaitlistButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        association.setStatus(R.string.event_assoc_status_cancelled);
                        updateAssociation();
                    }
                });
            }
            else if (Objects.equals(association.getStatus(), getString(R.string.event_assoc_status_invited))) {
                binding.inInvitedLayout.setVisibility(View.VISIBLE);
                binding.buttonReject.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        association.setStatus(R.string.event_assoc_status_cancelled);
                        updateAssociation();
                    }
                });
                binding.buttonAccept.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        event.acceptInvite(app.getUser(), (e, a, success) -> {
                            updateAssociation();
                        });
                    }
                });
            }
            else if (Objects.equals(association.getStatus(), getString(R.string.event_assoc_status_enrolled))) {
                binding.inEnrolledLayout.setVisibility(View.VISIBLE);
            }
        }

        else if (event.isRegistrationOpen()) {
            event.refreshData((e, success) -> {
                if (e.getWaitlistCapacity() < 0 || e.getCurrentWaitlist() < e.getWaitlistCapacity()) {
                    binding.joinWaitlistLayout.setVisibility(View.VISIBLE);
                    binding.eventJoinWaitlistButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (event.getRequiresLocation()) {
                                app.getLocation((l) -> {
                                    if (l == null) {
                                        Dialog dialog = new AlertDialog.Builder(getContext())
                                                .setMessage("Failed to get location.")
                                                .create();
                                        dialog.show();
                                    }
                                    else {
                                        GeoPoint geo = new GeoPoint(l.getLatitude(), l.getLongitude());
                                        event.addUserToWaitlist(app.getUser(), geo, (e, a, success) -> {
                                            updateAssociation();
                                        });
                                    }
                                });
                            }
                            else {
                                event.addUserToWaitlist(app.getUser(), null, (e, a, success) -> {
                                    updateAssociation();
                                });
                            }
                        }
                    });
                }
                else {
                    binding.waitlistFullLayout.setVisibility(View.VISIBLE);
                }
            });
        }

        DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        if (Timestamp.now().compareTo(event.getOpenRegistrationDate()) < 0) {
            binding.registrationDateInfoText.setText("Registration Opens " + df.format(event.getOpenRegistrationDate().toDate()));
        }
        else if (Timestamp.now().compareTo(event.getCloseRegistrationDate()) < 0) {
            binding.registrationDateInfoText.setText("Registration Open Until " + df.format(event.getCloseRegistrationDate().toDate()));
        }
        else {
            binding.registrationDateInfoText.setText("Registration Closed");
        }


    }

}