package com.syzygy.events;

import android.content.Intent;
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
import com.syzygy.events.databinding.ActivityOrganizerBinding;

public class OrganizerActivity extends AppCompatActivity {
    private ActivityOrganizerBinding binding;
    private NavController navController;
    private Database database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        binding = ActivityOrganizerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = binding.organizerNavView;

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_organizer_profile, R.id.nav_organizer_events, R.id.nav_organizer_create)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_organizer);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.organizerNavView, navController);

        navView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                for (;navController.navigateUp(); ) ;
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
}