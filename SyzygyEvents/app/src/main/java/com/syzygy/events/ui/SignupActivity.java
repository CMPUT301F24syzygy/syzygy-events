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
            Image.formatDefaultImage(Database.Collections.USERS, Image.Options.BIG_AVATAR).into(binding.signupProfile);
            binding.signupRemoveImage.setVisibility(View.INVISIBLE);
        }else{
            Image.formatImage(Picasso.get().load(uri), Image.Options.BIG_AVATAR).into(binding.signupProfile);;
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
        Set<Integer> invalidIds = User.validateDataMap(User.createDataMap(name, bio, "", "", email, phone, org, admin, false, Timestamp.now()));
        if(invalidIds.isEmpty()){
            Log.println(Log.DEBUG, "signup", "valid");
            binding.progressBar.setVisibility(View.VISIBLE);
            SyzygyApplication app = (SyzygyApplication) getApplication();
            if(image != null){
                Log.println(Log.DEBUG, "signup", "image");
                Image.NewInstance(app.getDatabase(), name, Database.Collections.USERS, "testingLocID", image, (instance, success) -> {
                    if(!success){
                        Log.println(Log.DEBUG, "signup", "image fail");
                        Toast.makeText(this, "An error occurred: Image", Toast.LENGTH_LONG).show();
                        binding.progressBar.setVisibility(View.GONE);
                        return;
                    }
                    Log.println(Log.DEBUG, "signup", "image good");
                    app.signupUser(name, email, phone, bio, admin, org, instance, success2 -> {

                        instance.dissolve();
                        if(success2){
                            Log.println(Log.DEBUG, "signup", "user good");
                            return;
                        };
                        Log.println(Log.DEBUG, "signup", "user fail");
                        Toast.makeText(this, "An error occurred", Toast.LENGTH_LONG).show();
                        binding.progressBar.setVisibility(View.GONE);
                    });
                });
                return;
            }
            Log.println(Log.DEBUG, "signup", "no image");
            app.signupUser(name, email, phone, bio, admin, org, null, success -> {
                if(success){
                    Log.println(Log.DEBUG, "signup", "user good");
                    return;
                };
                Log.println(Log.DEBUG, "signup", "user fail");
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
