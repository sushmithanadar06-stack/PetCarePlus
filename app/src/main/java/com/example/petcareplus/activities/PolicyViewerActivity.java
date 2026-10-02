package com.example.petcareplus.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;

public class PolicyViewerActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE = "type"; // "privacy" or "terms"

    private static final String PRIVACY_POLICY =
            "PRIVACY POLICY — PetCare+\n\n" +
                    "Last updated: September 2026\n\n" +
                    "1. Who We Are\n\n" +
                    "PetCare+ is a pet care and health tracking application. If you have questions about this Privacy Policy or how we handle your data, contact us at petcareplus.support@gmail.com.\n\n" +
                    "2. Data We Collect\n\n" +
                    "Depending on how you use PetCare+, we may collect:\n" +
                    "• Email address and account information required to create and manage an account\n" +
                    "• Authentication information processed through Firebase Authentication\n" +
                    "• Pet photos and documents voluntarily uploaded by you\n" +
                    "• Pet health and care information, including vaccinations, medications, vet visits, weight records, and wellness entries you choose to log\n\n" +
                    "3. How We Use Your Data\n\n" +
                    "Your data is used solely to provide the app's features to you — storing and displaying your pets' profiles, health records, and wellness data. We do not sell your data to third parties.\n\n" +
                    "4. Guest Mode\n\n" +
                    "If you use Guest Mode, your data is stored under a temporary anonymous account and is not linked to any email address. Uninstalling the app or logging out may result in permanent loss of guest data.\n\n" +
                    "5. Data Storage\n\n" +
                    "Your data is stored using Google Firebase (Firestore and Authentication), which applies industry-standard security practices.\n\n" +
                    "6. Your Rights\n\n" +
                    "You may request deletion of your account and all associated data at any time via Settings → Privacy & Security → Delete My Account, or by contacting us directly.\n\n" +
                    "7. Contact\n\n" +
                    "Email: petcareplusapp@gmail.com";

    private static final String TERMS_OF_SERVICE =
            "TERMS OF SERVICE — PetCare+\n\n" +
                    "Last updated: September 2026\n\n" +
                    "1. Acceptance of Terms\n\n" +
                    "By using PetCare+, you agree to these Terms of Service. If you do not agree, please do not use the app.\n\n" +
                    "2. Description of Service\n\n" +
                    "PetCare+ helps you organize information relating to your pets, including profiles, health records, vaccinations, medications, vet visits, weight tracking, and documents.\n\n" +
                    "3. User Accounts\n\n" +
                    "You are responsible for maintaining the confidentiality of your account credentials. Guest accounts are temporary and not linked to any email.\n\n" +
                    "4. User Content\n\n" +
                    "You retain ownership of any photos, documents, or information you upload. By uploading content, you confirm you have the right to do so.\n\n" +
                    "5. Limitation of Liability\n\n" +
                    "PetCare+ is provided for informational and organizational purposes only. It is not a substitute for professional veterinary advice, diagnosis, or treatment.\n\n" +
                    "6. Changes to These Terms\n\n" +
                    "We may update these Terms from time to time. Continued use of the app after changes constitutes acceptance.\n\n" +
                    "7. Contact\n\n" +
                    "Email: petcareplusapp@gmail.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_policy_viewer);

        TextView btnPolicyBack = findViewById(R.id.btnPolicyBack);
        TextView txtPolicyTitle = findViewById(R.id.txtPolicyTitle);
        TextView txtPolicyBody = findViewById(R.id.txtPolicyBody);

        String type = getIntent().getStringExtra(EXTRA_TYPE);

        if ("terms".equals(type)) {
            txtPolicyTitle.setText("Terms of Service");
            txtPolicyBody.setText(TERMS_OF_SERVICE);
        } else {
            txtPolicyTitle.setText("Privacy Policy");
            txtPolicyBody.setText(PRIVACY_POLICY);
        }

        btnPolicyBack.setOnClickListener(v -> finish());
    }
}