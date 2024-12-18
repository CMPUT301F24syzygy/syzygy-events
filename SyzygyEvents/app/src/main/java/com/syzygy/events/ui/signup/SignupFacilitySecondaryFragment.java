package com.syzygy.events.ui.signup;

import android.location.Address;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.GeoPoint;
import com.squareup.picasso.Picasso;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.Facility;
import com.syzygy.events.database.Image;
import com.syzygy.events.databinding.FragSignupOrganizerBinding;
import com.syzygy.events.ui.OrganizerActivity;

import java.util.Set;

/**
 * The fragment to create a new facility
 */
public class SignupFacilitySecondaryFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private FragSignupOrganizerBinding binding;
    /**
     * The current selected image
     */
    private Uri image;
    /**
     * The marker of the location
     */
    private Marker marker = null;
    /**
     * The map
     */
    private GoogleMap map = null;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragSignupOrganizerBinding.inflate(inflater, container, false);

        binding.createFacilityButtonSubmit.setOnClickListener(view -> submitData());
        binding.createFacilityEditImage.setOnClickListener(view -> choosePhoto());
        binding.createFacilityRemoveImage.setOnClickListener(view -> setImage(null));
        setImage(null);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFrag = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.create_facility_map);
        mapFrag.getMapAsync(this);

    }

    /**
     * Validates the information. If valid, creates the facility and navigates to the profile
     */
    private void submitData(){
        binding.createFacilityNameLayout.setError(null);

        if(marker == null || !marker.isVisible()){
            Snackbar.make(getActivity(), binding.getRoot(), "Please select a location", Snackbar.LENGTH_LONG)
                    .show();
            if (binding.createFacilityName.getText().toString().isEmpty()) {
                binding.createFacilityNameLayout.setError(getString(R.string.val_create_facility_name));
            }
            return;
        }

        String name = binding.createFacilityName.getText().toString().replaceAll("\\s+", " ");
        LatLng pos = marker.getPosition();
        GeoPoint loc = new GeoPoint(pos.latitude,pos.longitude);
        Address add = Facility.getFullAddressFromGeo(getActivity(), pos);
        String address = add == null ? "" : add.getAddressLine(0);
        Log.println(Log.INFO, "Map Selection", address);
        String bio = binding.createFacilityDescription.getText().toString().replaceAll("\\s+", " ");
        SyzygyApplication app = (SyzygyApplication) getActivity().getApplication();
        String user = app.getUser().getDocumentID();

        binding.progressBar.setVisibility(View.VISIBLE);
        Set<Integer> invalidIds = Facility.NewInstance(app.getDatabase(), name, loc, address, bio, image, user, (fac, fac_success) -> {
            if(fac_success){
                app.getUser().setFacility(fac);
                fac.dissolve();
                app.switchToActivity(OrganizerActivity.class);
            }else{
                binding.progressBar.setVisibility(View.GONE);
            }
        });
        binding.progressBar.setVisibility(View.GONE);
        if(invalidIds.isEmpty()) return;

        if(invalidIds.contains(R.string.database_fac_name)){
            binding.createFacilityNameLayout.setError(getString(R.string.val_create_facility_name));
        }
    }

    /**
     * Queries the user for an image
     */
    private void choosePhoto(){
        ((SyzygyApplication)getActivity().getApplication()).getImage(uri -> {
            if(uri == null){
                return;
            }
            setImage(uri);
        });
    }

    /**
     * Sets the current image to display. If null, removes the image
     * @param uri The image
     */
    private void setImage(Uri uri){
        image = uri;
        if(image == null){
            Image.formatDefaultImage(Database.Collections.FACILITIES, Image.Options.Square(Image.Options.Sizes.MEDIUM)).into(binding.facilityImage);
            binding.createFacilityEditImage.setText(R.string.add_image_button);
            binding.createFacilityRemoveImage.setVisibility(View.GONE);
        }else{
            Image.formatImage(Picasso.get().load(uri), Image.Options.Square(Image.Options.Sizes.MEDIUM)).into(binding.facilityImage);;
            binding.createFacilityEditImage.setText(R.string.change_image_button);
            binding.createFacilityRemoveImage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        LatLng latLng = new LatLng(53.559965, -113.470836);

        Log.println(Log.DEBUG, "fac map", "Ready");
        map.setOnMapClickListener(this);
        marker = map.addMarker(new MarkerOptions()
                .draggable(true)
                .position(latLng));
        this.map=map;

        marker.setVisible(false);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 8));
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        marker.setPosition(latLng);
        marker.setVisible(true);
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}