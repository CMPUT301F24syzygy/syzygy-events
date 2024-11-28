package com.syzygy.events.ui.admin;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInstance;
import com.syzygy.events.database.Image;
import com.syzygy.events.database.User;
import com.syzygy.events.databinding.FragAdminProfilePageBinding;
import com.syzygy.events.ui.AdminActivity;

public class AdminProfileFragment extends Fragment {

    private FragAdminProfilePageBinding binding;
    private User user;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragAdminProfilePageBinding.inflate(inflater, container, false);

        AdminActivity activity = (AdminActivity) getActivity();
        SyzygyApplication app = (SyzygyApplication) getActivity().getApplication();
        app.getDatabase().<User>getInstance(Database.Collections.USERS, activity.getUserID(), (instance, success) -> {
            if (!success) {
                activity.navigateUp("An unexpected error occured");
                return;
            }
            user = instance;
            if(user == null){
                activity.navigateUp("The user was not found");
                return;
            }

            binding.adminViewEntrantName.setText(user.getName());
            binding.adminViewPhone.setText(user.getPhoneNumber());
            binding.adminViewEmail.setText(user.getEmail());
            binding.adminViewEntrantBio.setText(user.getDescription());

            binding.adminViewPhoneLabel.setVisibility(user.getPhoneNumber().isEmpty() ? View.GONE : View.VISIBLE);
            ImageView image = binding.adminViewEntrantImage;
            Image.getFormatedAssociatedImage(user, Image.Options.Circle(Image.Options.Sizes.MEDIUM)).into(image);

            if (user.getFacility() != null) {
                View card = binding.getRoot().findViewById(R.id.entrant_facility_card);
                card.setVisibility(View.VISIBLE);
                TextView facility_name = binding.getRoot().findViewById(R.id.card_facility_name_text);
                facility_name.setText(user.getFacility().getName());
                TextView facility_address = binding.getRoot().findViewById(R.id.card_facility_location_text);
                facility_address.setText(user.getFacility().getAddress());
                ImageView facility_image = binding.getRoot().findViewById(R.id.card_facility_image_img);
                Image.getFormatedAssociatedImage(user.getFacility(), Image.Options.Square(Image.Options.Sizes.MEDIUM)).into(facility_image);

                card.setOnClickListener(v -> {
                    ((AdminActivity)getActivity()).openFacility(user.getFacilityID());
                });

            }

            binding.getRoot().findViewById(R.id.admin_view_delete_user_button).setOnClickListener(v -> {
                Dialog confirmRemoveDialog = new AlertDialog.Builder(getContext())
                        .setTitle("Confirm")
                        .setMessage("Are you sure you want to remove this user?")
                        .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                user.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s -> {
                                    activity.navigateUp();
                                });
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create();
                confirmRemoveDialog.show();
            });


        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(user != null){
            user.dissolve();
        }
        binding = null;
    }
}
