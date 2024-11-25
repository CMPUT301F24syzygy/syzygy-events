package com.syzygy.events.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyActivity;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.Image;
import com.syzygy.events.databinding.FragmentSignupBinding;

import java.util.Set;

/**
 * The activity that is used on startup when the device does not have an account
 * Gets the user to create an account
 */
public class SignupActivity extends SyzygyActivity {

    /**
     * The binding to the ui
     */
    private FragmentSignupBinding binding;
    /**
     * The current selected profile image
     */
    private Uri image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        binding = FragmentSignupBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        binding.signupSubmitButton.setOnClickListener(view -> submitData());
        binding.signupEditImageButton.setOnClickListener(view -> choosePhoto());
        binding.signupRemoveImageButton.setOnClickListener(view -> setImage(null));

        setImage(null);
    }

    /**
     * Gets an image from the user
     */
    private void choosePhoto(){
        ((SyzygyApplication)getApplication()).getImage(uri -> {
            if(uri == null){
                Toast.makeText(this, "Failed to get image", Toast.LENGTH_LONG).show();
                return;
            }
            setImage(uri);
        });
    }

    /**
     * Sets the image view to the given image
     * If null, removes the image that is currently displayed
     * @param uri The image that was selected
     */
    private void setImage(Uri uri){
        image = uri;
        if(image == null){
            Image.formatDefaultImage(Database.Collections.USERS, Image.Options.Circle(Image.Options.Sizes.MEDIUM)).into(binding.signupImageImg);
            binding.signupRemoveImageButton.setVisibility(View.INVISIBLE);
        }else{
            Image.formatImage(Picasso.get().load(uri), Image.Options.Circle(Image.Options.Sizes.MEDIUM)).into(binding.signupImageImg);;
            binding.signupRemoveImageButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Validates the information. If valid, creates the account and switches to the next activity
     */
    private void submitData(){
        String name = binding.signupName.getText().toString().replaceAll("\\s+", " ");
        String phone = binding.signupPhone.getText().toString();
        String email = binding.signupEmail.getText().toString();
        String bio = binding.signupBio.getText().toString().replaceAll("\\s+", " ");
        Boolean admin = binding.signupAdminNotificationsCheckbox.isChecked();
        Boolean org = binding.signupOrgNotificationsCheckbox.isChecked();


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
        binding.progressBar.setVisibility(View.GONE);
        if(invalidIds.contains(R.string.database_user_name)){
            binding.signupName.setError(getString(R.string.val_create_user_name));
        }
        if(invalidIds.contains(R.string.database_user_phoneNumber)){
            binding.signupPhone.setError(getString(R.string.val_create_user_phoneNumber));
        }
        if(invalidIds.contains(R.string.database_user_email)){
            binding.signupEmail.setError(getString(R.string.val_create_user_email));
        }
        if(invalidIds.contains(R.string.database_user_description)){
            binding.signupBio.setError(getString(R.string.val_create_user_description));
        }
        Toast.makeText(this, "Invalid", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
