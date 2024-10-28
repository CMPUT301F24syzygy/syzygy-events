package com.syzygy.events.database;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The handler for the Firestore database
 * <p>
 *     Contains a reference to the database and listens for changes.
 * </p>
 * <p>
 *     Provides methods to retrieve data and update data within the database
 * </p>
 * @author Gareth Kmet
 * @version 1.0
 * @since 19oct24
 */
public class Database implements EventListener<DocumentSnapshot> {

    /**
     * Set of listeners which are called whenever an error occurs
     */
    private final List<Consumer<RuntimeException>> errorListeners = new ArrayList<>();

    /**
     * The currently retrieved instances
     * <p>
     *     Each instance is identified by their database id
     * </p>
     * @see DatabaseInstance#getDatabaseID()
     */
    private final HashMap<String, DatabaseInstance<?>> cache = new HashMap<>();

    @NonNull Resources constants;

    /**
     * The firestore database
     */
    private final FirebaseFirestore db;
    /**
     * The firebase storage for images
     */
    private final StorageReference storage;

    public Database(@NonNull Resources constants){
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance().getReference();
        this.constants = constants;
    }

    public void setConstants(@NonNull Resources constants){
        this.constants = constants;
    }

    /**
     * Deletes the instance from cache
     * @param instance The instance that should be removed from cache
     * @param <T> The type of the instance
     * @throws IllegalStateException If the instance still has references
     */
    <T extends DatabaseInstance<T>> void returnInstance(@Observes DatabaseInstance<T> instance) throws IllegalStateException{
        if(instance.isReferenced()) throw new IllegalStateException("Instance is still referenced: " + instance.toString());
        cache.remove(instance.getDatabaseID());
    }

    /**
     * Updates the database to match the instance
     * @param instance The database instance
     * @param <T> The instance type
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     * @see DatabaseInstance#getDocumentReference()
     * @see DatabaseInstance#getData()
     */
    <T extends DatabaseInstance<T>> void updateDatabase(@Observes DatabaseInstance<T> instance) throws IllegalStateException{
        // TODO might be double notifiying
        instance.getDocumentReference().set(instance.getData());
    }

    /**
     * Removes the instance from the database
     * @param instance The database instance
     * @param <T> The instance type
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     * @see DatabaseInstance#getDocumentReference()
     */
    <T extends DatabaseInstance<T>> void deleteFromDatabase(@Observes DatabaseInstance<T> instance) throws IllegalStateException{
        // TODO might be double notifiying
        instance.getDocumentReference().delete();
    }

    /**
     * Adds the instance to the database
     * @param instance The database instance
     * @param <T> The instance type
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     * @see DatabaseInstance#getDocumentReference()
     * @see DatabaseInstance#getData()
     */
    <T extends DatabaseInstance<T>> void addToDatabase(@Observes DatabaseInstance<T> instance) throws IllegalStateException{
        // TODO might be double notifiying
        instance.getDocumentReference().set(instance.getData());
    }

    /**
     * Retrieves the document properties of the instance and updates the instance to match
     * @param instance The database instance
     * @param <T> The instance type
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     * @see DatabaseInstance#updateDataFromDatabase(Map, Querrier.EmptyListener)
     * @see DatabaseInstance#getDocumentReference()
     */
    <T extends DatabaseInstance<T>> void updateFromDatabase(@Observes DatabaseInstance<T> instance) throws IllegalStateException{
        instance.getDocumentReference().get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(!task.isSuccessful()) return; // TODO error
                DocumentSnapshot doc = task.getResult();
                if(!doc.exists()) return; //TODO error
                instance.updateDataFromDatabase(task.getResult().getData(), s ->{
                    if(!s){
                        return; // TODO error
                    }
                });
            }
        });
    }

    /**
     * Retrieves the document properties of the instance and sets the instance to match
     * @param instance The database instance
     * @param <T> The instance type
     * @param onComplete called when complete
     * @throws IllegalStateException if the instance is already initialized
     * @see DatabaseInstance#initializeData(Map, boolean, InitializationListener) 
     * @see DatabaseInstance#getDocumentReference()
     */
    <T extends DatabaseInstance<T>> void initializeFromDatabase(@Observes DatabaseInstance<T> instance, InitializationListener<T> onComplete) throws IllegalStateException{
        instance.getDocumentReference().get().addOnCompleteListener(task -> {
            if(!task.isSuccessful()) {
                Log.println(Log.DEBUG, "failedGet", instance.getDocumentID());
                onComplete.onInitialization(null, false);
                return; // TODO error
            }
            Log.println(Log.DEBUG, "successGet", instance.getDocumentID());
            DocumentSnapshot doc = task.getResult();
            instance.initializeData(doc.getData(), doc.exists(), onComplete);
        });
    }

    /**
     * Initializes and instance using the data in the given snapshot
     * @param instance The instance
     * @param snapshot The document snapshot
     * @param <T> The instance type
     * @param onComplete called when complete
     * @throws IllegalStateException If the instance is already initialized
     * @throws IllegalArgumentException If the document does not match the instance
     * @see DatabaseInstance#initializeData(Map, boolean, InitializationListener)
     */
    <T extends DatabaseInstance<T>> void initializeFromDatabase(@Observes DatabaseInstance<T> instance, DocumentSnapshot snapshot, InitializationListener<T> onComplete) throws IllegalStateException, IllegalArgumentException{
        if(!Objects.equals(snapshot.getId(), instance.getDocumentID())) throwE(new IllegalArgumentException("Snapshot id does not match instance id: "+snapshot.getId()+"|"+instance.getDocumentID()));
        instance.initializeData(snapshot.getData(), snapshot.exists(), onComplete);
    }

    /**
     * Returns the {@link DatabaseInstance} of the document within the collection.
     * <p>
     *     If the {@code DatabaseInstance} already exists in the cache, the cache object reference
     *     count is increased, and the object is returned
     * </p>
     * <p>
     *     If the {@code DatabaseInstance} does not exists in the cache, a new instance is retrieved
     *     from the database. The instance is added to the database
     * </p>
     * If provided, the {@code }initializeListener} will be triggered once the instance has been
     * populated.
     * <p>
     *     <b>Important</b>: The returned instance should not be used until the {@code InitializationListener} is called
     * </p>
     * <p>
     *     If the id does not exists, the listener will be called with {@code success = false} and in
     *     an illegal state
     * </p>
     * @param collection The collection that this instance resides in
     * @param documentID The id of the instance within the collection
     * @param <T> The type of instance
     * @see InitializationListener
     * @see Collections#newInstance(Database, String)
     */
    @MustStir
    public <T extends DatabaseInstance<T>> void getInstance(Collections collection, String documentID, InitializationListener<T> listener){
        getInstance(collection, documentID, listener, null);
    };

    /**
     * Returns the {@link DatabaseInstance} of the document within the collection given the document snapshot.
     * <p>
     *     If the {@code DatabaseInstance} already exists in the cache, the cache object reference
     *     count is increased, and the object is returned
     * </p>
     * <p>
     *     If the {@code DatabaseInstance} does not exists in the cache, a new instance is created using the given document.
     *     The instance is added to the database
     * </p>
     * If provided, the {@code }initializeListener} will be triggered once the instance has been
     * populated.
     * <p>
     *     <b>Important</b>: The returned instance should not be used until the {@code InitializationListener} is called
     * </p>
     * <p>
     *     If the id does not exists, the listener will be called with {@code success = false} and in
     *     an illegal state
     * </p>
     * @param collection The collection that this instance resides in
     * @param documentID The id of the instance within the collection
     * @param <T> The type of instance
     * @throws IllegalArgumentException if the instance must be created and the document does not match the instance
     * @see InitializationListener
     * @see Collections#newInstance(Database, String)
     */
    @SuppressWarnings("unchecked")
    @MustStir
    public <T extends DatabaseInstance<T>> void getInstance(Collections collection, String documentID, InitializationListener<T> listener, @Nullable DocumentSnapshot document) throws IllegalArgumentException{
        //TODO deal with no exists
        String databaseId = collection.getDatabaseID(documentID);

        DatabaseInstance<T> instance = (DatabaseInstance<T>)cache.get(databaseId);
        if(instance!=null){
            Log.println(Log.DEBUG, "foundInstance", documentID + " " + collection.toString());
            instance.addInitializationListener(listener);
            return;
        }
        Log.println(Log.DEBUG, "computeNewInstance", documentID + " " + collection.toString());
        DatabaseInstance<T> inst = collection.newInstance(this, documentID);
        cache.put(databaseId, inst);

        if(document != null){
            Log.println(Log.DEBUG, "computeFromSnapshot", documentID + " " + collection.toString());

            initializeFromDatabase(inst,document, (i, s) -> {
                if(!s){
                    Log.println(Log.DEBUG, "failedCompute", documentID + " " + collection.toString());
                    cache.remove(databaseId);
                    listener.onInitialization(null, false);
                    return;
                }
                Log.println(Log.DEBUG, "goodCompute", documentID + " " + collection.toString());
                inst.addInitializationListener(listener);
            });
        }else{
            Log.println(Log.DEBUG, "computeFromDatabase", documentID + " " + collection.toString());
            initializeFromDatabase(inst, (i,s)->{
                if(!s){
                    Log.println(Log.DEBUG, "failedCompute", documentID + " " + collection.toString());
                    cache.remove(databaseId);
                    listener.onInitialization(null, false);
                    return;
                }
                Log.println(Log.DEBUG, "goodCompute", documentID + " " + collection.toString());
                inst.addInitializationListener(listener);
            });
        }
    };

    /**
     * Gets an instance from the current cache
     * @param collection The collection of the instance
     * @param documentID The documentID of the instance
     * @return The instance from cache. If the instance is not in cache, returns {@code null}
     * @param <T> The type of the instance
     */
    @SuppressWarnings("unchecked")
    @MustStir
    public <T extends DatabaseInstance<T>> T getInstanceFromCache(Collections collection, String documentID) {
        DatabaseInstance<T> val = (DatabaseInstance<T>) cache.get(collection.getDatabaseID(documentID));
        if(val == null){
            return null;
        }else{
            return val.fetch();
        }
    };

    /**
     * Creates a new instance of the collection
     * <p>
     *      <b>Important</b>: The returned instance should not be used until the {@code InitializationListener} is called
     * </p>
     * @param collection The collection to which the instance belongs
     * @param documentID The documentID of the instance
     * @param data The data to initialize the instance with
     * @param listener The initialization listener.
     *                 The listener will be called once the instance is populated. If the documentID
     *                 already exists in the database, the listener is called with {@code success = false}
     *                 and the instance in an illegal state.
     *                 The listener is called before the database is updated with information
     * @param <T> The type of instance
     */
    @SuppressWarnings("unchecked")
    @MustStir()
    public <T extends DatabaseInstance<T>> void createNewInstance(Collections collection, String documentID, Map<String, Object> data, InitializationListener<T> listener){
        DatabaseInstance<T> instance = (DatabaseInstance<T>) cache.computeIfAbsent(collection.getDatabaseID(documentID), k -> collection.newInstance(this, documentID));
        instance.getDocumentReference().get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(!task.isSuccessful()) return;
                DocumentSnapshot doc = task.getResult();
                if(doc.exists()){
                    instance.fullDissolve();
                    listener.onInitialization(instance.cast(), false);
                }else{
                    instance.addInitializationListener(listener);
                    instance.initializeData(data, true, (instance1, success) -> {
                        if(success)
                            addToDatabase(instance);
                        //TODO error
                    });
                }
            }
        });
    };

    /**
     * Converts an ID property map to a Name property map
     * @param data The resID-value map where resID is the res id to the property name
     * @return The name-value map where name is the name of the property
     */
    public Map<String,Object> convertIDMapToNames(Map<Integer,Object> data){
        Map<String,Object> map = new HashMap<>();
        for(Map.Entry<Integer,Object> ent : data.entrySet()){
            map.put(constants.getString(ent.getKey()), ent.getValue());
        }
        return map;
    }

    /**
     * Adds the file to the database;
     * @param fileName The filename
     * @param file The file
     * @param listener Called on completion. null if upload failed, otherwise download url
     */
    public void addFileToStorage(String fileName, Uri file, Consumer<Uri> listener){
        Log.println(Log.DEBUG, "add file", fileName);
        StorageReference ref = storage.child(fileName);
        ref.putFile(file).addOnCompleteListener(task -> {
            if(!task.isSuccessful()){
                listener.accept(null);
                return;
            }
            ref.getDownloadUrl().addOnCompleteListener(task1 -> {
                if(!task.isSuccessful()){
                    ref.delete().addOnFailureListener(task2 -> {
                        Log.println(Log.ERROR, "image", "hanging image " + fileName);
                    });
                    listener.accept(null);
                }
                listener.accept(task1.getResult());
            });
        });
    }
    @Deprecated
    private String getFileExtension(Uri fileUri, ContentResolver contentResolver){
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(contentResolver.getType(fileUri));
    }

    /**
     * Deletes the image from the storage
     * @param fileName The filename including extension (e.g. `folder/image.jpg`)
     * @param listener Called on completion. true if the deletion was successful
     */
    public void deleteImage(String fileName, Consumer<Boolean> listener){
        StorageReference ref = storage.child(fileName);
        ref.delete().addOnCompleteListener(runnable -> {
            listener.accept(runnable.isSuccessful());
        });
    }


    @Override
    public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
        if(error != null || value == null || !value.exists()) {
            //TODO
            return;
        }
        DatabaseInstance<?> instance = cache.get(value.getId());
        if(instance == null){
            //TODO
            return;
        }

        instance.updateDataFromDatabase(value.getData(), s->{
            //TODO on error
        });
    }

    /**
     * Handles an error caused by the database
     * @param ex the exception
     */
    void throwE(RuntimeException ex){
        for(Consumer<RuntimeException> l : errorListeners){
            l.accept(ex);
        }
        throw ex;
    }

    /**
     * Adds an error listener. This listener is called whenever an error occurs
     * @param listener The listener
     */
    public void addErrorListener(Consumer<RuntimeException> listener){
        this.errorListeners.add(listener);
    }

    /**
     * Called to cleanup the database
     */
    public void cleanup(){};


    /**
     * Contains the different collections stored in the database;
     */
    public enum Collections {
        USERS,
        EVENTS,
        FACILITIES,
        NOTIFICATIONS,
        IMAGES,
        EVENT_ASSOCIATIONS;

        /**
         * The Firestore identifier for the collection
         */
        private final String dbIdentifier;
        private Collections(){
            this.dbIdentifier = toString().toLowerCase();
        }


        /**
         * Returns a unique identifier for the instance within the full database
         * @param instance The database instance
         * @return The unique database identifier
         */
        String getDatabaseID(@Observes DatabaseInstance<?> instance){
          return getDatabaseID(instance.getDocumentID());
        };

        /**
         * Returns a unique identifier for the instance within the full database
         * @param documentID The unique identifier of the instance within the collection
         * @return The unique database identifier
         */
        String getDatabaseID(String documentID){
            return dbIdentifier + '/' + documentID;
        };

        /**
         * Gets the instance id from the database id
         * @param databaseID The unique identifier of the instance within the database
         * @return The unique identifier of the instance within the collection
         */
        String instanceIDFromDatabaseID(String databaseID){
            return databaseID.replaceFirst(dbIdentifier, "");
        };

        /**
         * Returns the document ID
         * @return The document ID
         */
        String getCollectionID(){
            return dbIdentifier;
        }

        /**
         * Returns the database collection object
         * @param db The database
         * @return The {@code CollectionReference} for the collection
         */
        CollectionReference getCollection(Database db){
            return db.db.collection(dbIdentifier);
        }

        /**
         * Gets the document within the collection. If no document exists, creates a new document.
         * @param db The database
         * @param documentID The document identifier within the collection
         * @return The {@code DocumentReference} for the document
         */
        DocumentReference getDocument(Database db, String documentID) {
            return getCollection(db).document(documentID);
        }

        /**
         * @param db The database
         * @return A new unique id for the collection
         */
        String getNewID(Database db){
            return getCollection(db).document().getId();
        }

        /**
         * Returns a new instance of the collection item with the id
         * @param db The database
         * @param id The document id
         * @return The new instance
         * @param <T> The instance type
         */
        @SuppressWarnings("unchecked")
        @NonNull
        @Salty
        <T extends DatabaseInstance<T>> DatabaseInstance<T> newInstance(Database db, String id) {
            switch(this){
                case USERS:
                    return (DatabaseInstance<T>) new User(db, id);
                case EVENTS:
                    return (DatabaseInstance<T>) new Event(db, id);
                case IMAGES:
                    return (DatabaseInstance<T>) new Image(db, id);
                case FACILITIES:
                    return (DatabaseInstance<T>) new Facility(db, id);
                case NOTIFICATIONS:
                    return (DatabaseInstance<T>) new Notification(db, id);
                case EVENT_ASSOCIATIONS:
                    return (DatabaseInstance<T>) new EventAssociation(db, id);
            }
          throw new IllegalStateException("All cases covered, so can't reach this point");
        };
    }

    /**
     * Listeners for whenever an instance from the database is updated or created/deleted
     * @author Gareth Kmet
     * @version 1.0
     * @since 19oct24
     */
    public interface UpdateListener {
        public enum Type {
            /**
             * A property of the instance has been modified
             */
            UPDATE,
            SUBUPDATE,
            /**
             * The instance was deleted
             */
            DELETE,
            /**
             * The instance was created
             */
            INIT,
            /**
             * The instance was deallocated from the cache
             */
            DEREFERENCED
        };
        public <T extends DatabaseInstance <T>> void onUpdate(@Observes DatabaseInstance<T> instance, Type type);
    }

    /**
     * Called when the {@code DatabaseInstance} is initialized and enters a valid state
     * @author Gareth Kmet
     * @version 1.0
     * @since 19oct24
     * @param <T> The class of the instance
     * @see DatabaseInstance#initializeData(Map, boolean, InitializationListener) 
     */
    public interface InitializationListener<T extends DatabaseInstance<T>> {
        /**
         * Called when the instance is initialized and in a legal state
         * @param instance the {@code DatabaseInstance}
         * @param success {@code true} if the instance was successfully initialized. {@code false}
         *                            if the document for the instance did not exists or other errors
         *                            occurred.
         * @see DatabaseInstance#initializeData(Map, boolean, InitializationListener) 
         */
        public void onInitialization(@MustStir T instance, boolean success);
    }

    /**
     * A class which querries the firestore database
     */
    public interface Querrier<T extends Querrier<T>>{

        public void refreshData(Listener<T> listener);

        /**
         * Listener that is called when a query finishes loading and processing data
         */
        interface EmptyListener{
            /**
             * Called when the query has completed loading and processing data
             */
            public void onCompletion(boolean success);
        }

        /**
         * Listener that is called when a query finishes loading data
         * @param <S> The type of the querrier
         */
        interface Listener<S extends Querrier<S>> {
            /**
             * Called when the query has completed loading data and now contains all the new loaded instances
             * @param query The query
             */
            public void onCompletion(@Observes S query, boolean success);
        }
        /**
         * Listener that is called when a query finishes loading data and returns data
         * @param <S> The type of the querrier
         * @param <W> The type of return data
         */
        interface DataListener<S extends Querrier<S>, W extends QueryResult<?>> {
            /**
             * Called when the query has completed getting the data
             * @param query The query
             * @param data The list of instances found by the query
             */
            public void onCompletion(@Observes S query, @Particulates W data, boolean success);
        }

        public class QueryResult<V> {
            public final V result;

            public QueryResult(V result) {
                this.result = result;
            }
        }

        /**
         * The result of a query
         */
        public class QueryInstanceResult<V extends DatabaseInstance<V>> extends QueryResult<List<V>>{

            public QueryInstanceResult(List<V> list) {
                super(java.util.Collections.unmodifiableList(list));
            }
        }
    }

    /**
     * Represents a class which must call {@code .dissolve} when no longer used
     */
    @Documented
    @Target(ElementType.TYPE)
    public @interface Dissovable { }

    /**
     * Represents a instance return that gives up ownership of the instance. The receiver must dissolve the instance once complete
     */
    @Documented
    @Target({ElementType.PARAMETER, ElementType.CONSTRUCTOR, ElementType.METHOD})
    public @interface MustStir { public String when() default "Always";}

    /**
     * Represents a instance return that keeps ownership of the instance. The receiver must fetch the instance for further use. The user does not need to dissolve
     */
    @Documented
    @Target({ElementType.METHOD})
    public @interface Stirred {public String when() default "Always"; }


    /**
     * Represents a instance method that will dissolves itself after calling the method. The receiver cannot use the instance any longer
     */
    @Documented
    @Target({ElementType.METHOD})
    public @interface AutoStir {public String when() default "Always"; }

    /**
     * Represents an instance that may need to be fetched or dissolved depending on the documentation by the original method
     */
    @Documented
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface Particulates { }

    /**
     * Represents a parameter instance that will be fetched by the method. Thus if the caller dissolve the argument after, the argument will still be cached
     */
    @Documented
    @Target({ElementType.PARAMETER})
    public @interface Dilutes {public String when() default "Always"; }

    /**
     * Represents that the method fetches certain instances
     */
    @Documented
    @Target({ElementType.METHOD})
    public @interface Titrates {public String what(); public String when() default "Always"; }

    /**
     * Represents a instance return that has not yet fetched the instance, the instance must be fetched by the caller otherwise the instance will be stuck in a broken state
     */
    @Documented
    @Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
    public @interface Salty { }

    /**
     * Represents a parameter instance that will be not fetched by the method. Thus if the caller dissolves the argument after, the method may fail. If the caller wishes to use the instance, they must fetch
     */
    @Documented
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    public @interface Observes { }

    /**
     * Represents an instance that will be dissolved by the method. The caller must fetch the instance before calling if they wish for further use
     */
    @Documented
    @Target({ElementType.PARAMETER})
    public @interface Stirs { public String when() default "Always";}

    /**
     * Represents an method call that will dissolve an object
     */
    @Documented
    @Target({ElementType.METHOD})
    public @interface StirsDeep { public String what(); public String when() default "Always";}

}
