package com.syzygy.events.database;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Pair;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Query;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.syzygy.events.R;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

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

    public String getImageID(){
        return getPropertyValueI(R.string.database_img_imgid);
    }

    public Timestamp getUploadTime(){
        return getPropertyValueI(R.string.database_img_uploadTime);
    }

    public String getAddress(){
        return getPropertyValueI(R.string.database_img_address);
    }

    @Override
    @Database.Observes
    public Image getAssociatedImage() {
        return this;
    }

    /**
     * Loads the image into a picasso request and formats it based on the collection type
     * @param option How to format the image
     * @return The picasso creator with this image formated
     * @see #formatImage(RequestCreator, Database.Collections, FormattingOptions)
     */
    public RequestCreator loadAndFormatImage(FormattingOptions option){
        RequestCreator req = Picasso.get().load(getAddress()).placeholder(R.drawable.ic_launcher_background).error(R.drawable.ic_home_black_24dp);
        return formatImage(req, getCollection(), option);
    }


    /**
     * The list of the fields defined for a User
     */
    private static final PropertyField<?, ?>[] fields = {
            new PropertyField<String, PropertyField.NullInstance>(R.string.database_img_imgid, o -> o instanceof String && !((String) o).isBlank(), false),
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
        db.deleteImage(getImageID(), listener::onCompletion);
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

        String docID = Database.Collections.IMAGES.getNewID(db);

        String imageID = locType.toString() + "/" + docID;

        db.addFileToStorage(imageID, image, address -> {
            if(address == null){
                listener.onInitialization(null, false);
                return;
            }
            Map<Integer,Object> map = createDataMap(locName, locType.toString(), locID, imageID, address.toString(), Timestamp.now());

            if(!validateDataMap(map).isEmpty()){
                db.deleteImage(imageID, success2 -> {
                    if(!success2){
                        db.throwE(new IllegalStateException("Hanging Image: " + imageID + " :Image was created, uri retrieved, failed validation, failed to delete"));
                    }
                    listener.onInitialization(null, false);
                });
                return;
            }
            db.createNewInstance(Database.Collections.IMAGES, docID, db.convertIDMapToNames(map), listener);
        });
    }


    /**
     * Turns the properties as arguments into a map that is usable by the database
     * @param locName the Name of where the image is stored
     * @param locType the type of where the image is stored
     * @param locID the database ID of where the image is stored
     * @param imgid The id of the image with the storage
     * @param address The download url of the image
     * @param uploadTime The time when the image was uploaded
     * @return The map
     */
    public static Map<Integer, Object> createDataMap(String locName,
                                                     String locType,
                                                     String locID,
                                                     String imgid,
                                                     String address,
                                                     Timestamp uploadTime

    ){
        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_img_locType, locType);
        map.put(R.string.database_img_locName, locName);
        map.put(R.string.database_img_locID, locID);
        map.put(R.string.database_img_imgid, imgid);
        map.put(R.string.database_img_address, address);
        map.put(R.string.database_img_uploadTime, uploadTime);
        return map;
    }

    /**
     * Tests if the data is valid
     * @param dataMap The data map
     * @return The invalid ids
     * @see #createDataMap(String, String, String, String, String, Timestamp) 
     */
    public static Set<Integer> validateDataMap(Map<Integer, Object> dataMap){
        return com.syzygy.events.database.DatabaseInstance.isDataValid(dataMap, fields);
    }

    /**
     * Loads the associated image of the instance and formats it.
     * If the instance is null, uses a default image.
     * If the associated image is null, uses a default image for the instances collection.
     * @param instance The instance whos image should be loaded
     * @return The loaded and formatted image. Uses {@code .into(view)} to load the image to an {@code ImageView}
     * @see #formatImage(RequestCreator, Database.Collections, FormattingOptions)
     * @see #getDefaultImage(Database.Collections)
     */
    public static RequestCreator getFormatedAssociatedImage(@Nullable @Database.Observes DatabaseInstance<?> instance, FormattingOptions option){
        Picasso pic = Picasso.get();
        RequestCreator loadedPic;
        if(instance == null){
            return formatImage(null, null, option);
        }
        Image img = instance.getAssociatedImage();
        Database.Collections coll = instance.getCollection();
        if(img == null){
            return formatImage(null, coll, option);
        }
        return img.loadAndFormatImage(option);
    }

    /**
     * Gets and loads a default image for the collection
     * @param collection The collection
     * @return The loaded request creator for the image
     */
    public static RequestCreator getDefaultImage(@Nullable Database.Collections collection){
        return Picasso.get().load(R.drawable.ic_launcher_background);
    }

    /**
     * Formats the loaded image based on the collection
     * @param loadedPicasso The picasso element that is loaded with the image. If null, gets a default image for the collection
     * @param collection The collection to base styles on
     * @param option How to format the image
     * @return The loaded picasso after formatting
     * @see #getDefaultImage(Database.Collections)
     */
    public static RequestCreator formatImage(@Nullable RequestCreator loadedPicasso, @Nullable Database.Collections collection, @NonNull FormattingOptions option){
        RequestCreator pic = loadedPicasso == null ? getDefaultImage(collection) : loadedPicasso;
        switch(option){
            case AS_IS:
                break;
            case LIST_ITEM:
                pic.resize(64,64);
            case PROFILE_PAGE:
                pic.resize(256,256);
        }

        return loadedPicasso;
    }

    public enum FormattingOptions {
        /**
         * Returns the image as retrieved from the database
         */
        AS_IS,
        /**
         * Returns the image as a big avatar
         */
        PROFILE_PAGE,
        /**
         * Returns the image as a small avatar
         */
        LIST_ITEM
    }

}
