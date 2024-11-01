package com.syzygy.events.ui.organizer;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OrgEventSecondaryFragment extends Fragment {
    private SecondaryOrganizerEventBinding binding;
    private DatabaseInfLoadQuery<EventAssociation> query;
    Event event;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = SecondaryOrganizerEventBinding.inflate(inflater, container, false);

        OrganizerActivity activity = (OrganizerActivity)getActivity();
        SyzygyApplication app = (SyzygyApplication)getActivity().getApplication();
        app.getDatabase().<Event>getInstance(Database.Collections.EVENTS, activity.getEventID(), (instance, success) -> {
            event = instance;
            /*
            event.addListener(new Database.UpdateListener() {
                @Override
                public <T extends DatabaseInstance<T>> void onUpdate(DatabaseInstance<T> instance, Type type) {
                    ///check exists
                    ///
                    ///update view

                }
            });

             */

            binding.eventTitle.setText(event.getTitle());
            binding.eventPriceText.setText("$ " + event.getPrice().toString());
            Image.getFormatedAssociatedImage(event, Image.Options.AsIs()).into(binding.eventImg);
            if (event.getRequiresLocation()) {
                binding.eventGeoRequiredText.setVisibility(View.VISIBLE);
            }
            binding.eventDescriptionText.setText(event.getDescription());


            ////
            query = new DatabaseInfLoadQuery<>(DatabaseQuery.getAttachedUsers(app.getDatabase(), event, null, false));
            List<EventAssociation> dataList = query.getInstances();
            AssociatedEntrantsAdapter a = new AssociatedEntrantsAdapter(this.getContext(), dataList);
            query.refreshData((query1, s) -> {
                a.notifyDataSetChanged();
            });
            binding.eventAssociatedEntrantsList.setAdapter(a);

            ////

            binding.eventImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SyzygyApplication app = (SyzygyApplication)getActivity().getApplication();
                    app.displayImage(event);
                }
            });

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





            ///
            ///
            updateView();
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
        ///
        ///

    }

}