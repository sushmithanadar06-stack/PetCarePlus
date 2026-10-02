package com.example.petcareplus.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import com.example.petcareplus.R;

public class NotificationPreferencesActivity extends AppCompatActivity {

    private TextView btnNotifBack;
    private Switch switchReminders, switchCommunity, switchRescue, switchPush;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_preferences);

        prefs = getSharedPreferences("petcare_notifications", MODE_PRIVATE);

        btnNotifBack = findViewById(R.id.btnNotifBack);
        switchReminders = findViewById(R.id.switchReminders);
        switchCommunity = findViewById(R.id.switchCommunity);
        switchRescue = findViewById(R.id.switchRescue);
        switchPush = findViewById(R.id.switchPush);

        switchReminders.setChecked(prefs.getBoolean("reminders", true));
        switchCommunity.setChecked(prefs.getBoolean("community", true));
        switchRescue.setChecked(prefs.getBoolean("rescue", true));

        btnNotifBack.setOnClickListener(v -> finish());

        switchReminders.setOnCheckedChangeListener((v, checked) ->
                prefs.edit().putBoolean("reminders", checked).apply());

        switchCommunity.setOnCheckedChangeListener((v, checked) ->
                prefs.edit().putBoolean("community", checked).apply());

        switchRescue.setOnCheckedChangeListener((v, checked) ->
                prefs.edit().putBoolean("rescue", checked).apply());

        switchPush.setOnCheckedChangeListener((v, checked) -> {

            if (checked && !systemNotificationsEnabled()) {
                // Flipping our own switch ON means nothing if Android itself still has
                // notifications blocked for the app - send the person straight to the
                // real system setting instead of just recording a flag that won't work.
                switchPush.setChecked(false); // don't claim it's on when it isn't
                showSystemSettingsPrompt();
                return;
            }

            prefs.edit().putBoolean("push", checked).apply();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reflect the REAL state every time this screen is shown (e.g. after coming back
        // from system Settings), not just what our own preference flag says.
        boolean systemOk = systemNotificationsEnabled();
        boolean wantsOn = prefs.getBoolean("push", true);
        switchPush.setChecked(systemOk && wantsOn);
    }

    private boolean systemNotificationsEnabled() {
        return NotificationManagerCompat.from(this).areNotificationsEnabled();
    }

    private void showSystemSettingsPrompt() {

        Toast.makeText(this,
                "Notifications are turned off for PetCare+ in your phone's Settings. "
                        + "Opening Settings so you can turn them on.",
                Toast.LENGTH_LONG).show();

        Intent intent;
        if (Build.VERSION.SDK_INT >= 26) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
        }

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't open settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}