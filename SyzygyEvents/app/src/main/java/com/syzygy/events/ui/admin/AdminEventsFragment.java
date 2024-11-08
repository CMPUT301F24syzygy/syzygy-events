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
import com.syzygy.events.database.Event;
import com.syzygy.events.databinding.FragAdminEventsListBinding;

/**
 * The events tab for admin
 * <p>
 * Map
 * <pre>
 * 1. Admin Activity -> Browse Events
 * </pre>
 */
public class AdminEventsFragment extends Fragment {
    private FragAdminEventsListBinding binding;
    /**
     * The query to get all events
     */
    private DatabaseInfLoadQuery<Event> query;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragAdminEventsListBinding.inflate(inflater, container, false);

        SyzygyApplication app = (SyzygyApplication)getActivity().getApplication();
        query = new DatabaseInfLoadQuery<>(DatabaseQuery.getEvents(app.getDatabase()));

        AdminEventsAdapter a = new AdminEventsAdapter(this.getContext(), query.getInstances());

        query.refreshData((query1, success) -> {
            a.notifyDataSetChanged();
        });

        binding.adminEventsList.setAdapter(a);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        query.dissolve();
    }

}