package com.syzygy.events;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.syzygy.events.database.Database;
import com.syzygy.events.databinding.ActivityEntrantBinding;
import com.syzygy.events.databinding.ActivityOrganizerBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityEntrantBinding entrantBinding;
    private ActivityOrganizerBinding organizerBinding;
    private NavController navController;
    private Database database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        database = new Database(getResources());

        entrantBinding = ActivityEntrantBinding.inflate(getLayoutInflater());
        organizerBinding = ActivityOrganizerBinding.inflate(getLayoutInflater());

        setContentView(entrantBinding.getRoot());

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_entrant_profile, R.id.nav_entrant_events, R.id.nav_entrant_qr, R.id.nav_entrant_notifications)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_entrant);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(entrantBinding.navView, navController);

        entrantBinding.navView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            navController.navigateUp();
        }
        else {
            PopupMenu m = new PopupMenu(MainActivity.this, findViewById(item.getItemId()));
            m.getMenuInflater().inflate(R.menu.account_menu, m.getMenu());
            m.setForceShowIcon(true);
            m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    if (menuItem.getItemId() == R.id.entrant_item) {
                        switchToEntrant();
                    }
                    else if (menuItem.getItemId() == R.id.organizer_item) {
                        switchToOrganizer();
                    }
                    return true;
                }
            });
            m.show();
        }
        return true;
    }

    public void switchToEntrant() {
        setContentView(entrantBinding.getRoot());
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_entrant_profile, R.id.nav_entrant_events, R.id.nav_entrant_qr, R.id.nav_entrant_notifications)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_entrant);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(entrantBinding.navView, navController);

    }

    public void switchToOrganizer() {
        setContentView(organizerBinding.getRoot());
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_organizer_profile, R.id.nav_organizer_events, R.id.nav_organizer_create)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_organizer);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(organizerBinding.organizerNavView, navController);

    }


}