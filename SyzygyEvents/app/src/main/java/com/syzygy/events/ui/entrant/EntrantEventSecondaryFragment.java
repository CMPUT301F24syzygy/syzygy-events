package com.syzygy.events.ui.entrant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.syzygy.events.database.Event;
import com.syzygy.events.databinding.SecondaryEntrantEventBinding;
import com.syzygy.events.ui.EntrantActivity;

public class EntrantEventSecondaryFragment extends Fragment {

    private SecondaryEntrantEventBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = SecondaryEntrantEventBinding.inflate(inflater, container, false);

        EntrantActivity activity = (EntrantActivity)getActivity();
        Event event = activity.getEvent();

        binding.entrantEventTitle.setText(event.getTitle());
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