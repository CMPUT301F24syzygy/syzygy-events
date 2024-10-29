package com.syzygy.events;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.Image;
import com.syzygy.events.database.User;
import com.syzygy.events.ui.EntrantActivity;
import com.syzygy.events.ui.SignupActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class SyzygyApplication extends Application implements Consumer<RuntimeException> {

    private static final int LOCATION_REQUEST_CODE = 359;

    private Database db;
    private User user;
    private String deviceID;
    private SyzygyActivity currentActivity;
    private FusedLocationProviderClient location;

    private final List<Consumer<Location>> locationListeners = new ArrayList<>();
    private final List<Consumer<Uri>> imageListeners = new ArrayList<>();


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
        location = LocationServices.getFusedLocationProviderClient(this);

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

    void sendImage(Uri image){
        Log.println(Log.DEBUG, "image", "got");
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
     * To be called by the Signup activity upon submission
     */
    public void signupUser(String name, String email, String phone, String bio, Boolean admin, Boolean org, Image image, Consumer<Boolean> listener){
        User.NewInstance(db, deviceID, name, bio, image == null ? "" : image.getDocumentID(), "", email, phone, org, admin, false, (instance, success) -> {
            if(success){
                this.user = instance;
                if(image!=null)image.setLocID(user.getDocumentID());
                listener.accept(true);
                switchToActivity(EntrantActivity.class);
            }else{
                if(image!=null)image.deleteInstance(s -> {});
                listener.accept(false);
            }
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

    public void stringToLocation(String location) throws IOException {
        Geocoder geo = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = geo.getFromLocationName(location, 3);
    }

    /**
     * Called on errors in the database
     */
    @Override
    public void accept(RuntimeException e) {

    }

    public static abstract class SyzygyActivity extends AppCompatActivity {

        ActivityResultLauncher<String> getMediaContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> ((SyzygyApplication)getApplication()).sendImage(uri));

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            ((SyzygyApplication)getApplication()).registerActivity(this);
        }

        /**
         * Called when the application asks for permission to access location. Passes the result back to the application
         * @param requestCode The request code passed in {@link #requestPermissions(String[], int)}
         * @param permissions The requested permissions. Never null.
         * @param grantResults The grant results for the corresponding permissions
         *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
         *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
         *
         */
        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            Log.println(Log.DEBUG, "permission", "received");
            if(requestCode == LOCATION_REQUEST_CODE){
                Log.println(Log.DEBUG, "permission", "location");
                ((SyzygyApplication) getApplication()).pingLocation(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
            }
        }
    }
}
