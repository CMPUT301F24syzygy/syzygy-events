package com.syzygy.events.ui;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.squareup.picasso.Picasso;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.Image;
import com.syzygy.events.database.User;
import com.syzygy.events.databinding.FragmentSignupBinding;

import java.util.Set;

public class SignupActivity extends SyzygyApplication.SyzygyActivity {

    private FragmentSignupBinding binding;
    private Uri image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        binding = FragmentSignupBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        binding.signupButtonSubmit.setOnClickListener(view -> submitData());
        binding.signupEditImage.setOnClickListener(view -> choosePhoto());
        binding.signupRemoveImage.setOnClickListener(view -> setImage(null));

        setImage(null);
    }

    private void choosePhoto(){
        ((SyzygyApplication)getApplication()).getImage(uri -> {
            if(uri == null){
                Toast.makeText(this, "Failed to get image", Toast.LENGTH_LONG).show();
                return;
            }
            setImage(uri);
        });
    }

    private void setImage(Uri uri){
        image = uri;
        if(image == null){
            Image.formatDefaultImage(Database.Collections.USERS, Image.Options.Circle(256)).into(binding.signupProfile);
            binding.signupRemoveImage.setVisibility(View.INVISIBLE);
        }else{
            Image.formatImage(Picasso.get().load(uri), Image.Options.Circle(256)).into(binding.signupProfile);;
            binding.signupRemoveImage.setVisibility(View.VISIBLE);
        }
    }

    private void submitData(){
        String name = binding.signupName.getText().toString();
        String phone = binding.signupPhone.getText().toString();
        String email = binding.signupEmail.getText().toString();
        String bio = binding.signupBio.getText().toString();
        Boolean admin = binding.signupAdminNotifications.isChecked();
        Boolean org = binding.signupOrgNotifications.isChecked();


        SyzygyApplication app = (SyzygyApplication) getApplication();

        binding.progressBar.setVisibility(View.VISIBLE);
        Set<Integer> invalidIds = app.signupUser(name, email, phone, bio, admin, org, image, success -> {
            if(success){
                app.switchToActivity(EntrantActivity.class);
            }else{
                Toast.makeText(this, "An error occurred: Image", Toast.LENGTH_LONG).show();
                binding.progressBar.setVisibility(View.GONE);
            }
        });
        if(invalidIds.isEmpty()) return;

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
