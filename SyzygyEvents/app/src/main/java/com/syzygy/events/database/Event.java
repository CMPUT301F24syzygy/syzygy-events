package com.syzygy.events.database;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.AggregateQuery;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.syzygy.events.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * An instance of a event database item
 * - An event can be edited
 * @author Gareth Kmet
 * @version 1.0
 * @since 19oct24
 */
@Database.Dissovable
public class Event extends DatabaseInstance<Event> implements Database.Querrier<Event>{

    /**
     * The number of users counting against enrollment. Includes Enrolled and Invited users
     */
    private int currentEnrolled = 0;
    /**
     * The number of users on the waitlist. Only counts Waitlist status
     */
    private int currentWaitlist = 0;

    private final Query assocUsersQuery;

    /**
     * Checks to make sure the generic type is the type of this instance
     *
     * @param db The database
     * @param eventID The id of the event
     */
    @Database.Salty
    protected Event(Database db, String eventID) throws ClassCastException {
        super(db, eventID, Database.Collections.EVENTS, fields);

        assocUsersQuery = Database.Collections.EVENT_ASSOCIATIONS.getCollection(db).whereEqualTo(
                db.constants.getString(R.string.database_assoc_event),
                getDocumentID()
        );
    }

    /**
     * Refreshes the current enrolled and waitlist counts
     * @param listener The listener that will be called when the counts are loaded
     */
    @Override
    public void refreshData(Listener<Event> listener) {
        AggregateQuery waitlistCountQuery = assocUsersQuery.whereEqualTo(
                db.constants.getString(R.string.database_assoc_status),
                db.constants.getString(R.string.event_assoc_status_waitlist)
        ).count();
        AggregateQuery enrolledInvitedCountQuery = assocUsersQuery.whereIn(
                db.constants.getString(R.string.database_assoc_status),
                Arrays.asList(
                        db.constants.getString(R.string.event_assoc_status_invited),
                        db.constants.getString(R.string.event_assoc_status_enrolled)
                )
        ).count();
        waitlistCountQuery.get(AggregateSource.SERVER).addOnCompleteListener(task -> {
            if(!task.isSuccessful()){
                listener.onCompletion(this, false);
                return;
            }
            currentWaitlist = (int) task.getResult().getCount();
            enrolledInvitedCountQuery.get(AggregateSource.SERVER).addOnCompleteListener(task2 -> {
                if(!task2.isSuccessful()){
                    listener.onCompletion(this, false);
                    return;
                }
                currentEnrolled = (int) task2.getResult().getCount();
                listener.onCompletion(this, true);
            });
        });
    }


    /**
     * Refreshes the current enrolled and waitlist counts then queries the waitlist for a random {@code count} users.
     * Returns the selected users and the unselected users.
     * @param count The number of users to randomly select, if {@code count <= 0}, selects enough users to fill all open spots
     * @param listener The listener to be called on completion. {@link DataListener#onCompletion(Database.Querrier, QueryResult, boolean)}
     *                 is called with {@code false} if the data refresh failed.
     *                 If there are negative empty spots to be filled, {@code true} is returned with empty arrays
     */
    @Database.MustStir
    public void getLottery(final int count, DataListener<Event, LotteryResult> listener){
        refreshData((query, success) -> {
            if(!success){
                listener.onCompletion(query, null, false);
                return;
            }
            int getCount = count;
            if(getCount <= 0) getCount = currentEnrolled - currentWaitlist;
            shuffleWaitlist(getCount, listener);
        });
    }


    /**
     * Queries the waitlist and shuffles the result
     * @param listener Called on completion with the shuffled list
     */
    @Database.MustStir
    private void shuffleWaitlist(int count, DataListener<Event, LotteryResult> listener){
        if(count < 0){
            listener.onCompletion(this, new LotteryResult(EventAssociation.QueryModifier.EMPTY(db, this), EventAssociation.QueryModifier.EMPTY(db, this), count), true);
            return;
        }
        getWaitlistUsers((query, data, success) -> {
            if(!success){
                listener.onCompletion(query, null, false);
                return;
            }
            List<EventAssociation> users = new ArrayList<>(data.result);

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
            listener.onCompletion(query, new LotteryResult(c,u,count), true);
        });
    }

    /**
     * The random selected list user and unselected users returned by a lottery call along with a method to execute the lottery.
     * This acts as a confirmation stage for the lottery.
     */
    @Database.Dissovable
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
        public LotteryResult(@Database.Stirs EventAssociation.QueryModifier<Event> chosen, @Database.Stirs EventAssociation.QueryModifier<Event> notChosen, int count) {
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

        @Database.Observes
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
        @Database.Stirred
        @Database.AutoStir
        public void execute(DataListener<Event, EventAssociation.NotificationResult> listener, boolean notifyRejected) throws IllegalStateException{
            if(dissolved) db.throwE(new IllegalStateException("This lottery result was cancelled"));
            if(executed) db.throwE(new IllegalStateException("This lottery result has already been executed"));
            executed = true;
            if(notifyRejected) {
                notChosen.rejectUsersFromLottery((query, data, success) -> {
                    if(!success){
                        listener.onCompletion(query, data, false);
                        if(data != null)data.dissolve();
                        dissolve();
                        return;
                    }
                    result.inviteUsersToEventFromLottery((query1, data1, success2) -> {
                        listener.onCompletion(query1, data1, success2);
                        if(data1!=null)data1.dissolve();
                        dissolve();
                    });
                });
            }else{
                result.inviteUsersToEventFromLottery((query, data, success) -> {
                    listener.onCompletion(query, data, success);
                    if(data!=null)data.dissolve();
                    dissolve();
                });
            }
        }

        /**
         * Cancels the result. Returns all references and does not notify users
         */
        @Database.AutoStir
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
    @Database.MustStir
    public void getInvitedUsers(DataListener<Event, EventAssociation.QueryModifier<Event>> listener){
        getUsersByStatus(R.string.event_assoc_status_invited, listener);
    }

    /**
     * Returns all enrolled users
     * @param listener The listener which is called with the list
     */
    @Database.MustStir
    public void getEnrolledUsers(DataListener<Event, EventAssociation.QueryModifier<Event>> listener){
        getUsersByStatus(R.string.event_assoc_status_enrolled, listener);
    }

    /**
     * Returns all cancelled users
     * @param listener The listener which is called with the list
     */
    @Database.MustStir
    public void getCancelledUsers(DataListener<Event, EventAssociation.QueryModifier<Event>> listener){
        getUsersByStatus(R.string.event_assoc_status_cancelled, listener);
    }

    /**
     * Returns all waitlist users
     * @param listener The listener which is called with the list
     */
    @Database.MustStir
    public void getWaitlistUsers(DataListener<Event, EventAssociation.QueryModifier<Event>> listener){
        getUsersByStatus(R.string.event_assoc_status_waitlist, listener);
    }

    /**
     * Returns all associated users with the given association status
     * @param statusID The res ID of the status
     * @param listener The listener which is called on completion
     */
    @Database.MustStir
    public void getUsersByStatus(int statusID, DataListener<Event, EventAssociation.QueryModifier<Event>> listener){
        Query query = assocUsersQuery.whereEqualTo(
                db.constants.getString(R.string.database_assoc_status),
                db.constants.getString(statusID)
        );
        getAssociatedUsersFromQuery(query, listener);
    }

    /**
     * Gets the {@link EventAssociation.QueryModifier} from a {@link Query}.
     * @param query The query to evaluate
     * @param listener The listener that gets called on completion
     * @see DatabaseQuery#refreshData(Listener)
     */
    @Database.MustStir
    public void getAssociatedUsersFromQuery(Query query, DataListener<Event, EventAssociation.QueryModifier<Event>> listener){
        getAssociatedUsersFromQuery(new DatabaseQuery<>(db, query, Database.Collections.EVENT_ASSOCIATIONS, null), listener);
    }

    /**
     * Gets the {@link EventAssociation.QueryModifier} from a {@link DatabaseQuery}.
     * Dissolves the {@code DatabaseQuery} on completion
     * @param query The query to evaluate
     * @param listener The listener that gets called on completion
     * @see DatabaseQuery#refreshData(Listener)
     */
    @Database.MustStir
    public void getAssociatedUsersFromQuery(@Database.Stirs DatabaseQuery<EventAssociation> query, DataListener<Event, EventAssociation.QueryModifier<Event>> listener){
        query.refreshData((query2, success) -> {
            if(!success){
                listener.onCompletion(Event.this, null, false);
            }else{
                List<EventAssociation> users = new ArrayList<>(query.getCurrentInstances());
                EventAssociation.QueryModifier<Event> q = new EventAssociation.QueryModifier<>(db, Event.this, users);
                listener.onCompletion(Event.this, q, true);
            }
            query.dissolve();
        });
    }

    /**
     * Cancels all invited users who have not accepted. Users are set to Cancelled and then notified
     * @param listener The listener which is called with the list
     */
    @Database.Stirred
    public void cancelAllInvitedUsers(DataListener<Event, EventAssociation.NotificationResult> listener){
        getInvitedUsers((query, data, success) -> {
            if(!success){
                listener.onCompletion(query, null, false);
                if(data!=null)data.dissolve();
            }
            assert data != null;
            data.cancelUsers((query1, data2, success2) -> {
                listener.onCompletion(query1, data2, false);
                data2.dissolve();
                data.dissolve();
            });
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
    @Database.Observes
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

    @Database.StirsDeep(what = "The previous image")
    public boolean setPosterID(@Database.Dilutes String val){
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

    @Database.Observes
    public Image getPoster(){
        return getPropertyInstanceI(R.string.database_event_posterID);
    }


    /**
     * Sets the Image instance. This function will create a new reference to the instance.
     * @param val The new instance
     */
    @Database.StirsDeep(what = "The previous image")
    public boolean setPoster(@Nullable @Database.Dilutes Image val){
        return setPropertyInstance(R.string.database_event_posterID, val);
    }

    @Database.Observes
    public Facility getFacility(){
        return getPropertyInstanceI(R.string.database_event_facilityID);
    }

    public Timestamp getOpenRegistrationDate(){
        return getPropertyValueI(R.string.database_event_openDate);
    }

    public Timestamp getCloseRegistrationDate(){
        return getPropertyValueI(R.string.database_event_closedDate);
    }

    public List<Timestamp> getEventDates(){
        return getPropertyValueI(R.string.database_event_dates);
    }

    public boolean setEventDates(List<Timestamp> val){
        return setPropertyValue(R.string.database_event_dates, val);
    }

    public Double getPrice(){
        return getPropertyValueI(R.string.database_event_price);
    }

    public boolean setPrice(Double val){
        return setPropertyValue(R.string.database_event_price, val);
    }

    /**
     * Adds a user to the waitlist given that the user either does not have an association with the event or the association is cancelled.
     * <p>
     *     The listener will be returned with false success if an error occurs or the user cannot be added to the waitlist.
     * </p>
     * @param user The user to be added
     * @param location The current location of the user. If this event requires geolocation, the location cannot be null.
     *                 If the event does not require geo, the location is auto set to null
     * @param listener The listener for completion. Ownership of the event association is given to the user
     */
    @Database.MustStir
    public void addUserToWaitlist(@Database.Observes User user, @Nullable GeoPoint location, DataListener<Event, QueryResult<EventAssociation>> listener){

        if(getRequiresLocation()){
            if(location == null){
                listener.onCompletion(this, null, false);
                return;
            }
        }else{
            location = null;
        }

        GeoPoint finalLocation = getRequiresLocation() ? location : null;

        if(!isRegistrationOpen()){
            listener.onCompletion(this, null, false);
            return;
        }
        refreshData((query, success) -> {
            if(!success) {
                listener.onCompletion(this, null, false);
                return;
            }
            if(getCurrentWaitlist() >= getWaitlistCapacity()){
                listener.onCompletion(this, null, false);
                return;
            }

            getUserAssociation(user, (query1, data, success1) -> {
                if(!success1){
                    listener.onCompletion(this, null, false);
                    return;
                }
                if(data.size() > 1){
                    listener.onCompletion(this, null, false);
                    return;
                }
                if(data.size() == 1) {
                    data.setStatus(R.string.event_assoc_status_waitlist);
                    EventAssociation e = data.result.get(0).fetch();
                    String status = e.getStatus();

                    if (!Objects.equals(status, db.constants.getString(R.string.notification_cancelled_body))) {
                        //Not in state where can become waitlist
                        listener.onCompletion(this, new QueryResult<>(e), false);
                        data.dissolve();
                        return;
                    }
                    e.setStatus(R.string.database_event_waitlist);
                    listener.onCompletion(this, new QueryResult<>(data.result.get(0)), true);
                    data.dissolve();
                    return;
                }
                //User isnt already associated
                EventAssociation.NewInstance(db, getDocumentID(), finalLocation, db.constants.getString(R.string.event_assoc_status_waitlist), user.getDocumentID(), (instance, success2) -> {
                    listener.onCompletion(this, new QueryResult<>(instance), success2);
                    data.dissolve();
                });
            });

        });
    }

    /**
     * Gets the association of the user
     * @param user The user to retrieve
     * @param listener Ownership is passed of the event assoc
     */
    @Database.MustStir
    public void getUserAssociation(@Database.Observes User user, DataListener<Event, EventAssociation.QueryModifier<Event>> listener){
        getAssociatedUsersFromQuery(assocUsersQuery.whereEqualTo(db.constants.getString(R.string.database_assoc_user), user.getDocumentID()), listener);
    }

    /**
     * @return Returns if now is between the closed and open registration date.
     */
    public boolean isRegistrationOpen(){
        Timestamp now = Timestamp.now();
        return now.compareTo(getOpenRegistrationDate()) >= 0 && now.compareTo(getCloseRegistrationDate()) <= 0;
    }

    /**
     * Updates all properties of the event. If the event changes, a notification is sent to listeners once and the database is updated once
     * @param title The title of the event
     * @param posterID The id of the poster image
     * @param description the description of the event
     * @param qrHash the cashed qr code data for the event
     * @param price The price of the event
     * @param eventDates The dates that the event occurs
     * @return If the event changed as a result
     */
    @Database.StirsDeep(what = "The previous image")
    public boolean update(String title,
                          @Database.Dilutes String posterID,
                          String description,
                          String qrHash,
                          Double price,
                          List<Timestamp> eventDates
    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_event_title, title);
        map.put(R.string.database_event_posterID, posterID);
        map.put(R.string.database_event_description, description);
        map.put(R.string.database_event_qrHash, qrHash);
        map.put(R.string.database_event_price, price);
        map.put(R.string.database_event_dates, eventDates);
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
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_event_description, o -> o instanceof String, true),
            new PropertyField<Integer, PropertyField.NullInstance>(R.string.database_event_capacity, o -> o instanceof Integer && (Integer)o > 0, false),
            new PropertyField<Integer, PropertyField.NullInstance>(R.string.database_event_waitlist, o -> o instanceof Integer && ((Integer)o > 0 || (Integer)o == -1), false),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_event_qrHash, o -> o instanceof String, true),
            new PropertyField<Double, PropertyField.NullInstance>(R.string.database_event_price, o -> o instanceof Double && ((Double) o) >= 0, true),
            new PropertyField<Timestamp, PropertyField.NullInstance>(R.string.database_event_createdTime, o -> o instanceof Timestamp, false),
            new PropertyField<Timestamp, PropertyField.NullInstance>(R.string.database_event_openDate, o -> o instanceof Timestamp, false),
            new PropertyField<Timestamp, PropertyField.NullInstance>(R.string.database_event_closedDate, o -> o instanceof Timestamp, false),
            new PropertyField<List<Timestamp>, PropertyField.NullInstance>(R.string.database_event_dates, o -> o instanceof List && !((List<?>)o).isEmpty() && ((List<?>)o).stream().allMatch(i -> i instanceof Timestamp), true),
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
     * @param qrHash the cashed qr code data for the event,
     * @param price The price of the event
     * @param openRegistrationDate The date that registration opens, if null uses now
     * @param closedRegistrationDate The date that registration closes and the lottery opens
     * @param eventDates The dates that the event occurs
     * @see Database#createNewInstance(Database.Collections, String, Map, Database.InitializationListener)
     */
    @Database.MustStir
    public static void NewInstance(Database db,
                                       String title,
                                       @Database.Dilutes String posterID,
                                       @Database.Dilutes String facilityID,
                                       Boolean requiresLocation,
                                       String description,
                                       Integer capacity,
                                       Integer waitlistCapacity,
                                       String qrHash,
                                       Double price,
                                       @Nullable Timestamp openRegistrationDate,
                                       Timestamp closedRegistrationDate,
                                       List<Timestamp> eventDates,
                                       Database.InitializationListener<Event> listener
    ){
        Timestamp now = Timestamp.now();
        openRegistrationDate = openRegistrationDate == null ? now : openRegistrationDate;
        Map<Integer,Object> map = createDataMap(title,posterID, facilityID, requiresLocation, description, capacity, waitlistCapacity, qrHash, price, openRegistrationDate, closedRegistrationDate, eventDates,  now);

        if(!validateDataMap(map)){
            listener.onInitialization(null, false);
            return;
        }

        db.createNewInstance(Database.Collections.EVENTS, facilityID + "-" + now.toString(), db.convertIDMapToNames(map), listener);
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
     * @param price The price of the event
     * @param openRegistrationDate The date that registration opens
     * @param closedRegistrationDate The date that registration closes and the lottery opens
     * @param eventDates The dates that the event occurs
     * @param createdTime The time when the event was created
     * @return The map
     */
    public static Map<Integer, Object> createDataMap(String title,
                                                     @Database.Observes String posterID,
                                                     @Database.Observes String facilityID,
                                                     Boolean requiresLocation,
                                                     String description,
                                                     Integer capacity,
                                                     Integer waitlistCapacity,
                                                     String qrHash,
                                                     Double price,
                                                     Timestamp openRegistrationDate,
                                                     Timestamp closedRegistrationDate,
                                                     List<Timestamp> eventDates,
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
        map.put(R.string.database_event_price, price);
        map.put(R.string.database_event_createdTime, createdTime);
        map.put(R.string.database_event_openDate, openRegistrationDate);
        map.put(R.string.database_event_closedDate, closedRegistrationDate);
        map.put(R.string.database_event_dates, eventDates);

        return map;
    }

    /**
     * Tests if the data is valid
     * @param dataMap The data map
     * @return The
     * @see #createDataMap(String, String, String, Boolean, String, Integer, Integer, String, Double, Timestamp, Timestamp, List, Timestamp)
     */
    public static boolean validateDataMap(@Database.Observes Map<Integer, Object> dataMap){
        return DatabaseInstance.isDataValid(dataMap, fields);
    }
}
