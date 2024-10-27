package com.syzygy.events.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;


import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.databinding.FragmentLoginBinding;
import com.syzygy.events.databinding.FragmentSignupBinding;

public class InitActivity extends SyzygyApplication.SyzygyActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        FragmentLoginBinding binding = FragmentLoginBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

    }

    public void openEntrant(View v) {
        Intent intent = new Intent(InitActivity.this, EntrantActivity.class);
        startActivity(intent);
    }


}
