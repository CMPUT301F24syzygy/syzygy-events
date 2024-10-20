package com.syzygy.events.database;

import com.syzygy.events.R;

import java.util.HashMap;
import java.util.Map;

/**
 * An instance of a user database item
 * - A user can be edited
 * @author Gareth Kmet
 * @version 1.0
 * @since 19oct24
 */
public class User extends DatabaseInstance<User> {

    /**
     * Checks to make sure the generic type is the type of this instance
     *
     * @param db The database
     * @param deviceID The id of the device associated with the user
     */
    protected User(Database db, String deviceID) throws ClassCastException {
        super(db, deviceID, Database.Collections.USERS, fields);
    }

    @Override
    protected User cast() {
        return this;
    }

    /**
     * The list of the fields defined for a User
     */
    private static final PropertyField<?, ?>[] fields = {
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_user_name, o -> o instanceof String && !((String) o).isBlank(), true),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_user_description, o -> o instanceof String, true),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_user_profileID, o -> o instanceof String && !((String) o).isBlank(), true, true, Database.Collections.IMAGES, null, true),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_user_email, o -> o instanceof String && !((String) o).isBlank(), true),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_user_phoneNumber, o -> o instanceof String, true),
            new PropertyField<Boolean, PropertyField.NullInstance>(R.string.database_user_adminNotifications, o -> o instanceof Boolean, true),
            new PropertyField<Boolean, PropertyField.NullInstance>(R.string.database_user_orgNotifications, o -> o instanceof Boolean, true),

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
     * @param email The email of the user
     * @param phoneNumber The phone number of the user
     * @param organizerNotifications If the user should receive notifications from organizers
     * @param adminNotifications If the user should receive notifications from admins
     * @param listener The initializer listener: this will be called once the user is ready
     * @return The user instance in an illegal state
     * @see Database#createNewInstance(Database.Collections, String, Map, Database.InitializationListener)
     */
    public static User NewInstance(Database db,
                                   String deviceID,
                                   String name,
                                   String description,
                                   String profileImageID,
                                   String email,
                                   String phoneNumber,
                                   Boolean organizerNotifications,
                                   Boolean adminNotifications,
                                   Database.InitializationListener<User> listener
    ){
        Map<Integer,Object> map = createDataMap(name, description, profileImageID, email, phoneNumber, organizerNotifications, adminNotifications);

        if(!(validateDeviceID(deviceID) && validateDataMap(map))){
            return null;
        }

        return db.createNewInstance(Database.Collections.USERS, deviceID, db.convertIDMapToNames(map), listener);
    }

    public static boolean validateDeviceID(String deviceID){
        return deviceID != null && !deviceID.isBlank();
    }

    /**
     * Turns the properties as arguments into a map that is usable by the database
     * @param name The name of the user
     * @param description The description of the user
     * @param profileImageID The ID of the profile image
     * @param email The email of the user
     * @param phoneNumber The phone number of the user
     * @param organizerNotifications If the user should receive notifications from organizers
     * @param adminNotifications If the user should receive notifications from admins
     * @return The map
     */
    public static Map<Integer, Object> createDataMap(String name,
                                                     String description,
                                                     String profileImageID,
                                                     String email,
                                                     String phoneNumber,
                                                     Boolean organizerNotifications,
                                                     Boolean adminNotifications

    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_user_name, name);
        map.put(R.string.database_user_description, description);
        map.put(R.string.database_user_profileID, profileImageID);
        map.put(R.string.database_user_email, email);
        map.put(R.string.database_user_phoneNumber, phoneNumber);
        map.put(R.string.database_user_adminNotifications, adminNotifications);
        map.put(R.string.database_user_orgNotifications, organizerNotifications);
        return map;
    }

    /**
     * Tests if the data is valid
     * @param dataMap The data map
     * @return The
     * @see #createDataMap(String, String, String, String, String, Boolean, Boolean)
     */
    public static boolean validateDataMap(Map<Integer, Object> dataMap){
        return DatabaseInstance.isDataValid(dataMap, fields);
    }

}
