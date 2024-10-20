package com.syzygy.events.database;

import com.google.firebase.Timestamp;
import com.syzygy.events.R;

import java.util.HashMap;
import java.util.Map;

/**
 * An instance of a notification database item
 * - A notification item cannot be edited
 * @author Gareth Kmet
 * @version 1.0
 * @since 19oct24
 */
public class Notification extends DatabaseInstance<Notification> {

    /**
     * Checks to make sure the generic type is the type of this instance
     *
     * @param db The database
     * @param id The id of the notification
     */
    protected Notification(Database db, String id) throws ClassCastException {
        super(db, id, Database.Collections.FACILITIES, fields);
    }

    @Override
    protected Notification cast() {
        return this;
    }

    /**
     * The list of the fields defined for a User
     */
    private static final PropertyField<?, ?>[] fields = {
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_not_subject, o -> o instanceof String && !((String) o).isBlank(), false),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_not_body, o -> o instanceof String && !((String) o).isBlank(), false),
            new PropertyField<Timestamp, PropertyField.NullInstance>(R.string.database_not_time, o -> o instanceof Timestamp, false),
            new PropertyField<Boolean, PropertyField.NullInstance>(R.string.database_not_read, o -> o instanceof Boolean, false),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_not_eventID, o -> o instanceof String && !((String) o).isBlank(), false, true, Database.Collections.EVENTS, true),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_not_receiverID, o -> o instanceof String && !((String) o).isBlank(), false, true, Database.Collections.USERS, false),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_not_senderID, o -> o instanceof String && !((String) o).isBlank(), false, true, Database.Collections.USERS, false)
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
     * @param subject The subject of the notification
     * @param body The body of the notification
     * @param sentTime The time the notification was sent
     * @param isRead If the notification is read
     * @param eventID The ID of the event associated to the notification
     * @param receiverID The id of the receiver
     * @param senderID The id of the sender
     * @return The user instance in an illegal state
     * @see Database#createNewInstance(Database.Collections, String, Map, Database.InitializationListener)
     */
    public static Notification NewInstance(Database db,
                                       String subject,
                                       String  body,
                                       Timestamp sentTime,
                                       Boolean isRead,
                                       String eventID, 
                                       String receiverID, 
                                       String senderID,
                                       Database.InitializationListener<Notification> listener
    ){
        Map<Integer,Object> map = createDataMap(subject, body, sentTime, isRead, eventID, receiverID, senderID);

        if(!validateDataMap(map)){
            return null;
        }

        return db.createNewInstance(Database.Collections.NOTIFICATIONS, senderID + "-"+receiverID+"-"+sentTime.toString(), db.convertIDMapToNames(map), listener);
    }

    /**
     * Turns the properties as arguments into a map that is usable by the database
     * @param subject The subject of the notification
     * @param body The body of the notification
     * @param sentTime The time the notification was sent
     * @param isRead If the notification is read
     * @param eventID The ID of the event associated to the notification
     * @param receiverID The id of the receiver
     * @param senderID The id of the sender
     * @return The map
     */
    public static Map<Integer, Object> createDataMap(String subject,
                                                     String  body,
                                                     Timestamp sentTime,
                                                     Boolean isRead,
                                                     String eventID,
                                                     String receiverID,
                                                     String senderID

    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_not_subject, subject);
        map.put(R.string.database_not_body, body);
        map.put(R.string.database_not_time, sentTime);
        map.put(R.string.database_not_read, isRead);
        map.put(R.string.database_not_eventID, eventID);
        map.put(R.string.database_not_senderID, senderID);
        map.put(R.string.database_not_receiverID, receiverID);
        return map;
    }

    /**
     * Tests if the data is valid
     * @param dataMap The data map
     * @return The
     * @see #createDataMap(String, String, Timestamp, Boolean, String, String, String) 
     */
    public static boolean validateDataMap(Map<Integer, Object> dataMap){
        return DatabaseInstance.isDataValid(dataMap, fields);
    }
}
