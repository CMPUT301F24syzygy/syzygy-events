package com.syzygy.events.database;

import android.util.Pair;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * An instance of an association of a user to an event
 */
@Database.Dissolves
public class EventAssociation extends DatabaseInstance<EventAssociation>{
    /**
     * Checks to make sure the generic type is the type of this instance
     *
     * @param db The database
     * @param assocID The id of the association
     */
    @Database.Salty
    protected EventAssociation(Database db, String assocID) throws ClassCastException {
        super(db, assocID, Database.Collections.EVENT_ASSOCIATIONS, fields);
    }

    @Override
    @Database.Observes
    protected EventAssociation cast() {
        return this;
    }

    public GeoPoint getLocation(){
        return getPropertyValueI(R.string.database_assoc_geo);
    }

    public boolean setLocation(GeoPoint val){
        return setPropertyValue(R.string.database_assoc_geo, val, s -> {});
    }

    public String getEventID(){
        return getPropertyValueI(R.string.database_assoc_event);
    }

    public String getStatus(){
        return getPropertyValueI(R.string.database_assoc_status);
    }

    public boolean setStatus(String val){
        return setPropertyValue(R.string.database_assoc_status, val, s -> {});
    }

    public boolean setStatus(int resID){
        return setPropertyValue(R.string.database_assoc_status, db.constants.getString(resID), s -> {});
    }

    public String getUserID(){
        return getPropertyValueI(R.string.database_assoc_user);
    }

    @Database.Observes
    public User getUser(){
        return getPropertyInstanceI(R.string.database_assoc_user);
    }

    @Database.Observes
    public Event getEvent(){
        return getPropertyInstanceI(R.string.database_assoc_event);
    }


    public Timestamp getJoinTime(){
        return getPropertyValueI(R.string.database_assoc_time);
    }

    /**
     * Validates and updates all properties of the assoc. If the assoc changes, a notification is sent to listeners once and the database is updated once
     * @param location The location where the user signed into the event
     * @param status The status of the association
     * @return The ids of all invalid properties
     * @see #updateDataFromMap(Map, Consumer)
     */
    public Set<Integer> update(GeoPoint location, String status){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_assoc_geo, location);
        map.put(R.string.database_assoc_status, status);
        return updateDataFromMap(map, s->{});
    }

    /**
     * The list of the fields defined for a User
     */
    static final PropertyField<?, ?>[] fields = {
            new PropertyField<String, User>(R.string.database_assoc_user, o -> o instanceof String && !((String) o).isBlank(), true, true, Database.Collections.USERS, false, false),
            new PropertyField<String, Event>(R.string.database_assoc_event, o -> o instanceof String && !((String) o).isBlank(), true, true, Database.Collections.EVENTS, false, false),
            new PropertyField<GeoPoint, PropertyField.NullInstance>(R.string.database_assoc_geo, o -> o instanceof GeoPoint || o == null, true),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_assoc_status, o -> o instanceof String && !((String) o).isBlank(), true),
            new PropertyField<Timestamp, PropertyField.NullInstance>(R.string.database_assoc_time, o -> o instanceof Timestamp, false)
    };

    @Override
    protected List<Pair<Query, Database.Collections>> subInstanceCascadeDeleteQuery() {
        return Collections.emptyList();
    }

    /**
     * Validates and creates a new Image instance in the database using the given data.
     * @param db The database
     * @param eventID The ID of the event
     * @param location The location where the user signed into the event
     * @param status The status of the association
     * @param userID The id of the user
     * @param listener Called once the instance is initialized. Not called if properties are invalid
     * @return The property id of all invalid properties
     * @see Database#createNewInstance(Database.Collections, String, Map, Database.InitializationListener)
     */
    @Database.MustStir
    public static Set<Integer> NewInstance(Database db,
                                    @Database.Dilutes String eventID,
                                    GeoPoint location,
                                    String status,
                                    @Database.Dilutes String userID,
                                    Database.InitializationListener<EventAssociation> listener
    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_assoc_geo, location);
        map.put(R.string.database_assoc_event, eventID);
        map.put(R.string.database_assoc_status, status);
        map.put(R.string.database_assoc_user, userID);
        map.put(R.string.database_assoc_time, Timestamp.now());

        String id = Database.Collections.EVENT_ASSOCIATIONS.getNewID(db);

        return db.createNewInstance(Database.Collections.EVENT_ASSOCIATIONS, id, map, listener);
    }

    /**
     * A query result of EventAssociations. This provides a methods to mass modify and notify user
     */
    @Database.Dissolves
    public static class Methods<T extends Database.Querrier<T>> extends Database.Querrier.QueryInstanceResult<EventAssociation> implements Database.Dissolvable {

        private boolean dissolved = false;
        private final Database db;
        private final T querrier;

        /**
         * Fetches all instances in the list and stores them in a unmodifiable reference
         * @param db The database
         * @param querrier The querrier that got the results
         * @param list The list of results
         */
        @Database.MustStir
        public Methods(Database db, @Database.Observes T querrier, @Database.Dilutes List<EventAssociation> list) {
            super(list);
            this.querrier = querrier;
            result.forEach(DatabaseInstance::fetch);
            this.db = db;
        }

        /**
         * Sets the status of each association
         * @param statusID The resId of the new status
         */
        public void setStatus(int statusID){
            if(dissolved) {
                db.throwE(new IllegalStateException("Invalid list"));
                return;
            }
            String status = db.constants.getString(statusID);
            result.forEach(e -> e.setStatus(status));
        }

        /**
         * Invites all users to the corresponding events. Sets the association to Invited and notifies the user that they were selected by the lottery
         * @param listener The listener called on completion with the {@link NotificationResult}
         */
        @Database.Stirred
        public void inviteUsersToEventFromLottery(Database.Querrier.DataListener<T, NotificationResult> listener){
            setStatus(R.string.event_assoc_status_invited,
                    db.constants.getString(R.string.notification_lottery_chosen_subject),
                    db.constants.getString(R.string.notification_lottery_chosen_body),
                    true, true, listener
            );
        }
        /**
         * Notifies the users that they were rejected by the lottery for their associated event
         * @param listener The listener called on completion with the {@link NotificationResult}
         */
        @Database.Stirred
        public void rejectUsersFromLottery(Database.Querrier.DataListener<T, NotificationResult> listener){
            notify(db.constants.getString(R.string.notification_lottery_notChosen_subject),
                    db.constants.getString(R.string.notification_lottery_notChosen_body),
                    true, false, true, listener
            );
        }

        /**
         * Cancels all users from the respective events. Sets the association to Cancelled and notifies the users that they were removed from the event
         * @param listener The listener called on completion with the {@link NotificationResult}
         */
        @Database.Stirred
        public void cancelUsers(Database.Querrier.DataListener<T, NotificationResult> listener){
            setStatus(R.string.event_assoc_status_cancelled,
                    db.constants.getString(R.string.notification_cancelled_subject),
                    db.constants.getString(R.string.notification_cancelled_body),
                    true, false, listener
            );
        }

        /**
         * Deletes all associations. Then dissolves self
         */
        @Database.StirsDeep(what="Deletes the result instances")
        @Database.AutoStir
        public void delete(){
            result.forEach(i -> i.deleteInstance(DeletionType.HARD_DELETE, success -> {}));
            dissolve();
        }

        /**
         * Sets the status of each association and notifies the user
         * @param statusID The resId of the new status
         * @param notificationSubject The subject of the notification
         * @param notificationBody The body of the notification
         * @param notificationAttachEvent If the event should be attached to the notification
         * @param notificationFromOrganizer If the even should be sent from the organizer
         * @param listener The listener that is called with the notification result upon completion.
         *                 Only the {@code onSuccess} is called
         * @see NotificationResult
         */
        @Database.Stirred
        public void setStatus(int statusID, String notificationSubject, String notificationBody, boolean notificationAttachEvent, boolean notificationFromOrganizer, Database.Querrier.DataListener<T, NotificationResult> listener){
            if(dissolved) {
                db.throwE(new IllegalStateException("Invalid list"));
                listener.onCompletion(null, null, false);
                return;
            }
            String status = db.constants.getString(statusID);
            notify(
                    e -> e.setStatus(status),
                    notificationSubject,
                    notificationBody,
                    notificationAttachEvent,
                    notificationFromOrganizer,
                    true,
                    listener
            );
        }

        /**
         * Sends a notification to all users
         * @param subject The subject of the notification
         * @param body The body of the notification
         * @param attachEvent If the event should be attached to the notification
         * @param fromOrganizer If the even should be sent from the organizer
         * @param ignoresOptOut If the notification should ignore opt out settings
         * @param listener The listener that is called with the notification result upon completion.
         *                 Only the {@code onSuccess} is called
         * @see NotificationResult
         */
        @Database.Stirred
        public void notify(String subject, String body, boolean attachEvent, boolean fromOrganizer, boolean ignoresOptOut, Database.Querrier.DataListener<T, NotificationResult> listener){
            if(dissolved) {
                db.throwE(new IllegalStateException("Invalid list"));
                listener.onCompletion(null, null, false);
                return;
            }
            notify(e -> {}, subject, body, attachEvent, fromOrganizer, ignoresOptOut, listener);
        }

        /**
         * Applies the consumer to each association and notifies the users
         * @param subject The subject of the notification
         * @param body The body of the notification
         * @param attachEvent If the event should be attached to the notification
         * @param fromOrganizer If the notification should be sent from the organizer
         * @param ignoresOptOut If the notification should ignore opt out settings
         * @param listener The listener that is called with the notification result upon completion.
         *                 Only the {@code onSuccess} is called. Ownership is passed on to the caller
         * @see NotificationResult
         */
        @Database.Stirred
        public void notify(@Database.Observes Consumer<EventAssociation> consumer, String subject, String body, boolean attachEvent, boolean fromOrganizer, boolean ignoresOptOut, Database.Querrier.DataListener<T, NotificationResult> listener){
            if(dissolved) {
                db.throwE(new IllegalStateException("Invalid list"));
                listener.onCompletion(null, null, false);
                return;
            }
            List<Notification> failedNotifications = new ArrayList<>();
            List<Notification> successNotifications = new ArrayList<>();

            Database.InitializationListener<Notification> asyncForLoop = new Database.InitializationListener<Notification>() {
                private int i = -1;
                @Override
                public void onInitialization( Notification instance, boolean success) {
                    if(!success){
                        failedNotifications.add(instance);
                    }else{
                        if(i >= 0) successNotifications.add(instance);
                    }
                    ++i;
                    if(i >= result.size()){
                        NotificationResult n = new NotificationResult(successNotifications, failedNotifications);
                        listener.onCompletion(querrier, n, true);
                        n.dissolve();
                        return;
                    }


                    EventAssociation e = result.get(i);
                    consumer.accept(e);

                    //Send notification
                    Notification.NewInstance(db,
                            subject,
                            body,
                            attachEvent ? e.getEventID() : "",
                            e.getUserID(),
                            fromOrganizer ? e.getEvent().getFacility().getOrganizerID() : SyzygyApplication.SYSTEM_ACCOUNT_ID,
                            ignoresOptOut,
                            this
                    );
                }
            };

            asyncForLoop.onInitialization(null, true);
        }

        public int size() {
            return result.size();
        }

        /**
         * Returns the references
         */
        @Database.AutoStir
        @Database.StirsDeep(what="The result instances")
        public void dissolve(){
            if(dissolved) return;
            dissolved = true;
            result.forEach(DatabaseInstance::dissolve);
        }


        @Database.MustStir
        public static <T extends Database.Querrier<T>> Methods<T> EMPTY(Database db, @Database.Observes T q){
            return new Methods<>(db, q, new ArrayList<>());
        }

        @Database.MustStir
        public static <T extends Database.Querrier<T>> Methods<T> SINGLETON(Database db, @Database.Observes T q, @Database.Dilutes EventAssociation assoc){
            return new Methods<>(db, q, Collections.singletonList(assoc));
        }
    }

    /**
     * Returns an object which contains methods that help notify the user or change the status of the association.
     * This object must be called with {@code .dissolve} once complete
     * @return The methods object
     */
    @Database.MustStir
    @Database.Titrates(what="This")
    public Methods<Event> methods(){
        return Methods.SINGLETON(db, getEvent(), this);
    }

    /**
     * Stores the result of a mass notification.
     * <p>
     *     Stores all sent notifications as the {@code result}.
     * </p>
     */
    @Database.Dissolves
    public static class NotificationResult extends Database.Querrier.QueryInstanceResult<Notification> implements Database.Dissolvable {

        /**
         * All notifications that failed to send
         */
        public final List<Notification> failedNotifications;

        @Database.MustStir
        public NotificationResult(@Database.Stirs List<Notification> list, @Database.Stirs List<Notification> failedNotifications) {
            super(list);
            this.failedNotifications = Collections.unmodifiableList(failedNotifications);
        }

        @Database.AutoStir
        @Database.StirsDeep(what = "The result and failure notifications")
        public void dissolve(){
            result.forEach(DatabaseInstance::dissolve);
            failedNotifications.forEach(DatabaseInstance::dissolve);
        }
    }
}
