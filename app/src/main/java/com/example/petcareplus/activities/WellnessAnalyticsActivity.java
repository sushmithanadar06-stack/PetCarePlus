package com.example.petcareplus.activities;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.models.Pet;
import com.example.petcareplus.models.WellnessDailySummary;
import com.example.petcareplus.models.WellnessEntry;
import com.example.petcareplus.utils.WellnessScoreCalculator;
import com.example.petcareplus.views.LineGraphView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WellnessAnalyticsActivity extends AppCompatActivity {

    private TextView btnAnalyticsBack;
    private TextView txtAvgWeight, txtCareTotals, txtCommonMood;
    private EditText etDaysRange;
    private TextView btnApplyRange;
    private LineGraphView graphHealthScore;

    private Pet pet;
    private FirebaseFirestore db;
    private int selectedRangeDays = 30;

    private final List<WellnessEntry> allWeightEntries = new ArrayList<>();
    private final Map<String, WellnessDailySummary> summariesByDay = new HashMap<>();
    private final List<WellnessEntry> todayCareEntries = new ArrayList<>();
    private final List<WellnessEntry> todayMoodEntries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wellness_analytics);

        db = FirebaseFirestore.getInstance();

        btnAnalyticsBack = findViewById(R.id.btnAnalyticsBack);
        txtAvgWeight = findViewById(R.id.txtAvgWeight);
        txtCareTotals = findViewById(R.id.txtCareTotals);
        txtCommonMood = findViewById(R.id.txtCommonMood);
        etDaysRange = findViewById(R.id.etDaysRange);
        btnApplyRange = findViewById(R.id.btnApplyRange);
        graphHealthScore = findViewById(R.id.graphHealthScore);

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

        btnAnalyticsBack.setOnClickListener(v -> finish());

        btnApplyRange.setOnClickListener(v -> {
            String input = etDaysRange.getText().toString().trim();
            int days;
            try {
                days = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Enter a valid number of days", Toast.LENGTH_SHORT).show();
                return;
            }
            if (days < 1 || days > 365) {
                Toast.makeText(this, "Enter a number between 1 and 365", Toast.LENGTH_SHORT).show();
                return;
            }
            selectedRangeDays = days;
            recompute();
        });

        loadAllData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pet != null) loadAllData();
    }

    private long getStartOfTodayMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private void loadAllData() {

        db.collection("pets").document(pet.getId())
                .collection("wellnessEntries")
                .whereEqualTo("type", "weight")
                .get()
                .addOnSuccessListener(weightSnap -> {

                    allWeightEntries.clear();
                    for (QueryDocumentSnapshot doc : weightSnap) {
                        WellnessEntry entry = doc.toObject(WellnessEntry.class);
                        entry.setId(doc.getId());
                        allWeightEntries.add(entry);
                    }
                    Collections.sort(allWeightEntries, (a, b) ->
                            Long.compare(a.getTimestampMillis(), b.getTimestampMillis()));

                    db.collection("pets").document(pet.getId())
                            .collection("dailySummaries")
                            .get()
                            .addOnSuccessListener(summarySnap -> {

                                summariesByDay.clear();
                                for (QueryDocumentSnapshot doc : summarySnap) {
                                    WellnessDailySummary summary = doc.toObject(WellnessDailySummary.class);
                                    summary.setId(doc.getId());
                                    summariesByDay.put(summary.getId(), summary);
                                }

                                loadTodayLiveData();
                            })
                            .addOnFailureListener(e -> loadTodayLiveData());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void loadTodayLiveData() {

        db.collection("pets").document(pet.getId())
                .collection("wellnessEntries")
                .whereGreaterThanOrEqualTo("timestampMillis", getStartOfTodayMillis())
                .get()
                .addOnSuccessListener(todaySnap -> {

                    todayCareEntries.clear();
                    todayMoodEntries.clear();

                    for (QueryDocumentSnapshot doc : todaySnap) {
                        WellnessEntry entry = doc.toObject(WellnessEntry.class);
                        entry.setId(doc.getId());
                        String type = entry.getType();
                        if ("meal".equals(type) || "walk".equals(type) || "water".equals(type)) {
                            todayCareEntries.add(entry);
                        } else if ("mood".equals(type)) {
                            todayMoodEntries.add(entry);
                        }
                    }

                    if (todayMoodEntries.size() > 1) {
                        Collections.sort(todayMoodEntries, (a, b) ->
                                Long.compare(a.getTimestampMillis(), b.getTimestampMillis()));
                    }

                    recompute();
                })
                .addOnFailureListener(e -> recompute());
    }

    private double getAgeInMonthsAt(long referenceMillis) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar birth = Calendar.getInstance();
            birth.setTime(sdf.parse(pet.getDateOfBirth()));

            Calendar reference = Calendar.getInstance();
            reference.setTimeInMillis(referenceMillis);

            int months = (reference.get(Calendar.YEAR) - birth.get(Calendar.YEAR)) * 12
                    + (reference.get(Calendar.MONTH) - birth.get(Calendar.MONTH));

            if (reference.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH)) {
                months -= 1;
            }

            return Math.max(months, 0);

        } catch (Exception e) {
            return 12;
        }
    }

    private String weightAsOfDay(long dayEndMillis) {
        String result = null;
        for (WellnessEntry e : allWeightEntries) {
            if (e.getTimestampMillis() <= dayEndMillis) {
                result = e.getValue();
            } else {
                break;
            }
        }
        return result;
    }

    private void recompute() {

        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat labelFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        String todayKey = keyFormat.format(new java.util.Date());

        List<Integer> scores = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int totalMeals = 0, totalWalks = 0, totalWater = 0;
        Map<String, Integer> moodCounts = new HashMap<>();

        Calendar cal = Calendar.getInstance();

        for (int i = selectedRangeDays - 1; i >= 0; i--) {

            Calendar dayCal = (Calendar) cal.clone();
            dayCal.add(Calendar.DAY_OF_YEAR, -i);
            dayCal.set(Calendar.HOUR_OF_DAY, 23);
            dayCal.set(Calendar.MINUTE, 59);
            long dayEndMillis = dayCal.getTimeInMillis();

            String dayKey = keyFormat.format(dayCal.getTime());
            labels.add(labelFormat.format(dayCal.getTime()));

            List<WellnessEntry> careEntries = new ArrayList<>();
            List<WellnessEntry> moodEntries = new ArrayList<>();

            if (dayKey.equals(todayKey)) {
                careEntries.addAll(todayCareEntries);
                moodEntries.addAll(todayMoodEntries);

                for (WellnessEntry e : todayCareEntries) {
                    if ("meal".equals(e.getType())) totalMeals++;
                    if ("walk".equals(e.getType())) totalWalks++;
                    if ("water".equals(e.getType())) totalWater++;
                }
                if (!todayMoodEntries.isEmpty()) {
                    String mood = todayMoodEntries.get(todayMoodEntries.size() - 1).getValue();
                    moodCounts.put(mood, moodCounts.getOrDefault(mood, 0) + 1);
                }

            } else {
                WellnessDailySummary summary = summariesByDay.get(dayKey);
                if (summary != null) {
                    if (summary.isHadMeal()) { careEntries.add(new WellnessEntry("", pet.getId(), "meal", "1", "", dayEndMillis)); totalMeals++; }
                    if (summary.isHadWalk()) { careEntries.add(new WellnessEntry("", pet.getId(), "walk", "1", "", dayEndMillis)); totalWalks++; }
                    if (summary.isHadWater()) { careEntries.add(new WellnessEntry("", pet.getId(), "water", "1", "", dayEndMillis)); totalWater++; }
                    if (summary.getMood() != null) {
                        moodEntries.add(new WellnessEntry("", pet.getId(), "mood", summary.getMood(), "", dayEndMillis));
                        moodCounts.put(summary.getMood(), moodCounts.getOrDefault(summary.getMood(), 0) + 1);
                    }
                }
            }

            String weight = weightAsOfDay(dayEndMillis);
            double ageMonths = getAgeInMonthsAt(dayEndMillis);

            WellnessScoreCalculator.Result result =
                    WellnessScoreCalculator.compute(pet, weight, ageMonths, careEntries, moodEntries);

            scores.add(result.hasAnyData ? result.score : null);
        }

        graphHealthScore.setData(scores, labels);

        double total = 0;
        int count = 0;
        long rangeStartMillis = System.currentTimeMillis() - (long) selectedRangeDays * 24 * 60 * 60 * 1000L;
        for (WellnessEntry e : allWeightEntries) {
            if (e.getTimestampMillis() >= rangeStartMillis) {
                try {
                    total += Double.parseDouble(e.getValue());
                    count++;
                } catch (NumberFormatException ignored) {}
            }
        }
        txtAvgWeight.setText(count == 0
                ? "No weight logs in this range"
                : String.format(Locale.getDefault(), "%.1f kg (%d entries)", total / count, count));

        txtCareTotals.setText(totalMeals == 0 && totalWalks == 0 && totalWater == 0
                ? "No entries yet"
                : totalMeals + " meals · " + totalWalks + " walks · " + totalWater + " water logs");

        if (moodCounts.isEmpty()) {
            txtCommonMood.setText("No entries yet");
        } else {
            String topMood = null;
            int topCount = 0;
            for (Map.Entry<String, Integer> e : moodCounts.entrySet()) {
                if (e.getValue() > topCount) { topCount = e.getValue(); topMood = e.getKey(); }
            }
            txtCommonMood.setText(topMood + " (" + topCount + " days)");
        }
    }
}