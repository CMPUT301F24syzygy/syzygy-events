package com.syzygy.events.database;

import android.content.ContentResolver;
import android.content.res.Resources;
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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firestore.v1.Write;
import com.syzygy.events.R;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The handler for the Firestore database
 * <p>
 *     Contains a reference to the database and listens for changes.
 * </p>
 * <p>
 *     Provides methods to retrieve data and update data within the database
 * </p>
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
    /**
     * If all created instances should be tracked
     */
    private final boolean trackCreatedInstances;
    private final List<DatabaseInstance<?>> trackedInstances = new ArrayList<>();

    public Database(@NonNull Resources constants){
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance().getReference();
        this.constants = constants;
        trackCreatedInstances = false;
    }

    public Database(@NonNull Resources constants, FirebaseFirestore db, StorageReference storage) {
        this.constants = constants;
        this.db = db;
        this.storage = storage;
        trackCreatedInstances = true;
    }

    public void setConstants(@NonNull Resources constants){
        this.constants = constants;
    }

    /**
     * Gets all tracked instances reference
     * @return A list if tracked instances is set to true
     */
    public List<DatabaseInstance<?>> getTrackedInstances(){
        return trackedInstances;
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
        instance.getDocumentReference().set(instance.getData());
    }

    /**
     * Updates the field in the instance given by the collection and documentID without loading the instance.
     * <p>
     *     Does not do any cascading.
     * </p>
     * @param collection The collection of the instance to update
     * @param documentId The documentID of the instance to update
     * @param propertyNameId The id of the property to update
     * @param newValue The new value to be put in the property
     * @param onComplete called on completion with if the update occurred successfully.
     */
    void modifyField(@NonNull Collections collection, @NonNull String documentId, int propertyNameId, Object newValue, Consumer<Boolean> onComplete){
        DocumentReference doc = collection.getDocument(this, documentId);
        doc.update(constants.getString(propertyNameId), newValue).addOnCompleteListener(task -> {
            onComplete.accept(task.isSuccessful());
        });
    }

    /**
     * Updates the field in all instances returned by the query without loading the instances
     * <p>
     *     Does not do any cascading.
     * </p>
     * @param q The query which returns all documents that should be updated
     * @param propertyNameId The id of the property to update
     * @param newValue The new value to be put in the property
     * @param onComplete called on completion with if the update occurred successfully.
     */
    void bulkModifyField(Query q, int propertyNameId, Object newValue, Consumer<Boolean> onComplete){
        WriteBatch b = db.batch();
        String prop = constants.getString(propertyNameId);
        q.get().addOnCompleteListener(t -> {
            if(!t.isSuccessful()){
                onComplete.accept(false);
                return;
            }
            t.getResult().getDocuments().forEach(d -> {
                b.update(d.getReference(), prop, newValue);
            });
            b.commit().addOnCompleteListener(t2 -> onComplete.accept(t2.isSuccessful()));
        });
    }

    /**
     * Retrieves the document properties of the instance and updates the instance to match
     * @param instance The database instance
     * @param <T> The instance type
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     * @see DatabaseInstance#updateDataFromDatabase(Map, Consumer)
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
     * Creates the image then calls the method. One completion of both or failure of one, calls the onComplete.
     * Dissolves the image before calling onComplete.
     * All data is passed from oldImage to newImage. If oldImage is null, then the data passed as arguments is used
     * @param newImage The image to create and set
     * @param oldImage The previous image that will be deleted on the successful set of the new image
     * @param locName The name of the instance that will contain the image if oldImage is null
     * @param collection The collection of the instance that will contain the image if oldImage is null
     * @param documentID The documentId of the instance that will contain the image if oldImage is null
     * @param updateVariable The method to call after creating the image. This method should update the variable to the new image
     *               The consumer must be called with if the setting succeeded.
     *               If false is returned, deletes the new image.
     *               If true is returned, deletes the old image
     *               Any data that should be passed to the onComplete method can be given by the first param.
     *               Success should be passed in the second param.
     * @param onComplete The method to call after all has succeeded.
     *                   The data returned by the method will be passed in the first argument.
     * @param <W> The type of data to be passed from the method to the onComplete
     */
    @StirsDeep(what = "The image given to the method")
    public <W> void replaceImage(@Nullable Uri newImage, @Nullable @Stirs(when = "if new image is set successfully") Image oldImage, String locName, Collections collection, String documentID, BiConsumer<Image, BiConsumer<W, Boolean>> updateVariable, BiConsumer<W, Boolean> onComplete){
        if(newImage == null && oldImage == null){
            updateVariable.accept(null, onComplete);
            return;
        }

        if(oldImage == null){
            createImageAndThen(newImage, locName, collection, documentID, updateVariable, onComplete);
            return;
        }

        replaceImage(newImage, oldImage, updateVariable, onComplete);
    }

    /**
     * Creates the image then calls the method. One completion of both or failure of one, calls the onComplete.
     * Dissolves the image before calling onComplete.
     * All data is passed from oldImage to newImage.
     * @param newImage The image to create and set
     * @param oldImage The previous image that will be deleted on the successful set of the new image
     * @param updateVariable The method to call after creating the image. This method should update the variable to the new image
     *               The consumer must be called with if the setting succeeded.
     *               If false is returned, deletes the new image.
     *               If true is returned, deletes the old image
     *               Any data that should be passed to the onComplete method can be given by the first param.
     *               Success should be passed in the second param.
     * @param onComplete The method to call after all has succeeded.
     *                   The data returned by the method will be passed in the first argument.
     * @param <W> The type of data to be passed from the method to the onComplete
     */
    @StirsDeep(what = "The image given to the method")
    public <W> void replaceImage(@Nullable Uri newImage, @NonNull @Stirs(when = "if new image is set successfully") Image oldImage, BiConsumer<Image, BiConsumer<W, Boolean>> updateVariable, BiConsumer<W, Boolean> onComplete){
        oldImage.fetch();
        if(newImage == null){
            Log.println(Log.DEBUG, "UpdateVariable", "Before Remove");
            updateVariable.accept(null, (passData, success) -> {
                Log.println(Log.DEBUG, "UpdateVariable", success+" Remove");
                if(success){
                    oldImage.deleteInstance(DatabaseInstance.DeletionType.REPLACEMENT, s -> {
                        if (!s) {
                            Log.println(Log.ERROR, "ReplaceImage", "Hanging image");
                        }
                    });
                    onComplete.accept(passData, true);
                    return;
                }
                oldImage.dissolve();
                onComplete.accept(passData, false);
            });
            return;
        }
        Log.println(Log.DEBUG, "UpdateVariable", "Before Create");
        this.createImageAndThen(newImage, oldImage.getLocName(), oldImage.getLocType(), oldImage.getDatabaseID(), updateVariable, (passData, success) -> {
            Log.println(Log.DEBUG, "UpdateVariable", success+" Create");
            if(success){
                oldImage.deleteInstance(DatabaseInstance.DeletionType.REPLACEMENT, s -> {
                    if (!s) {
                        Log.println(Log.ERROR, "ReplaceImage", "Hanging image");
                    }
                });
                onComplete.accept(passData, true);
                return;
            }
            oldImage.dissolve();
            onComplete.accept(passData, false);
        });
    }

    /**
     * Creates the image then calls the method. One completion of both or failure of one, calls the onComplete.
     * Dissolves the image before calling onComplete
     * @param image The image to create
     * @param locName The name of the instance that will contain the image
     * @param collection The collection of the instance that will contain the image
     * @param documentID The documentId of the instance that will contain the image
     * @param method The method to call after creating the image.
     *               The consumer must be called with if the method succeeded.
     *               If false if returned, deletes the image.
     *               Any data that should be passed to the onComplete method can be given by the first param.
     *               Success should be passed in the second param
     * @param onComplete The method to call after all has succeeded.
     *                   The data returned by the method will be passed in the first argument.
     * @param <W> The type of data to be passed from the method to the onComplete
     */
    @Observes
    @StirsDeep(what = "The image given to the method")
    public <W> void createImageAndThen(@NonNull Uri image, String locName, Database.Collections collection, String documentID, BiConsumer<Image, BiConsumer<W, Boolean>> method, BiConsumer<W, Boolean> onComplete){
        Image.NewInstance(this, locName, collection, documentID, image, (img, img_success) -> {
            Log.println(Log.DEBUG, "CreateImage", "called");
            if(!img_success){
                onComplete.accept(null, false);
                return;
            }
            method.accept(img, (passData, m_success) -> {
                if(m_success){
                    img.dissolve();
                    onComplete.accept(passData, true);
                }else{
                    String img_id = img.getDocumentID();
                    img.deleteInstance(DatabaseInstance.DeletionType.ERROR, s -> {
                        if(!s) {
                            Log.println(Log.ERROR, "CreateInstance", "Hanging image: " + img_id);
                        }
                    });
                    onComplete.accept(passData, false);
                }
            });
        });
    }

    /**
     * Validates the information and creates a new instance of the collection and the associated image.
     * <p>
     *     If one or more properties are invalid, does not create the instance, and the listener is not called.
     * </p>
     * <p>
     *     Creates the image, if the image is created successfully, creates the instance.
     * </p>
     * @param collection The collection to which the instance belongs
     * @param documentID The documentID of the instance
     * @param data The data to initialize the instance with (resId -> value)
     * @param image The uri of the image
     * @param locName The name of the location where the image is stored
     * @param listener The initialization listener.
     *                 The listener will be called once the instance is populated. If the documentID
     *                 already exists in the database, the listener is called with {@code success = false}
     *                 and the instance in an illegal state.
     *                 The listener is called before the database is updated with information
     * @param <T> The type of instance
     * @return The invalid property ids
     * @see #createImageAndThen(Uri, String, Collections, String, BiConsumer, BiConsumer) 
     * @see #createNewInstance(Collections, String, Map, InitializationListener)
     */
    @MustStir
    public <T extends DatabaseInstance<T>> Set<Integer> createNewInstance(Collections collection, String documentID, @Dilutes Map<Integer, Object> data , @Nullable Uri image, String locName, InitializationListener<T> listener){
        Set<Integer> invalidProperties = DatabaseInstance.isDataValid(data, collection);
        if(!invalidProperties.isEmpty()){
            return invalidProperties;
        }

        if(image != null){
            this.createImageAndThen(image, locName, collection, documentID, (img, returnSuccess) -> {
                data.put(collection.getAssociatedImagePropertyId(), img.getDocumentID());
                Set<Integer> ids = this.createNewInstance(collection, documentID, data, returnSuccess::accept);
                if(!ids.isEmpty()){
                    returnSuccess.accept(null, false);
                };
            }, listener::onInitialization);
            return invalidProperties;
        }else{
            data.put(collection.getAssociatedImagePropertyId(), "");
            return this.createNewInstance(collection, documentID, data, listener);
        }
    }

    /**
     * Validates the information and creates a new instance of the collection.
     * <p>
     *     If one or more properties are invalid, does not create the instance, and the listener is not called.
     * </p>
     * @param collection The collection to which the instance belongs
     * @param documentID The documentID of the instance
     * @param data The data to initialize the instance with (resid -> value)
     * @param listener The initialization listener.
     *                 The listener will be called once the instance is populated. If the documentID
     *                 already exists in the database, the listener is called with {@code success = false}
     *                 and the instance in an illegal state.
     *                 The listener is called before the database is updated with information
     * @param <T> The type of instance
     * @return The set of invalid properties.
     */
    @SuppressWarnings("unchecked")
    @MustStir
    public <T extends DatabaseInstance<T>> Set<Integer> createNewInstance(Collections collection, String documentID, @Dilutes Map<Integer, Object> data, InitializationListener<T> listener){
        Set<Integer> invalidProperties = DatabaseInstance.isDataValid(data, collection);
        if(!invalidProperties.isEmpty()) {
            Log.println(Log.DEBUG, "NewInstance", "invalid properties");
            for(int i : invalidProperties){
                Log.println(Log.DEBUG, "NewInstance", "\t"+constants.getString(i));
            }
            return invalidProperties;
        }

        DatabaseInstance<T> instance = (DatabaseInstance<T>) cache.computeIfAbsent(collection.getDatabaseID(documentID), k -> collection.newInstance(this, documentID));
        instance.getDocumentReference().get().addOnCompleteListener(task -> {
            if(!task.isSuccessful()) return;
            DocumentSnapshot doc = task.getResult();
            if(doc.exists()){
                instance.fullDissolve();
                listener.onInitialization(instance.cast(), false);
            }else{
                instance.addInitializationListener(listener);
                instance.initializeData(convertIDMapToNames(data), true, (instance1, success) -> {
                    if(success)
                        addToDatabase(instance);
                    //TODO error
                });
            }
        });
        return invalidProperties;
    }

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
    public void deleteFile(String fileName, Consumer<Boolean> listener){
        StorageReference ref = storage.child(fileName);
        ref.delete().addOnCompleteListener(runnable -> {
            listener.accept(runnable.isSuccessful());
        });
    }


    @Override
    public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {

        if(error != null || value == null) {
            return;
        }

        DatabaseInstance<?> instance = cache.get(value.getReference().getPath());

        if(instance == null || !instance.isLegalState()){
            return;
        }

        if(!value.exists()){
            instance.deleteInstance(DatabaseInstance.DeletionType.FROM_DATABASE, s->{});
        }else{
            instance.updateDataFromDatabase(value.getData(), s->{});
        }
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
    public void cleanup(){

    };


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
        public String getDatabaseID(@Observes DatabaseInstance<?> instance){
          return getDatabaseID(instance.getDocumentID());
        };

        /**
         * Returns a unique identifier for the instance within the full database
         * @param documentID The unique identifier of the instance within the collection
         * @return The unique database identifier
         */
        public String getDatabaseID(String documentID){
            return dbIdentifier + '/' + documentID;
        };

        /**
         * Gets the instance id from the database id
         * @param databaseID The unique identifier of the instance within the database
         * @return The unique identifier of the instance within the collection
         */
        public String instanceIDFromDatabaseID(String databaseID){
            return databaseID.replaceFirst(dbIdentifier, "");
        };

        /**
         * Returns the document ID
         * @return The document ID
         */
        public String getCollectionID(){
            return dbIdentifier;
        }

        /**
         * Returns the database collection object
         * @param db The database
         * @return The {@code CollectionReference} for the collection
         */
        public CollectionReference getCollection(Database db){
            return db.db.collection(dbIdentifier);
        }

        /**
         * Gets the document within the collection. If no document exists, creates a new document.
         * @param db The database
         * @param documentID The document identifier within the collection
         * @return The {@code DocumentReference} for the document
         */
        public DocumentReference getDocument(Database db, String documentID) {
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
            DatabaseInstance<T> inst;
            switch(this){
                case USERS:
                    inst = (DatabaseInstance<T>) new User(db, id); break;
                case EVENTS:
                    inst = (DatabaseInstance<T>) new Event(db, id); break;
                case IMAGES:
                    inst = (DatabaseInstance<T>) new Image(db, id); break;
                case FACILITIES:
                    inst = (DatabaseInstance<T>) new Facility(db, id); break;
                case NOTIFICATIONS:
                    inst = (DatabaseInstance<T>) new Notification(db, id); break;
                case EVENT_ASSOCIATIONS:
                    inst = (DatabaseInstance<T>) new EventAssociation(db, id); break;
                default:
                    throw new IllegalStateException("All cases covered, so can't reach this point");
            }
            if(db.trackCreatedInstances){
                db.trackedInstances.add(inst);
            }
            return inst;
        };

        public static final int DOES_NOT_HAVE_ASSOCIATED_IMAGE = -1,
                                IS_ITS_OWN_ASSOCIATED_IMAGE = -2;

        /**
         * Returns the property id of the associated image for a instance of this collection.
         * If this collection does not have associated images, returns {@code -1}.
         * If the instance is itself the associated image, returns {@code -2}
         */
        public int getAssociatedImagePropertyId() {
            switch(this){
                case USERS:
                    return R.string.database_user_profileID;
                case EVENTS:
                    return R.string.database_event_posterID;
                case IMAGES:
                    return IS_ITS_OWN_ASSOCIATED_IMAGE;
                case FACILITIES:
                    return R.string.database_fac_imageID;
                case NOTIFICATIONS:
                case EVENT_ASSOCIATIONS:
                default:
                    return DOES_NOT_HAVE_ASSOCIATED_IMAGE;
            }
        }
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
            /**
             * A property of a subinstance was updated
             */
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
            DEREFERENCED;
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

        void refreshData(Listener<T> listener);

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


            public QueryResult(@Observes V result) {
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
    public @interface Dissolves { }

    /**
     * Represents a class which must call {@code .dissolve} when no longer used
     */
    @Dissolves
    public interface Dissolvable {
        void dissolve();
    }

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
