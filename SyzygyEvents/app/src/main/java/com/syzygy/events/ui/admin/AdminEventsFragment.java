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
import com.syzygy.events.database.Notification;
import com.syzygy.events.databinding.FragmentAdminEventsBinding;
import com.syzygy.events.ui.entrant.EntrantNotificationsAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminEventsFragment extends Fragment {
    private com.syzygy.events.databinding.FragmentAdminEventsBinding binding;
    private DatabaseInfLoadQuery<Event> query;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAdminEventsBinding.inflate(inflater, container, false);

        SyzygyApplication app = (SyzygyApplication)getActivity().getApplication();
        query = new DatabaseInfLoadQuery<>(DatabaseQuery.getEvents(app.getDatabase()));

        List<Event> dataList = query.getInstances();

        AdminEventsAdapter a = new AdminEventsAdapter(this.getContext(), dataList);

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
    }

}