package com.syzygy.events.ui.entrant;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInfLoadQuery;
import com.syzygy.events.database.DatabaseQuery;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.Image;
import com.syzygy.events.database.Notification;
import com.syzygy.events.databinding.FragEntrantNotificationsListBinding;
import com.syzygy.events.ui.EntrantActivity;

/**
 * The fragment that the user opens to see their notifications. The notification tab
 * <p>
 * Map
 * <pre>
 * 2. Entrant Activity -> Notifications -> [Notification]
 * </pre>
 */
public class EntrantNotificationsFragment extends Fragment {
    private FragEntrantNotificationsListBinding binding;
    /**
     * The query to get all notifications
     */
    private DatabaseInfLoadQuery<Notification> query;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragEntrantNotificationsListBinding.inflate(inflater, container, false);

        SyzygyApplication app = (SyzygyApplication)getActivity().getApplication();
        query = new DatabaseInfLoadQuery<>(DatabaseQuery.getMyNotifications(app.getDatabase(), app.getUser()));

        EntrantNotificationsAdapter a = new EntrantNotificationsAdapter(this.getContext(), query.getInstances());

        query.refreshData((query1, success) -> {
            a.notifyDataSetChanged();
            binding.emptyNotificationsText.setVisibility(binding.entrantNotificationsList.getCount()<1 ? View.VISIBLE : View.GONE);
        });

        binding.entrantNotificationsList.setAdapter(a);

        binding.entrantNotificationsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Notification notification = a.getItem(position);
                Dialog dialog = new AlertDialog.Builder(getContext())
                        .setView(R.layout.popup_notification)
                        .create();
                dialog.show();

                notification.setIsRead(true);
                a.notifyDataSetChanged();

                TextView subject = dialog.findViewById(R.id.popup_notification_subject_text);
                subject.setText(notification.getSubject());

                TextView date = dialog.findViewById(R.id.popup_notification_date_text);
                date.setText(app.formatTimestamp(notification.getSentTime()));

                TextView body = dialog.findViewById(R.id.popup_notification_body_text);
                body.setText(notification.getBody());

                ImageView image = dialog.findViewById(R.id.popup_notification_sender_profile_img);
                TextView sender = dialog.findViewById(R.id.popup_notification_sender_text);

                if(notification.getSender() != null){
                    sender.setText(notification.getSender() == null ? "" : notification.getSender().getName());
                    Image.getFormatedAssociatedImage(notification.getSender(), Image.Options.Circle(Image.Options.Sizes.SMALL)).into(image);
                }
                else {
                    Image.formatDefaultImage(Database.Collections.USERS, Image.Options.Circle(Image.Options.Sizes.MEDIUM)).into(image);
                }

                if (notification.getEvent() != null) {
                    View event_card = dialog.findViewById(R.id.event_card);
                    event_card.setVisibility(View.VISIBLE);

                    TextView event_title = dialog.findViewById(R.id.card_event_title_text);
                    event_title.setText(notification.getEvent().getTitle());

                    TextView event_details = dialog.findViewById(R.id.card_event_details_text);
                    String start = app.formatTimestamp(notification.getEvent().getStartDate());
                    String start_end = String.format("%s - %s", start, app.formatTimestamp(notification.getEvent().getEndDate()));
                    event_details.setText(notification.getEvent().getEventDates() == Event.Dates.NO_REPEAT ? start : start_end);

                    ImageView event_image = dialog.findViewById(R.id.card_event_poster_img);
                    Image.getFormatedAssociatedImage(notification.getEvent(), Image.Options.Square(Image.Options.Sizes.LARGE)).into(event_image);

                    event_card.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                            EntrantActivity activity = (EntrantActivity)getActivity();
                            activity.openEvent(notification.getEventID());
                        }
                    });
                }

                dialog.setOnDismissListener(d -> {
                    query.refreshData((query1, success) -> {
                        a.notifyDataSetChanged();
                    });
                });

            }
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