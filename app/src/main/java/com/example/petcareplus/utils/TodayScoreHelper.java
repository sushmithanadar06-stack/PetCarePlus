package com.example.petcareplus.utils;

import com.example.petcareplus.models.Pet;
import com.example.petcareplus.models.WellnessEntry;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TodayScoreHelper {

    public interface Callback {
        void onResult(WellnessScoreCalculator.Result result);
    }

    private static long getStartOfTodayMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private static double getAgeInMonthsAt(Pet pet, long referenceMillis) {
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

    /**
     * Computes the pet's current health score using the exact same
     * weight + daily-care + mood formula as Wellness Analytics, so the
     * number shown on the Home dashboard card always matches Analytics.
     */
    public static void computeTodayScore(FirebaseFirestore db, Pet pet, Callback callback) {

        db.collection("pets").document(pet.getId())
                .collection("wellnessEntries")
                .whereEqualTo("type", "weight")
                .get()
                .addOnSuccessListener(weightSnap -> {

                    List<WellnessEntry> weightEntries = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : weightSnap) {
                        WellnessEntry entry = doc.toObject(WellnessEntry.class);
                        entry.setId(doc.getId());
                        weightEntries.add(entry);
                    }
                    Collections.sort(weightEntries, (a, b) ->
                            Long.compare(b.getTimestampMillis(), a.getTimestampMillis()));

                    String latestWeight = weightEntries.isEmpty() ? null : weightEntries.get(0).getValue();
                    long asOfMillis = weightEntries.isEmpty()
                            ? System.currentTimeMillis()
                            : weightEntries.get(0).getTimestampMillis();

                    db.collection("pets").document(pet.getId())
                            .collection("wellnessEntries")
                            .whereGreaterThanOrEqualTo("timestampMillis", getStartOfTodayMillis())
                            .get()
                            .addOnSuccessListener(todaySnap -> {

                                List<WellnessEntry> careEntries = new ArrayList<>();
                                List<WellnessEntry> moodEntries = new ArrayList<>();

                                for (QueryDocumentSnapshot doc : todaySnap) {
                                    WellnessEntry entry = doc.toObject(WellnessEntry.class);
                                    String type = entry.getType();
                                    if ("meal".equals(type) || "walk".equals(type) || "water".equals(type)) {
                                        careEntries.add(entry);
                                    } else if ("mood".equals(type)) {
                                        moodEntries.add(entry);
                                    }
                                }

                                if (moodEntries.size() > 1) {
                                    Collections.sort(moodEntries, (a, b) ->
                                            Long.compare(a.getTimestampMillis(), b.getTimestampMillis()));
                                }

                                double ageMonths = getAgeInMonthsAt(pet, asOfMillis);

                                WellnessScoreCalculator.Result result = WellnessScoreCalculator.compute(
                                        pet, latestWeight, ageMonths, careEntries, moodEntries
                                );

                                callback.onResult(result);
                            })
                            .addOnFailureListener(e -> {
                                double ageMonths = getAgeInMonthsAt(pet, asOfMillis);
                                WellnessScoreCalculator.Result result = WellnessScoreCalculator.compute(
                                        pet, latestWeight, ageMonths, new ArrayList<>(), new ArrayList<>()
                                );
                                callback.onResult(result);
                            });
                })
                .addOnFailureListener(e -> callback.onResult(new WellnessScoreCalculator.Result(0, false)));
    }
}