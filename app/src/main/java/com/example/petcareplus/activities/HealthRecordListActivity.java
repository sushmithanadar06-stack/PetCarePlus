package com.example.petcareplus.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.models.HealthRecord;
import com.example.petcareplus.models.Pet;
import com.example.petcareplus.utils.HealthRecordStatus;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;

public class HealthRecordListActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY = "category";

    private TextView btnListBack, txtListTitle, btnAddRecord, txtNoRecords;
    private LinearLayout recordsListContainer;
    private LinearLayout navBarHome, navBarRecords, navBarRescue, navBarCommunity, navBarProfile;

    private String category;
    private Pet pet;
    private final ArrayList<HealthRecord> records = new ArrayList<>();
    private FirebaseFirestore db;

    private long selectedDate = 0;
    private long selectedMedStartDate = 0;
    private long selectedVetDate = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_record_list);

        db = FirebaseFirestore.getInstance();

        btnListBack = findViewById(R.id.btnListBack);
        txtListTitle = findViewById(R.id.txtListTitle);
        btnAddRecord = findViewById(R.id.btnAddRecord);
        recordsListContainer = findViewById(R.id.recordsListContainer);
        txtNoRecords = findViewById(R.id.txtNoRecords);

        navBarHome = findViewById(R.id.navBarHome);
        navBarRecords = findViewById(R.id.navBarRecords);
        navBarRescue = findViewById(R.id.navBarRescue);
        navBarCommunity = findViewById(R.id.navBarCommunity);
        navBarProfile = findViewById(R.id.navBarProfile);

        category = getIntent().getStringExtra(EXTRA_CATEGORY);

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            pet = getIntent().getSerializableExtra("pet", Pet.class);
        } else {
            pet = (Pet) getIntent().getSerializableExtra("pet");
        }

        if (category == null || pet == null) {
            Toast.makeText(this, "Unable to load", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        txtListTitle.setText(getCategoryTitle(category));

        btnListBack.setOnClickListener(v -> finish());
        btnAddRecord.setOnClickListener(v -> {
            if ("medication".equals(category)) {
                showAddMedicationDialog();
            } else if ("vetvisit".equals(category)) {
                showAddVetVisitDialog();
            } else {
                showAddDialog();
            }
        });

        setupBottomNav();
        loadRecords();
    }

    private CollectionReference recordsCollection() {
        return db.collection("pets").document(pet.getId()).collection("healthRecords");
    }

    private void setupBottomNav() {

        navBarHome.setOnClickListener(v -> {
            Intent intent = new Intent(HealthRecordListActivity.this, PetListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        navBarRecords.setOnClickListener(v -> {
            Intent intent = new Intent(HealthRecordListActivity.this, MedicalRecordsHubActivity.class);
            intent.putExtra("pet", pet);
            startActivity(intent);
            finish();
        });

        navBarRescue.setOnClickListener(v -> Toast.makeText(this, "Coming in a future module", Toast.LENGTH_SHORT).show());
        navBarCommunity.setOnClickListener(v -> Toast.makeText(this, "Coming in a future module", Toast.LENGTH_SHORT).show());

        navBarProfile.setOnClickListener(v ->
                startActivity(new Intent(HealthRecordListActivity.this, ProfileActivity.class))
        );
    }

    private String getCategoryTitle(String cat) {
        switch (cat) {
            case "vaccination": return "Vaccination Records";
            case "medication": return "Medication Records";
            case "deworming": return "Deworming Records";
            case "vetvisit": return "Vet Visit History";
            default: return "Records";
        }
    }

    private String getCategoryIcon(String cat) {
        switch (cat) {
            case "vaccination": return "💉";
            case "medication": return "💊";
            case "deworming": return "🪱";
            case "vetvisit": return "🩺";
            default: return "📋";
        }
    }

    private void loadRecords() {
        recordsCollection()
                .whereEqualTo("category", category)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    records.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        HealthRecord r = doc.toObject(HealthRecord.class);
                        r.setId(doc.getId());
                        records.add(r);
                    }
                    Collections.sort(records, (a, b) -> Long.compare(b.getDateMillis(), a.getDateMillis()));
                    renderList();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void renderList() {

        recordsListContainer.removeAllViews();

        if (records.isEmpty()) {
            txtNoRecords.setVisibility(View.VISIBLE);
            return;
        }

        txtNoRecords.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        String icon = getCategoryIcon(category);
        boolean isMedication = "medication".equals(category);
        boolean isVetVisit = "vetvisit".equals(category);

        for (HealthRecord record : records) {

            View row = inflater.inflate(R.layout.item_health_record, recordsListContainer, false);

            TextView txtRecordIcon = row.findViewById(R.id.txtRecordIcon);
            TextView txtRecordTitle = row.findViewById(R.id.txtRecordTitle);
            TextView txtRecordSubtitle = row.findViewById(R.id.txtRecordSubtitle);
            TextView txtRecordBadge = row.findViewById(R.id.txtRecordBadge);

            txtRecordIcon.setText(icon);
            txtRecordTitle.setText(record.getTitle());

            if (isMedication) {
                String statusLine = HealthRecordStatus.medicationSubtitle(record);
                String subtitle = record.getSubtitle() != null && !record.getSubtitle().isEmpty()
                        ? record.getSubtitle() + (statusLine.isEmpty() ? "" : " · " + statusLine)
                        : statusLine;
                txtRecordSubtitle.setText(subtitle);
            } else {
                String dateStr = DateFormat.format("dd MMM yyyy", record.getDateMillis()).toString();
                String subtitle = record.getSubtitle() != null && !record.getSubtitle().isEmpty()
                        ? record.getSubtitle() + " · " + dateStr
                        : dateStr;
                txtRecordSubtitle.setText(subtitle);
            }

            HealthRecordStatus.Badge badge = HealthRecordStatus.compute(record);
            if (badge != null) {
                txtRecordBadge.setVisibility(View.VISIBLE);
                txtRecordBadge.setText(badge.label);
                txtRecordBadge.getBackground().setTint(android.graphics.Color.parseColor(badge.colorHex));
            } else {
                txtRecordBadge.setVisibility(View.GONE);
            }

            if (isVetVisit) {
                row.setOnClickListener(v -> {
                    Intent intent = new Intent(HealthRecordListActivity.this, LocationDetailActivity.class);
                    intent.putExtra(LocationDetailActivity.EXTRA_TITLE, record.getTitle());
                    intent.putExtra(LocationDetailActivity.EXTRA_ADDRESS, record.getAddress());
                    intent.putExtra(LocationDetailActivity.EXTRA_HOURS, record.getHours());
                    intent.putExtra(LocationDetailActivity.EXTRA_PHONE, record.getPhone());
                    startActivity(intent);
                });
            }

            row.setOnLongClickListener(v -> {
                showViewDeleteMenu(record);
                return true;
            });

            recordsListContainer.addView(row);
        }
    }

    private void showViewDeleteMenu(HealthRecord record) {

        String[] options = {"View", "Delete"};

        new AlertDialog.Builder(this)
                .setTitle(record.getTitle())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showViewDialog(record);
                    } else {
                        confirmDelete(record);
                    }
                })
                .show();
    }

    private void showViewDialog(HealthRecord record) {

        StringBuilder message = new StringBuilder();

        if ("medication".equals(category)) {
            String dateStr = DateFormat.format("dd MMM yyyy", record.getDateMillis()).toString();
            message.append("Started: ").append(dateStr);
            if (record.getNextDueMillis() > 0) {
                String endStr = DateFormat.format("dd MMM yyyy", record.getNextDueMillis()).toString();
                message.append("\nEnds: ").append(endStr);
            }
            if (record.getSubtitle() != null && !record.getSubtitle().isEmpty()) {
                message.append("\nNotes: ").append(record.getSubtitle());
            }
            message.append("\nStatus: ").append(HealthRecordStatus.medicationSubtitle(record));
        } else if ("vetvisit".equals(category)) {
            String dateStr = DateFormat.format("dd MMM yyyy", record.getDateMillis()).toString();
            message.append("Visited: ").append(dateStr);
            if (record.getAddress() != null && !record.getAddress().isEmpty()) {
                message.append("\nAddress: ").append(record.getAddress());
            }
            if (record.getPhone() != null && !record.getPhone().isEmpty()) {
                message.append("\nContact: ").append(record.getPhone());
            }
        } else {
            String dateStr = DateFormat.format("dd MMM yyyy", record.getDateMillis()).toString();
            message.append("Date: ").append(dateStr);
            if (record.getSubtitle() != null && !record.getSubtitle().isEmpty()) {
                message.append("\nNotes: ").append(record.getSubtitle());
            }
        }

        HealthRecordStatus.Badge badge = HealthRecordStatus.compute(record);
        if (badge != null) {
            message.append("\nStatus: ").append(badge.label);
        }

        new AlertDialog.Builder(this)
                .setTitle(record.getTitle())
                .setMessage(message.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    private void confirmDelete(HealthRecord record) {

        new AlertDialog.Builder(this)
                .setTitle("Delete this record?")
                .setMessage("This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    recordsCollection().document(record.getId())
                            .delete()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show();
                                loadRecords();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ================= STANDARD DIALOG (Vaccination / Deworming) =================

    private void showAddDialog() {

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_health_record, null);

        EditText etTitle = dialogView.findViewById(R.id.etRecordTitle);
        EditText etSubtitle = dialogView.findViewById(R.id.etRecordSubtitle);
        Button btnPickDate = dialogView.findViewById(R.id.btnPickDate);

        selectedDate = 0;

        btnPickDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date").build();
            picker.addOnPositiveButtonClickListener(sel -> {
                selectedDate = sel;
                btnPickDate.setText(DateFormat.format("dd MMM yyyy", sel));
            });
            picker.show(getSupportFragmentManager(), "date");
        });

        new AlertDialog.Builder(this)
                .setTitle("Add " + getCategoryTitle(category).replace(" Records", ""))
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {

                    String title = etTitle.getText().toString().trim();
                    String subtitle = etSubtitle.getText().toString().trim();

                    if (title.isEmpty()) {
                        Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (selectedDate == 0) {
                        Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveRecord(title, subtitle, selectedDate, 0, null, null, null);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ================= MEDICATION DIALOG =================

    private void showAddMedicationDialog() {

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_medication, null);

        EditText etTitle = dialogView.findViewById(R.id.etMedTitle);
        EditText etNotes = dialogView.findViewById(R.id.etMedNotes);
        Button btnStartDate = dialogView.findViewById(R.id.btnMedStartDate);
        EditText etDays = dialogView.findViewById(R.id.etMedDays);

        selectedMedStartDate = 0;

        btnStartDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Start Date").build();
            picker.addOnPositiveButtonClickListener(sel -> {
                selectedMedStartDate = sel;
                btnStartDate.setText(DateFormat.format("dd MMM yyyy", sel));
            });
            picker.show(getSupportFragmentManager(), "medStartDate");
        });

        new AlertDialog.Builder(this)
                .setTitle("Add Medication")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {

                    String title = etTitle.getText().toString().trim();
                    String notes = etNotes.getText().toString().trim();
                    String daysStr = etDays.getText().toString().trim();

                    if (title.isEmpty()) {
                        Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (selectedMedStartDate == 0) {
                        Toast.makeText(this, "Please select a start date", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (daysStr.isEmpty()) {
                        Toast.makeText(this, "Please enter number of days", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int days;
                    try {
                        days = Integer.parseInt(daysStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Enter a valid number of days", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (days <= 0) {
                        Toast.makeText(this, "Days must be greater than 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long endMillis = selectedMedStartDate + ((long) days * 24 * 60 * 60 * 1000);

                    saveRecord(title, notes, selectedMedStartDate, endMillis, null, null, null);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ================= VET VISIT DIALOG =================

    private void showAddVetVisitDialog() {

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_vet_visit, null);

        EditText etTitle = dialogView.findViewById(R.id.etVetTitle);
        EditText etAddress = dialogView.findViewById(R.id.etVetAddress);
        EditText etHours = dialogView.findViewById(R.id.etVetHours);
        EditText etPhone = dialogView.findViewById(R.id.etVetPhone);
        Button btnDate = dialogView.findViewById(R.id.btnVetDate);

        selectedVetDate = 0;

        btnDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Visit Date").build();
            picker.addOnPositiveButtonClickListener(sel -> {
                selectedVetDate = sel;
                btnDate.setText(DateFormat.format("dd MMM yyyy", sel));
            });
            picker.show(getSupportFragmentManager(), "vetDate");
        });

        new AlertDialog.Builder(this)
                .setTitle("Log Vet Visit")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {

                    String title = etTitle.getText().toString().trim();
                    String address = etAddress.getText().toString().trim();
                    String hours = etHours.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();

                    if (title.isEmpty()) {
                        Toast.makeText(this, "Please enter doctor / clinic name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (address.isEmpty()) {
                        Toast.makeText(this, "Please enter the clinic address", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (selectedVetDate == 0) {
                        Toast.makeText(this, "Please select a visit date", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveRecord(title, "", selectedVetDate, 0, address, hours, phone);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ================= SHARED SAVE =================

    private void saveRecord(String title, String subtitle, long dateMillis, long nextDueMillis,
                            String address, String hours, String phone) {

        try {
            String id = recordsCollection().document().getId();

            HealthRecord record = new HealthRecord();
            record.setId(id);
            record.setPetId(pet.getId());
            record.setCategory(category);
            record.setTitle(title);
            record.setSubtitle(subtitle);
            record.setDateMillis(dateMillis);
            record.setNextDueMillis(nextDueMillis);
            record.setNotes("");
            record.setAddress(address);
            record.setHours(hours);
            record.setPhone(phone);

            recordsCollection().document(id)
                    .set(record)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Record saved", Toast.LENGTH_SHORT).show();
                        loadRecords();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());

        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}