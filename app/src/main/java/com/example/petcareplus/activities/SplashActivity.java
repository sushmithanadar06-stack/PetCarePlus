package com.example.petcareplus.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.utils.AuthGuard;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(this::decideNextScreen, SPLASH_DELAY_MS);
    }

    private void decideNextScreen() {

        SharedPreferences prefs = getSharedPreferences("petcare", MODE_PRIVATE);
        boolean onboardingSeen = prefs.getBoolean("onboarding_seen", false);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        Intent intent;

        if (!onboardingSeen) {
            intent = new Intent(SplashActivity.this, OnboardingActivity.class);
        } else if (user == null) {
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        } else if (AuthGuard.isAllowedIn(user)) {
            // guest, verified email, or Google account
            intent = new Intent(SplashActivity.this, PetListActivity.class);
        } else {
            // signed in but the email is NOT verified yet -> never go to the app
            intent = new Intent(SplashActivity.this, EmailVerificationActivity.class);
        }

        startActivity(intent);
        finish();
    }
}