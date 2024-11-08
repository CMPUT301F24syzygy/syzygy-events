package com.syzygy.events.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.DatabaseInfLoadQuery;
import com.syzygy.events.database.DatabaseQuery;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.User;
import com.syzygy.events.databinding.FragAdminProfilesListBinding;

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

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        query.dissolve();
        binding = null;
    }

}