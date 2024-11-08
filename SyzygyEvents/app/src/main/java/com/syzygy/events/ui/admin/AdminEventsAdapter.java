package com.syzygy.events.ui.admin;

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
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.Image;

import java.util.List;
import java.util.Locale;

/**
 * The array adapter used for showing different events
 * <p>
 * Map
 * <pre>
 * 1. Admin Activity -> Browse Events
 * </pre>
 */
public class AdminEventsAdapter extends ArrayAdapter<Event> {

    public AdminEventsAdapter(Context context, List<Event> dataList) {
        super(context, 0, dataList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
        Event event = getItem(position);

        SyzygyApplication app = (SyzygyApplication) getContext().getApplicationContext();

        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(
                    R.layout.item_admin_event_list, parent, false);
        }

        TextView title = view.findViewById(R.id.admin_events_item_title_text);
        title.setText(event.getTitle());

        TextView price = view.findViewById(R.id.admin_events_item_price_text);
        price.setText(String.format(Locale.getDefault(), "$%3.2f", event.getPrice()));

        TextView dates = view.findViewById(R.id.admin_events_item_start_end_text);
        String start = app.formatTimestamp(event.getStartDate());
        String start_end = String.format("%s - %s", start, app.formatTimestamp(event.getEndDate()));
        dates.setText(event.getEventDates() == Event.Dates.NO_REPEAT ? start : start_end);

        TextView weekdays = view.findViewById(R.id.admin_events_item_weekdays_text);
        weekdays.setText(event.getFormattedEventDates());

        TextView address = view.findViewById(R.id.admin_events_item_location_text);
        address.setText(event.getFacility().getAddress());

        ImageView image = view.findViewById(R.id.admin_events_item_poster_img);
        Image.getFormatedAssociatedImage(event, Image.Options.Square(Image.Options.Sizes.MEDIUM)).into(image);

        return view;
    }


}