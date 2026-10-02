package com.example.petcareplus.utils;

import com.example.petcareplus.models.WellnessDailySummary;
import com.example.petcareplus.models.WellnessEntry;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DailySummaryHelper {

    public interface OnRollComplete {
        void onDone();
    }

    private static String dayKey(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date(millis));
    }

    private static long startOfDay(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Finds every meal/walk/water/mood entry NOT from today, rolls each day
     * into a compact dailySummaries/{yyyy-MM-dd} document, then deletes the
     * raw entries for that day. Today's entries are left untouched.
     */
    public static void rollPreviousDays(FirebaseFirestore db, String petId, OnRollComplete callback) {

        String todayKey = dayKey(System.currentTimeMillis());

        db.collection("pets").document(petId).collection("wellnessEntries")
                .whereIn("type", Arrays.asList("meal", "walk", "water", "mood"))
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    Map<String, List<QueryDocumentSnapshot>> byDay = new HashMap<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        WellnessEntry entry = doc.toObject(WellnessEntry.class);
                        String key = dayKey(entry.getTimestampMillis());
                        if (key.equals(todayKey)) continue; // leave today alone
                        if (!byDay.containsKey(key)) byDay.put(key, new ArrayList<>());
                        byDay.get(key).add(doc);
                    }

                    if (byDay.isEmpty()) {
                        callback.onDone();
                        return;
                    }

                    WriteBatch batch = db.batch();

                    for (Map.Entry<String, List<QueryDocumentSnapshot>> group : byDay.entrySet()) {

                        String dateKey = group.getKey();
                        boolean hadMeal = false, hadWalk = false, hadWater = false;
                        String mood = null;
                        long latestMoodTs = -1;
                        long dayStartMillis = 0;

                        for (QueryDocumentSnapshot doc : group.getValue()) {
                            WellnessEntry entry = doc.toObject(WellnessEntry.class);
                            dayStartMillis = startOfDay(entry.getTimestampMillis());

                            switch (entry.getType()) {
                                case "meal": hadMeal = true; break;
                                case "walk": hadWalk = true; break;
                                case "water": hadWater = true; break;
                                case "mood":
                                    if (entry.getTimestampMillis() > latestMoodTs) {
                                        latestMoodTs = entry.getTimestampMillis();
                                        mood = entry.getValue();
                                    }
                                    break;
                            }

                            batch.delete(doc.getReference());
                        }

                        WellnessDailySummary summary = new WellnessDailySummary(
                                dateKey, petId, hadMeal, hadWalk, hadWater, mood, dayStartMillis
                        );

                        batch.set(
                                db.collection("pets").document(petId)
                                        .collection("dailySummaries").document(dateKey),
                                summary,
                                SetOptions.merge()
                        );
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onDone())
                            .addOnFailureListener(e -> callback.onDone());
                })
                .addOnFailureListener(e -> callback.onDone());
    }
}