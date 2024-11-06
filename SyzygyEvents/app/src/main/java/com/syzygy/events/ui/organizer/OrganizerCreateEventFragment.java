package com.syzygy.events.ui.organizer;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

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
import java.util.Date;
import java.util.Objects;
import java.util.Set;

public class OrganizerCreateEventFragment extends Fragment {
    private FragOrgCreateEventBinding binding;
    private Uri image;
    private Event event;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragOrgCreateEventBinding.inflate(inflater, container, false);

        binding.eventCreateSubmitButton.setOnClickListener(view -> submitData());
        binding.eventCreateEditPosterButton.setOnClickListener(view -> choosePhoto());
        binding.eventCreateRemovePosterButton.setOnClickListener(view -> setImage(null));


        binding.eventCreateOptionChips.setOnCheckedStateChangeListener((group, checkedIds) -> onChangeOfRepeat());

        setImage(null);

        return binding.getRoot();
    }

    private void onChangeOfRepeat() {
        int v = binding.createEventSequenceChip.isChecked() ? View.VISIBLE : View.GONE;
        binding.eventCreateEndDateLayout.setVisibility(v);
        binding.eventCreateStartDateLayout.setVisibility(v);
        binding.createEventWeekdayChips.setVisibility(v);
        binding.eventCreateDateLayout.setVisibility(v == View.GONE ? View.VISIBLE : View.GONE);
    }

    private void choosePhoto() {
        ((SyzygyApplication) getActivity().getApplication()).getImage(uri -> {
            if (uri == null) {
                Toast.makeText(getActivity(), "Failed to get image", Toast.LENGTH_LONG).show();
                return;
            }
            setImage(uri);
        });
    }

    private void setImage(Uri uri) {
        image = uri;
        if (image == null) {
            binding.eventCreateEditPosterButton.setText(R.string.add_poster_button);
            Image.formatDefaultImage(Database.Collections.EVENTS, Image.Options.Square(Image.Options.Sizes.MEDIUM)).into(binding.eventCreatePosterImg);
            binding.eventCreateRemovePosterButton.setVisibility(View.INVISIBLE);
        } else {
            binding.eventCreateEditPosterButton.setText(R.string.change_poster_button);
            Image.formatImage(Picasso.get().load(uri), Image.Options.Square(Image.Options.Sizes.MEDIUM)).into(binding.eventCreatePosterImg);
            binding.eventCreateRemovePosterButton.setVisibility(View.VISIBLE);
        }
    }

    private void submitData() {

        String title = binding.eventCreateName.getText().toString();
        String facilityID = ((SyzygyApplication) getActivity().getApplication()).getUser().getFacilityID();
        Boolean requiresGeo = binding.eventCreateRequireLocationCheckbox.isChecked();
        String description = binding.eventCreateBio.getText().toString();

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
            openDate = new Timestamp(Objects.requireNonNull(formatter.parse(openDay + " 12:01")));
            Timestamp testDate = new Timestamp(Objects.requireNonNull(formatter.parse(openDay + " 23:59")));
            if(Timestamp.now().compareTo(testDate) >= 0){
                openDate = null;
            }
        } catch (ParseException | NullPointerException ex) {
            openDate = null;
        }

        try {
            closeDate = new Timestamp(Objects.requireNonNull(formatter.parse(closeDay + " 12:01")));
            if(openDate != null && openDate.compareTo(closeDate) >= 0){
                closeDate = null;
            }
        } catch (ParseException | NullPointerException ex) {
            closeDate = null;
        }

        try {
            startDate = new Timestamp(Objects.requireNonNull(formatter.parse(startDay + " 12:01")));
            if(closeDate != null){
                if(closeDate.compareTo(startDate) >= 0){
                    startDate = null;
                }
            }else if(openDate != null && openDate.compareTo(startDate) >= 0){
                startDate = null;
            }
        } catch (ParseException | NullPointerException ex) {
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
        } catch (ParseException | NullPointerException ex) {
            endDate = null;
        }

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
                Toast.makeText(getActivity(), "An error occurred", Toast.LENGTH_LONG).show();
                binding.progressBar.setVisibility(View.GONE);
            }
        });

        if (invalidIds.isEmpty()) return;
        binding.progressBar.setVisibility(View.GONE);

        if (invalidIds.contains(R.string.database_event_title)) {
            binding.eventCreateName.setError(getString(R.string.val_create_event_title));
        }
        if (invalidIds.contains(R.string.database_event_description)) {
            binding.eventCreateBio.setError(getString(R.string.val_create_event_description));
        }
        if (invalidIds.contains(R.string.database_event_capacity)) {
            binding.eventCreateCapacity.setError(getString(R.string.val_create_event_capacity));
        }
        if (invalidIds.contains(R.string.database_event_waitlist)) {
            binding.eventCreateWaitlistCap.setError(getString(R.string.val_create_event_waitlist_cap));
        }
        if (invalidIds.contains(R.string.database_event_price)) {
            binding.eventCreatePrice.setError(getString(R.string.val_create_event_price));
        }
        if (invalidIds.contains(R.string.database_event_openDate)) {
            binding.eventCreateOpenDate.setError(getString(R.string.val_create_event_open));
        }
        if (invalidIds.contains(R.string.database_event_closedDate)) {
            binding.eventCreateCloseDate.setError(getString(R.string.val_create_event_closed));
        }
        if (invalidIds.contains(R.string.database_event_start)) {
            if (repeat) {
                binding.eventCreateStartDate.setError(getString(R.string.val_create_event_start));
            } else {
                binding.eventCreateDate.setError(getString(R.string.val_create_event_date));
            }
        }
        if (invalidIds.contains(R.string.database_event_end)) {
            binding.eventCreateEndDate.setError(getString(R.string.val_create_event_end));
        }
        Toast.makeText(getActivity(), "Invalid", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (event != null) event.dissolve();
        binding = null;
    }


}