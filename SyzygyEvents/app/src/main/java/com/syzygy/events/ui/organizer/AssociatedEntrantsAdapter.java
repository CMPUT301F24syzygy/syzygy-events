package com.syzygy.events.ui.organizer;

import android.app.AlertDialog;
import android.app.Dialog;
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
import com.syzygy.events.database.User;

import java.util.List;
import java.util.Objects;

public class AssociatedEntrantsAdapter extends ArrayAdapter<EventAssociation> {

    public AssociatedEntrantsAdapter(Context context, List<EventAssociation> dataList) {
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

        ImageView image = view.findViewById(R.id.associated_entrant_item_profile_img);
        Image.getFormatedAssociatedImage(association.getUser(), Image.Options.Circle(200)).into(image);


        return view;
    }


}