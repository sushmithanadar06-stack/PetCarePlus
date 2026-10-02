package com.example.petcareplus.utils;

import com.example.petcareplus.models.HealthRecord;

import java.util.Calendar;

public class HealthRecordStatus {

    public static class Badge {
        public String label;
        public String colorHex;

        public Badge(String label, String colorHex) {
            this.label = label;
            this.colorHex = colorHex;
        }
    }

    public static Badge compute(HealthRecord record) {

        if ("vetvisit".equals(record.getCategory())) {
            return null;
        }

        if ("medication".equals(record.getCategory())) {
            return computeMedicationBadge(record);
        }

        // Vaccination / Deworming: Due (today) / Upcoming (future) / Done (past)
        if (isSameDay(record.getDateMillis(), System.currentTimeMillis())) {
            return new Badge("Due", "#F2704A");
        } else if (record.getDateMillis() < System.currentTimeMillis()) {
            return new Badge("Done", "#2F6B52");
        } else {
            return new Badge("Upcoming", "#E8B24B");
        }
    }

    private static Badge computeMedicationBadge(HealthRecord record) {
        long now = System.currentTimeMillis();
        long endMillis = record.getNextDueMillis();

        if (endMillis == 0 || now <= endMillis) {
            return new Badge("Active", "#2F6B52");
        } else {
            return new Badge("Completed", "#8A8A8A");
        }
    }

    /**
     * Returns a human-readable status line for medication records,
     * e.g. "5 days left" or "Completed 2 weeks ago".
     */
    public static String medicationSubtitle(HealthRecord record) {

        long now = System.currentTimeMillis();
        long endMillis = record.getNextDueMillis();

        if (endMillis == 0) return "";

        long diffMillis = endMillis - now;
        long diffDays = diffMillis / (1000L * 60 * 60 * 24);

        if (diffMillis >= 0) {
            if (diffDays == 0) return "Ends today";
            return diffDays + (diffDays == 1 ? " day left" : " days left");
        } else {
            long daysAgo = Math.abs(diffDays);
            if (daysAgo < 7) {
                return "Completed " + daysAgo + (daysAgo == 1 ? " day ago" : " days ago");
            } else {
                long weeksAgo = daysAgo / 7;
                return "Completed " + weeksAgo + (weeksAgo == 1 ? " week ago" : " weeks ago");
            }
        }
    }

    private static boolean isSameDay(long millis1, long millis2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(millis1);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTimeInMillis(millis2);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
}