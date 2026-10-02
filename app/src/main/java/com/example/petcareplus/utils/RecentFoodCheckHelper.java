package com.example.petcareplus.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class RecentFoodCheckHelper {

    private static final String PREFS_NAME = "petcare_food_safety";
    private static final String KEY_RECENT = "recent_foods";
    private static final int MAX_RECENT = 10;

    public static void addRecent(Context context, String foodName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String stored = prefs.getString(KEY_RECENT, "");

        List<String> list = new ArrayList<>();
        if (!stored.isEmpty()) {
            for (String s : stored.split("\\|\\|")) {
                if (!s.trim().isEmpty()) list.add(s);
            }
        }

        list.remove(foodName);
        list.add(0, foodName);

        LinkedHashSet<String> deduped = new LinkedHashSet<>(list);
        List<String> finalList = new ArrayList<>(deduped);
        if (finalList.size() > MAX_RECENT) {
            finalList = finalList.subList(0, MAX_RECENT);
        }

        StringBuilder sb = new StringBuilder();
        for (String s : finalList) {
            if (sb.length() > 0) sb.append("||");
            sb.append(s);
        }

        prefs.edit().putString(KEY_RECENT, sb.toString()).apply();
    }

    public static List<String> getRecent(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String stored = prefs.getString(KEY_RECENT, "");
        List<String> list = new ArrayList<>();
        if (!stored.isEmpty()) {
            for (String s : stored.split("\\|\\|")) {
                if (!s.trim().isEmpty()) list.add(s);
            }
        }
        return list;
    }
}