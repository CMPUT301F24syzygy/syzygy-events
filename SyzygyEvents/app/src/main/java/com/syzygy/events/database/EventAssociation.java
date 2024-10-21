package com.syzygy.events.database;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.GeoPoint;
import com.syzygy.events.R;

import java.util.HashMap;
import java.util.Map;
/**
 * An instance of an association of a user to an event
 * @author Gareth Kmet
 * @version 1.0
 * @since 20oct24
 */
public class EventAssociation extends DatabaseInstance<EventAssociation>{
    /**
     * Checks to make sure the generic type is the type of this instance
     *
     * @param db The database
     * @param assocID The id of the association
     */
    protected EventAssociation(Database db, String assocID) throws ClassCastException {
        super(db, assocID, Database.Collections.EVENT_ASSOCIATIONS, fields);
    }

    @Override
    protected EventAssociation cast() {
        return this;
    }

    public GeoPoint getLocation(){
        return getPropertyValueI(R.string.database_assoc_geo);
    }

    public boolean setLocation(GeoPoint val){
        return setPropertyValue(R.string.database_assoc_geo, val);
    }

    public String getEventID(){
        return getPropertyValueI(R.string.database_assoc_event);
    }

    public String getStatus(){
        return getPropertyValueI(R.string.database_assoc_status);
    }

    public boolean setStatus(String val){
        return setPropertyValue(R.string.database_assoc_status, val);
    }

    public String getUserID(){
        return getPropertyValueI(R.string.database_assoc_user);
    }

    public User getUser(){
        return getPropertyInstanceI(R.string.database_assoc_user);
    }
    public Event getEvent(){
        return getPropertyInstanceI(R.string.database_assoc_event);
    }

    /**
     * Updates all properties of the assoc. If the assoc changes, a notification is sent to listeners once and the database is updated once
     * @param location The location where the user signed into the event
     * @param status The status of the association
     * @return If the assoc changed as a result
     */
    public boolean update(GeoPoint location, String status){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_assoc_geo, location);
        map.put(R.string.database_assoc_status, status);
        return updateDataFromMap(db.convertIDMapToNames(map));
    }

    /**
     * The list of the fields defined for a User
     */
    private static final PropertyField<?, ?>[] fields = {
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_assoc_user, o -> o instanceof String && !((String) o).isBlank(), false),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_assoc_event, o -> o instanceof String && !((String) o).isBlank(), false),
            new PropertyField<GeoPoint, PropertyField.NullInstance>(R.string.database_assoc_geo, o -> o instanceof GeoPoint, true),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_assoc_status, o -> o instanceof String && !((String) o).isBlank(), true)
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
     * @param eventID The ID of the event
     * @param location The location where the user signed into the event
     * @param status The status of the association
     * @param userID The id of the user
     * @param listener The initializer listener: this will be called once the user is ready
     * @return The user instance in an illegal state
     * @see Database#createNewInstance(Database.Collections, String, Map, Database.InitializationListener)
     */
    public static Image NewInstance(Database db,
                                    String eventID,
                                    GeoPoint location,
                                    String status,
                                    String userID,
                                    Database.InitializationListener<Image> listener
    ){
        Map<Integer,Object> map = createDataMap(eventID, location, status, userID);

        if(!validateDataMap(map)){
            return null;
        }

        return db.createNewInstance(Database.Collections.EVENT_ASSOCIATIONS, eventID+"-"+userID, db.convertIDMapToNames(map), listener);
    }

    /**
     * Turns the properties as arguments into a map that is usable by the database
     * @param eventID The ID of the event
     * @param location The location where the user signed into the event
     * @param status The status of the association
     * @param userID The id of the user
     * @return The map
     */
    public static Map<Integer, Object> createDataMap(String eventID,
                                                     GeoPoint location,
                                                     String status,
                                                     String userID

    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_assoc_geo, location);
        map.put(R.string.database_assoc_event, eventID);
        map.put(R.string.database_assoc_status, status);
        map.put(R.string.database_assoc_user, userID);
        return map;
    }

    /**
     * Tests if the data is valid
     * @param dataMap The data map
     * @return The
     * @see #createDataMap(String, GeoPoint, String, String) 
     */
    public static boolean validateDataMap(Map<Integer, Object> dataMap){
        return DatabaseInstance.isDataValid(dataMap, fields);
    }
}
