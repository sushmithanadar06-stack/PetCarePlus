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
import com.example.petcareplus.utils.NotificationsHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdoptionAllRequestsActivity extends AppCompatActivity {

    private static class RequestRow {
        AdoptionRequest request;
        AdoptionListing listing;
    }

    private TextView btnAllRequestsBack, txtNoAllRequests;
    private LinearLayout allRequestsContainer;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private final List<RequestRow> rows = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adoption_all_requests);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        btnAllRequestsBack = findViewById(R.id.btnAllRequestsBack);
        txtNoAllRequests = findViewById(R.id.txtNoAllRequests);
        allRequestsContainer = findViewById(R.id.allRequestsContainer);

        btnAllRequestsBack.setOnClickListener(v -> finish());

        if (auth.getCurrentUser() != null) {
            NotificationsHelper.markAdoptionNotificationsRead(db, auth.getCurrentUser().getUid());
        }

        loadMyListingsThenRequests();
    }

    private void loadMyListingsThenRequests() {

        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        db.collection("adoptionListings")
                .whereEqualTo("ownerId", uid)
                .get()
                .addOnSuccessListener(listingsSnap -> {

                    List<AdoptionListing> myListings = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : listingsSnap) {
                        AdoptionListing listing = doc.toObject(AdoptionListing.class);
                        listing.setId(doc.getId());
                        myListings.add(listing);
                    }

                    if (myListings.isEmpty()) {
                        renderList();
                        return;
                    }

                    fetchRequestsForListings(myListings, 0);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void fetchRequestsForListings(List<AdoptionListing> myListings, int index) {

        if (index >= myListings.size()) {
            Collections.sort(rows, (a, b) ->
                    Long.compare(b.request.getTimestampMillis(), a.request.getTimestampMillis()));
            renderList();
            return;
        }

        AdoptionListing listing = myListings.get(index);

        db.collection("adoptionListings").document(listing.getId())
                .collection("requests")
                .get()
                .addOnSuccessListener(requestsSnap -> {

                    for (QueryDocumentSnapshot doc : requestsSnap) {
                        AdoptionRequest request = doc.toObject(AdoptionRequest.class);
                        request.setId(doc.getId());

                        RequestRow row = new RequestRow();
                        row.request = request;
                        row.listing = listing;
                        rows.add(row);
                    }

                    fetchRequestsForListings(myListings, index + 1);
                })
                .addOnFailureListener(e -> fetchRequestsForListings(myListings, index + 1));
    }

    private void renderList() {

        allRequestsContainer.removeAllViews();

        if (rows.isEmpty()) {
            txtNoAllRequests.setVisibility(View.VISIBLE);
            return;
        }

        txtNoAllRequests.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (RequestRow row : rows) {

            View view = inflater.inflate(R.layout.item_adoption_request_all, allRequestsContainer, false);

            ImageView imgAllReqPetPhoto = view.findViewById(R.id.imgAllReqPetPhoto);
            TextView txtRequesterName = view.findViewById(R.id.txtAllReqRequesterName);
            TextView txtPetName = view.findViewById(R.id.txtAllReqPetName);

            txtRequesterName.setText(row.request.getRequesterName());
            txtPetName.setText("Interested in " + row.listing.getPetName());

            // NEW: photo thumbnail + tap-to-zoom, matching the treatment
            // AdoptionRequestsActivity already has for a single listing.
            String thumbnailPhoto = row.listing.getFirstPhoto();

            if (thumbnailPhoto != null && ImageUtils.isBase64Image(thumbnailPhoto)) {
                Bitmap bitmap = ImageUtils.base64ToBitmap(thumbnailPhoto);
                if (bitmap != null) imgAllReqPetPhoto.setImageBitmap(bitmap);
            }

            imgAllReqPetPhoto.setOnClickListener(v -> {
                if (thumbnailPhoto != null) {
                    Intent intent = new Intent(AdoptionAllRequestsActivity.this, PhotoViewerActivity.class);
                    intent.putExtra("photoBase64", thumbnailPhoto);
                    startActivity(intent);
                }
            });

            view.setOnClickListener(v -> {
                String chatId = row.request.getListingId() + "_" + row.request.getRequesterId();
                Intent intent = new Intent(AdoptionAllRequestsActivity.this, AdoptionChatActivity.class);
                intent.putExtra("chatId", chatId);
                intent.putExtra("petName", row.listing.getPetName());
                startActivity(intent);
            });

            allRequestsContainer.addView(view);
        }
    }
}