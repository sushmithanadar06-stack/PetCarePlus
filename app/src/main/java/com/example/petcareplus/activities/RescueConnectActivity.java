package com.example.petcareplus.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.models.RescueReport;
import com.example.petcareplus.utils.RescueImageUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class RescueConnectActivity extends AppCompatActivity {

    private LinearLayout listContainer;
    private TextView txtEmpty, txtSummary, tabOpen, tabRescued;
    private ProgressBar progress;
    private FirebaseFirestore db;

    private final List<RescueReport> reports = new ArrayList<>();
    private boolean showRescued = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rescue_connect);

        db = FirebaseFirestore.getInstance();

        listContainer = findViewById(R.id.listContainer);
        txtEmpty = findViewById(R.id.txtEmpty);
        txtSummary = findViewById(R.id.txtSummary);
        tabOpen = findViewById(R.id.tabOpen);
        tabRescued = findViewById(R.id.tabRescued);
        progress = findViewById(R.id.progress);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tabOpen.setOnClickListener(v -> {
            showRescued = false;
            render();
        });
        tabRescued.setOnClickListener(v -> {
            showRescued = true;
            render();
        });

        findViewById(R.id.btnReport).setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null || user.isAnonymous()) {
                Toast.makeText(this, "Please create an account to report a rescue",
                        Toast.LENGTH_LONG).show();
                return;
            }
            startActivity(new Intent(this, ReportRescueActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReports();
    }

    private void loadReports() {
        progress.setVisibility(View.VISIBLE);

        db.collection("rescueReports")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .addOnSuccessListener(qs -> {
                    progress.setVisibility(View.GONE);
                    reports.clear();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        reports.add(RescueReport.fromSnapshot(d));
                    }
                    render();
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load reports: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /** Shows either the Open or the Rescued reports, with counts on the tabs. */
    private void render() {
        int open = 0;
        int rescued = 0;
        for (RescueReport r : reports) {
            if (r.isRescued()) rescued++;
            else open++;
        }

        tabOpen.setText("Open (" + open + ")");
        tabRescued.setText("Rescued (" + rescued + ")");
        styleTab(tabOpen, !showRescued);
        styleTab(tabRescued, showRescued);

        if (showRescued && rescued > 0) {
            txtSummary.setText("\uD83D\uDC3E " + rescued
                    + (rescued == 1 ? " animal has" : " animals have")
                    + " been rescued and are safe");
            txtSummary.setVisibility(View.VISIBLE);
        } else {
            txtSummary.setVisibility(View.GONE);
        }

        listContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        int shown = 0;
        for (RescueReport r : reports) {
            if (r.isRescued() == showRescued) {
                listContainer.addView(buildItem(inflater, r));
                shown++;
            }
        }

        txtEmpty.setText(showRescued
                ? "No animals have been rescued yet."
                : "No open rescue reports right now.\nSpotted an animal in trouble? Tap the button below.");
        txtEmpty.setVisibility(shown == 0 ? View.VISIBLE : View.GONE);
    }

    private void styleTab(TextView tab, boolean selected) {
        tab.setBackgroundResource(selected ? R.drawable.bg_button_orange_filled
                : R.drawable.bg_rescue_input);
        tab.setTextColor(selected ? 0xFFFFFFFF : 0xFF1B2B24);
    }

    private View buildItem(LayoutInflater inflater, RescueReport r) {
        View v = inflater.inflate(R.layout.item_rescue_report, listContainer, false);

        ImageView img = v.findViewById(R.id.imgThumb);
        TextView txtTitle = v.findViewById(R.id.txtTitle);
        TextView txtStatus = v.findViewById(R.id.txtStatus);
        TextView txtDesc = v.findViewById(R.id.txtDesc);
        TextView txtMeta = v.findViewById(R.id.txtMeta);

        txtTitle.setText("Rescue #" + r.shortCode());
        txtDesc.setText(r.description == null ? "" : r.description);

        txtStatus.setText(r.statusLabel());
        if (r.isRescued()) {
            txtStatus.setBackgroundResource(R.drawable.bg_chip_green);
            txtStatus.setTextColor(0xFF1E5C45);
        } else {
            txtStatus.setBackgroundResource(R.drawable.bg_chip_orange);
            txtStatus.setTextColor(0xFFD9622B);
        }
        txtStatus.setPadding(dp(8), dp(3), dp(8), dp(3));

        Bitmap thumb = RescueImageUtils.base64ToBitmap(r.thumb);
        if (thumb != null) {
            img.setImageBitmap(thumb);
        } else {
            img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            img.setImageResource(android.R.drawable.ic_menu_gallery);
            img.setImageTintList(ColorStateList.valueOf(0xFFB0B0A8));
        }

        StringBuilder meta = new StringBuilder("\uD83D\uDCCD ").append(r.displayLocation());
        if (r.photoCount > 0) {
            meta.append(" \u2022 ").append(r.photoCount)
                    .append(r.photoCount == 1 ? " photo" : " photos");
        }
        String ago = RescueReport.timeAgo(r.createdAt);
        if (!ago.isEmpty()) meta.append(" \u2022 ").append(ago);
        txtMeta.setText(meta.toString());

        v.setOnClickListener(view -> {
            Intent i = new Intent(this, RescueDetailActivity.class);
            i.putExtra("reportId", r.id);
            startActivity(i);
        });
        return v;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}