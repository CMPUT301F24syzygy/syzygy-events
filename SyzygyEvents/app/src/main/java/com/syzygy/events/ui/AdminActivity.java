package com.syzygy.events.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyActivity;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.databinding.ActivityAdminBinding;

import java.util.List;

/**
 * The activity that handles all fragments that are visible in the admin view.
 */
public class AdminActivity extends SyzygyActivity {

    private String selectedEventID;
    private String selectedFacilityID;

    private String selectedUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        com.syzygy.events.databinding.ActivityAdminBinding adminBinding = ActivityAdminBinding.inflate(getLayoutInflater());
        setContentView(adminBinding.getRoot());

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_admin_profiles, R.id.nav_admin_events, R.id.nav_admin_images)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_admin);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(adminBinding.adminNavView, navController);

        adminBinding.adminNavView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                for (; navController.navigateUp(); );
                NavigationUI.onNavDestinationSelected(item, navController);
                return true;
            }
        });

    }

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
        } else {
            PopupMenu m = new PopupMenu(AdminActivity.this, findViewById(item.getItemId()));
            m.getMenuInflater().inflate(R.menu.account_menu, m.getMenu());
            m.setForceShowIcon(true);

            Menu menu = m.getMenu();
            SyzygyApplication app = (SyzygyApplication) getApplication();
            app.loadMenuItemIcons(menu);

            if (app.getUser().getFacility() != null) {
                menu.findItem(R.id.add_organizer_item).setVisible(false);
                menu.findItem(R.id.organizer_item).setVisible(true);
            }
            menu.findItem(R.id.admin_item).setVisible(true);

            m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    if (menuItem.getItemId() == R.id.entrant_item) {
                        app.switchToActivity(EntrantActivity.class);
                    } else if (menuItem.getItemId() == R.id.organizer_item) {
                        app.switchToActivity(OrganizerActivity.class);
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

    public String getFacilityID() {
        return selectedFacilityID;
    }

    public String getUserID() {
        return selectedUserID;
    }

    public void openEvent(String id) {
        selectedEventID = id;
        navController.navigate(R.id.nav_admin_event_secondary);
    }

    public void openFacility(String id) {
        selectedFacilityID = id;
        navController.navigate(R.id.nav_admin_facility_secondary);
    }

    public void openProfile(String id) {
        selectedUserID = id;
        navController.navigate(R.id.nav_admin_profile_secondary);
    }


}