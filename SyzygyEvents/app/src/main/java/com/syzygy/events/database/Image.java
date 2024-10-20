package com.syzygy.events.database;

import com.syzygy.events.R;

import java.util.HashMap;
import java.util.Map;

/**
 * An instance of an image database item
 * - An image cannot be edited
 * @author Gareth Kmet
 * @version 1.0
 * @since 19oct24
 */
public class Image extends DatabaseInstance<Image> {

    /**
     * Checks to make sure the generic type is the type of this instance
     *
     * @param db The database
     * @param imageID The id of the image
     */
    protected Image(Database db, String imageID) throws ClassCastException {
        super(db, imageID, Database.Collections.IMAGES, fields);
    }

    @Override
    protected Image cast() {
        return this;
    }

    /**
     * The list of the fields defined for a User
     */
    private static final PropertyField<?, ?>[] fields = {
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_img_address, o -> o instanceof String && !((String) o).isBlank(), false),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_img_locName, o -> o instanceof String && !((String) o).isBlank(), false),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_img_locType, o -> o instanceof String && !((String) o).isBlank(), false),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_img_locID, o -> o instanceof String && !((String) o).isBlank(), false)
    };


    /**
     * Creates a new Image instance in the database using the given data.
     * <p>
     *     Data is validated before creating. If the data is invalid, {@code null} is returned
     * </p>
     * <p>
     *     The instance will be invalid on return, only use it after waiting for the initialization listener
     * </p>
     * @param db The database
     * @param locName the Name of where the image is stored
     * @param locType the type of where the image is stored
     * @param locID the database ID of where the image is stored
     * @param listener The initializer listener: this will be called once the user is ready
     * @return The user instance in an illegal state
     * @see Database#createNewInstance(Database.Collections, String, Map, Database.InitializationListener)
     */
    public static Image NewInstance(Database db,
                                   String locName,
                                   String locType,
                                   String locID,
                                   String address,
                                   Database.InitializationListener<Image> listener
    ){
        Map<Integer,Object> map = createDataMap(locName, locType, locID, address);

        if(!validateDataMap(map)){
            return null;
        }

        return db.createNewInstance(Database.Collections.IMAGES, locID, db.convertIDMapToNames(map), listener);
    }

    /**
     * Turns the properties as arguments into a map that is usable by the database
     * @param locName the Name of where the image is stored
     * @param locType the type of where the image is stored
     * @param locID the database ID of where the image is stored
     * @param address The address of the ID
     * @return The map
     */
    public static Map<Integer, Object> createDataMap(String locName,
                                                     String locType,
                                                     String locID,
                                                     String address

    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_img_locType, locType);
        map.put(R.string.database_img_locName, locName);
        map.put(R.string.database_img_locID, locID);
        map.put(R.string.database_img_address, address);
        return map;
    }

    /**
     * Tests if the data is valid
     * @param dataMap The data map
     * @return The
     * @see #createDataMap(String, String, String, String)
     */
    public static boolean validateDataMap(Map<Integer, Object> dataMap){
        return DatabaseInstance.isDataValid(dataMap, fields);
    }
}
