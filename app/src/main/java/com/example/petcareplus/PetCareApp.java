package com.example.petcareplus;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.petcareplus.utils.AuthGuard;
import com.example.petcareplus.utils.NotificationWatcher;
import com.example.petcareplus.utils.NotificationsHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * - Runs the email-verification check on EVERY screen (AuthGuard).
 * - Starts the live notification watcher (phone alerts while the app is running).
 * - Marks a notification as read when a screen is opened from its phone alert.
 * - FIX: forces a fresh login token whenever a verified account is detected, so
 *   Firestore security rules (which read the OLD verified-status baked into the
 *   token at login time, not the live value) don't keep rejecting writes from an
 *   account that verified its email after that token was issued.
 *
 * Registered in AndroidManifest.xml with android:name=".PetCareApp".
 */
public class PetCareApp extends Application {

    /** Only force the refresh once per account per app run - it's a network call. */
    private String lastRefreshedUid = null;

    @Override
    public void onCreate() {
        super.onCreate();

        NotificationWatcher.init(this);
        NotificationsHelper.init(this);

        // FIX: Firestore rules check request.auth.token.email_verified, which is
        // frozen into the login token (JWT) at the moment it was issued - it does
        // NOT update just because user.reload() shows isEmailVerified() = true
        // locally. Without forcing a fresh token, a genuinely-verified account can
        // still get PERMISSION_DENIED on every write until its token naturally
        // expires and refreshes (up to an hour later, or on next full login).
        FirebaseAuth.getInstance().addAuthStateListener(auth -> {

            FirebaseUser user = auth.getCurrentUser();
            if (user == null || user.isAnonymous() || !user.isEmailVerified()) return;

            if (user.getUid().equals(lastRefreshedUid)) return; // already done this run
            lastRefreshedUid = user.getUid();

            user.getIdToken(true); // forces a fresh token with the current verified status
        });

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                if (activity.getIntent() != null) {
                    String notificationId = activity.getIntent().getStringExtra("notificationId");
                    if (notificationId != null) {
                        NotificationsHelper.markRead(FirebaseFirestore.getInstance(), notificationId);
                    }
                }
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                AuthGuard.enforce(activity);
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                AuthGuard.enforce(activity);
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });
    }
}