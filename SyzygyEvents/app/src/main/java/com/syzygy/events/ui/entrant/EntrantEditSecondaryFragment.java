package com.syzygy.events.ui.entrant;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.squareup.picasso.Picasso;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.Image;
import com.syzygy.events.database.User;
import com.syzygy.events.databinding.SecondaryEntrantEditBinding;
import com.syzygy.events.ui.EntrantActivity;

import java.util.Set;
import java.util.function.Consumer;

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
        Image.getFormatedAssociatedImage(user, Image.Options.Circle(Image.Options.Sizes.MEDIUM)).into(binding.entrantEditProfile);

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
        Set<Integer> invalidIds;

        binding.progressBar.setVisibility(View.VISIBLE);

        if(imageSelected){
            invalidIds = user.update(name, bio, image, email, phone, org, admin, user.isAdmin(), this::onUpdateInstance);
        }else{
            invalidIds = user.update(name, bio, email, phone, org, admin, user.isAdmin(), this::onUpdateInstance);
        }

        if(invalidIds.isEmpty()) return;

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
        binding.progressBar.setVisibility(View.GONE);

    }

    /**
     * Called on update of user
     * @param success If the success
     * @see User#update(String, String, Uri, String, String, Boolean, Boolean, Boolean, Consumer)
     */
    private void onUpdateInstance(boolean success) {
        Log.println(Log.DEBUG, "EditProfile", "update " + success);
        if(success){
            //Instead of navigating back were going to use the switch application
            //This forces the menu icons to refresh in case the profile image was changed
            ((SyzygyApplication)getActivity().getApplication()).switchToActivity(EntrantActivity.class);
        }else{
            Toast.makeText(getActivity(), "An error occurred", Toast.LENGTH_LONG).show();
            binding.progressBar.setVisibility(View.GONE);
        }
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
            Image.formatDefaultImage(Database.Collections.USERS, Image.Options.Circle(Image.Options.Sizes.MEDIUM)).into(binding.entrantEditProfile);
            binding.entrantEditRemoveImage.setVisibility(View.INVISIBLE);
        }else{
            Image.formatImage(Picasso.get().load(uri), Image.Options.Circle(Image.Options.Sizes.MEDIUM)).into(binding.entrantEditProfile);;
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
