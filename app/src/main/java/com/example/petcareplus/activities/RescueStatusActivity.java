package com.example.petcareplus.activities;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.models.RescueReport;
import com.google.firebase.firestore.FirebaseFirestore;

public class RescueStatusActivity extends AppCompatActivity {

    private static final String[] KEYS = {
            RescueReport.STEP_REPORTED,
            RescueReport.STEP_VOLUNTEER,
            RescueReport.STEP_RESCUED
    };
    private static final String[] TITLES = {
            "Reported", "Volunteer en route", "Rescued & Safe"
    };
    private static final String[] ICONS = {
            "\uD83D\uDCDD", "\uD83D\uDE97", "\uD83D\uDC3E"
    };

    private LinearLayout stepsContainer;
    private String reportId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rescue_status);

        reportId = getIntent().getStringExtra("reportId");
        stepsContainer = findViewById(R.id.stepsContainer);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (reportId == null) {
            finish();
            return;
        }

        FirebaseFirestore.getInstance().collection("rescueReports").document(reportId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "This report is no longer available",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    render(RescueReport.fromSnapshot(doc));
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Failed to load status: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void render(RescueReport r) {
        stepsContainer.removeAllViews();

        // index of the last completed step (a later step being done implies the earlier ones)
        int lastDone = 0; // "Reported" is always done
        for (int i = 0; i < KEYS.length; i++) {
            if (r.stepTimes.containsKey(KEYS[i])) lastDone = Math.max(lastDone, i);
        }
        if (r.hasVolunteer()) lastDone = Math.max(lastDone, 1);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < KEYS.length; i++) {
            View item = inflater.inflate(R.layout.item_rescue_status_step, stepsContainer, false);

            TextView icon = item.findViewById(R.id.txtIcon);
            TextView title = item.findViewById(R.id.txtStepTitle);
            TextView sub = item.findViewById(R.id.txtStepSub);
            TextView chip = item.findViewById(R.id.txtChip);

            title.setText(TITLES[i]);

            boolean done = i <= lastDone;
            boolean inProgress = !done && i == lastDone + 1;

            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);

            if (done) {
                circle.setColor(0xFF1E5C45);
                icon.setText("\u2705");
                Long t = r.stepTimes.get(KEYS[i]);
                sub.setText(t != null ? RescueReport.formatTime(t) : "Completed");
                chip.setVisibility(View.GONE);
            } else if (inProgress) {
                circle.setColor(0xFFFDE3D3);
                icon.setText(ICONS[i]);
                sub.setText("In progress");
                chip.setVisibility(View.VISIBLE);
                chip.setBackgroundResource(R.drawable.bg_chip_orange);
                chip.setPadding(dp(8), dp(3), dp(8), dp(3));
            } else {
                circle.setColor(0xFFFDE3D3);
                icon.setText("\u23F3");
                sub.setText("Pending");
                chip.setVisibility(View.GONE);
            }

            icon.setBackground(circle);
            stepsContainer.addView(item);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}