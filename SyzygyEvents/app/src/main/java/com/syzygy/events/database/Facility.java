package com.syzygy.events.database;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
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

    public String getImageID(){
        return getPropertyValueI(R.string.database_fac_imageID);
    }

    public boolean setImageID(String val, Database.Querrier.EmptyListener onComplete){
        return setPropertyValue(R.string.database_fac_imageID, val, onComplete);
    }

    public String getOrganizerID(){
        return getPropertyValueI(R.string.database_fac_organizer);
    }

    @Database.Observes
    public Image getImage(){
        return getPropertyInstanceI(R.string.database_fac_imageID);
    }

    @Override
    @Database.Observes
    public Image getAssociatedImage() {
        return getImage();
    }

    /**
     * Sets the Image instance. This function will create a new reference to the instance.
     * @param val The new instance
     */
    @Database.StirsDeep(what = "The previous image")
    public boolean setImage(@Nullable @Database.Dilutes Image val){
        return setPropertyInstance(R.string.database_fac_imageID, val);
    }

    @Database.Observes
    public User getOrganizer(){
        return getPropertyInstanceI(R.string.database_fac_organizer);
    }

    /**
     * Updates all properties of the facility. If the facility changes, a notification is sent to listeners once and the database is updated once
     * @param name The name of the facility
     * @param location The location of the facility
     * @param address The address of the facility
     * @param description The description of the facility
     * @param imageID The ID of the facility profile image
     * @return If the facility changed as a result
     */
    @Database.StirsDeep(what = "The previous image")
    public boolean update(String name,
                          GeoPoint location,
                          String address,
                          String description,
                          @Database.Dilutes String imageID,
                          Database.Querrier.EmptyListener onComplete
    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_fac_name, name);
        map.put(R.string.database_fac_location, location);
        map.put(R.string.database_fac_address, address);
        map.put(R.string.database_fac_description, description);
        map.put(R.string.database_fac_imageID, imageID);
        return updateDataFromMap(db.convertIDMapToNames(map), onComplete);
    }

    /**
     * The list of the fields defined for a User
     */
    private static final PropertyField<?, ?>[] fields = {
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
     * <p>
     *     Data is validated before creating. If the data is invalid, {@code null} is returned
     * </p>
     * <p>
     *     The instance will be invalid on return, only use it after waiting for the initialization listener
     * </p>
     * @param db The database
     * @param name The name of the facility
     * @param location The location of the facility
     * @param address The address of the location
     * @param description The description of the facility
     * @param imageID The ID of the facility profile image
     * @param organizerID The Id of the organizer
     * @param listener The initializer listener: this will be called once the user is ready
     * @see Database#createNewInstance(Database.Collections, String, Map, Database.InitializationListener)
     */
    @Database.MustStir
    public static void NewInstance(Database db,
                                    String name,
                                    GeoPoint location,
                                    String address,
                                    String description,
                                    @Database.Dilutes String imageID,
                                    @Database.Dilutes String organizerID,
                                    Database.InitializationListener<Facility> listener
    ){
        Map<Integer,Object> map = createDataMap(name, location, address, description, imageID, organizerID);

        if(!validateDataMap(map).isEmpty()){
            listener.onInitialization(null, false);
            return;
        }

        db.createNewInstance(Database.Collections.FACILITIES, organizerID, db.convertIDMapToNames(map), listener);
    }

    /**
     * Turns the properties as arguments into a map that is usable by the database
     * @param name The name of the facility
     * @param location The location of the facility
     * @param description The description of the facility
     * @param imageID The ID of the facility profile image
     * @param organizerID The Id of the organizer
     * @return The map
     */
    public static Map<Integer, Object> createDataMap(String name,
                                                     GeoPoint location,
                                                     String address,
                                                     String description,
                                                     @Database.Observes String imageID,
                                                     @Database.Observes String organizerID

    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_fac_name, name);
        map.put(R.string.database_fac_location, location);
        map.put(R.string.database_fac_address, address);
        map.put(R.string.database_fac_description, description);
        map.put(R.string.database_fac_imageID, imageID);
        map.put(R.string.database_fac_organizer, organizerID);
        return map;
    }

    /**
     * Tests if the data is valid
     * @param dataMap The data map
     * @return The invalid ids
     * @see #createDataMap(String, GeoPoint, String, String, String, String) 
     */
    public static Set<Integer> validateDataMap(@Database.Observes Map<Integer, Object> dataMap){
        return DatabaseInstance.isDataValid(dataMap, fields);
    }
}
