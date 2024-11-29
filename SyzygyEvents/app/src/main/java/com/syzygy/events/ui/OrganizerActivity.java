package com.syzygy.events.ui;

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
import com.syzygy.events.databinding.ActivityOrganizerBinding;

import java.util.List;

/**
 * The activity that handles all fragments that are visible in the organizer view.
 */
public class OrganizerActivity extends SyzygyActivity {

    private ActivityOrganizerBinding organizerBinding;
    /**
     * The currently selected event
     */
    private String eventID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        organizerBinding = ActivityOrganizerBinding.inflate(getLayoutInflater());

        setContentView(organizerBinding.getRoot());

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_organizer_profile, R.id.nav_organizer_events, R.id.nav_organizer_create)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_organizer);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(organizerBinding.organizerNavView, navController);

        organizerBinding.organizerNavView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                for (; navController.navigateUp(); ) ;
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
            PopupMenu m = new PopupMenu(OrganizerActivity.this, findViewById(item.getItemId()));
            m.getMenuInflater().inflate(R.menu.account_menu, m.getMenu());
            m.setForceShowIcon(true);
            SyzygyApplication app = (SyzygyApplication) getApplication();
            Menu menu = m.getMenu();
            app.loadMenuItemIcons(menu);

            menu.findItem(R.id.add_organizer_item).setVisible(false);
            menu.findItem(R.id.organizer_item).setVisible(true);
            if (app.getUser().isAdmin()) {
                m.getMenu().findItem(R.id.admin_item).setVisible(true);
            }
            m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    if (menuItem.getItemId() == R.id.entrant_item) {
                        app.switchToActivity(EntrantActivity.class);
                    } else if (menuItem.getItemId() == R.id.admin_item) {
                        app.switchToActivity(AdminActivity.class);
                    }
                    return true;
                }
            });
            m.show();
        }
        return true;
    }

    /**
     * @return The id of the currently selected event
     */
    public String getEventID() {
        return eventID;
    }

    /**
     * Navigates to the profile of the event
     * @param id The id of the event to navigate to
     */
    public void openEvent(String id) {
        eventID = id;
        navController.navigate(R.id.nav_organizer_events);
        navController.navigate(R.id.nav_organizer_event_secondary);
    }

    /**
     * Opens the edit profile fragment
     */
    public void openEdit() {
        navController.navigate(R.id.nav_organizer_edit_secondary);
    }

    public void openCreateEvent() {
        navController.navigate(R.id.nav_organizer_create);
    }
}