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
import com.syzygy.events.databinding.FragmentEntrantProfileBinding;
import com.syzygy.events.ui.EntrantActivity;

public class EntrantProfileFragment extends Fragment implements Database.UpdateListener {
    private FragmentEntrantProfileBinding binding;
    private User user;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEntrantProfileBinding.inflate(inflater, container, false);
        user = ((SyzygyApplication)getActivity().getApplication()).getUser().fetch(this);

        updateValues();
        binding.entrantProfileImage.setOnClickListener(v -> {
            ((SyzygyApplication)getActivity().getApplication()).displayImage(user);
        });

        binding.entrantButtonEdit.setOnClickListener(v -> {
            ((EntrantActivity)getActivity()).openEditProfile();
        });
        return binding.getRoot();
    }

    private void updateValues(){
        binding.entrantProfileBio.setText(user.getDescription());
        binding.entrantProfileEmail.setText(user.getEmail());
        binding.entrantProfileName.setText(user.getName());
        binding.entrantProfilePhone.setText(user.getPhoneNumber());
        Image.getFormatedAssociatedImage(user, Image.Options.Circle(Image.Options.Sizes.MEDIUM)).into(binding.entrantProfileImage);
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