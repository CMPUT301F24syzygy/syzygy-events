package com.syzygy.events.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.syzygy.events.R;
import com.syzygy.events.databinding.FragmentAdminProfilesBinding;

import java.util.ArrayList;
import java.util.Arrays;

public class AdminProfilesFragment extends Fragment {
    private FragmentAdminProfilesBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAdminProfilesBinding.inflate(inflater, container, false);

        String[] alpha = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"};
        ArrayList<String> dataList = new ArrayList<>(Arrays.asList(alpha));
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(), R.layout.item_admin_profiles, dataList);
        binding.adminProfilesList.setAdapter(adapter);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}