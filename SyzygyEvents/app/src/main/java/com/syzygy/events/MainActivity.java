package com.syzygy.events;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.syzygy.events.database.Database;
import com.syzygy.events.databinding.ActivityEntrantBinding;
import com.syzygy.events.databinding.FragmentSignupBinding;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Database database = new Database(getResources());

        FragmentSignupBinding binding = FragmentSignupBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

    }

    public void openEntrant(View v) {
        Intent intent = new Intent(MainActivity.this, EntrantActivity.class);
        startActivity(intent);
    }


}
