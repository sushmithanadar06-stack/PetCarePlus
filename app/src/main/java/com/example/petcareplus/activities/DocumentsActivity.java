package com.example.petcareplus.activities;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.petcareplus.R;
import com.example.petcareplus.models.DocumentRecord;
import com.example.petcareplus.models.Pet;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

public class DocumentsActivity extends AppCompatActivity {

    private TextView btnDocsBack;
    private LinearLayout btnUploadDocument;
    private LinearLayout documentsListContainer;
    private TextView txtNoDocuments;

    private LinearLayout navBarHome, navBarRecords, navBarRescue, navBarCommunity, navBarProfile;

    private final ArrayList<DocumentRecord> docs = new ArrayList<>();
    private FirebaseFirestore db;
    private Pet pet;

    private final ActivityResultLauncher<String[]> filePicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) {}
                    copyAndSaveDocument(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documents);

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

        btnDocsBack = findViewById(R.id.btnDocsBack);
        btnUploadDocument = findViewById(R.id.btnUploadDocument);
        documentsListContainer = findViewById(R.id.documentsListContainer);
        txtNoDocuments = findViewById(R.id.txtNoDocuments);

        navBarHome = findViewById(R.id.navBarHome);
        navBarRecords = findViewById(R.id.navBarRecords);
        navBarRescue = findViewById(R.id.navBarRescue);
        navBarCommunity = findViewById(R.id.navBarCommunity);
        navBarProfile = findViewById(R.id.navBarProfile);

        btnDocsBack.setOnClickListener(v -> finish());
        btnUploadDocument.setOnClickListener(v -> filePicker.launch(new String[]{"*/*"}));

        setupBottomNav();
        loadDocuments();
    }

    private CollectionReference documentsCollection() {
        return db.collection("pets").document(pet.getId()).collection("documents");
    }

    private void setupBottomNav() {
        navBarHome.setOnClickListener(v -> {
            Intent intent = new Intent(DocumentsActivity.this, PetListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
        navBarRecords.setOnClickListener(v -> {
            Intent intent = new Intent(DocumentsActivity.this, MedicalRecordsHubActivity.class);
            intent.putExtra("pet", pet);
            startActivity(intent);
        });
        navBarRescue.setOnClickListener(v -> Toast.makeText(this, "Coming in a future module", Toast.LENGTH_SHORT).show());
        navBarCommunity.setOnClickListener(v -> Toast.makeText(this, "Coming in a future module", Toast.LENGTH_SHORT).show());
        navBarProfile.setOnClickListener(v ->
                startActivity(new Intent(DocumentsActivity.this, ProfileActivity.class))
        );
    }

    private void copyAndSaveDocument(Uri sourceUri) {

        try {
            String fileName = getFileName(sourceUri);

            File docsDir = new File(getFilesDir(), "pet_documents");
            if (!docsDir.exists()) docsDir.mkdirs();

            File destFile = new File(docsDir, System.currentTimeMillis() + "_" + fileName);

            InputStream in = getContentResolver().openInputStream(sourceUri);
            FileOutputStream out = new FileOutputStream(destFile);

            byte[] buffer = new byte[4096];
            int read;
            while (in != null && (read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            if (in != null) in.close();
            out.close();

            String id = documentsCollection().document().getId();

            DocumentRecord record = new DocumentRecord(
                    id, pet.getId(), fileName, destFile.getAbsolutePath(),
                    destFile.length(), System.currentTimeMillis()
            );

            documentsCollection().document(id)
                    .set(record)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Document uploaded", Toast.LENGTH_SHORT).show();
                        loadDocuments();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show());

        } catch (Exception e) {
            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getFileName(Uri uri) {
        String name = "document";
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = cursor.getString(idx);
            }
            cursor.close();
        }
        return name;
    }

    private void loadDocuments() {
        documentsCollection()
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    docs.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        DocumentRecord d = doc.toObject(DocumentRecord.class);
                        d.setId(doc.getId());
                        docs.add(d);
                    }
                    Collections.sort(docs, (a, b) -> Long.compare(b.getUploadedMillis(), a.getUploadedMillis()));
                    renderList();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void renderList() {

        documentsListContainer.removeAllViews();

        if (docs.isEmpty()) {
            txtNoDocuments.setVisibility(View.VISIBLE);
            return;
        }

        txtNoDocuments.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (DocumentRecord doc : docs) {

            View row = inflater.inflate(R.layout.item_document, documentsListContainer, false);

            TextView txtDocName = row.findViewById(R.id.txtDocName);
            TextView txtDocMeta = row.findViewById(R.id.txtDocMeta);

            txtDocName.setText(doc.getFileName());

            String size = android.text.format.Formatter.formatShortFileSize(this, doc.getSizeBytes());
            String date = android.text.format.DateFormat.format("dd MMM", doc.getUploadedMillis()).toString();
            txtDocMeta.setText(size + " · Uploaded " + date);

            row.setOnClickListener(v -> openDocument(doc));

            row.setOnLongClickListener(v -> {
                showViewDeleteMenu(doc);
                return true;
            });

            documentsListContainer.addView(row);
        }
    }

    private void openDocument(DocumentRecord doc) {

        File file = new File(doc.getLocalPath());

        if (!file.exists()) {
            Toast.makeText(this, "File not found on device", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri contentUri = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", file
            );

            String mimeType = getContentResolver().getType(contentUri);
            if (mimeType == null) mimeType = "*/*";

            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setDataAndType(contentUri, mimeType);
            viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (viewIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(viewIntent);
            } else {
                Toast.makeText(this, "No app found to open this file type", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Unable to open file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showViewDeleteMenu(DocumentRecord doc) {

        String[] options = {"View", "Delete"};

        new AlertDialog.Builder(this)
                .setTitle(doc.getFileName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openDocument(doc);
                    } else {
                        confirmDelete(doc);
                    }
                })
                .show();
    }

    private void confirmDelete(DocumentRecord doc) {

        new AlertDialog.Builder(this)
                .setTitle("Delete this document?")
                .setMessage("This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {

                    new File(doc.getLocalPath()).delete();

                    documentsCollection().document(doc.getId())
                            .delete()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Document deleted", Toast.LENGTH_SHORT).show();
                                loadDocuments();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}