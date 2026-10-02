package com.example.petcareplus.utils;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

/**
 * On Android 12+ (API 31+), an app cannot schedule EXACT alarms unless the
 * person has explicitly turned on "Alarms & reminders" for it in system
 * settings. Without this, reminders silently become inexact and can be
 * delayed for hours by Doze — which looks exactly like "reminders don't work".
 */
public class AlarmPermissionHelper {

    /** True if this app is currently allowed to schedule exact alarms. */
    public static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true; // not needed pre-Android 12
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return am != null && am.canScheduleExactAlarms();
    }

    /** Opens the exact system settings screen where the person can grant it. */
    public static void openExactAlarmSettings(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}