package com.syzygy.events.ui.general;

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
import com.google.firebase.firestore.GeoPoint;
import com.squareup.picasso.Picasso;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.Facility;
import com.syzygy.events.database.Image;
import com.syzygy.events.database.User;
import com.syzygy.events.databinding.FragmentSignupBinding;
import com.syzygy.events.databinding.SecondarySignupFacilityBinding;
import com.syzygy.events.ui.OrganizerActivity;

import java.util.Set;

public class SignupFacilitySecondaryFragment extends Fragment {

    private SecondarySignupFacilityBinding binding;
    private Uri image;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = SecondarySignupFacilityBinding.inflate(inflater, container, false);

        binding.createFacilityButtonSubmit.setOnClickListener(view -> submitData());
        binding.createFacilityEditImage.setOnClickListener(view -> choosePhoto());
        binding.createFacilityRemoveImage.setOnClickListener(view -> setImage(null));

        setImage(null);

        return binding.getRoot();
    }

    private void submitData(){
        String name = binding.createFacilityName.getText().toString();
        String location = binding.createFacilityLocation.getText().toString();
        GeoPoint loc = new GeoPoint(0,0);
        String bio = binding.createFacilityBio.getText().toString();
        SyzygyApplication app = (SyzygyApplication) getActivity().getApplication();
        String user = app.getUser().getDocumentID();
        Set<Integer> invalidIds = Facility.validateDataMap(Facility.createDataMap(name, loc, bio, "", user));
        if(invalidIds.isEmpty()){
            Log.println(Log.DEBUG, "createfac", "valid");
            binding.progressBar.setVisibility(View.VISIBLE);
            if(image != null){
                Log.println(Log.DEBUG, "createfac", "image");
                Image.NewInstance(app.getDatabase(), name, Database.Collections.FACILITIES, user, image, (img, img_success) -> {
                    if(!img_success){
                        Log.println(Log.DEBUG, "createfac", "image fail");
                        Toast.makeText(getActivity(), "An error occurred: Image", Toast.LENGTH_LONG).show();
                        binding.progressBar.setVisibility(View.GONE);
                        return;
                    }
                    Log.println(Log.DEBUG, "createfac", "image good");
                    Facility.NewInstance(app.getDatabase(), name, loc, bio, img.getDocumentID(), user, (fac, fac_success) ->{
                        img.dissolve();
                        if(fac_success){
                            Log.println(Log.DEBUG, "createfac", "fac good");
                            app.getUser().setFacility(fac);
                            fac.dissolve();
                            Log.println(Log.DEBUG, "createfac", "user good");
                            app.switchToActivity(OrganizerActivity.class);
                            return;
                        };
                        Log.println(Log.DEBUG, "createfac", "fac fail");
                        Toast.makeText(getActivity(), "An error occurred", Toast.LENGTH_LONG).show();
                        binding.progressBar.setVisibility(View.GONE);
                    });
                });
                return;
            }
            Log.println(Log.DEBUG, "createfac", "no image");
            Facility.NewInstance(app.getDatabase(), name, loc, bio, "", user, (fac, fac_success) ->{
                if(fac_success){
                    Log.println(Log.DEBUG, "createfac", "fac good");
                    app.getUser().setFacility(fac);
                    fac.dissolve();
                    Log.println(Log.DEBUG, "createfac", "user good");
                    app.switchToActivity(OrganizerActivity.class);
                    return;
                };
                Log.println(Log.DEBUG, "createfac", "user fail");
                Toast.makeText(getActivity(), "An error occurred", Toast.LENGTH_LONG).show();
                binding.progressBar.setVisibility(View.GONE);
            });
            return;
        }
        for(int i : invalidIds){
            Log.println(Log.INFO, "Fac Invalid", getString(i));
        }
        if(invalidIds.contains(R.string.database_fac_name)){
            binding.createFacilityName.setError("Bad");
        }
        if(invalidIds.contains(R.string.database_fac_description)){
            binding.createFacilityBio.setError("Bad");
        }
        if(invalidIds.contains(R.string.database_fac_location)){
            binding.createFacilityLocation.setError("Bad");
        }
        Toast.makeText(getActivity(), "Invalid", Toast.LENGTH_SHORT).show();
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
            Image.formatDefaultImage(Database.Collections.FACILITIES, Image.Options.Circle(256)).into(binding.createFacilityProfile);
            binding.createFacilityRemoveImage.setVisibility(View.INVISIBLE);
        }else{
            Image.formatImage(Picasso.get().load(uri), Image.Options.Circle(256)).into(binding.createFacilityProfile);;
            binding.createFacilityRemoveImage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }



    public void submit() {
        //validate
        //
        //create facility
        SyzygyApplication app = (SyzygyApplication)getActivity().getApplication();
        app.switchToActivity(OrganizerActivity.class);
    }
}