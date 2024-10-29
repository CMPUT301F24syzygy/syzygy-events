package com.syzygy.events.ui.entrant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInstance;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.EventAssociation;
import com.syzygy.events.database.Image;
import com.syzygy.events.databinding.SecondaryEntrantEventBinding;
import com.syzygy.events.ui.EntrantActivity;

import java.util.Date;

public class EntrantEventSecondaryFragment extends Fragment {

    private SecondaryEntrantEventBinding binding;
    Event event;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = SecondaryEntrantEventBinding.inflate(inflater, container, false);

        EntrantActivity activity = (EntrantActivity)getActivity();
        SyzygyApplication app = (SyzygyApplication)getActivity().getApplication();
        app.getDatabase().getInstance(Database.Collections.EVENTS, activity.getEventID(), (instance, success) -> {
            event = (Event) instance;

            event.addListener(new Database.UpdateListener() {
                @Override
                public <T extends DatabaseInstance<T>> void onUpdate(DatabaseInstance<T> instance, Type type) {
                    ///check exists
                    ///
                    ///update view
                }
            });


            binding.eventTitle.setText(event.getTitle());
            binding.eventPriceText.setText("$ " + event.getPrice().toString());
            Image.getFormatedAssociatedImage(event, Image.Options.AsIs()).into(binding.eventImg);
            if (event.getRequiresLocation()) {
                binding.eventGeoRequiredText.setVisibility(View.VISIBLE);
            }
            ///binding.
            ///

            TextView facility_name = binding.getRoot().findViewById(R.id.card_facility_name);
            facility_name.setText(event.getFacility().getName());
            TextView facility_address = binding.getRoot().findViewById(R.id.card_facility_address);
            facility_address.setText(event.getFacility().getAddress());
            ImageView facility_image = binding.getRoot().findViewById(R.id.facility_image);
            Image.getFormatedAssociatedImage(event.getFacility(), Image.Options.AsIs()).into(facility_image);


            updateView();
        });


        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        event.dissolve();
    }


    private void updateView() {
        SyzygyApplication app = (SyzygyApplication)getActivity().getApplication();

        //event.getUserAssociation(app.getUser(), (e, a, success) -> {
            //if (a != null) {
                binding.joinWaitlistLayout.setVisibility(View.VISIBLE); ///if...
                ///configure status_bar
                ///if waitlist : show leave
                ///if invited : show accept / decline
            //}
            //else if (Timestamp.now().compareTo(event.getCloseRegistrationDate()) < 0) {
                ///and waitlist not at capacity
                //binding.joinWaitlistLayout.setVisibility(View.VISIBLE); ///
            //}
        //});

        ///set capacity
    }

}