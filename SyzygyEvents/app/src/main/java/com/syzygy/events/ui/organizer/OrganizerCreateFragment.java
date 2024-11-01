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
import com.syzygy.events.database.User;
import com.syzygy.events.databinding.FragmentOrganizerCreateBinding;
import com.syzygy.events.databinding.FragmentOrganizerProfileBinding;
import com.syzygy.events.ui.OrganizerActivity;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class OrganizerCreateFragment extends Fragment {
    private FragmentOrganizerCreateBinding binding;
    private Uri image;
    private Event event;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOrganizerCreateBinding.inflate(inflater, container, false);

        binding.eventCreateButtonSubmit.setOnClickListener(view -> submitData());
        binding.eventCreateEditImage.setOnClickListener(view -> choosePhoto());
        binding.eventCreateRemoveImage.setOnClickListener(view -> setImage(null));

        binding.eventCreateCapWaitlist.setOnClickListener(view -> {
            binding.eventCreateWaitlistCnt.setVisibility(binding.eventCreateCapWaitlist.isChecked() ? View.VISIBLE : View.GONE);
        });

        binding.eventCreateRepeat.setOnClickListener(view -> {
            int v = binding.eventCreateRepeat.isChecked() ? View.VISIBLE : View.GONE;
            binding.eventCreateEndDate.setVisibility(v);
            binding.createEventDaysCnt.setVisibility(v);
        });

        setImage(null);

        return binding.getRoot();
    }

    private void choosePhoto(){
        ((SyzygyApplication)getActivity().getApplication()).getImage(uri -> {
            if(uri == null){
                Toast.makeText(getActivity(), "Failed to get image", Toast.LENGTH_LONG).show();
                return;
            }
            setImage(uri);
        });
    }

    private void setImage(Uri uri){
        image = uri;
        if(image == null){
            Image.formatDefaultImage(Database.Collections.USERS, Image.Options.Circle(256)).into(binding.eventCreateProfile);
            binding.eventCreateRemoveImage.setVisibility(View.INVISIBLE);
        }else{
            Image.formatImage(Picasso.get().load(uri), Image.Options.Circle(256)).into(binding.eventCreateProfile);;
            binding.eventCreateRemoveImage.setVisibility(View.VISIBLE);
        }
    }

    private void submitData(){
        String title = binding.eventCreateName.getText().toString();
        String facilityID = ((SyzygyApplication)getActivity().getApplication()).getUser().getFacilityID();
        Boolean requiresGeo = binding.eventCreateRequireGeo.isChecked();
        String description = binding.eventCreateBio.getText().toString();

        Long capacity;
        Long waitlistCapacity;
        Double price;
        try{
            capacity = Long.parseLong(binding.eventCreateCapacity.getText().toString());
        }catch (NumberFormatException ex){
            capacity = null;
        }
        try{
            waitlistCapacity = binding.eventCreateCapWaitlist.isChecked() ? Long.parseLong(binding.eventCreateWaitlist.getText().toString()) : -1;
        }catch (NumberFormatException ex){
            waitlistCapacity = null;
        }
        try{
            price = Double.parseDouble(binding.eventCreatePrice.getText().toString());
        }catch (NumberFormatException ex){
            price = null;
        }

        Timestamp openDate;
        Timestamp closeDate;
        Timestamp startDate;
        Timestamp endDate;

        boolean repeat = binding.eventCreateRepeat.isChecked();
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String startDay = binding.eventCreateStartDate.getText().toString();
        String startTime = binding.eventCreateStartTime.getText().toString();
        String endTime = binding.eventCreateEndTime.getText().toString();
        String endDay = repeat ? binding.eventCreateEndDate.getText().toString() : startDay;
        try{
            startDate = new Timestamp(Objects.requireNonNull(formatter.parse(startDay + " " + startTime)));
        }catch (ParseException | NullPointerException ex){
            startDate = null;
        }

        try{
            endDate = new Timestamp(Objects.requireNonNull(formatter.parse(endDay + " " + endTime)));
        }catch (ParseException | NullPointerException ex){
            endDate = null;
        }

        String openDay = binding.eventCreateOpenDate.getText().toString();
        String openTime = binding.eventCreateOpenTime.getText().toString();
        String closeTime = binding.eventCreateCloseTime.getText().toString();
        String closeDay = binding.eventCreateCloseDate.getText().toString();
        try{
            openDate = new Timestamp(Objects.requireNonNull(formatter.parse(openDay + " " + openTime)));
        }catch (ParseException | NullPointerException ex){
            openDate = null;
        }

        try{
            closeDate = new Timestamp(Objects.requireNonNull(formatter.parse(closeDay + " " + closeTime)));
        }catch (ParseException | NullPointerException ex){
            closeDate = null;
        }

        Long dates;
        if(repeat){
            dates = (binding.createEventDaysM.isChecked() ? Event.Dates.MONDAY : 0) |
                    (binding.createEventDaysT.isChecked() ? Event.Dates.TUESDAY : 0) |
                    (binding.createEventDaysW.isChecked() ? Event.Dates.WEDNESDAY : 0) |
                    (binding.createEventDaysR.isChecked() ? Event.Dates.THURSDAY : 0) |
                    (binding.createEventDaysF.isChecked() ? Event.Dates.FRIDAY : 0) |
                    (binding.createEventDaysSat.isChecked() ? Event.Dates.SATURDAY : 0) |
                    (binding.createEventDaysSun.isChecked() ? Event.Dates.SUNDAY : 0);
        } else {
            dates = Event.Dates.NO_REPEAT;
        }

        SyzygyApplication app = (SyzygyApplication) getActivity().getApplication();
        binding.progressBar.setVisibility(View.VISIBLE);
        Set<Integer> invalidIds = Event.NewInstance(app.getDatabase(), title, image, facilityID, requiresGeo, description, capacity, waitlistCapacity, price, openDate, closeDate, startDate, endDate, dates, (evnt, evnt_success) -> {
            binding.progressBar.setVisibility(View.GONE);
            if(evnt_success){
                this.event = evnt;
                ((OrganizerActivity)getActivity()).openEvent(evnt.getDocumentID());
            }else{
                Toast.makeText(getActivity(), "An error occurred", Toast.LENGTH_LONG).show();
                binding.progressBar.setVisibility(View.GONE);
            }
        });

        if(invalidIds.isEmpty()) return;

        if(invalidIds.contains(R.string.database_event_title)){
            binding.eventCreateName.setError("Bad");
        }
        if(invalidIds.contains(R.string.database_event_description)){
            binding.eventCreateBio.setError("Bad");
        }
        if(invalidIds.contains(R.string.database_event_capacity)){
            binding.eventCreateCapacity.setError("Bad");
        }
        if(invalidIds.contains(R.string.database_event_waitlist)){
            binding.eventCreateWaitlist.setError("Bad");
        }
        if(invalidIds.contains(R.string.database_event_price)){
            binding.eventCreatePrice.setError("Bad");
        }
        if(invalidIds.contains(R.string.database_event_openDate)){
            binding.eventCreateOpenDate.setError("Badopen");
            binding.eventCreateOpenTime.setError("Badopen");
        }
        if(invalidIds.contains(R.string.database_event_closedDate)){
            binding.eventCreateCloseDate.setError("Badclose");
            binding.eventCreateCloseTime.setError("Badclose");
        }
        if(invalidIds.contains(R.string.database_event_start)){
            binding.eventCreateStartDate.setError("Bad");
            binding.eventCreateStartTime.setError("Bad");
        }
        if(invalidIds.contains(R.string.database_event_end)){
            binding.eventCreateEndDate.setError("Bad");
            binding.eventCreateEndTime.setError("Bad");
        }
        if(invalidIds.contains(R.string.database_event_dates)){
            binding.eventCreateRepeat.setError("Baddates");
        }
        Toast.makeText(getActivity(), "Invalid", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(event!=null) event.dissolve();
        binding = null;
    }
}