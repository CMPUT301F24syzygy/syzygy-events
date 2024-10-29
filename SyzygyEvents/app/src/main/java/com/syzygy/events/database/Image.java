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
     * @see #formatImage(RequestCreator, Options)
     */
    public RequestCreator loadAndFormatImage(Options option){
        RequestCreator req = Picasso.get().load(getAddress());
        return formatImage(req, option);
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
     * @see #formatImageOrDefault(RequestCreator, Database.Collections, Options) 
     * @see #getDefaultImage(Database.Collections)
     */
    public static RequestCreator getFormatedAssociatedImage(@Nullable @Database.Observes DatabaseInstance<?> instance, Options option){
        if(instance == null){
            return formatDefaultImage(null, option);
        }
        Image img = instance.getAssociatedImage();
        Database.Collections coll = instance.getCollection();
        if(img == null){
            return formatDefaultImage(null, option);
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
        return Picasso.get().load(R.drawable.penguin_blue);
    }

    /**
     * Formats the loaded image based on the collection
     * @param loadedPicasso The picasso element that is loaded with the image. If null, gets a default image for the collection
     * @param collection The collection to base the default on if the loaded is null
     * @param option How to format the image
     * @return The loaded picasso after formatting
     * @see #formatDefaultImage(Database.Collections, Options)
     * @see #formatImage(RequestCreator, Options)
     * @see #getDefaultImage(Database.Collections)
     */
    public static RequestCreator formatImageOrDefault(@Nullable RequestCreator loadedPicasso, @Nullable Database.Collections collection, @NonNull Options option){
        if(loadedPicasso == null){
            return formatDefaultImage(collection, option);
        }else{
            return formatImage(loadedPicasso, option);
        }
    }

    /**
     * Formats a default image based on the collection
     * @param collection The collection to base the default on
     * @param option How to format the image
     * @return The loaded picasso after formatting
     * @see #formatImage(RequestCreator, Options) 
     * @see #getDefaultImage(Database.Collections)
     */
    public static RequestCreator formatDefaultImage(@Nullable Database.Collections collection, @NonNull Options option){
        RequestCreator pic = getDefaultImage(collection);
        return formatImage(getDefaultImage(collection), option);
    }

    /**
     * Formats the loaded image based on the collection
     * @param loadedPicasso The picasso element that is loaded with the image.
     * @param option How to format the image
     * @return The loaded picasso after formatting
     * @see #getDefaultImage(Database.Collections)
     */
    public static RequestCreator formatImage(@NonNull RequestCreator loadedPicasso, @NonNull Options option){
        Log.println(Log.DEBUG, "format", "formating");
        loadedPicasso.placeholder(R.drawable.transparent).error(R.drawable.transparent);
        option.modifyImage(loadedPicasso);
        return loadedPicasso;
    }

    public static class Options {

        private final int width, height;
        private final boolean isCircle;
        public Options(int width, int height, boolean isCircle){
            this.width = width; this.height = height;
            this.isCircle = isCircle;
        }

        /**
         * Modifies the image based off the parameters of the option
         * @param img The image to modify
         */
        public void modifyImage(RequestCreator img){
            if(width >= 0 && height >= 0){
                img.resize(width, height);
            }
            if(isCircle){
                img.transform(new CircleTransform());
            }
        }

        /**
         * Returns the image resized to size*size
         * @param size The width and height of the image
         */
        public static Options Square(int size){
            return new Options(size, size, false);
        }

        /**
         * Returns the image cropped to the largest circle that can fit within the image
         */
        public static Options LargestCircle(){
            return new Options(-1, -1, true);
        }

        /**
         * Returns the image resized to a size*size square then cropped to the largest circle that fits within
         * @param size the diameter of the circle
         */
        public static Options Circle(int size){
            return new Options(size, size, true);
        }

        /**
         * Returns the image as is
         */
        public static Options AsIs(){
            return new Options(-1,-1,false);
        }


    }

    //https://stackoverflow.com/questions/26112150/android-create-circular-image-with-picasso
    private static class CircleTransform implements Transformation {
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
