package com.syzygy.events.ui.organizer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.chip.ChipGroup;
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

import java.util.List;
import java.util.Locale;

public class OrgEventSecondaryFragment extends Fragment implements Database.UpdateListener, OnMapReadyCallback {
    private SecondaryOrganizerEventBinding binding;
    private DatabaseInfLoadQuery<EventAssociation> queryAll;
    /*
    private DatabaseInfLoadQuery<EventAssociation> queryWaitlist, queryInvited, queryEnrolled, queryCancelled;
    */

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
            //binding.eventStartEndText.setText(event.getFormattedStartEnd());
            //binding.eventWeekdaysTimeText.setText(getString(R.string.weekdays_time, event.getFormattedEventDates(), event.getFormattedTime()));
            binding.eventGeoRequiredText.setVisibility(event.getRequiresLocation() ? View.VISIBLE : View.GONE);
            binding.eventDescriptionText.setText(event.getDescription());


            queryAll = new DatabaseInfLoadQuery<>(DatabaseQuery.getAttachedUsers(app.getDatabase(), event, null, false));
            AssociatedEntrantsAdapter allAdapter = new AssociatedEntrantsAdapter(getContext(), queryAll.getInstances());
            queryAll.refreshData((query1, s) -> {
                allAdapter.notifyDataSetChanged();
            });
            binding.eventAssociatedEntrantsList.setAdapter(allAdapter);

            /*
            queryWaitlist = new DatabaseInfLoadQuery<>(DatabaseQuery.getAttachedUsers(
                    app.getDatabase(), event, getString(R.string.event_assoc_status_waitlist), false));
            AssociatedEntrantsAdapter waitlistAdapter = new AssociatedEntrantsAdapter(this.getContext(), queryWaitlist.getInstances());
            queryWaitlist.refreshData((query1, s) -> {
                waitlistAdapter.notifyDataSetChanged();
            });
            queryInvited = new DatabaseInfLoadQuery<>(DatabaseQuery.getAttachedUsers(
                    app.getDatabase(), event, getString(R.string.event_assoc_status_invited), false));
            AssociatedEntrantsAdapter invitedAdapter = new AssociatedEntrantsAdapter(this.getContext(), queryInvited.getInstances());
            queryInvited.refreshData((query1, s) -> {
                invitedAdapter.notifyDataSetChanged();
            });
            queryEnrolled = new DatabaseInfLoadQuery<>(DatabaseQuery.getAttachedUsers(
                    app.getDatabase(), event, getString(R.string.event_assoc_status_enrolled), false));
            AssociatedEntrantsAdapter enrolledAdapter = new AssociatedEntrantsAdapter(this.getContext(), queryEnrolled.getInstances());
            queryEnrolled.refreshData((query1, s) -> {
                enrolledAdapter.notifyDataSetChanged();
            });
            queryCancelled = new DatabaseInfLoadQuery<>(DatabaseQuery.getAttachedUsers(
                    app.getDatabase(), event, getString(R.string.event_assoc_status_cancelled), false));
            AssociatedEntrantsAdapter cancelledAdapter = new AssociatedEntrantsAdapter(this.getContext(), queryCancelled.getInstances());
            queryCancelled.refreshData((query1, s) -> {
                cancelledAdapter.notifyDataSetChanged();
            });
            *////

            binding.eventImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    app.displayImage(event);
                }
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

            binding.entrantFilterChips.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
                @Override
                public void onCheckedChanged(@NonNull ChipGroup group, @NonNull List<Integer> checkedIds) {
                    if (marker != null) {
                        marker.setVisible(false);
                    }
                    if (checkedIds.get(0) == R.id.all_chip) {
                        binding.eventAssociatedEntrantsList.setAdapter(allAdapter);
                    } /*else if (checkedIds.get(0) == R.id.waitlist_chip) {
                        binding.eventAssociatedEntrantsList.setAdapter(waitlistAdapter);
                    } else if (checkedIds.get(0) == R.id.invited_chip) {
                        binding.eventAssociatedEntrantsList.setAdapter(invitedAdapter);
                    } else if (checkedIds.get(0) == R.id.enrolled_chip) {
                        binding.eventAssociatedEntrantsList.setAdapter(enrolledAdapter);
                    } else if (checkedIds.get(0) == R.id.cancelled_chip) {
                        binding.eventAssociatedEntrantsList.setAdapter(cancelledAdapter);
                    }*/
                }
            });

            binding.editPosterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ///edit poster
                }
            });

            binding.copyQrButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(getContext().CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("QR hash", event.getQrHash()));
                }
            });


            binding.downloadQrButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ///download qr
                }
            });

            binding.createNotificationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ///compose notification popup (dev)
                }
            });

            binding.openLotteryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Dialog dialog = new AlertDialog.Builder(getContext())
                            .setView(R.layout.popup_lottery)
                            .create();
                    dialog.show();
                    ///fix layout & set layout values
                    dialog.findViewById(R.id.lottery_run).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            event.getLottery(-1, (e, result, success) -> {
                                result.execute((ev, r, f) -> {
                                }, true);
                            });
                        }
                    });
                }
            });

            if (event.getRequiresLocation()) {
                binding.entrantLocationMap.setVisibility(View.VISIBLE);
                SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.entrant_location_map);
                mapFragment.getMapAsync(this);
            }

            binding.eventAssociatedEntrantsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (event.getRequiresLocation()) {
                        AssociatedEntrantsAdapter a = (AssociatedEntrantsAdapter) binding.eventAssociatedEntrantsList.getAdapter();
                        GeoPoint l = a.getItem(position).getLocation();
                        LatLng latLng = new LatLng(l.getLatitude(), l.getLongitude());
                        marker.setPosition(latLng);
                        marker.setVisible(true);
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    }
                }
            });
            ///ability to cancel individual entrants

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

        Image.getFormatedAssociatedImage(event, Image.Options.AsIs()).into(binding.eventImg);
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
            binding.downloadQrButton.setVisibility(View.VISIBLE);
        } else {
            binding.copyQrButton.setVisibility(View.GONE);
            binding.downloadQrButton.setVisibility(View.GONE);
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


}