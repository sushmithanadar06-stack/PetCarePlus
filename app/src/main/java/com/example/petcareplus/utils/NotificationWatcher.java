package com.example.petcareplus.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.app.NotificationChannel;
import android.media.AudioAttributes;
import android.net.Uri;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.petcareplus.R;
import com.example.petcareplus.models.AppNotification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * Shows a phone notification whenever a NEW notification arrives while the app is running
 * (open or in the background). Started once from PetCareApp.
 *
 * (Getting alerts while the app is completely closed needs Firebase Cloud Functions +
 * push messages, which is a paid Firebase feature - not used here.)
 */
public final class NotificationWatcher {

    private static final String CHANNEL_ID = "petcare_updates_v2";

    private static ListenerRegistration registration;
    private static String watchingUid;

    private NotificationWatcher() {
    }

    public static void init(Application app) {

        createChannel(app);

        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {

            FirebaseUser user = firebaseAuth.getCurrentUser();

            // only verified people / guests get notifications (same rule as the rest of the app)
            if (user == null || !AuthGuard.isAllowedIn(user)) {
                stop();
                return;
            }

            start(app, user.getUid());
        });
    }

    private static void start(Application app, String uid) {

        if (uid.equals(watchingUid) && registration != null) return;

        stop();
        watchingUid = uid;

        // ignore what is already there; only alert for notifications that arrive from now on
        final boolean[] synced = {false};

        registration = FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("recipientId", uid)
                .whereEqualTo("read", false)
                .addSnapshotListener((snap, e) -> {

                    if (e != null || snap == null) return;

                    if (!synced[0]) {
                        if (!snap.getMetadata().isFromCache()) synced[0] = true;
                        return;
                    }

                    for (DocumentChange change : snap.getDocumentChanges()) {
                        if (change.getType() == DocumentChange.Type.ADDED) {
                            DocumentSnapshot doc = change.getDocument();
                            AppNotification n = doc.toObject(AppNotification.class);
                            if (n != null) {
                                n.setId(doc.getId());
                                show(app, n);
                            }
                        }
                    }
                });
    }

    private static void stop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        watchingUid = null;
    }

    private static void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "PetCare+ updates", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Rescue and adoption updates");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 250, 100, 250});

            Uri sound = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            channel.setSound(sound, attrs);

            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    @SuppressLint("MissingPermission")
    private static void show(Context ctx, AppNotification n) {

        // DIAGNOSTIC: check Logcat filter "NotifPopup" if a popup still doesn't show.
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w("NotifPopup", "BLOCKED: system POST_NOTIFICATIONS permission not granted. "
                    + "Fix in Settings > Apps > PetCare+ > Notifications.");
            return;
        }

        boolean systemLevelEnabled = androidx.core.app.NotificationManagerCompat.from(ctx).areNotificationsEnabled();
        if (!systemLevelEnabled) {
            Log.w("NotifPopup", "BLOCKED: notifications are turned OFF for this app at the "
                    + "system level (Settings > Apps > PetCare+ > Notifications main switch is off).");
            return;
        }

        // The person can turn OFF the phone popup specifically while still getting
        // notifications inside the app (the Firestore doc + Notifications screen are
        // never affected by this switch - only whether a popup shows).
        SharedPreferences prefs = ctx.getSharedPreferences("petcare_notifications", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("push", true)) {
            Log.d("NotifPopup", "SKIPPED: person turned off the in-app push switch.");
            return;
        }

        Log.d("NotifPopup", "SHOWING popup for type=" + n.getType());

        Intent target = NotificationsHelper.targetIntent(ctx, n);
        target.putExtra("notificationId", n.getId());   // PetCareApp marks it read when opened
        target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int requestCode = n.getId() != null ? n.getId().hashCode() : (int) System.currentTimeMillis();

        PendingIntent pendingIntent = PendingIntent.getActivity(
                ctx, requestCode, target,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String message = NotificationsHelper.messageFor(n);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(NotificationsHelper.titleFor(n))
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(ctx).notify(requestCode, builder.build());
    }
}