package com.example.petcareplus.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.petcareplus.R;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    private static final String SUPPORT_EMAIL = "petcareplusapp@gmail.com";

    private TextView btnSettingsBack;
    private CardView cardNotificationPrefs, cardPrivacy, cardLanguage, cardHelp, cardLogoutSettings;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        auth = FirebaseAuth.getInstance();

        btnSettingsBack = findViewById(R.id.btnSettingsBack);
        cardNotificationPrefs = findViewById(R.id.cardNotificationPrefs);
        cardPrivacy = findViewById(R.id.cardPrivacy);
        cardLanguage = findViewById(R.id.cardLanguage);
        cardHelp = findViewById(R.id.cardHelp);
        cardLogoutSettings = findViewById(R.id.cardLogoutSettings);

        btnSettingsBack.setOnClickListener(v -> finish());

        cardNotificationPrefs.setOnClickListener(v ->
                startActivity(new Intent(SettingsActivity.this, NotificationPreferencesActivity.class)));

        cardPrivacy.setOnClickListener(v ->
                startActivity(new Intent(SettingsActivity.this, PrivacySecurityActivity.class)));

        cardLanguage.setOnClickListener(v ->
                startActivity(new Intent(SettingsActivity.this, LanguageActivity.class)));

        cardHelp.setOnClickListener(v -> openSupportEmail());

        cardLogoutSettings.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void openSupportEmail() {

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + SUPPORT_EMAIL));
        intent.putExtra(Intent.EXTRA_SUBJECT, "PetCare+ Support Request");

        try {
            startActivity(Intent.createChooser(intent, "Send email via"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No email app found. Contact us at " + SUPPORT_EMAIL, Toast.LENGTH_LONG).show();
        }
    }

    private void showLogoutConfirmation() {

        new AlertDialog.Builder(this)
                .setTitle("Log out?")
                .setMessage("Are you sure you want to end your session?")
                .setPositiveButton("Log Out", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {

        auth.signOut();

        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}