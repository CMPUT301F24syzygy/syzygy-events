package com.syzygy.events.ui.organizer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.Timestamp;
import com.squareup.picasso.Picasso;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.Image;
import com.syzygy.events.databinding.FragOrgCreateEventBinding;
import com.syzygy.events.ui.OrganizerActivity;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;

/**
 * The fragment that the user sees when they create an event. The add event tab
 * <p>
 * Map
 * <pre>
 * 1. Organizer Activity -> Add Event
 * </pre>
 */
public class OrganizerCreateEventFragment extends Fragment {
    private FragOrgCreateEventBinding binding;
    /**
     * The current selected poster image
     */
    private Uri image;
    /**
     * The created event.
     * We store this and dissolve after navigating so that we don't have to load it up again
     */
    private Event event;

    MaterialDatePicker<Long> datePicker;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragOrgCreateEventBinding.inflate(inflater, container, false);

        binding.eventCreateSubmitButton.setOnClickListener(view -> submitData());
        binding.eventCreateEditPosterButton.setOnClickListener(view -> choosePhoto());
        binding.eventCreateRemovePosterButton.setOnClickListener(view -> setImage(null));

        binding.eventCreateOptionChips.setOnCheckedStateChangeListener((group, checkedIds) -> onChangeOfRepeat());


        setImage(null);

        binding.eventCreateOpenDate.setOnClickListener(v -> {
            openDatePicker(binding.eventCreateOpenDate);
        });
        binding.eventCreateCloseDate.setOnClickListener(v -> {
            openDatePicker(binding.eventCreateCloseDate);
        });
        binding.eventCreateStartDate.setOnClickListener(v -> {
            openDatePicker(binding.eventCreateStartDate);
        });
        binding.eventCreateEndDate.setOnClickListener(v -> {
            openDatePicker(binding.eventCreateEndDate);
        });
        binding.eventCreateDate.setOnClickListener(v -> {
            openDatePicker(binding.eventCreateDate);
        });

        return binding.getRoot();
    }

    /**
     * Called when the user switches between single and sequence
     */
    private void onChangeOfRepeat() {
        int v = binding.createEventSequenceChip.isChecked() ? View.VISIBLE : View.GONE;
        binding.eventCreateEndDateLayout.setVisibility(v);
        binding.eventCreateStartDateLayout.setVisibility(v);
        binding.createEventWeekdayChips.setVisibility(v);
        binding.eventCreateDateLayout.setVisibility(v == View.GONE ? View.VISIBLE : View.GONE);
    }

    /**
     * Querries the user for an image
     */
    private void choosePhoto() {
        ((SyzygyApplication) getActivity().getApplication()).getImage(uri -> {
            if (uri != null) {
                setImage(uri);
            }
        });
    }

    /**
     * Displays the image as the poster. If null, removes the current image
     * @param uri The image
     */
    private void setImage(Uri uri) {
        image = uri;
        if (image == null) {
            binding.eventCreateEditPosterButton.setText(R.string.add_poster_button);
            Image.formatDefaultImage(Database.Collections.EVENTS, Image.Options.Square(Image.Options.Sizes.MEDIUM)).into(binding.eventCreatePosterImg);
            binding.eventCreateRemovePosterButton.setVisibility(View.GONE);
        } else {
            binding.eventCreateEditPosterButton.setText(R.string.change_poster_button);
            Image.formatImage(Picasso.get().load(uri), Image.Options.Square(Image.Options.Sizes.MEDIUM)).into(binding.eventCreatePosterImg);
            binding.eventCreateRemovePosterButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Validates the data. If valid, creates the event and navigates to the event profile
     */
    private void submitData() {

        String title = binding.eventCreateName.getText().toString();
        String facilityID = ((SyzygyApplication) getActivity().getApplication()).getUser().getFacilityID();
        Boolean requiresGeo = binding.eventCreateRequireLocationCheckbox.isChecked();
        String description = binding.eventCreateBio.getText().toString().replaceAll("\\s+", " ");

        Long capacity;
        Long waitlistCapacity;
        Double price;

        try {
            capacity = Long.parseLong(binding.eventCreateCapacity.getText().toString());
        } catch (NumberFormatException ex) {
            capacity = null;
        }

        String waitListTxt = binding.eventCreateWaitlistCap.getText().toString();
        if (waitListTxt.isBlank()) {
            waitlistCapacity = -1L;
        } else {
            try {
                waitlistCapacity = Long.parseLong(waitListTxt);
            } catch (NumberFormatException ex) {
                waitlistCapacity = null;
            }
        }

        try {
            price = Double.parseDouble(binding.eventCreatePrice.getText().toString());
        } catch (NumberFormatException ex) {
            price = null;
        }

        Timestamp openDate;
        Timestamp closeDate;
        Timestamp startDate;
        Timestamp endDate;

        boolean repeat = binding.createEventSequenceChip.isChecked();
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String startDay = repeat ? binding.eventCreateStartDate.getText().toString() : binding.eventCreateDate.getText().toString();

        String openDay = binding.eventCreateOpenDate.getText().toString();
        String closeDay = binding.eventCreateCloseDate.getText().toString();
        try {
            openDate = new Timestamp(Objects.requireNonNull(formatter.parse(openDay + " 00:03")));
            Timestamp testDate = new Timestamp(Objects.requireNonNull(formatter.parse(openDay + " 23:59")));
            if(Timestamp.now().compareTo(testDate) >= 0){
                openDate = null;
            }
        } catch (ParseException | NullPointerException | IllegalArgumentException ex) {
            openDate = null;
        }

        try {
            closeDate = new Timestamp(Objects.requireNonNull(formatter.parse(closeDay + " 00:02")));
            if(openDate != null && openDate.compareTo(closeDate) >= 0){
                closeDate = null;
            }
        } catch (ParseException | NullPointerException | IllegalArgumentException ex ) {
            closeDate = null;
        }

        try {
            startDate = new Timestamp(Objects.requireNonNull(formatter.parse(startDay + " 00:01")));
            if(closeDate != null){
                if(closeDate.compareTo(startDate) >= 0){
                    startDate = null;
                }
            }else if(openDate != null && openDate.compareTo(startDate) >= 0){
                startDate = null;
            }
        } catch (ParseException | NullPointerException | IllegalArgumentException ex) {
            startDate = null;
        }

        String endDay = repeat ? binding.eventCreateEndDate.getText().toString() : startDay;
        try {
            endDate = new Timestamp(Objects.requireNonNull(formatter.parse(endDay + " 23:59")));
            if(startDate != null) {
                if(startDate.compareTo(endDate) >= 0){
                    endDate = null;
                }
            }else if(closeDate != null){
                if(closeDate.compareTo(endDate) >= 0){
                    endDate = null;
                }
            }else if(openDate != null && openDate.compareTo(endDate) >= 0){
                endDate = null;
            }
        } catch (ParseException | NullPointerException | IllegalArgumentException ex) {
            endDate = null;
        }

        openDate = openDay.matches("\\d{1,2}/\\d{1,2}/\\d{4}") ? openDate : null;
        closeDate = closeDay.matches("\\d{1,2}/\\d{1,2}/\\d{4}") ? closeDate : null;
        startDate = startDay.matches("\\d{1,2}/\\d{1,2}/\\d{4}") ? startDate : null;
        endDate = endDay.matches("\\d{1,2}/\\d{1,2}/\\d{4}") ? endDate : null;

        Long dates;
        if (repeat) {
            dates = (binding.monChip.isChecked() ? Event.Dates.MONDAY : 0) |
                    (binding.tueChip.isChecked() ? Event.Dates.TUESDAY : 0) |
                    (binding.wedChip.isChecked() ? Event.Dates.WEDNESDAY : 0) |
                    (binding.thuChip.isChecked() ? Event.Dates.THURSDAY : 0) |
                    (binding.friChip.isChecked() ? Event.Dates.FRIDAY : 0) |
                    (binding.satChip.isChecked() ? Event.Dates.SATURDAY : 0) |
                    (binding.sunChip.isChecked() ? Event.Dates.SUNDAY : 0);
        } else {
            dates = Event.Dates.NO_REPEAT;
        }

        SyzygyApplication app = (SyzygyApplication) getActivity().getApplication();
        binding.progressBar.setVisibility(View.VISIBLE);
        Set<Integer> invalidIds = Event.NewInstance(app.getDatabase(), title, image, facilityID, requiresGeo, description, capacity, waitlistCapacity, price, openDate, closeDate, startDate, endDate, dates, (evnt, evnt_success) -> {
            binding.progressBar.setVisibility(View.GONE);
            if (evnt_success) {
                this.event = evnt;
                ((OrganizerActivity) getActivity()).openEvent(evnt.getDocumentID());
            } else {
                binding.progressBar.setVisibility(View.GONE);
            }
        });

        binding.eventCreateNameLayout.setError(null);
        binding.eventCreateCapacityLayout.setError(null);
        binding.eventCreateWaitlistCapLayout.setError(null);
        binding.eventCreatePriceLayout.setError(null);
        binding.eventCreateOpenDateLayout.setError(null);
        binding.eventCreateCloseDateLayout.setError(null);
        binding.eventCreateStartDateLayout.setError(null);
        binding.eventCreateEndDateLayout.setError(null);
        binding.eventCreateDateLayout.setError(null);

        if (invalidIds.isEmpty()) return;
        binding.progressBar.setVisibility(View.GONE);

        if (invalidIds.contains(R.string.database_event_title)) {
            binding.eventCreateNameLayout.setError("Required");
        }
        if (invalidIds.contains(R.string.database_event_capacity)) {
            binding.eventCreateCapacityLayout.setError("Must be greater than 0");
        }
        if (invalidIds.contains(R.string.database_event_waitlist)) {
            binding.eventCreateWaitlistCapLayout.setError("Invalid");
        }
        if (invalidIds.contains(R.string.database_event_price)) {
            binding.eventCreatePriceLayout.setError("Required");
        }
        if (invalidIds.contains(R.string.database_event_openDate)) {
            if (!openDay.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
                binding.eventCreateOpenDateLayout.setError("Invalid");
            }
            else binding.eventCreateOpenDateLayout.setError(getString(R.string.val_create_event_open));
        }
        if (invalidIds.contains(R.string.database_event_closedDate)) {
            if (!closeDay.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
                binding.eventCreateCloseDateLayout.setError("Invalid");
            }
            else binding.eventCreateCloseDateLayout.setError(getString(R.string.val_create_event_closed));
        }
        if (invalidIds.contains(R.string.database_event_start)) {
            if (repeat) {
                if (!startDay.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
                    binding.eventCreateStartDateLayout.setError("Invalid");
                }
                else binding.eventCreateStartDateLayout.setError(getString(R.string.val_create_event_start));
            } else {
                if (!startDay.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
                    binding.eventCreateDateLayout.setError("Invalid");
                }
                else binding.eventCreateDateLayout.setError(getString(R.string.val_create_event_date));
            }
        }
        if (invalidIds.contains(R.string.database_event_end) && repeat) {
            if (!endDay.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
                binding.eventCreateEndDateLayout.setError("Invalid");
            }
            else binding.eventCreateEndDateLayout.setError(getString(R.string.val_create_event_end));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (event != null) event.dissolve();
        binding = null;
    }

    private void openDatePicker(EditText txt) {
        if (datePicker!=null && datePicker.isAdded())
            return;

        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        int offsetFromUTC = TimeZone.getDefault().getOffset(new Date().getTime()) * -1;

        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
        builder.setTitleText("");
        builder.setTextInputFormat(format);

        try {
            builder.setSelection(format.parse(txt.getText().toString()).getTime());
        } catch (ParseException|NullPointerException e) {
            builder.setSelection((new Date()).getTime() - offsetFromUTC);
        }

        datePicker =  builder.build();

        datePicker.show(getActivity().getSupportFragmentManager(), "DATE_PICKER");
        datePicker.addOnPositiveButtonClickListener(w -> {
            if (datePicker.getSelection() != null) {
                txt.setText(format.format(new Date(datePicker.getSelection() + offsetFromUTC)));
            }
        });
    }


}