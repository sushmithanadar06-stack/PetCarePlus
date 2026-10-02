package com.example.petcareplus.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
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

public class AdoptionMessagesActivity extends AppCompatActivity {

    private static class ChatSummary {
        String chatId;
        String petName;
        String otherPartyName;
        String lastMessageText;
        long lastMessageTimestamp;
    }

    private TextView btnMessagesBack, txtNoMessagesInbox;
    private LinearLayout messagesListContainer;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private final List<ChatSummary> summaries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adoption_messages);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        btnMessagesBack = findViewById(R.id.btnMessagesBack);
        txtNoMessagesInbox = findViewById(R.id.txtNoMessagesInbox);
        messagesListContainer = findViewById(R.id.messagesListContainer);

        btnMessagesBack.setOnClickListener(v -> finish());

        loadConversations();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConversations();
    }

    private void loadConversations() {

        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        Map<String, DocumentSnapshot> combined = new HashMap<>();

        db.collection("adoptionChats").whereEqualTo("ownerId", uid).get()
                .addOnSuccessListener(ownerSnap -> {

                    for (QueryDocumentSnapshot doc : ownerSnap) {
                        combined.put(doc.getId(), doc);
                    }

                    db.collection("adoptionChats").whereEqualTo("requesterId", uid).get()
                            .addOnSuccessListener(requesterSnap -> {

                                for (QueryDocumentSnapshot doc : requesterSnap) {
                                    combined.put(doc.getId(), doc);
                                }

                                buildSummaries(combined, uid);
                            })
                            .addOnFailureListener(e -> buildSummaries(combined, uid));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void buildSummaries(Map<String, DocumentSnapshot> combined, String uid) {

        summaries.clear();

        for (Map.Entry<String, DocumentSnapshot> entry : combined.entrySet()) {

            DocumentSnapshot doc = entry.getValue();

            ChatSummary summary = new ChatSummary();
            summary.chatId = entry.getKey();
            summary.petName = doc.getString("petName");

            String ownerId = doc.getString("ownerId");
            String ownerName = doc.getString("ownerName");
            String requesterName = doc.getString("requesterName");

            summary.otherPartyName = uid.equals(ownerId)
                    ? (requesterName != null ? requesterName : "Interested user")
                    : (ownerName != null ? ownerName : "Pet owner");

            summary.lastMessageText = doc.getString("lastMessageText");
            Long ts = doc.getLong("lastMessageTimestamp");
            summary.lastMessageTimestamp = ts != null ? ts : 0L;

            summaries.add(summary);
        }

        Collections.sort(summaries, (a, b) ->
                Long.compare(b.lastMessageTimestamp, a.lastMessageTimestamp));

        renderList();
    }

    private void renderList() {

        messagesListContainer.removeAllViews();

        if (summaries.isEmpty()) {
            txtNoMessagesInbox.setVisibility(View.VISIBLE);
            return;
        }

        txtNoMessagesInbox.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        SimpleDateFormat timeFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

        for (ChatSummary summary : summaries) {

            View row = inflater.inflate(R.layout.item_adoption_message, messagesListContainer, false);

            TextView txtTitle = row.findViewById(R.id.txtMsgListTitle);
            TextView txtPreview = row.findViewById(R.id.txtMsgListPreview);
            TextView txtTime = row.findViewById(R.id.txtMsgListTime);

            txtTitle.setText(summary.otherPartyName
                    + (summary.petName != null ? " · " + summary.petName : ""));

            txtPreview.setText(summary.lastMessageText != null && !summary.lastMessageText.isEmpty()
                    ? summary.lastMessageText : "Photo attached");

            txtTime.setText(summary.lastMessageTimestamp > 0
                    ? timeFormat.format(new Date(summary.lastMessageTimestamp)) : "");

            row.setOnClickListener(v -> {
                Intent intent = new Intent(AdoptionMessagesActivity.this, AdoptionChatActivity.class);
                intent.putExtra("chatId", summary.chatId);
                intent.putExtra("petName", summary.petName);
                startActivity(intent);
            });

            messagesListContainer.addView(row);
        }
    }
}