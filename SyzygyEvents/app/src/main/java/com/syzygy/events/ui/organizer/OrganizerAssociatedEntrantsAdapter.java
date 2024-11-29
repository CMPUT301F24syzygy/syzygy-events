package com.syzygy.events.ui.organizer;

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
import com.syzygy.events.database.EventAssociation;
import com.syzygy.events.database.Image;

import java.util.List;

/**
 * The array adapter used for showing users associated to the event
 * <p>
 * Map
 * <pre>
 * 1. Organizer Activity -> Events -> [Event]
 * 2. Organizer Activity -> Add Event -> [Submit]
 * </pre>
 */
public class OrganizerAssociatedEntrantsAdapter extends ArrayAdapter<EventAssociation> {

    public OrganizerAssociatedEntrantsAdapter(Context context, List<EventAssociation> dataList) {
        super(context, 0, dataList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
        EventAssociation association = getItem(position);

        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(
                    R.layout.item_event_associated_entrants, parent, false);
        }

        TextView name = view.findViewById(R.id.associated_entrant_item_name_text);
        name.setText(association.getUser().getName());

        TextView email = view.findViewById(R.id.associated_entrant_item_info_text);
        email.setText(association.getUser().getEmail());

        ImageView image = view.findViewById(R.id.associated_entrant_item_profile_img);
        Image.getFormatedAssociatedImage(association.getUser(), Image.Options.Circle(200)).into(image);


        return view;
    }


}