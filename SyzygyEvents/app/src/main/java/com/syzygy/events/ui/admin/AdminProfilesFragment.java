package com.syzygy.events.ui.admin;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.DatabaseInfLoadQuery;
import com.syzygy.events.database.DatabaseInstance;
import com.syzygy.events.database.DatabaseQuery;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.Image;
import com.syzygy.events.database.Notification;
import com.syzygy.events.database.User;
import com.syzygy.events.databinding.FragAdminProfilesListBinding;
import com.syzygy.events.ui.EntrantActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The profiles tab for admin
 * <p>
 * Map
 * <pre>
 * 1. Admin Activity -> Browse Users
 * 2. Admin Activity
 * </pre>
 */
public class AdminProfilesFragment extends Fragment {
    private FragAdminProfilesListBinding binding;
    /**
     * The query to get all profiles
     */
    private DatabaseInfLoadQuery<User> query;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragAdminProfilesListBinding.inflate(inflater, container, false);

        SyzygyApplication app = (SyzygyApplication)getActivity().getApplication();
        query = new DatabaseInfLoadQuery<>(DatabaseQuery.getUsers(app.getDatabase()));

        List<User> dataList = query.getInstances();

        AdminProfilesAdapter a = new AdminProfilesAdapter(this.getContext(), dataList);

        query.refreshData((query1, success) -> {
            a.notifyDataSetChanged();
        });

        binding.adminProfilesList.setAdapter(a);

        binding.adminProfilesList.setOnItemClickListener((parent, view, position, id) -> {
            User user = a.getItem(position);
            Dialog dialog = new AlertDialog.Builder(getContext())
                    .setView(R.layout.popup_admin_user_view)
                    .create();
            dialog.show();

            TextView name = dialog.findViewById(R.id.admin_view_entrant_name);
            name.setText(user.getName());
            TextView phone = dialog.findViewById(R.id.admin_view_phone);
            phone.setText(user.getPhoneNumber());
            TextView email = dialog.findViewById(R.id.admin_view_email);
            email.setText(user.getEmail());
            TextView bio = dialog.findViewById(R.id.admin_view_entrant_bio);
            bio.setText(user.getDescription());

            TextView phonelabel = dialog.findViewById(R.id.admin_view_phone_label);
            phonelabel.setVisibility(user.getPhoneNumber().isEmpty() ? View.GONE : View.VISIBLE);

            ImageView image = dialog.findViewById(R.id.admin_view_entrant_image);
            Image.getFormatedAssociatedImage(user, Image.Options.Circle(Image.Options.Sizes.MEDIUM)).into(image);

            if (user.getFacility() != null) {
                View card = dialog.findViewById(R.id.entrant_facility_card);
                card.setVisibility(View.VISIBLE);
                TextView facility_name = dialog.findViewById(R.id.card_facility_name_text);
                facility_name.setText(user.getFacility().getName());
                TextView facility_address = dialog.findViewById(R.id.card_facility_location_text);
                facility_address.setText(user.getFacility().getAddress());
                ImageView facility_image = dialog.findViewById(R.id.card_facility_image_img);
                Image.getFormatedAssociatedImage(user.getFacility(), Image.Options.Square(Image.Options.Sizes.MEDIUM)).into(facility_image);
            }

            dialog.findViewById(R.id.admin_view_delete_user_button).setOnClickListener(v -> {
                dialog.dismiss();
                user.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s -> {
                    query.refreshData((query1, success) -> {
                        a.notifyDataSetChanged();
                    });
                    binding.adminProfilesList.setAdapter(a);
                });
            });

        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        query.dissolve();
        binding = null;
    }

}