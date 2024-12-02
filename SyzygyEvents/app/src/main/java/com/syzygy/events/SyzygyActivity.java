package com.syzygy.events;

import static android.app.PendingIntent.getActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
import com.syzygy.events.database.User;
import com.syzygy.events.ui.AdminActivity;
import com.syzygy.events.ui.EntrantActivity;
import com.syzygy.events.ui.InitActivity;
import com.syzygy.events.ui.OrganizerActivity;
import com.syzygy.events.ui.SignupActivity;

/**
 * An abstract class for all activities related to syzugy-events.
 * Contains helper methods and functions
 */
public abstract class SyzygyActivity extends AppCompatActivity implements Database.UpdateListener{

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
        SyzygyApplication app = (SyzygyApplication) getApplication();
        app.registerActivity(this);
    }

    @Override
    public <T extends DatabaseInstance<T>> void onUpdate(DatabaseInstance<T> instance, Type type) {
        SyzygyApplication app = (SyzygyApplication) getApplication();
        User user = (User) instance;
        if (!user.isLegalState()) {
            Dialog dialog = new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle("Notice")
                    .setMessage("This account has been removed and can no longer be accessed.")
                    .setPositiveButton("Ok", null)
                    .create();
            app.clearUser();
            dialog.setOnDismissListener(d -> {
                app.switchToActivity(SignupActivity.class);
            });
            dialog.show();
        }
        else if (user.getFacility() == null && this.getClass() == OrganizerActivity.class) {
            Dialog dialog = new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle("Notice")
                    .setMessage("This facility has been removed and can no longer be accessed. Upon closing this dialog you will be taken to your user account.")
                    .setPositiveButton("Ok", null)
                    .create();
            dialog.setOnDismissListener(d -> {
                app.switchToActivity(EntrantActivity.class);
            });
            dialog.show();
        }
        else if (!user.isAdmin() && this.getClass() == AdminActivity.class) {
            Dialog dialog = new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle("Notice")
                    .setMessage("You no longer have admin privileges. Upon closing this dialog you will be taken to your user account.")
                    .setPositiveButton("Ok", null)
                    .create();
            dialog.setOnDismissListener(d -> {
                app.switchToActivity(EntrantActivity.class);
            });
            dialog.show();
        }
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
        Log.println(Log.DEBUG, "NAV", "navigateUp(reg)");
    }

    /**
     * Navigates back to the previous fragment and displays the error as a popup
     * @param error The error to display
     */
    public void navigateUp(String error) {
        navigateUp();
        Log.println(Log.DEBUG, "NAV", "navigateUp(error)");
        showErrorDialog(error);
    }

    public void showErrorDialog(String error) {
        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle("Oops!")
                .setMessage(error)
                .setPositiveButton("dismiss", null)
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
