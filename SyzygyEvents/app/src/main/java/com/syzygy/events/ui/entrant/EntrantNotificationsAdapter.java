package com.syzygy.events.ui.entrant;

import com.syzygy.events.R;
import com.syzygy.events.database.Notification;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

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
        body.setText(notification.getBody());

        if(notification.getSender() != null){
            TextView sender = view.findViewById(R.id.notification_item_sender_text);
            sender.setText(notification.getSender().getName());
        }

        TextView sent_date = view.findViewById(R.id.notification_item_sent_date_text);
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        sent_date.setText(df.format(notification.getSentTime().toDate()));

        ///notification.getIsRead();
        ///


        return view;
    }


}
