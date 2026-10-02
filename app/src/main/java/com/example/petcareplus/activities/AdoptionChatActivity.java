package com.example.petcareplus.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petcareplus.R;
import com.example.petcareplus.adapters.AdoptionChatAdapter;
import com.example.petcareplus.models.AdoptionMessage;
import com.example.petcareplus.utils.BlockedUsersHelper;
import com.example.petcareplus.utils.ImageUtils;
import com.example.petcareplus.utils.NotificationsHelper;
import com.example.petcareplus.utils.ProfanityFilter;
import com.example.petcareplus.utils.ReportsHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdoptionChatActivity extends AppCompatActivity {

    private static final long SEND_COOLDOWN_MILLIS = 2000L;

    private View chatHeaderBar;
    private TextView btnChatBack, txtChatTitle, txtChatSubtitle, txtChatAvatarInitial,
            btnChatMenu, btnAttachPhoto, btnSendMessage, txtPhotoPreviewLabel;
    private ImageView imgChatAvatarPhoto;
    private View btnChatProfileRow;
    private LinearLayout replyPreviewBar;
    private TextView txtReplyBarSender, txtReplyBarText, btnCancelReply;

    private EditText etChatMessage;
    private RecyclerView recyclerChatMessages;

    private String chatId;
    private String petName;
    private String otherPartyUid = "";
    private String otherPartyName = "this user";
    private String pendingPhotoBase64 = "";
    private AdoptionMessage replyingTo = null;
    private ListenerRegistration messagesListener;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final List<AdoptionMessage> allMessages = new ArrayList<>();
    private final List<AdoptionMessage> visibleMessages = new ArrayList<>();
    private Set<String> blockedUids = new HashSet<>();
    private AdoptionChatAdapter adapter;

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            String base64 = ImageUtils.uriToCompressedBase64(this, uri);
                            if (!base64.isEmpty()) {
                                pendingPhotoBase64 = base64;
                                txtPhotoPreviewLabel.setVisibility(View.VISIBLE);
                            }
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adoption_chat);

        View rootView = findViewById(R.id.rootAdoptionChat);
        chatHeaderBar = findViewById(R.id.chatHeaderBar);

        int extraHeaderTopPaddingPx = (int) (14 * getResources().getDisplayMetrics().density);
        int headerBaseTopPaddingPx = chatHeaderBar.getPaddingTop();

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {

            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            int bottomPadding = Math.max(imeInsets.bottom, systemBarInsets.bottom);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomPadding);

            chatHeaderBar.setPadding(
                    chatHeaderBar.getPaddingLeft(),
                    systemBarInsets.top + extraHeaderTopPaddingPx + headerBaseTopPaddingPx,
                    chatHeaderBar.getPaddingRight(),
                    chatHeaderBar.getPaddingBottom()
            );

            return insets;
        });

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        btnChatBack = findViewById(R.id.btnChatBack);
        txtChatTitle = findViewById(R.id.txtChatTitle);
        txtChatSubtitle = findViewById(R.id.txtChatSubtitle);
        txtChatAvatarInitial = findViewById(R.id.txtChatAvatarInitial);
        imgChatAvatarPhoto = findViewById(R.id.imgChatAvatarPhoto);
        btnChatMenu = findViewById(R.id.btnChatMenu);
        btnChatProfileRow = findViewById(R.id.btnChatProfileRow);
        recyclerChatMessages = findViewById(R.id.recyclerChatMessages);
        etChatMessage = findViewById(R.id.etChatMessage);
        btnAttachPhoto = findViewById(R.id.btnAttachPhoto);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        txtPhotoPreviewLabel = findViewById(R.id.txtPhotoPreviewLabel);
        replyPreviewBar = findViewById(R.id.replyPreviewBar);
        txtReplyBarSender = findViewById(R.id.txtReplyBarSender);
        txtReplyBarText = findViewById(R.id.txtReplyBarText);
        btnCancelReply = findViewById(R.id.btnCancelReply);

        chatId = getIntent().getStringExtra("chatId");
        petName = getIntent().getStringExtra("petName");

        if (chatId == null || auth.getCurrentUser() == null) {
            Toast.makeText(this, "Unable to load chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (auth.getCurrentUser().isAnonymous()) {
            Toast.makeText(this, "Please create an account to access Community", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        txtChatTitle.setText(petName != null ? petName : "Chat");
        txtChatSubtitle.setText("Adoption chat");

        recyclerChatMessages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdoptionChatAdapter(visibleMessages, auth.getCurrentUser().getUid(), this::showMessageOptions);
        recyclerChatMessages.setAdapter(adapter);

        btnChatBack.setOnClickListener(v -> finish());
        btnChatMenu.setOnClickListener(v -> showChatMenu());

        btnChatProfileRow.setOnClickListener(v -> {
            Intent intent = new Intent(this, OtherUserProfileActivity.class);
            intent.putExtra("otherUid", otherPartyUid);
            intent.putExtra("otherName", otherPartyName);
            startActivity(intent);
        });

        btnAttachPhoto.setOnClickListener(v -> imagePicker.launch("image/*"));

        txtPhotoPreviewLabel.setOnClickListener(v -> {
            pendingPhotoBase64 = "";
            txtPhotoPreviewLabel.setVisibility(View.GONE);
        });

        btnCancelReply.setOnClickListener(v -> cancelReply());

        btnSendMessage.setOnClickListener(v -> sendMessage());

        loadChatMetaThenMessages();
    }

    private void loadChatMetaThenMessages() {

        String myUid = auth.getCurrentUser().getUid();

        db.collection("adoptionChats").document(chatId).get()
                .addOnSuccessListener(chatDoc -> {

                    String ownerId = chatDoc.getString("ownerId");
                    String ownerName = chatDoc.getString("ownerName");
                    String requesterId = chatDoc.getString("requesterId");
                    String requesterName = chatDoc.getString("requesterName");

                    boolean iAmOwner = myUid.equals(ownerId);

                    otherPartyUid = iAmOwner
                            ? (requesterId != null ? requesterId : "")
                            : (ownerId != null ? ownerId : "");

                    String storedOtherName = iAmOwner ? requesterName : ownerName;

                    boolean nameLooksValid = storedOtherName != null
                            && !storedOtherName.trim().isEmpty()
                            && !storedOtherName.equalsIgnoreCase("this user");

                    otherPartyName = nameLooksValid ? storedOtherName : "this user";

                    updateHeader();
                    loadOtherPartyPublicProfile(iAmOwner, nameLooksValid);
                })
                .addOnFailureListener(e -> loadBlockedUidsThenMessages());
    }

    private void loadOtherPartyPublicProfile(boolean iAmOwner, boolean nameAlreadyValid) {

        if (otherPartyUid.isEmpty()) {
            loadBlockedUidsThenMessages();
            return;
        }

        db.collection("publicProfiles").document(otherPartyUid).get()
                .addOnSuccessListener(profileDoc -> {

                    if (!nameAlreadyValid) {
                        String realName = profileDoc.getString("name");
                        if (realName != null && !realName.trim().isEmpty()) {

                            otherPartyName = realName;
                            updateHeader();

                            Map<String, Object> patch = new HashMap<>();
                            patch.put(iAmOwner ? "requesterName" : "ownerName", realName);
                            db.collection("adoptionChats").document(chatId).set(patch, SetOptions.merge());
                        }
                    }

                    applyOtherPartyPhoto(profileDoc);
                    loadBlockedUidsThenMessages();
                })
                .addOnFailureListener(e -> loadBlockedUidsThenMessages());
    }

    private void updateHeader() {

        txtChatTitle.setText(otherPartyName);
        txtChatSubtitle.setText(petName != null ? "About " + petName : "Adoption chat");

        String initial = (otherPartyName != null && !otherPartyName.trim().isEmpty())
                ? otherPartyName.trim().substring(0, 1).toUpperCase() : "?";
        txtChatAvatarInitial.setText(initial);
    }

    private void applyOtherPartyPhoto(DocumentSnapshot profileDoc) {

        String photoBase64 = profileDoc.getString("photoUri");

        if (photoBase64 == null || photoBase64.isEmpty() || !ImageUtils.isBase64Image(photoBase64)) {
            return;
        }

        Bitmap bitmap = ImageUtils.base64ToBitmap(photoBase64);

        if (bitmap != null) {
            txtChatAvatarInitial.setVisibility(View.GONE);
            imgChatAvatarPhoto.setVisibility(View.VISIBLE);
            imgChatAvatarPhoto.setImageBitmap(bitmap);
        }
    }

    private void loadBlockedUidsThenMessages() {
        BlockedUsersHelper.getBlockedUids(db, auth.getCurrentUser().getUid(), ids -> {
            blockedUids = ids;
            loadMessages();
        });
    }

    /**
     * FIX: this used to be a one-time .get() - the other person's messages only appeared
     * after leaving and reopening the chat. Now it's a live listener, so new messages
     * appear instantly on both sides while the chat is open.
     */
    private void loadMessages() {

        if (messagesListener != null) messagesListener.remove();

        messagesListener = db.collection("adoptionChats").document(chatId)
                .collection("messages")
                .addSnapshotListener((querySnapshot, e) -> {

                    if (e != null) {
                        Toast.makeText(this, "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (querySnapshot == null) return;

                    allMessages.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        AdoptionMessage message = doc.toObject(AdoptionMessage.class);
                        message.setId(doc.getId());
                        allMessages.add(message);
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

        for (AdoptionMessage msg : allMessages) {
            if (!blockedUids.contains(msg.getSenderId()) && !msg.isDeletedFor(myUid)) {
                visibleMessages.add(msg);
            }
        }

        adapter.notifyDataSetChanged();
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (!visibleMessages.isEmpty()) {
            recyclerChatMessages.post(() -> recyclerChatMessages.scrollToPosition(visibleMessages.size() - 1));
        }
    }

    private void startReply(AdoptionMessage msg) {
        replyingTo = msg;
        boolean isMine = msg.getSenderId().equals(auth.getCurrentUser().getUid());
        txtReplyBarSender.setText(isMine ? "You" : otherPartyName);
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
            return;
        }

        if (ProfanityFilter.containsProfanity(text)) {
            Toast.makeText(this, "Please remove inappropriate language before sending", Toast.LENGTH_LONG).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        btnSendMessage.setEnabled(false);
        btnSendMessage.postDelayed(() -> btnSendMessage.setEnabled(true), SEND_COOLDOWN_MILLIS);

        DocumentReference newRef = db.collection("adoptionChats")
                .document(chatId)
                .collection("messages")
                .document();

        AdoptionMessage message;

        if (replyingTo != null) {
            boolean replyIsMine = replyingTo.getSenderId().equals(uid);
            String replySenderLabel = replyIsMine ? "You" : otherPartyName;
            String replyPreview = replyingTo.getText() != null && !replyingTo.getText().isEmpty()
                    ? replyingTo.getText() : "📷 Photo";

            message = new AdoptionMessage(
                    newRef.getId(), uid, text, pendingPhotoBase64, System.currentTimeMillis(),
                    replyingTo.getId(), replySenderLabel, replyPreview
            );
        } else {
            message = new AdoptionMessage(
                    newRef.getId(), uid, text, pendingPhotoBase64, System.currentTimeMillis()
            );
        }

        newRef.set(message)
                .addOnSuccessListener(unused -> {

                    Map<String, Object> patch = new HashMap<>();
                    patch.put("lastMessageText", text.isEmpty() ? "📷 Photo" : text);
                    patch.put("lastMessageTimestamp", System.currentTimeMillis());
                    patch.put("lastSenderId", uid);

                    db.collection("adoptionChats").document(chatId).set(patch, SetOptions.merge());

                    // Module 13: let the other person know a message arrived
                    NotificationsHelper.notifyChatMessage(
                            db, otherPartyUid, chatId,
                            petName != null ? petName : "your chat",
                            NotificationsHelper.currentUserName());

                    etChatMessage.setText("");
                    pendingPhotoBase64 = "";
                    txtPhotoPreviewLabel.setVisibility(View.GONE);
                    cancelReply();
                    loadMessages();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showMessageOptions(AdoptionMessage msg) {

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

            String[] options = {"Reply", "Delete for Me"};

            new AlertDialog.Builder(this)
                    .setTitle(otherPartyName)
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) startReply(msg);
                        else deleteForMe(msg);
                    })
                    .show();
        }
    }

    private void confirmDeleteForEveryone(AdoptionMessage msg) {
        new AlertDialog.Builder(this)
                .setTitle("Delete for everyone?")
                .setMessage("This removes it from the whole conversation.")
                .setPositiveButton("Delete", (dialog, which) -> deleteForEveryone(msg))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteForEveryone(AdoptionMessage msg) {
        db.collection("adoptionChats").document(chatId)
                .collection("messages").document(msg.getId())
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                    loadMessages();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void deleteForMe(AdoptionMessage msg) {

        String myUid = auth.getCurrentUser().getUid();

        db.collection("adoptionChats").document(chatId)
                .collection("messages").document(msg.getId())
                .update("deletedForUids", FieldValue.arrayUnion(myUid))
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Removed from your chat", Toast.LENGTH_SHORT).show();
                    loadMessages();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showChatMenu() {

        String[] options = {"Report " + otherPartyName, "Block " + otherPartyName};

        new AlertDialog.Builder(this)
                .setTitle(otherPartyName)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) reportUser();
                    else confirmBlockUser();
                })
                .show();
    }

    private void reportUser() {

        FirebaseUser user = auth.getCurrentUser();
        String reporterName = user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                ? user.getDisplayName() : "A user";

        String preview = !visibleMessages.isEmpty()
                ? visibleMessages.get(visibleMessages.size() - 1).getText() : "";

        ReportsHelper.submitReport(
                db, user.getUid(), reporterName,
                otherPartyUid, otherPartyName,
                "user", chatId, preview, "Reported from adoption chat menu",
                success -> {
                    if (success) Toast.makeText(this, "Reported — thank you, we'll review it", Toast.LENGTH_SHORT).show();
                    else Toast.makeText(this, "Failed to submit report", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void confirmBlockUser() {

        if (otherPartyUid.isEmpty()) {
            Toast.makeText(this, "Unable to identify this user", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Block " + otherPartyName + "?")
                .setMessage("You won't see their messages or listings anymore, and they can't message you.")
                .setPositiveButton("Block", (dialog, which) -> {
                    BlockedUsersHelper.blockUser(
                            db, auth.getCurrentUser().getUid(),
                            otherPartyUid, otherPartyName,
                            success -> {
                                if (success) {
                                    Toast.makeText(this, otherPartyName + " blocked", Toast.LENGTH_SHORT).show();
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