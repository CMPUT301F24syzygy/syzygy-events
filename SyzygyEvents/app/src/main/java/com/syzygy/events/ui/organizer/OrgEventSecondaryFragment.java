package com.syzygy.events.ui.organizer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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
import com.syzygy.events.databinding.SecondaryOrganizerEventBinding;
import com.syzygy.events.ui.EntrantActivity;
import com.syzygy.events.ui.OrganizerActivity;

import java.util.Locale;
import java.util.Objects;

public class OrgEventSecondaryFragment extends Fragment implements Database.UpdateListener, OnMapReadyCallback {
    private SecondaryOrganizerEventBinding binding;
    private DatabaseInfLoadQuery<EventAssociation> query;

    private Event event;
    private GoogleMap map;
    private Marker marker;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = SecondaryOrganizerEventBinding.inflate(inflater, container, false);

        OrganizerActivity activity = (OrganizerActivity) getActivity();
        SyzygyApplication app = (SyzygyApplication) getActivity().getApplication();

        app.getDatabase().<Event>getInstance(Database.Collections.EVENTS, activity.getEventID(), (instance, success) -> {
            if (!success) {
                activity.navigateUp();
                return;
            }

            event = instance;
            event.addListener(this);

            binding.eventTitle.setText(event.getTitle());
            binding.eventPriceText.setText(String.format(Locale.getDefault(), "$ %3.2f", event.getPrice()));
            String start = app.formatTimestamp(event.getStartDate());
            String start_end = String.format("%s - %s", start, app.formatTimestamp(event.getEndDate()));
            binding.eventStartEndText.setText(event.getEventDates() == Event.Dates.NO_REPEAT ? start : start_end);
            binding.eventWeekdaysTimeText.setText(event.getFormattedEventDates());
            binding.eventGeoRequiredText.setVisibility(event.getRequiresLocation() ? View.VISIBLE : View.GONE);
            binding.eventDescriptionText.setText(event.getDescription());

            query = new DatabaseInfLoadQuery<>(DatabaseQuery.getAttachedUsers(app.getDatabase(), event, null, false));
            AssociatedEntrantsAdapter adapter = new AssociatedEntrantsAdapter(getContext(), query.getInstances());
            query.refreshData((query1, s) -> {
                adapter.notifyDataSetChanged();
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
                AssociatedEntrantsAdapter a = new AssociatedEntrantsAdapter(getContext(), query.getInstances());
                query.refreshData((query1, s) -> {
                    a.notifyDataSetChanged();
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
                setLotteryPopupView(dialog);
                event.refreshData((e, s) -> {
                    setLotteryPopupView(dialog);
                });
                dialog.findViewById(R.id.lottery_run_button).setOnClickListener(v -> {
                    event.getLottery(-1, (e, result, s) -> {
                        result.execute((ev, r, f) -> {
                            dialog.dismiss();
                            updateView();
                        }, true);
                    });
                });
            });
            binding.cancelEntrantButton.setOnClickListener(view -> {
                entrantUnselected();
                AssociatedEntrantsAdapter a = (AssociatedEntrantsAdapter) binding.eventAssociatedEntrantsList.getAdapter();
                EventAssociation association = a.getItem(binding.eventAssociatedEntrantsList.getCheckedItemPosition());
                association.setStatus(R.string.event_assoc_status_cancelled);
                binding.eventAssociatedEntrantsList.clearChoices();
                query.refreshData((query1, s) -> {
                    a.notifyDataSetChanged();
                });
                binding.eventAssociatedEntrantsList.setAdapter(a);

            });

            if (event.getRequiresLocation()) {
                binding.entrantLocationMap.setVisibility(View.VISIBLE);
                SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.entrant_location_map);
                mapFragment.getMapAsync(this);
            }

            binding.eventAssociatedEntrantsList.setOnItemClickListener((parent, view, position, id) -> {
                entrantUnselected();
                AssociatedEntrantsAdapter a = (AssociatedEntrantsAdapter) binding.eventAssociatedEntrantsList.getAdapter();
                if (Objects.equals(a.getItem(position).getStatus(), getString(R.string.event_assoc_status_waitlist)) ||
                        Objects.equals(a.getItem(position).getStatus(), getString(R.string.event_assoc_status_invited))) {
                    binding.cancelEntrantButton.setVisibility(View.VISIBLE);
                }
                if (event.getRequiresLocation()) {
                    GeoPoint l = a.getItem(position).getLocation();
                    LatLng latLng = new LatLng(l.getLatitude(), l.getLongitude());
                    marker.setPosition(latLng);
                    marker.setVisible(true);
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                }
            });

            updateView();
        });

        return binding.getRoot();
    }


    @Override
    public void onDestroyView() {
        event.dissolve(this);
        super.onDestroyView();
        binding = null;
    }


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

    private void posterUpdatedSuccess(Boolean success) {
        updateView();
    }

    private void entrantUnselected() {
        if (marker != null) {
            marker.setVisible(false);
        }
        binding.cancelEntrantButton.setVisibility(View.GONE);
    }

    private void setLotteryPopupView(Dialog dialog) {

        TextView message_enrolled_full = dialog.findViewById(R.id.lottery_full_enrolled_message);
        TextView message_waitlist_empty = dialog.findViewById(R.id.lottery_empty_waitlist_message);
        Button button = dialog.findViewById(R.id.lottery_run_button);

        message_enrolled_full.setVisibility(View.GONE);
        message_waitlist_empty.setVisibility(View.GONE);
        button.setVisibility(View.GONE);

        TextView enrolled = dialog.findViewById(R.id.lottery_enrolled_text);
        enrolled.setText(getString(R.string.lottery_enrolled_count, event.getCurrentEnrolled()));

        TextView invited = dialog.findViewById(R.id.lottery_invited_text);
        //invited.setText(getString(R.string.lottery_invited_count, event.getCurrentInvited()));

        TextView waitlist = dialog.findViewById(R.id.lottery_waitlist_text);
        waitlist.setText(getString(R.string.lottery_waitlist_count, event.getCurrentWaitlist()));

        TextView capacity = dialog.findViewById(R.id.lottery_capacity_text);
        capacity.setText(getString(R.string.lottery_capacity, event.getCapacity()));

        TextView open = dialog.findViewById(R.id.lottery_open_text);
        //int n = event.getCapacity() - event.getCurrentEnrolled() - event.getCurrentInvited();
        //open.setText((getString(R.string.lottery_waitlist_count, n)));

        if (event.getCurrentEnrolled() == event.getCapacity()) {
            message_enrolled_full.setVisibility(View.VISIBLE);
        } else if (event.getCurrentWaitlist() == 0) {
            message_waitlist_empty.setVisibility(View.VISIBLE);
        } else {
            button.setVisibility(View.VISIBLE);
        }
    }


}