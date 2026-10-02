package com.example.petcareplus.activities;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.petcareplus.R;
import com.example.petcareplus.models.HealthRecord;
import com.example.petcareplus.utils.PdfReportGenerator;
import com.example.petcareplus.utils.PdfReportGenerator.Row;
import com.example.petcareplus.utils.PdfReportGenerator.Section;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HealthReportActivity extends AppCompatActivity {

    private static final long DAY = 24L * 60 * 60 * 1000;

    private Button btnGenerateReport;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_report);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        TextView btnReportBack = findViewById(R.id.btnReportBack);
        btnGenerateReport = findViewById(R.id.btnGenerateReport);

        btnReportBack.setOnClickListener(v -> finish());
        btnGenerateReport.setOnClickListener(v -> loadPetsThenAsk());
    }

    // ---------------- 1. ask which pet ----------------

    private void loadPetsThenAsk() {

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGenerateReport.setEnabled(false);

        db.collection("pets")
                .whereEqualTo("ownerId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(snap -> {

                    btnGenerateReport.setEnabled(true);

                    if (snap.isEmpty()) {
                        Toast.makeText(this, "Add a pet first to generate a report", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<DocumentSnapshot> docs = snap.getDocuments();
                    String[] names = new String[docs.size()];
                    for (int i = 0; i < docs.size(); i++) names[i] = str(docs.get(i), "name", "Pet");

                    new AlertDialog.Builder(this)
                            .setTitle("Which pet is this report for?")
                            .setItems(names, (d, which) -> generateReportFor(docs.get(which)))
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    btnGenerateReport.setEnabled(true);
                    Toast.makeText(this, "Failed to load pets: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ---------------- 2. read the records ----------------

    private void generateReportFor(DocumentSnapshot petDoc) {

        final String petId = petDoc.getId();
        final String petName = str(petDoc, "name", "Pet");
        final String petType = str(petDoc, "type", "-");
        final String petBreed = str(petDoc, "breed", "-");
        final String petAge = str(petDoc, "age", "-");
        final String petWeight = str(petDoc, "weight", "-");

        final long now = System.currentTimeMillis();
        final long from = now - 30L * DAY;

        Toast.makeText(this, "Preparing report...", Toast.LENGTH_SHORT).show();
        btnGenerateReport.setEnabled(false);

        db.collection("pets").document(petId).collection("healthRecords")
                .get()
                .addOnSuccessListener(healthSnap -> {

                    List<HealthRecord> all = new ArrayList<>();
                    for (QueryDocumentSnapshot d : healthSnap) {
                        HealthRecord r = d.toObject(HealthRecord.class);
                        r.setId(d.getId());
                        all.add(r);
                    }
                    Collections.sort(all, (a, b) -> Long.compare(b.getDateMillis(), a.getDateMillis()));

                    db.collection("pets").document(petId).collection("wellnessEntries")
                            .whereEqualTo("type", "weight")
                            .get()
                            .addOnSuccessListener(weightSnap ->
                                    build(petName, petType, petBreed, petAge, petWeight, now, from, all, weightSnap.getDocuments()))
                            .addOnFailureListener(e ->
                                    build(petName, petType, petBreed, petAge, petWeight, now, from, all, new ArrayList<>()));
                })
                .addOnFailureListener(e -> {
                    btnGenerateReport.setEnabled(true);
                    Toast.makeText(this, "Failed to load records: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ---------------- 3. build sections + PDF ----------------

    private void build(String petName, String petType, String petBreed, String petAge, String petWeight,
                       long now, long from, List<HealthRecord> all, List<DocumentSnapshot> weightDocs) {

        Section vacc = new Section("Vaccinations", 0xFF2F6B52);
        Section meds = new Section("Medications", 0xFF6F52B5);
        Section worm = new Section("Deworming", 0xFFD9728A);
        Section vets = new Section("Vet Visits", 0xFF3E7BE0);
        Section weights = new Section("Weight Log", 0xFF1E9E8A);
        List<Row> upcoming = new ArrayList<>();

        int totVacc = 0, totMed = 0, totWorm = 0, totVet = 0;

        for (HealthRecord r : all) {

            String cat = r.getCategory() == null ? "" : r.getCategory();
            long start = r.getDateMillis();
            long end = r.getNextDueMillis();
            String title = r.getTitle() == null ? "Entry" : r.getTitle();
            String notes = r.getSubtitle() == null ? "" : r.getSubtitle();

            switch (cat) {
                case "vaccination": totVacc++; break;
                case "medication":  totMed++;  break;
                case "deworming":   totWorm++; break;
                case "vetvisit":    totVet++;  break;
                default: continue;
            }

            boolean inPeriod = start >= from && start <= now;

            if (cat.equals("medication")) {
                // include a course if it overlapped the last 30 days
                boolean overlaps = start <= now && (end == 0 || end >= from);
                if (overlaps) {
                    String course = "Course: " + date(start) + (end > 0 ? " to " + date(end) : "");
                    meds.rows.add(new Row(title, date(start), join(notes, course)));
                }
                continue;
            }

            if (inPeriod) {
                switch (cat) {
                    case "vaccination":
                        vacc.rows.add(new Row(title, date(start),
                                join(notes, end > 0 ? "Next due: " + date(end) : "")));
                        break;
                    case "deworming":
                        worm.rows.add(new Row(title, date(start),
                                join(notes, end > 0 ? "Next due: " + date(end) : "")));
                        break;
                    case "vetvisit":
                        vets.rows.add(new Row(title, date(start),
                                join(join(r.getAddress(), r.getPhone() == null || r.getPhone().isEmpty()
                                        ? "" : "Phone: " + r.getPhone()), r.getHours() == null || r.getHours().isEmpty()
                                        ? "" : "Hours: " + r.getHours())));
                        break;
                }
            }

            // upcoming: future-dated entries, or a "next due" date still ahead
            if (start > now) {
                upcoming.add(new Row(label(cat) + ": " + title, date(start), notes));
            } else if (end > now) {
                upcoming.add(new Row(label(cat) + " due: " + title, date(end), notes));
            }
        }

        // weights from wellness entries
        List<DocumentSnapshot> ws = new ArrayList<>(weightDocs);
        Collections.sort(ws, (a, b) -> Long.compare(ts(b), ts(a)));
        for (DocumentSnapshot d : ws) {
            long t = ts(d);
            if (t < from || t > now) continue;
            String v = str(d, "value", "-");
            weights.rows.add(new Row(v + " kg", date(t), ""));
        }

        int inMonth = vacc.rows.size() + meds.rows.size() + worm.rows.size()
                + vets.rows.size() + weights.rows.size();

        String summary = inMonth + " entries in the last 30 days: "
                + vacc.rows.size() + " vaccination(s), " + meds.rows.size() + " medication(s), "
                + worm.rows.size() + " deworming, " + vets.rows.size() + " vet visit(s), "
                + weights.rows.size() + " weight log(s).  All-time on file: "
                + totVacc + " vaccinations, " + totMed + " medications, "
                + totWorm + " dewormings, " + totVet + " vet visits.";

        List<Section> sections = new ArrayList<>();
        sections.add(vacc);
        sections.add(meds);
        sections.add(worm);
        sections.add(vets);
        sections.add(weights);

        Collections.sort(upcoming, (a, b) -> a.date.compareTo(b.date));

        String period = date(from) + " - " + date(now);

        try {
            File pdf = PdfReportGenerator.generate(this, petName, petType, petBreed, petAge, petWeight,
                    period, summary, sections, upcoming);
            btnGenerateReport.setEnabled(true);
            askShareOrSave(petName, pdf);
        } catch (Exception e) {
            btnGenerateReport.setEnabled(true);
            Toast.makeText(this, "Failed to generate PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ---------------- 4. share / email / download ----------------

    private void askShareOrSave(String petName, File pdf) {
        new AlertDialog.Builder(this)
                .setTitle("Report ready")
                .setMessage(petName + "'s 30-day health report is ready.")
                .setPositiveButton("Share / Email", (d, w) -> share(petName, pdf))
                .setNeutralButton("Save to Downloads", (d, w) -> saveToDownloads(pdf))
                .setNegativeButton("Close", null)
                .show();
    }

    private void share(String petName, File pdf) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdf);
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("application/pdf");
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.putExtra(Intent.EXTRA_SUBJECT, petName + "'s Health Report - PetCare+");
            i.putExtra(Intent.EXTRA_TEXT, "Attached is " + petName + "'s 30-day health report from PetCare+.");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(i, "Share or email report"));
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't share: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveToDownloads(File pdf) {

        if (Build.VERSION.SDK_INT >= 29) {
            try {
                ContentValues v = new ContentValues();
                v.put(MediaStore.Downloads.DISPLAY_NAME, pdf.getName());
                v.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                if (uri == null) throw new Exception("could not create file");

                try (OutputStream os = getContentResolver().openOutputStream(uri);
                     FileInputStream in = new FileInputStream(pdf)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
                }
                Toast.makeText(this, "Saved to Downloads: " + pdf.getName(), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Use Share and pick Files/Drive to save it", Toast.LENGTH_LONG).show();
            share("Pet", pdf);
        }
    }

    // ---------------- helpers ----------------

    private String date(long millis) {
        return millis <= 0 ? "-" : fmt.format(new Date(millis));
    }

    private long ts(DocumentSnapshot d) {
        Long t = d.getLong("timestampMillis");
        return t == null ? 0 : t;
    }

    private String str(DocumentSnapshot d, String key, String fallback) {
        Object o = d.get(key);
        if (o == null) return fallback;
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? fallback : s;
    }

    private String join(String a, String b) {
        boolean ea = a == null || a.isEmpty(), eb = b == null || b.isEmpty();
        if (ea) return eb ? "" : b;
        return eb ? a : a + "  |  " + b;
    }

    private String label(String cat) {
        switch (cat) {
            case "vaccination": return "Vaccination";
            case "medication":  return "Medication";
            case "deworming":   return "Deworming";
            case "vetvisit":    return "Vet visit";
            default:            return "Record";
        }
    }
}