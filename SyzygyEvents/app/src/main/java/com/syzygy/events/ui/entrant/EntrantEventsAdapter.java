package com.syzygy.events.ui.entrant;

import static java.lang.String.format;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.syzygy.events.R;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.EventAssociation;
import com.syzygy.events.database.Image;

import java.util.List;
import java.util.Locale;

public class EntrantEventsAdapter extends ArrayAdapter<EventAssociation> {

    public EntrantEventsAdapter(Context context, List<EventAssociation> dataList) {
        super(context, 0, dataList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
        EventAssociation association = getItem(position);

        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(
                    R.layout.item_entrant_events, parent, false);
        }

        TextView title = view.findViewById(R.id.entrant_events_item_title_text);
        title.setText(association.getEvent().getTitle());

        TextView price = view.findViewById(R.id.entrant_events_item_price_text);
        price.setText(String.format(Locale.getDefault(), "$ %3.2f", association.getEvent().getPrice()));

        ImageView image = view.findViewById(R.id.entrant_events_item_poster_img);
        Image.getFormatedAssociatedImage(association.getEvent(), Image.Options.Square(Image.Options.Sizes.MEDIUM)).into(image);


        ///
        ///

        return view;
    }


}