package com.syzygy.events.database;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.AggregateQuery;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.Query;
import com.syzygy.events.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
public class Event extends DatabaseInstance<Event> implements Database.Querrier<Event>{

    /**
     * The number of users counting against enrollment. Includes Enrolled and Invited users
     */
    private int currentEnrolled = 0;
    /**
     * The number of users on the waitlist. Only counts Waitlist status
     */
    private int currentWaitlist = 0;

    private final Query waitlistQuery;
    private final Query inviteQuery;
    private final AggregateQuery waitlistCountQuery;
    private final AggregateQuery enrolledCountQuery;

    /**
     * Checks to make sure the generic type is the type of this instance
     *
     * @param db The database
     * @param eventID The id of the event
     */
    protected Event(Database db, String eventID) throws ClassCastException {
        super(db, eventID, Database.Collections.EVENTS, fields);

        Query assocUsersQuery = Database.Collections.EVENT_ASSOCIATIONS.getCollection(db).whereEqualTo(
                db.constants.getString(R.string.database_assoc_event),
                getDocumentID()
        );

        waitlistQuery = assocUsersQuery.whereEqualTo(
                db.constants.getString(R.string.database_assoc_status),
                db.constants.getString(R.string.event_assoc_status_waitlist)
        );

        inviteQuery = assocUsersQuery.whereEqualTo(
                db.constants.getString(R.string.database_assoc_status),
                db.constants.getString(R.string.event_assoc_status_invited)
        );

        Query enrolledQuery = assocUsersQuery.whereIn(
                db.constants.getString(R.string.database_assoc_status),
                Arrays.asList(
                        db.constants.getString(R.string.event_assoc_status_invited),
                        db.constants.getString(R.string.event_assoc_status_enrolled)
                )
        );
        
        waitlistCountQuery = waitlistQuery.count();
        enrolledCountQuery = enrolledQuery.count();
    }

    /**
     * Refreshes the current enrolled and waitlist counts
     * @param listener The listener that will be called when the counts are loaded
     */
    @Override
    public void refreshData(Listener<Event> listener) {
        waitlistCountQuery.get(AggregateSource.SERVER).addOnCompleteListener(task -> {
            if(!task.isSuccessful()){
                listener.onError(this);
                return;
            }
            currentWaitlist = (int) task.getResult().getCount();
            enrolledCountQuery.get(AggregateSource.SERVER).addOnCompleteListener(task2 -> {
                if(!task2.isSuccessful()){
                    listener.onError(this);
                    return;
                }
                currentEnrolled = (int) task2.getResult().getCount();
                listener.onSuccess(this);
            });
        });
    }


    /**
     * Refreshes the current enrolled and waitlist counts then queries the waitlist for a random {@code count} users.
     * Returns the selected users and the unselected users.
     * @param count The number of users to randomly select, if {@code count <= 0}, selects enough users to fill all open spots
     * @param listener The listener to be called on completion. {@link DataListener#onError(Database.Querrier)}
     *                 is called if the data refresh failed.
     *                 If there are negative empty spots to be filled, the
     *                 {@link DataListener#onSuccess(Database.Querrier, QueryResult)} with empty arrays
     */
    public void getLottery(final int count, DataListener<Event, LotteryResult> listener){
        refreshData(new Listener<Event>() {
            @Override
            public void onError(Event query) {
                listener.onError(Event.this);
            }

            @Override
            public void onSuccess(Event query) {
                int getCount = count;
                if(getCount <= 0) getCount = currentEnrolled - currentWaitlist;
                shuffleWaitlist(getCount, listener);
            }
        });
    }


    /**
     * Queries the waitlist and shuffles the result
     * @param listener Called on completion with the shuffled list
     */
    @SuppressWarnings("unchecked")
    private void shuffleWaitlist(int count, DataListener<Event, LotteryResult> listener){
        if(count < 0){
            listener.onSuccess(this, new LotteryResult(EventAssociation.QueryModifier.EMPTY(db, this), EventAssociation.QueryModifier.EMPTY(db, this), count));
            return;
        }
        DatabaseQuery<EventAssociation> waitlistUsers = new DatabaseQuery<>(db, waitlistQuery, Database.Collections.EVENT_ASSOCIATIONS, null);
        waitlistUsers.refreshData(new Listener<DatabaseQuery<EventAssociation>>() {
            @Override
            public void onError(DatabaseQuery<EventAssociation> query) {
                waitlistUsers.dissolve();
                listener.onError(Event.this);
            }

            @Override
            public void onSuccess(DatabaseQuery<EventAssociation> query) {
                List<EventAssociation> users = new ArrayList<>(query.getCurrentInstances());

                List<EventAssociation> chosen = users;
                List<EventAssociation> unChosen = new ArrayList<>();

                if(count == 0){
                    chosen = unChosen;
                    unChosen = users;
                }else if(count > users.size()){
                    Collections.shuffle(users);
                    chosen = users.subList(0,count);
                    unChosen = users.subList(count, users.size());
                }
                EventAssociation.QueryModifier<Event> c = new EventAssociation.QueryModifier<>(db, Event.this, chosen);
                EventAssociation.QueryModifier<Event> u = new EventAssociation.QueryModifier<>(db, Event.this, unChosen);
                waitlistUsers.dissolve();
                listener.onSuccess(Event.this, new LotteryResult(c,u,count));
            }
        });
    }

    public class LotteryResult extends QueryResult<EventAssociation.QueryModifier<Event>> {
        /**
         * The count of users that should have been selected
         */
        public final int count;
        public final EventAssociation.QueryModifier<Event> notChosen;
        private boolean dissolved = false;
        private boolean executed = false;

        /**
         * Assumes all instances of User have already been fetched
         * @param chosen The list of chosen users
         * @param notChosen The list of notChosen users
         * @param count The count of users that should have been selected
         */
        public LotteryResult(EventAssociation.QueryModifier<Event> chosen, EventAssociation.QueryModifier<Event> notChosen, int count) {
            super(chosen);
            this.count = count;
            this.notChosen = notChosen;
        }

        /**
         * @return {@code true} if a user was selected for all spots asked to be filled
         */
        public boolean filledAllSpots(){
            return count == result.size();
        }

        public Event getEvent(){
            return Event.this;
        }

        /**
         * Executes a lottery result.
         * Sets all selected users to Invited and notifies them of their invitation.
         * Notifies all unselected users of their rejection.
         * Dissolves the lottery result on completion
         * @param listener The listener. Only the on success method is called.
         *                 The on success method receives a {@link EventAssociation.NotificationResult} with the
         *                 successful notifications and any notifications that failed to send
         * @param notifyRejected If the rejected users should be notified
         * TODO if an update errors
         */
        public void execute(DataListener<Event, EventAssociation.NotificationResult> listener, boolean notifyRejected) throws IllegalStateException{
            if(dissolved) db.throwE(new IllegalStateException("This lottery result was cancelled"));
            if(executed) db.throwE(new IllegalStateException("This lottery result has already been executed"));
            executed = true;
            if(notifyRejected) {
                notChosen.rejectUsersFromLottery(new DataListener<Event, EventAssociation.NotificationResult>() {
                    @Override public void onError(Event query) {
                        listener.onError(query);
                        dissolve();
                    }

                    @Override public void onSuccess(Event query, EventAssociation.NotificationResult data) {
                        result.inviteUsersToEventFromLottery(new DataListener<Event, EventAssociation.NotificationResult>() {
                            @Override
                            public void onError(Event query) {
                                listener.onError(query);
                                dissolve();
                            }

                            @Override
                            public void onSuccess(Event query, EventAssociation.NotificationResult data) {
                                listener.onSuccess(query, data);
                                dissolve();
                            }
                        });
                    }
                });
            }else{
                result.inviteUsersToEventFromLottery(new DataListener<Event, EventAssociation.NotificationResult>() {
                    @Override
                    public void onError(Event query) {
                        listener.onError(query);
                        dissolve();
                    }

                    @Override
                    public void onSuccess(Event query, EventAssociation.NotificationResult data) {
                        listener.onSuccess(query, data);
                        dissolve();
                    }
                });
            }
        }

        /**
         * Cancels the result. Returns all references and does not notify users
         */
        public void dissolve(){
            if(dissolved) return;
            dissolved = true;
            result.dissolve();
            notChosen.dissolve();
        }
    }

    /**
     * Returns all invited users who have not accepted
     * @param listener The listener which is called with the list
     */
    public void getInvitedUsers(DataListener<Event, EventAssociation.QueryModifier<Event>> listener){
        DatabaseQuery<EventAssociation> inviteUsers = new DatabaseQuery<>(db, inviteQuery, Database.Collections.EVENT_ASSOCIATIONS, null);
        inviteUsers.refreshData(new Listener<DatabaseQuery<EventAssociation>>() {
            @Override
            public void onError(DatabaseQuery<EventAssociation> query) {
                inviteUsers.dissolve();
                listener.onError(Event.this);
            }

            @Override
            public void onSuccess(DatabaseQuery<EventAssociation> query) {
                List<EventAssociation> users = new ArrayList<>(query.getCurrentInstances());
                EventAssociation.QueryModifier<Event> q = new EventAssociation.QueryModifier<>(db, Event.this, users);
                inviteUsers.dissolve();
                listener.onSuccess(Event.this, q);
            }
        });
    }

    /**
     * Cancels all invited users who have not accepted. Users are set to Cancelled and then notified
     * @param listener The listener which is called with the list
     */
    public void cancelAllInvitedUsers(DataListener<Event, EventAssociation.NotificationResult> listener){
        getInvitedUsers(new DataListener<Event, EventAssociation.QueryModifier<Event>>() {
            @Override
            public void onError(Event query) {
                listener.onError(query);
            }

            @Override
            public void onSuccess(Event query, EventAssociation.QueryModifier<Event> data) {
                data.cancelUsers(new DataListener<Event, EventAssociation.NotificationResult>() {
                    @Override
                    public void onError(Event query) {
                        listener.onError(query);
                        data.dissolve();
                    }

                    @Override
                    public void onSuccess(Event query, EventAssociation.NotificationResult data2) {
                        listener.onSuccess(query, data2);
                        data.dissolve();
                    }
                });
            }
        });
    }


    /**
     * @return The number of users that count against enrollment. Includes Enrolled and Invited
     * @see #refreshData(Listener)
     */
    public int getCurrentEnrolled() {
        return currentEnrolled;
    }

    /**
     * @return The number of users that count against the waitlist. Includes Waitlist
     * @see #refreshData(Listener)
     */
    public int getCurrentWaitlist() {
        return currentWaitlist;
    }

    @Override
    protected void subDereferenceInstance() {
        super.subDereferenceInstance();
    }

    @Override
    protected Event cast() {
        return this;
    }

    public String getTitle(){
        return getPropertyValueI(R.string.database_event_title);
    }

    public boolean setTitle(String val){
        return setPropertyValue(R.string.database_event_title, val);
    }

    public String getPosterID(){
        return getPropertyValueI(R.string.database_event_posterID);
    }

    public boolean setPosterID(String val){
        return setPropertyValue(R.string.database_event_posterID, val);
    }

    public String getFacilityID(){
        return getPropertyValueI(R.string.database_event_facilityID);
    }

    public Boolean getRequiresLocation(){
        return getPropertyValueI(R.string.database_event_geo);
    }

    public String getDescription(){
        return getPropertyValueI(R.string.database_event_description);
    }

    public boolean setDescription(String val){
        return setPropertyValue(R.string.database_event_description, val);
    }

    public Integer getCapacity(){
        return getPropertyValueI(R.string.database_event_capacity);
    }

    public Integer getWaitlistCapacity(){
        return getPropertyValueI(R.string.database_event_waitlist);
    }

    public String getQrHash(){
        return getPropertyValueI(R.string.database_event_qrHash);
    }

    public boolean setQrHash(String val){
        return setPropertyValue(R.string.database_event_qrHash, val);
    }
    
    public Timestamp getCreatedDate(){
        return getPropertyValueI(R.string.database_event_createdTime);
    }

    public Image getPoster(){
        return getPropertyInstanceI(R.string.database_event_posterID);
    }


    /**
     * Sets the Image instance. This function will create a new reference to the instance.
     * @param val The new instance
     */
    public boolean setPoster(@Nullable Image val){
        return setPropertyInstance(R.string.database_event_posterID, val);
    }

    public Facility getFacility(){
        return getPropertyInstanceI(R.string.database_event_facilityID);
    }

    /**
     * Updates all properties of the event. If the event changes, a notification is sent to listeners once and the database is updated once
     * @param title The title of the event
     * @param posterID The id of the poster image
     * @param description the description of the event
     * @param qrHash the cashed qr code data for the event
     * @return If the event changed as a result
     */
    public boolean update(String title,
                          String posterID,
                          String description,
                          String qrHash
    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_event_title, title);
        map.put(R.string.database_event_posterID, posterID);
        map.put(R.string.database_event_description, description);
        map.put(R.string.database_event_qrHash, qrHash);

        return updateDataFromMap(db.convertIDMapToNames(map));
    }

    /**
     * The list of the fields defined for a User
     */
    private static final PropertyField<?, ?>[] fields = {
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_event_title, o -> o instanceof String && !((String) o).isBlank(), true),
            new PropertyField<String, Image>(R.string.database_event_posterID, o -> o instanceof String, true, true, Database.Collections.IMAGES, true),
            new PropertyField<String, Facility>(R.string.database_event_facilityID, o -> o instanceof String && !((String) o).isBlank(), false, true, Database.Collections.FACILITIES, false),
            new PropertyField<Boolean, PropertyField.NullInstance>(R.string.database_event_geo, o -> o instanceof Boolean, false),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_event_description, o -> o instanceof String && !((String) o).isBlank(), true),
            new PropertyField<Integer, PropertyField.NullInstance>(R.string.database_event_capacity, o -> o instanceof Integer && (Integer)o > 0, false),
            new PropertyField<Integer, PropertyField.NullInstance>(R.string.database_event_waitlist, o -> o instanceof Integer && ((Integer)o > 0 || (Integer)o == -1), false),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_event_qrHash, o -> o instanceof String, true),
            new PropertyField<Timestamp, PropertyField.NullInstance>(R.string.database_event_createdTime, o -> o instanceof Timestamp, false)
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
                                       Database.InitializationListener<Event> listener
    ){
        Timestamp now = Timestamp.now();
        Map<Integer,Object> map = createDataMap(title,posterID, facilityID, requiresLocation, description, capacity, waitlistCapacity, qrHash, now);

        if(!validateDataMap(map)){
            return null;
        }

        return db.createNewInstance(Database.Collections.EVENTS, facilityID + "-" + now.toString(), db.convertIDMapToNames(map), listener);
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
     * @param createdTime The time when the event was created
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
                                                     Timestamp createdTime

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
        map.put(R.string.database_event_createdTime, createdTime);

        return map;
    }

    /**
     * Tests if the data is valid
     * @param dataMap The data map
     * @return The
     * @see #createDataMap(String, String, String, Boolean, String, Integer, Integer, String, Timestamp)
     */
    public static boolean validateDataMap(Map<Integer, Object> dataMap){
        return DatabaseInstance.isDataValid(dataMap, fields);
    }
}
