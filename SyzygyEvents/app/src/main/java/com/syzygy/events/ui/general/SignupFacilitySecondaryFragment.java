package com.syzygy.events.ui.general;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.databinding.SecondarySignupFacilityBinding;
import com.syzygy.events.ui.OrganizerActivity;

public class SignupFacilitySecondaryFragment extends Fragment {

    private SecondarySignupFacilityBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = SecondarySignupFacilityBinding.inflate(inflater, container, false);
        return binding.getRoot();
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