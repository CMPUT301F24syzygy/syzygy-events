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

import java.util.Collections;
import java.util.List;
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
        //todo try catch numbers
        Integer capacity = Integer.parseInt(binding.eventCreateCapacity.getText().toString());
        Integer waitlistCapacity = binding.eventCreateCapWaitlist.isChecked() ? Integer.parseInt(binding.eventCreateWaitlist.getText().toString()) : -1;
        Double price = Double.parseDouble(binding.eventCreatePrice.getText().toString());
        Timestamp openDate = null;//todo
        Timestamp closeDate = Timestamp.now();//todo
        List<Timestamp> dates = Collections.singletonList(Timestamp.now()); //todo


        Set<Integer> invalidIds = Event.validateDataMap(Event.createDataMap(
                title, "", facilityID, requiresGeo, description, capacity, waitlistCapacity, "", price, openDate, closeDate, dates, Timestamp.now()));
        if(invalidIds.isEmpty()){
            Log.println(Log.DEBUG, "create event", "valid");
            binding.progressBar.setVisibility(View.VISIBLE);
            SyzygyApplication app = (SyzygyApplication) getActivity().getApplication();
            if(image != null){
                Log.println(Log.DEBUG, "create event", "image");
                Image.NewInstance(app.getDatabase(), title, Database.Collections.EVENTS, "testingLocID", image, (img, img_success) -> {
                    if(!img_success){
                        Log.println(Log.DEBUG, "create event", "image fail");
                        Toast.makeText(getActivity(), "An error occurred: Image", Toast.LENGTH_LONG).show();
                        binding.progressBar.setVisibility(View.GONE);
                        return;
                    }
                    Log.println(Log.DEBUG, "create event", "image good");
                    Event.NewInstance(app.getDatabase(), title, img.getDocumentID(), facilityID, requiresGeo, description, capacity, waitlistCapacity, price, openDate, closeDate, dates, (evnt, evnt_success) -> {
                        if(evnt_success){
                            img.dissolve();
                            Log.println(Log.DEBUG, "create event", "event good");
                            this.event = evnt;
                            ((OrganizerActivity)getActivity()).openEvent(evnt.getDocumentID());
                            return;
                        }
                        img.deleteInstance(s->{if(!s){
                            Log.println(Log.ERROR, "Event create image", "Hanging image");
                        }});
                        Log.println(Log.DEBUG, "create event", "event fail");
                        Toast.makeText(getActivity(), "An error occurred", Toast.LENGTH_LONG).show();
                        binding.progressBar.setVisibility(View.GONE);
                    });
                });
                return;
            }
            Log.println(Log.DEBUG, "create event", "no image");

            Event.NewInstance(app.getDatabase(), title, "", facilityID, requiresGeo, description, capacity, waitlistCapacity, price, openDate, closeDate, dates, (evnt, evnt_success) -> {
                if(evnt_success){
                    Log.println(Log.DEBUG, "create event", "event good");
                    this.event = evnt;
                    ((OrganizerActivity)getActivity()).openEvent(evnt.getDocumentID());
                    return;
                }
                Log.println(Log.DEBUG, "create event", "event fail");
                Toast.makeText(getActivity(), "An error occurred", Toast.LENGTH_LONG).show();
                binding.progressBar.setVisibility(View.GONE);
            });
            return;
        }
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
            binding.eventCreateName.setError("Badopen");//todo
        }
        if(invalidIds.contains(R.string.database_event_closedDate)){
            binding.eventCreateName.setError("Badclose");//todo
        }
        if(invalidIds.contains(R.string.database_event_dates)){
            binding.eventCreateName.setError("Baddates");//todo
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