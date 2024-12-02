package com.syzygy.events.database;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;

import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


/**
 * Stores a query that is executed on a collection within the database. Loads instances returned from this query in pages
 * @param <T> The type of instance being returned
 */
@Database.Dissolves
public class DatabaseQuery <T extends DatabaseInstance<T>> implements Database.UpdateListener, Database.Querrier<DatabaseQuery<T>>, Database.Dissolvable {
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
     * The number of results returned per page. If null, all results are returned
     */
    private final @Nullable Integer resultsPerPage;
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
    @Database.MustStir
    public DatabaseQuery(@NonNull Database db, @NonNull Query query, @NonNull Database.Collections collection, @Nullable Integer resultsPerPage){
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
    @Database.Titrates(what = "Result instances", when = "All success")
    @Database.StirsDeep(what = "Previous Instances", when = "All success")
    @Database.Observes
    public void refreshData(Listener<DatabaseQuery<T>> listener){
        if(currentPage == null){
            firstPageQuery();
        }
        currentPage.get().addOnCompleteListener(task -> {
            if(!task.isSuccessful()){
                listener.onCompletion(this, false);
                return;
            }
            snapshot = task.getResult();
            if(resultsPerPage != null){
                if(thisPage == Page.NEXT && snapshot.size() < resultsPerPage) {
                    thisPage = Page.LAST;
                }else if(thisPage == Page.PREVIOUS && snapshot.size() < resultsPerPage){
                    thisPage = Page.FIRST;
                }else if(thisPage == Page.FIRST && snapshot.size() < resultsPerPage){
                    thisPage = Page.FIRST_LAST;
                }
            }else{
                thisPage = Page.FIRST_LAST;
            }
            loadFromSnapshot(listener);
        });
    }

    /**
     * Returns an object which contains methods that help notify the user or change the status of the associations.
     * This object must be called with {@code .dissolve} once complete
     * @param query The query containing the associations
     * @return The set of methods for the associations currently in the list (does not match on change)
     */
    @Database.MustStir
    public static EventAssociation.Methods<DatabaseQuery<EventAssociation>> methods(@Database.Observes DatabaseQuery<EventAssociation> query){
        return new EventAssociation.Methods<>(query.db, query, query.getCurrentInstances());
    }

    /**
     * Returns an object which contains methods that help notify the user or change the status of the associations.
     * This object must be called with {@code .dissolve} once complete
     * @param querrier The querrier of the results
     * @param query The query containing the associations
     * @param eas The list of event associations to get methods for
     * @return The set of methods for the associations currently in the list (does not match on change)
     */
    @Database.MustStir
    static <T extends Database.Querrier<T>> EventAssociation.Methods<T> methods(@Database.Observes T querrier, @Database.Observes DatabaseQuery<EventAssociation> query, @Database.Dilutes List<EventAssociation> eas){
        return new EventAssociation.Methods<>(query.db, querrier, eas);
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
    @Database.Titrates(what = "Result Instances", when = "All success")
    @Database.StirsDeep(what = "Previous Instances", when = "All success")
    public void gotoNextPage(Listener<DatabaseQuery<T>> listener){
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
    @Database.Titrates(what = "Result Instances", when = "All success")
    @Database.StirsDeep(what = "Previous Instances", when = "All success")
    public void gotoPreviousPage(Listener<DatabaseQuery<T>> listener){
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
    @Database.Titrates(what = "Result Instances", when = "All success")
    @Database.StirsDeep(what = "Previous Instances", when = "All success")
    public void gotoFirstPage(Listener<DatabaseQuery<T>> listener){
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
    @Database.Titrates(what = "Result Instances", when = "All success")
    @Database.StirsDeep(what = "Previous Instances", when = "All success")
    public void gotoLastPage(Listener<DatabaseQuery<T>> listener){
        thisPage = Page.LAST;
        lastPageQuery();
        refreshData(listener);
    }

    /**
     * Goes to the corresponding page
     * @param listener The listener
     * @param page The page to goto
     * @see #gotoFirstPage(Listener)
     * @see #gotoLastPage(Listener)
     * @see #gotoNextPage(Listener)
     * @see #gotoPreviousPage(Listener)
     */
    @Database.Titrates(what = "Result Instances", when = "All success")
    @Database.StirsDeep(what = "Previous Instances", when = "All success")
    public void gotoPage(Listener<DatabaseQuery<T>> listener, Page page){
        switch (page){
            case NULL:
                dissolve(); listener.onCompletion(this, true); break;
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
        currentPage = resultsPerPage == null ? query : query.startAfter(snapshot.getDocuments().get(snapshot.size())).limit(resultsPerPage);
    }

    /**
     * Sets the currentPage query to the set of resultsPerPage before the first document in the previous result.
     */
    private void previousPageQuery(){
        assert snapshot != null;
        currentPage = resultsPerPage == null ? query : query.endBefore(snapshot.getDocuments().get(0)).limitToLast(resultsPerPage);
    }

    /**
     * Gets the first resultsPerPage of the query
     */
    private void firstPageQuery(){
        currentPage = resultsPerPage == null ? query : query.limit(resultsPerPage);
    }
    /**
     * Gets the last resultsPerPage of the query
     */
    private void lastPageQuery(){
        currentPage = resultsPerPage == null ? query : query.limitToLast(resultsPerPage);
    }


    /**
     * @return The number of results per page. {@code null} if infinite
     */
    public @Nullable Integer getResultsPerPage(){
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
    @Database.Titrates(what = "Result Instances", when = "All success")
    @Database.StirsDeep(what = "Previous Instances", when = "All success")
    @Database.Observes
    private void loadFromSnapshot(Listener<DatabaseQuery<T>> listener){
        if(snapshot == null) {
            listener.onCompletion(this, false);
            return;
        }
        List<DocumentSnapshot> newInstanceDocuments = snapshot.getDocuments();
        final List<T> newInstances = new ArrayList<>();
        Database.InitializationListener<T> l = new Database.InitializationListener<T>() {
            private int count = -1;
            @Override
            public void onInitialization(@Database.Observes T instance, boolean success) {
                if(!success){
                    newInstances.forEach(i -> {
                        if (i != null) i.dissolve();
                    });
                    listener.onCompletion(DatabaseQuery.this, false);
                    return;
                }
                if(count >= 0) newInstances.add(instance);
                count ++;
                if(count >= newInstanceDocuments.size()){
                    setNewInstances(newInstances);
                    listener.onCompletion(DatabaseQuery.this, true);
                    return;
                }
                DocumentSnapshot doc = newInstanceDocuments.get(count);
                Log.println(Log.DEBUG, "init", doc.getId());
                db.getInstance(collection, doc.getId(), this, doc);
            }
        };
        l.onInitialization(null, true);
    }

    /**
     * Sets the new instances. Dissolves all previous instances and clears updates/deletions
     * @param newInstances the new instances
     */
    @Database.Titrates(what = "New Instances")
    @Database.StirsDeep(what = "Previous Instances")
    private void setNewInstances(@Database.Dilutes List<T> newInstances){
        List<T> previous = new ArrayList<>(currentInstances);
        currentInstances.clear();
        currentInstances.addAll(newInstances);
        //currentInstances.forEach(i -> i.fetch(this)); already fetched by initializer
        previous.forEach(DatabaseInstance::dissolve);
        updates = false;
        deletes = false;
    }

    /**
     * Removes references to all instances that have been created and clears the current instance
     */
    @Database.StirsDeep(what = "Previous Instances")
    public void dissolve(){
        thisPage = Page.NULL;
        currentInstances.forEach(i -> i.dissolve(this));
        currentInstances.clear();
    }

    @Override
    public <S extends DatabaseInstance<S>> void onUpdate(@Database.Observes DatabaseInstance<S> instance, Type type) {
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
    @Database.Observes
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

    @Database.MustStir
    public static DatabaseQuery<EventAssociation> getMyEventsFilter(Database db, @Database.Observes User u){
        Filter f1 = Filter.equalTo(db.constants.getString(R.string.database_assoc_user), u.getDocumentID());
        Filter f2 = Filter.notEqualTo(db.constants.getString(R.string.database_assoc_status), db.constants.getString(R.string.event_assoc_status_cancelled));
        Database.Collections c = Database.Collections.EVENT_ASSOCIATIONS;
        Query q = c.getCollection(db).where(Filter.and(f1,f2)).orderBy(db.constants.getString(R.string.database_assoc_time), Query.Direction.DESCENDING);
        return new DatabaseQuery<>(db, q, c, null);
    }

    @Database.MustStir
    public static DatabaseQuery<Event> getFacilityEvents(Database db, @Database.Observes Facility facility){
        Filter f = Filter.equalTo(db.constants.getString(R.string.database_event_facilityID), facility.getDocumentID());
        Database.Collections c = Database.Collections.EVENTS;
        Query q = c.getCollection(db).where(f).orderBy(db.constants.getString(R.string.database_event_createdTime), Query.Direction.DESCENDING);
        return new DatabaseQuery<>(db, q, c, null);
    }

    @Database.MustStir
    public static DatabaseQuery<Notification> getMyNotifications(Database db, @Database.Observes User u){
        Query q = getMyNotificationsQuery(db, u);
        return new DatabaseQuery<>(db, q, Database.Collections.NOTIFICATIONS, null);
    }

    public static Query getMyNotificationsQuery(Database db, @Database.Observes User u){
        Filter f = Filter.equalTo(db.constants.getString(R.string.database_not_receiverID), u.getDocumentID());
        Database.Collections c = Database.Collections.NOTIFICATIONS;
        return c.getCollection(db).where(f).orderBy(db.constants.getString(R.string.database_not_time), Query.Direction.DESCENDING);
    }

    /**
     * @param status iF null or blank, returns all
     */
    @Database.MustStir
    public static DatabaseQuery<EventAssociation> getAttachedUsers(Database db, @Database.Observes Event e, String status, boolean returnAll){
        Query q = getAttachedUsersQuery(db, e, status);
        return new DatabaseQuery<>(db, q, Database.Collections.EVENT_ASSOCIATIONS, null);
    }

    @Database.MustStir
    public static Query getAttachedUsersQuery(Database db, @Database.Observes Event e, String status){
        Filter f = Filter.equalTo(db.constants.getString(R.string.database_assoc_event), e.getDocumentID());
        if(status != null && !status.isBlank()){
            f = Filter.and(f, Filter.equalTo(db.constants.getString(R.string.database_assoc_status), status));
        }
        return Database.Collections.EVENT_ASSOCIATIONS
                .getCollection(db)
                .where(f)
                .orderBy(db.constants.getString(R.string.database_assoc_time), Query.Direction.DESCENDING);
    }

    @Database.MustStir
    public static DatabaseQuery<User> getUsers(Database db){
        Filter f = Filter.notEqualTo(FieldPath.documentId(), SyzygyApplication.SYSTEM_ACCOUNT_ID);
        Database.Collections c = Database.Collections.USERS;
        Query q = c.getCollection(db).where(f).orderBy(db.constants.getString(R.string.database_user_createdTime), Query.Direction.DESCENDING);
        return new DatabaseQuery<>(db, q, c, null);
    }

    @Database.MustStir
    public static DatabaseQuery<Event> getEvents(Database db){
        Filter f = Filter.and(); //TODO - does this work
        Database.Collections c = Database.Collections.EVENTS;
        Query q = c.getCollection(db).where(f).orderBy(db.constants.getString(R.string.database_event_createdTime), Query.Direction.DESCENDING);
        return new DatabaseQuery<>(db, q, c, null);
    }

    @Database.MustStir
    public static DatabaseQuery<Image> getImages(Database db){
        Filter f = Filter.notEqualTo(db.constants.getString(R.string.database_img_locID), SyzygyApplication.SYSTEM_ACCOUNT_ID);
        Database.Collections c = Database.Collections.IMAGES;
        Query q = c.getCollection(db).where(f).orderBy(db.constants.getString(R.string.database_img_uploadTime), Query.Direction.DESCENDING);
        return new DatabaseQuery<>(db, q, c, null);
    }

    /**
     * Tools to query for documents without loading them
     */
    public static class Util{
        private Util(){}

        /**
         * Gets the documents matching the query. Returns all results and does not load the instances
         * @param q The query to evaluate
         * @param listener The on complete listener
         */
        public static void getDocumentsFromQuery(Query q, BiConsumer<List<DocumentSnapshot>, Boolean> listener){
            q.get().addOnCompleteListener(t -> {
                if(!t.isSuccessful()){
                    listener.accept(null, false);
                    return;
                }
                listener.accept(t.getResult().getDocuments(), true);
            });
        }

        /**
         * Updates the field on each field
         * WARNING!! DOES NOT TEST THE VALIDITY OF THE VALUE
         * @param documents The documents to update
         * @param field The field to change
         * @param value The new value that the field should be
         * @param onComplete The listener, returns how many errors occured
         */
        public static void updateFieldOfDocuments(List<DocumentSnapshot> documents, String field, Object value, Consumer<Integer> onComplete){
            OnCompleteListener<Void> oc = new OnCompleteListener<Void>() {
                private int i = 0;
                private int err = 0;
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(!task.isSuccessful()){
                        err++;
                    }
                    i++;
                    if(i >= documents.size()){
                        onComplete.accept(err);
                    }
                }
            };
            if(documents.isEmpty()){
                onComplete.accept(0);
            }else{
                documents.forEach(d -> d.getReference().update(field, value).addOnCompleteListener(oc));
            }
        }

        /**
         * Deletes the documents from the database.
         * WARNING!!!
         * DOES NOT DELETE SUB INSTANCES
         * @param documents The documents to delete
         */
        public static void deleteDocuments(List<DocumentSnapshot> documents){
            documents.forEach(d -> d.getReference().delete());
        }
    }

    public enum Page{
        /**
         * @see #dissolve()
         */
        NULL,
        /**
         * @see #gotoFirstPage(Listener)
         */
        FIRST,
        /**
         * @see #gotoLastPage(Listener)
         */
        LAST,
        /**
         * @see #gotoNextPage(Listener)
         */
        NEXT,
        /**
         * @see #gotoPreviousPage(Listener)
         */
        PREVIOUS,
        /**
         * @see #gotoFirstPage(Listener)
         */
        FIRST_LAST
    }
}
