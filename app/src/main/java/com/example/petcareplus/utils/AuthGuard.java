package com.example.petcareplus.utils;

import android.app.Activity;
import android.content.Intent;

import com.example.petcareplus.activities.EmailVerificationActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * One place that decides "is this person allowed inside the app?".
 *
 * Allowed:  guests (anonymous), verified email accounts, Google accounts.
 * Blocked:  email/password accounts whose email is NOT verified yet.
 */
public final class AuthGuard {

    /** Screens an unverified user may still see (login, sign-up, verification...). */
    private static final Set<String> OPEN_SCREENS = new HashSet<>(Arrays.asList(
            "SplashActivity",
            "OnboardingActivity",
            "LoginActivity",
            "SignupActivity",
            "EmailVerificationActivity",
            "PolicyViewerActivity",
            "LanguageActivity"
    ));

    private AuthGuard() {
    }

    public static boolean isAllowedIn(FirebaseUser user) {
        if (user == null) return false;
        if (user.isAnonymous()) return true;
        if (user.isEmailVerified()) return true;

        for (UserInfo info : user.getProviderData()) {
            if ("google.com".equals(info.getProviderId())) return true;
        }
        return false;
    }

    /**
     * Called for every screen of the app (see PetCareApp). If the signed-in user is not
     * verified and this is not one of the open screens, the whole back stack is cleared
     * and the verification screen is shown instead - so Back can never leak into the app.
     */
    public static void enforce(Activity activity) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // not signed in -> the existing login flow handles that
        if (user == null || isAllowedIn(user)) return;

        // only our own screens (ignore uCrop, Google sign-in, camera...)
        if (!activity.getClass().getName().startsWith(activity.getPackageName())) return;

        if (OPEN_SCREENS.contains(activity.getClass().getSimpleName())) return;

        Intent intent = new Intent(activity, EmailVerificationActivity.class);
        intent.putExtra("email", user.getEmail());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}