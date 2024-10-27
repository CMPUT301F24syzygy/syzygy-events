package com.syzygy.events.ui.entrant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.syzygy.events.R;
import com.syzygy.events.database.Database;
import com.syzygy.events.database.Notification;
import com.syzygy.events.databinding.FragmentEntrantNotificationsBinding;

import java.util.ArrayList;
import java.util.Arrays;

public class EntrantNotificationsFragment extends Fragment {
    private FragmentEntrantNotificationsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEntrantNotificationsBinding.inflate(inflater, container, false);


        ArrayList<Notification> dataList = new ArrayList<Notification>();

        ////
        ////
        ////

        EntrantNotificationsAdapter a = new EntrantNotificationsAdapter(this.getContext(), dataList);
        binding.entrantNotificationsList.setAdapter(a);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}