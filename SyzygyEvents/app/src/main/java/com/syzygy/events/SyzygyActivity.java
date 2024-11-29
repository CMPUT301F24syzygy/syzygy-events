package com.syzygy.events;

import static android.app.PendingIntent.getActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;

import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInstance;

/**
 * An abstract class for all activities related to syzugy-events.
 * Contains helper methods and functions
 */
public abstract class SyzygyActivity extends AppCompatActivity {

    /**
     * The navigation controller to switch between fragments
     */
    protected NavController navController;

    /**
     * An activity that gets an image selected by the users
     */
    ActivityResultLauncher<String> getMediaContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
            uri -> ((SyzygyApplication) getApplication()).sendImage(uri));

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((SyzygyApplication) getApplication()).registerActivity(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_nav_menu, menu);
        ((SyzygyApplication)getApplication()).loadMenuIcon(menu);
        return true;
    }

    /**
     * Navigates back to the previous fragment
     */
    public void navigateUp() {
        navController.navigateUp();
    }

    /**
     * Navigates back to the previous fragment and displays the error as a popup
     * @param error The error to display
     */
    public void navigateUp(String error) {
        navigateUp();
        Dialog dialog = new AlertDialog.Builder(this)
                .setMessage(error)
                .create();
        dialog.show();
    }

    /**
     * Called when the application asks for permission to access location. Passes the result back to the application
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     *                     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.println(Log.DEBUG, "permission", "received");
        if (requestCode == SyzygyApplication.LOCATION_REQUEST_CODE) {
            Log.println(Log.DEBUG, "permission", "location");
            ((SyzygyApplication) getApplication()).pingLocation(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        }
    }
}
