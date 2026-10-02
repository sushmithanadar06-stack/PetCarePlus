package com.example.petcareplus.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AdoptionMessage implements Serializable {

    private String id;
    private String senderId;
    private String text;
    private String photoBase64;
    private long timestampMillis;

    // NEW — reply-to support
    private String replyToMessageId;
    private String replyToSenderName;
    private String replyToPreviewText;

    // NEW — "Delete for me" support. A uid in this list means that user
    // has hidden this message in their own view only. "Delete for
    // everyone" instead removes the whole Firestore document, so it
    // never needs this list.
    private List<String> deletedForUids;

    public AdoptionMessage() {
        this.deletedForUids = new ArrayList<>();
    }

    // Original constructor — kept as-is so existing code that builds a
    // plain message (no reply) still compiles unchanged.
    public AdoptionMessage(String id, String senderId, String text, String photoBase64, long timestampMillis) {
        this.id = id;
        this.senderId = senderId;
        this.text = text;
        this.photoBase64 = photoBase64;
        this.timestampMillis = timestampMillis;
        this.deletedForUids = new ArrayList<>();
    }

    // New constructor — used when sending a message that replies to
    // another one.
    public AdoptionMessage(String id, String senderId, String text, String photoBase64, long timestampMillis,
                           String replyToMessageId, String replyToSenderName, String replyToPreviewText) {
        this.id = id;
        this.senderId = senderId;
        this.text = text;
        this.photoBase64 = photoBase64;
        this.timestampMillis = timestampMillis;
        this.replyToMessageId = replyToMessageId;
        this.replyToSenderName = replyToSenderName;
        this.replyToPreviewText = replyToPreviewText;
        this.deletedForUids = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getPhotoBase64() { return photoBase64; }
    public void setPhotoBase64(String photoBase64) { this.photoBase64 = photoBase64; }
    public long getTimestampMillis() { return timestampMillis; }
    public void setTimestampMillis(long timestampMillis) { this.timestampMillis = timestampMillis; }

    public String getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(String replyToMessageId) { this.replyToMessageId = replyToMessageId; }
    public String getReplyToSenderName() { return replyToSenderName; }
    public void setReplyToSenderName(String replyToSenderName) { this.replyToSenderName = replyToSenderName; }
    public String getReplyToPreviewText() { return replyToPreviewText; }
    public void setReplyToPreviewText(String replyToPreviewText) { this.replyToPreviewText = replyToPreviewText; }

    public List<String> getDeletedForUids() {
        if (deletedForUids == null) deletedForUids = new ArrayList<>();
        return deletedForUids;
    }
    public void setDeletedForUids(List<String> deletedForUids) { this.deletedForUids = deletedForUids; }

    /** Convenience check used when filtering the message list for display. */
    public boolean isDeletedFor(String uid) {
        return deletedForUids != null && uid != null && deletedForUids.contains(uid);
    }

    /** True if this message is a reply to another message. */
    public boolean isReply() {
        return replyToMessageId != null && !replyToMessageId.isEmpty();
    }
}