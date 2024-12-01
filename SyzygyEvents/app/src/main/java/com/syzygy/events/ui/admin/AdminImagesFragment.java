package com.syzygy.events.ui.admin;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.syzygy.events.database.Image;
import com.syzygy.events.database.User;
import com.syzygy.events.databinding.FragAdminImagesListBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The images tab for admin
 * <p>
 * Map
 * <pre>
 * 1. Admin Activity -> Browse Images
 * </pre>
 */
public class AdminImagesFragment extends Fragment {
    private com.syzygy.events.databinding.FragAdminImagesListBinding binding;
    /**
     * The query to get all images
     */
    private DatabaseInfLoadQuery<Image> query;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragAdminImagesListBinding.inflate(inflater, container, false);

        SyzygyApplication app = (SyzygyApplication)getActivity().getApplication();
        query = new DatabaseInfLoadQuery<>(DatabaseQuery.getImages(app.getDatabase()));
        AdminImagesAdapter a = new AdminImagesAdapter(this.getContext(), query.getInstances());

        query.refreshData((query1, success) -> {
            a.notifyDataSetChanged();
        });
        binding.adminImagesList.setAdapter(a);

        binding.adminImagesList.setOnItemClickListener((parent, view, position, id) -> {
            Image img = a.getItem(position);
            Dialog dialog = new AlertDialog.Builder(getContext())
                    .setView(R.layout.popup_admin_image_view)
                    .create();
            dialog.show();

            ImageView imageview = dialog.findViewById(R.id.admin_view_image_img);
            Image.getFormatedAssociatedImage(img, Image.Options.Square(Image.Options.Sizes.MEDIUM)).into(imageview);

            dialog.findViewById(R.id.admin_view_delete_image_button).setOnClickListener(v -> {
                Dialog confirmRemoveDialog = new AlertDialog.Builder(getContext())
                        .setTitle("Confirm")
                        .setMessage("Are you sure you want to remove this image? This action cannot be undone.")
                        .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                img.deleteInstance(DatabaseInstance.DeletionType.HARD_DELETE, s -> {
                                    dialog.dismiss();
                                    query.refreshData((query1, success) -> {
                                        a.notifyDataSetChanged();
                                        binding.adminImagesList.setAdapter(a);
                                    });
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
        query.dissolve();
        binding = null;
    }

}