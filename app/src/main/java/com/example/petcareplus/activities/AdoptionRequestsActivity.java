package com.example.petcareplus.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.models.AdoptionListing;
import com.example.petcareplus.models.AdoptionRequest;
import com.example.petcareplus.utils.ImageUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdoptionRequestsActivity extends AppCompatActivity {

    private TextView btnRequestsBack, txtRequestsTitle, txtNoRequests;
    private LinearLayout requestsContainer;

    private AdoptionListing listing;
    private FirebaseFirestore db;
    private final List<AdoptionRequest> requests = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adoption_requests);

        db = FirebaseFirestore.getInstance();

        btnRequestsBack = findViewById(R.id.btnRequestsBack);
        txtRequestsTitle = findViewById(R.id.txtRequestsTitle);
        requestsContainer = findViewById(R.id.requestsContainer);
        txtNoRequests = findViewById(R.id.txtNoRequests);

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            listing = getIntent().getSerializableExtra("listing", AdoptionListing.class);
        } else {
            listing = (AdoptionListing) getIntent().getSerializableExtra("listing");
        }

        if (listing == null) {
            Toast.makeText(this, "Unable to load listing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        txtRequestsTitle.setText(listing.getPetName() + "'s Requests");
        btnRequestsBack.setOnClickListener(v -> finish());

        loadRequests();
    }

    private void loadRequests() {

        db.collection("adoptionListings").document(listing.getId())
                .collection("requests")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    requests.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        AdoptionRequest r = doc.toObject(AdoptionRequest.class);
                        r.setId(doc.getId());
                        requests.add(r);
                    }

                    Collections.sort(requests, (a, b) ->
                            Long.compare(b.getTimestampMillis(), a.getTimestampMillis()));

                    renderList();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void renderList() {

        requestsContainer.removeAllViews();

        if (requests.isEmpty()) {
            txtNoRequests.setVisibility(View.VISIBLE);
            return;
        }

        txtNoRequests.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        String thumbnailPhoto = listing.getFirstPhoto();

        for (AdoptionRequest request : requests) {

            View row = inflater.inflate(R.layout.item_adoption_request, requestsContainer, false);

            ImageView imgRequesterPhoto = row.findViewById(R.id.imgRequesterPhoto);
            TextView txtRequesterName = row.findViewById(R.id.txtRequesterName);

            txtRequesterName.setText(request.getRequesterName());

            if (thumbnailPhoto != null && ImageUtils.isBase64Image(thumbnailPhoto)) {
                Bitmap bitmap = ImageUtils.base64ToBitmap(thumbnailPhoto);
                if (bitmap != null) imgRequesterPhoto.setImageBitmap(bitmap);
            }

            // FIX: was FullscreenPhotoActivity, which doesn't exist in this
            // project — that's what was causing the compile error. Tap to
            // zoom now goes through the real PhotoViewerActivity, same one
            // already working for photos in Community and Adoption chat.
            imgRequesterPhoto.setOnClickListener(v -> {
                if (thumbnailPhoto != null) {
                    Intent intent = new Intent(AdoptionRequestsActivity.this, PhotoViewerActivity.class);
                    intent.putExtra("photoBase64", thumbnailPhoto);
                    startActivity(intent);
                }
            });

            row.setOnClickListener(v -> {
                String chatId = listing.getId() + "_" + request.getRequesterId();
                Intent intent = new Intent(AdoptionRequestsActivity.this, AdoptionChatActivity.class);
                intent.putExtra("chatId", chatId);
                intent.putExtra("petName", listing.getPetName());
                startActivity(intent);
            });

            requestsContainer.addView(row);
        }
    }
}