package com.syzygy.events.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;


import com.syzygy.events.SyzygyActivity;
import com.syzygy.events.databinding.FragmentLoginBinding;

/**
 * The activity that the app starts up to.
 * Displays a loading screen while the app fetches the device's account
 */
public class InitActivity extends SyzygyActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        FragmentLoginBinding binding = FragmentLoginBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

    }

    /**
     * Opens the entrant view
     */
    public void openEntrant(View v) {
        Intent intent = new Intent(InitActivity.this, EntrantActivity.class);
        startActivity(intent);
    }


}
