package com.syzygy.events.ui.entrant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInstance;
import com.syzygy.events.database.Image;
import com.syzygy.events.database.User;
import com.syzygy.events.databinding.FragEntrantProfileBinding;
import com.syzygy.events.ui.EntrantActivity;
/**
 * The fragment that the user opens to see their profile
 * <p>
 * Map
 * <pre>
 * 1. Entrant Activity -> My Profile
 * 2. Entrant Activity
 * </pre>
 */
public class EntrantProfileFragment extends Fragment implements Database.UpdateListener {
    private FragEntrantProfileBinding binding;
    /**
     * The user
     */
    private User user;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragEntrantProfileBinding.inflate(inflater, container, false);
        user = ((SyzygyApplication)getActivity().getApplication()).getUser().fetch(this);

        updateValues();

        binding.entrantProfileEditButton.setOnClickListener(v -> {
            ((EntrantActivity)getActivity()).openEditProfile();
        });
        return binding.getRoot();
    }

    /**
     * Updates the values in the fields
     */
    private void updateValues(){
        binding.entrantProfileBioText.setText(user.getDescription());
        binding.entrantProfileEmailText.setText(user.getEmail());
        binding.entrantProfileNameText.setText(user.getName());
        binding.entrantProfilePhoneText.setText(user.getPhoneNumber());
        binding.entrantProfilePhoneLabel.setVisibility(user.getPhoneNumber().isEmpty() ? View.GONE : View.VISIBLE);
        Image.getFormatedAssociatedImage(user, Image.Options.Circle(Image.Options.Sizes.MEDIUM)).into(binding.entrantProfileImageImg);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        user.dissolve(this);
    }

    @Override
    public <T extends DatabaseInstance<T>> void onUpdate(DatabaseInstance<T> instance, Type type) {
        updateValues();
    }
}