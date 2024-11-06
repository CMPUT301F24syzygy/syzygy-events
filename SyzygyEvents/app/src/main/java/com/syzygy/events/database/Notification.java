package com.syzygy.events.database;

import android.util.Pair;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Query;
import com.syzygy.events.R;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An instance of a notification database item
 * - A notification item cannot be edited
 * @author Gareth Kmet
 * @version 1.0
 * @since 19oct24
 */
@Database.Dissovable
public class Notification extends DatabaseInstance<Notification> {



    /**
     * Checks to make sure the generic type is the type of this instance
     *
     * @param db The database
     * @param id The id of the notification
     */
    @Database.Salty
    protected Notification(Database db, String id) throws ClassCastException {
        super(db, id, Database.Collections.NOTIFICATIONS, fields);
    }

    @Override
    @Database.Observes
    protected Notification cast() {
        return this;
    }

    public String getSubject(){
        return getPropertyValueI(R.string.database_not_subject);
    }

    public String getBody(){
        return getPropertyValueI(R.string.database_not_body);
    }

    public Timestamp getSentTime(){
        return getPropertyValueI(R.string.database_not_time);
    }

    public Boolean getIsRead(){
        return getPropertyValueI(R.string.database_not_read);
    }


    public String getEventID(){
        return getPropertyValueI(R.string.database_not_eventID);
    }


    public String getSenderID(){
        return getPropertyValueI(R.string.database_not_senderID);
    }


    public String getReceiverID(){
        return getPropertyValueI(R.string.database_not_receiverID);
    }

    @Database.Observes
    public Event getEvent(){
        return getPropertyInstanceI(R.string.database_not_eventID);
    }
    @Database.Observes
    public User getSender(){
        return getPropertyInstanceI(R.string.database_not_senderID);
    }
    @Database.Observes
    public User getReceiver(){
        return getPropertyInstanceI(R.string.database_not_receiverID);
    }

    public Boolean setIsRead(Boolean b) {
        return setPropertyValue(R.string.database_not_read, b, s -> {});
    }

    /**
     * The list of the fields defined for a User
     */
    static final PropertyField<?, ?>[] fields = {
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_not_subject, o -> o instanceof String && !((String) o).isBlank(), false),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_not_body, o -> o instanceof String && !((String) o).isBlank(), false),
            new PropertyField<Timestamp, PropertyField.NullInstance>(R.string.database_not_time, o -> o instanceof Timestamp, false),
            new PropertyField<Boolean, PropertyField.NullInstance>(R.string.database_not_read, o -> o instanceof Boolean, true),
            new PropertyField<String, Event>(R.string.database_not_eventID, o -> o instanceof String && !((String) o).isBlank(), false, true, Database.Collections.EVENTS, true, false),
            new PropertyField<String, User>(R.string.database_not_receiverID, o -> o instanceof String && !((String) o).isBlank(), false, true, Database.Collections.USERS, false, false),
            new PropertyField<String, User>(R.string.database_not_senderID, o -> o instanceof String && !((String) o).isBlank(), false, true, Database.Collections.USERS, true, false)
    };

    @Override
    protected List<Pair<Query, Database.Collections>> subInstanceCascadeDeleteQuery() {
        return Collections.emptyList();
    }

    /**
     * Validates and creates a new notification instance in the database using the given data.
     * <p>
     *     Sets {@code isRead} to {@code false}
     * </p>
     * @param db The database
     * @param subject The subject of the notification
     * @param body The body of the notification
     * @param eventID The ID of the event associated to the notification
     * @param receiverID The id of the receiver
     * @param senderID The id of the sender
     * @param listener Will be called once the notification is initialized. Is not called if the data is invalid
     * @return The property id of all invalid properties
     * @see Database#createNewInstance(Database.Collections, String, Map, Database.InitializationListener)
     */
    @Database.MustStir
    public static Set<Integer> NewInstance(Database db,
                                       String subject,
                                       String body,
                                       @Database.Dilutes String eventID,
                                       @Database.Dilutes String receiverID,
                                       @Database.Dilutes String senderID,
                                       Database.InitializationListener<Notification> listener
    ){
        Timestamp sentTime = Timestamp.now();

        String id = Database.Collections.NOTIFICATIONS.getNewID(db);

        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_not_subject, subject);
        map.put(R.string.database_not_body, body);
        map.put(R.string.database_not_time, sentTime);
        map.put(R.string.database_not_read, false);
        map.put(R.string.database_not_eventID, eventID);
        map.put(R.string.database_not_senderID, senderID);
        map.put(R.string.database_not_receiverID, receiverID);

        return db.createNewInstance(Database.Collections.NOTIFICATIONS, id, map, listener);
    }
}
