package com.example.petcareplus.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.models.AdoptionListing;
import com.example.petcareplus.utils.ImageUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class PostAdoptionActivity extends AppCompatActivity {

    private static final int MAX_PHOTOS = 4;

    private TextView btnPostBack, txtPostTitle;
    private LinearLayout photoThumbsContainer;
    private EditText etPetName, etBreed, etAge, etReason, etLocation;
    private Spinner spGender;
    private Button btnSubmitPost;

    private final List<String> photoList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private AdoptionListing editingListing = null;

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;

                if (photoList.size() >= MAX_PHOTOS) {
                    Toast.makeText(this, "You can add up to " + MAX_PHOTOS + " photos", Toast.LENGTH_SHORT).show();
                    return;
                }

                String base64 = ImageUtils.uriToCompressedBase64(this, uri);
                if (!base64.isEmpty()) {
                    photoList.add(base64);
                    renderPhotoThumbs();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_adoption);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        btnPostBack = findViewById(R.id.btnPostBack);
        txtPostTitle = findViewById(R.id.txtPostTitle);
        photoThumbsContainer = findViewById(R.id.photoThumbsContainer);
        etPetName = findViewById(R.id.etPetName);
        etBreed = findViewById(R.id.etBreed);
        etAge = findViewById(R.id.etAge);
        etReason = findViewById(R.id.etReason);
        etLocation = findViewById(R.id.etLocation);
        spGender = findViewById(R.id.spGender);
        btnSubmitPost = findViewById(R.id.btnSubmitPost);

        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"Male", "Female"});
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGender.setAdapter(genderAdapter);

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            editingListing = getIntent().getSerializableExtra("editListing", AdoptionListing.class);
        } else {
            editingListing = (AdoptionListing) getIntent().getSerializableExtra("editListing");
        }

        if (editingListing != null) {
            prefillForEdit();
        }

        btnPostBack.setOnClickListener(v -> finish());
        btnSubmitPost.setOnClickListener(v -> submitPost());

        renderPhotoThumbs();
    }

    private void prefillForEdit() {

        txtPostTitle.setText("Edit Adoption Post");
        btnSubmitPost.setText("Update Listing");

        etPetName.setText(editingListing.getPetName());
        etBreed.setText(editingListing.getBreed());
        etAge.setText(editingListing.getAge());
        etReason.setText(editingListing.getReason());
        etLocation.setText(editingListing.getLocation());

        if ("Female".equalsIgnoreCase(editingListing.getGender())) {
            spGender.setSelection(1);
        }

        if (editingListing.getPhotoList() != null) {
            for (String p : editingListing.getPhotoList()) {
                if (p != null && !p.isEmpty()) photoList.add(p);
            }
        }
    }

    private void renderPhotoThumbs() {

        photoThumbsContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < photoList.size(); i++) {

            String base64 = photoList.get(i);
            final int index = i;

            View thumb = inflater.inflate(R.layout.item_adoption_photo_thumb, photoThumbsContainer, false);
            ImageView imgThumb = thumb.findViewById(R.id.imgThumb);
            TextView btnRemoveThumb = thumb.findViewById(R.id.btnRemoveThumb);

            android.graphics.Bitmap bitmap = ImageUtils.base64ToBitmap(base64);
            if (bitmap != null) imgThumb.setImageBitmap(bitmap);

            btnRemoveThumb.setOnClickListener(v -> {
                photoList.remove(index);
                renderPhotoThumbs();
            });

            photoThumbsContainer.addView(thumb);
        }

        if (photoList.size() < MAX_PHOTOS) {

            FrameLayout addTile = new FrameLayout(this);
            addTile.setLayoutParams(new LinearLayout.LayoutParams(
                    (int) (72 * getResources().getDisplayMetrics().density),
                    (int) (72 * getResources().getDisplayMetrics().density)
            ));
            addTile.setBackgroundResource(R.drawable.bg_button_outline_purple);

            TextView plus = new TextView(this);
            plus.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            plus.setGravity(android.view.Gravity.CENTER);
            plus.setText("+ Add");
            plus.setTextSize(12);
            plus.setTextColor(0xFF2F6B52);

            addTile.addView(plus);
            addTile.setOnClickListener(v -> imagePicker.launch("image/*"));

            photoThumbsContainer.addView(addTile);
        }
    }

    private void submitPost() {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String petName = etPetName.getText().toString().trim();
        String breed = etBreed.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String gender = spGender.getSelectedItem() != null ? spGender.getSelectedItem().toString() : "";
        String reason = etReason.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        if (petName.isEmpty()) { etPetName.setError("Enter a name"); return; }
        if (breed.isEmpty()) { etBreed.setError("Enter a breed"); return; }
        if (location.isEmpty()) { etLocation.setError("Enter your location"); return; }

        btnSubmitPost.setEnabled(false);

        String ownerName = user.isAnonymous() ? "Guest"
                : (user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                   ? user.getDisplayName() : "A user");

        if (editingListing != null) {

            AdoptionListing updated = new AdoptionListing(
                    editingListing.getId(), editingListing.getOwnerId(), ownerName, petName, breed, age,
                    gender, reason, location, new ArrayList<>(photoList),
                    editingListing.isNgoVerified(), editingListing.getTimestampMillis()
            );

            db.collection("adoptionListings").document(editingListing.getId())
                    .set(updated)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Listing updated", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnSubmitPost.setEnabled(true);
                    });

        } else {

            DocumentReference newRef = db.collection("adoptionListings").document();

            AdoptionListing listing = new AdoptionListing(
                    newRef.getId(), user.getUid(), ownerName, petName, breed, age, gender,
                    reason, location, new ArrayList<>(photoList), false, System.currentTimeMillis()
            );

            newRef.set(listing)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Posted to community", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnSubmitPost.setEnabled(true);
                    });
        }
    }
}