package com.example.petcareplus.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.petcareplus.R;
import com.example.petcareplus.models.Pet;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MedicalRecordsHubActivity extends AppCompatActivity {

    private TextView btnHubBack;
    private CardView cardVaccination, cardMedication, cardDeworming, cardVetVisit, cardDocuments;
    private TextView txtVaccinationSummary, txtMedicationSummary, txtDewormingSummary, txtVetVisitSummary, txtDocumentsSummary;

    private LinearLayoutHolder navBar;
    private FirebaseFirestore db;
    private Pet pet;

    private static class LinearLayoutHolder {
        android.widget.LinearLayout home, records, rescue, community, profile;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_records_hub);

        db = FirebaseFirestore.getInstance();

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            pet = getIntent().getSerializableExtra("pet", Pet.class);
        } else {
            pet = (Pet) getIntent().getSerializableExtra("pet");
        }

        if (pet == null) {
            Toast.makeText(this, "Unable to load pet", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnHubBack = findViewById(R.id.btnHubBack);
        cardVaccination = findViewById(R.id.cardVaccination);
        cardMedication = findViewById(R.id.cardMedication);
        cardDeworming = findViewById(R.id.cardDeworming);
        cardVetVisit = findViewById(R.id.cardVetVisit);
        cardDocuments = findViewById(R.id.cardDocuments);

        txtVaccinationSummary = findViewById(R.id.txtVaccinationSummary);
        txtMedicationSummary = findViewById(R.id.txtMedicationSummary);
        txtDewormingSummary = findViewById(R.id.txtDewormingSummary);
        txtVetVisitSummary = findViewById(R.id.txtVetVisitSummary);
        txtDocumentsSummary = findViewById(R.id.txtDocumentsSummary);

        navBar = new LinearLayoutHolder();
        navBar.home = findViewById(R.id.navBarHome);
        navBar.records = findViewById(R.id.navBarRecords);
        navBar.rescue = findViewById(R.id.navBarRescue);
        navBar.community = findViewById(R.id.navBarCommunity);
        navBar.profile = findViewById(R.id.navBarProfile);

        btnHubBack.setOnClickListener(v -> finish());

        cardVaccination.setOnClickListener(v -> openCategory("vaccination"));
        cardMedication.setOnClickListener(v -> openCategory("medication"));
        cardDeworming.setOnClickListener(v -> openCategory("deworming"));
        cardVetVisit.setOnClickListener(v -> openCategory("vetvisit"));

        cardDocuments.setOnClickListener(v -> {
            Intent intent = new Intent(MedicalRecordsHubActivity.this, DocumentsActivity.class);
            intent.putExtra("pet", pet);
            startActivity(intent);
        });

        setupBottomNav();
    }

    private void setupBottomNav() {
        navBar.home.setOnClickListener(v -> {
            Intent intent = new Intent(MedicalRecordsHubActivity.this, PetListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
        navBar.records.setOnClickListener(v -> {}); // already here
        navBar.rescue.setOnClickListener(v ->
                Toast.makeText(this, "Coming in a future module", Toast.LENGTH_SHORT).show());
        navBar.community.setOnClickListener(v ->
                Toast.makeText(this, "Coming in a future module", Toast.LENGTH_SHORT).show());
        navBar.profile.setOnClickListener(v ->
                startActivity(new Intent(MedicalRecordsHubActivity.this, ProfileActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSummaries();
    }

    private void openCategory(String category) {
        Intent intent = new Intent(MedicalRecordsHubActivity.this, HealthRecordListActivity.class);
        intent.putExtra(HealthRecordListActivity.EXTRA_CATEGORY, category);
        intent.putExtra("pet", pet);
        startActivity(intent);
    }

    private void loadSummaries() {

        db.collection("pets").document(pet.getId()).collection("healthRecords")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    int vacc = 0, med = 0, deworm = 0, vet = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String cat = doc.getString("category");
                        if (cat == null) continue;
                        switch (cat) {
                            case "vaccination": vacc++; break;
                            case "medication": med++; break;
                            case "deworming": deworm++; break;
                            case "vetvisit": vet++; break;
                        }
                    }

                    txtVaccinationSummary.setText(vacc == 0 ? "No records yet" : vacc + " record" + (vacc == 1 ? "" : "s"));
                    txtMedicationSummary.setText(med == 0 ? "No records yet" : med + " record" + (med == 1 ? "" : "s"));
                    txtDewormingSummary.setText(deworm == 0 ? "No records yet" : deworm + " record" + (deworm == 1 ? "" : "s"));
                    txtVetVisitSummary.setText(vet == 0 ? "No visits logged" : vet + " visit" + (vet == 1 ? "" : "s") + " logged");
                })
                .addOnFailureListener(e -> {});

        db.collection("pets").document(pet.getId()).collection("documents")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    txtDocumentsSummary.setText(count == 0 ? "No files uploaded" : count + " file" + (count == 1 ? "" : "s") + " uploaded");
                })
                .addOnFailureListener(e -> {});
    }
}