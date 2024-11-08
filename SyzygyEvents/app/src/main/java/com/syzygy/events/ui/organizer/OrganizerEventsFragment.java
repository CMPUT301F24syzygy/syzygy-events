package com.syzygy.events.ui.organizer;

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
import com.syzygy.events.databinding.FragOrgEventsListBinding;
import com.syzygy.events.ui.OrganizerActivity;

/**
 * The fragment displays all events associated with the facility. The Events tab
 * <p>
 * Map
 * <pre>
 * 1. Organizer Activity -> Events
 * </pre>
 */
public class OrganizerEventsFragment extends Fragment {
    private FragOrgEventsListBinding binding;
    /**
     * The query to get all events
     */
    private DatabaseInfLoadQuery<Event> query;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragOrgEventsListBinding.inflate(inflater, container, false);

        SyzygyApplication app = (SyzygyApplication)getActivity().getApplication();
        query = new DatabaseInfLoadQuery<>(DatabaseQuery.getFacilityEvents(app.getDatabase(), app.getUser().getFacility()));

        OrganizerEventsAdapter a = new OrganizerEventsAdapter(this.getContext(), query.getInstances());

        query.refreshData((query1, success) -> {
            a.notifyDataSetChanged();
        });

        binding.organizerEventsList.setAdapter(a);

        binding.organizerEventsList.setOnItemClickListener((parent, view, position, id) -> {
            OrganizerActivity activity = (OrganizerActivity)getActivity();
            activity.openEvent(a.getItem(position).getDocumentID());
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