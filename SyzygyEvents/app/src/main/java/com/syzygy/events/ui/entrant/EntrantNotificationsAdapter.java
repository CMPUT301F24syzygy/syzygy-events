package com.syzygy.events.ui.entrant;

import static androidx.core.content.res.ResourcesCompat.getColor;
import static java.lang.Math.max;
import static java.lang.Math.min;

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
import com.syzygy.events.database.Database;
import com.syzygy.events.database.Image;
import com.syzygy.events.database.Notification;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * The array adapter used for showing notifications
 * <p>
 * Map
 * <pre>
 * 1. Entrant Activity -> Notifications
 * </pre>
 */
public class EntrantNotificationsAdapter extends ArrayAdapter<Notification> {

    public EntrantNotificationsAdapter(Context context, List<Notification> dataList) {
        super(context, 0, dataList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
        Notification notification = getItem(position);

        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(
                    R.layout.item_entrant_notifications, parent, false);
        }

        TextView subject = view.findViewById(R.id.notification_item_subject_text);
        subject.setText(notification.getSubject());

        TextView body = view.findViewById(R.id.notification_item_body_preview_text);
        String preview = notification.getBody().split("[\\n\\t\\v]")[0];
        if (preview.length() >= 25) {
            preview = preview.substring(0, 25);
            preview = preview.substring(0, max(0, preview.lastIndexOf(" ")));
        }
        body.setText(preview + " ...");

        TextView sender = view.findViewById(R.id.notification_item_sender_text);
        ImageView image = view.findViewById(R.id.notification_item_sender_profile_img);
        if(notification.getSender() != null){
            sender.setText(notification.getSender().getName());
            Image.getFormatedAssociatedImage(notification.getSender(), Image.Options.Circle(200)).into(image);
        }
        else {
            sender.setText("------");
            Image.formatDefaultImage(Database.Collections.USERS, Image.Options.Circle(Image.Options.Sizes.MEDIUM)).into(image);
        }

        TextView sent_date = view.findViewById(R.id.notification_item_sent_date_text);
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        sent_date.setText(df.format(notification.getSentTime().toDate()));

        ImageView marker = view.findViewById(R.id.notification_unread_mark);
        marker.setVisibility(notification.getIsRead() ? View.GONE : View.VISIBLE);

        //view.setBackgroundColor(getContext().getColor(notification.getIsRead() ? R.color.highlight_01 : R.color.white));

        return view;
    }


}
