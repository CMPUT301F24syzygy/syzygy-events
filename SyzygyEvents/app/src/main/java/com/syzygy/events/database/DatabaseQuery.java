package com.syzygy.events.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.syzygy.events.R;

import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Stores a query that is executed on a collection within the database. Loads instances returned from this query in pages
 * @author Gareth Kmet
 * @version 1.0
 * @since 20oct24
 * @param <T> The type of instance being returned
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
     * The number of results returned per page
     */
    private final int resultsPerPage;
    /**
     * The current list of loaded instances
     */
    private final List<T> currentInstances = new ArrayList<>();

    /**
     * The current query used to get the page
     */
    private Query currentPage;

    /**
     * The page
     */
    private Page thisPage = Page.NULL;

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
     * @param query The query
     * @param collection The collection which this queries
     */
    public DatabaseQuery(@NonNull Database db, @NonNull Query query, @NonNull Database.Collections collection, int resultsPerPage){
        this.db = db;
        this.collection = collection;
        this.query = query;
        this.resultsPerPage = resultsPerPage;

    }

    /**
     * Refreshes the query and loads the new instances at the current page.
     * Loads all instances of the current snapshot at the page.
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
        currentPage.get().addOnCompleteListener(task -> {
            if(!task.isSuccessful()){
                listener.onError(this);
                return;
            }
            QuerySnapshot snap = task.getResult();
            if(thisPage == Page.NEXT && snap.size() < resultsPerPage) {
                thisPage = Page.LAST;
            }else if(thisPage == Page.PREVIOUS && snap.size() < resultsPerPage){
                thisPage = Page.FIRST;
            }else if(thisPage == Page.FIRST && snap.size() < resultsPerPage){
                thisPage = Page.FIRST_LAST;
            }
            loadFromSnapshot(listener);
        });
    }

    /**
     * Refreshes the query and loads the new instances starting at the instance following the last instance of the current page.
     * If the result is less the the limit, gets the first page.
     * If the current page does not exist or is empty, gets the first page
     * Loads all instances of the current snapshot at the page.
     * On completion, dissolves all previous instances and sets the current instances to the new instances.
     * Then notifies the listener of success.
     * <p>
     *     If any point, and instances errors, dissolves all newly loaded instances and notifies the listener.
     *     The current instances remains the same as before the function call.
     * </p>
     * <p>
     *     This will refresh data even if the given page is the current page
     * </p>
     * @param listener The listener. Once the data is loaded, the listener is notified of success.
     *                 Otherwise the listener is notified of failure.
     *
     */
    public void gotoNextPage(DataRefreshListener<T> listener){
        if(thisPage == Page.NULL || snapshot == null || snapshot.isEmpty()) {
            gotoFirstPage(listener);
            return;
        }
        thisPage = Page.NEXT;
        nextPageQuery();
        refreshData(listener);
    }

    /**
     * Refreshes the query and loads the new instances ending at the instance before the first instance of the current page.
     * If the results are empty, gets the last page
     * If the current page does not exist or is empty, gets the last page
     * Loads all instances of the current snapshot at the page.
     * On completion, dissolves all previous instances and sets the current instances to the new instances.
     * Then notifies the listener of success.
     * <p>
     *     If any point, and instances errors, dissolves all newly loaded instances and notifies the listener.
     *     The current instances remains the same as before the function call.
     * </p>
     * <p>
     *     This will refresh data even if the given page is the current page
     * </p>
     * @param listener The listener. Once the data is loaded, the listener is notified of success.
     *                 Otherwise the listener is notified of failure.
     *
     */
    public void gotoPreviousPage(DataRefreshListener<T> listener){
        if(thisPage == Page.NULL || snapshot == null || snapshot.isEmpty()) {
            lastPageQuery();
            return;
        }
        thisPage = Page.PREVIOUS;
        previousPageQuery();
        refreshData(listener);
    }

    /**
     * Refreshes the query and loads the first page of results
     * Loads all instances of the current snapshot at the page.
     * On completion, dissolves all previous instances and sets the current instances to the new instances.
     * Then notifies the listener of success.
     * <p>
     *     If any point, and instances errors, dissolves all newly loaded instances and notifies the listener.
     *     The current instances remains the same as before the function call.
     * </p>
     * <p>
     *     This will refresh data even if the given page is the current page
     * </p>
     * @param listener The listener. Once the data is loaded, the listener is notified of success.
     *                 Otherwise the listener is notified of failure.
     *
     */
    public void gotoFirstPage(DataRefreshListener<T> listener){
        thisPage = Page.FIRST;
        firstPageQuery();
        refreshData(listener);
    }

    /**
     * Refreshes the query and loads the last page of results
     * Loads all instances of the current snapshot at the page.
     * On completion, dissolves all previous instances and sets the current instances to the new instances.
     * Then notifies the listener of success.
     * <p>
     *     If any point, and instances errors, dissolves all newly loaded instances and notifies the listener.
     *     The current instances remains the same as before the function call.
     * </p>
     * <p>
     *     This will refresh data even if the given page is the current page
     * </p>
     * @param listener The listener. Once the data is loaded, the listener is notified of success.
     *                 Otherwise the listener is notified of failure.
     *
     */
    public void gotoLastPage(DataRefreshListener<T> listener){
        thisPage = Page.LAST;
        lastPageQuery();
        refreshData(listener);
    }

    /**
     * Goes to the corresponding page
     * @param listener The listener
     * @param page The page to goto
     * @see #gotoFirstPage(DataRefreshListener) 
     * @see #gotoLastPage(DataRefreshListener) 
     * @see #gotoNextPage(DataRefreshListener) 
     * @see #gotoPreviousPage(DataRefreshListener) 
     */
    public void gotoPage(DataRefreshListener<T> listener, Page page){
        switch (page){
            case NULL:
                dissolve(); listener.onSuccess(this); break;
            case LAST:
                gotoLastPage(listener); break;
            case NEXT:
                gotoNextPage(listener); break;
            case FIRST:
            case FIRST_LAST:
                gotoFirstPage(listener); break;
            case PREVIOUS:
                gotoPreviousPage(listener); break;

        }
    }

    /**
     * @return {@code false} if this thinks there is more data before
     */
    public boolean isFirstPage(){
        return thisPage == Page.FIRST || thisPage == Page.FIRST_LAST;
    }

    /**
     * @return {@code true} if this thinks there is more data after
     */
    public boolean isLastPage(){
        return thisPage == Page.LAST || thisPage == Page.FIRST_LAST;
    }

    /**
     * Sets the currentPage query to the set of resultsPerPage after the last document in the previous result.
     */
    private void nextPageQuery(){
        assert snapshot != null;
        currentPage = query.startAfter(snapshot.getDocuments().get(snapshot.size())).limit(resultsPerPage);
    }

    /**
     * Sets the currentPage query to the set of resultsPerPage before the first document in the previous result.
     */
    private void previousPageQuery(){
        assert snapshot != null;
        currentPage = query.endBefore(snapshot.getDocuments().get(0)).limitToLast(resultsPerPage);
    }

    /**
     * Gets the first resultsPerPage of the query
     */
    private void firstPageQuery(){
        currentPage = query.limit(resultsPerPage);
    }
    /**
     * Gets the last resultsPerPage of the query
     */
    private void lastPageQuery(){
        currentPage = query.limitToLast(resultsPerPage);
    }


    /**
     * @return The number of results per page
     */
    public int getResultsPerPage(){
        return resultsPerPage;
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
        currentInstances.addAll(newInstances);
        currentInstances.forEach(i -> i.addListener(this));
        updates = false;
        deletes = false;
    }

    /**
     * Removes references to all instances that have been created and clears the current instance
     */
    public void dissolve(){
        thisPage = Page.NULL;
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
        Filter f1 = Filter.equalTo(db.constants.getString(R.string.database_assoc_user), u.getDocumentID());
        Filter f2 = Filter.notEqualTo(db.constants.getString(R.string.database_assoc_status), db.constants.getString(R.string.event_assoc_status_cancelled));
        Database.Collections c = Database.Collections.EVENT_ASSOCIATIONS;
        Query q = c.getCollection(db).where(Filter.and(f1,f2)).orderBy(db.constants.getString(R.string.database_assoc_time), Query.Direction.DESCENDING);
        return new DatabaseQuery<>(db, q, c, 10);
    }

    public static DatabaseQuery<Event> getFacilityEvents(Database db, Facility facility){
        Filter f = Filter.equalTo(db.constants.getString(R.string.database_event_facilityID), facility.getDocumentID());
        Database.Collections c = Database.Collections.EVENTS;
        Query q = c.getCollection(db).where(f).orderBy(db.constants.getString(R.string.database_event_createdTime), Query.Direction.DESCENDING);
        return new DatabaseQuery<>(db, q, c, 10);
    }

    public static DatabaseQuery<Notification> getMyNotifications(Database db, User u){
        Filter f = Filter.equalTo(db.constants.getString(R.string.database_not_receiverID), u.getDocumentID());
        Database.Collections c = Database.Collections.NOTIFICATIONS;
        Query q = c.getCollection(db).where(f).orderBy(db.constants.getString(R.string.database_not_time), Query.Direction.DESCENDING);
        return new DatabaseQuery<>(db, q, c, 25);
    }

    /**
     * @param status iF null or blank, returns all
     */
    public static DatabaseQuery<User> getAttachedUsers(Database db, Event e, String status){
        Filter f = Filter.arrayContains(db.constants.getString(R.string.database_assoc_event), e.getDocumentID());
        if(status != null && !status.isBlank()){
            f = Filter.and(f, Filter.equalTo(db.constants.getString(R.string.database_assoc_status), status));
        }
        Database.Collections c = Database.Collections.EVENT_ASSOCIATIONS;
        Query q = c.getCollection(db).where(f).orderBy(db.constants.getString(R.string.database_assoc_time), Query.Direction.DESCENDING);
        return new DatabaseQuery<>(db, q, c, 25);
    }

    public static DatabaseQuery<User> getUsers(Database db){
        Filter f = Filter.and(); //TODO - does this work
        Database.Collections c = Database.Collections.USERS;
        Query q = c.getCollection(db).where(f).orderBy(db.constants.getString(R.string.database_user_createdTime), Query.Direction.DESCENDING);
        return new DatabaseQuery<>(db, q, c, 25);
    }

    public static DatabaseQuery<Event> getEvents(Database db){
        Filter f = Filter.and(); //TODO - does this work
        Database.Collections c = Database.Collections.EVENTS;
        Query q = c.getCollection(db).where(f).orderBy(db.constants.getString(R.string.database_event_createdTime), Query.Direction.DESCENDING);
        return new DatabaseQuery<>(db, q, c, 10);
    }

    public static DatabaseQuery<Image> getImages(Database db){
        Filter f = Filter.and(); //TODO - does this work
        Database.Collections c = Database.Collections.IMAGES;
        Query q = c.getCollection(db).where(f).orderBy(db.constants.getString(R.string.database_img_uploadTime), Query.Direction.DESCENDING);
        return new DatabaseQuery<>(db, q, c, 25);
    }

    public enum Page{
        /**
         * @see #dissolve()
         */
        NULL,
        /**
         * @see #gotoFirstPage(DataRefreshListener) 
         */
        FIRST,
        /**
         * @see #gotoLastPage(DataRefreshListener) 
         */
        LAST,
        /**
         * @see #gotoNextPage(DataRefreshListener)
         */
        NEXT,
        /**
         * @see #gotoPreviousPage(DataRefreshListener)
         */
        PREVIOUS,
        /**
         * @see #gotoFirstPage(DataRefreshListener)
         */
        FIRST_LAST
    }
}
