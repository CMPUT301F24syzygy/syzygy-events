package com.syzygy.events.database;

import android.net.Uri;
import android.util.Pair;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Query;
import com.syzygy.events.R;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An instance of an image database item
 * - An image cannot be edited
 * @author Gareth Kmet
 * @version 1.0
 * @since 19oct24
 */
@Database.Dissovable
public class Image extends DatabaseInstance<Image> {

    /**
     * Checks to make sure the generic type is the type of this instance
     *
     * @param db The database
     * @param imageID The id of the image
     */
    @Database.Salty
    protected Image(Database db, String imageID) throws ClassCastException {
        super(db, imageID, Database.Collections.IMAGES, fields);
    }

    @Override
    @Database.Observes
    protected Image cast() {
        return this;
    }

    public Database.Collections getLocType(){
        return Database.Collections.valueOf((String)getPropertyValue(R.string.database_img_locType));
    }

    public String getLocName(){
        return getPropertyValueI(R.string.database_img_locName);
    }

    public String getLocID(){
        return getPropertyValueI(R.string.database_img_locID);
    }

    public boolean setLocID(String val){
        return setPropertyValue(R.string.database_img_locID, val, s -> {});
    }

    public boolean setLocName(String val){
        return setPropertyValue(R.string.database_img_locID, val, s -> {});
    }

    public boolean setLocType(Database.Collections val){
        return setPropertyValue(R.string.database_img_locID, val.toString(), s -> {});
    }

    public String getAddress(){
        return getPropertyValueI(R.string.database_img_address);
    }

    public Uri getImage()  {
        return Uri.parse(getAddress());
    }

    public Timestamp getUploadTime(){
        return getPropertyValueI(R.string.database_img_uploadTime);
    }


    /**
     * The list of the fields defined for a User
     */
    private static final PropertyField<?, ?>[] fields = {
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_img_address, o -> o instanceof String && !((String) o).isBlank(), false),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_img_locName, o -> o instanceof String, true),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_img_locType, o -> o instanceof String && !((String)o).isBlank(), true),
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_img_locID, o -> o instanceof String, true),
            new PropertyField<Timestamp, PropertyField.NullInstance>(R.string.database_img_uploadTime, o -> o instanceof Timestamp, false)
    };

    @Override
    protected List<Pair<Query, Database.Collections>> subInstanceCascadeDeleteQuery() {
        return Collections.emptyList();
    }

    @Override
    protected void requiredFirstDelete(Database.Querrier.EmptyListener listener) {
        db.deleteImage(getDocumentID(), listener::onCompletion);
    }

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
     * @param image the image file
     * @param listener The initializer listener: this will be called once the user is ready
     * @see Database#createNewInstance(Database.Collections, String, Map, Database.InitializationListener)
     */
    @Database.MustStir
    public static void NewInstance(Database db,
                                   String locName,
                                   Database.Collections locType,
                                   String locID,
                                   Uri image,
                                   Database.InitializationListener<Image> listener
    ){
        if(image == null){
            listener.onInitialization(null, false);
            return;
        }

        db.addImageToStorage(locID, image, success -> {
            if(!success){
                listener.onInitialization(null, false);
                return;
            }
            db.getImageURL(locID, uri -> {
                if(uri == null){
                    db.deleteImage(locID, success2 -> {
                        if(!success2){
                            db.throwE(new IllegalStateException("Hanging Image: " + locID + " :Image was created, failed to get uri, failed to delete"));
                        }
                        listener.onInitialization(null, false);
                    });
                    return;
                }

                String address = uri.toString();

                Map<Integer,Object> map = createDataMap(locName, locType.toString(), locID, address, Timestamp.now());

                if(!validateDataMap(map).isEmpty()){
                    db.deleteImage(locID, success2 -> {
                        if(!success2){
                            db.throwE(new IllegalStateException("Hanging Image: " + locID + " :Image was created, uri retrieved, failed validation, failed to delete"));
                        }
                        listener.onInitialization(null, false);
                    });
                    return;
                }
                db.createNewInstance(Database.Collections.IMAGES, locID, db.convertIDMapToNames(map), listener);
            });
        });
    }


    /**
     * Turns the properties as arguments into a map that is usable by the database
     * @param locName the Name of where the image is stored
     * @param locType the type of where the image is stored
     * @param locID the database ID of where the image is stored
     * @param address The address of the ID
     * @param uploadTime The time when the image was uploaded
     * @return The map
     */
    public static Map<Integer, Object> createDataMap(String locName,
                                                     String locType,
                                                     String locID,
                                                     String address,
                                                     Timestamp uploadTime

    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_img_locType, locType);
        map.put(R.string.database_img_locName, locName);
        map.put(R.string.database_img_locID, locID);
        map.put(R.string.database_img_address, address);
        map.put(R.string.database_img_uploadTime, uploadTime);
        return map;
    }

    /**
     * Tests if the data is valid
     * @param dataMap The data map
     * @return The invalid ids
     * @see #createDataMap(String, String, String, String, Timestamp)
     */
    public static Set<Integer> validateDataMap(Map<Integer, Object> dataMap){
        return DatabaseInstance.isDataValid(dataMap, fields);
    }
}
