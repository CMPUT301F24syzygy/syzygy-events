package com.syzygy.events.database;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Query;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Transformation;
import com.syzygy.events.R;

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
     * @see #formatImage(RequestCreator, Database.Collections, Options)
     */
    public RequestCreator loadAndFormatImage(Options option){
        RequestCreator req = Picasso.get().load(getAddress()).placeholder(R.drawable.two_apples).error(R.drawable.two_apples);
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
     * @see #formatImage(RequestCreator, Database.Collections, Options)
     * @see #getDefaultImage(Database.Collections)
     */
    public static RequestCreator getFormatedAssociatedImage(@Nullable @Database.Observes DatabaseInstance<?> instance, Options option){
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
        //Must be jpg or png
        return Picasso.get().load(R.drawable.two_apples);
    }

    /**
     * Formats the loaded image based on the collection
     * @param loadedPicasso The picasso element that is loaded with the image. If null, gets a default image for the collection
     * @param collection The collection to base the default on if the loaded is null
     * @param option How to format the image
     * @return The loaded picasso after formatting
     * @see #getDefaultImage(Database.Collections)
     */
    public static RequestCreator formatImage(@Nullable RequestCreator loadedPicasso, @Nullable Database.Collections collection, @NonNull Options option){
        RequestCreator pic = loadedPicasso == null ? getDefaultImage(collection) : loadedPicasso;
        Log.println(Log.DEBUG, "format", "formating");
        option.modifyImage(pic);
        return pic;
    }

    public enum Options {
        /**
         * Returns the image as retrieved from the database
         */
        AS_IS(-1,false),
        /**
         * Returns the image as the biggest circular image
         */
        CIRCLE(-1,true),
        /**
         * Returns the image as a circular 256*256 image
         */
        BIG_AVATAR(256, true),
        /**
         * Returns the image as a square 256*256 image
         */
        BIG_SQUARE(256,false),
        /**
         * Returns the image as a circular 128*128 image
         */
        MEDIUM_AVATAR(128,true),
        /**
         * Returns the image as a square 128*128 image
         */
        MEDIUM_SQUARE(128,false),
        /**
         * Returns the image as a circular 64*64 image
         */
        SMALL_AVATAR(64,true),
        /**
         * Returns the image as a square 64*64 image
         */
        SMALL_SQUARE(64,false),
        /**
         * Returns the image as a circular 32*32 image
         */
        TINY_AVATAR(32,true),
        /**
         * Returns the image as a square 32*32 image
         */
        TINY_SQUARE(32,false);

        private final Consumer<RequestCreator> funct;

        private Options(int squareSize, boolean isCircle){
            if(squareSize >= 0 && isCircle){
                funct = img -> {
                    square(img, squareSize);
                    circle(img);
                };
            }else if(squareSize >= 0){
                funct = img -> square(img, squareSize);
            }else if(isCircle){
                funct = this::circle;
            }else{
                funct = null;
            }
        }

        private Options(Consumer<RequestCreator> modify){
            funct = modify;
        }

        public void modifyImage(RequestCreator img){
            if(funct == null) return;
            funct.accept(img);
        }

        private void circle(RequestCreator img){
            img.transform(new CircleTransform());
        }

        private void square(RequestCreator img, int size){
            img.resize(size, size);
        }

    }

    //https://stackoverflow.com/questions/26112150/android-create-circular-image-with-picasso
    public static class CircleTransform implements Transformation {
        @Override
        public Bitmap transform(Bitmap source) {
            int size = Math.min(source.getWidth(), source.getHeight());

            int x = (source.getWidth() - size) / 2;
            int y = (source.getHeight() - size) / 2;

            Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
            if (squaredBitmap != source) {
                source.recycle();
            }

            Bitmap bitmap = Bitmap.createBitmap(size, size, source.getConfig());

            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            BitmapShader shader = new BitmapShader(squaredBitmap,
                    Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            paint.setShader(shader);
            paint.setAntiAlias(true);

            float r = size / 2f;
            canvas.drawCircle(r, r, r, paint);

            squaredBitmap.recycle();
            return bitmap;
        }

        @Override
        public String key() {
            return "circle";
        }
    }

}
