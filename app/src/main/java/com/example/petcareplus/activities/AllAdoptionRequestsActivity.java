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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// NOTE: Based on the actual navigation code in AdoptionListingsActivity, the
// "🎗️ Requests" button launches AdoptionAllRequestsActivity, not this class.
// This file is very likely unused/duplicate. Run Find Usages on this class
// name to confirm before deleting it — if nothing references it, it's safe
// to remove along with its layout files (activity_all_adoption_requests.xml,
// item_all_requests_row.xml).
public class AllAdoptionRequestsActivity extends AppCompatActivity {

    private TextView btnAllReqBack, txtNoAllRequests;
    private LinearLayout requestsContainer;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private static class CombinedRequest {
        AdoptionRequest request;
        AdoptionListing listing;
    }

    private final List<CombinedRequest> combined = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_adoption_requests);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        btnAllReqBack = findViewById(R.id.btnAllReqBack);
        requestsContainer = findViewById(R.id.requestsContainer);
        txtNoAllRequests = findViewById(R.id.txtNoAllRequests);

        btnAllReqBack.setOnClickListener(v -> finish());

        loadAllRequests();
    }

    private void loadAllRequests() {

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("adoptionListings")
                .whereEqualTo("ownerId", uid)
                .get()
                .addOnSuccessListener(listingsSnapshot -> {

                    combined.clear();
                    List<AdoptionListing> myListings = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : listingsSnapshot) {
                        AdoptionListing listing = doc.toObject(AdoptionListing.class);
                        listing.setId(doc.getId());
                        myListings.add(listing);
                    }

                    if (myListings.isEmpty()) {
                        renderList();
                        return;
                    }

                    int[] pending = {myListings.size()};

                    for (AdoptionListing listing : myListings) {
                        db.collection("adoptionListings").document(listing.getId())
                                .collection("requests")
                                .get()
                                .addOnSuccessListener(requestsSnapshot -> {

                                    for (QueryDocumentSnapshot doc : requestsSnapshot) {
                                        AdoptionRequest r = doc.toObject(AdoptionRequest.class);
                                        r.setId(doc.getId());

                                        CombinedRequest cr = new CombinedRequest();
                                        cr.request = r;
                                        cr.listing = listing;
                                        combined.add(cr);
                                    }

                                    pending[0]--;
                                    if (pending[0] == 0) {
                                        Collections.sort(combined, (a, b) ->
                                                Long.compare(b.request.getTimestampMillis(), a.request.getTimestampMillis()));
                                        renderList();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    pending[0]--;
                                    if (pending[0] == 0) renderList();
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void renderList() {

        requestsContainer.removeAllViews();

        if (combined.isEmpty()) {
            txtNoAllRequests.setVisibility(View.VISIBLE);
            return;
        }

        txtNoAllRequests.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (CombinedRequest cr : combined) {

            View row = inflater.inflate(R.layout.item_all_requests_row, requestsContainer, false);

            ImageView imgAllReqPetPhoto = row.findViewById(R.id.imgAllReqPetPhoto);
            TextView txtAllReqRequesterName = row.findViewById(R.id.txtAllReqRequesterName);
            TextView txtAllReqPetName = row.findViewById(R.id.txtAllReqPetName);

            txtAllReqRequesterName.setText(cr.request.getRequesterName());
            txtAllReqPetName.setText("Interested in " + cr.listing.getPetName());

            String thumbnailPhoto = cr.listing.getFirstPhoto();

            if (thumbnailPhoto != null && ImageUtils.isBase64Image(thumbnailPhoto)) {
                Bitmap bitmap = ImageUtils.base64ToBitmap(thumbnailPhoto);
                if (bitmap != null) imgAllReqPetPhoto.setImageBitmap(bitmap);
            }

            // FIX: was FullscreenPhotoActivity, which doesn't exist in this
            // project. Now uses the real PhotoViewerActivity.
            final String photoForZoom = thumbnailPhoto;
            imgAllReqPetPhoto.setOnClickListener(v -> {
                if (photoForZoom != null) {
                    Intent intent = new Intent(AllAdoptionRequestsActivity.this, PhotoViewerActivity.class);
                    intent.putExtra("photoBase64", photoForZoom);
                    startActivity(intent);
                }
            });

            row.setOnClickListener(v -> {
                String chatId = cr.listing.getId() + "_" + cr.request.getRequesterId();
                Intent intent = new Intent(AllAdoptionRequestsActivity.this, AdoptionChatActivity.class);
                intent.putExtra("chatId", chatId);
                intent.putExtra("petName", cr.listing.getPetName());
                startActivity(intent);
            });

            requestsContainer.addView(row);
        }
    }
}