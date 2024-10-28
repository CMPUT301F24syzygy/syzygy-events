package com.syzygy.events.ui.entrant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInfLoadQuery;
import com.syzygy.events.database.DatabaseQuery;
import com.syzygy.events.database.EventAssociation;

import com.syzygy.events.databinding.FragmentEntrantEventsBinding;
import com.syzygy.events.ui.EntrantActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EntrantEventsFragment extends Fragment {
    private FragmentEntrantEventsBinding binding;
    private DatabaseInfLoadQuery<EventAssociation> query;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEntrantEventsBinding.inflate(inflater, container, false);

        SyzygyApplication app = (SyzygyApplication)getActivity().getApplication();
        query = new DatabaseInfLoadQuery<>(DatabaseQuery.getMyEventsFilter(app.getDatabase(), app.getUser()));

        List<EventAssociation> dataList = query.getInstances();

        EntrantEventsAdapter a = new EntrantEventsAdapter(this.getContext(), dataList);

        query.refreshData((query1, success) -> {
            a.notifyDataSetChanged();
            Log.println(Log.DEBUG, "success", success + "");
            Log.println(Log.DEBUG, "qSize", query.getInstances().size() + "");
            Log.println(Log.DEBUG, "Size", dataList.size() + "");
        });

        binding.entrantEventsList.setAdapter(a);

        binding.entrantEventsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                EntrantActivity activity = (EntrantActivity)getActivity();
                activity.openEvent(a.getItem(position).getEvent());
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        query.dissolve();
    }

}