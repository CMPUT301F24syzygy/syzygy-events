package com.syzygy.events.database;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.GeoPoint;
import com.syzygy.events.R;

import java.util.HashMap;
import java.util.Map;

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
    protected Facility(Database db, String orgID) throws ClassCastException {
        super(db, orgID, Database.Collections.FACILITIES, fields);
    }

    @Override
    protected Facility cast() {
        return this;
    }

    public String getName(){
        return getPropertyValueI(R.string.database_fac_name);
    }

    public boolean setName(String val){
        return setPropertyValue(R.string.database_fac_name, val);
    }

    public GeoPoint getLocation(){
        return getPropertyValueI(R.string.database_fac_location);
    }

    public boolean setLocation(GeoPoint val){
        return setPropertyValue(R.string.database_fac_location, val);
    }

    public String getDescription(){
        return getPropertyValueI(R.string.database_fac_description);
    }

    public boolean setDescription(String val){
        return setPropertyValue(R.string.database_fac_description, val);
    }

    public String getImageID(){
        return getPropertyValueI(R.string.database_fac_imageID);
    }

    public boolean setImageID(String val){
        return setPropertyValue(R.string.database_fac_imageID, val);
    }

    public String getOrganizerID(){
        return getPropertyValueI(R.string.database_fac_organizer);
    }

    public Image getImage(){
        return getPropertyInstanceI(R.string.database_fac_imageID);
    }

    /**
     * Sets the Image instance. This function will create a new reference to the instance.
     * @param val The new instance
     */
    public boolean setImage(@Nullable Image val){
        return setPropertyInstance(R.string.database_fac_imageID, val);
    }

    public User getOrganizer(){
        return getPropertyInstanceI(R.string.database_fac_organizer);
    }

    /**
     * Updates all properties of the facility. If the facility changes, a notification is sent to listeners once and the database is updated once
     * @param name The name of the facility
     * @param location The location of the facility
     * @param description The description of the facility
     * @param imageID The ID of the facility profile image
     * @return If the facility changed as a result
     */
    public boolean update(String name,
                          GeoPoint location,
                          String description,
                          String imageID
    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_fac_name, name);
        map.put(R.string.database_fac_location, location);
        map.put(R.string.database_fac_description, description);
        map.put(R.string.database_fac_imageID, imageID);
        return updateDataFromMap(db.convertIDMapToNames(map));
    }

    /**
     * The list of the fields defined for a User
     */
    private static final PropertyField<?, ?>[] fields = {
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_fac_name, o -> o instanceof String && !((String) o).isBlank(), true),
            new PropertyField<GeoPoint, PropertyField.NullInstance>(R.string.database_fac_location, o -> o instanceof GeoPoint, true),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_fac_description, o -> o instanceof String, true),
            new PropertyField<String, User>(R.string.database_fac_organizer, o -> o instanceof String && !((String) o).isBlank(), false, true, Database.Collections.USERS, false),
            new PropertyField<String, Image>(R.string.database_fac_imageID, o -> o instanceof String && !((String) o).isBlank(), true, true, Database.Collections.IMAGES, true)
    };


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
     * @param description The description of the facility
     * @param imageID The ID of the facility profile image
     * @param organizerID The Id of the organizer
     * @param listener The initializer listener: this will be called once the user is ready
     * @return The user instance in an illegal state
     * @see Database#createNewInstance(Database.Collections, String, Map, Database.InitializationListener)
     */
    public static Facility NewInstance(Database db,
                                    String name,
                                    GeoPoint location,
                                    String description,
                                    String imageID,
                                    String organizerID,
                                    Database.InitializationListener<Facility> listener
    ){
        Map<Integer,Object> map = createDataMap(name, location, description, imageID, organizerID);

        if(!validateDataMap(map)){
            return null;
        }

        return db.createNewInstance(Database.Collections.FACILITIES, organizerID, db.convertIDMapToNames(map), listener);
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
                                                     String description,
                                                     String imageID,
                                                     String organizerID

    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_fac_name, name);
        map.put(R.string.database_fac_location, location);
        map.put(R.string.database_fac_description, description);
        map.put(R.string.database_fac_imageID, imageID);
        map.put(R.string.database_fac_organizer, organizerID);
        return map;
    }

    /**
     * Tests if the data is valid
     * @param dataMap The data map
     * @return The
     * @see #createDataMap(String, GeoPoint, String, String, String)
     */
    public static boolean validateDataMap(Map<Integer, Object> dataMap){
        return DatabaseInstance.isDataValid(dataMap, fields);
    }
}
