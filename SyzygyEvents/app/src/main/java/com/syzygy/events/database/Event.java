package com.syzygy.events.database;

import android.net.Uri;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.AggregateQuery;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.syzygy.events.R;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
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
    public void cancelAllInvitedUsers(DataListener<Event, EventAssociation.NotificationResult> listener) {
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
        return setPropertyValue(R.string.database_event_title, val, s -> {});
    }

    public String getPosterID(){
        return getPropertyValueI(R.string.database_event_posterID);
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
        return setPropertyValue(R.string.database_event_description, val, s -> {});
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
        return setPropertyValue(R.string.database_event_qrHash, val, s -> {});
    }
    
    public Timestamp getCreatedDate(){
        return getPropertyValueI(R.string.database_event_createdTime);
    }

    @Database.Observes
    public Image getPoster(){
        return getPropertyInstanceI(R.string.database_event_posterID);
    }


    /**
     * Sets the profile image instance. This function will create a new reference to the instance.
     * @param image The new instance
     * @param onComplete called on completion with if the update was successful
     * @see #setAssociatedImage(Uri, String, Consumer)
     */
    @Database.StirsDeep(what = "The previous Image")
    public void setPoster(@Nullable Uri image, Consumer<Boolean> onComplete){
        setAssociatedImage(image, getTitle(), onComplete);
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

    public Integer getEventDates(){
        return getPropertyValueI(R.string.database_event_dates);
    }

    public String getFormattedEventDates(){
        return Dates.format(getEventDates());
    }

    public Timestamp getStartDate(){
        return getPropertyValueI(R.string.database_event_start);
    }

    public Timestamp getEndDate(){
        return getPropertyValueI(R.string.database_event_end);
    }

    public Double getPrice(){
        return getPropertyValueI(R.string.database_event_price);
    }

    public boolean setPrice(Double val){
        return setPropertyValue(R.string.database_event_price, val, s -> {});
    }

    /**
     * Adds a user to the waitlist given that the user either does not have an association with the event or the association is cancelled.
     * <p>
     *     The listener will be returned with false success if an error occurs or the user cannot be added to the waitlist. If the this is because the user is already attached, the association is returned with false success
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
            int waitListCapacity = getWaitlistCapacity();
            if(waitListCapacity >= 0 && getCurrentWaitlist() >= waitListCapacity){
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
                    EventAssociation e = data.result.get(0).fetch();
                    String status = e.getStatus();

                    if (!Objects.equals(status, db.constants.getString(R.string.event_assoc_status_cancelled))) {
                        //Not in state where can become waitlist
                        listener.onCompletion(this, new QueryResult<>(e), false);
                        data.dissolve();
                        return;
                    }
                    e.setStatus(R.string.event_assoc_status_waitlist);
                    listener.onCompletion(this, new QueryResult<>(e), true);
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
     * Adds a user to the enrolled given that the user is currently invited
     * <p>
     *     The listener will be returned with false success if an error occurs or the user cannot be enrolled. If the this is because the user in a not invite status, the association is returned with false success
     * </p>
     * @param user
     * @param listener
     */
    @Database.MustStir
    public void acceptInvite(@Database.Observes User user, DataListener<Event, QueryResult<EventAssociation>> listener){

        refreshData((query, success) -> {
            if(!success) {
                listener.onCompletion(this, null, false);
                return;
            }

            if(getCurrentEnrolled() >= getCapacity()){
                listener.onCompletion(this, null, false);
                return;
            }

            getUserAssociation(user, (query1, data, success1) -> {
                if(!success1){
                    listener.onCompletion(this, null, false);
                    return;
                }
                if(data.size() != 1){
                    listener.onCompletion(this, null, false);
                    return;
                }

                EventAssociation e = data.result.get(0).fetch();
                String status = e.getStatus();

                if (!Objects.equals(status, db.constants.getString(R.string.event_assoc_status_invited))) {
                    //Not in state where can become enrolled
                    listener.onCompletion(this, new QueryResult<>(e), false);
                    data.dissolve();
                    return;
                }

                e.setStatus(R.string.event_assoc_status_cancelled);
                listener.onCompletion(this, new QueryResult<>(e), true);
                data.dissolve();
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
     * @return Returns if now is after the close date
     */
    public boolean isAfterRegistration(){
        Timestamp now = Timestamp.now();
        return now.compareTo(getCloseRegistrationDate()) > 0;
    }

    /**
     * @return Returns if now is before the open date
     */
    public boolean isBeforeRegistration(){
        Timestamp now = Timestamp.now();
        return now.compareTo(getOpenRegistrationDate()) < 0;
    }

    /**
     * Updates all properties of the event. If the event changes, a notification is sent to listeners once and the database is updated once
     * @param title The title of the event
     * @param description the description of the event
     * @param posterImage The new Poster image
     * @param qrHash the cashed qr code data for the event
     * @param price The price of the event
     * @param onComplete called on completion. Not called if properties are invalid
     * @return All invalid ids
     * @see #updateDataFromMap(Map, Uri, String, Consumer)
     */
    @Database.StirsDeep(what = "The previous image")
    public Set<Integer> update(String title,
                               @Database.Dilutes String posterID,
                               String description,
                               Uri posterImage,
                               String qrHash,
                               Double price,
                               Consumer<Boolean> onComplete
    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_event_title, title);
        map.put(R.string.database_event_description, description);
        map.put(R.string.database_event_qrHash, qrHash);
        map.put(R.string.database_event_price, price);

        return updateDataFromMap(map, posterImage, title, onComplete);
    }

    /**
     * Updates all properties of the event. If the event changes, a notification is sent to listeners once and the database is updated once
     * @param title The title of the event
     * @param description the description of the event
     * @param qrHash the cashed qr code data for the event
     * @param price The price of the event
     * @param onComplete will always be true and will be called before return
     * @return All invalid ids
     * @see #updateDataFromMap(Map, Consumer)
     */
    public Set<Integer> update(String title,
                          String description,
                          String qrHash,
                          Double price,
                          Consumer<Boolean> onComplete
    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_event_title, title);
        map.put(R.string.database_event_description, description);
        map.put(R.string.database_event_qrHash, qrHash);
        map.put(R.string.database_event_price, price);

        return updateDataFromMap(map, onComplete);
    }

    /**
     * The list of the fields defined for a User
     */
    static final PropertyField<?, ?>[] fields = {
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_event_title, o -> o instanceof String && !((String) o).isBlank(), true),
            new PropertyField<String, Image>(R.string.database_event_posterID, o -> o instanceof String, true, true, Database.Collections.IMAGES, true, true),
            new PropertyField<String, Facility>(R.string.database_event_facilityID, o -> o instanceof String && !((String) o).isBlank(), false, true, Database.Collections.FACILITIES, false, false),
            new PropertyField<Boolean, PropertyField.NullInstance>(R.string.database_event_geo, o -> o instanceof Boolean, false),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_event_description, o -> o instanceof String, true),
            new PropertyField<Integer, PropertyField.NullInstance>(R.string.database_event_capacity, o -> o instanceof Integer && (Integer)o > 0, false),
            new PropertyField<Integer, PropertyField.NullInstance>(R.string.database_event_waitlist, o -> o instanceof Integer && ((Integer)o > 0 || (Integer)o == -1), false),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_event_qrHash, o -> o instanceof String, true),
            new PropertyField<Double, PropertyField.NullInstance>(R.string.database_event_price, o -> o instanceof Double && ((Double) o) >= 0, true),
            new PropertyField<Timestamp, PropertyField.NullInstance>(R.string.database_event_createdTime, o -> o instanceof Timestamp, false),
            new PropertyField<Timestamp, PropertyField.NullInstance>(R.string.database_event_openDate, o -> o instanceof Timestamp || o == null, false),
            new PropertyField<Timestamp, PropertyField.NullInstance>(R.string.database_event_closedDate, o -> o instanceof Timestamp, false),
            new PropertyField<Timestamp, PropertyField.NullInstance>(R.string.database_event_start, o -> o instanceof Timestamp, false),
            new PropertyField<Timestamp, PropertyField.NullInstance>(R.string.database_event_end, o -> o instanceof Timestamp, false),
            new PropertyField<Integer, PropertyField.NullInstance>(R.string.database_event_dates, o -> o instanceof Integer, false),
    };

    @Override
    protected List<Pair<Query, Database.Collections>> subInstanceCascadeDeleteQuery() {
        return Collections.singletonList(
                new Pair<>(Database.Collections.EVENT_ASSOCIATIONS.getCollection(db)
                        .whereEqualTo(db.constants.getString(R.string.database_assoc_event), getDocumentID()), Database.Collections.EVENT_ASSOCIATIONS)
        );
    }

    /**
     * Validates and creates a new Image instance in the database using the given data.
     * @param db The database
     * @param title The title of the event
     * @param posterImage The the poster image
     * @param facilityID the id of the facility
     * @param requiresLocation If the users geolocation is recorded on signup
     * @param description the description of the event
     * @param capacity The max capacity of the event
     * @param waitlistCapacity the max waitlist capacity of the event
     * @param price The price of the event
     * @param openRegistrationDate The date that registration opens, if null uses now
     * @param closedRegistrationDate The date that registration closes and the lottery opens
     * @param startDate The datetime that the event starts
     * @param endDate The datetime that the event stored
     * @param eventDates The days of the week that the event occurs {@link Dates}
     * @param listener Will be called once the event is initialized. Is not called if the data is invalid
     * @return The property id of all invalid properties
     * @see Database#createNewInstance(Database.Collections, String, Map, Uri, String, Database.InitializationListener)
     */
    @Database.MustStir
    public static Set<Integer> NewInstance(Database db,
                                       String title,
                                       Uri posterImage,
                                       @Database.Dilutes String facilityID,
                                       Boolean requiresLocation,
                                       String description,
                                       Integer capacity,
                                       Integer waitlistCapacity,
                                       Double price,
                                       @Nullable Timestamp openRegistrationDate,
                                       Timestamp closedRegistrationDate,
                                       Timestamp startDate,
                                       Timestamp endDate,
                                       Integer eventDates,
                                       Database.InitializationListener<Event> listener
    ){
        Timestamp now = Timestamp.now();
        openRegistrationDate = openRegistrationDate == null ? now : openRegistrationDate;
        String id = Database.Collections.EVENTS.getNewID(db);

        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_event_title, title);
        map.put(R.string.database_event_facilityID, facilityID);
        map.put(R.string.database_event_geo, requiresLocation);
        map.put(R.string.database_event_description, description);
        map.put(R.string.database_event_capacity, capacity);
        map.put(R.string.database_event_waitlist, waitlistCapacity);
        map.put(R.string.database_event_qrHash, id);
        map.put(R.string.database_event_price, price);
        map.put(R.string.database_event_createdTime, now);
        map.put(R.string.database_event_openDate, openRegistrationDate);
        map.put(R.string.database_event_closedDate, closedRegistrationDate);
        map.put(R.string.database_event_start, startDate);
        map.put(R.string.database_event_end, endDate);
        map.put(R.string.database_event_dates, eventDates);

        return db.createNewInstance(Database.Collections.EVENTS, id, map, posterImage, title, listener);
    }

    /**
     * Used to store repetition of dates in the database
     * @see #NO_REPEAT
     * @see #MONDAY
     * @see #TUESDAY
     * @see #WEDNESDAY
     * @see #THURSDAY
     * @see #FRIDAY
     * @see #SATURDAY
     * @see #SUNDAY
     * @see #WEEKDAYS
     * @see #EVERY_DAY
     */
    public enum Dates { // used as a namespace
        ;
        public static final int
                NO_REPEAT   = 0b0000000,
                MONDAY      = 0b1000000,
                TUESDAY     = 0b0100000,
                WEDNESDAY   = 0b0010000,
                THURSDAY    = 0b0001000,
                FRIDAY      = 0b0000100,
                SATURDAY    = 0b0000010,
                SUNDAY      = 0b0000001,
                WEEKDAYS    = 0b1111100,
                EVERY_DAY   = 0b1111111;

        /**
         * Checks if the collection of dates contains the specific day
         * @param collection The set of {@link Dates} to check
         * @param day The {@link Dates} to look for
         * @return {@code true} if the collection contains the day. Will return {@code true} if the
         * collection contains other days as well
         */
        public static boolean collectionContainsDay(int collection, int day){
            return (collection & day) == day;
        }

        /**
         * Gets the number of days selected
         * @param collection The set of {@link Dates} to check
         * @return The number of days in the selection
         */
        public static int numberOfDays(int collection){
            int count = 0;
            for(int i=0; i<7; i++){
                if(collectionContainsDay(collection, 1<<i)) count ++;
            }
            return count;
        }

        /**
         * Formats the set of days to a user friendly string
         * @param collection The set of days
         * @return The formated string
         */
        public static String format(int collection){

            if(collection == WEEKDAYS) return "Weekdays";
            if(collection == EVERY_DAY) return "Every day";
            if(collection == NO_REPEAT) return "No Repetition";

            int count = numberOfDays(collection);
            List<String> result = new ArrayList<>();
            if(collectionContainsDay(collection, MONDAY)){
                if(count == 1) return "Mondays";
                if(count <= 3) result.add("Mon");
                else result.add("M");
            }
            if(collectionContainsDay(collection, TUESDAY)){
                if(count == 1) return "Tuesdays";
                if(count <= 3) result.add("Tues");
                else result.add("Tu");
            }
            if(collectionContainsDay(collection, WEDNESDAY)){
                if(count == 1) return "Wednesdays";
                if(count <= 3) result.add("Wed");
                else result.add("W");
            }
            if(collectionContainsDay(collection, THURSDAY)){
                if(count == 1) return "Thursdays";
                if(count <= 3) result.add("Thurs");
                else result.add("Th");
            }
            if(collectionContainsDay(collection, FRIDAY)){
                if(count == 1) return "Fridays";
                if(count <= 3) result.add("Fri");
                else result.add("F");
            }
            if(collectionContainsDay(collection, SATURDAY)){
                if(count == 1) return "Saturdays";
                if(count <= 3) result.add("Sat");
                else result.add("Sa");
            }
            if(collectionContainsDay(collection, SUNDAY)){
                if(count == 1) return "Sundays";
                if(count <= 3) result.add("Sun");
                else result.add("Su");
            }
            return String.join(", ", result);
        }
    }
}
