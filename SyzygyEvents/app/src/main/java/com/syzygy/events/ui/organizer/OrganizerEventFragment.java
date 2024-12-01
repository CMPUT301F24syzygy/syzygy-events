package com.syzygy.events.ui.organizer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.chip.Chip;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.GeoPoint;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInfLoadQuery;
import com.syzygy.events.database.DatabaseInstance;
import com.syzygy.events.database.DatabaseQuery;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.EventAssociation;
import com.syzygy.events.database.Image;
import com.syzygy.events.databinding.FragOrgEventPageBinding;
import com.syzygy.events.ui.EntrantActivity;
import com.syzygy.events.ui.OrganizerActivity;

import java.util.Locale;
import java.util.Objects;

/**
 * The fragment that the user sees when they open an event's profile in the organizer view.
 * Also allows user to edit the poster and action the lottery.
 * <p>
 * Map
 * <pre>
 * 1. Organizer Activity -> Events -> [Event]
 * 2. Organizer Activity -> Add Event -> [Notification] -> [Event]
 * </pre>
 */
public class OrganizerEventFragment extends Fragment implements Database.UpdateListener, OnMapReadyCallback {
    private FragOrgEventPageBinding binding;
    /**
     * The query to get all users associated with the event
     */
    private DatabaseInfLoadQuery<EventAssociation> query;
    /**
     * The event to display
     */
    private Event event;
    /**
     * The map for the location of associated users
     */
    private GoogleMap map;
    /**
     * The marker of the location of the current selected user
     */
    private Marker marker;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragOrgEventPageBinding.inflate(inflater, container, false);

        OrganizerActivity activity = (OrganizerActivity) getActivity();
        SyzygyApplication app = (SyzygyApplication) getActivity().getApplication();

        app.getDatabase().<Event>getInstance(Database.Collections.EVENTS, activity.getEventID(), (instance, success) -> {
            if (!success) {
                activity.navigateUp("The event was not found");
                return;
            }
            //Set up fields
            event = instance;
            event.addListener(this);

            binding.eventTitle.setText(event.getTitle());
            binding.eventPriceText.setText(String.format(Locale.getDefault(), "$%3.2f", event.getPrice()));
            String start = app.formatTimestamp(event.getStartDate());
            String start_end = String.format("%s - %s", start, app.formatTimestamp(event.getEndDate()));
            binding.eventStartEndText.setText(event.getEventDates() == Event.Dates.NO_REPEAT ? start : start_end);
            binding.eventWeekdaysTimeText.setText(event.getFormattedEventDates());
            binding.eventGeoRequiredText.setVisibility(event.getRequiresLocation() ? View.VISIBLE : View.GONE);
            binding.eventDescriptionText.setText(event.getDescription());

            if (event.getRequiresLocation()) {
                binding.entrantLocationMap.setVisibility(View.VISIBLE);
                SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.entrant_location_map);
                mapFragment.getMapAsync(this);
            } else {
                ViewGroup.LayoutParams params = binding.eventAssociatedEntrantsList.getLayoutParams();
                params.height = (int)(params.height*1.3);
                binding.eventAssociatedEntrantsList.setLayoutParams(params);
                binding.div7.setVisibility(View.GONE);
            }

            query = new DatabaseInfLoadQuery<>(DatabaseQuery.getAttachedUsers(app.getDatabase(), event, null, false));
            OrganizerAssociatedEntrantsAdapter adapter = new OrganizerAssociatedEntrantsAdapter(getContext(), query.getInstances());
            query.refreshData((query1, s) -> {
                adapter.notifyDataSetChanged();
                binding.composeNotificationButton.setVisibility(binding.eventAssociatedEntrantsList.getCount()<1 ? View.GONE : View.VISIBLE);
            });
            binding.eventAssociatedEntrantsList.setAdapter(adapter);

            binding.eventImg.setOnClickListener(view -> {
                app.displayImage(event);
            });

            binding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    if (tab.getPosition() == 0) {
                        binding.actionsTabLayout.setVisibility(View.VISIBLE);
                        binding.entrantsTabLayout.setVisibility(View.GONE);
                    } else {
                        binding.actionsTabLayout.setVisibility(View.GONE);
                        binding.entrantsTabLayout.setVisibility(View.VISIBLE);
                    }
                    updateView();
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }
            });

            binding.entrantFilterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
                entrantUnselected();
                String status = null;
                if (checkedIds.get(0) != R.id.all_chip) {
                    Chip chip = binding.getRoot().findViewById(checkedIds.get(0));
                    status = chip.getText().toString();
                }
                query = new DatabaseInfLoadQuery<>(DatabaseQuery.getAttachedUsers(app.getDatabase(), event, status, false));
                OrganizerAssociatedEntrantsAdapter a = new OrganizerAssociatedEntrantsAdapter(getContext(), query.getInstances());
                query.refreshData((query1, s) -> {
                    a.notifyDataSetChanged();
                    binding.composeNotificationButton.setVisibility(binding.eventAssociatedEntrantsList.getCount()<1 ? View.GONE : View.VISIBLE);
                });
                binding.eventAssociatedEntrantsList.setAdapter(a);
            });

            binding.editPosterButton.setOnClickListener(view -> {
                ((SyzygyApplication) getActivity().getApplication()).getImage(uri -> {
                    if (uri == null) {
                        return;
                    }
                    event.setPoster(uri, this::posterUpdatedSuccess);
                });
            });

            binding.copyQrButton.setOnClickListener(view -> {
                getContext();
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("QR hash", event.getQrHash()));
            });

            binding.openLotteryButton.setOnClickListener(view -> {
                Dialog dialog = new AlertDialog.Builder(getContext())
                        .setView(R.layout.popup_lottery)
                        .create();
                dialog.show();
                event.refreshData((e, s) -> {
                    setLotteryPopupView(dialog);
                });
                dialog.findViewById(R.id.lottery_run_button).setOnClickListener(v -> {
                    event.getLottery(-1, (e, result, s) -> {
                        result.execute(
                                (ev, r, f) -> {
                                    dialog.dismiss();
                                    updateView();
                                },
                                !event.hasRunLottery()
                        );
                    });
                });
            });
            binding.cancelEntrantButton.setOnClickListener(view -> {
                entrantUnselected();
                OrganizerAssociatedEntrantsAdapter a = (OrganizerAssociatedEntrantsAdapter) binding.eventAssociatedEntrantsList.getAdapter();
                EventAssociation association = a.getItem(binding.eventAssociatedEntrantsList.getCheckedItemPosition());
                association.setStatus(R.string.event_assoc_status_cancelled);
                binding.eventAssociatedEntrantsList.clearChoices();
                query.refreshData((query1, s) -> {
                    a.notifyDataSetChanged();
                    binding.composeNotificationButton.setVisibility(binding.eventAssociatedEntrantsList.getCount()<1 ? View.GONE : View.VISIBLE);
                });
                binding.eventAssociatedEntrantsList.setAdapter(a);

            });

            binding.eventAssociatedEntrantsList.setOnItemClickListener((parent, view, position, id) -> {
                entrantUnselected();
                OrganizerAssociatedEntrantsAdapter a = (OrganizerAssociatedEntrantsAdapter) binding.eventAssociatedEntrantsList.getAdapter();
                if (Objects.equals(a.getItem(position).getStatus(), getString(R.string.event_assoc_status_waitlist)) ||
                        Objects.equals(a.getItem(position).getStatus(), getString(R.string.event_assoc_status_invited))) {
                    binding.cancelEntrantButton.setVisibility(View.VISIBLE);
                }
                if (event.getRequiresLocation()) {
                    GeoPoint l = a.getItem(position).getLocation();
                    LatLng latLng = new LatLng(l.getLatitude(), l.getLongitude());
                    marker.setPosition(latLng);
                    marker.setVisible(true);
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 4));
                }
            });

            binding.composeNotificationButton.setOnClickListener(view -> {
                Chip chip = binding.getRoot().findViewById(binding.entrantFilterChips.getCheckedChipId());
                String selected = chip.getText().toString();
                Dialog dialog = new AlertDialog.Builder(getContext())
                        .setView(R.layout.popup_create_notification)
                        .setTitle("New Notification")
                        .setMessage("\nTo: " + selected.toUpperCase() + " Entrants")
                        .create();
                dialog.show();
                dialog.findViewById(R.id.new_notification_send_button).setOnClickListener(v -> {
                    EditText edit_subject =  dialog.findViewById(R.id.new_notification_subject);
                    String subject = edit_subject.getText().toString();
                    EditText edit_body =  dialog.findViewById(R.id.new_notification_body);
                    TextInputLayout edit_body_layout =  dialog.findViewById(R.id.new_notification_body_layout);
                    TextInputLayout edit_subject_layout =  dialog.findViewById(R.id.new_notification_subject_layout);
                    edit_body_layout.setError(null);
                    edit_subject_layout.setError(null);
                    String body = edit_body.getText().toString();
                    if (subject.isEmpty()) {
                        edit_subject_layout.setError("Required.");
                    } if (body.isEmpty()) {
                        edit_body_layout.setError("Required.");
                    } else if (body.split("\\n").length>16) {
                        edit_body_layout.setError("This message is too long.");
                    } else if (!subject.isEmpty()) {
                        dialog.dismiss();
                        query.refreshData((query1, s) -> {
                            adapter.notifyDataSetChanged();
                            Database db = ((SyzygyApplication)getActivity().getApplication()).getDatabase();
                            new EventAssociation.Methods<Event>(db, event, query.getInstances())
                                    .notify(subject, body, true, true, false, (q, data, t) -> {});
                            Toast.makeText(getContext(), "Notification Sent", Toast.LENGTH_SHORT).show();
                            binding.composeNotificationButton.setVisibility(binding.eventAssociatedEntrantsList.getCount()<1 ? View.GONE : View.VISIBLE);
                        });
                    }
                });
            });

            updateView();
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        event.dissolve(this);
        query.dissolve();
        super.onDestroyView();
        binding = null;
    }

    /**
     * Sets up fields that could changed. Triggered whenever the event is updated
     */
    private void updateView() {

        Image.getFormatedAssociatedImage(event, Image.Options.Square(Image.Options.Sizes.MEDIUM)).into(binding.eventImg);
        SyzygyApplication app = (SyzygyApplication) getActivity().getApplication();

        if (!event.getQrHash().isEmpty()) {
            BitMatrix m;
            try {
                m = new MultiFormatWriter().encode(event.getQrHash(), BarcodeFormat.QR_CODE, 100, 100);
            } catch (WriterException e) {
                throw new RuntimeException(e);
            }
            Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565);
            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 100; j++) {
                    bitmap.setPixel(i, j, m.get(i, j) ? Color.BLACK : Color.WHITE);
                }
            }
            binding.facilityEventQrImg.setImageBitmap(bitmap);
            binding.copyQrButton.setVisibility(View.VISIBLE);
        } else {
            binding.copyQrButton.setVisibility(View.GONE);
        }

        if (event.isBeforeRegistration()) {
            binding.registrationDateInfoText.setText(
                    getString(R.string.before_reg_text, app.formatTimestamp(event.getOpenRegistrationDate())));
        } else if (event.isRegistrationOpen()) {
            binding.registrationDateInfoText.setText(
                    getString(R.string.reg_open_text, app.formatTimestamp(event.getCloseRegistrationDate())));
        } else {
            binding.registrationDateInfoText.setText(getString(R.string.after_reg_text));
            binding.openLotteryButton.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public <T extends DatabaseInstance<T>> void onUpdate(DatabaseInstance<T> instance, Type type) {
        if (!event.isLegalState()) {
            EntrantActivity activity = (EntrantActivity) getActivity();
            activity.navigateUp();
            return;
        }
        updateView();
    }


    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        marker = map.addMarker(new MarkerOptions()
                .draggable(false)
                .position(event.getFacility().getLatLngLocation()));
        this.map = map;
        marker.setVisible(false);
    }

    /**
     * Called when the poster is updated
     */
    private void posterUpdatedSuccess(Boolean success) {
        updateView();
    }

    /**
     * Called when an associated user is unselected
     */
    private void entrantUnselected() {
        if (marker != null) {
            marker.setVisible(false);
        }
        binding.cancelEntrantButton.setVisibility(View.GONE);
    }

    /**
     * Called when the lottery button is clicked. Displays a popup showing the current amount of
     * capacity/spots filled
     * @param dialog
     */
    private void setLotteryPopupView(Dialog dialog) {

        TextView message = dialog.findViewById(R.id.lottery_message);
        Button button = dialog.findViewById(R.id.lottery_run_button);
        TextView button_info = dialog.findViewById(R.id.lottery_info_text);
        message.setVisibility(View.GONE);
        button.setVisibility(View.GONE);
        button_info.setVisibility(View.GONE);

        int enrolled_c = event.getCurrentEnrolled();
        int invited_c = event.getCurrentInvited();
        int waitlist_c = event.getCurrentWaitlist();

        TextView cap = dialog.findViewById(R.id.org_event_cap_txt);
        cap.setText("Capacity : " + event.getCapacity());

        TextView inv = dialog.findViewById(R.id.org_event_inv_txt);
        inv.setText("- Invited : " + invited_c);

        TextView enr = dialog.findViewById(R.id.org_event_enr_txt);
        enr.setText("- Enrolled : " + enrolled_c);

        TextView waitlist = dialog.findViewById(R.id.lottery_waitlist_text);
        waitlist.setText(getString(R.string.lottery_waitlist_count, waitlist_c));

        TextView open = dialog.findViewById(R.id.lottery_open_text);
        open.setText((getString(R.string.lottery_open_count, event.getCapacity() - enrolled_c - invited_c)));

        String m;
        if (enrolled_c == event.getCapacity()) {
            message.setVisibility(View.VISIBLE);
            m = "Event is at capacity!";
            message.setText(m);
        } else if (enrolled_c + invited_c == event.getCapacity()) {
            message.setVisibility(View.VISIBLE);
            m = "There are no open spots.";
            message.setText(m);
        } else if (waitlist_c == 0) {
            message.setVisibility(View.VISIBLE);
            m = "The waitlist is empty. Remaining spots cannot be filled.";
            message.setText(m);
        } else {
            button.setVisibility(View.VISIBLE);
            button.setText("Fill " + (event.getCapacity() - enrolled_c - invited_c) + " Spots");
            button_info.setVisibility(View.VISIBLE);
        }
    }


}