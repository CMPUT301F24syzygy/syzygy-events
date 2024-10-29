package com.syzygy.events.ui.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.DatabaseInfLoadQuery;
import com.syzygy.events.database.DatabaseQuery;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.EventAssociation;
import com.syzygy.events.databinding.FragmentOrganizerEventsBinding;
import com.syzygy.events.ui.EntrantActivity;
import com.syzygy.events.ui.OrganizerActivity;
import com.syzygy.events.ui.entrant.EntrantEventsAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OrganizerEventsFragment extends Fragment {
    private FragmentOrganizerEventsBinding binding;
    private DatabaseInfLoadQuery<Event> query;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOrganizerEventsBinding.inflate(inflater, container, false);

        SyzygyApplication app = (SyzygyApplication)getActivity().getApplication();
        query = new DatabaseInfLoadQuery<>(DatabaseQuery.getFacilityEvents(app.getDatabase(), app.getUser().getFacility()));

        List<Event> dataList = query.getInstances();

        OrganizerEventsAdapter a = new OrganizerEventsAdapter(this.getContext(), dataList);

        query.refreshData((query1, success) -> {
            a.notifyDataSetChanged();
        });

        binding.organizerEventsList.setAdapter(a);

        binding.organizerEventsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                OrganizerActivity activity = (OrganizerActivity)getActivity();
                activity.openEvent(a.getItem(position).getIdentifier());
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