package com.example.petcareplus.models;

import java.io.Serializable;

/**
 * One notification, stored in the "notifications" collection.
 *
 * type values (see NotificationsHelper): adoption_interest, adoption_adopted,
 * adoption_closed, rescue_volunteer, rescue_helped, rescue_withdrawn, rescue_rescued.
 *
 * petName holds the "subject" of the notification: the pet's name for adoption,
 * or "Rescue #ABCD" for rescue reports.
 */
public class AppNotification implements Serializable {

    private String id;
    private String recipientId;
    private String type;
    private String listingId;
    private String petName;
    private String fromName;
    private long timestampMillis;
    private boolean read;

    // NEW (Module 13)
    private String targetId;   // what to open when tapped: rescue report id or adoption listing id
    private String fromId;     // uid of the person who caused it (checked by the Firestore rules)

    public AppNotification() {}

    // Original constructor - kept so existing code still compiles.
    public AppNotification(String id, String recipientId, String type, String listingId,
                           String petName, String fromName, long timestampMillis, boolean read) {
        this.id = id;
        this.recipientId = recipientId;
        this.type = type;
        this.listingId = listingId;
        this.petName = petName;
        this.fromName = fromName;
        this.timestampMillis = timestampMillis;
        this.read = read;
    }

    public AppNotification(String id, String recipientId, String type, String listingId,
                           String petName, String fromName, long timestampMillis, boolean read,
                           String targetId, String fromId) {
        this(id, recipientId, type, listingId, petName, fromName, timestampMillis, read);
        this.targetId = targetId;
        this.fromId = fromId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
    public long getTimestampMillis() { return timestampMillis; }
    public void setTimestampMillis(long timestampMillis) { this.timestampMillis = timestampMillis; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    /** Old adoption notifications only have listingId, so fall back to it. */
    public String getTargetId() { return targetId != null ? targetId : listingId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getFromId() { return fromId; }
    public void setFromId(String fromId) { this.fromId = fromId; }
}