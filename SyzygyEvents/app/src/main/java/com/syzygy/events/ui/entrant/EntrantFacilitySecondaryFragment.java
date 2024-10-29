package com.syzygy.events.ui.entrant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.syzygy.events.database.Event;
import com.syzygy.events.databinding.SecondaryEntrantFacilityBinding;
import com.syzygy.events.ui.EntrantActivity;

public class EntrantFacilitySecondaryFragment extends Fragment {

    private SecondaryEntrantFacilityBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = SecondaryEntrantFacilityBinding.inflate(inflater, container, false);

        EntrantActivity activity = (EntrantActivity)getActivity();
        ///get event

        ///binding.
        ///

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
