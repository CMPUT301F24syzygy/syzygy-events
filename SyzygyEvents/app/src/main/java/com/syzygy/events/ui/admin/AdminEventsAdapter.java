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
import com.syzygy.events.database.Event;
import com.syzygy.events.database.Image;

import java.util.List;
import java.util.Locale;

public class AdminEventsAdapter extends ArrayAdapter<Event> {

    public AdminEventsAdapter(Context context, List<Event> dataList) {
        super(context, 0, dataList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
        Event event = getItem(position);

        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(
                    R.layout.item_admin_events, parent, false);
        }

        TextView title = view.findViewById(R.id.admin_events_item_title_text);
        title.setText(event.getTitle());

        TextView price = view.findViewById(R.id.admin_events_item_price_text);
        price.setText(String.format(Locale.getDefault(), "$ %3.2f", event.getPrice()));

        ImageView image = view.findViewById(R.id.admin_events_item_poster_img);
        Image.getFormatedAssociatedImage(event, Image.Options.Square(Image.Options.Sizes.MEDIUM)).into(image);


        ///
        ///

        return view;
    }


}