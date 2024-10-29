package com.syzygy.events.ui.entrant;

import android.location.Address;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.model.LatLng;
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
import com.syzygy.events.databinding.SecondaryEntrantEditBinding;
import com.syzygy.events.ui.EntrantActivity;
import com.syzygy.events.ui.OrganizerActivity;

import java.util.Set;

public class EntrantEditSecondaryFragment extends Fragment {

    private SecondaryEntrantEditBinding binding;
    private User user;
    private Uri image;
    private boolean imageSelected = false;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = SecondaryEntrantEditBinding.inflate(inflater, container, false);

        user = ((SyzygyApplication)getActivity().getApplication()).getUser().fetch();

        binding.entrantEditBio.setText(user.getDescription());
        binding.entrantEditEmail.setText(user.getEmail());
        binding.entrantEditName.setText(user.getName());
        binding.entrantEditPhone.setText(user.getPhoneNumber());
        binding.entrantEditOrgNotifications.setChecked(user.getOrganizerNotifications());
        binding.entrantEditAdminNotifications.setChecked(user.getAdminNotifications());
        Image.getFormatedAssociatedImage(user, Image.Options.Circle(256)).into(binding.entrantEditProfile);

        binding.entrantEditButtonSubmit.setOnClickListener(view -> submitData());
        binding.entrantEditButtonCancel.setOnClickListener(view -> ((EntrantActivity)getActivity()).navigateUp());
        binding.entrantEditEditImage.setOnClickListener(view -> choosePhoto());
        binding.entrantEditRemoveImage.setOnClickListener(view -> setImage(null));

        binding.entrantEditRemoveImage.setVisibility(user.getProfileImage() == null ? View.INVISIBLE : View.VISIBLE);

        return binding.getRoot();
    }

    private void submitData(){
        String name = binding.entrantEditName.getText().toString();
        String phone = binding.entrantEditPhone.getText().toString();
        String email = binding.entrantEditEmail.getText().toString();
        String bio = binding.entrantEditBio.getText().toString();
        Boolean admin = binding.entrantEditAdminNotifications.isChecked();
        Boolean org = binding.entrantEditOrgNotifications.isChecked();

        SyzygyApplication app = (SyzygyApplication) getActivity().getApplication();
        Set<Integer> invalidIds = User.validateDataMap(User.createDataMap(name, bio, "", "", email, phone, org, admin, false, Timestamp.now()));

        if(!invalidIds.isEmpty()){
            if(invalidIds.contains(R.string.database_user_name)){
                binding.entrantEditName.setError("Bad");
            }
            if(invalidIds.contains(R.string.database_user_phoneNumber)){
                binding.entrantEditPhone.setError("Bad");
            }
            if(invalidIds.contains(R.string.database_user_email)){
                binding.entrantEditEmail.setError("Bad");
            }
            if(invalidIds.contains(R.string.database_user_description)){
                binding.entrantEditBio.setError("Bad");
            }
            Toast.makeText(getActivity(), "Invalid", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.println(Log.DEBUG, "edituser", "valid");
        binding.progressBar.setVisibility(View.VISIBLE);
        Image currentImage = user.getProfileImage();
        if(currentImage != null) currentImage.fetch();

        if(image != null){
            Log.println(Log.DEBUG, "edituser", "image");
            Image.NewInstance(app.getDatabase(), name, Database.Collections.USERS, user.getDocumentID(), image, (img, img_success) -> {
                if(!img_success){
                    Log.println(Log.DEBUG, "editusr", "image fail");
                    Toast.makeText(getActivity(), "An error occurred: Image", Toast.LENGTH_LONG).show();
                    binding.progressBar.setVisibility(View.GONE);
                    if(currentImage!=null)currentImage.dissolve();
                    return;
                }
                Log.println(Log.DEBUG, "edituser", "image good");
                user.update(name, bio, img.getDocumentID(), email, phone, org, admin, user.isAdmin(), usr_success -> {
                    if(usr_success){
                        img.dissolve();
                        Log.println(Log.DEBUG, "edituser", "fac good");
                        if(currentImage != null) currentImage.deleteInstance(s -> {
                            if(!s){
                                Log.println(Log.ERROR, "user edit image", "Hanging image");
                            }
                        });
                        user.dissolve();
                        ((EntrantActivity)getActivity()).navigateUp();
                        return;
                    }
                    if(currentImage!=null)currentImage.dissolve();
                    img.deleteInstance(s->{if(!s){
                        Log.println(Log.ERROR, "user edit image", "Hanging image");
                    }});
                    Log.println(Log.DEBUG, "edituser", "user fail");
                    Toast.makeText(getActivity(), "An error occurred", Toast.LENGTH_LONG).show();
                    binding.progressBar.setVisibility(View.GONE);
                });
            });
            return;
        }
        Log.println(Log.DEBUG, "edituser", "no image");
        user.update(name, bio, imageSelected||currentImage==null?"":currentImage.getDocumentID(), email, phone, org, admin, user.isAdmin(), usr_success -> {
            if(usr_success){
                Log.println(Log.DEBUG, "edituser", "fac good");
                if(currentImage != null && imageSelected) currentImage.deleteInstance(s -> {
                    if(!s){
                        Log.println(Log.ERROR, "Fac edit image", "Hanging image");
                    }
                });
                user.dissolve();
                ((EntrantActivity)getActivity()).navigateUp();
                return;
            }
            if(currentImage!=null)currentImage.dissolve();
            Log.println(Log.DEBUG, "edituser", "fac fail");
            Toast.makeText(getActivity(), "An error occurred", Toast.LENGTH_LONG).show();
            binding.progressBar.setVisibility(View.GONE);
        });

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
        imageSelected = true;
        image = uri;
        if(image == null){
            Image.formatDefaultImage(Database.Collections.USERS, Image.Options.Circle(256)).into(binding.entrantEditProfile);
            binding.entrantEditRemoveImage.setVisibility(View.INVISIBLE);
        }else{
            Image.formatImage(Picasso.get().load(uri), Image.Options.Circle(256)).into(binding.entrantEditProfile);;
            binding.entrantEditRemoveImage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        user.dissolve();
        binding = null;
    }
}
