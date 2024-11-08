package com.syzygy.events.ui.entrant;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.squareup.picasso.Picasso;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.Image;
import com.syzygy.events.database.User;
import com.syzygy.events.databinding.FragEntrantEditProfileBinding;
import com.syzygy.events.ui.EntrantActivity;

import java.util.Set;
import java.util.function.Consumer;

/**
 * The fragment that the user sees when they edit their own user profile
 * <p>
 * Map
 * <pre>
 * 1. Entrant Activity -> My Profile -> Edit Profile
 * </pre>
 */
public class EntrantEditProfileFragment extends Fragment {

    private FragEntrantEditProfileBinding binding;
    /**
     * The user that is being edited
     */
    private User user;
    /**
     * The current profile image selected by the user
     */
    private Uri image;
    /**
     * If the users has selected a profile image
     */
    private boolean imageSelected = false;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragEntrantEditProfileBinding.inflate(inflater, container, false);

        user = ((SyzygyApplication)getActivity().getApplication()).getUser().fetch();
        //Set up fields
        binding.entrantEditBio.setText(user.getDescription());
        binding.entrantEditEmail.setText(user.getEmail());
        binding.entrantEditName.setText(user.getName());
        binding.entrantEditPhone.setText(user.getPhoneNumber());
        binding.orgNotificationsCheckbox.setChecked(user.getOrganizerNotifications());
        binding.adminNotificationsCheckbox.setChecked(user.getAdminNotifications());
        Image.getFormatedAssociatedImage(user, Image.Options.Circle(Image.Options.Sizes.MEDIUM)).into(binding.entrantEditProfile);
        //Set up buttons
        binding.entrantEditButtonSubmit.setOnClickListener(view -> submitData());
        binding.entrantEditButtonCancel.setOnClickListener(view -> ((EntrantActivity)getActivity()).navigateUp());
        binding.entrantEditEditImage.setOnClickListener(view -> choosePhoto());
        binding.entrantEditRemoveImage.setOnClickListener(view -> setImage(null));

        binding.entrantEditRemoveImage.setVisibility(user.getProfileImage() == null ? View.INVISIBLE : View.VISIBLE);
        binding.entrantEditEditImage.setText(user.getProfileImage() != null ? R.string.change_image_button : R.string.add_image_button);

        return binding.getRoot();
    }

    /**
     * Validates and submits the edit. If valid, navigates back to the user profile
     */
    private void submitData(){
        String name = binding.entrantEditName.getText().toString();
        String phone = binding.entrantEditPhone.getText().toString();
        String email = binding.entrantEditEmail.getText().toString();
        String bio = binding.entrantEditBio.getText().toString();
        Boolean admin = binding.adminNotificationsCheckbox.isChecked();
        Boolean org = binding.orgNotificationsCheckbox.isChecked();

        SyzygyApplication app = (SyzygyApplication) getActivity().getApplication();
        Set<Integer> invalidIds;

        binding.progressBar.setVisibility(View.VISIBLE);

        if(imageSelected){
            invalidIds = user.update(name, bio, image, email, phone, org, admin, user.isAdmin(), this::onUpdateInstance);
        }else{
            invalidIds = user.update(name, bio, email, phone, org, admin, user.isAdmin(), this::onUpdateInstance);
        }

        binding.progressBar.setVisibility(View.GONE);

        if(invalidIds.isEmpty()) return;

        if(invalidIds.contains(R.string.database_user_name)){
            binding.entrantEditName.setError(getString(R.string.val_create_user_name));
        }
        if(invalidIds.contains(R.string.database_user_phoneNumber)){
            binding.entrantEditPhone.setError(getString(R.string.val_create_user_phoneNumber));
        }
        if(invalidIds.contains(R.string.database_user_email)){
            binding.entrantEditEmail.setError(getString(R.string.val_create_user_email));
        }
        if(invalidIds.contains(R.string.database_user_description)){
            binding.entrantEditBio.setError(getString(R.string.val_create_user_description));
        }
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
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Queries the user for an image
     */
    private void choosePhoto(){
        ((SyzygyApplication)getActivity().getApplication()).getImage(uri -> {
            if(uri == null){
                return;
            }
            setImage(uri);
        });
    }

    /**
     * Sets the currently displayed image
     * @param uri The image to display
     */
    private void setImage(Uri uri){
        imageSelected = true;
        image = uri;
        if(image == null){
            Image.formatDefaultImage(user, Image.Options.Circle(Image.Options.Sizes.MEDIUM)).into(binding.entrantEditProfile);
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
