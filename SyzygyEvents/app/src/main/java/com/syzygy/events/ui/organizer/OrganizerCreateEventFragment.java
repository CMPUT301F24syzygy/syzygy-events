package com.syzygy.events.ui.organizer;

import android.net.Uri;
import android.os.Bundle;
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
import com.syzygy.events.database.Event;
import com.syzygy.events.database.Image;
import com.syzygy.events.databinding.FragOrgCreateEventBinding;
import com.syzygy.events.ui.OrganizerActivity;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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


        binding.createEventSequenceChip.setOnClickListener(view -> onChangeOfRepeat());
        binding.createEventSingleChip.setOnClickListener(view -> onChangeOfRepeat());

        setImage(null);

        return binding.getRoot();
    }

    private void onChangeOfRepeat() {
        int v = binding.createEventSequenceChip.isChecked() ? View.VISIBLE : View.GONE;
        binding.eventCreateEndDate.setVisibility(v);
        binding.createEventWeekdayChips.setVisibility(v);
        binding.eventCreateDate.setVisibility(v == View.GONE ? View.VISIBLE : View.INVISIBLE);
        binding.eventCreateStartDate.setVisibility(v == View.GONE ? View.INVISIBLE : v);
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
            Image.formatDefaultImage(null, Image.Options.Square(Image.Options.Sizes.MEDIUM)).into(binding.eventCreatePosterImg);
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

        try {
            startDate = new Timestamp(Objects.requireNonNull(formatter.parse(startDay + " 12:01")));
        } catch (ParseException | NullPointerException ex) {
            startDate = null;
        }

        String endDay = repeat ? binding.eventCreateEndDate.getText().toString() : startDay;
        try {
            endDate = new Timestamp(Objects.requireNonNull(formatter.parse(endDay + " 23:59")));
        } catch (ParseException | NullPointerException ex) {
            endDate = null;
        }

        String openDay = binding.eventCreateOpenDate.getText().toString();
        String closeDay = binding.eventCreateCloseDate.getText().toString();
        try {
            openDate = new Timestamp(Objects.requireNonNull(formatter.parse(openDay + " 12:01")));
        } catch (ParseException | NullPointerException ex) {
            openDate = null;
        }

        try {
            closeDate = new Timestamp(Objects.requireNonNull(formatter.parse(closeDay + " 12:01")));
        } catch (ParseException | NullPointerException ex) {
            closeDate = null;
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
            binding.eventCreateName.setError("Bad");
        }
        if (invalidIds.contains(R.string.database_event_description)) {
            binding.eventCreateBio.setError("Bad");
        }
        if (invalidIds.contains(R.string.database_event_capacity)) {
            binding.eventCreateCapacity.setError("Bad");
        }
        if (invalidIds.contains(R.string.database_event_waitlist)) {
            binding.eventCreateWaitlistCap.setError("Bad");
        }
        if (invalidIds.contains(R.string.database_event_price)) {
            binding.eventCreatePrice.setError("Bad");
        }
        if (invalidIds.contains(R.string.database_event_openDate)) {
            binding.eventCreateOpenDate.setError("Badopen");
        }
        if (invalidIds.contains(R.string.database_event_closedDate)) {
            binding.eventCreateCloseDate.setError("Badclose");
        }
        if (invalidIds.contains(R.string.database_event_start)) {
            if (repeat) {
                binding.eventCreateStartDate.setError("Bad");
            } else {
                binding.eventCreateDate.setError("Bad");
            }
        }
        if (invalidIds.contains(R.string.database_event_end)) {
            binding.eventCreateEndDate.setError("Bad");
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