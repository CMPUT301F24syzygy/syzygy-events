package com.syzygy.events.database;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Query;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;
import com.squareup.picasso.Transformation;
import com.syzygy.events.R;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * An instance of an image database item
 * - An image cannot be edited
 * @author Gareth Kmet
 * @version 1.0
 * @since 19oct24
 *
 * TODO on image delete
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
        return setPropertyValue(R.string.database_img_locName, val, s -> {});
    }

    public boolean setLocType(Database.Collections val){
        return setPropertyValue(R.string.database_img_locType, val.toString(), s -> {});
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
    static final PropertyField<?, ?>[] fields = {
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
    protected void requiredFirstDelete(int deletionType, Consumer<Boolean> listener) {
        db.deleteImage(getImageID(), success -> {
            if(!success){
                listener.accept(false);
                return;
            }
            if((deletionType & DeletionType.HARD_DELETE) != 0){
                Database.Collections col = getLocType();
                db.modifyField(getLocType(), getLocID(), col.getAssociatedImagePropertyId(), "", listener);
            }
        });
    }

    /**
     * Validates and creates a new Image instance in the database using the given data.
     * @param db The database
     * @param locName the Name of where the image is stored
     * @param locType the type of where the image is stored
     * @param locID the database ID of where the image is stored
     * @param image the image file
     * @param listener This will be called once the image is initialized. Is not called if the data is invalid
     * @return The property id of all invalid properties
     * @see Database#createNewInstance(Database.Collections, String, Map, Database.InitializationListener)
     */
    @Database.MustStir
    public static Set<Integer> NewInstance(Database db,
                                   String locName,
                                   Database.Collections locType,
                                   String locID,
                                   @NonNull Uri image,
                                   Database.InitializationListener<Image> listener
    ){

        String docID = Database.Collections.IMAGES.getNewID(db);

        String imageID = locType.toString() + "/" + docID;

        Map<Integer,Object> map = new HashMap<>();
        map.put(R.string.database_img_locType, locType.toString());
        map.put(R.string.database_img_locName, locName);
        map.put(R.string.database_img_locID, locID);
        map.put(R.string.database_img_imgid, imageID);
        map.put(R.string.database_img_uploadTime, Timestamp.now());

        Set<Integer> invalidIDs = isDataValid(map, fields);
        if(!invalidIDs.isEmpty()) {
            Log.println(Log.DEBUG, "Newimage", "invalid");
            return invalidIDs;
        }

        db.addFileToStorage(imageID, image, address -> {
            if(address == null){
                Log.println(Log.DEBUG, "Newimage", "failed image");
                listener.onInitialization(null, false);
                return;
            }

            Log.println(Log.DEBUG, "Newimage", "created image file");

            map.put(R.string.database_img_address, address.toString());

            db.<Image>createNewInstance(Database.Collections.IMAGES, docID, map, (instance, success) -> {
                Log.println(Log.DEBUG, "Newimage", "created image");
                if(!success){
                    db.deleteImage(imageID, success2 -> {
                        if(!success2){
                            db.throwE(new IllegalStateException("Hanging Image: " + imageID + " :Image was created, uri retrieved, failed validation, failed to delete"));
                        }
                        listener.onInitialization(null, false);
                    });
                    return;
                }
                listener.onInitialization(instance, success);
            });
        });

        return invalidIDs;
    }

    /**
     * Loads the associated image of the instance and formats it.
     * If the instance is null, uses a default image.
     * If the associated image is null, uses a default image for the instances collection.
     * @param instance The instance whos image should be loaded
     * @param option The formatting options
     * @param resources The resources to use for creating the drawable
     * @param useDrawable Called on preparation with a placeHolder drawable, then again on completion
     *                    of load with the drawable or an error drawable if failed.
     *                    <ul>
     *                    <li>If placeholder, {@code success = null}</li>
     *                    <li>If error, {@code success = false}</li>
     *                    <li>If good, {@code success = true}</li>
     *                    </ul>
     * @see #getFormatedAssociatedImage(DatabaseInstance, Options)
     * @see #loadAsDrawable(RequestCreator, Resources, BiConsumer)
     */
    public static void getFormatedAssociatedImageAsDrawable(@Nullable @Database.Observes DatabaseInstance<?> instance, Options option, Resources resources, BiConsumer<Boolean, Drawable> useDrawable){
        loadAsDrawable(getFormatedAssociatedImage(instance, option), resources, useDrawable);
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
     * Formats the drawable
     * @param resID the id of the drawable
     * @param option How to format the image
     * @return The loaded picasso after formatting
     * @see #getDefaultImage(Database.Collections)
     */
    public static RequestCreator formatImage(@DrawableRes int resID, @NonNull Options option){
        Log.println(Log.DEBUG, "format", "formating");
        RequestCreator loadedPicasso =  Picasso.get().load(resID).placeholder(R.drawable.transparent).error(R.drawable.transparent);
        return formatImage(loadedPicasso, option);
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

    /**
     * Turns a loaded picasso into a drawable
     * @param loadedPicasso The picasso element that is loaded with the image.
     * @param resources The resources to use for creating the drawable
     * @param useDrawable Called on preparation with a placeHolder drawable, then again on completion
     *                    of load with the drawable or an error drawable if failed.
     *                    <ul>
     *                    <li>If placeholder, {@code success = null}</li>
     *                    <li>If error, {@code success = false}</li>
     *                    <li>If good, {@code success = true}</li>
     *                    </ul>
     */
    public static void loadAsDrawable(@NonNull RequestCreator loadedPicasso, Resources resources, BiConsumer<Boolean, Drawable> useDrawable){
        Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                useDrawable.accept(true, new BitmapDrawable(resources, bitmap));
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                useDrawable.accept(false, errorDrawable);
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                useDrawable.accept(null, placeHolderDrawable);
            }
        };
        loadedPicasso.into(target);
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

        /**
         * A list of predefined sizes
         */
        public enum Sizes {;
            public static final int
                    LARGE = 400,
                    MEDIUM = 256,
                    SMALL = 200,
                    ICON = 128;
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
