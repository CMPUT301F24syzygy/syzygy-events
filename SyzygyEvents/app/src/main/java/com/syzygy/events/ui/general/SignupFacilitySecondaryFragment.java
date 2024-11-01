package com.syzygy.events.ui.general;

import android.location.Address;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.GeoPoint;
import com.squareup.picasso.Picasso;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.Facility;
import com.syzygy.events.database.Image;
import com.syzygy.events.databinding.SecondarySignupFacilityBinding;
import com.syzygy.events.ui.OrganizerActivity;

import java.util.Set;

public class SignupFacilitySecondaryFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private SecondarySignupFacilityBinding binding;
    private Uri image;
    private SupportMapFragment mapFrag;
    private Marker marker = null;
    private GoogleMap map = null;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = SecondarySignupFacilityBinding.inflate(inflater, container, false);

        binding.createFacilityButtonSubmit.setOnClickListener(view -> submitData());
        binding.createFacilityEditImage.setOnClickListener(view -> choosePhoto());
        binding.createFacilityRemoveImage.setOnClickListener(view -> setImage(null));
        setImage(null);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapFrag = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.create_facility_map);
        mapFrag.getMapAsync(this);

    }

    private void submitData(){
        if(marker == null){
            Toast.makeText(getActivity(), "Select a location", Toast.LENGTH_LONG).show();
            return;
        }
        String name = binding.createFacilityName.getText().toString();
        LatLng pos = marker.getPosition();
        GeoPoint loc = new GeoPoint(pos.latitude,pos.longitude);
        Address add = Facility.getFullAddressFromGeo(getActivity(), pos);
        String address = add == null ? "" : add.getAddressLine(0);
        Log.println(Log.INFO, "Map Selection", address);
        String bio = binding.createFacilityBio.getText().toString();
        SyzygyApplication app = (SyzygyApplication) getActivity().getApplication();
        String user = app.getUser().getDocumentID();

        binding.progressBar.setVisibility(View.VISIBLE);
        Set<Integer> invalidIds = Facility.NewInstance(app.getDatabase(), name, loc, address, bio, image, user, (fac, fac_success) -> {
            if(fac_success){
                app.getUser().setFacility(fac);
                fac.dissolve();
                app.switchToActivity(OrganizerActivity.class);
            }else{
                Toast.makeText(getActivity(), "An error occurred", Toast.LENGTH_LONG).show();
                binding.progressBar.setVisibility(View.GONE);
            }
        });
        if(invalidIds.isEmpty()) return;

        if(invalidIds.contains(R.string.database_fac_name)){
            binding.createFacilityName.setError("Bad");
        }
        if(invalidIds.contains(R.string.database_fac_description)){
            binding.createFacilityBio.setError("Bad");
        }
        Toast.makeText(getActivity(), "Invalid", Toast.LENGTH_SHORT).show();
    }

    private void choosePhoto(){
        ((SyzygyApplication)getActivity().getApplication()).getImage(uri -> {
            if(uri == null){
                Toast.makeText(getActivity(), "Failed to get image", Toast.LENGTH_LONG).show();
                return;
            }
            setImage(uri);
        });
    }

    private void setImage(Uri uri){
        image = uri;
        if(image == null){
            Image.formatDefaultImage(Database.Collections.FACILITIES, Image.Options.Circle(Image.Options.Sizes.MEDIUM)).into(binding.createFacilityProfile);
            binding.createFacilityRemoveImage.setVisibility(View.INVISIBLE);
        }else{
            Image.formatImage(Picasso.get().load(uri), Image.Options.Circle(Image.Options.Sizes.MEDIUM)).into(binding.createFacilityProfile);;
            binding.createFacilityRemoveImage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        Log.println(Log.DEBUG, "fac map", "Ready");
        map.setOnMapClickListener(this);
        this.map=map;
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        if(marker == null){
            marker = map.addMarker(new MarkerOptions()
                    .position(latLng).draggable(true)
            );
        }else{
            marker.setPosition(latLng);
        }

    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}