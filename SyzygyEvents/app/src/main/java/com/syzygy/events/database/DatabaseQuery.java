package com.syzygy.events.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.syzygy.events.R;

import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Stores a query that is executed on a collection within the database. Loads instances returned from this query
 * @author Gareth Kmet
 * @version 1.0
 * @since 20oct24
 */
public class DatabaseQuery <T extends DatabaseInstance<T>> implements Database.UpdateListener {
    /**
     * The database
     */
    private final @NonNull Database db;
    /**
     * The collection which this queries
     */
    private final @NonNull Database.Collections collection;
    /**
     * The query to evaluate
     */
    private final @NonNull Query query;
    /**
     * The current document snapshot
     */
    private @Nullable QuerySnapshot snapshot;
    /**
     * The current list of loaded instances
     */
    private List<T> currentInstances = new ArrayList<>();

    /**
     * If an update has occurred since the last refresh
     */
    private boolean updates = false;
    /**
     * If an update has occurred since the last refresh
     */
    private boolean deletes = false;

    /**
     * Creates a database query which can be refreshed to load the set of instances which are returned by the query
     * @param db The database
     * @param filter The filter which is used to filter the collection
     * @param collection The collection which this queries
     */
    public DatabaseQuery(@NonNull Database db, @NonNull Filter filter, @NonNull Database.Collections collection){
        this.db = db;
        this.collection = collection;
        this.query = collection.getCollection(db).where(filter);
    }

    /**
     * Refreshes the query and loads the new instances
     * Loads all instances of the current snapshot.
     * On completion, dissolves all previous instances and sets the current instances to the new instances.
     * Then notifies the listener of success.
     * <p>
     *     If any point, and instances errors, dissolves all newly loaded instances and notifies the listener.
     *     The current instances remains the same as before the function call.
     * </p>
     * @param listener The listener. Once the data is loaded, the listener is notified of success.
     *                 Otherwise the listener is notified of failure.
     *
     */
    public void refreshData(DataRefreshListener<T> listener){
        query.get().addOnCompleteListener(task -> {
            if(!task.isSuccessful()){
                listener.onError(this);
                return;
            }
            snapshot = task.getResult();
            loadFromSnapshot(listener);
        });
    }


    /**
     * Loads all instances of the current snapshot.
     * On completion, dissolves all previous instances and sets the current instances to the new instances.
     * Then notifies the listener of success.
     * <p>
     *     If any point, and instances errors, dissolves all newly loaded instances and notifies the listener.
     *     The current instances remains the same as before the function call.
     * </p>
     * @param listener The listener for the refresh
     */
    private void loadFromSnapshot(DataRefreshListener<T> listener){
        if(snapshot == null) return;
        List<DocumentSnapshot> newInstanceDocuments = snapshot.getDocuments();
        final List<T> newInstances = new ArrayList<>();
        Database.InitializationListener<T> l = new Database.InitializationListener<T>() {
            private int count = -1;
            @Override
            public void onInitialization(T instance, boolean success) {
                if(!success){
                    newInstances.forEach(i -> {
                        if (i != null) i.dissolve();
                    });
                    listener.onError(DatabaseQuery.this);
                    return;
                }
                count ++;
                if(count >= newInstanceDocuments.size()){
                    setNewInstances(newInstances);
                    listener.onSuccess(DatabaseQuery.this);
                    return;
                }
                DocumentSnapshot doc = newInstanceDocuments.get(count);
                newInstances.add(db.getInstance(collection, doc.getId(), this, doc));
            }
        };
        l.onInitialization(null, true);
    }

    /**
     * Sets the new instances. Dissolves all previous instances and clears updates/deletions
     * @param newInstances the new instances
     */
    private void setNewInstances(List<T> newInstances){
        dissolve();
        currentInstances = newInstances;
        currentInstances.forEach(i -> i.addListener(this));
        updates = false;
        deletes = false;
    }

    /**
     * Removes references to all instances that have been created and clears the current instance
     */
    public void dissolve(){
        currentInstances.forEach(i -> i.dissolve(this));
        currentInstances.clear();
    }

    @Override
    public <S extends DatabaseInstance<S>> void onUpdate(DatabaseInstance<S> instance, Type type) {
        switch (type){
            case UPDATE:
                updates = true;
                break;
            case DELETE:
                deletes = true;
                break;
            case SUBUPDATE:
            case INIT:
            case DEREFERENCED:
                break;
        }
    }

    /**
     * @return An unmodifiable list of the current instances loaded
     */
    @Unmodifiable
    @NonNull
    public List<T> getCurrentInstances(){
        return Collections.unmodifiableList(currentInstances);
    }

    /**
     * @return The collection to which the instances belong
     */
    public Database.Collections getCollection(){
        return this.collection;
    }

    /**
     * @return {@code true} if one of the items in this query has been updated or deleted since the last refresh.
     *          Does not account for additions.
     */
    public boolean outOfDate(){
        return updates || deletes;
    }

    /**
     * @return {@code true} if one of the items in this query has been updated since the last refresh
     */
    public boolean updateHasOccurred(){
        return updates;
    }

    /**
     * @return {@code true} if one of the items in this query has been deleted since the last refresh
     */
    public boolean deletionHasOccurred(){
        return deletes;
    }

    /**
     * Listener that is called when a query finishes loading data
     * @param <T> The type of the instance
     */
    public interface DataRefreshListener<T extends DatabaseInstance<T>>{
        /**
         * Called if an error occurred while loading data.
         * The data of the query is not changed by the refresh
         * @param query The query
         */
        void onError(DatabaseQuery<T> query);

        /**
         * Called when the query has completed refreshing data and now contains all the new loaded instances
         * @param query The query
         */
        void onSuccess(DatabaseQuery<T> query);
    }

    public static DatabaseQuery<EventAssociation> getMyEventsFilter(Database db, User u){
        return new DatabaseQuery<>(db, Filter.arrayContains(db.constants.getString(R.string.database_assoc_user), u.getDocumentID()), Database.Collections.EVENT_ASSOCIATIONS);
    }

    public static DatabaseQuery<Event> getFacilityEvents(Database db, Facility facility){
        return new DatabaseQuery<>(db, Filter.equalTo(db.constants.getString(R.string.database_event_facilityID), facility.getDocumentID()), Database.Collections.EVENTS);
    }

    public static DatabaseQuery<Notification> getMyNotifications(Database db, User u){
        return new DatabaseQuery<>(db, Filter.equalTo(db.constants.getString(R.string.database_not_receiverID), u.getDocumentID()), Database.Collections.NOTIFICATIONS);
    }

    public static DatabaseQuery<User> getUsers(Database db){
        return null; //TODO
    }

    public static DatabaseQuery<Event> getEvents(Database db){
        return null; //TODO
    }

    public static DatabaseQuery<Image> getImages(Database db){
        return null; //TODO
    }


}
