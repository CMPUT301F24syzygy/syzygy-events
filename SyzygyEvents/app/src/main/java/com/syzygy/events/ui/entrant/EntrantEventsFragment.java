package com.syzygy.events.ui.entrant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.DatabaseInfLoadQuery;
import com.syzygy.events.database.DatabaseQuery;
import com.syzygy.events.database.EventAssociation;
import com.syzygy.events.databinding.FragEntrantEventsListBinding;
import com.syzygy.events.ui.EntrantActivity;

public class EntrantEventsFragment extends Fragment {
    private FragEntrantEventsListBinding binding;
    private DatabaseInfLoadQuery<EventAssociation> query;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragEntrantEventsListBinding.inflate(inflater, container, false);

        SyzygyApplication app = (SyzygyApplication)getActivity().getApplication();
        query = new DatabaseInfLoadQuery<>(DatabaseQuery.getMyEventsFilter(app.getDatabase(), app.getUser()));

        EntrantEventsAdapter a = new EntrantEventsAdapter(this.getContext(), query.getInstances());

        query.refreshData((query1, success) -> {
            a.notifyDataSetChanged();
        });

        binding.entrantEventsList.setAdapter(a);

        binding.entrantEventsList.setOnItemClickListener((parent, view, position, id) -> {
            EntrantActivity activity = (EntrantActivity)getActivity();
            activity.openEvent(a.getItem(position).getEventID());
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