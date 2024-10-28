package com.syzygy.events.ui.entrant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.EventAssociation;
import com.syzygy.events.databinding.SecondaryEntrantEventBinding;
import com.syzygy.events.ui.EntrantActivity;

import java.util.Date;

public class EntrantEventSecondaryFragment extends Fragment {

    private SecondaryEntrantEventBinding binding;
    Event event;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = SecondaryEntrantEventBinding.inflate(inflater, container, false);

        EntrantActivity activity = (EntrantActivity)getActivity();
        event = activity.getEvent();


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


    private void updateStatus() {
        SyzygyApplication app = (SyzygyApplication)getActivity().getApplication();

        event.getUserAssociation(app.getUser(), (e, a, success) -> {
            if (a != null) {

                ///show status_bar
                ///configure status_bar
                ///if waitlist : show leave
                ///if invited : show accept / decline
            }
            else if (Timestamp.now().compareTo(event.getCloseRegistrationDate()) < 0) {
                ///and waitlist not at capacity ///show join waitlist
            }
        });

        ///set capacity
    }

}