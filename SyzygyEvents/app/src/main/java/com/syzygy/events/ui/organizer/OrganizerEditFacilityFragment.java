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
import com.syzygy.events.database.Facility;
import com.syzygy.events.database.Image;
import com.syzygy.events.databinding.FragOrgEditFacilityBinding;
import com.syzygy.events.ui.OrganizerActivity;

import java.util.Set;
import java.util.function.Consumer;

/**
 * The fragment that the user sees when they want to edit their facility profile
 * <p>
 * Map
 * <pre>
 * 1. Organizer Activity -> My Facility -> [Edit]
 * </pre>
 */
public class OrganizerEditFacilityFragment extends Fragment  implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private FragOrgEditFacilityBinding binding;
    /**
     * The facility
     */
    private Facility facility;
    /**
     * The image that the user has selected
     */
    private Uri image;
    /**
     * If the user has selected a new image
     */
    private boolean selectedImage = false;
    /**
     * The marker of the location on the map
     */
    private Marker marker = null;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragOrgEditFacilityBinding.inflate(inflater, container, false);

        facility = ((SyzygyApplication)getActivity().getApplication()).getUser().getFacility().fetch();

        binding.editFacilityButtonSubmit.setOnClickListener(view -> submitData());
        binding.editFacilityButtonCancel.setOnClickListener(view -> {
            ((OrganizerActivity)getActivity()).navigateUp();
        });
        binding.editFacilityEditImage.setOnClickListener(view -> choosePhoto());
        binding.editFacilityRemoveImage.setOnClickListener(view -> setImage(null));
        Image.getFormatedAssociatedImage(facility, Image.Options.Square(Image.Options.Sizes.MEDIUM)).into(binding.facilityImage);
        binding.editFacilityRemoveImage.setVisibility(facility.getImage() == null ? View.INVISIBLE : View.VISIBLE);
        binding.editFacilityEditImage.setText(facility.getImage() != null ? R.string.change_image_button : R.string.add_image_button);
        binding.editFacilityDescription.setText(facility.getDescription());
        binding.editFacilityName.setText(facility.getName());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        /**
         * The map
         */
        SupportMapFragment mapFrag = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.edit_facility_map);
        mapFrag.getMapAsync(this);
    }

    /**
     * Validates the data. If valid, edits the facility and navigates to the profile
     */
    private void submitData(){
        if(marker == null){
            Toast.makeText(getActivity(), "Select a location", Toast.LENGTH_LONG).show();
            return;
        }
        String name = binding.editFacilityName.getText().toString().replaceAll("\\s+", " ");
        LatLng pos = marker.getPosition();
        GeoPoint loc = new GeoPoint(pos.latitude,pos.longitude);
        Address add = Facility.getFullAddressFromGeo(getActivity(), pos);
        String address = add == null ? "" : add.getAddressLine(0);
        Log.println(Log.INFO, "Map Selection", address);
        String bio = binding.editFacilityDescription.getText().toString().replaceAll("\\s+", " ");
        SyzygyApplication app = (SyzygyApplication) getActivity().getApplication();
        String user = app.getUser().getDocumentID();

        Set<Integer> invalidIds;
        binding.progressBar.setVisibility(View.VISIBLE);
        if(selectedImage){
            invalidIds = facility.update(name, loc, address, bio, image, this::onUpdateInstance);
        }else{
            invalidIds = facility.update(name, loc, address, bio, this::onUpdateInstance);
        }

        binding.progressBar.setVisibility(View.GONE);


        if(invalidIds.isEmpty()) return;

        if(invalidIds.contains(R.string.database_fac_name)){
            binding.editFacilityName.setError(getString(R.string.val_create_facility_name));
        }
        if(invalidIds.contains(R.string.database_fac_description)){
            binding.editFacilityDescription.setError(getString(R.string.val_create_facility_description));
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
            //Instead of navigating back were going to use the switch application
            //This forces the menu icons to refresh in case the profile image was changed
            ((SyzygyApplication)getActivity().getApplication()).switchToActivity(OrganizerActivity.class);
        }else{
            Toast.makeText(getActivity(), "An error occurred", Toast.LENGTH_LONG).show();
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Querries the user for an image
     */
    private void choosePhoto(){
        ((SyzygyApplication)getActivity().getApplication()).getImage(uri -> {
            if(uri == null){
                Toast.makeText(getActivity(), "Failed to get image", Toast.LENGTH_LONG).show();
                return;
            }
            setImage(uri);
        });
    }

    /**
     * Displays the selected image
     * @param uri The image
     */
    private void setImage(Uri uri){
        selectedImage = true;
        image = uri;
        if(image == null){
            Image.formatDefaultImage(facility, Image.Options.Square(Image.Options.Sizes.MEDIUM)).into(binding.facilityImage);
            binding.editFacilityEditImage.setText(R.string.add_image_button);
            binding.editFacilityRemoveImage.setVisibility(View.INVISIBLE);
        }else{
            Image.formatImage(Picasso.get().load(uri), Image.Options.Square(Image.Options.Sizes.MEDIUM)).into(binding.facilityImage);;
            binding.editFacilityEditImage.setText(R.string.change_image_button);
            binding.editFacilityRemoveImage.setVisibility(View.VISIBLE);
        }
    }

    //Set the map marker to the location of the facility
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        Log.println(Log.DEBUG, "fac map", "Ready");
        map.setOnMapClickListener(this);
        LatLng pos = facility.getLatLngLocation();
        marker = map.addMarker(new MarkerOptions().draggable(false).position(pos));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
    }
    //Updates the location
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