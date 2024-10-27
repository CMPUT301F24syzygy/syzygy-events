package com.syzygy.events.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.User;
import com.syzygy.events.databinding.FragmentSignupBinding;

import java.util.Set;

public class SignupActivity extends SyzygyApplication.SyzygyActivity {

    private FragmentSignupBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        binding = FragmentSignupBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        binding.signupButtonSubmit.setOnClickListener(view -> submitData());

    }

    private void submitData(){
        String name = binding.signupName.getText().toString();
        String phone = binding.signupPhone.getText().toString();
        String email = binding.signupEmail.getText().toString();
        String bio = binding.signupBio.getText().toString();
        Boolean admin = binding.signupAdminNotifications.isChecked();
        Boolean org = binding.signupOrgNotifications.isChecked();
        Set<Integer> invalidIds = User.validateDataMap(User.createDataMap(name, bio, "", "", email, phone, org, admin, false, Timestamp.now()));
        if(invalidIds.isEmpty()){
            binding.progressBar.setVisibility(View.VISIBLE);
            ((SyzygyApplication)getApplication()).signupUser(name, email, phone, bio, admin, org, success -> {
                if(success){
                    return;
                };
                Toast.makeText(this, "An error occurred", Toast.LENGTH_LONG).show();
                binding.progressBar.setVisibility(View.GONE);
            });
            return;
        }
        if(invalidIds.contains(R.string.database_user_name)){
            binding.signupName.setError("Bad");
        }
        if(invalidIds.contains(R.string.database_user_phoneNumber)){
            binding.signupPhone.setError("Bad");
        }
        if(invalidIds.contains(R.string.database_user_email)){
            binding.signupEmail.setError("Bad");
        }
        if(invalidIds.contains(R.string.database_user_description)){
            binding.signupBio.setError("Bad");
        }
        Toast.makeText(this, "Invalid", Toast.LENGTH_SHORT).show();
    }

    public void openEntrant(View v) {
        Intent intent = new Intent(SignupActivity.this, EntrantActivity.class);
        startActivity(intent);
    }


}
