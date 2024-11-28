package com.syzygy.events.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.MarkerOptions;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInstance;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.Facility;
import com.syzygy.events.database.Image;
import com.syzygy.events.database.User;
import com.syzygy.events.databinding.FragAdminFacilityPageBinding;
import com.syzygy.events.ui.AdminActivity;

public class AdminFacilityFragment extends Fragment implements OnMapReadyCallback {

    private FragAdminFacilityPageBinding binding;
    /**
     * The facility to display
     */
    private Facility facility;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragAdminFacilityPageBinding.inflate(inflater, container, false);

        AdminActivity activity = (AdminActivity) getActivity();
        SyzygyApplication app = (SyzygyApplication) getActivity().getApplication();
        app.getDatabase().<Facility>getInstance(Database.Collections.FACILITIES, activity.getFacilityID(), (instance, success) -> {
            if (!success) {
                activity.navigateUp("An unexpected error occured");
                return;
            }
            facility = instance;
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

            binding.adminRemoveFacilityButton.setOnClickListener(v -> {
                facility.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s -> {});
            });
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
    }


}
