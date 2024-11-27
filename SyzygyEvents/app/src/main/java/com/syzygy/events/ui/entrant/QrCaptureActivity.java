package com.syzygy.events.ui.entrant;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toolbar;

import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.syzygy.events.R;

public class QrCaptureActivity extends com.journeyapps.barcodescanner.CaptureActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected DecoratedBarcodeView initializeContent() {
        setContentView(R.layout.qr_capture);
        Toolbar t = findViewById(R.id.toolbar);
        t.setTitle("Scan");
        setActionBar(t);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        return findViewById(R.id.qr_barcode_scanner);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return true;
    }

}
