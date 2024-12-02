package com.syzygy.events.ui.admin;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
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
                activity.navigateUp("The facility was not found.");
                return;
            }
            facility = instance;
            if(facility == null){
                activity.navigateUp("The facility was not found.");
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
                Dialog confirmRemoveDialog = new AlertDialog.Builder(getContext())
                        .setTitle("Confirm")
                        .setMessage("Are you sure you want to remove this facility? This action cannot be undone.")
                        .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.println(Log.DEBUG, "NAV", "start delete");
                                facility.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s -> {
                                    activity.navigateUp();
                                });
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create();
                confirmRemoveDialog.show();
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
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(facility.getLatLngLocation(), 9));
    }


}
