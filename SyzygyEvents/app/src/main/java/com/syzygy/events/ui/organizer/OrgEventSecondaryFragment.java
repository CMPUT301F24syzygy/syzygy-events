package com.syzygy.events.ui.organizer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.Timestamp;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.syzygy.events.R;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInfLoadQuery;
import com.syzygy.events.database.DatabaseInstance;
import com.syzygy.events.database.DatabaseQuery;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.EventAssociation;
import com.syzygy.events.database.Image;
import com.syzygy.events.database.Notification;
import com.syzygy.events.database.User;
import com.syzygy.events.databinding.SecondaryOrganizerEventBinding;
import com.syzygy.events.ui.EntrantActivity;
import com.syzygy.events.ui.OrganizerActivity;
import com.syzygy.events.ui.entrant.EntrantNotificationsAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OrgEventSecondaryFragment extends Fragment {
    private SecondaryOrganizerEventBinding binding;
    private DatabaseInfLoadQuery<EventAssociation> queryAll;
    private DatabaseInfLoadQuery<EventAssociation> queryWaitlist;
    private DatabaseInfLoadQuery<EventAssociation> queryInvited;
    private DatabaseInfLoadQuery<EventAssociation> queryEnrolled;
    private DatabaseInfLoadQuery<EventAssociation> queryCancelled;
    private Event event;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = SecondaryOrganizerEventBinding.inflate(inflater, container, false);

        OrganizerActivity activity = (OrganizerActivity)getActivity();
        SyzygyApplication app = (SyzygyApplication)getActivity().getApplication();
        app.getDatabase().<Event>getInstance(Database.Collections.EVENTS, activity.getEventID(), (instance, success) -> {
            if (!success) {
                activity.navigateUp();
            }
            event = instance;

            event.addListener(new Database.UpdateListener() {
                @Override
                public <T extends DatabaseInstance<T>> void onUpdate(DatabaseInstance<T> instance, Type type) {
                    if (!event.isLegalState()) {
                        OrganizerActivity activity = (OrganizerActivity)getActivity();
                        activity.navigateUp();
                    }
                    ///
                    ///
                }
            });

            Image.getFormatedAssociatedImage(event, Image.Options.AsIs()).into(binding.eventImg);
            binding.eventTitle.setText(event.getTitle());
            binding.eventPriceText.setText("$ " + event.getPrice().toString());
            binding.eventStartEndText.setText("");
            binding.eventWeekdaysTimeText.setText(event.getFormattedEventDates());
            if (event.getRequiresLocation()) {
                binding.eventGeoRequiredText.setVisibility(View.VISIBLE);
            }
            binding.eventDescriptionText.setText(event.getDescription());


            ////
            queryAll = new DatabaseInfLoadQuery<>(DatabaseQuery.getAttachedUsers(app.getDatabase(), event, null, false));
            AssociatedEntrantsAdapter allAdapter = new AssociatedEntrantsAdapter(this.getContext(), queryAll.getInstances());
            queryAll.refreshData((query1, s) -> { allAdapter.notifyDataSetChanged(); });

            queryWaitlist = new DatabaseInfLoadQuery<>(DatabaseQuery.getAttachedUsers(app.getDatabase(), event, null, false));
            AssociatedEntrantsAdapter waitlistAdapter = new AssociatedEntrantsAdapter(this.getContext(), queryWaitlist.getInstances());
            queryWaitlist.refreshData((query1, s) -> { waitlistAdapter.notifyDataSetChanged(); });

            queryInvited = new DatabaseInfLoadQuery<>(DatabaseQuery.getAttachedUsers(app.getDatabase(), event, null, false));
            AssociatedEntrantsAdapter invitedAdapter = new AssociatedEntrantsAdapter(this.getContext(), queryInvited.getInstances());
            queryInvited.refreshData((query1, s) -> { invitedAdapter.notifyDataSetChanged(); });

            queryEnrolled = new DatabaseInfLoadQuery<>(DatabaseQuery.getAttachedUsers(app.getDatabase(), event, null, false));
            AssociatedEntrantsAdapter enrolledAdapter = new AssociatedEntrantsAdapter(this.getContext(), queryEnrolled.getInstances());
            queryEnrolled.refreshData((query1, s) -> { enrolledAdapter.notifyDataSetChanged(); });

            queryCancelled = new DatabaseInfLoadQuery<>(DatabaseQuery.getAttachedUsers(app.getDatabase(), event, null, false));
            AssociatedEntrantsAdapter cancelledAdapter = new AssociatedEntrantsAdapter(this.getContext(), queryCancelled.getInstances());
            queryCancelled.refreshData((query1, s) -> { cancelledAdapter.notifyDataSetChanged(); });

            binding.eventAssociatedEntrantsList.setAdapter(allAdapter);
            ////


            binding.eventImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SyzygyApplication app = (SyzygyApplication)getActivity().getApplication();
                    app.displayImage(event);
                }
            });
            updateView();

            binding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    if (tab.getPosition() == 0) {
                        binding.actionsTabLayout.setVisibility(View.VISIBLE);
                        binding.entrantsTabLayout.setVisibility(View.GONE);
                    }
                    else {
                        binding.actionsTabLayout.setVisibility(View.GONE);
                        binding.entrantsTabLayout.setVisibility(View.VISIBLE);
                    }
                }
                @Override
                public void onTabUnselected(TabLayout.Tab tab) {return;}
                @Override
                public void onTabReselected(TabLayout.Tab tab) {return;}

            });


            binding.entrantFilterChips.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
               @Override
               public void onCheckedChanged(@NonNull ChipGroup group, @NonNull List<Integer> checkedIds) {
                   if (checkedIds.get(0) == R.id.all_chip) {
                       binding.eventAssociatedEntrantsList.setAdapter(allAdapter);
                   } else if (checkedIds.get(0) == R.id.waitlist_chip) {
                       binding.eventAssociatedEntrantsList.setAdapter(waitlistAdapter);
                   } else if (checkedIds.get(0) == R.id.invited_chip) {
                       binding.eventAssociatedEntrantsList.setAdapter(invitedAdapter);
                   } else if (checkedIds.get(0) == R.id.enrolled_chip) {
                       binding.eventAssociatedEntrantsList.setAdapter(enrolledAdapter);
                   } else if (checkedIds.get(0) == R.id.cancelled_chip) {
                       binding.eventAssociatedEntrantsList.setAdapter(cancelledAdapter);
                   }
               }
           });


            binding.editPosterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ///run edit poster
                }
            });

            binding.copyQrButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(getContext().CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("qr hash", event.getQrHash());
                    clipboard.setPrimaryClip(clip);
                }
            });

            binding.downloadQrButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ///download qr
                }
            });

            binding.runLotteryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ///load amount cancelled
                    Dialog dialog = new AlertDialog.Builder(getContext())
                            .setMessage("run and send invites") //layout
                            .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    event.getLottery(-1, (e, result, success) -> {
                                        result.execute((ev, r, f) -> {}, true);
                                    });
                                }
                            })
                            .create();
                    dialog.show();
                }
            });

            binding.createNotificationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ///compose notification popup
                }
            });



            DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
            if (Timestamp.now().compareTo(event.getOpenRegistrationDate()) < 0) {
                binding.registrationDateInfoText.setText("Registration Opens " + df.format(event.getOpenRegistrationDate().toDate()));
            }
            else if (Timestamp.now().compareTo(event.getCloseRegistrationDate()) < 0) {
                binding.registrationDateInfoText.setText("Registration Open Until " + df.format(event.getCloseRegistrationDate().toDate()));
            }
            else {
                binding.registrationDateInfoText.setText("Registration Closed");
                binding.runLotteryButton.setVisibility(View.VISIBLE);
            }

            ///

        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void updateView() {
        ///
        if (!event.getQrHash().isEmpty()) {
            BitMatrix m;
            try {
                m = new MultiFormatWriter().encode(event.getQrHash(), BarcodeFormat.QR_CODE, 100, 100);
            } catch (WriterException e) {
                throw new RuntimeException(e);
            }
            Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565);
            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 100; j++) {
                    bitmap.setPixel(i, j, m.get(i, j) ? Color.BLACK : Color.WHITE);
                }
            }
            binding.facilityEventQrImg.setImageBitmap(bitmap);
        }
        else {
            binding.copyQrButton.setVisibility(View.GONE);
            binding.downloadQrButton.setVisibility(View.GONE);
        }

        ///
        ///

    }

}