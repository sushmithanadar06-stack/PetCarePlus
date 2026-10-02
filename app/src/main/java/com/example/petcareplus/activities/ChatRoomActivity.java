package com.example.petcareplus.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petcareplus.R;
import com.example.petcareplus.adapters.CommunityChatAdapter;
import com.example.petcareplus.models.ChatMessage;
import com.example.petcareplus.models.ChatRoom;
import com.example.petcareplus.utils.BlockedUsersHelper;
import com.example.petcareplus.utils.ChatRoomsDatabase;
import com.example.petcareplus.utils.ImageUtils;
import com.example.petcareplus.utils.NotificationsHelper;
import com.example.petcareplus.utils.ProfanityFilter;
import com.example.petcareplus.utils.ReportsHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatRoomActivity extends AppCompatActivity {

    private static final int PICK_CHAT_IMAGE = 700;
    private static final long SEND_COOLDOWN_MILLIS = 2000L;

    private TextView btnChatBack, txtChatRoomName, txtHealthDisclaimer;
    private RecyclerView recyclerChatMessages;
    private TextView txtNoMessages;
    private LinearLayout selectedPhotoPreview, replyPreviewBar;
    private ImageView imgSelectedPreview;
    private TextView btnRemovePhoto, btnAttachPhoto, btnSendMessage;
    private TextView txtReplyBarSender, txtReplyBarText, btnCancelReply;
    private EditText etChatMessage;

    private ChatRoom room;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String pendingPhotoBase64 = "";
    private ChatMessage replyingTo = null;
    private ListenerRegistration messagesListener;

    private final List<ChatMessage> allMessages = new ArrayList<>();
    private final List<ChatMessage> visibleMessages = new ArrayList<>();
    private Set<String> blockedUids = new HashSet<>();
    private CommunityChatAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        View rootView = findViewById(R.id.rootChatRoom);

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int bottomPadding = Math.max(imeInsets.bottom, systemBarInsets.bottom);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomPadding);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        btnChatBack = findViewById(R.id.btnChatBack);
        txtChatRoomName = findViewById(R.id.txtChatRoomName);
        txtHealthDisclaimer = findViewById(R.id.txtHealthDisclaimer);
        recyclerChatMessages = findViewById(R.id.recyclerChatMessages);
        txtNoMessages = findViewById(R.id.txtNoMessages);
        selectedPhotoPreview = findViewById(R.id.selectedPhotoPreview);
        imgSelectedPreview = findViewById(R.id.imgSelectedPreview);
        btnRemovePhoto = findViewById(R.id.btnRemovePhoto);
        btnAttachPhoto = findViewById(R.id.btnAttachPhoto);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        etChatMessage = findViewById(R.id.etChatMessage);
        replyPreviewBar = findViewById(R.id.replyPreviewBar);
        txtReplyBarSender = findViewById(R.id.txtReplyBarSender);
        txtReplyBarText = findViewById(R.id.txtReplyBarText);
        btnCancelReply = findViewById(R.id.btnCancelReply);

        String roomId = getIntent().getStringExtra("roomId");
        room = roomId != null ? ChatRoomsDatabase.findById(roomId) : null;

        if (room == null) {
            Toast.makeText(this, "Unable to open this room", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "You must be signed in to use Community", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (currentUser.isAnonymous()) {
            Toast.makeText(this, "Please create an account to access Community", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        txtChatRoomName.setText(room.icon + "  " + room.name);

        if (room.showHealthDisclaimer) {
            txtHealthDisclaimer.setVisibility(View.VISIBLE);
        }

        recyclerChatMessages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CommunityChatAdapter(visibleMessages, auth.getCurrentUser().getUid(), this::showMessageOptions);
        recyclerChatMessages.setAdapter(adapter);

        btnChatBack.setOnClickListener(v -> finish());
        btnAttachPhoto.setOnClickListener(v -> openImagePicker());
        btnRemovePhoto.setOnClickListener(v -> clearPendingPhoto());
        btnSendMessage.setOnClickListener(v -> sendMessage());
        btnCancelReply.setOnClickListener(v -> cancelReply());

        loadBlockedUidsThenMessages();
    }

    private long getCutoffMillis() {
        return System.currentTimeMillis() - ((long) room.retentionDays * 24 * 60 * 60 * 1000L);
    }

    private void loadBlockedUidsThenMessages() {
        BlockedUsersHelper.getBlockedUids(db, auth.getCurrentUser().getUid(), ids -> {
            blockedUids = ids;
            cleanupThenLoad();
        });
    }

    private void cleanupThenLoad() {
        long cutoff = getCutoffMillis();

        db.collection("communityRooms").document(room.id)
                .collection("messages")
                .whereLessThan("timestampMillis", cutoff)
                .get()
                .addOnSuccessListener(expiredSnap -> {
                    if (expiredSnap.isEmpty()) { loadMessages(); return; }
                    for (QueryDocumentSnapshot doc : expiredSnap) doc.getReference().delete();
                    loadMessages();
                })
                .addOnFailureListener(e -> loadMessages());
    }

    /**
     * FIX: this used to be a one-time .get() - a message someone else sent only showed up
     * after leaving and reopening the room. Now it's a live listener, so messages appear
     * instantly for everyone with the room open.
     */
    private void loadMessages() {
        long cutoff = getCutoffMillis();

        if (messagesListener != null) messagesListener.remove();

        messagesListener = db.collection("communityRooms").document(room.id)
                .collection("messages")
                .whereGreaterThanOrEqualTo("timestampMillis", cutoff)
                .addSnapshotListener((querySnapshot, e) -> {

                    if (e != null) {
                        Toast.makeText(this, "Failed to load messages: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (querySnapshot == null) return;

                    allMessages.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        ChatMessage msg = doc.toObject(ChatMessage.class);
                        msg.setId(doc.getId());
                        allMessages.add(msg);
                    }
                    Collections.sort(allMessages, (a, b) -> Long.compare(a.getTimestampMillis(), b.getTimestampMillis()));
                    applyBlockFilter();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) messagesListener.remove();
    }

    private void applyBlockFilter() {

        String myUid = auth.getCurrentUser().getUid();
        visibleMessages.clear();

        for (ChatMessage msg : allMessages) {
            if (!blockedUids.contains(msg.getSenderId()) && !msg.isDeletedFor(myUid)) {
                visibleMessages.add(msg);
            }
        }

        txtNoMessages.setVisibility(visibleMessages.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (!visibleMessages.isEmpty()) {
            recyclerChatMessages.post(() -> recyclerChatMessages.scrollToPosition(visibleMessages.size() - 1));
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_CHAT_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_CHAT_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            String base64 = ImageUtils.uriToCompressedBase64(this, imageUri);

            if (base64.isEmpty()) {
                Toast.makeText(this, "Unable to process photo", Toast.LENGTH_SHORT).show();
                return;
            }

            pendingPhotoBase64 = base64;
            android.graphics.Bitmap preview = ImageUtils.base64ToBitmap(base64);
            if (preview != null) imgSelectedPreview.setImageBitmap(preview);
            selectedPhotoPreview.setVisibility(View.VISIBLE);
        }
    }

    private void clearPendingPhoto() {
        pendingPhotoBase64 = "";
        imgSelectedPreview.setImageDrawable(null);
        selectedPhotoPreview.setVisibility(View.GONE);
    }

    private void startReply(ChatMessage msg) {
        replyingTo = msg;
        txtReplyBarSender.setText(msg.getSenderId().equals(auth.getCurrentUser().getUid()) ? "You" : msg.getSenderName());
        txtReplyBarText.setText(
                msg.getText() != null && !msg.getText().isEmpty() ? msg.getText() : "📷 Photo"
        );
        replyPreviewBar.setVisibility(View.VISIBLE);
        etChatMessage.requestFocus();
    }

    private void cancelReply() {
        replyingTo = null;
        replyPreviewBar.setVisibility(View.GONE);
    }

    private void sendMessage() {

        String text = etChatMessage.getText().toString().trim();

        if (text.isEmpty() && pendingPhotoBase64.isEmpty()) {
            Toast.makeText(this, "Type a message or attach a photo", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ProfanityFilter.containsProfanity(text)) {
            Toast.makeText(this, "Please remove inappropriate language before sending", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        String senderName = user.getDisplayName();
        if (senderName == null || senderName.trim().isEmpty()) {
            senderName = user.isAnonymous() ? "Guest" : "Pet Parent";
        }

        btnSendMessage.setEnabled(false);
        btnSendMessage.postDelayed(() -> btnSendMessage.setEnabled(true), SEND_COOLDOWN_MILLIS);

        DocumentReference newRef = db.collection("communityRooms")
                .document(room.id)
                .collection("messages")
                .document();

        ChatMessage message;

        // Module 13: capture who we're replying to BEFORE cancelReply() clears it
        final String replyToRecipientId = (replyingTo != null) ? replyingTo.getSenderId() : null;
        final String finalSenderName = senderName;

        if (replyingTo != null) {
            String replySenderLabel = replyingTo.getSenderId().equals(user.getUid()) ? "You" : replyingTo.getSenderName();
            String replyPreview = replyingTo.getText() != null && !replyingTo.getText().isEmpty()
                    ? replyingTo.getText() : "📷 Photo";

            message = new ChatMessage(
                    newRef.getId(), room.id, user.getUid(), senderName,
                    text, pendingPhotoBase64, System.currentTimeMillis(),
                    replyingTo.getId(), replySenderLabel, replyPreview
            );
        } else {
            message = new ChatMessage(
                    newRef.getId(), room.id, user.getUid(), senderName,
                    text, pendingPhotoBase64, System.currentTimeMillis()
            );
        }

        newRef.set(message)
                .addOnSuccessListener(unused -> {

                    if (replyToRecipientId != null) {
                        NotificationsHelper.notifyCommunityReply(
                                db, replyToRecipientId, room.id, room.name, finalSenderName);
                    }

                    // Module 13: notify everyone who's previously sent a message in this room
                    // (that's the closest thing this room has to a "member list" - a plain
                    // public room with no fixed membership can't sensibly notify literally
                    // every app user on every message).
                    registerAsParticipantThenNotifyOthers(finalSenderName);

                    etChatMessage.setText("");
                    clearPendingPhoto();
                    cancelReply();
                    loadMessages();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /**
     * Adds me to this room's participant list (so future messages notify me too), then reads
     * the full list and notifies everyone else on it about this message.
     */
    private void registerAsParticipantThenNotifyOthers(String senderName) {

        String myUid = auth.getCurrentUser().getUid();

        db.collection("communityRooms").document(room.id)
                .set(java.util.Collections.singletonMap("participantIds",
                        FieldValue.arrayUnion(myUid)), SetOptions.merge())
                .addOnSuccessListener(unused ->
                        db.collection("communityRooms").document(room.id).get()
                                .addOnSuccessListener(doc -> {

                                    List<String> ids = (List<String>) doc.get("participantIds");
                                    if (ids == null) return;

                                    NotificationsHelper.notifyCommunityMessage(
                                            db, ids, room.id, room.name, senderName);
                                })
                );
    }

    private void showMessageOptions(ChatMessage msg) {

        String myUid = auth.getCurrentUser().getUid();
        boolean isMine = myUid.equals(msg.getSenderId());

        if (isMine) {

            String[] options = {"Reply", "Delete for Everyone"};

            new AlertDialog.Builder(this)
                    .setTitle("Message options")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) startReply(msg);
                        else confirmDeleteForEveryone(msg);
                    })
                    .show();

        } else {

            String[] options = {"Reply", "Delete for Me", "Report Message", "Block " + msg.getSenderName()};

            new AlertDialog.Builder(this)
                    .setTitle(msg.getSenderName())
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) startReply(msg);
                        else if (which == 1) deleteForMe(msg);
                        else if (which == 2) reportMessage(msg);
                        else confirmBlockUser(msg);
                    })
                    .show();
        }
    }

    private void confirmDeleteForEveryone(ChatMessage msg) {
        new AlertDialog.Builder(this)
                .setTitle("Delete for everyone?")
                .setMessage("This will remove it for everyone in this room.")
                .setPositiveButton("Delete", (dialog, which) -> deleteForEveryone(msg))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteForEveryone(ChatMessage msg) {
        db.collection("communityRooms").document(room.id)
                .collection("messages").document(msg.getId())
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                    loadMessages();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void deleteForMe(ChatMessage msg) {

        String myUid = auth.getCurrentUser().getUid();

        db.collection("communityRooms").document(room.id)
                .collection("messages").document(msg.getId())
                .update("deletedForUids", FieldValue.arrayUnion(myUid))
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Removed from your chat", Toast.LENGTH_SHORT).show();
                    loadMessages();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void reportMessage(ChatMessage msg) {

        FirebaseUser user = auth.getCurrentUser();
        String reporterName = user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                ? user.getDisplayName() : "A user";

        ReportsHelper.submitReport(
                db, user.getUid(), reporterName,
                msg.getSenderId(), msg.getSenderName(),
                "message", msg.getId(), msg.getText(), "Reported from " + room.name,
                success -> {
                    if (success) Toast.makeText(this, "Reported — thank you, we'll review it", Toast.LENGTH_SHORT).show();
                    else Toast.makeText(this, "Failed to submit report", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void confirmBlockUser(ChatMessage msg) {

        new AlertDialog.Builder(this)
                .setTitle("Block " + msg.getSenderName() + "?")
                .setMessage("You won't see their messages or listings anymore, and they can't message you.")
                .setPositiveButton("Block", (dialog, which) -> {
                    BlockedUsersHelper.blockUser(
                            db, auth.getCurrentUser().getUid(),
                            msg.getSenderId(), msg.getSenderName(),
                            success -> {
                                if (success) {
                                    Toast.makeText(this, msg.getSenderName() + " blocked", Toast.LENGTH_SHORT).show();
                                    loadBlockedUidsThenMessages();
                                } else {
                                    Toast.makeText(this, "Failed to block", Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}