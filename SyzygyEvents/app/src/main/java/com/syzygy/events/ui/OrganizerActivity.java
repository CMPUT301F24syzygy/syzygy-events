package com.syzygy.events.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.qrcode.QRCodeWriter;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.Event;
import com.syzygy.events.databinding.ActivityOrganizerBinding;

public class OrganizerActivity extends SyzygyApplication.SyzygyActivity {

    private ActivityOrganizerBinding organizerBinding;
    private NavController navController;
    private Database database;
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_nav_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            navController.navigateUp();
        } else {
            PopupMenu m = new PopupMenu(OrganizerActivity.this, findViewById(item.getItemId()));
            m.getMenuInflater().inflate(R.menu.account_menu, m.getMenu());
            m.setForceShowIcon(true);
            SyzygyApplication app = (SyzygyApplication)getApplication();
            m.getMenu().findItem(R.id.add_organizer_item).setVisible(false);
            m.getMenu().findItem(R.id.organizer_item).setVisible(true);
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

    public String getEventID() {
        return eventID;
    }

    public void openEvent(String id) {
        eventID = id;
        navController.navigate(R.id.nav_organizer_event_secondary);
    }
}