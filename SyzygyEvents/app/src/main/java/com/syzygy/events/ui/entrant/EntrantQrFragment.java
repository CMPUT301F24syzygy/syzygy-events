package com.syzygy.events.ui.entrant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.zxing.integration.android.IntentIntegrator;
import com.syzygy.events.databinding.FragEntrantQrBinding;

/**
 * The fragment that the user opens when they want to scan a qr code
 * <p>
 * Map
 * <pre>
 * 3. Entrant Activity -> QR Scan
 * </pre>
 */
public class EntrantQrFragment extends Fragment {
    private FragEntrantQrBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragEntrantQrBinding.inflate(inflater, container, false);
        binding.scanQrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator intentIntegrator = new IntentIntegrator(getActivity());
                intentIntegrator.setCaptureActivity(QrCaptureActivity.class);
                intentIntegrator.setPrompt("Scan a QR Code");
                intentIntegrator.setBeepEnabled(false);
                intentIntegrator.initiateScan();
            }
        });
        return binding.getRoot();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}