package com.syzygy.events.ui.organizer;

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

import com.google.android.gms.maps.CameraUpdateFactory;
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
import com.syzygy.events.database.User;
import com.syzygy.events.databinding.SecondaryOrganizerEditBinding;
import com.syzygy.events.databinding.SecondarySignupFacilityBinding;
import com.syzygy.events.ui.EntrantActivity;
import com.syzygy.events.ui.OrganizerActivity;

import java.util.Set;
import java.util.function.Consumer;

public class OrgEditSecondaryFragment extends Fragment  implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private SecondaryOrganizerEditBinding binding;
    private Facility facility;
    private Uri image;
    private boolean selectedImage = false;
    private SupportMapFragment mapFrag;
    private Marker marker = null;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = SecondaryOrganizerEditBinding.inflate(inflater, container, false);

        facility = ((SyzygyApplication)getActivity().getApplication()).getUser().getFacility().fetch();

        binding.editFacilityButtonSubmit.setOnClickListener(view -> submitData());
        binding.editFacilityButtonCancel.setOnClickListener(view -> {
            ((OrganizerActivity)getActivity()).navigateUp();
        });
        binding.editFacilityEditImage.setOnClickListener(view -> choosePhoto());
        binding.editFacilityRemoveImage.setOnClickListener(view -> setImage(null));
        Image.getFormatedAssociatedImage(facility, Image.Options.Circle(256)).into(binding.editFacilityProfile);
        binding.editFacilityRemoveImage.setVisibility(facility.getImage() == null ? View.INVISIBLE : View.VISIBLE);
        binding.editFacilityBio.setText(facility.getDescription());
        binding.editFacilityName.setText(facility.getName());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapFrag = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.edit_facility_map);
        mapFrag.getMapAsync(this);
    }

    private void submitData(){
        if(marker == null){
            Toast.makeText(getActivity(), "Select a location", Toast.LENGTH_LONG).show();
            return;
        }
        String name = binding.editFacilityName.getText().toString();
        LatLng pos = marker.getPosition();
        GeoPoint loc = new GeoPoint(pos.latitude,pos.longitude);
        Address add = Facility.getFullAddressFromGeo(getActivity(), pos);
        String address = add == null ? "" : add.getAddressLine(0);
        Log.println(Log.INFO, "Map Selection", address);
        String bio = binding.editFacilityBio.getText().toString();
        SyzygyApplication app = (SyzygyApplication) getActivity().getApplication();
        String user = app.getUser().getDocumentID();

        Set<Integer> invalidIds;
        if(selectedImage){
            invalidIds = facility.update(name, loc, address, bio, image, this::onUpdateInstance);
        }else{
            invalidIds = facility.update(name, loc, address, bio, this::onUpdateInstance);
        }

        binding.progressBar.setVisibility(View.VISIBLE);


        if(invalidIds.isEmpty()) return;

        if(invalidIds.contains(R.string.database_fac_name)){
            binding.editFacilityName.setError("Bad");
        }
        if(invalidIds.contains(R.string.database_fac_description)){
            binding.editFacilityBio.setError("Bad");
        }
        Toast.makeText(getActivity(), "Invalid", Toast.LENGTH_SHORT).show();
        binding.progressBar.setVisibility(View.GONE);
    }

    /**
     * Called on update of facility
     * @param success If the success
     * @see Facility#update(String, GeoPoint, String, String, Uri, Consumer)
     */
    private void onUpdateInstance(boolean success) {
        if(success){
            facility.dissolve();
            ((EntrantActivity)getActivity()).navigateUp();
        }else{
            Toast.makeText(getActivity(), "An error occurred", Toast.LENGTH_LONG).show();
            binding.progressBar.setVisibility(View.GONE);
        }
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
        selectedImage = true;
        image = uri;
        if(image == null){
            Image.formatDefaultImage(Database.Collections.FACILITIES, Image.Options.Circle(256)).into(binding.editFacilityProfile);
            binding.editFacilityRemoveImage.setVisibility(View.INVISIBLE);
        }else{
            Image.formatImage(Picasso.get().load(uri), Image.Options.Circle(256)).into(binding.editFacilityProfile);;
            binding.editFacilityRemoveImage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        Log.println(Log.DEBUG, "fac map", "Ready");
        map.setOnMapClickListener(this);
        LatLng pos = facility.getLatLngLocation();
        marker = map.addMarker(new MarkerOptions().draggable(false).position(pos));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        marker.setPosition(latLng);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        facility.dissolve();
        binding = null;
    }
}