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

import com.syzygy.events.databinding.ActivityEntrantBinding;

public class EntrantActivity extends AppCompatActivity {

    private ActivityEntrantBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityEntrantBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_entrant_profile, R.id.nav_entrant_events, R.id.nav_entrant_qr, R.id.nav_entrant_notifications)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_entrant);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        navView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
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
            PopupMenu m = new PopupMenu(EntrantActivity.this, findViewById(item.getItemId()));
            m.getMenuInflater().inflate(R.menu.account_menu, m.getMenu());
            m.setForceShowIcon(true);
            m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    Intent intent = new Intent(EntrantActivity.this, EntrantActivity.class);
                    startActivity(intent);
                    return true;
                }
            });
            m.show();
        }
        return true;
    }


}