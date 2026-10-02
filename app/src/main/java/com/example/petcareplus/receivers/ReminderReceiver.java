package com.example.petcareplus.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.petcareplus.R;
import com.example.petcareplus.activities.PetListActivity;
import com.example.petcareplus.utils.ReminderMessageUtils;
import com.example.petcareplus.utils.ReminderScheduler;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "petcare_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {

        String reminderId = intent.getStringExtra("reminderId");
        String petId = intent.getStringExtra("petId");
        String petName = intent.getStringExtra("petName");
        String title = intent.getStringExtra("title");
        String type = intent.getStringExtra("type");
        String repeatType = intent.getStringExtra("repeatType");

        if (petName == null || petName.trim().isEmpty()) {
            petName = "Your pet";
        }

        createChannelIfNeeded(context);
        showNotification(context, reminderId, petName, title, type);

        if (repeatType != null && !repeatType.equals("None") && reminderId != null && petId != null) {
            rescheduleNext(context, petId, petName, reminderId, title, type, repeatType);
        }
    }

    private void showNotification(Context context, String reminderId, String petName, String title, String type) {

        Intent openIntent = new Intent(context, PetListActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int notifId = reminderId != null ? reminderId.hashCode() : (int) System.currentTimeMillis();

        PendingIntent contentIntent = PendingIntent.getActivity(
                context, notifId, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String notifTitle = ReminderMessageUtils.buildTitle(type);
        String notifMessage = ReminderMessageUtils.buildMessage(petName, type);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(notifTitle)
                .setContentText(notifMessage)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notifMessage))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notifId, builder.build());
        }
    }

    private void rescheduleNext(Context context, String petId, String petName, String reminderId,
                                String title, String type, String repeatType) {

        Calendar cal = Calendar.getInstance();

        switch (repeatType) {
            case "Daily": cal.add(Calendar.DAY_OF_YEAR, 1); break;
            case "Weekly": cal.add(Calendar.WEEK_OF_YEAR, 1); break;
            case "Monthly": cal.add(Calendar.MONTH, 1); break;
            default: return;
        }

        long nextMillis = cal.getTimeInMillis();

        ReminderScheduler.scheduleAlarm(context, petId, petName, reminderId, title, type, nextMillis, repeatType);

        FirebaseFirestore.getInstance()
                .collection("pets").document(petId)
                .collection("reminders").document(reminderId)
                .update("dateTimeMillis", nextMillis);
    }

    private void createChannelIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, "Pet Reminders", NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Feeding, medication, grooming and vet reminders");
                manager.createNotificationChannel(channel);
            }
        }
    }
}