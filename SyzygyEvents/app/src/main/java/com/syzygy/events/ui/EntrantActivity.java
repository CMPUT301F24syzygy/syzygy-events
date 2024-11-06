package com.syzygy.events.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;

import com.syzygy.events.database.Image;
import com.syzygy.events.databinding.ActivityEntrantBinding;

public class EntrantActivity extends SyzygyApplication.SyzygyActivity {
    private ActivityEntrantBinding entrantBinding;
    private NavController navController;
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

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_nav_menu, menu);
        ((SyzygyApplication)getApplication()).loadMenuIcon(menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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

    public String getEventID() {
        return selectedEventID;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (intentResult != null && intentResult.getContents() != null) {
            openEvent(intentResult.getContents());
        }
    }
    public void openEditProfile() {
        navController.navigate(R.id.nav_entrant_edit_secondary);
    }

    public void openEvent(String id) {
        selectedEventID = id;
        navController.navigate(R.id.nav_entrant_event_secondary);
    }

    public void openFacility() {
        navController.navigate(R.id.nav_entrant_facility_secondary);
    }

    public void navigateUp() {
        navController.navigateUp();
    }

    public void eventError() {
        navController.navigateUp();
        Dialog dialog = new AlertDialog.Builder(this)
                .setMessage("Event does not exist.")
                .create();
        dialog.show();
    }



}