package com.syzygy.events.database;

import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * An instance of a database item which should be updated whenever the database is updated.
 * @author Gareth Kmet
 * @version 1.0
 * @param <T> The class of the instance.
 * @since 19oct24
 *
 * TODO - Add error handling on instance exchanges
 * TODO - change on instance load, if null, update subdelete
 */
@Database.Dissovable
public abstract class DatabaseInstance<T extends DatabaseInstance<T>> implements Database.UpdateListener {
    /**
     * The number of references to this Instance
     */
    private int referenceCount = 0;
    /**
     * If this instance is dereferenced.
     * <p>
     *     If the instance is no longer referenced, it is no longer maintained and updated by the database.
     * </p>
     */
    private boolean isDereferenced = false;

    /**
     * If the instance has been initialized with data
     */
    private boolean isInitialized = false;

    /**
     * If the instance has been initialized with data
     */
    private boolean isInitializing = false;

    /**
     * If the instance has been deleted
     */
    private boolean isDeleted = false;

    /**
     * The database
     */
    protected final Database db;

    /**
     * A non-modifiable documentID that uniquely identifies the instance within its collection
     */
    private final String documentID;

    /**
     * The snapshot listener that is associated with the database. This listener is called whenever
     * the associated document is updated.
     */
    private ListenerRegistration snapshotListener = null;

    /**
     * The database collection which the instance belongs to
     */
    private final Database.Collections collection;

    /**
     * The set of properties that don't need to load
     */
    protected final Map<String, PropertyWrapper<?, ?>> properties = new HashMap<>();
    /**
     * The set of properties that need to load
     */
    protected final List<String> iproperties = new ArrayList<>();


    /**
     * The set of listeners.
     * Whenever the data of this instance changes, each listener is notified
     * @see Database.UpdateListener
     */
    private final Set<Database.UpdateListener> updateListeners = new HashSet<>();

    /**
     * A set of listeners which are called when the instance is initialized
     * @see Database.InitializationListener
     * @see #initializeData(Map, boolean, Database.InitializationListener)
     */
    private final Set<Database.InitializationListener<T>> initializationListeners = new HashSet<>();



    /**
     * Checks to make sure the generic type is the type of this instance
     *
     * @param db The database
     * @param documentID the unique identifier of the instance within its collection
     * @param collection the database collection to which the instance belongs
     * @throws ClassCastException If the generic type is not the type of this instance
     * @throws IllegalArgumentException if the properties are empty
     *
     * @see Database.Collections
     * @see DatabaseInstance#documentID
     */
    @SuppressWarnings("unchecked")
    @Database.Salty
    protected DatabaseInstance(Database db, String documentID, Database.Collections collection, @NonNull PropertyField<?,?>[] properties) throws ClassCastException, IllegalArgumentException{
        T t = (T)this;
        if(properties.length == 0) db.throwE(new IllegalArgumentException("Properties is empty: " + collection));
        this.db = db;
        this.documentID = documentID;
        this.collection = collection;
        for(PropertyField<?,?> prop : properties){
            String name = db.constants.getString(prop.propertyNameID);

            this.properties.put(name, prop.getWrapper());
            if(prop.loads){
                this.iproperties.add(name);
            }
        }
    }

    /**
     * Gets this instance casted to the generic type
     * @return This as the generic type
     */
    @Database.Observes
    protected abstract T cast();

    /**
     * Returns the image associated with this instance. Null if no image
     */
    @Nullable
    @Database.Observes
    public final Image getAssociatedImage(){
        int id = collection.getAssociatedImagePropertyId();
        if(id == Database.Collections.DOES_NOT_HAVE_ASSOCIATED_IMAGE) return null;
        if(id == Database.Collections.IS_ITS_OWN_ASSOCIATED_IMAGE) return (Image)this;
        return getPropertyInstanceI(id);
    }

    /**
     * Returns the name that should be given to the image instance. If no image is associated, returns null
     */
    public String getAssociatedImageLocName() {return null;}

    /**
     * Adds a new update listener to this instance
     * <p>
     * If the same listener is added twice, the listener will only be notified of an update once
     * @param listener The listener
     * @see Database.UpdateListener
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     */
    public final void addListener(@Nullable Database.UpdateListener listener) throws IllegalStateException{
        assertNotIllegalState();
        if(listener == null) return;
        updateListeners.add(listener);
    }

    /**
     * Removes a listener
     * @param listener The listener
     * @return If the set of listeners changed as a result
     */
    public final boolean removeListener(@NonNull Database.UpdateListener listener){
        return updateListeners.remove(listener);
    }

    /**
     * Adds a new initialization listener to this instance
     * <p>
     * If the same listener is added twice, the listener will only be notified of an update once
     * <p>
     *     If the instance is already initialized, the listener is called immediately
     * </p>
     * @param listener The listener
     * @see Database.InitializationListener
     * @throws IllegalStateException if the instance is dereferenced
     */
    @Database.MustStir
    public final void addInitializationListener(@Nullable Database.InitializationListener<T> listener) throws IllegalStateException{
        Log.println(Log.DEBUG, "add init listener", getDocumentID() + " " + getCollection() + " " + isInitialized + " " + isInitializing);
        if(listener == null) return;
        if(isInitialized){
            assertNotIllegalState();
            listener.onInitialization(fetch(), true);
        }else if(isInitializing){
            listener.onInitialization(fetchWithoutThrow(), true);
        }else{
            initializationListeners.add(listener);
        }
    }

    /**
     * Notifies each listener
     * @param type The type of update that occurred
     * @see Database.UpdateListener
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     */
    protected final void notifyUpdate(Database.UpdateListener.Type type) throws IllegalStateException{
        updateListeners.forEach(l -> l.onUpdate(cast(), type));
    }

    /**
     * Increases the reference count of this instance.
     * @return This instance casted to the generic type
     */
    @Database.MustStir
    private T fetchWithoutThrow() {
        referenceCount ++;
        Log.println(Log.DEBUG, "reference", getDocumentID() + " " + getCollection() + " " + referenceCount);
        return cast();
    }

    /**
     * Increases the reference count of this instance.
     * @return This instance casted to the generic type
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     */
    @Database.MustStir
    public T fetch() throws IllegalStateException{
        assertNotIllegalState();
        return fetchWithoutThrow();
    }

    /**
     * Increases the reference count of this instance and attaches the listener
     * @param listener the update listener
     * @return This instance casted to the generic type
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     */
    @Database.MustStir
    public T fetch(Database.UpdateListener listener) throws IllegalStateException{
        assertNotIllegalState();
        fetch();
        addListener(listener);
        return cast();
    }

    /**
     * Decreases the reference count of this instance
     */
    @Database.AutoStir
    @Database.StirsDeep(what="Property Instances", when="No Longer Referenced")
    public void dissolve(){
        if(!isReferenced()) return;
        referenceCount = Math.max(referenceCount - 1, 0);
        Log.println(Log.DEBUG, "dissolve", getDocumentID() + " " + getCollection().toString() + " " + referenceCount);
        if(!isReferenced()) dereferenceInstance();
    }

    /**
     * Removes the listener from the instance and decreases the reference
     * @param listener The listener
     */
    @Database.AutoStir
    @Database.StirsDeep(what="Property Instances", when="No Longer Referenced")
    public void dissolve(Database.UpdateListener listener){
        removeListener(listener);
        dissolve();
    }

    /**
     * Decreases the reference count to zero
     */
    @Database.AutoStir
    @Database.StirsDeep(what="Property Instances")
    public void fullDissolve() {
        referenceCount = 0;
        dereferenceInstance();
    }

    /**
     * Checks if the reference count is positive
     * @return {@code true} if one or more objects are referencing this instance
     */
    protected final boolean isReferenced(){
        return referenceCount > 0 && !isDereferenced;
    }

    /**
     * Returns if the object is in a legal state
     * @return If the object is in a legal state
     * @see #assertNotIllegalState()
     */
    public final boolean isLegalState(){
        return !isDereferenced && isInitialized && !isDeleted;
    }

    /**
     * Checks if the instance is in a legal state
     * <p>
     *     The instance is in an illegal state if
     *     <ol>
     *         <li>The instance is dereferenced {@link #isDereferenced} or</li>
     *         <li>The instance has not been initialized with data {@link #isInitialized}</li>
     *     </ol>
     * </p>
     * @see #isDereferenced
     * @see #isInitialized
     * @throws IllegalStateException if the instance is in an illegal state
     * @see #isLegalState()
     */
    protected final void assertNotIllegalState() throws IllegalStateException{
        if(isDeleted) db.throwE(new IllegalStateException("This instance has been deleted. No methods should be called. Instance: " + toString()));
        if(isDereferenced) db.throwE(new IllegalStateException("This instance is unreferenced and not maintained. No methods should be called. Instance: " + toString()));
        if(!isInitialized) db.throwE(new IllegalStateException("This instance has not been initialized with data. No methods should be called. Instance: " + toString()));

    }

    /**
     * Checks if the instance is dereferenced, in which case its tells the database to forget this
     * instance and to remove it from cache. Also, clears all listeners.
     */
    @Database.StirsDeep(what="Property Instances")
    protected final void dereferenceInstance() {
        if (isReferenced()) return;
        if (isDereferenced) return; // already dereferenced
        Log.println(Log.DEBUG, "dereference", getDocumentID() + " " + getCollection().toString());
        if(isInitialized) subDereferenceInstance();
        db.returnInstance(this);
        notifyUpdate(Database.UpdateListener.Type.DEREFERENCED);
        updateListeners.clear();
        if(snapshotListener != null) snapshotListener.remove();
        isDereferenced = true;
    }

    /**
     * Called when this instance is dereferenced. This function should dissolve any sub-instances
     */
    @Database.StirsDeep(what="Property Instances")
    protected void subDereferenceInstance() {
        for(String name : iproperties){
            PropertyWrapper<?,?> prop = properties.get(name);
            assert prop != null;
            InstancePropertyWrapper<?> iprop = prop.iS();
            if(iprop.instance == this){
                Log.println(Log.ERROR, "SubDereference", "Recurse");
                throw new IllegalStateException("Reference " + getDocumentID() + " " + name + " " + collection);
            }
            if(iprop.instance != null) iprop.instance.dissolve();
        }
    }

    /**
     * Called whenever any subinstance is updated.
     * Notifies the listeners that a subupdate occurred unless an instance was deleted, then an actual update is notified
     * @param instance The subinstance
     * @param type The type of the update
     * @param <S> The type of the subinstance
     * @see #onSubInstanceDelete(DatabaseInstance)
     */
    @Database.AutoStir(when="Instance is deleted and Property is not nullable")
    @Database.StirsDeep(what="Property Instances", when="Instance is deleted and Property is not nullable")
    @Override
    public <S extends DatabaseInstance<S>> void onUpdate(@Database.Observes DatabaseInstance<S> instance, Type type) {
        if(!isLegalState()) return;
        switch (type){
            case UPDATE:
            case SUBUPDATE:
                notifyUpdate(Type.SUBUPDATE);
                break;
            case DEREFERENCED:
            case INIT:
                break;
            case DELETE:
                onSubInstanceDelete(instance);
                break;
        }
    }

    /**
     * Updates this instance appropriately when the subinstance is deleted
     *<p>
     *     Removes the instance. If the property cannot be null and is now null, this instance is deleted.
     *     Otherwise, notifies the listeners that an update occurred
     *</p>
     * @param instance The subinstance
     */
    @Database.AutoStir(when="Property is not nullable")
    @Database.StirsDeep(what="Property Instances", when="Property is not nullable")
    private void onSubInstanceDelete(@Database.Observes DatabaseInstance<?> instance){
        for(Map.Entry<String,PropertyWrapper<?,?>> ent : properties.entrySet()){
            PropertyWrapper<?,?> prop = ent.getValue();
            if(!prop.meta.loads || prop.meta.loadsCollection != instance.collection) continue;
            InstancePropertyWrapper<?> iprop = prop.iS();
            if(!Objects.equals(iprop.value, instance.getDocumentID())){
                continue;
            }
            //Delete
            iprop.value = "";
            iprop.instance = null;
            if(!prop.meta.loadsNullable){
                deleteInstance(DeletionType.UP_FALL, success -> {});
            }else{
                notifyUpdate(Type.UPDATE);
            }
        }
    }

    @NonNull
    @Override
    public String toString() throws IllegalStateException{
        //assertNotIllegalState();
        return collection.toString() + " Instance";
    }

    /**
     * Returns the document ID
     * @return the unique identifier of the instance within the collection
     */
    public final String getDocumentID(){
        return this.documentID;
    }

    /**
     * Returns the unique identifier of this instance
     * @return the unique identifier of the instance within the collection
     */
    public final String getIdentifier(){
        return this.documentID;
    }

    /**
     * Returns a unique identifier for the instance within the full database
     * @return The unique database identifier
     * @see Database.Collections#getDatabaseID(DatabaseInstance)
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     */
    final String getDatabaseID() throws IllegalStateException{
        return collection.getDatabaseID(this);
    }

    /**
     * Exchanges a instance property's value with the given id. Loads the new ID.
     * @param p The property to edit
     * @param newID The ID of the new instance
     * @param <W> The type of the instance
     * @param onComplete called on completion, true if no errors occurred, might not be called before return
     * @throws IllegalArgumentException If the property is not an instance property
     * @throws ClassCastException If the type does not match the properties type
     */
    @SuppressWarnings("unchecked")
    @Database.StirsDeep(what="Previous Instance")
    private <W extends DatabaseInstance<W>> void exchangeInstance(PropertyWrapper<?,?> p, @Database.Dilutes String newID, Consumer<Boolean> onComplete) throws IllegalArgumentException, ClassCastException{
        if(!p.meta.loads) db.throwE(new IllegalArgumentException("Invalid property : " + p.meta.propertyNameID));
        InstancePropertyWrapper<W> prop = (InstancePropertyWrapper<W>) p;

        if(Objects.equals(newID, prop.value)) {
            onComplete.accept(true);
            return;
        }
        Log.println(Log.DEBUG, "modPropExchange", getDocumentID() + " " + db.constants.getString(prop.meta.propertyNameID) + " " + String.valueOf(newID));

        if(!newID.isBlank()){
            Log.println(Log.DEBUG, "modPropLoading", getDocumentID() + " " + db.constants.getString(prop.meta.propertyNameID) + " " + String.valueOf(newID));
            db.<W>getInstance(prop.meta.loadsCollection, newID, (i, s) -> {
                if(!s){
                    Log.println(Log.DEBUG, "modPropLoadFail", getDocumentID() + " " + db.constants.getString(prop.meta.propertyNameID) + " " + String.valueOf(newID));
                    setPropertyValue(prop.meta.propertyNameID, "", $ -> {
                        onComplete.accept(false);
                    });
                    return;
                }
                Log.println(Log.DEBUG, "modPropLoadSuccess", getDocumentID() + " " + db.constants.getString(prop.meta.propertyNameID) + " " + String.valueOf(newID));
                if(prop.instance != null){
                    prop.instance.dissolve();
                }
                prop.instance = i;
                prop.value = newID;
                onComplete.accept(true);
            });
        }else {
            Log.println(Log.DEBUG, "modPropNulling", getDocumentID() + " " + db.constants.getString(prop.meta.propertyNameID) + " " + String.valueOf(newID));
            if (!prop.meta.loadsNullable)
                db.throwE(new IllegalArgumentException("The value is null but the property is not nullable: " + db.constants.getString(prop.meta.propertyNameID) + " - " + newID));
            if(prop.instance != null){
                prop.instance.dissolve();
            }
            prop.instance = null;
            prop.value = newID;
            onComplete.accept(true);
        }

    }

    /**
     * Exchanges a instance property's instance and id with the given new instance. Fetches a reference to the newInstance
     * @param prop The property to edit
     * @param newInstance The new Instance
     * @param <W> The type of the instance
     * @throws IllegalArgumentException If the property is not an instance property
     * @throws ClassCastException If the type does not match the properties type
     */
    @Database.AutoStir
    @Database.StirsDeep(what="Previous Instance")
    private <W extends DatabaseInstance<W>> void exchangeInstance(InstancePropertyWrapper<W> prop, @Database.Dilutes W newInstance) throws ClassCastException{
        if(Objects.equals(newInstance, prop.instance)){
            return;
        }
        if (!prop.meta.loadsNullable && newInstance == null)
            db.throwE(new IllegalArgumentException("The value is null but the property is not nullable: " + prop.meta.propertyNameID));
        if(newInstance!=null)newInstance.fetch();
        if(prop.instance != null){
            prop.instance.dissolve();
        }

        prop.instance = newInstance;
        prop.value = newInstance == null?"":newInstance.getDocumentID();
    }

    /**
     * Modifies the properties of the object given the data set.
     * Calls all initialization listeners.
     * Adds a snapshot listener to the document.
     * <p>
     * Ignores if the field can edit
     *
     * @param data The property key-value set
     * @param onComplete called on completion. true if no errors. If none of the properties are setting an instance id, will be called before return.
     * @return {@code true} if the instance was changed as a result
     * @throws IllegalArgumentException if a key is not one of the available properties, or if the value is not valid
     * @throws ClassCastException if a value does not match an properties type
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Database.StirsDeep(what="Old Instances of Instance properties that are modified")
    protected final boolean modifyData(@Database.Dilutes Map<String, Object> data, Consumer<Boolean> onComplete) throws IllegalArgumentException, ClassCastException{
        boolean diff = false;

        List<Pair<PropertyWrapper<?,?>, String>> loadIds = new ArrayList<>();

        for(Map.Entry<String,Object> ent : data.entrySet()){
            PropertyWrapper<?,?> prop = properties.get(ent.getKey());
            if(prop == null) db.throwE(new IllegalArgumentException("Invalid property"));
            Log.println(Log.DEBUG, "modProp", getDocumentID() + " " + ent.getKey() + " " + String.valueOf(ent.getValue()));
            if(Objects.equals(prop.value, ent.getValue())) {
                Log.println(Log.DEBUG, "modEqual", getDocumentID() + " " + ent.getKey() + " " + String.valueOf(ent.getValue()));
                continue;
            };
            diff = true;
            if(prop.meta.loads){
                if(ent.getValue() instanceof DatabaseInstance){
                    Log.println(Log.DEBUG, "modPropLoadsI", getDocumentID() + " " + ent.getKey() + " " + String.valueOf(ent.getValue()));
                    exchangeInstance((InstancePropertyWrapper)prop, (DatabaseInstance)ent.getValue());
                }else{
                    Log.println(Log.DEBUG, "modPropLoadsId", getDocumentID() + " " + ent.getKey() + " " + String.valueOf(ent.getValue()));
                    loadIds.add(new Pair<>(prop, String.valueOf(ent.getValue())));
                }
            }else{
                Log.println(Log.DEBUG, "modPropVal", getDocumentID() + " " + ent.getKey() + " " + String.valueOf(ent.getValue()));
                prop.setValue(ent.getValue());
            }

        }
        if(loadIds.isEmpty()){
            onComplete.accept(true);
            return diff;
        }

        Consumer<Boolean> l = new Consumer<Boolean>() {
            private int i = -1;
            private boolean s = true;
            @Override
            public void accept(Boolean success) {
                i ++;
                s = s || success;
                if(i >= loadIds.size()){
                    onComplete.accept(s);
                    return;
                }
                Pair<PropertyWrapper<?,?>, String> instance = loadIds.get(i);
                Log.println(Log.DEBUG, "modPropExchangeAsync", getDocumentID() + " " + db.constants.getString(instance.first.meta.propertyNameID) + " " + instance.second);
                exchangeInstance(instance.first, instance.second, this);
            }
        };
        l.accept(true);
        return diff;
    }



    /**
     * Edits the value of a property. If the property is an instance, loads the property and fetches the reference
     * @param resID The res id of the property name
     * @param newValue the new value of the property
     * @param onComplete called on completion, true if no errors occurred. Will be called before return if setting a non loading property
     *                   or if setting a loading property with an already instantiated instance
     * @return if the property was changed
     * @throws IllegalArgumentException if the property does not exist or if the property cannot be edited or the new value is invalid
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     * @throws ClassCastException if the value type does not match the properties type
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Database.StirsDeep(what="The previous instance", when="Modifying an instance property")
    public final boolean setPropertyValue(int resID, @Database.Dilutes Object newValue, Consumer<Boolean> onComplete) throws IllegalArgumentException, IllegalStateException{
        assertNotIllegalState();
        String name = db.constants.getString(resID);
        PropertyWrapper<?,?> prop = properties.get(name);
        if(prop==null) db.throwE(new IllegalArgumentException("Invalid property: " + name));
        if(!prop.meta.canEdit) db.throwE(new IllegalArgumentException("Invalid property - cannot edit: " + name));
        if(!isPropertyValid(prop, newValue)) db.throwE(new IllegalArgumentException("Invalid value for " + name + ": " + newValue.toString()));
        if(Objects.equals(prop.value, newValue)) {
            onComplete.accept(true);
            return false;
        }
        if(!prop.meta.loads){
            prop.setValue(newValue);
            processUpdate();
            onComplete.accept(true);
            return true;
        }
        if(newValue instanceof DatabaseInstance) {
            exchangeInstance((InstancePropertyWrapper)prop, (DatabaseInstance) newValue);
            processUpdate();
            onComplete.accept(true);
            return true;
        }

        exchangeInstance(prop, (String)newValue, s -> {
            if(s){
                processUpdate();
                onComplete.accept(true);
            }else{
                onComplete.accept(false);
            }
        });

        return true;

    }

    /**
     * Edits the value of an instance property to the new instance. Fetches a reference to the new instance
     * @param resID The res id of the property name
     * @param instance the instance to make the new value. This function will retrieve its own reference
     * @return if the property was changed
     * @throws IllegalArgumentException if the property does not exist or if the property cannot be edited or the new value is invalid or the property is not an instance property
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     * @throws ClassCastException if the instance type does not match the properties type
     */
    @SuppressWarnings("unchecked")
    @Database.StirsDeep(what="The previous instance")
    public final <W extends DatabaseInstance<W>> boolean setPropertyInstance(int resID, @Nullable @Database.Dilutes W instance) throws IllegalArgumentException, ClassCastException, IllegalStateException{
        assertNotIllegalState();
        String name = db.constants.getString(resID);
        Log.println(Log.DEBUG, "setPropertyStart", getDocumentID() + " " + getCollection() + " " + name + " : " + (instance == null ? "" : (instance.getDocumentID() + " " + instance.getCollection())));
        PropertyWrapper<?,?> prop = properties.get(name);
        if(prop==null || !prop.meta.loads) db.throwE(new IllegalArgumentException("Invalid property: " + name));
        if(!prop.meta.canEdit) db.throwE(new IllegalArgumentException("Invalid property - cannot edit: " + name));
        if(instance == null && !prop.meta.loadsNullable) db.throwE(new IllegalArgumentException("Cannot set to null: " + name));
        if(instance != null && !instance.isLegalState()) db.throwE(new IllegalArgumentException("Instance is in illegal state " + name));
        String id = instance == null? "": instance.getDocumentID();
        if(!isPropertyValid(prop, id)) db.throwE(new IllegalArgumentException("Invalid value for " + name + ": " + instance.toString()));
        InstancePropertyWrapper<W> iprop = (InstancePropertyWrapper<W>) prop;
        if(Objects.equals(iprop.instance, instance)) {
            Log.println(Log.DEBUG, "setPropertyEqual", getDocumentID() + " " + getCollection() + " " + name + " : " + (instance == null ? "" : (instance.getDocumentID() + " " + instance.getCollection())));
            return false;
        }
        exchangeInstance(iprop, instance);
        processUpdate();
        Log.println(Log.DEBUG, "setPropertyEnd", getDocumentID() + " " + getCollection() + " " + name + " : " + (instance == null ? "" : (instance.getDocumentID() + " " + instance.getCollection())));
        return true;
    }

    /**
     * Gets the value of a single property. In the case of instance properties, this returns the id
     * @param resID The res id of the property name
     * @return The value of the property as an Object
     * @throws IllegalArgumentException if the property does not exist
     * @throws ClassCastException if the generic is incorrect
     */
    @Database.Observes
    public final Object getPropertyValue(int resID) throws IllegalArgumentException, ClassCastException{
        String name = db.constants.getString(resID);
        PropertyWrapper<?,?> prop = (PropertyWrapper<?, ?>) properties.get(name);
        if(prop==null) db.throwE(new IllegalArgumentException("Invalid property: " + name));
        return prop.value;
    }

    /**
     * Gets the value of a single property. In the case of instance properties, this returns the id
     * @param resID The res id of the property name
     * @return The value of the property as an Object
     * @param <V> The type of the property
     * @throws IllegalArgumentException if the property does not exist
     * @throws ClassCastException if the generic is incorrect
     */
    @SuppressWarnings("unchecked")
    @Database.Observes
    public final <V> V getPropertyValueI(int resID) throws IllegalArgumentException, ClassCastException{
        return (V) getPropertyValue(resID);
    }

    /**
     * Gets the instance of an single instance property
     * @param resID The res id of the property name
     * @return The value of the property as an Object
     * @throws IllegalArgumentException if the property does not exist or does not have instances
     */
    @Database.Observes
    public final Object getPropertyInstance(int resID) throws IllegalArgumentException{
        String name = db.constants.getString(resID);
        PropertyWrapper<?,?> prop = properties.get(name);
        if(prop==null) db.throwE(new IllegalArgumentException("Invalid property: " + name));
        return prop.iS().instance;
    }

    /**
     * Gets the instance of an single instance property
     * @param resID The res id of the property name
     * @return The value of the property as an Object
     * @throws IllegalArgumentException if the property does not exist or does not have instances
     * @throws ClassCastException if the generic is incorrect
     */
    @SuppressWarnings("unchecked")
    @Database.Observes
    public final <W extends DatabaseInstance<W>> W getPropertyInstanceI(int resID) throws IllegalArgumentException, ClassCastException{
        return (W) getPropertyInstance(resID);
    }


    /**
     * Test if the data is valid for the property
     * @param resID The res id of the property name
     * @param value The value to test
     * @return {@code true} if the value is valid
     */
    public final boolean isPropertyValid(int resID, @Database.Observes Object value){
        PropertyWrapper<?, ?> prop = properties.get(db.constants.getString(resID));
        if(prop == null) return false;
        return isPropertyValid(prop, value);
    }

    /**
     * Test if the data is valid for the property
     * @param prop the property
     * @param value the value to test
     * @return {@code true} if the value is valid
     */
    public final boolean isPropertyValid(PropertyWrapper<?,?> prop, @Database.Observes Object value){
        return prop.meta.isValid.test(value);
    }

    /**
     * Gets all properties of the given collection instance
     * @return The property fields of the instance type
     */
    public static PropertyField<?,?>[] getFields(Database.Collections collection){
        switch (collection){
            case NOTIFICATIONS:
                return Notification.fields;
            case FACILITIES:
                return Facility.fields;
            case EVENTS:
                return Event.fields;
            case USERS:
                return User.fields;
            case IMAGES:
                return Image.fields;
            case EVENT_ASSOCIATIONS:
                return EventAssociation.fields;
            default:
                return new PropertyField[0];
        }
    }


    /**
     * Tests if the map is valid for this instance
     * @param map The property resID-value map where resID is the res ID of the property name
     * @param collection The collection of the instance
     * @return The set of all invalid ids
     */
    public static Set<Integer> isDataValid(@Database.Observes Map<Integer, Object> map, Database.Collections collection){
        return isDataValid(map, getFields(collection));
    }

    /**
     * Tests if the map is valid for this instance
     * @param map The property resID-value map where resID is the res ID of the property name
     * @param fields The set of propertyFields to test against
     * @return The set of all invalid ids
     */
    static Set<Integer> isDataValid(@Database.Observes Map<Integer, Object> map, PropertyField<?,?>[] fields){
        Set<Integer> ids = new HashSet<>(map.keySet());
        for(PropertyField<?,?> prop : fields){
            if(!map.containsKey(prop.propertyNameID)) continue;
            if(prop.isValid.test(map.get(prop.propertyNameID))){
                ids.remove(prop.propertyNameID);
            }
        }
        return ids;
    }

    /**
     * Checks if the property is editable
     * @param resID the res id of the property name
     * @return {@code true} if the property is present and editable
     */
    public final boolean isPropertyEditable(int resID){
        PropertyWrapper<?,?> prop = properties.get(db.constants.getString(resID));
        if(prop == null) return false;
        return prop.meta.canEdit;
    }

    /**
     * Checks if the property loads an instance
     * @param resID the res id of the property name
     * @return {@code true} if the property is present and loads an instance
     */
    public final boolean doesPropertyLoadInstance(int resID){
        PropertyWrapper<?,?> prop = properties.get(db.constants.getString(resID));
        if(prop == null) return false;
        return prop.meta.loads;
    }

    /**
     * Initializes the instance with data.
     * <p>
     *     This function sets the data.
     *     Then it sets the instance to a valid state.
     *     Then it notifies all initialization listeners and update listeners.
     *     Finally it adds the snapshot listener to the document
     * </p>
     * <p>
     *     If it does not exist, full dissolves and notifies all listeners with null
     * </p>
     * @param data The property key-value mapping of the data
     * @param exists If the document exists in the database
     * @param onComplete Called when initialization is complete before the any other initialization listeners.
     *                   This listener will <b>not</b> return a fetched instance, all other initialization listeners
     *                   attached to the object will return a fetch instance.
     * @throws IllegalStateException if the instance has already been initialized
     */
    @Database.AutoStir(when="Error or not exists")
    @Database.Stirred
    final void initializeData(@Database.Dilutes Map<String, Object> data, boolean exists, Database.InitializationListener<T> onComplete) throws IllegalStateException{
        if(isInitialized || isInitializing) db.throwE(new IllegalStateException("This instance has already been initialized: " + toString()));
        Log.println(Log.DEBUG, "initData", getDocumentID()+" "+exists);
        if(!exists){
            Log.println(Log.DEBUG, "initDataFail", getDocumentID());
            fullDissolve();
            onComplete.onInitialization(null, false);
            initializationListeners.forEach(l -> l.onInitialization(null, false));
            initializationListeners.clear();
            return;
        }
        Log.println(Log.DEBUG, "initDataGood", getDocumentID());
        isInitializing = true;
        modifyData(data, success -> {
            if(!success){
                Log.println(Log.DEBUG, "initDataModifyFail", getDocumentID());
                fullDissolve();
                onComplete.onInitialization(null, false);
                initializationListeners.forEach(l -> l.onInitialization(null, false));
                initializationListeners.clear();
                return;
            }
            Log.println(Log.DEBUG, "initDataModified", getDocumentID());
            isInitialized = true;
            onComplete.onInitialization(cast(), true);
            initializationListeners.forEach(l -> l.onInitialization(fetch(), true));
            initializationListeners.clear();
            notifyUpdate(Type.INIT);
            snapshotListener = getDocumentReference().addSnapshotListener(db);
        });
    }

    /**
     * This function is called after this instance's data has been initialized and set. This function
     * should call the getter's for each sub-instance that needs to be stored. The listener should
     * only be called once all sub-objects have been loaded. The listener's success should be set to
     * false if any of the objects failed. Only sub-instances that are successfully loaded should be non-null
     * when the listener is called, all failed should be set to null (they will be dereferenced automatically
     * by their own initializers)
     * @param listener The listener that should be called once all initialization is complete
     * @throws IllegalArgumentException If the value is empty but the property is not nullable
     * TODO does this X hack work?
     * @deprecated Modify data already does this, this is never called without modify data being called first
     */
    @Database.Titrates(what="Sub instances")
    @SuppressWarnings("unchecked")
    @Deprecated
    protected <X extends DatabaseInstance<X>>  void subInitialize(Database.InitializationListener<T> listener, int count) throws IllegalArgumentException{
        if(count >= iproperties.size()) {
            Log.println(Log.DEBUG, "subInitEnd", "endCount");
            listener.onInitialization(this.cast(), true);
            return;
        }
        PropertyWrapper<?,?> prop = properties.get(iproperties.get(count));
        assert prop != null;
        InstancePropertyWrapper<X> iprop = (InstancePropertyWrapper<X>) prop.iS();
        if(iprop.value.isBlank()){
            if(!prop.meta.loadsNullable) db.throwE(new IllegalArgumentException("The value is null but the property is not nullable: "+ prop.meta.propertyNameID));
            subInitialize(listener, count+1);
            return;
        }
        Database.Collections collection = prop.meta.loadsCollection;
        db.<X>getInstance(collection, iprop.value, (instance, success) -> {
            if(!success){
                iprop.value = "";
                iprop.instance = null;
                Log.println(Log.DEBUG, "subInitEnd", "endSuccess");
                listener.onInitialization(this.cast(), false);
                return;
            }
            iprop.instance = instance;
            subInitialize(listener, count+1);
        });
    }

    /**
     * Modifies the properties of the object given the data set and notifies all listeners if the
     * instance was changed.
     * @param data The property key-value set
     * @param onComplete called once complete, true if no errors. If not modifying any instance ids, will be called before return
     * @return {@code true} if the instance was changed as a result
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     */
    @Database.StirsDeep(what = "Old Instances of Instance properties that are modified")
    final boolean updateDataFromDatabase(@Database.Dilutes Map<String, Object> data, Consumer<Boolean> onComplete) throws IllegalStateException{
        assertNotIllegalState();
        return modifyData(data, success -> {
            notifyUpdate(Type.UPDATE);
            onComplete.accept(success);
        });
    }

    /**
     * Validates the given data then creates the image and upon success updates the values and notifies listeners. Then updates the database.
     * Updates the associated image with the new image
     * @param data The property name id -> value mapping of all fields to be edited
     * @param image The image
     * @param locName The name of the location where the image is stored
     * @param onComplete called once complete, true if no errors. If not modifying any instance ids, will be called before return
     * @return The set of invalid property ids upon validation
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     * @throws IllegalArgumentException if a key is not one of the available properties to edit or a value is not valid
     * @throws ClassCastException If a value's type does not match the property type
     * @see #updateDataFromMap(Map, Consumer)
     */
    @Database.StirsDeep(what = "Old Instances of Instance properties that are modified")
    public final Set<Integer> updateDataFromMap(@Database.Dilutes Map<Integer,Object> data, @Nullable Uri image, String locName, Consumer<Boolean> onComplete) throws IllegalStateException, IllegalArgumentException, ClassCastException{
        assertNotIllegalState();
        Set<Integer> ids = DatabaseInstance.isDataValid(data, getCollection());
        if(!ids.isEmpty()){
            Log.println(Log.DEBUG, "Listener", "skipping udfmi");
            return ids;
        }

        db.replaceImage(image, getAssociatedImage(), locName, getCollection(), getDocumentID(), (newImage, returnSuccess) -> {
            data.put(collection.getAssociatedImagePropertyId(), newImage == null ? "" : newImage.getDocumentID());
            updateDataFromMap(data, success -> returnSuccess.accept(null, success));
        }, (_null, success) -> onComplete.accept(success));
        return ids;
    }

    /**
     * Validates the given data then updates the values and notifies listeners. Then updates the database.
     * @param data The property name id -> value mapping of all fields to be edited
     * @param onComplete called once complete, true if no errors. If not modifying any instance ids, will be called before return
     * @return The set of invalid property ids upon validation
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     * @throws IllegalArgumentException if a key is not one of the available properties to edit or a value is not valid
     * @throws ClassCastException If a value's type does not match the property type
     */
    @Database.StirsDeep(what = "Old Instances of Instance properties that are modified")
    public final Set<Integer> updateDataFromMap(@Database.Dilutes Map<Integer,Object> data, Consumer<Boolean> onComplete) throws IllegalStateException, IllegalArgumentException, ClassCastException{
        assertNotIllegalState();
        Set<Integer> ids = DatabaseInstance.isDataValid(data, getCollection());
        if(!ids.isEmpty()){
            Log.println(Log.DEBUG, "Listener", "skipping udfm");
            for(int i : ids){
                Log.println(Log.DEBUG, "Listener", "\t"+db.constants.getString(i));
            }
            return ids;
        }
        modifyData(db.convertIDMapToNames(data), success -> {
            processUpdate();
            onComplete.accept(success);
        });
        return ids;
    }

    /**
     * Sets the associated Image instance. This function will create a new reference to the instance.
     * @param image The new instance
     * @param onComplete called on completion with if the update was successful
     * @throws UnsupportedOperationException if the instance type does not have an editable associated image
     */
    @Database.StirsDeep(what = "The previous Image")
    public void setAssociatedImage(@Nullable Uri image, Consumer<Boolean> onComplete) throws UnsupportedOperationException{
        int id = getCollection().getAssociatedImagePropertyId();
        if(id == Database.Collections.IS_ITS_OWN_ASSOCIATED_IMAGE || id == Database.Collections.DOES_NOT_HAVE_ASSOCIATED_IMAGE || !isPropertyEditable(id)){
            db.throwE(new UnsupportedOperationException("This instance does not have an editable associated image"));
            return;
        }
        db.replaceImage(image, getAssociatedImage(), getAssociatedImageLocName(), getCollection(), getDocumentID(), (newImage, returnSuccess) -> {
            setPropertyInstance(getCollection().getAssociatedImagePropertyId(), newImage);
        }, (_null, success) -> onComplete.accept(success));
    }

    /**
     * Returns the data of the instance
     * @return The property key-value map of the instance
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     */
    @Database.Observes
    protected final Map<String, Object> getData() throws IllegalStateException{
        assertNotIllegalState();
        Map<String, Object> values = new HashMap<>();
        for(Map.Entry<String, PropertyWrapper<?,?>> ent : properties.entrySet()){
            values.put(ent.getKey(), ent.getValue().value);
        }
        return values;
    }

    /**
     * Notifies all listeners that an update has occurred and modifies the database to match
     *
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     * @see Database#updateDatabase(DatabaseInstance)
     */
    protected final void processUpdate() throws IllegalStateException{
        notifyUpdate(Database.UpdateListener.Type.UPDATE);
        db.updateDatabase(this);
        Image assocImage = getAssociatedImage();
        String assocLocName = getAssociatedImageLocName();
        if(assocImage != null && assocLocName != null){
            assocImage.setLocName(assocLocName);
        }
    }

    /**
     * @see #REPLACMENT
     * @see #HARD_DELETE
     * @see #CASCADE
     * @see #ERROR
     */
    public enum DeletionType {;
        /**
         * If the instance is being deleted because it is being replaced by another version of the instance.
         */
        public final static int REPLACMENT = 0b00001;
        /**
         * If the instance is being deleted explicitly
         */
        public final static int HARD_DELETE = 0b00010;
        /**
         * If the instance is being deleted because another instance was deleted
         */
        public final static int CASCADE = 0b00100;
        /**
         * If the instance is being deleted because a non nullable sub instance was deleted
         */
        public final static int UP_FALL = 0b01000;
        /**
         * If the instance is being deleted because
         */
        public final static int ERROR = 0b10000;
    }

    /**
     * Dereferences the instance. Then cascade deletes all cascading properties. Then notifies all listeners that the instance was deleted.
     * @param deletionType The {@link DeletionType} reason this instance is being deleted
     * @param listener called on completion of deletion, returns false if one or more errors occured
     * @see Database#deleteFromDatabase(DatabaseInstance)
     */
    @Database.AutoStir
    @Database.StirsDeep(what="Sub Instances")
    public void deleteInstance(int deletionType, Consumer<Boolean> listener){
        if(!isLegalState()) return;
        Log.println(Log.DEBUG, "DeleteInstance", getDocumentID() + " " + getCollection());
        requiredFirstDelete(deletionType, success -> {
            if(!success){
                listener.accept(false);
                return;
            }
            isDeleted = true;
            db.deleteFromDatabase(this);
            deleteSubInstances(deletionType, success2 -> {
                notifyUpdate(Database.UpdateListener.Type.DELETE); //Might need to change which order
                fullDissolve();
                listener.accept(true);
            });
        });
    }

    /**
     * Deletes any sub objects that are not instances
     * @param deletionType The {@link DeletionType} reason this instance is being deleted
     * @param listener Called before the instance and sub instances are deleted; they are only deleted if this returns true
     */
    protected void requiredFirstDelete(int deletionType, Consumer<Boolean> listener){
        listener.accept(true);
    }

    /**
     * Deletes all subinstances that are cascaded
     * @param deletionType The {@link DeletionType} reason this instance is being deleted
     * @param listener called on completion of deletion
     */
    @Database.StirsDeep(what = "Sub instances")
    private void deleteSubInstances(int deletionType, Consumer<Boolean> listener){
        List<Pair<Query, Database.Collections>> queries = subInstanceCascadeDeleteQuery();
        //Have I ever mentioned that I hate async
        Consumer<Boolean> l2 = new Consumer<Boolean>() {
            private int i = -1;
            private boolean s;
            @Override
            public void accept(Boolean success) {
                s = s || success;
                i ++;
                if(i > queries.size()){
                    listener.accept(s);
                    return;
                }
                Pair<Query, Database.Collections> qc = queries.get(i);
                DatabaseQuery<?> dq = new DatabaseQuery<>(db, qc.first, qc.second, null);
                dq.refreshData((query, success2) -> {
                    if(!success) this.accept(false);
                    Consumer<Boolean> thiser = this;
                    Consumer<Boolean> l3 = new Consumer<Boolean>() {
                        private int j = -1;
                        private boolean s2 = true;
                        @Override
                        public void accept(Boolean success) {
                            s2 = s2 || success;
                            j++;
                            if(j > dq.getCurrentInstances().size()){
                                dq.dissolve();
                                thiser.accept(s2);
                                return;
                            }
                            dq.getCurrentInstances().get(j).deleteInstance(deletionType | DeletionType.CASCADE, this);
                        }
                    };
                });
            }
        };

        Consumer<Boolean> l1 = new Consumer<Boolean>() {
            private int i = -1;
            private boolean s= true;
            @Override
            public void accept(Boolean success) {
                s = s || success;
                i ++;
                if(i > iproperties.size()){
                    l2.accept(s);
                    return;
                }
                PropertyWrapper<?,?> p = properties.get(iproperties.get(i));
                assert p != null;
                InstancePropertyWrapper<?> iprop = p.iS();
                if(iprop.instance!=null && iprop.meta.cascadeDelete){
                    iprop.instance.deleteInstance(deletionType | DeletionType.CASCADE, this);
                }
            }
        };
        l1.accept(false);
    }

    /**
     * @return A list of all querries that should be run whose instances will be deleted when this instance is deleted
     */
    protected abstract List<Pair<Query, Database.Collections>> subInstanceCascadeDeleteQuery();

    /**
     * Returns the collection that this instance is apart of
     * @return the {@code Database.Collections}
     */
    protected final Database.Collections getCollection(){
        return collection;
    }

    /**
     * Returns the {@code DocumentReference} associated with this instance
     * <p>
     *     If the document did not exist, a new document is created
     * </p>
     * @return the database reference to this instance
     * @see Database.Collections#getCollection(Database)
     */
    final DocumentReference getDocumentReference() {
        return collection.getDocument(db, documentID);
    }

    /**
     * Returns the {@code CollectionReference} associated with this instance
     * @return the database reference to the collection containing this instance
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     * @see Database.Collections#getDocument(Database, String)
     */
    final CollectionReference getCollectionReference() throws IllegalStateException{
        assertNotIllegalState();
        return collection.getCollection(db);
    }

    /**
     * Designates a property that is maintained by a Database instance
     * @param <V> The type of the value that the property stores
     * @param <W> The type of the instance that the property stores if applicable
     */
    //Abuse of generics
    public static class PropertyField<V,W extends DatabaseInstance<W>> {
        /**
         * The ID of the res string that defines the string used for the property key in the database
         */
        public final int propertyNameID;
        /**
         * The function to test if a given value is valid for this property
         */
        public final Predicate<Object> isValid;
        /**
         * If the property can be edited
         */
        public final boolean canEdit;
        /**
         * If the property loads an instance
         */
        public final boolean loads;
        /**
         * The collection that the instance is apart of if applicable
         */
        public final Database.Collections loadsCollection;
        /**
         * If the instance can be null
         */
        public final boolean loadsNullable;
        /**
         *
         */
        public final boolean cascadeDelete;

        /**
         * @param propertyNameID The ID of the res string that defines the string used for the property key in the database
         * @param isValid The function to test if a given value is valid for this property
         * @param canEdit If the property can be edited
         * @param loads If the property loads an instance
         * @param loadsCollection The collection that the instance is apart of if applicable
         * @param nullable If the instance can be null
         * @param cascadeDelete If the instance should be deleted upon the deletion of this instance
         */
        public PropertyField(int propertyNameID, Predicate<Object> isValid, boolean canEdit, boolean loads, Database.Collections loadsCollection, boolean nullable, boolean cascadeDelete) {
            this.propertyNameID = propertyNameID;
            this.isValid = isValid;
            this.canEdit = canEdit;
            this.loads = loads;
            this.loadsCollection = loadsCollection;
            this.loadsNullable = nullable;
            this.cascadeDelete = cascadeDelete;
        }

        /**
         * <p>
         * Sets {@link #loads} to {@code false}
         * @param propertyNameID The ID of the res string that defines the string used for the property key in the database
         * @param isValid The function to test if a given value is valid for this property
         * @param canEdit If the property can be edited
         */
        public PropertyField(int propertyNameID, Predicate<Object> isValid, boolean canEdit){
            this(propertyNameID, isValid, canEdit, false, null, true, false);
        }

        /**
         * Gets the associated wrapper object
         * @return  If the property is a instance {@link InstancePropertyWrapper};
         *          if the property is a value {@link PropertyWrapper}
         */
        @SuppressWarnings("unchecked")
        PropertyWrapper<?,?> getWrapper(){
            if(loads){
                return new InstancePropertyWrapper<>((PropertyField<String,W>)this);
            }else{
                return new PropertyWrapper<>((PropertyField<V,NullInstance>)this);
            }
        }

        /**
         * This class exists to fulfill the wildcard for PropertyWrappers
         */
        public static class NullInstance extends DatabaseInstance<NullInstance> {

            private NullInstance(Database db, String documentID, Database.Collections collection, @NonNull PropertyField<?, ?>[] properties) throws ClassCastException, IllegalArgumentException {
                super(db, documentID, collection, properties);
                throw new UnsupportedOperationException("No");
            }

            @Override
            protected NullInstance cast() {
                throw new UnsupportedOperationException();
            }

            @Override
            protected List<Pair<Query, Database.Collections>> subInstanceCascadeDeleteQuery() {
                return Collections.emptyList();
            }
        }
    }

    /**
     * Designates a property and its current value for a database instance
     * @param <V> The type of the value
     * @param <W> The type of the instance if applicable
     */
    public static class PropertyWrapper<V,W extends DatabaseInstance<W>>{
        /**
         * The property meta data
         */
        public final PropertyField<V,W> meta;
        /**
         * The current value
         */
        public V value;

        PropertyWrapper(PropertyField<V,W> meta) {
            this.meta = meta;
        }

        /**
         * Sets the value from an Object value
         * @param value The value
         * @throws ClassCastException if the value is not the correct type
         */
        @SuppressWarnings("unchecked")
        public void setValue(@Database.Observes Object value) throws ClassCastException{
            if(!meta.isValid.test(value)) throw new ClassCastException("Invalid value: " + value + " for property id " + meta.propertyNameID);
            this.value = (V) value;
        }

        /**
         * Returns the instance property wrapper of this wrapper
         * @return The casted property wrapper
         * @throws ClassCastException If this property does not load an instance
         */
        @SuppressWarnings("unchecked")
        public InstancePropertyWrapper<W> iS() throws ClassCastException{
            return (InstancePropertyWrapper<W>)this;
        }
    }

    /**
     * A property wrapper for Single instance properties
     * @param <W> The type of the property
     */
    public static class InstancePropertyWrapper<W extends DatabaseInstance<W>> extends PropertyWrapper<String,W>{
        /**
         * The current instance
         */
        public W instance;

        InstancePropertyWrapper(PropertyField<String,W> meta) {
            super(meta);
        }
    }
}
