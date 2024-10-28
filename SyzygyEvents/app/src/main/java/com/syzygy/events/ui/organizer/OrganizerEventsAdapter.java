package com.syzygy.events.ui.organizer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.syzygy.events.R;
import com.syzygy.events.database.EventAssociation;

import java.util.List;

public class OrganizerEventsAdapter extends ArrayAdapter<EventAssociation> {

    public OrganizerEventsAdapter(Context context, List<EventAssociation> dataList) {
        super(context, 0, dataList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
        EventAssociation association = getItem(position);

        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(
                    R.layout.item_organizer_events, parent, false);
        }

        TextView title = view.findViewById(R.id.organizer_events_item_title_text);
        title.setText(association.getEvent().getTitle());
        ///
        ///
        ///

        return view;
    }


}