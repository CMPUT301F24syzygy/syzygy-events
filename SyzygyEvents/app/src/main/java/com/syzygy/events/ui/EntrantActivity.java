package com.syzygy.events.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyActivity;
import com.syzygy.events.SyzygyApplication;

import com.syzygy.events.database.Database;
import com.syzygy.events.database.Event;
import com.syzygy.events.databinding.ActivityEntrantBinding;

import java.util.List;
import java.util.Objects;

/**
 * The activity that handles all fragments that are visible in the entrant view.
 */
public class EntrantActivity extends SyzygyActivity {

    private ActivityEntrantBinding entrantBinding;

    /**
     * The current selected event if the user is traversing to an event profile.
     * Whenever the user selects an event. The eventID is stored here before traversal
     */
    private String selectedEventID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        entrantBinding = ActivityEntrantBinding.inflate(getLayoutInflater());

        setContentView(entrantBinding.getRoot());

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_entrant_profile, R.id.nav_entrant_events, R.id.nav_entrant_qr, R.id.nav_entrant_notifications)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_entrant);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(entrantBinding.entrantNavView, navController);

        entrantBinding.entrantNavView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                for(;navController.navigateUp(););
                NavigationUI.onNavDestinationSelected(item, navController);
                return true;
            }
        });
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 300);
        }

    }

    //Sets up the menu items
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        List<Fragment> frags = getSupportFragmentManager().getFragments();
        for (int i=0; i<frags.size(); i++) {
            if (frags.get(i).isVisible() && frags.get(i).getView() != null) {
                break;
            } else if (i == frags.size()-1) {
                return true;
            }
        }

        if (item.getItemId() == android.R.id.home) {
            navController.navigateUp();
        }
        else {
            PopupMenu m = new PopupMenu(EntrantActivity.this, findViewById(item.getItemId()));
            m.getMenuInflater().inflate(R.menu.account_menu, m.getMenu());
            m.setForceShowIcon(true);

            Menu menu = m.getMenu();
            SyzygyApplication app = (SyzygyApplication) getApplication();
            app.loadMenuItemIcons(menu);

            if (app.getUser().getFacility() != null) {
                menu.findItem(R.id.add_organizer_item).setVisible(false);
                menu.findItem(R.id.organizer_item).setVisible(true);
            }
            if (app.getUser().isAdmin()) {
                menu.findItem(R.id.admin_item).setVisible(true);
            }
            m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    if (menuItem.getItemId() == R.id.organizer_item) {
                        app.switchToActivity(OrganizerActivity.class);
                    } else if (menuItem.getItemId() == R.id.admin_item) {
                        app.switchToActivity(AdminActivity.class);
                    } else if (menuItem.getItemId() == R.id.add_organizer_item) {
                        navController.navigate(R.id.nav_signup_facility_secondary);
                    }
                    return true;
                }
            });
            m.show();
        }
        return true;
    }

    /**
     * @return The id of the current selected event by the user
     */
    public String getEventID() {
        return selectedEventID;
    }

    //Called when scanning qrs
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (intentResult != null && intentResult.getContents() != null) {
            Database db = ((SyzygyApplication)getApplication()).getDatabase();
            ///dumb
            db.<Event>getInstance(Database.Collections.EVENTS, intentResult.getContents(), (e, s) -> {
                if (s && Objects.equals(e.getQrHash(), intentResult.getContents())) {
                    openEvent(intentResult.getContents());
                }
                else {
                    this.showErrorDialog("The event you are trying to access does not exist.");
                }
            });
        }
    }

    /**
     * Opens the edit profile fragment
     */
    public void openEditProfile() {
        navController.navigate(R.id.nav_entrant_edit_secondary);
    }

    /**
     * Opens the event profile fragment
     * @param id The id of the event
     */
    public void openEvent(String id) {
        selectedEventID = id;
        navController.navigate(R.id.nav_entrant_event_secondary);
    }

    /**
     * Opens the facility profile of the selected event
     */
    public void openFacility() {
        navController.navigate(R.id.nav_entrant_facility_secondary);
    }



}