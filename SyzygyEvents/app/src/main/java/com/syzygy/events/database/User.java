package com.syzygy.events.database;

import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Query;
import com.syzygy.events.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * An instance of a user database item
 * - A user can be edited
 * @author Gareth Kmet
 * @version 1.0
 * @since 19oct24
 */
@Database.Dissovable
public class User extends DatabaseInstance<User> {

    /**
     * Checks to make sure the generic type is the type of this instance
     *
     * @param db The database
     * @param deviceID The id of the device associated with the user
     */
    @Database.Salty
    protected User(Database db, String deviceID) throws ClassCastException {
        super(db, deviceID, Database.Collections.USERS, fields);
    }

    @Override
    @Database.Observes
    protected User cast() {
        return this;
    }


    public String getName(){
        return getPropertyValueI(R.string.database_user_name);
    }

    public boolean setName(String val){
        return setPropertyValue(R.string.database_user_name, val, s -> {});
    }

    public String getDescription(){
        return getPropertyValueI(R.string.database_user_description);
    }

    public boolean setDescription(String val){
        return setPropertyValue(R.string.database_user_description, val, s -> {});
    }

    public String getProfileImageID(){
        return getPropertyValueI(R.string.database_user_profileID);
    }

    public String getFacilityID(){
        return getPropertyValueI(R.string.database_user_facilityID);
    }

    public boolean setFacilityID(@Database.Dilutes String val, Consumer<Boolean> onComplete){
        return setPropertyValue(R.string.database_user_facilityID, val, onComplete);
    }

    public String getEmail(){
        return getPropertyValueI(R.string.database_user_email);
    }

    public boolean setEmail(String val){
        return setPropertyValue(R.string.database_user_email, val, s -> {});
    }

    public String getPhoneNumber(){
        return getPropertyValueI(R.string.database_user_phoneNumber);
    }

    public boolean setPhoneNumber(String val){
        return setPropertyValue(R.string.database_user_phoneNumber, val, s -> {});
    }

    public Boolean getAdminNotifications(){
        return getPropertyValueI(R.string.database_user_adminNotifications);
    }

    public boolean setAdminNotifications(Boolean val){
        return setPropertyValue(R.string.database_user_adminNotifications, val, s -> {});
    }

    public Boolean getOrganizerNotifications(){
        return getPropertyValueI(R.string.database_user_orgNotifications);
    }

    public boolean setOrganizerNotifications(Boolean val){
        return setPropertyValue(R.string.database_user_orgNotifications, val, s -> {});
    }

    public Boolean isAdmin(){
        return getPropertyValueI(R.string.database_user_isAdmin);
    }

    public boolean makeAdmin(Boolean val){
        return setPropertyValue(R.string.database_user_isAdmin, val, s -> {});
    }

    public Timestamp getCreatedTime(){
        return getPropertyValueI(R.string.database_user_createdTime);
    }

    @Database.Observes
    public Image getProfileImage(){
        return getPropertyInstanceI(R.string.database_user_profileID);
    }

    /**
     * Sets the Image instance. This function will create a new reference to the instance.
     * @param image The new instance
     * @param onComplete called on completion with if the update was successful
     * @see #setAssociatedImage(Uri, String, Consumer) 
     */
    @Database.StirsDeep(what = "The previous Image")
    public void setProfileImage(@Nullable Uri image, Consumer<Boolean> onComplete){
        setAssociatedImage(image, getName(), onComplete);
    }

    @Database.Observes
    public Facility getFacility(){
        return getPropertyInstanceI(R.string.database_user_facilityID);
    }

    /**
     * Sets the Facility instance. This function will create a new reference to the instance.
     * @param val The new instance
     */
    @Database.StirsDeep(what = "The previous Image")
    public boolean setFacility(@Nullable @Database.Dilutes Facility val){
        //Success will be called before return so no need
        return setPropertyInstance(R.string.database_user_facilityID, val);
    }

    /**
     * Validates and updates all properties of the user. If the user changes, a notification is sent to listeners once and the database is updated once
     * @param name The name of the user
     * @param description The description of the user
     * @param profileImage the profile image
     * @param email The email of the user
     * @param phoneNumber The phone number of the user
     * @param organizerNotifications If the user should receive notifications from organizers
     * @param adminNotifications If the user should receive notifications from admins
     * @param isAdmin If the user has admin privileges
     * @param onComplete called once update is complete with weather the update was successful. Not called if properties are invalid
     * @return If the user changed as a result
     * @see #updateDataFromMap(Map, Uri, String, Consumer)
     */
    @Database.StirsDeep(what = "The previous image")
    public Set<Integer> update(String name,
                          String description,
                          @Nullable Uri profileImage,
                          String email,
                          String phoneNumber,
                          Boolean organizerNotifications,
                          Boolean adminNotifications,
                          Boolean isAdmin,
                          Consumer<Boolean> onComplete
    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_user_name, name);
        map.put(R.string.database_user_description, description);
        map.put(R.string.database_user_email, email);
        map.put(R.string.database_user_phoneNumber, phoneNumber);
        map.put(R.string.database_user_adminNotifications, adminNotifications);
        map.put(R.string.database_user_orgNotifications, organizerNotifications);
        map.put(R.string.database_user_isAdmin, isAdmin);
        return updateDataFromMap(map, profileImage, name, onComplete);
    }

    /**
     * Validates and updates all properties of the user. If the user changes, a notification is sent to listeners once and the database is updated once
     * @param name The name of the user
     * @param description The description of the user
     * @param email The email of the user
     * @param phoneNumber The phone number of the user
     * @param organizerNotifications If the user should receive notifications from organizers
     * @param adminNotifications If the user should receive notifications from admins
     * @param isAdmin If the user has admin privileges
     * @param onComplete will always be true and will be called before return
     * @return All invalid ids
     * @see #updateDataFromMap(Map, Uri, String, Consumer)
     */
    public Set<Integer> update(String name,
                               String description,
                               String email,
                               String phoneNumber,
                               Boolean organizerNotifications,
                               Boolean adminNotifications,
                               Boolean isAdmin,
                               Consumer<Boolean> onComplete
    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_user_name, name);
        map.put(R.string.database_user_description, description);
        map.put(R.string.database_user_email, email);
        map.put(R.string.database_user_phoneNumber, phoneNumber);
        map.put(R.string.database_user_adminNotifications, adminNotifications);
        map.put(R.string.database_user_orgNotifications, organizerNotifications);
        map.put(R.string.database_user_isAdmin, isAdmin);
        return updateDataFromMap(map, onComplete);
    }


    @Override
    protected List<Pair<Query, Database.Collections>> subInstanceCascadeDeleteQuery() {
        return Arrays.asList(
                new Pair<>(Database.Collections.EVENT_ASSOCIATIONS.getCollection(db)
                        .whereEqualTo(db.constants.getString(R.string.database_assoc_user), getDocumentID()), Database.Collections.EVENT_ASSOCIATIONS),
                new Pair<>(Database.Collections.NOTIFICATIONS.getCollection(db)
                        .whereEqualTo(db.constants.getString(R.string.database_not_receiverID), getDocumentID()), Database.Collections.NOTIFICATIONS)
        );
    }

    /**
     * The list of the fields defined for a User
     */
    static final PropertyField<?, ?>[] fields = {
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_user_name, o -> o instanceof String && !((String) o).isBlank(), true),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_user_description, o -> o instanceof String, true),
            new PropertyField<String, Image>(R.string.database_user_profileID, o -> o instanceof String, true, true, Database.Collections.IMAGES, true, true),
            new PropertyField<String, Facility>(R.string.database_user_facilityID, o -> o instanceof String, true, true, Database.Collections.FACILITIES, true, true),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_user_email, o -> o instanceof String && !((String) o).isBlank(), true),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_user_phoneNumber, o -> o instanceof String, true),
            new PropertyField<Boolean, PropertyField.NullInstance>(R.string.database_user_adminNotifications, o -> o instanceof Boolean, true),
            new PropertyField<Boolean, PropertyField.NullInstance>(R.string.database_user_orgNotifications, o -> o instanceof Boolean, true),
            new PropertyField<Boolean, PropertyField.NullInstance>(R.string.database_user_isAdmin, o -> o instanceof Boolean, true),
            new PropertyField<Timestamp, PropertyField.NullInstance>(R.string.database_user_createdTime, o -> o instanceof Timestamp, false),

    };


    /**
     * Validates and creates a new User instance in the database using the given data.
     * @param db The database
     * @param deviceID The ID of the device associated with the User
     * @param name The name of the user
     * @param description The description of the user
     * @param profileImage the profileImage
     * @param facilityID The ID of the user's facility
     * @param email The email of the user
     * @param phoneNumber The phone number of the user
     * @param organizerNotifications If the user should receive notifications from organizers
     * @param adminNotifications If the user should receive notifications from admins
     * @param isAdmin If the user has admin privileges
     * @param listener Will be called once the user is initialized. Is not called if the data is invalid
     * @return The property id of all invalid properties
     * @see Database#createNewInstance(Database.Collections, String, Map, Database.InitializationListener) 
     * @throws IllegalArgumentException If deviceId is invalid
     */
    @Database.MustStir
    public static Set<Integer> NewInstance(Database db,
                                   String deviceID,
                                   String name,
                                   String description,
                                   @Nullable Uri profileImage,
                                   @Database.Dilutes String facilityID,
                                   String email,
                                   String phoneNumber,
                                   Boolean organizerNotifications,
                                   Boolean adminNotifications,
                                   Boolean isAdmin,
                                   Database.InitializationListener<User> listener
    ){
        if(deviceID == null || deviceID.isBlank()){
            db.throwE(new IllegalArgumentException("Illegal device ID: " + deviceID));
            return Set.of(-1);
        }

        Timestamp createdTime = Timestamp.now();

        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_user_name, name);
        map.put(R.string.database_user_description, description);
        map.put(R.string.database_user_facilityID, facilityID);
        map.put(R.string.database_user_email, email);
        map.put(R.string.database_user_phoneNumber, phoneNumber);
        map.put(R.string.database_user_adminNotifications, adminNotifications);
        map.put(R.string.database_user_orgNotifications, organizerNotifications);
        map.put(R.string.database_user_createdTime, createdTime);
        map.put(R.string.database_user_isAdmin, isAdmin);

        return db.createNewInstance(Database.Collections.USERS, deviceID, map, profileImage, name, listener);
    }
}
