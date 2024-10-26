package com.syzygy.events.database;

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
        return setPropertyValue(R.string.database_user_name, val);
    }

    public String getDescription(){
        return getPropertyValueI(R.string.database_user_description);
    }

    public boolean setDescription(String val){
        return setPropertyValue(R.string.database_user_description, val);
    }

    public String getProfileImageID(){
        return getPropertyValueI(R.string.database_user_profileID);
    }

    public boolean setProfileImageID(@Database.Dilutes String val){
        return setPropertyValue(R.string.database_user_profileID, val);
    }

    public String getFacilityID(){
        return getPropertyValueI(R.string.database_user_facilityID);
    }

    public boolean setFacilityID(@Database.Dilutes String val){
        return setPropertyValue(R.string.database_user_facilityID, val);
    }

    public String getEmail(){
        return getPropertyValueI(R.string.database_user_email);
    }

    public boolean setEmail(String val){
        return setPropertyValue(R.string.database_user_email, val);
    }

    public String getPhoneNumber(){
        return getPropertyValueI(R.string.database_user_phoneNumber);
    }

    public boolean setPhoneNumber(String val){
        return setPropertyValue(R.string.database_user_phoneNumber, val);
    }

    public Boolean getAdminNotifications(){
        return getPropertyValueI(R.string.database_user_adminNotifications);
    }

    public boolean setAdminNotifications(Boolean val){
        return setPropertyValue(R.string.database_user_adminNotifications, val);
    }

    public Boolean getOrganizerNotifications(){
        return getPropertyValueI(R.string.database_user_orgNotifications);
    }

    public boolean setOrganizerNotifications(Boolean val){
        return setPropertyValue(R.string.database_user_orgNotifications, val);
    }

    public Boolean isAdmin(){
        return getPropertyValueI(R.string.database_user_isAdmin);
    }

    public boolean makeAdmin(Boolean val){
        return setPropertyValue(R.string.database_user_isAdmin, val);
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
     * @param val The new instance
     */
    @Database.StirsDeep(what = "The previous Image")
    public boolean setProfileImage(@Nullable @Database.Dilutes Image val){
        return setPropertyInstance(R.string.database_user_profileID, val);
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
        return setPropertyInstance(R.string.database_user_facilityID, val);
    }

    /**
     * Updates all properties of the user. If the user changes, a notification is sent to listeners once and the database is updated once
     * @param name The name of the user
     * @param description The description of the user
     * @param profileImageID The ID of the profile image
     * @param email The email of the user
     * @param phoneNumber The phone number of the user
     * @param organizerNotifications If the user should receive notifications from organizers
     * @param adminNotifications If the user should receive notifications from admins
     * @param isAdmin If the user has admin privileges
     * @return If the user changed as a result
     */
    @Database.StirsDeep(what = "The previous image")
    public boolean update(String name,
                          String description,
                          @Database.Dilutes String profileImageID,
                          String email,
                          String phoneNumber,
                          Boolean organizerNotifications,
                          Boolean adminNotifications,
                          Boolean isAdmin
    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_user_name, name);
        map.put(R.string.database_user_description, description);
        map.put(R.string.database_user_profileID, profileImageID);
        map.put(R.string.database_user_email, email);
        map.put(R.string.database_user_phoneNumber, phoneNumber);
        map.put(R.string.database_user_adminNotifications, adminNotifications);
        map.put(R.string.database_user_orgNotifications, organizerNotifications);
        map.put(R.string.database_user_isAdmin, isAdmin);
        return updateDataFromMap(db.convertIDMapToNames(map));
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
    private static final PropertyField<?, ?>[] fields = {
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
     * Creates a new User instance in the database using the given data.
     * <p>
     *     Data is validated before creating. If the data is invalid, {@code null} is returned
     * </p>
     * <p>
     *     The instance will be invalid on return, only use it after waiting for the initialization listener
     * </p>
     * @param db The database
     * @param deviceID The ID of the device associated with the User
     * @param name The name of the user
     * @param description The description of the user
     * @param profileImageID The ID of the profile image
     * @param facilityID The ID of the user's facility
     * @param email The email of the user
     * @param phoneNumber The phone number of the user
     * @param organizerNotifications If the user should receive notifications from organizers
     * @param adminNotifications If the user should receive notifications from admins
     * @param isAdmin If the user has admin privileges
     * @param listener The initializer listener: this will be called once the user is ready
     * @see Database#createNewInstance(Database.Collections, String, Map, Database.InitializationListener)
     */
    @Database.MustStir
    public static void NewInstance(Database db,
                                   String deviceID,
                                   String name,
                                   String description,
                                   @Database.Dilutes String profileImageID,
                                   @Database.Dilutes String facilityID,
                                   String email,
                                   String phoneNumber,
                                   Boolean organizerNotifications,
                                   Boolean adminNotifications,
                                   Boolean isAdmin,
                                   Database.InitializationListener<User> listener
    ){
        Map<Integer,Object> map = createDataMap(name, description, profileImageID, facilityID, email, phoneNumber, organizerNotifications, adminNotifications, isAdmin, Timestamp.now());

        if(!(validateDeviceID(deviceID) && validateDataMap(map))){
            listener.onInitialization(null, false);
            return;
        }

        db.createNewInstance(Database.Collections.USERS, deviceID, db.convertIDMapToNames(map), listener);
    }

    public static boolean validateDeviceID(String deviceID){
        return deviceID != null && !deviceID.isBlank();
    }

    /**
     * Turns the properties as arguments into a map that is usable by the database
     * @param name The name of the user
     * @param description The description of the user
     * @param profileImageID The ID of the profile image
     * @param facilityID The ID of the user's facility
     * @param email The email of the user
     * @param phoneNumber The phone number of the user
     * @param organizerNotifications If the user should receive notifications from organizers
     * @param adminNotifications If the user should receive notifications from admins
     * @param isAdmin If the user has admin privileges
     * @param createdTime The time when the account was created
     * @return The map
     */
    public static Map<Integer, Object> createDataMap(String name,String description,
                                                      @Database.Observes String profileImageID,
                                                      @Database.Observes String facilityID,
                                                      String email,
                                                      String phoneNumber,
                                                      Boolean organizerNotifications,
                                                      Boolean adminNotifications,
                                                      Boolean isAdmin,
                                                      Timestamp createdTime

    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_user_name, name);
        map.put(R.string.database_user_description, description);
        map.put(R.string.database_user_profileID, profileImageID);
        map.put(R.string.database_user_facilityID, facilityID);
        map.put(R.string.database_user_email, email);
        map.put(R.string.database_user_phoneNumber, phoneNumber);
        map.put(R.string.database_user_adminNotifications, adminNotifications);
        map.put(R.string.database_user_orgNotifications, organizerNotifications);
        map.put(R.string.database_user_createdTime, createdTime);
        map.put(R.string.database_user_isAdmin, isAdmin);
        return map;
    }

    /**
     * Tests if the data is valid
     * @param dataMap The data map
     * @return The
     * @see #createDataMap(String, String, String, String, String, String, Boolean, Boolean, Boolean, Timestamp)
     */
    public static boolean validateDataMap(@Database.Observes Map<Integer, Object> dataMap){
        return DatabaseInstance.isDataValid(dataMap, fields);
    }


}
