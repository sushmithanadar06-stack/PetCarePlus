package com.example.petcareplus.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.utils.AuthGuard;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EmailVerificationActivity extends AppCompatActivity {

    private TextView txtVerifyEmailAddress, txtResendVerification, txtChangeEmail;
    private Button btnIveVerified;

    private FirebaseAuth auth;
    private FirebaseUser user;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable autoCheckRunnable;

    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private int cooldownRemaining = 0;
    private Runnable cooldownRunnable;

    /** Stops us from opening the app twice if two checks succeed at the same moment. */
    private boolean navigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        if (user == null) {
            goToLogin();
            return;
        }

        // already allowed in (verified / guest / Google) -> nothing to verify
        if (AuthGuard.isAllowedIn(user)) {
            goToMain();
            return;
        }

        txtVerifyEmailAddress = findViewById(R.id.txtVerifyEmailAddress);
        txtResendVerification = findViewById(R.id.txtResendVerification);
        txtChangeEmail = findViewById(R.id.txtChangeEmail);
        btnIveVerified = findViewById(R.id.btnIveVerified);

        txtVerifyEmailAddress.setText("We've sent a verification link to " + user.getEmail());

        // BACK must never lead into the app: leave the app instead.
        // (Opening the app again brings the person straight back to this screen.)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });

        btnIveVerified.setOnClickListener(v -> checkVerificationStatus(true));

        txtResendVerification.setOnClickListener(v -> resendVerificationEmail());

        txtChangeEmail.setOnClickListener(v -> {
            auth.signOut();
            goToLogin();
        });

        startAutoCheck();
    }

    private void startAutoCheck() {
        autoCheckRunnable = () -> {
            checkVerificationStatus(false);
            handler.postDelayed(autoCheckRunnable, 3000);
        };
        handler.postDelayed(autoCheckRunnable, 3000);
    }

    private void checkVerificationStatus(boolean showFeedback) {

        if (user == null || navigated) return;

        user.reload()
                .addOnSuccessListener(unused -> {
                    if (user.isEmailVerified()) {
                        onVerified();
                    } else if (showFeedback) {
                        Toast.makeText(this, "Not verified yet — check your inbox", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (showFeedback) {
                        Toast.makeText(this, "Couldn't check status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void onVerified() {

        if (navigated) return;
        navigated = true;

        if (autoCheckRunnable != null) handler.removeCallbacks(autoCheckRunnable);

        // Refresh the login token so the database also sees "email verified" right away.
        user.getIdToken(true).addOnCompleteListener(task -> {
            Toast.makeText(this, "Email verified!", Toast.LENGTH_SHORT).show();
            goToMain();
        });
    }

    private void resendVerificationEmail() {

        if (cooldownRemaining > 0) {
            Toast.makeText(this, "Please wait " + cooldownRemaining + "s before resending", Toast.LENGTH_SHORT).show();
            return;
        }

        if (user == null) return;

        user.sendEmailVerification()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Verification email resent", Toast.LENGTH_SHORT).show();
                    startResendCooldown();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to resend: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void startResendCooldown() {

        cooldownRemaining = RESEND_COOLDOWN_SECONDS;
        txtResendVerification.setEnabled(false);

        cooldownRunnable = new Runnable() {
            @Override
            public void run() {
                if (cooldownRemaining > 0) {
                    txtResendVerification.setText("Resend available in " + cooldownRemaining + "s");
                    cooldownRemaining--;
                    handler.postDelayed(this, 1000);
                } else {
                    txtResendVerification.setText("Resend verification email");
                    txtResendVerification.setEnabled(true);
                }
            }
        };
        handler.post(cooldownRunnable);
    }

    private void goToMain() {
        Intent intent = new Intent(EmailVerificationActivity.this, PetListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToLogin() {
        Intent intent = new Intent(EmailVerificationActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoCheckRunnable != null) handler.removeCallbacks(autoCheckRunnable);
        if (cooldownRunnable != null) handler.removeCallbacks(cooldownRunnable);
    }
}