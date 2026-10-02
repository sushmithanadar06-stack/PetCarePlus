package com.example.petcareplus.utils;

import com.example.petcareplus.models.Pet;
import com.example.petcareplus.models.WellnessEntry;

import java.util.List;

public class WellnessScoreCalculator {

    public static class Result {
        public int score;
        public String verdict;
        public String weightUsed;
        public boolean hasAnyData;

        public Result(int score, String verdict, String weightUsed) {
            this.score = score;
            this.verdict = verdict;
            this.weightUsed = weightUsed;
            this.hasAnyData = true;
        }

        public Result(int score, boolean hasAnyData) {
            this.score = score;
            this.verdict = "";
            this.weightUsed = null;
            this.hasAnyData = hasAnyData;
        }
    }

    /** Weight-only score — legacy, kept for anything still calling the simple version. */
    public static Result compute(Pet pet, String loggedWeight, double ageMonths) {

        if (loggedWeight == null || loggedWeight.trim().isEmpty()) {
            return new Result(0, false);
        }

        double weightKg;
        try {
            weightKg = Double.parseDouble(loggedWeight.trim());
        } catch (NumberFormatException e) {
            return new Result(0, false);
        }

        BreedWeightReference.WeightStatus status = BreedWeightReference.evaluate(
                pet.getType(), pet.getBreed(), ageMonths, weightKg
        );

        int score;
        switch (status.verdict) {
            case "Perfect weight": score = 90; break;
            case "Underweight":
            case "Overweight": score = 55; break;
            default: score = 70;
        }

        return new Result(score, status.verdict, loggedWeight);
    }

    /**
     * Combined score: Weight 40% + Daily Care 30% + Mood 30%.
     * A category with NO data contributes 0, not "skipped" — so logging
     * only one category can never reach 100 on its own. If literally
     * nothing across all three categories is logged for this day,
     * hasAnyData is false and the day should not be plotted at all.
     */
    public static Result compute(Pet pet, String weightAsOfDay, double ageMonths,
                                 List<WellnessEntry> careEntriesForDay, List<WellnessEntry> moodEntriesForDay) {

        boolean hasWeight = weightAsOfDay != null && !weightAsOfDay.trim().isEmpty();
        boolean hasCare = careEntriesForDay != null && !careEntriesForDay.isEmpty();
        boolean hasMood = moodEntriesForDay != null && !moodEntriesForDay.isEmpty();

        if (!hasWeight && !hasCare && !hasMood) {
            return new Result(0, false);
        }

        double weightScore = 0;
        if (hasWeight) {
            try {
                double weightKg = Double.parseDouble(weightAsOfDay.trim());
                BreedWeightReference.WeightStatus status = BreedWeightReference.evaluate(
                        pet.getType(), pet.getBreed(), ageMonths, weightKg
                );
                switch (status.verdict) {
                    case "Perfect weight": weightScore = 100; break;
                    case "Underweight":
                    case "Overweight": weightScore = 55; break;
                    default: weightScore = 70;
                }
            } catch (NumberFormatException ignored) {}
        }

        double careScore = 0;
        if (hasCare) {
            boolean meal = false, walk = false, water = false;
            for (WellnessEntry e : careEntriesForDay) {
                if ("meal".equals(e.getType())) meal = true;
                if ("walk".equals(e.getType())) walk = true;
                if ("water".equals(e.getType())) water = true;
            }
            int done = (meal ? 1 : 0) + (walk ? 1 : 0) + (water ? 1 : 0);
            careScore = done / 3.0 * 100;
        }

        double moodScore = 0;
        if (hasMood) {
            String mood = moodEntriesForDay.get(moodEntriesForDay.size() - 1).getValue();
            moodScore = moodScoreFor(mood);
        }

        int finalScore = (int) Math.round(weightScore * 0.4 + careScore * 0.3 + moodScore * 0.3);

        return new Result(finalScore, true);
    }

    private static int moodScoreFor(String mood) {
        if (mood == null) return 70;
        switch (mood) {
            case "Happy":
            case "Playful": return 100;
            case "Neutral": return 70;
            case "Tired": return 55;
            case "Sad": return 40;
            case "Sick": return 25;
            default: return 70;
        }
    }
}