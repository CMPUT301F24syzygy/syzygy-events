package com.syzygy.events.ui.organizer;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.syzygy.events.SyzygyApplication;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.DatabaseInstance;
import com.syzygy.events.database.Event;
import com.syzygy.events.database.Image;
import com.syzygy.events.databinding.SecondaryOrganizerEventBinding;
import com.syzygy.events.ui.EntrantActivity;
import com.syzygy.events.ui.OrganizerActivity;

public class OrgEventSecondaryFragment extends Fragment {
    private SecondaryOrganizerEventBinding binding;
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