package com.syzygy.events;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.widget.ImageView;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.Timestamp;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInstance;
import com.syzygy.events.database.Image;
import com.syzygy.events.database.User;
import com.syzygy.events.ui.AdminActivity;
import com.syzygy.events.ui.EntrantActivity;
import com.syzygy.events.ui.OrganizerActivity;
import com.syzygy.events.ui.SignupActivity;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The main application class
 * This class handles the navigation between the different activities
 * This class manages the database and keeps track of which user is logged in
 * Also contains helper functions for activities
 */
public class SyzygyApplication extends Application implements Consumer<RuntimeException> {

    /**
     * The id of the User account associated with the Syzygy system
     */
    public static final String SYSTEM_ACCOUNT_ID = "0";

    /**
     * The request code to use when requesting permissions for location
     */
    public static final int LOCATION_REQUEST_CODE = 359;

    /**
     * The database instance for this appp
     */
    private Database db;
    /**
     * The user logged into the device
     */
    private User user;
    /**
     * The id of the device
     */
    private String deviceID;
    /**
     * The current activity displayed to the user
     */
    private SyzygyActivity currentActivity;
    /**
     * The client to get the location
     */
    private FusedLocationProviderClient location;

    /**
     * Set of listeners that are waiting on receiving the device's location
     */
    private final List<Consumer<Location>> locationListeners = new ArrayList<>();
    /**
     * Set of listeners that are waiting on receiving a user selected image
     */
    private final List<Consumer<Uri>> imageListeners = new ArrayList<>();

    /**
     * The icon to use for admin menu option
     */
    Drawable menuAdminIcon;
    /**
     * The icon to use for the create facility menu option
     */
    Drawable menuAddFacilityIcon;

    @Override
    public void onCreate() {
        super.onCreate();
        db = new Database(getResources());
        db.addErrorListener(this);
        deviceID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        db.<User>getInstance(Database.Collections.USERS, deviceID, (instance, success) -> {
            if(success) this.user = instance;
            switchToActivity(success ? EntrantActivity.class : SignupActivity.class);
        });

        Image.loadAsDrawable(Image.formatImage(R.drawable.default_user, Image.Options.Circle(Image.Options.Sizes.ICON)), getResources(), (s,d) -> {
            menuAdminIcon = d;
        });
        Image.loadAsDrawable(Image.formatImage(R.drawable.create_facility, Image.Options.Circle(Image.Options.Sizes.ICON)), getResources(), (s,d) -> {
            menuAddFacilityIcon = d;
        });

        location = LocationServices.getFusedLocationProviderClient(this);

    }

    /**
     * Loads the icon for the account menu
     * @param menu The menu
     */
    public void loadMenuIcon(Menu menu){
        Class<? extends SyzygyActivity> clazz = currentActivity.getClass();
        if(clazz == EntrantActivity.class){
            Image.getFormatedAssociatedImageAsDrawable(getUser(), Database.Collections.USERS, Image.Options.Circle(Image.Options.Sizes.ICON), getResources(), (s, d) -> {
                menu.getItem(0).setIcon(d);
            });
        }else if(clazz == OrganizerActivity.class){
            Image.getFormatedAssociatedImageAsDrawable(getUser().getFacility(), Database.Collections.FACILITIES, Image.Options.Circle(Image.Options.Sizes.ICON), getResources(), (s, d) -> {
                menu.getItem(0).setIcon(d);
            });
        }else if(clazz == AdminActivity.class){
            menu.getItem(0).setIcon(menuAdminIcon);
        }
    }

    /**
     * Loads the icons for the account menu options
     * @param menu The menu
     */
    public void loadMenuItemIcons(Menu menu){
        Image.getFormatedAssociatedImageAsDrawable(getUser(), Database.Collections.USERS, Image.Options.Circle(Image.Options.Sizes.ICON), getResources(), (s, d) -> {
            menu.findItem(R.id.entrant_item).setIcon(d);
        });
        Image.getFormatedAssociatedImageAsDrawable(getUser().getFacility(), Database.Collections.FACILITIES, Image.Options.Circle(Image.Options.Sizes.ICON), getResources(), (s, d) -> {
            menu.findItem(R.id.organizer_item).setIcon(d);
        });
        menu.findItem(R.id.admin_item).setIcon(menuAdminIcon);
        menu.findItem(R.id.add_organizer_item).setIcon(menuAddFacilityIcon);
    }

    @Override
    public void onTerminate() {
        if(user != null)user.dissolve();
        if(db != null) db.cleanup();
        super.onTerminate();
    }

    /**
     * Called on create of activities to register that this activity is the current activity
     * @param activity The now current activity
     */
    void registerActivity(SyzygyActivity activity){
        this.currentActivity = activity;
    }

    /**
     * @return The user currently using this app
     */
    @Database.Observes
    public User getUser(){
        return user;
    }

    /**
     * Gets the database
     * @return the database
     */
    public Database getDatabase(){
        return db;
    }

    /**
     * @return If permission has been granted to get location
     */
    private boolean canGetLocation(){
        return ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Gets the current location and actions all location listeners. Location listeners are passed
     * {@code null} if permissions are not granted or the location fails.
     * Clears the listeners list after actioning.
     */
    @SuppressLint("MissingPermission")
    private void retrieveLocation(){
        if(canGetLocation()){
            location.getLastLocation().addOnCompleteListener(task -> {
                if(!task.isSuccessful()){
                    pingLocation(false);
                    return;
                }
                Location loc = task.getResult();
                locationListeners.forEach(l -> l.accept(loc));
                locationListeners.clear();
            });
        }else{
            pingLocation(false);
        }
    }


    /**
     * Called when users grants/declines location permission.
     * Actions all waiting location listeners. Passes {@code null} if permissions are not granted or the location fails.
     * Clears the listeners list after retrieval.
     * @param granted If permission was granted
     */
    void pingLocation(boolean granted){
        if(granted){
            retrieveLocation();
        }else{
            for(Consumer<Location> l : locationListeners){
                l.accept(null);
            }
            locationListeners.clear();
        }
    }

    /**
     * Called when the user selects an image.
     * Sends the image to all current listeners
     * @param image The image
     */
    void sendImage(Uri image){
        for(Consumer<Uri> l : imageListeners){
            l.accept(image);
        }
        imageListeners.clear();
    }

    /**
     * Gets the location
     * <p>
     *     Checks if location permissions are granted. If not, asks for permission.
     *     Then actions the listener with {@code null} if permissions are still not granted or an error occurs.
     *     Otherwise passes the location
     * </p>
     * @param locationListener The listener. Called when the location has been retrieved
     */
    public void getLocation(Consumer<Location> locationListener){
        locationListeners.add(locationListener);
        if(!canGetLocation()){
            ActivityCompat.requestPermissions(currentActivity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST_CODE);
        }else{
            retrieveLocation();
        }
    }

    /**
     * Gets an image from the user
     * <p>
     *     Checks if file search permissions are granted. If not, asks for permission.
     *     Then actions the listener with {@code null} if permissions are still not granted or an error occurs.
     *     Otherwise passes the image
     * </p>
     * @param imageListener The listener. Called when the image has been retrieved
     */
    public void getImage(Consumer<Uri> imageListener){
        Log.println(Log.DEBUG, "image", "retrieve image");
        imageListeners.add(imageListener);
        currentActivity.getMediaContent.launch("image/*");
    }

    /**
     * Displays the associated image of the instance to the user as a popup
     * @param instance The instance whose image should be displayed
     */
    public void displayImage(DatabaseInstance<?> instance) {
        Dialog dialog = new AlertDialog.Builder(currentActivity)
                .setView(R.layout.popup_image)
                .create();
        dialog.show();
        ImageView v = dialog.findViewById(R.id.popup_img);
        Image.getFormatedAssociatedImage(instance, Image.Options.Square(1000)).into(v);
    }

    /**
     * To be called by the Signup activity upon submission
     * @return all invalid properties
     */
    public Set<Integer> signupUser(String name, String email, String phone, String bio, Boolean admin, Boolean org, Uri image, Consumer<Boolean> onComplete){
        return User.NewInstance(db, deviceID, name, bio, image, "", email, phone, org, admin, false, (instance, success) -> {
            if(success){
                this.user = instance;
            }
            onComplete.accept(success);
        });
    }

    /**
     * Opens the new activity and closes the old activity
     * @param to The new activity
     */
    public void switchToActivity(Class<? extends Activity> to){
        Intent i = new Intent(currentActivity, to);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        currentActivity.finish();
    }

    /**
     * Formats a timestamp
     * @param t the time
     * @return The timestamp as "dd/MM/yyyy"
     */
    public String formatTimestamp(Timestamp t) {
        DateFormat format = new SimpleDateFormat(getString(R.string.date_format_basic));
        return format.format(t.toDate());
    }

    /**
     * Called on errors in the database
     */
    @Override
    public void accept(RuntimeException e) {
        //TODO
    }

}
