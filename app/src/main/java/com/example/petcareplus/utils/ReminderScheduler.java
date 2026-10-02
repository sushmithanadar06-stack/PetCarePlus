package com.example.petcareplus.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.petcareplus.models.Reminder;
import com.example.petcareplus.receivers.ReminderReceiver;

public class ReminderScheduler {

    public static void schedule(Context context, Reminder reminder, String petName) {
        scheduleAlarm(
                context,
                reminder.getPetId(),
                petName,
                reminder.getId(),
                reminder.getTitle(),
                reminder.getType(),
                reminder.getDateTimeMillis(),
                reminder.getRepeatType()
        );
    }

    public static void scheduleAlarm(Context context, String petId, String petName, String reminderId,
                                     String title, String type, long triggerMillis, String repeatType) {

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null || reminderId == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("reminderId", reminderId);
        intent.putExtra("petId", petId);
        intent.putExtra("petName", petName);
        intent.putExtra("title", title);
        intent.putExtra("type", type);
        intent.putExtra("repeatType", repeatType);

        int requestCode = reminderId.hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent);
            }
        } catch (SecurityException e) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent);
        }
    }

    public static void cancel(Context context, String reminderId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null || reminderId == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        int requestCode = reminderId.hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }
}