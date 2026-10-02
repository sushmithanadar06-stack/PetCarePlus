package com.example.petcareplus.activities;

import android.content.Intent;
import android.text.InputType;
import android.text.TextUtils;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.utils.AuthGuard;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText etLoginEmail, etLoginPassword;
    private Button btnLogin, btnGuestLogin;
    private TextView txtGoToSignup, txtForgotPassword;
    private ImageView btnTogglePassword;

    private FirebaseAuth auth;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        FirebaseUser existing = auth.getCurrentUser();

        if (existing != null) {
            if (AuthGuard.isAllowedIn(existing)) {
                goToMain();
            } else {
                startActivity(new Intent(LoginActivity.this, EmailVerificationActivity.class));
            }
            finish();
            return;
        }

        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGuestLogin = findViewById(R.id.btnGuestLogin);
        txtGoToSignup = findViewById(R.id.txtGoToSignup);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);

        btnLogin.setOnClickListener(v -> doLogin());

        txtGoToSignup.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignupActivity.class))
        );

        txtForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        btnGuestLogin.setOnClickListener(v -> doGuestLogin());
    }

    private void togglePasswordVisibility() {

        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            etLoginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_eye_open);
        } else {
            etLoginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_eye_closed);
        }

        etLoginPassword.setSelection(etLoginPassword.getText().length());
    }

    private void doLogin() {

        String email = etLoginEmail.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etLoginEmail.setError("Please enter your email");
            etLoginEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etLoginPassword.setError("Please enter your password");
            etLoginPassword.requestFocus();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {

                    if (result.getUser() != null && !AuthGuard.isAllowedIn(result.getUser())) {
                        // email not verified yet -> verification screen, never the app
                        startActivity(new Intent(LoginActivity.this, EmailVerificationActivity.class));
                        finish();
                    } else {
                        goToMain();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(LoginActivity.this,
                                "Login failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void showForgotPasswordDialog() {

        EditText input = new EditText(this);
        input.setHint("Enter your email");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        String prefill = etLoginEmail.getText().toString().trim();
        if (!prefill.isEmpty()) input.setText(prefill);

        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your email and we'll send you a password reset link.")
                .setView(input)
                .setPositiveButton("Send", (dialog, which) -> sendResetEmail(input.getText().toString().trim(), true))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendResetEmail(String email, boolean allowResendPrompt) {

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter an email", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {

                    if (allowResendPrompt) {
                        showResendOption(email);
                    } else {
                        Toast.makeText(this, "Reset link sent again", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void showResendOption(String email) {

        new AlertDialog.Builder(this)
                .setTitle("Check your inbox")
                .setMessage("A reset link was sent to " + email + ". Didn't get it? Check spam, or resend below.")
                .setPositiveButton("Resend", (dialog, which) -> sendResetEmail(email, false))
                .setNegativeButton("Done", null)
                .show();
    }

    private void doGuestLogin() {

        auth.signInAnonymously()
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, "Continuing as guest", Toast.LENGTH_SHORT).show();
                    goToMain();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Guest login failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void goToMain() {
        startActivity(new Intent(LoginActivity.this, PetListActivity.class));
        finish();
    }
}