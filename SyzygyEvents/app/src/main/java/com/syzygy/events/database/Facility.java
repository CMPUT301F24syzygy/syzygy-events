package com.syzygy.events.database;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.syzygy.events.R;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * An instance of a facility database item
 * - A facility can be edited except for the organizer
 * @author Gareth Kmet
 * @version 1.0
 * @since 19oct24
 */
@Database.Dissovable
public class Facility extends DatabaseInstance<Facility> {

    /**
     * Checks to make sure the generic type is the type of this instance
     *
     * @param db The database
     * @param orgID The id of the organizer
     */
    @Database.Salty
    protected Facility(Database db, String orgID) throws ClassCastException {
        super(db, orgID, Database.Collections.FACILITIES, fields);
    }

    @Override
    @Database.Observes
    protected Facility cast() {
        return this;
    }

    public String getName(){
        return getPropertyValueI(R.string.database_fac_name);
    }

    public boolean setName(String val){
        return setPropertyValue(R.string.database_fac_name, val, s -> {});
    }

    public GeoPoint getLocation(){
        return getPropertyValueI(R.string.database_fac_location);
    }

    public boolean setLocation(GeoPoint val){
        return setPropertyValue(R.string.database_fac_location, val, s -> {});
    }

    public String getDescription(){
        return getPropertyValueI(R.string.database_fac_description);
    }

    public boolean setDescription(String val){
        return setPropertyValue(R.string.database_fac_description, val, s -> {});
    }

    public String getAddress(){
        return getPropertyValueI(R.string.database_fac_address);
    }

    public boolean setAddress(String address){
        return setPropertyValue(R.string.database_fac_address, address, s->{});
    }

    public Address getFullAddressFromGeo(Context context){
        GeoPoint geo = getLocation();
        return Facility.getFullAddressFromGeo(context, geo);
    }

    public LatLng getLatLngLocation(){
        GeoPoint geo = getLocation();
        return new LatLng(geo.getLatitude(), geo.getLongitude());
    }

    public static Address getFullAddressFromGeo(Context context, GeoPoint geo){
        return Facility.getFullAddressFromGeo(context, geo.getLatitude(), geo.getLongitude());
    }

    public static Address getFullAddressFromGeo(Context context, LatLng latlng){
        return Facility.getFullAddressFromGeo(context, latlng.latitude, latlng.longitude);
    }

    public static Address getFullAddressFromGeo(Context context, double lat, double lng){
        try {
            List<Address> ads = new Geocoder(context).getFromLocation(lat, lng, 1);
            return ads == null || ads.isEmpty() ? null : ads.get(0);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String getAssociatedImageLocName() {
        return getName();
    }

    public String getImageID(){
        return getPropertyValueI(R.string.database_fac_imageID);
    }

    public String getOrganizerID(){
        return getPropertyValueI(R.string.database_fac_organizer);
    }

    @Database.Observes
    public Image getImage(){
        return getPropertyInstanceI(R.string.database_fac_imageID);
    }

    /**
     * Sets the Image instance. This function will create a new reference to the instance.
     * @param image The new instance
     * @param onComplete called on completion with if the update was successful
     * @see #setAssociatedImage(Uri, Consumer)
     */
    @Database.StirsDeep(what = "The previous Image")
    public void setFacilityImage(@Nullable Uri image, Consumer<Boolean> onComplete){
        setAssociatedImage(image, onComplete);
    }

    @Database.Observes
    public User getOrganizer(){
        return getPropertyInstanceI(R.string.database_fac_organizer);
    }

    /**
     * Validates an updates all properties of the facility. If the facility changes, a notification is sent to listeners once and the database is updated once
     * @param name The name of the facility
     * @param location The location of the facility
     * @param address The address of the facility
     * @param description The description of the facility
     * @param image The new image
     * @param onComplete called once update is complete with weather the update was successful. Not called if properties are invalid
     * @return If the facility changed as a result
     * @see #updateDataFromMap(Map, Uri, String, Consumer)
     */
    @Database.StirsDeep(what = "The previous image")
    public Set<Integer> update(String name,
                          GeoPoint location,
                          String address,
                          String description,
                          Uri image,
                          Consumer<Boolean> onComplete
    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_fac_name, name);
        map.put(R.string.database_fac_location, location);
        map.put(R.string.database_fac_address, address);
        map.put(R.string.database_fac_description, description);
        return updateDataFromMap(map, image, name, onComplete);
    }

    /**
     * Validates an updates all properties of the facility. If the facility changes, a notification is sent to listeners once and the database is updated once
     * @param name The name of the facility
     * @param location The location of the facility
     * @param address The address of the facility
     * @param description The description of the facility
     * @param onComplete will always be true and will be called before return
     * @return If the facility changed as a result
     * @see #updateDataFromMap(Map, Consumer)
     */
    public Set<Integer> update(String name,
                               GeoPoint location,
                               String address,
                               String description,
                               Consumer<Boolean> onComplete
    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_fac_name, name);
        map.put(R.string.database_fac_location, location);
        map.put(R.string.database_fac_address, address);
        map.put(R.string.database_fac_description, description);
        return updateDataFromMap(map, onComplete);
    }

    /**
     * The list of the fields defined for a User
     */
    static final PropertyField<?, ?>[] fields = {
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_fac_name, o -> o instanceof String && !((String) o).isBlank(), true),
            new PropertyField<GeoPoint, PropertyField.NullInstance>(R.string.database_fac_location, o -> o instanceof GeoPoint, true),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_fac_address, o -> o instanceof String, true),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_fac_description, o -> o instanceof String, true),
            new PropertyField<String, User>(R.string.database_fac_organizer, o -> o instanceof String && !((String) o).isBlank(), false, true, Database.Collections.USERS, false, false),
            new PropertyField<String, Image>(R.string.database_fac_imageID, o -> o instanceof String, true, true, Database.Collections.IMAGES, true, true)
    };

    @Override
    protected List<Pair<Query, Database.Collections>> subInstanceCascadeDeleteQuery() {
        return Collections.singletonList(
                new Pair<>(Database.Collections.EVENTS.getCollection(db)
                        .whereEqualTo(db.constants.getString(R.string.database_event_facilityID), getDocumentID()), Database.Collections.EVENTS)
        );
    }

    /**
     * Creates a new Image instance in the database using the given data.
     * @param db The database
     * @param name The name of the facility
     * @param location The location of the facility
     * @param address The address of the location
     * @param description The description of the facility
     * @param image The facility profile image
     * @param organizerID The Id of the organizer
     * @param listener called once the instance is initialized. Not called if properties are invalid
     * @return The set property ids that are invalid
     * @see Database#createNewInstance(Database.Collections, String, Map, Database.InitializationListener)
     */
    @Database.MustStir
    public static Set<Integer> NewInstance(Database db,
                                    String name,
                                    GeoPoint location,
                                    String address,
                                    String description,
                                    Uri image,
                                    @Database.Dilutes String organizerID,
                                    Database.InitializationListener<Facility> listener
    ){

        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_fac_name, name);
        map.put(R.string.database_fac_location, location);
        map.put(R.string.database_fac_address, address);
        map.put(R.string.database_fac_description, description);
        map.put(R.string.database_fac_organizer, organizerID);

        return db.createNewInstance(Database.Collections.FACILITIES, organizerID, map, image, name, listener);
    }
}
