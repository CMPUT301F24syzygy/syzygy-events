package com.syzygy.events.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.databinding.FragmentSignupBinding;

public class SignupActivity extends SyzygyApplication.SyzygyActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        FragmentSignupBinding binding = FragmentSignupBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

    }

    public void openEntrant(View v) {
        Intent intent = new Intent(SignupActivity.this, EntrantActivity.class);
        startActivity(intent);
    }


}
