package com.syzygy.events.database;

import androidx.annotation.NonNull;

import com.syzygy.events.R;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
    //TODO test if collectionID works with hashing - img_collectionid might be retrieved first
    private static final PropertyField<?, ?>[] fields = {
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_img_address, o -> o instanceof String && !((String) o).isBlank(), false),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_img_collectionID, o -> o instanceof String && !((String) o).isBlank(), false),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_img_instID, o -> o instanceof String && !((String) o).isBlank(), false, true, Database.Collections.IMAGES, null, false),
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
     * @param instanceID The ID of the database instance associated with this image
     * @param collection The collection associated with the database item
     * @param address The address of the ID
     * @param listener The initializer listener: this will be called once the user is ready
     * @return The user instance in an illegal state
     * @see Database#createNewInstance(Database.Collections, String, Map, Database.InitializationListener)
     */
    public static Image NewInstance(Database db,
                                   String instanceID,
                                   Database.Collections collection,
                                   String address,
                                   Database.InitializationListener<Image> listener
    ){
        Map<Integer,Object> map = createDataMap(instanceID, collection, address);

        if(!validateDataMap(map)){
            return null;
        }

        return db.createNewInstance(Database.Collections.IMAGES, collection.getDatabaseID(instanceID), db.convertIDMapToNames(map), listener);
    }

    /**
     * Turns the properties as arguments into a map that is usable by the database
     * @param instanceID The ID of the database instance associated with this image
     * @param collection The ID of the collection associated with the database item
     * @param address The address of the ID
     * @return The map
     */
    public static Map<Integer, Object> createDataMap(String instanceID,
                                                     Database.Collections collection,
                                                     String address

    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_img_collectionID, collection.toString());
        map.put(R.string.database_img_instID, instanceID);
        map.put(R.string.database_img_address, address);
        return map;
    }

    /**
     * Tests if the data is valid
     * @param dataMap The data map
     * @return The
     * @see #createDataMap(String, Database.Collections, String)
     */
    public static boolean validateDataMap(Map<Integer, Object> dataMap){
        return DatabaseInstance.isDataValid(dataMap, fields);
    }
}
