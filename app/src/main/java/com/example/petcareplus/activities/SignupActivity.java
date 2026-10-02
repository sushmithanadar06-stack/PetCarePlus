package com.example.petcareplus.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private EditText etSignupName, etSignupEmail, etSignupPhone, etSignupState, etSignupPassword;
    private Button btnSignup;
    private TextView txtGoToLogin;
    private TextView btnSignupBack;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etSignupName = findViewById(R.id.etSignupName);
        etSignupEmail = findViewById(R.id.etSignupEmail);
        etSignupPhone = findViewById(R.id.etSignupPhone);
        etSignupState = findViewById(R.id.etSignupState);
        etSignupPassword = findViewById(R.id.etSignupPassword);
        btnSignup = findViewById(R.id.btnSignup);
        txtGoToLogin = findViewById(R.id.txtGoToLogin);
        btnSignupBack = findViewById(R.id.btnSignupBack);

        btnSignupBack.setOnClickListener(v -> finish());

        btnSignup.setOnClickListener(v -> doSignup());

        txtGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void doSignup() {

        String name = etSignupName.getText().toString().trim();
        String email = etSignupEmail.getText().toString().trim();
        String phone = etSignupPhone.getText().toString().trim();
        String state = etSignupState.getText().toString().trim();
        String password = etSignupPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etSignupName.setError("Please enter your name");
            etSignupName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etSignupEmail.setError("Please enter your email");
            etSignupEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            etSignupPhone.setError("Please enter your phone number");
            etSignupPhone.requestFocus();
            return;
        }

        if (phone.length() != 10) {
            etSignupPhone.setError("Enter a valid 10-digit phone number");
            etSignupPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(state)) {
            etSignupState.setError("Please enter your state");
            etSignupState.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etSignupPassword.setError("Password must be at least 6 characters");
            etSignupPassword.requestFocus();
            return;
        }

        final String finalPhone = phone;
        final String finalState = state;

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {

                    if (result.getUser() != null) {

                        UserProfileChangeRequest profileUpdate =
                                new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build();

                        result.getUser().updateProfile(profileUpdate);

                        Map<String, Object> userProfile = new HashMap<>();
                        userProfile.put("name", name);
                        userProfile.put("phone", "+91" + finalPhone);
                        userProfile.put("state", finalState);

                        db.collection("users")
                                .document(result.getUser().getUid())
                                .set(userProfile);

                        result.getUser().sendEmailVerification()
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(SignupActivity.this,
                                            "Account created — check your email to verify",
                                            Toast.LENGTH_LONG).show();
                                    startActivity(new Intent(SignupActivity.this, EmailVerificationActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    // Account exists but verification email failed to send — still route to the verify screen so they can retry there
                                    startActivity(new Intent(SignupActivity.this, EmailVerificationActivity.class));
                                    finish();
                                });

                    } else {
                        startActivity(new Intent(SignupActivity.this, EmailVerificationActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(SignupActivity.this,
                                "Sign up failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }
}