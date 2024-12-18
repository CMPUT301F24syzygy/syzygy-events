package com.syzygy.events.ui.entrant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.MarkerOptions;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.Facility;
import com.syzygy.events.database.Image;
import com.syzygy.events.databinding.FragEntrantFacilityPageBinding;
import com.syzygy.events.ui.EntrantActivity;

/**
 * The fragment that the user sees when they open a facility page. Displays the facility of the
 * currently selected event
 * <p>
 * Map
 * <pre>
 * 1. Entrant Activity -> My Events -> [Event] -> [Facility]
 * 2. Entrant Activity -> Notifications -> [Notification] -> [Event] -> [Facility]
 * 3. Entrant Activity -> QR Scan -> [Scan QR] -> [Facility]
 * </pre>
 */
public class EntrantFacilityPageFragment extends Fragment implements OnMapReadyCallback {

    private FragEntrantFacilityPageBinding binding;
    /**
     * The facility to display
     */
    private Facility facility;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragEntrantFacilityPageBinding.inflate(inflater, container, false);

        EntrantActivity activity = (EntrantActivity) getActivity();
        SyzygyApplication app = (SyzygyApplication) getActivity().getApplication();
        app.getDatabase().<Event>getInstance(Database.Collections.EVENTS, activity.getEventID(), (instance, success) -> {
            if (!success) {
                activity.navigateUp("An unexpected error occured");
                return;
            }
            facility = instance.getFacility();
            instance.dissolve();
            if(facility == null){
                activity.navigateUp("The facility was not found");
                return;
            }
            facility.fetch();
            binding.facilityNameText.setText(facility.getName());
            binding.facilityLocationText.setText(facility.getAddress());
            binding.facilityDescriptionText.setText(facility.getDescription());
            Image.getFormatedAssociatedImage(facility, Image.Options.Square(400)).into(binding.facilityImg);

            SupportMapFragment map = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.facility_location_map);
            map.getMapAsync(this);
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(facility != null){
            facility.dissolve();
        }
        binding = null;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        map.addMarker(new MarkerOptions()
                .draggable(false)
                .position(facility.getLatLngLocation()));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(facility.getLatLngLocation(), 9));
    }

}