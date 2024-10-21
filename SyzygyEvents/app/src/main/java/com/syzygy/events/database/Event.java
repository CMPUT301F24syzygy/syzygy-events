package com.syzygy.events.database;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.syzygy.events.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An instance of a event database item
 * - An event can be edited
 * @author Gareth Kmet
 * @version 1.0
 * @since 19oct24
 */
public class Event extends DatabaseInstance<Event> {

    /**
     * Checks to make sure the generic type is the type of this instance
     *
     * @param db The database
     * @param eventID The id of the event
     */
    protected Event(Database db, String eventID) throws ClassCastException {
        super(db, eventID, Database.Collections.EVENTS, fields);
    }

    @Override
    protected Event cast() {
        return this;
    }

    /**
     * The list of the fields defined for a User
     */
    private static final PropertyField<?, ?>[] fields = {
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_event_title, o -> o instanceof String && !((String) o).isBlank(), true),
            new PropertyField<String, Image>(R.string.database_event_facilityID, o -> o instanceof String && !((String) o).isBlank(), true, true, Database.Collections.IMAGES, false),
            new PropertyField<String, Facility>(R.string.database_event_posterID, o -> o instanceof String, true, true, Database.Collections.FACILITIES, true),
            new PropertyField<Boolean, PropertyField.NullInstance>(R.string.database_event_geo, o -> o instanceof Boolean, true),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_event_description, o -> o instanceof String && !((String) o).isBlank(), true),
            new PropertyField<Integer, PropertyField.NullInstance>(R.string.database_event_capacity, o -> o instanceof Integer && (Integer)o > 0, true),
            new PropertyField<Integer, PropertyField.NullInstance>(R.string.database_event_waitlist, o -> o instanceof Integer && ((Integer)o > 0 || (Integer)o == -1), true),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_event_qrHash, o -> o instanceof String && !((String) o).isBlank(), false),
            new PropertyField<Double, PropertyField.NullInstance>(R.string.database_event_price, o -> o instanceof Double && ((Double)o)>=0, true),
            new PropertyField<Timestamp, PropertyField.NullInstance>(R.string.database_event_date, o -> o instanceof Timestamp, true)
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
     * @param title The title of the event
     * @param posterID The id of the poster image
     * @param facilityID the id of the facility
     * @param requiresLocation If the users geolocation is recorded on signup
     * @param description the description of the event
     * @param capacity The max capacity of the event
     * @param waitlistCapacity the max waitlist capacity of the event
     * @param qrHash the cashed qr code data for the event
     * @param price the price of the event
     * @param date the date of the event
     * @return The user instance in an illegal state
     * @see Database#createNewInstance(Database.Collections, String, Map, Database.InitializationListener)
     */
    public static Event NewInstance(Database db,
                                       String title,
                                       String posterID,
                                       String facilityID,
                                       Boolean requiresLocation,
                                       String description,
                                       Integer capacity,
                                       Integer waitlistCapacity,
                                       String qrHash,
                                       Double price,
                                       Timestamp date,
                                       Database.InitializationListener<Event> listener
    ){
        Map<Integer,Object> map = createDataMap(title,posterID, facilityID, requiresLocation, description, capacity, waitlistCapacity, qrHash, price, date);

        if(!validateDataMap(map)){
            return null;
        }

        return db.createNewInstance(Database.Collections.EVENTS, facilityID + "-" +Timestamp.now().toString(), db.convertIDMapToNames(map), listener);
    }

    /**
     * Turns the properties as arguments into a map that is usable by the database
     * @param title The title of the event
     * @param posterID The id of the poster image
     * @param facilityID the id of the facility
     * @param requiresLocation If the users geolocation is recorded on signup
     * @param description the description of the event
     * @param capacity The max capacity of the event
     * @param waitlistCapacity the max waitlist capacity of the event
     * @param qrHash the cashed qr code data for the event
     * @param price the price of the event
     * @param date the date of the event
     * @return The map
     */
    public static Map<Integer, Object> createDataMap(String title,
                                                     String posterID,
                                                     String facilityID,
                                                     Boolean requiresLocation,
                                                     String description,
                                                     Integer capacity,
                                                     Integer waitlistCapacity,
                                                     String qrHash,
                                                     Double price,
                                                     Timestamp date

    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_event_title, title);
        map.put(R.string.database_event_posterID, posterID);
        map.put(R.string.database_event_facilityID, facilityID);
        map.put(R.string.database_event_geo, requiresLocation);
        map.put(R.string.database_event_description, description);
        map.put(R.string.database_event_capacity, capacity);
        map.put(R.string.database_event_waitlist, waitlistCapacity);
        map.put(R.string.database_event_qrHash, qrHash);
        map.put(R.string.database_event_date, date);
        map.put(R.string.database_event_price, price);

        return map;
    }

    /**
     * Tests if the data is valid
     * @param dataMap The data map
     * @return The
     * @see #createDataMap(String, String, String, Boolean, String, Integer, Integer, String, Double, Timestamp)  
     */
    public static boolean validateDataMap(Map<Integer, Object> dataMap){
        return DatabaseInstance.isDataValid(dataMap, fields);
    }
}
