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
import com.example.petcareplus.utils.ImageUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shows the other person's public profile — their name, real profile photo
 * if they've set one (falling back to an initials circle if not), and
 * every adoption chat shared between the current user and them. No phone
 * number, email, or other private details are shown here — those stay in
 * /users/{uid} and are never mirrored to /publicProfiles.
 */
public class OtherUserProfileActivity extends AppCompatActivity {

    private static class ChatSummary {
        String chatId;
        String petName;
        String lastMessageText;
        long lastMessageTimestamp;
    }

    private TextView btnProfileViewBack, txtProfileViewInitial, txtProfileViewName;
    private ImageView imgProfileViewPhoto;
    private LinearLayout profileChatsContainer;
    private TextView txtNoProfileChats;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String otherUid;
    private String otherName;
    private String otherPhotoBase64 = "";

    private final List<ChatSummary> summaries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other_user_profile);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        btnProfileViewBack = findViewById(R.id.btnProfileViewBack);
        txtProfileViewInitial = findViewById(R.id.txtProfileViewInitial);
        imgProfileViewPhoto = findViewById(R.id.imgProfileViewPhoto);
        txtProfileViewName = findViewById(R.id.txtProfileViewName);
        profileChatsContainer = findViewById(R.id.profileChatsContainer);
        txtNoProfileChats = findViewById(R.id.txtNoProfileChats);

        otherUid = getIntent().getStringExtra("otherUid");
        otherName = getIntent().getStringExtra("otherName");

        if (otherUid == null || otherUid.isEmpty()) {
            Toast.makeText(this, "Unable to load profile", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String displayName = (otherName != null && !otherName.trim().isEmpty()) ? otherName : "this user";
        txtProfileViewName.setText(displayName);
        txtProfileViewInitial.setText(displayName.trim().substring(0, 1).toUpperCase());

        btnProfileViewBack.setOnClickListener(v -> finish());

        loadPublicProfile();
        loadSharedChats();
    }

    /**
     * Fetches the other person's public profile (name + photo only — see
     * class doc). Falls back to the initials circle already showing if
     * they haven't set a photo, or if the document doesn't exist.
     */
    private void loadPublicProfile() {

        db.collection("publicProfiles").document(otherUid).get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) return;

                    String name = doc.getString("name");
                    if (name != null && !name.trim().isEmpty()) {
                        otherName = name;
                        txtProfileViewName.setText(name);
                        txtProfileViewInitial.setText(name.trim().substring(0, 1).toUpperCase());
                    }

                    String photoUri = doc.getString("photoUri");

                    if (photoUri != null && !photoUri.isEmpty() && ImageUtils.isBase64Image(photoUri)) {

                        Bitmap bitmap = ImageUtils.base64ToBitmap(photoUri);

                        if (bitmap != null) {
                            otherPhotoBase64 = photoUri;
                            imgProfileViewPhoto.setImageBitmap(bitmap);
                            imgProfileViewPhoto.setVisibility(View.VISIBLE);
                            txtProfileViewInitial.setVisibility(View.GONE);

                            // Tap to view full-size and zoomable — same
                            // viewer used everywhere else in the app.
                            imgProfileViewPhoto.setOnClickListener(v -> {
                                Intent intent = new Intent(OtherUserProfileActivity.this, PhotoViewerActivity.class);
                                intent.putExtra("photoBase64", otherPhotoBase64);
                                startActivity(intent);
                            });
                        }
                    }
                });
        // No failure toast here — a missing public profile just means this
        // person hasn't set one yet, which isn't an error worth surfacing.
    }

    private void loadSharedChats() {

        if (auth.getCurrentUser() == null) return;

        String myUid = auth.getCurrentUser().getUid();
        Map<String, DocumentSnapshot> combined = new HashMap<>();

        // I'm the owner, they're the requester
        db.collection("adoptionChats")
                .whereEqualTo("ownerId", myUid)
                .whereEqualTo("requesterId", otherUid)
                .get()
                .addOnSuccessListener(snapA -> {

                    for (QueryDocumentSnapshot doc : snapA) {
                        combined.put(doc.getId(), doc);
                    }

                    // I'm the requester, they're the owner
                    db.collection("adoptionChats")
                            .whereEqualTo("ownerId", otherUid)
                            .whereEqualTo("requesterId", myUid)
                            .get()
                            .addOnSuccessListener(snapB -> {

                                for (QueryDocumentSnapshot doc : snapB) {
                                    combined.put(doc.getId(), doc);
                                }

                                buildSummaries(combined);
                            })
                            .addOnFailureListener(e -> buildSummaries(combined));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load chats: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void buildSummaries(Map<String, DocumentSnapshot> combined) {

        summaries.clear();

        for (Map.Entry<String, DocumentSnapshot> entry : combined.entrySet()) {

            DocumentSnapshot doc = entry.getValue();

            ChatSummary summary = new ChatSummary();
            summary.chatId = entry.getKey();
            summary.petName = doc.getString("petName");
            summary.lastMessageText = doc.getString("lastMessageText");

            Long ts = doc.getLong("lastMessageTimestamp");
            summary.lastMessageTimestamp = ts != null ? ts : 0L;

            summaries.add(summary);
        }

        Collections.sort(summaries, (a, b) ->
                Long.compare(b.lastMessageTimestamp, a.lastMessageTimestamp));

        renderChats();
    }

    private void renderChats() {

        profileChatsContainer.removeAllViews();

        if (summaries.isEmpty()) {
            txtNoProfileChats.setVisibility(View.VISIBLE);
            return;
        }

        txtNoProfileChats.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        SimpleDateFormat timeFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

        for (ChatSummary summary : summaries) {

            // Reuses the same row layout as the Messages inbox — same
            // fields (title, preview, time), so no new layout needed.
            View row = inflater.inflate(R.layout.item_adoption_message, profileChatsContainer, false);

            TextView txtTitle = row.findViewById(R.id.txtMsgListTitle);
            TextView txtPreview = row.findViewById(R.id.txtMsgListPreview);
            TextView txtTime = row.findViewById(R.id.txtMsgListTime);

            txtTitle.setText(summary.petName != null ? summary.petName : "Adoption chat");

            txtPreview.setText(summary.lastMessageText != null && !summary.lastMessageText.isEmpty()
                    ? summary.lastMessageText : "No messages yet");

            txtTime.setText(summary.lastMessageTimestamp > 0
                    ? timeFormat.format(new Date(summary.lastMessageTimestamp)) : "");

            row.setOnClickListener(v -> {
                Intent intent = new Intent(OtherUserProfileActivity.this, AdoptionChatActivity.class);
                intent.putExtra("chatId", summary.chatId);
                intent.putExtra("petName", summary.petName);
                startActivity(intent);
            });

            profileChatsContainer.addView(row);
        }
    }
}