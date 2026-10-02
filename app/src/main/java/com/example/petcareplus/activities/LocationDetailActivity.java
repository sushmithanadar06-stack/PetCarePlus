package com.example.petcareplus.activities;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.petcareplus.R;

public class LocationDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "name";
    public static final String EXTRA_ADDRESS = "address";
    public static final String EXTRA_HOURS = "hours";
    public static final String EXTRA_PHONE = "phone";

    private String name, address, hours, phone;
    private double lat, lng;

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_detail);

        // keep the header below the status bar
        View rootDetail = findViewById(R.id.rootDetail);
        View detailHeader = findViewById(R.id.detailHeader);
        int basePadTop = detailHeader.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(rootDetail, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            detailHeader.setPadding(detailHeader.getPaddingLeft(), bars.top + basePadTop,
                    detailHeader.getPaddingRight(), detailHeader.getPaddingBottom());
            return insets;
        });

        TextView btnBack = findViewById(R.id.btnDetailBack);
        TextView txtDetailName = findViewById(R.id.txtDetailName);
        TextView txtDetailAddress = findViewById(R.id.txtDetailAddress);
        TextView txtDetailHours = findViewById(R.id.txtDetailHours);
        TextView txtDetailPhone = findViewById(R.id.txtDetailPhone);
        View rowContact = findViewById(R.id.rowContact);          // CardView -> View (fixes the crash)
        Button btnGetDirections = findViewById(R.id.btnGetDirections);
        WebView webLocationMap = findViewById(R.id.webLocationMap);

        name = getIntent().getStringExtra(EXTRA_TITLE);
        address = getIntent().getStringExtra(EXTRA_ADDRESS);
        hours = getIntent().getStringExtra(EXTRA_HOURS);
        phone = getIntent().getStringExtra(EXTRA_PHONE);

        lat = getIntent().getDoubleExtra("latitude", 19.0760);
        lng = getIntent().getDoubleExtra("longitude", 72.8777);

        txtDetailName.setText(name != null && !name.isEmpty() ? name : "Location");
        txtDetailAddress.setText(address != null && !address.isEmpty() ? address : "Not available");
        txtDetailHours.setText(hours != null && !hours.isEmpty() ? hours : "Not available");
        txtDetailPhone.setText(phone != null && !phone.isEmpty() ? phone : "Not available");

        // ---- MAP (OpenStreetMap, no API key) ----
        WebSettings settings = webLocationMap.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webLocationMap.setWebViewClient(new WebViewClient());

        // let the map pan/zoom without the page scrolling at the same time
        webLocationMap.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        double d = 0.004;
        String mapUrl = "https://www.openstreetmap.org/export/embed.html?bbox="
                + (lng - d) + "%2C" + (lat - d) + "%2C" + (lng + d) + "%2C" + (lat + d)
                + "&layer=mapnik&marker=" + lat + "%2C" + lng;
        webLocationMap.loadUrl(mapUrl);

        btnBack.setOnClickListener(v -> finish());

        rowContact.setOnClickListener(v -> {
            if (phone == null || phone.trim().isEmpty() || phone.equalsIgnoreCase("Not specified")) {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "No phone app found", Toast.LENGTH_SHORT).show();
            }
        });

        btnGetDirections.setOnClickListener(v -> openDirections());
    }

    /** Turn-by-turn navigation in Google Maps; falls back to the browser. */
    private void openDirections() {

        try {
            Intent nav = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + lat + "," + lng));
            nav.setPackage("com.google.android.apps.maps");
            startActivity(nav);
            return;
        } catch (ActivityNotFoundException ignored) {
        }

        try {
            Uri web = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng);
            startActivity(new Intent(Intent.ACTION_VIEW, web));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No maps app or browser found", Toast.LENGTH_SHORT).show();
        }
    }
}