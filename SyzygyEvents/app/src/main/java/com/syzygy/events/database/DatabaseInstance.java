package com.syzygy.events.database;

import android.util.Property;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * An instance of a database item which should be updated whenever the database is updated.
 * @author Gareth Kmet
 * @version 1.0
 * @param <T> The class of the instance.
 * @since 19oct24
 */
public abstract class DatabaseInstance<T extends DatabaseInstance<T>> {
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
     * If the instance has been initalized with data
     */
    private boolean isInitialized = false;

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
    private final Set<Database.UpdateListener<T>> updateListeners = new HashSet<>();

    /**
     * A set of listeners which are called when the instance is initialized
     * @see Database.InitializationListener
     * @see #initializeData(Map, boolean)
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
    protected DatabaseInstance(Database db, String documentID, Database.Collections collection, @NonNull PropertyField<?,?>[] properties) throws ClassCastException, IllegalArgumentException{
        T t = (T)this;
        if(properties.length == 0) throw new IllegalArgumentException("Properties is empty: " + collection);
        this.db = db;
        this.documentID = documentID;
        this.collection = collection;
        this.db.initializeFromDatabase(this);
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
    protected abstract T cast();

    /**
     * Adds a new update listener to this instance
     * <p>
     * If the same listener is added twice, the listener will only be notified of an update once
     * @param listener The listener
     * @see Database.UpdateListener
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     */
    public final void addListener(@Nullable Database.UpdateListener<T> listener) throws IllegalStateException{
        assertNotIllegalState();
        if(listener == null) return;
        updateListeners.add(listener);
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
    public final void addInitializationListener(@Nullable Database.InitializationListener<T> listener) throws IllegalStateException{
        if(listener == null) return;
        if(isInitialized){
            assertNotIllegalState();
            listener.onInitialization(cast(), true);
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
        assertNotIllegalState();
        updateListeners.forEach(l -> l.onUpdate(cast(), type));
    }

    /**
     * Increases the reference count of this instance.
     * @return This instance casted to the generic type
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     */
    public T fetch() throws IllegalStateException{
        assertNotIllegalState();
        referenceCount ++;
        return cast();
    }

    /**
     * Decreases the reference count of this instance
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     */
    public void dissolve() throws IllegalStateException{
        assertNotIllegalState();
        referenceCount --;
        dereferenceInstance();
    }

    /**
     * Decreases the reference count to zero
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     */
    public void fullDissolve() throws IllegalStateException{
        assertNotIllegalState();
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
        return !isDereferenced && isInitialized;
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
        if(isDereferenced) throw new IllegalStateException("This instance is unreferenced and not maintained. No methods should be called. Instance: " + toString());
        if(!isInitialized) throw new IllegalStateException("This instance has not been initialized with data. No methods should be called. Instance: " + toString());
    }

    /**
     * Checks if the instance is dereferenced, in which case its tells the database to forget this
     * instance and to remove it from cache. Also, clears all listeners.
     */
    protected final void dereferenceInstance() {
        if (isReferenced()) return;
        if (isDereferenced) return; // already dereferenced
        subDereferenceInstance();
        db.returnInstance(this);
        notifyUpdate(Database.UpdateListener.Type.DEREFERENCED);
        updateListeners.clear();
        if(snapshotListener != null) snapshotListener.remove();
        isDereferenced = true;
    }

    /**
     * Called when this instance is dereferenced. This function should dereference any sub-instances
     */
    protected void subDereferenceInstance() {
        for(String name : iproperties){
            InstancePropertyWrapper<?,?> iprop = properties.get(name).i();
            if(iprop.instance != null) iprop.instance.dissolve();
        }
    }

    @Override
    public String toString() throws IllegalStateException{
        assertNotIllegalState();
        return collection.toString() + " Instance";
    }

    /**
     * Returns the document ID
     * @return the unique identifier of the instance within the collection
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     */
    final String getDocumentID() throws IllegalStateException{
        assertNotIllegalState();
        return this.documentID;
    }

    /**
     * Returns the unique identifier of this instance
     * @return the unique identifier of the instance within the collection
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     */
    public final String getIdentifier() throws IllegalStateException{
        assertNotIllegalState();
        return this.documentID;
    }

    /**
     * Returns a unique identifier for the instance within the full database
     * @return The unique database identifier
     * @see Database.Collections#getDatabaseID(DatabaseInstance)
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     */
    final String getDatabaseID() throws IllegalStateException{
        assertNotIllegalState();
        return collection.getDatabaseID(this);
    }

    //TODO javadocs
    @SuppressWarnings("unchecked")
    private <W extends DatabaseInstance<W>> void exchangeInstance(PropertyWrapper<?,?> p, String newID) throws IllegalArgumentException{
        InstancePropertyWrapper<String,W> prop = (InstancePropertyWrapper<String,W>) p;
        if(Objects.equals(newID, prop.value)) return;
        if(!prop.value.isBlank() && prop.instance != null){
            if(prop.instance.isLegalState()) prop.instance.dissolve();
        }
        prop.value = newID;
        prop.instance = null;
        if(!newID.isBlank()){
            prop.instance = db.getInstance(prop.meta.getLoadsCollection(properties), newID, (i, s) -> setPropertyValue(prop.meta.propertyNameID, ""));
        }else{
            if(!prop.meta.loadsNullable) throw new IllegalArgumentException("The value is null but the property is not nullable: "+ prop.meta.propertyNameID + " - " + newID);
        }
    }

    /**
     * Modifies the properties of the object given the data set.
     * Calls all initialization listeners.
     * Adds a snapshot listener to the document.
     * <p>
     * Ignores if the field can edit
     *
     * @param data The property key-value set
     * @return {@code true} if the instance was changed as a result
     * @throws IllegalArgumentException if a key is not one of the available properties, or if the value is not valid
     */

    protected final boolean modifyData(Map<String, Object> data) throws IllegalArgumentException{
        boolean diff = false;
        for(Map.Entry<String,Object> ent : data.entrySet()){
            PropertyWrapper<?,?> prop = properties.get(ent.getKey());
            if(prop == null) throw new IllegalArgumentException();
            if(Objects.equals(prop.value, ent.getValue())) continue;
            diff = true;
            if(!isPropertyValid(prop, ent.getValue())) throw new IllegalArgumentException();
            if(prop.meta.loads){
                exchangeInstance(prop, (String) ent.getValue());
            }else{
                prop.setValue(ent.getValue());
            }
        }
        return diff;
    }



    /**
     * Edits the value of a property
     * @param resID The res id of the property name
     * @param newValue the new value of the property
     * @return if the property was changed
     * @throws IllegalArgumentException if the property does not exist or if the property cannot be edited or the new value is invalid
     */
    public final boolean setPropertyValue(int resID, Object newValue) throws IllegalArgumentException{
        //TODO fix
        String name = db.constants.getString(resID);
        PropertyWrapper<?,?> prop = properties.get(name);
        if(prop==null) throw new IllegalArgumentException("Invalid property: " + name);
        if(!prop.meta.canEdit) throw new IllegalArgumentException("Invalid property - cannot edit: " + name);
        if(!isPropertyValid(prop, newValue)) throw new IllegalArgumentException("Invalid value for " + name + ": " + newValue.toString());
        if(Objects.equals(prop.value, newValue)) return false;
        if(prop.meta.loads){
            exchangeInstance(prop, (String)newValue);
        }else{
            prop.setValue(newValue);
        }
        processUpdate(true);
        return true;
    }


    /**
     * Gets the value of a property. In the case of instance properties, this returns the id
     * @param resID The res id of the property name
     * @return The value of the property as an Object
     * @throws IllegalArgumentException if the property does not exist
     * @throws ClassCastException if the generic is incorrect
     */
    @SuppressWarnings("unchecked")
    public final <V> V getPropertyValueI(int resID) throws IllegalArgumentException, ClassCastException{
        String name = db.constants.getString(resID);
        PropertyWrapper<V,?> prop = (PropertyWrapper<V, ?>) properties.get(name);
        if(prop==null) throw new IllegalArgumentException("Invalid property: " + name);
        return prop.value;
    }

    /**
     * Gets the instance of an instance property
     * @param resID The res id of the property name
     * @return The value of the property as an Object
     * @throws IllegalArgumentException if the property does not exist or does not have instances
     */
    public final Object getPropertyInstance(int resID) throws IllegalArgumentException{
        String name = db.constants.getString(resID);
        PropertyWrapper<?,?> prop = properties.get(name);
        if(prop==null) throw new IllegalArgumentException("Invalid property: " + name);
        return prop.value;
    }

    /**
     * Gets the instance of an instance property
     * @param resID The res id of the property name
     * @return The value of the property as an Object
     * @throws IllegalArgumentException if the property does not exist or does not have instances
     * @throws ClassCastException if the generic is incorrect
     */
    @SuppressWarnings("unchecked")
    public final <W extends DatabaseInstance<W>> W getPropertyInstanceI(int resID) throws IllegalArgumentException, ClassCastException{
        String name = db.constants.getString(resID);
        InstancePropertyWrapper<String, W> prop = (InstancePropertyWrapper<String, W>) properties.get(name).iS();
        if(prop==null) throw new IllegalArgumentException("Invalid property: " + name);
        return prop.instance;
    }

    /**
     * Test if the data is valid for the property
     * @param resID The res id of the property name
     * @param value The value to test
     * @return {@code true} if the value is valid
     */
    public final boolean isPropertyValid(int resID, Object value){
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
    public final boolean isPropertyValid(PropertyWrapper<?,?> prop, Object value){
        return prop.meta.isValid.test(value);
    }

    /**
     * Tests if the map is valid for this instance
     * @param data The property name-value map
     * @return {@code true} is the data is valid
     */
    public final boolean isDataValid(Map<String, Object> data){
        for(Map.Entry<String, Object> ent : data.entrySet()){
            PropertyWrapper<?,?> prop = properties.get(ent.getKey());
            if(prop == null) return false;
            if(!isPropertyValid(prop, ent.getValue())) return false;
        }
        return true;
    }

    /**
     * Tests if the map is valid for this instance
     * @param data The property resID-value map where resID is the res ID of the property name
     * @return {@code true} is the data is valid
     */
    public final boolean isDataValidIDs(Map<Integer, Object> data){
        for(Map.Entry<Integer, Object> ent : data.entrySet()){
            if(!isPropertyValid(ent.getKey(),ent.getValue())) return false;
        }
        return true;
    }

    /**
     * Tests if the map is valid for this instance
     * @param map The property resID-value map where resID is the res ID of the property name
     * @param fields The set of propertyFields to test against
     * @return {@code true} is the data is valid
     */
    public static boolean isDataValid(Map<Integer, Object> map, PropertyField<?,?>[] fields){
        Set<Integer> ids = map.keySet();
        for(PropertyField<?,?> prop : fields){
            if(!map.containsKey(prop.propertyNameID)) continue;
            ids.remove(prop.propertyNameID);
            if(!prop.isValid.test(map.get(prop.propertyNameID))) return false;
        }
        return !ids.isEmpty();
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
     * @param data The property key-value mapping of the data
     * @param exists If the document exists in the database
     * @throws IllegalStateException if the instance has already been initialized
     */
    final void initializeData(Map<String, Object> data, boolean exists) throws IllegalStateException{
        if(isInitialized) throw new IllegalStateException("This instance has already been initialized: " + toString());
        modifyData(data);
        subInitialize(new Database.InitializationListener<T>() {
            @Override
            public void onInitialization(T instance, boolean success) {
                isInitialized = success;
                initializationListeners.forEach(l -> l.onInitialization(cast(), exists && success));
                initializationListeners.clear();
                notifyUpdate(Database.UpdateListener.Type.UPDATE);
                if(!(exists && success)){
                    fullDissolve();
                }else{
                    snapshotListener = getDocumentReference().addSnapshotListener(db);
                }
            }
        }, 0);
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
     */
    @SuppressWarnings("unchecked")
    //TODO add if isarray
    protected void subInitialize(Database.InitializationListener<T> listener, int count) throws IllegalArgumentException{
        if(count >= iproperties.size()) {
            listener.onInitialization(this.cast(), true);
            return;
        }
        InstancePropertyWrapper<String, ?> prop = properties.get(iproperties.get(count)).iS();
        assert prop != null;

        if(prop.value.isBlank()){
            if(!prop.meta.loadsNullable) throw new IllegalArgumentException("The value is null but the property is not nullable: "+ prop.meta.propertyNameID + " - " + prop.value);
            subInitialize(listener, count+1);
            return;
        }
        Database.Collections collection = prop.meta.getLoadsCollection(properties);
        prop.instance = db.getInstance(collection, prop.value, (instance, success) -> {
           if(!success){
               prop.value = "";
               prop.instance = null;
               listener.onInitialization(this.cast(), false);
               return;
           }
           subInitialize(listener, count+1);
        });
    }

    /**
     * Modifies the properties of the object given the data set and notifies all listeners if the
     * instance was changed.
     * @param data The property key-value set
     * @return {@code true} if the instance was changed as a result
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     */
    final boolean updateDataFromDatabase(Map<String, Object> data) throws IllegalStateException{
        assertNotIllegalState();
        boolean mod = modifyData(data);
        if(mod){
            notifyUpdate(Database.UpdateListener.Type.UPDATE);
        }
        return mod;
    }

    /**
     * Returns the data of the instance
     * @return The property key-value map of the instance
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     */
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
     * @param changeDB If the database should be updated
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     * @see Database#updateDatabase(DatabaseInstance)
     */
    protected final void processUpdate(boolean changeDB) throws IllegalStateException{
        notifyUpdate(Database.UpdateListener.Type.UPDATE);
        if(changeDB){
            db.updateDatabase(this);
        }
    }

    /**
     * Dereferences the instance and notifies all listeners that the instance was deleted
     * @see Database#deleteFromDatabase(DatabaseInstance)
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     */
    public void deleteInstance() throws IllegalStateException{
        assertNotIllegalState();
        db.deleteFromDatabase(this);
        notifyUpdate(Database.UpdateListener.Type.DELETE);
        fullDissolve();
    }

    /**
     * Returns the collection that this instance is apart of
     * @return the {@code Database.Collections}
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     */
    protected final Database.Collections getCollection(){
        assertNotIllegalState();
        return collection;
    }

    /**
     * Returns the {@code DocumentReference} associated with this instance
     * <p>
     *     If the document did not exist, a new document is created
     * </p>
     * @return the database reference to this instance
     * @throws IllegalStateException if the instance is in an illegal state {@link DatabaseInstance#assertNotIllegalState()}
     * @see Database.Collections#getCollection(Database)
     */
    final DocumentReference getDocumentReference() throws IllegalStateException{
        assertNotIllegalState();
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

    //TODO javadocs
    //Abuse of generics
    public static class PropertyField<V,W extends DatabaseInstance<W>> {
        public final int propertyNameID;
        public final Predicate<Object> isValid;
        public final boolean canEdit;
        public final boolean loads;
        public final Database.Collections loadsCollection;
        public final String loadProperty;
        public final boolean loadsNullable;
        public final boolean isArray;

        public PropertyField(int propertyNameID, Predicate<Object> isValid, boolean canEdit, boolean loads, Database.Collections loadsCollection, String loadProperty, boolean nullable, boolean isArray) {
            this.propertyNameID = propertyNameID;
            this.isValid = isValid;
            this.canEdit = canEdit;
            this.loads = loads;
            this.loadsCollection = loadsCollection;
            this.loadProperty = loadProperty;
            this.loadsNullable = nullable;
            this.isArray = isArray;
        }

        public PropertyField(int propertyNameID, Predicate<Object> isValid, boolean canEdit, boolean loads, Database.Collections loadsCollection, String loadProperty, boolean nullable) {
            this(propertyNameID, isValid, canEdit, loads, loadsCollection, loadProperty, nullable, false);
        }

        public PropertyField(int propertyNameID, Predicate<Object> isValid, boolean canEdit, boolean isArray){
            this(propertyNameID, isValid, canEdit, false, null, null, true, isArray);
        }

        public PropertyField(int propertyNameID, Predicate<Object> isValid, boolean canEdit){
            this(propertyNameID, isValid, canEdit, false, null, null, true, false);
        }

        @SuppressWarnings("unchecked")
        PropertyWrapper<?,?> getWrapper(){
            if(loads){
                if(isArray){
                    return new InstancePropertyWrapper<List<String>, W>((PropertyField<List<String>,W>)this);
                }else{
                    return new InstancePropertyWrapper<String, W>((PropertyField<String,W>)this);
                }
            }else{
                return new PropertyWrapper<V,NullInstance>((PropertyField<V,NullInstance>)this);
            }
        }

        public Database.Collections getLoadsCollection(Map<V, PropertyWrapper<?, ?>> properties) {
            return loadsCollection == null ? Database.Collections.valueOf((String) properties.get(loadProperty).value) : loadsCollection;
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
        }
    }

    public static class PropertyWrapper<V,W extends DatabaseInstance<W>>{
        public final PropertyField<V,?> meta;
        public V value;

        PropertyWrapper(PropertyField<V,?> meta) {
            this.meta = meta;
        }

        @SuppressWarnings("unchecked")
        public void setValue(Object value) {
            this.value = (V) value;
        }

        public InstancePropertyWrapper<?, W> i() {
            return (InstancePropertyWrapper<?, W>)this;
        }

        @SuppressWarnings("unchecked")
        public InstancePropertyWrapper<String,W> iS() {
            return (InstancePropertyWrapper<String, W>)this;
        }

        @SuppressWarnings("unchecked")
        public InstancePropertyWrapper<List<String>, W> iA() {
            return (InstancePropertyWrapper<List<String>, W>)this;
        }
    }

    /**
     *
     * @param <W>
     */
    public static class InstancePropertyWrapper<V, W extends DatabaseInstance<W>> extends PropertyWrapper<V,W>{
        public W instance;

        InstancePropertyWrapper(PropertyField<V,W> meta) {
            super(meta);
        }
    }
}
