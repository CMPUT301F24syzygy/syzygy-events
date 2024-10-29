package com.syzygy.events.ui.organizer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInstance;
import com.syzygy.events.database.Facility;
import com.syzygy.events.database.Image;
import com.syzygy.events.databinding.FragmentOrganizerProfileBinding;
import com.syzygy.events.ui.OrganizerActivity;

public class OrganizerProfileFragment extends Fragment implements Database.UpdateListener, OnMapReadyCallback {
    private FragmentOrganizerProfileBinding binding;
    private Facility facility;
    private SupportMapFragment mapFrag;
    private GoogleMap map;
    private Marker marker;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOrganizerProfileBinding.inflate(inflater, container, false);
        facility = ((SyzygyApplication)getActivity().getApplication()).getUser().getFacility().fetch(this);
        binding.facilityButtonEdit.setOnClickListener(v -> {
            ((OrganizerActivity)getActivity()).openEdit();
        });
        updateValues();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapFrag = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.facility_map);
        mapFrag.getMapAsync(this);
    }

    private void updateValues(){
        binding.facilityProfileBio.setText(facility.getDescription());
        binding.facilityProfileName.setText(facility.getName());
        binding.facilityProfileAddress.setText(facility.getAddress());
        Image.getFormatedAssociatedImage(facility, Image.Options.Circle(256)).into(binding.facilityProfileImage);
        updateMapPoints();
    }

    private void updateMapPoints(){
        if(map == null) return;
        LatLng pos = facility.getLatLngLocation();
        marker.setPosition(pos);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        Log.println(Log.DEBUG, "fac map", "Ready");
        this.map = map;
        marker = map.addMarker(new MarkerOptions().draggable(false).position(new LatLng(0,0)));
        updateMapPoints();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        facility.dissolve(this);
        binding = null;
    }

    @Override
    public <T extends DatabaseInstance<T>> void onUpdate(DatabaseInstance<T> instance, Type type) {
        updateValues();
    }
}