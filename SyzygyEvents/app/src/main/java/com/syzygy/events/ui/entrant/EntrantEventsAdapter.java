package com.syzygy.events.ui.entrant;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.syzygy.events.R;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.EventAssociation;

import java.util.List;

public class EntrantEventsAdapter extends ArrayAdapter<EventAssociation> {

    public EntrantEventsAdapter(Context context, List<EventAssociation> dataList) {
        super(context, 0, dataList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
        EventAssociation event = getItem(position);

        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(
                    R.layout.item_entrant_events, parent, false);
        }

        TextView title = view.findViewById(R.id.entrant_events_item_title_text);
        title.setText(event.getEvent().getTitle());

        ///
        ///

        return view;
    }


}