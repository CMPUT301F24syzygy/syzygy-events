package com.syzygy.events.ui.admin;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInstance;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.Image;
import com.syzygy.events.databinding.FragAdminEventPageBinding;
import com.syzygy.events.ui.AdminActivity;

import java.util.Locale;

public class AdminEventFragment extends Fragment implements Database.UpdateListener {

    private FragAdminEventPageBinding binding;
    /**
     * The event that is being displayed
     */
    private Event event;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragAdminEventPageBinding.inflate(inflater, container, false);

        SyzygyApplication app = (SyzygyApplication) getActivity().getApplication();

        //Get the event from the selected id stored in the activity
        app.getDatabase().<Event>getInstance(Database.Collections.EVENTS, ((AdminActivity) getActivity()).getEventID(), (instance, success) -> {
            if (!success) {
                ((AdminActivity) getActivity()).navigateUp("The selected event was not found");
                return;
            }

            event = instance;
            event.addListener(this);
            //Set up fields
            binding.eventTitle.setText(event.getTitle());
            binding.eventPriceText.setText(String.format(Locale.getDefault(), "$%3.2f", event.getPrice()));
            String start = app.formatTimestamp(event.getStartDate());
            String start_end = String.format("%s - %s", start, app.formatTimestamp(event.getEndDate()));
            binding.eventStartEndText.setText(event.getEventDates() == Event.Dates.NO_REPEAT ? start : start_end);
            binding.eventWeekdaysTimeText.setText(event.getFormattedEventDates());
            binding.eventGeoRequiredText.setVisibility(event.getRequiresLocation() ? View.VISIBLE : View.GONE);
            binding.eventDescriptionText.setText(event.getDescription());
            binding.capacityInfoText.setText(String.format(Locale.getDefault(), "Capacity: %d", event.getCapacity()));
            if (event.getWaitlistCapacity() > 0) {
                binding.waitlistCapacityInfoText.setText(String.format(Locale.getDefault(), "Waitlist Limit: %d", event.getWaitlistCapacity()));
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
                ((AdminActivity) getActivity()).openFacility(event.getFacilityID());
            });

            binding.copyQrButton.setOnClickListener(view -> {
                getContext();
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("QR hash", event.getQrHash()));
            });

            binding.adminRemoveQrButton.setOnClickListener(view -> {
                Dialog confirmRemoveDialog = new AlertDialog.Builder(getContext())
                        .setTitle("Confirm")
                        .setMessage("Are you sure you want to remove the hash? This action cannot be undone.")
                        .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                event.setQrHash("");
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create();
                confirmRemoveDialog.show();
            });

            binding.adminRemoveEventButton.setOnClickListener(view -> {
                Dialog confirmRemoveDialog = new AlertDialog.Builder(getContext())
                        .setTitle("Confirm")
                        .setMessage("Are you sure you want to remove this event? This action cannot be undone.")
                        .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                event.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s -> {});
                                ((AdminActivity)getActivity()).navigateUp();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create();
                confirmRemoveDialog.show();
            });

            updateView();
        });

        return binding.getRoot();


    }

    @Override
    public void onDestroyView() {
        if (event != null) {
            event.dissolve(this);
        }
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
            binding.eventQrImg.setImageBitmap(bitmap);
            binding.copyQrButton.setVisibility(View.VISIBLE);
        } else {
            binding.copyQrButton.setVisibility(View.GONE);
            binding.adminRemoveQrButton.setVisibility(View.GONE);
        }


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

    @Override
    public <T extends DatabaseInstance<T>> void onUpdate(DatabaseInstance<T> instance, Type type) {
        if (!event.isLegalState()) {
            ((AdminActivity) getActivity()).navigateUp();
            return;
        }
        updateView();
    }



}