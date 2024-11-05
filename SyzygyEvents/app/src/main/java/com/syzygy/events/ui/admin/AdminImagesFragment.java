package com.syzygy.events.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.DatabaseInfLoadQuery;
import com.syzygy.events.database.DatabaseQuery;
import com.syzygy.events.database.Image;
import com.syzygy.events.databinding.FragAdminImagesListBinding;

public class AdminImagesFragment extends Fragment {
    private com.syzygy.events.databinding.FragAdminImagesListBinding binding;
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

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}