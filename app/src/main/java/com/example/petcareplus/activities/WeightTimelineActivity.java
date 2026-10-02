package com.example.petcareplus.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.models.Pet;
import com.example.petcareplus.models.WellnessEntry;
import com.example.petcareplus.utils.BreedWeightReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class WeightTimelineActivity extends AppCompatActivity {

    private TextView btnWeightBack;
    private TextView txtWeightVerdict, txtWeightVerdictDetail;
    private EditText etNewWeight;
    private Button btnSaveWeight;
    private LinearLayout weightHistoryList;
    private TextView txtNoHistory;

    private Pet pet;
    private FirebaseFirestore db;
    private final List<WellnessEntry> entries = new ArrayList<>();
    private WellnessEntry currentMonthEntry = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight_timeline);

        db = FirebaseFirestore.getInstance();

        btnWeightBack = findViewById(R.id.btnWeightBack);
        txtWeightVerdict = findViewById(R.id.txtWeightVerdict);
        txtWeightVerdictDetail = findViewById(R.id.txtWeightVerdictDetail);
        etNewWeight = findViewById(R.id.etNewWeight);
        btnSaveWeight = findViewById(R.id.btnSaveWeight);
        weightHistoryList = findViewById(R.id.weightHistoryList);
        txtNoHistory = findViewById(R.id.txtNoHistory);

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

        btnWeightBack.setOnClickListener(v -> finish());
        btnSaveWeight.setOnClickListener(v -> saveWeight());

        loadHistory();
    }

    private double getAgeInMonthsAt(long referenceMillis) {
        try {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

            Calendar birth = Calendar.getInstance();
            birth.setTime(sdf.parse(pet.getDateOfBirth()));

            Calendar reference = Calendar.getInstance();
            reference.setTimeInMillis(referenceMillis);

            int months =
                    (reference.get(Calendar.YEAR) - birth.get(Calendar.YEAR)) * 12
                            + (reference.get(Calendar.MONTH) - birth.get(Calendar.MONTH));

            if (reference.get(Calendar.DAY_OF_MONTH)
                    < birth.get(Calendar.DAY_OF_MONTH)) {
                months -= 1;
            }

            return Math.max(months, 0);

        } catch (Exception e) {
            return 12;
        }
    }

    private String monthKey(long millis) {
        return new SimpleDateFormat(
                "yyyy-MM",
                Locale.getDefault()
        ).format(new java.util.Date(millis));
    }

    private void updateVerdict(String weightStr, long asOfMillis) {

        if (weightStr == null || weightStr.trim().isEmpty()) {
            txtWeightVerdict.setText("Enter a weight to see status");
            txtWeightVerdictDetail.setText("");
            return;
        }

        double weightKg;

        try {
            weightKg = Double.parseDouble(weightStr.trim());
        } catch (NumberFormatException e) {
            txtWeightVerdict.setText("Enter a valid number");
            txtWeightVerdictDetail.setText("");
            return;
        }

        double ageMonths = getAgeInMonthsAt(asOfMillis);

        BreedWeightReference.WeightStatus status =
                BreedWeightReference.evaluate(
                        pet.getType(),
                        pet.getBreed(),
                        ageMonths,
                        weightKg
                );

        txtWeightVerdict.setText(status.verdict);

        txtWeightVerdictDetail.setText(
                String.format(
                        Locale.getDefault(),
                        "Target range for this age: %.1f – %.1f kg",
                        status.lowerBound,
                        status.upperBound
                )
        );
    }

    private void saveWeight() {

        String weightStr = etNewWeight.getText().toString().trim();

        if (weightStr.isEmpty()) {
            etNewWeight.setError("Enter a weight");
            etNewWeight.requestFocus();
            return;
        }

        double weightKg;

        try {
            weightKg = Double.parseDouble(weightStr);
        } catch (NumberFormatException e) {
            etNewWeight.setError("Enter a valid number");
            etNewWeight.requestFocus();
            return;
        }

        if (weightKg <= 0) {
            etNewWeight.setError("Weight must be greater than 0");
            etNewWeight.requestFocus();
            return;
        }

        btnSaveWeight.setEnabled(false);

        long now = System.currentTimeMillis();

        if (currentMonthEntry != null) {

            // Update this month's existing entry in place — one entry per month
            db.collection("pets")
                    .document(pet.getId())
                    .collection("wellnessEntries")
                    .document(currentMonthEntry.getId())
                    .update(
                            "value",
                            weightStr,
                            "timestampMillis",
                            now
                    )
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(
                                this,
                                "Weight updated",
                                Toast.LENGTH_SHORT
                        ).show();

                        updateVerdict(weightStr, now);
                        btnSaveWeight.setEnabled(true);
                        loadHistory();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(
                                this,
                                "Failed: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                        btnSaveWeight.setEnabled(true);
                    });

        } else {

            DocumentReference newRef =
                    db.collection("pets")
                            .document(pet.getId())
                            .collection("wellnessEntries")
                            .document();

            WellnessEntry entry = new WellnessEntry(
                    newRef.getId(),
                    pet.getId(),
                    "weight",
                    weightStr,
                    "",
                    now
            );

            newRef.set(entry)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(
                                this,
                                "Weight logged",
                                Toast.LENGTH_SHORT
                        ).show();

                        updateVerdict(weightStr, now);
                        btnSaveWeight.setEnabled(true);
                        loadHistory();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(
                                this,
                                "Failed to save: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                        btnSaveWeight.setEnabled(true);
                    });
        }
    }

    private void loadHistory() {

        db.collection("pets")
                .document(pet.getId())
                .collection("wellnessEntries")
                .whereEqualTo("type", "weight")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    entries.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {

                        WellnessEntry entry =
                                doc.toObject(WellnessEntry.class);

                        entry.setId(doc.getId());
                        entries.add(entry);
                    }

                    Collections.sort(
                            entries,
                            (a, b) -> Long.compare(
                                    b.getTimestampMillis(),
                                    a.getTimestampMillis()
                            )
                    );

                    String thisMonth =
                            monthKey(System.currentTimeMillis());

                    currentMonthEntry = null;

                    for (WellnessEntry e : entries) {

                        if (monthKey(e.getTimestampMillis())
                                .equals(thisMonth)) {

                            currentMonthEntry = e;
                            break;
                        }
                    }

                    if (currentMonthEntry != null) {

                        etNewWeight.setText(
                                currentMonthEntry.getValue()
                        );

                        btnSaveWeight.setText("Update");

                        updateVerdict(
                                currentMonthEntry.getValue(),
                                currentMonthEntry.getTimestampMillis()
                        );

                    } else {

                        etNewWeight.setText("");
                        btnSaveWeight.setText("Save");

                        updateVerdict(
                                pet.getWeight(),
                                System.currentTimeMillis()
                        );
                    }

                    renderHistory();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Failed to load history: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    private void renderHistory() {

        weightHistoryList.removeAllViews();

        if (entries.isEmpty()) {
            txtNoHistory.setVisibility(View.VISIBLE);
            return;
        }

        txtNoHistory.setVisibility(View.GONE);

        LayoutInflater inflater =
                LayoutInflater.from(this);

        SimpleDateFormat displayFormat =
                new SimpleDateFormat(
                        "MMMM yyyy",
                        Locale.getDefault()
                );

        String thisMonth =
                monthKey(System.currentTimeMillis());

        for (WellnessEntry entry : entries) {

            View row = inflater.inflate(
                    R.layout.item_weight_entry,
                    weightHistoryList,
                    false
            );

            TextView txtEntryWeight =
                    row.findViewById(R.id.txtEntryWeight);

            TextView txtEntryDate =
                    row.findViewById(R.id.txtEntryDate);

            TextView txtEntryTag =
                    row.findViewById(R.id.txtEntryTag);

            txtEntryDate.setText(
                    displayFormat.format(
                            new java.util.Date(
                                    entry.getTimestampMillis()
                            )
                    )
            );

            String verdict = "";

            try {

                double weightKg =
                        Double.parseDouble(entry.getValue());

                double ageAtEntry =
                        getAgeInMonthsAt(
                                entry.getTimestampMillis()
                        );

                BreedWeightReference.WeightStatus status =
                        BreedWeightReference.evaluate(
                                pet.getType(),
                                pet.getBreed(),
                                ageAtEntry,
                                weightKg
                        );

                verdict = " — " + status.verdict;

            } catch (Exception ignored) {
            }

            txtEntryWeight.setText(
                    entry.getValue() + " kg" + verdict
            );

            if (monthKey(entry.getTimestampMillis())
                    .equals(thisMonth)) {

                txtEntryTag.setVisibility(View.VISIBLE);

            } else {

                txtEntryTag.setVisibility(View.GONE);
            }

            // Tap a history row to delete it
            final WellnessEntry finalEntry = entry;
            row.setOnClickListener(v ->
                    confirmDeleteWeight(finalEntry)
            );

            weightHistoryList.addView(row);
        }
    }

    private void confirmDeleteWeight(WellnessEntry entry) {

        new AlertDialog.Builder(this)
                .setTitle("Delete this weight entry?")
                .setMessage(
                        entry.getValue() + " kg — logged " +
                                new SimpleDateFormat(
                                        "MMMM yyyy",
                                        Locale.getDefault()
                                ).format(
                                        new java.util.Date(
                                                entry.getTimestampMillis()
                                        )
                                )
                )
                .setPositiveButton(
                        "Delete",
                        (dialog, which) -> {

                            db.collection("pets")
                                    .document(pet.getId())
                                    .collection("wellnessEntries")
                                    .document(entry.getId())
                                    .delete()
                                    .addOnSuccessListener(unused -> {

                                        Toast.makeText(
                                                this,
                                                "Deleted",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        loadHistory();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(
                                                    this,
                                                    "Failed: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT
                                            ).show()
                                    );
                        }
                )
                .setNegativeButton("Cancel", null)
                .show();
    }
}