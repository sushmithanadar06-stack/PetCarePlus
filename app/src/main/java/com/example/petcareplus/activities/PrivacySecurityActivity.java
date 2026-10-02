package com.example.petcareplus.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.petcareplus.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class PrivacySecurityActivity extends AppCompatActivity {

    private TextView btnPrivacyBack;
    private CardView cardChangePassword, cardPrivacyPolicy, cardTerms;
    private Button btnDeleteAccount;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_security);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnPrivacyBack = findViewById(R.id.btnPrivacyBack);
        cardChangePassword = findViewById(R.id.cardChangePassword);
        cardPrivacyPolicy = findViewById(R.id.cardPrivacyPolicy);
        cardTerms = findViewById(R.id.cardTerms);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);

        btnPrivacyBack.setOnClickListener(v -> finish());

        cardChangePassword.setOnClickListener(v -> sendPasswordReset());

        cardPrivacyPolicy.setOnClickListener(v -> {
            Intent intent = new Intent(PrivacySecurityActivity.this, PolicyViewerActivity.class);
            intent.putExtra(PolicyViewerActivity.EXTRA_TYPE, "privacy");
            startActivity(intent);
        });

        cardTerms.setOnClickListener(v -> {
            Intent intent = new Intent(PrivacySecurityActivity.this, PolicyViewerActivity.class);
            intent.putExtra(PolicyViewerActivity.EXTRA_TYPE, "terms");
            startActivity(intent);
        });

        btnDeleteAccount.setOnClickListener(v -> confirmDeleteAccount());
    }

    private void sendPasswordReset() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null || user.isAnonymous() || user.getEmail() == null) {
            Toast.makeText(this, "Guest accounts don't have a password to reset", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.sendPasswordResetEmail(user.getEmail())
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Reset link sent to " + user.getEmail(), Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Delete your account?")
                .setMessage("This permanently deletes your account and all your pets' data, including health records and documents. This cannot be undone.")
                .setPositiveButton("Delete Everything", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        btnDeleteAccount.setEnabled(false);
        String uid = user.getUid();

        db.collection("pets")
                .whereEqualTo("ownerId", uid)
                .get()
                .addOnSuccessListener(petsSnapshot -> {

                    for (QueryDocumentSnapshot petDoc : petsSnapshot) {
                        petDoc.getReference().delete();
                    }

                    db.collection("users").document(uid).delete()
                            .addOnCompleteListener(unused -> finishAccountDeletion(user));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnDeleteAccount.setEnabled(true);
                });
    }

    private void finishAccountDeletion(FirebaseUser user) {

        user.delete()
                .addOnSuccessListener(unused -> {

                    Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(PrivacySecurityActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(this,
                            "Data deleted, but account removal failed: " + e.getMessage() +
                                    " — you may need to log in again first before deleting.",
                            Toast.LENGTH_LONG).show();

                    btnDeleteAccount.setEnabled(true);
                });
    }
}