package com.syzygy.events.ui.entrant;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInfLoadQuery;
import com.syzygy.events.database.DatabaseQuery;
import com.syzygy.events.database.Notification;
import com.syzygy.events.databinding.FragmentEntrantNotificationsBinding;
import com.syzygy.events.ui.EntrantActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EntrantNotificationsFragment extends Fragment {
    private FragmentEntrantNotificationsBinding binding;

    private DatabaseInfLoadQuery<Notification> query;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEntrantNotificationsBinding.inflate(inflater, container, false);

        SyzygyApplication app = (SyzygyApplication)getActivity().getApplication();
        query = new DatabaseInfLoadQuery<>(DatabaseQuery.getMyNotifications(app.getDatabase(), app.getUser()));

        List<Notification> dataList = query.getInstances();

        EntrantNotificationsAdapter a = new EntrantNotificationsAdapter(this.getContext(), dataList);

        query.refreshData((query1, success) -> {
            a.notifyDataSetChanged();
            Log.println(Log.DEBUG, "success", success + "");
            Log.println(Log.DEBUG, "qSize", query.getInstances().size() + "");
            Log.println(Log.DEBUG, "Size", dataList.size() + "");
        });

        binding.entrantNotificationsList.setAdapter(a);

        binding.entrantNotificationsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                EntrantActivity activity = (EntrantActivity)getActivity();
                ///
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}