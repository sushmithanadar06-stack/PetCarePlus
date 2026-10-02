package com.example.petcareplus.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.models.Pet;
import com.example.petcareplus.models.WellnessEntry;
import com.example.petcareplus.utils.DailySummaryHelper;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DailyCareActivity extends AppCompatActivity {

    private TextView btnDailyCareBack;
    private TextView txtMealCount, txtWalkCount, txtWaterCount;
    private TextView btnAddMeal, btnAddWalk, btnAddWater;
    private LinearLayout dailyCareLogList;
    private TextView txtNoDailyCareLog;

    private Pet pet;
    private FirebaseFirestore db;
    private final List<WellnessEntry> todayEntries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_care);

        db = FirebaseFirestore.getInstance();

        btnDailyCareBack = findViewById(R.id.btnDailyCareBack);
        txtMealCount = findViewById(R.id.txtMealCount);
        txtWalkCount = findViewById(R.id.txtWalkCount);
        txtWaterCount = findViewById(R.id.txtWaterCount);
        btnAddMeal = findViewById(R.id.btnAddMeal);
        btnAddWalk = findViewById(R.id.btnAddWalk);
        btnAddWater = findViewById(R.id.btnAddWater);
        dailyCareLogList = findViewById(R.id.dailyCareLogList);
        txtNoDailyCareLog = findViewById(R.id.txtNoDailyCareLog);

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            pet = getIntent().getSerializableExtra("pet", Pet.class);
        } else {
            pet = (Pet) getIntent().getSerializableExtra("pet");
        }

        if (pet == null) {
            Toast.makeText(this, "Unable to load pet", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnDailyCareBack.setOnClickListener(v -> finish());

        btnAddMeal.setOnClickListener(v -> logEntry("meal"));
        btnAddWalk.setOnClickListener(v -> logEntry("walk"));
        btnAddWater.setOnClickListener(v -> logEntry("water"));

        rollThenLoad();
    }

    private void rollThenLoad() {
        DailySummaryHelper.rollPreviousDays(db, pet.getId(), this::loadToday);
    }

    private long getStartOfTodayMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private void logEntry(String type) {

        DocumentReference newRef = db.collection("pets")
                .document(pet.getId())
                .collection("wellnessEntries")
                .document();

        WellnessEntry entry = new WellnessEntry(
                newRef.getId(), pet.getId(), type, "1", "", System.currentTimeMillis()
        );

        newRef.set(entry)
                .addOnSuccessListener(unused -> loadToday())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to log: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void loadToday() {

        db.collection("pets")
                .document(pet.getId())
                .collection("wellnessEntries")
                .whereGreaterThanOrEqualTo("timestampMillis", getStartOfTodayMillis())
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    todayEntries.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        WellnessEntry entry = doc.toObject(WellnessEntry.class);
                        entry.setId(doc.getId());
                        String t = entry.getType();
                        if ("meal".equals(t) || "walk".equals(t) || "water".equals(t)) {
                            todayEntries.add(entry);
                        }
                    }

                    Collections.sort(todayEntries, (a, b) ->
                            Long.compare(b.getTimestampMillis(), a.getTimestampMillis()));

                    updateCounts();
                    renderLog();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void updateCounts() {
        int meals = 0, walks = 0, water = 0;
        for (WellnessEntry entry : todayEntries) {
            switch (entry.getType()) {
                case "meal": meals++; break;
                case "walk": walks++; break;
                case "water": water++; break;
            }
        }
        txtMealCount.setText(meals + " today");
        txtWalkCount.setText(walks + " today");
        txtWaterCount.setText(water + " today");
    }

    private void renderLog() {

        dailyCareLogList.removeAllViews();

        if (todayEntries.isEmpty()) {
            txtNoDailyCareLog.setVisibility(View.VISIBLE);
            return;
        }

        txtNoDailyCareLog.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

        for (WellnessEntry entry : todayEntries) {

            View row = inflater.inflate(R.layout.item_log_row, dailyCareLogList, false);

            // Updated view bindings matching the new item_log_row.xml design
            TextView txtLogIcon = row.findViewById(R.id.txtLogIcon);
            TextView txtLogTitle = row.findViewById(R.id.txtLogTitle);
            TextView txtLogSubtitle = row.findViewById(R.id.txtLogSubtitle);

            String title, icon;
            switch (entry.getType()) {
                case "meal":
                    title = "Meal logged";
                    icon = "🍽️";
                    break;
                case "walk":
                    title = "Walk logged";
                    icon = "🐾";
                    break;
                default:
                    title = "Water logged";
                    icon = "💧";
                    break;
            }

            txtLogIcon.setText(icon);
            txtLogTitle.setText(title);
            txtLogSubtitle.setText(timeFormat.format(new java.util.Date(entry.getTimestampMillis())));

            dailyCareLogList.addView(row);
        }
    }
}
