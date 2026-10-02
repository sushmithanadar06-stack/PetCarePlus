package com.example.petcareplus.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.petcareplus.activities.AdoptionAllRequestsActivity;
import com.example.petcareplus.activities.AdoptionChatActivity;
import com.example.petcareplus.activities.AdoptionListingsActivity;
import com.example.petcareplus.activities.ChatRoomActivity;
import com.example.petcareplus.activities.PetListActivity;
import com.example.petcareplus.activities.RescueDetailActivity;
import com.example.petcareplus.models.AppNotification;
import com.example.petcareplus.models.RescueReport;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class NotificationsHelper {

    private static final String PREFS_NAME = "petcare_notifications";
    private static Context appContext;

    /** Call once, from PetCareApp.onCreate(), so send() can check the person's on/off switches. */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    /**
     * "Community Activity" covers adoption notifications, "Rescue Alerts" covers rescue
     * notifications. Defaults to ON (matches NotificationPreferencesActivity's defaults),
     * and stays ON if init() was never called, so nothing silently stops sending.
     */
    private static boolean isEnabledFor(String type) {

        if (appContext == null) return true;

        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (type != null && (type.startsWith("adoption_") || type.startsWith("community_"))) {
            return prefs.getBoolean("community", true);
        }
        if (type != null && type.startsWith("rescue_")) {
            return prefs.getBoolean("rescue", true);
        }
        return true;
    }

    // ---- notification types -------------------------------------------------
    public static final String TYPE_ADOPTION_INTEREST = "adoption_interest";
    public static final String TYPE_ADOPTION_ADOPTED = "adoption_adopted";
    public static final String TYPE_ADOPTION_CLOSED = "adoption_closed";
    public static final String TYPE_RESCUE_VOLUNTEER = "rescue_volunteer";
    public static final String TYPE_RESCUE_HELPED = "rescue_helped";
    public static final String TYPE_RESCUE_WITHDRAWN = "rescue_withdrawn";
    public static final String TYPE_RESCUE_RESCUED = "rescue_rescued";
    public static final String TYPE_RESCUE_DELETED = "rescue_deleted";
    public static final String TYPE_ADOPTION_DELETED = "adoption_deleted";
    public static final String TYPE_ADOPTION_MESSAGE = "adoption_message";
    public static final String TYPE_COMMUNITY_REPLY = "community_reply";
    public static final String TYPE_COMMUNITY_MESSAGE = "community_message";

    public interface CountCallback {
        void onCount(int unreadCount);
    }

    // =========================================================================
    // SENDING
    // =========================================================================

    /**
     * Sends one notification. Never notifies yourself, and does nothing when
     * nobody is signed in.
     *
     * @param listingId only for adoption notifications (kept for the old badge code), else null
     * @param targetId  what opens when the notification is tapped (rescue report id / listing id)
     * @param subject   pet name, or "Rescue #ABCD"
     */
    private static void send(FirebaseFirestore db, String recipientId, String type,
                             String listingId, String targetId, String subject, String fromName) {

        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();

        if (me == null || recipientId == null || recipientId.isEmpty()
                || recipientId.equals(me.getUid())) {
            return;
        }

        if (!isEnabledFor(type)) return;

        DocumentReference ref = db.collection("notifications").document();

        AppNotification notification = new AppNotification(
                ref.getId(), recipientId, type, listingId,
                subject, fromName, System.currentTimeMillis(), false,
                targetId, me.getUid()
        );

        // IMPORTANT: this used to fail silently. If a notification isn't showing up,
        // check Logcat for "NotifSend" - it will show the exact Firestore error
        // (almost always PERMISSION_DENIED if the rules reject it).
        ref.set(notification)
                .addOnSuccessListener(v -> Log.d("NotifSend",
                        "sent " + type + " to " + recipientId))
                .addOnFailureListener(e -> Log.e("NotifSend",
                        "FAILED to send " + type + " to " + recipientId + ": " + e.getMessage(), e));
    }

    /** Adoption: someone is interested in the owner's pet (existing method, same signature). */
    public static void notifyAdoptionInterest(FirebaseFirestore db, String ownerId, String listingId,
                                              String petName, String fromName) {

        send(db, ownerId, TYPE_ADOPTION_INTEREST, listingId, listingId, petName, fromName);
    }

    /**
     * Adoption: the owner marked the pet as adopted.
     * The chosen adopter gets "adopted", everybody else who asked gets "closed".
     */
    public static void notifyAdoptionResult(FirebaseFirestore db, String listingId, String petName,
                                            String ownerName, String adopterId,
                                            List<String> allRequesterIds) {

        if (adopterId != null && !adopterId.isEmpty()) {
            send(db, adopterId, TYPE_ADOPTION_ADOPTED, listingId, listingId, petName, ownerName);
        }

        Set<String> others = new LinkedHashSet<>(allRequesterIds);
        if (adopterId != null) others.remove(adopterId);

        for (String uid : others) {
            send(db, uid, TYPE_ADOPTION_CLOSED, listingId, listingId, petName, ownerName);
        }
    }

    /** Rescue: tell the person who reported it (type = volunteer / helped / withdrawn). */
    public static void notifyRescueReporter(FirebaseFirestore db, RescueReport r, String type) {
        if (r == null) return;
        send(db, r.reporterId, type, null, r.id, "Rescue #" + r.shortCode(), currentUserName());
    }

    /** Rescue: the animal is safe -> tell every volunteer who offered or gave help. */
    public static void notifyRescueVolunteers(FirebaseFirestore db, RescueReport r) {
        if (r == null) return;

        Set<String> ids = new LinkedHashSet<>(r.helperIds);
        ids.addAll(r.helpedIds);

        for (String uid : ids) {
            send(db, uid, TYPE_RESCUE_RESCUED, null, r.id, "Rescue #" + r.shortCode(), currentUserName());
        }
    }

    /** Rescue: the report was deleted -> tell every volunteer who offered or gave help. */
    public static void notifyRescueDeleted(FirebaseFirestore db, RescueReport r) {
        if (r == null) return;

        Set<String> ids = new LinkedHashSet<>(r.helperIds);
        ids.addAll(r.helpedIds);

        for (String uid : ids) {
            send(db, uid, TYPE_RESCUE_DELETED, null, r.id, "Rescue #" + r.shortCode(), currentUserName());
        }
    }

    /** Adoption: the listing was deleted/withdrawn -> tell everyone who had asked about it. */
    public static void notifyAdoptionDeleted(FirebaseFirestore db, String listingId, String petName,
                                             String ownerName, List<String> allRequesterIds) {
        for (String uid : new LinkedHashSet<>(allRequesterIds)) {
            send(db, uid, TYPE_ADOPTION_DELETED, listingId, listingId, petName, ownerName);
        }
    }

    /** Community: a new message in a 1:1 adoption chat. targetId carries the chatId. */
    public static void notifyChatMessage(FirebaseFirestore db, String recipientId, String chatId,
                                         String petName, String fromName) {
        send(db, recipientId, TYPE_ADOPTION_MESSAGE, null, chatId, petName, fromName);
    }

    /** Community: someone replied to your message in a chat room. targetId carries the roomId. */
    public static void notifyCommunityReply(FirebaseFirestore db, String recipientId, String roomId,
                                            String roomName, String fromName) {
        send(db, recipientId, TYPE_COMMUNITY_REPLY, null, roomId, roomName, fromName);
    }

    /**
     * Community: a new plain message in a chat room -> notify every OTHER participant
     * who has sent a message in this room before (recipientIds may include the sender;
     * send() already skips notifying yourself).
     */
    public static void notifyCommunityMessage(FirebaseFirestore db, List<String> recipientIds,
                                              String roomId, String roomName, String fromName) {
        for (String uid : new LinkedHashSet<>(recipientIds)) {
            send(db, uid, TYPE_COMMUNITY_MESSAGE, null, roomId, roomName, fromName);
        }
    }

    public static String currentUserName() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return "Someone";
        if (u.isAnonymous()) return "A guest";
        String name = u.getDisplayName();
        return (name != null && !name.trim().isEmpty()) ? name.trim() : "Someone";
    }

    // =========================================================================
    // READING / MARKING
    // =========================================================================

    /** Live unread count (used for the bell badge). Call remove() on the result when done. */
    public static ListenerRegistration listenUnreadCount(FirebaseFirestore db, String uid,
                                                         CountCallback callback) {
        return db.collection("notifications")
                .whereEqualTo("recipientId", uid)
                .whereEqualTo("read", false)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    callback.onCount(snap.size());
                });
    }

    public static void markRead(FirebaseFirestore db, String notificationId) {
        if (notificationId == null) return;
        db.collection("notifications").document(notificationId).update("read", true);
    }

    public static void markAllRead(FirebaseFirestore db, String uid) {

        if (uid == null) return;

        db.collection("notifications")
                .whereEqualTo("recipientId", uid)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) return;
                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : query) {
                        batch.update(doc.getReference(), "read", true);
                    }
                    batch.commit();
                });
    }

    // ---- existing adoption-badge methods (unchanged) -------------------------

    public static void getUnreadAdoptionCount(FirebaseFirestore db, String uid, CountCallback callback) {

        if (uid == null) {
            callback.onCount(0);
            return;
        }

        db.collection("notifications")
                .whereEqualTo("recipientId", uid)
                .whereEqualTo("type", TYPE_ADOPTION_INTEREST)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(query -> callback.onCount(query.size()))
                .addOnFailureListener(e -> callback.onCount(0));
    }

    public static void markAdoptionNotificationsRead(FirebaseFirestore db, String uid) {

        if (uid == null) return;

        db.collection("notifications")
                .whereEqualTo("recipientId", uid)
                .whereEqualTo("type", TYPE_ADOPTION_INTEREST)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) return;
                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : query) {
                        batch.update(doc.getReference(), "read", true);
                    }
                    batch.commit();
                });
    }

    // =========================================================================
    // TEXT, ICON AND TARGET SCREEN
    // =========================================================================

    public static String titleFor(AppNotification n) {
        switch (safe(n.getType())) {
            case TYPE_ADOPTION_INTEREST: return "New adoption request";
            case TYPE_ADOPTION_ADOPTED:  return "Adoption confirmed \uD83C\uDF89";
            case TYPE_ADOPTION_CLOSED:   return "This pet found a home";
            case TYPE_RESCUE_VOLUNTEER:  return "A volunteer is on the way";
            case TYPE_RESCUE_HELPED:     return "A volunteer helped";
            case TYPE_RESCUE_WITHDRAWN:  return "A volunteer stepped back";
            case TYPE_RESCUE_RESCUED:    return "Animal rescued \uD83D\uDC3E";
            case TYPE_RESCUE_DELETED:    return "Rescue report removed";
            case TYPE_ADOPTION_DELETED:  return "Listing no longer available";
            case TYPE_ADOPTION_MESSAGE:  return "New message";
            case TYPE_COMMUNITY_REPLY:   return "New reply";
            case TYPE_COMMUNITY_MESSAGE: return "New message";
            default:                     return "PetCare+";
        }
    }

    public static String messageFor(AppNotification n) {
        String from = n.getFromName() != null && !n.getFromName().isEmpty() ? n.getFromName() : "Someone";
        String subject = n.getPetName() != null ? n.getPetName() : "";

        switch (safe(n.getType())) {
            case TYPE_ADOPTION_INTEREST:
                return from + " is interested in adopting " + subject;
            case TYPE_ADOPTION_ADOPTED:
                return "Congratulations! " + from + " confirmed that you adopted " + subject + ".";
            case TYPE_ADOPTION_CLOSED:
                return subject + " has been adopted. Thank you for your interest!";
            case TYPE_RESCUE_VOLUNTEER:
                return from + " is helping with " + subject;
            case TYPE_RESCUE_HELPED:
                return from + " has finished helping with " + subject;
            case TYPE_RESCUE_WITHDRAWN:
                return from + " can no longer help with " + subject + ". It may still need a volunteer.";
            case TYPE_RESCUE_RESCUED:
                return subject + " is rescued and safe. Thank you for helping!";
            case TYPE_RESCUE_DELETED:
                return "The reporter removed " + subject + ".";
            case TYPE_ADOPTION_DELETED:
                return from + " removed the listing for " + subject + ".";
            case TYPE_ADOPTION_MESSAGE:
                return from + " sent a message about " + subject;
            case TYPE_COMMUNITY_REPLY:
                return from + " replied to you in " + subject;
            case TYPE_COMMUNITY_MESSAGE:
                return from + " sent a message in " + subject;
            default:
                return (from + " " + subject).trim();
        }
    }

    public static String iconFor(AppNotification n) {
        switch (safe(n.getType())) {
            case TYPE_ADOPTION_INTEREST: return "\uD83D\uDC8C";
            case TYPE_ADOPTION_ADOPTED:  return "\uD83C\uDF89";
            case TYPE_ADOPTION_CLOSED:   return "\uD83C\uDFE0";
            case TYPE_RESCUE_VOLUNTEER:  return "\uD83D\uDE97";
            case TYPE_RESCUE_HELPED:     return "\uD83E\uDD1D";
            case TYPE_RESCUE_WITHDRAWN:  return "\u26A0\uFE0F";
            case TYPE_RESCUE_RESCUED:    return "\uD83D\uDC3E";
            case TYPE_RESCUE_DELETED:    return "\uD83D\uDDD1";
            case TYPE_ADOPTION_DELETED:  return "\uD83D\uDDD1";
            case TYPE_ADOPTION_MESSAGE:  return "\uD83D\uDCAC";
            case TYPE_COMMUNITY_REPLY:   return "\uD83D\uDCAC";
            case TYPE_COMMUNITY_MESSAGE: return "\uD83D\uDCAC";
            default:                     return "\uD83D\uDD14";
        }
    }

    /** The screen to open when a notification is tapped. */
    public static Intent targetIntent(Context ctx, AppNotification n) {

        switch (safe(n.getType())) {

            case TYPE_ADOPTION_INTEREST:
                return new Intent(ctx, AdoptionAllRequestsActivity.class);

            case TYPE_ADOPTION_ADOPTED:
            case TYPE_ADOPTION_CLOSED:
            case TYPE_ADOPTION_DELETED: {
                Intent i = new Intent(ctx, AdoptionListingsActivity.class);
                i.putExtra("showAdopted", n.getType().equals(TYPE_ADOPTION_ADOPTED)
                        || n.getType().equals(TYPE_ADOPTION_CLOSED));
                return i;
            }

            case TYPE_ADOPTION_MESSAGE: {
                Intent i = new Intent(ctx, AdoptionChatActivity.class);
                i.putExtra("chatId", n.getTargetId());
                i.putExtra("petName", n.getPetName());
                return i;
            }

            case TYPE_COMMUNITY_REPLY:
            case TYPE_COMMUNITY_MESSAGE: {
                Intent i = new Intent(ctx, ChatRoomActivity.class);
                i.putExtra("roomId", n.getTargetId());
                return i;
            }

            case TYPE_RESCUE_VOLUNTEER:
            case TYPE_RESCUE_HELPED:
            case TYPE_RESCUE_WITHDRAWN:
            case TYPE_RESCUE_RESCUED:
            case TYPE_RESCUE_DELETED: {
                Intent i = new Intent(ctx, RescueDetailActivity.class);
                i.putExtra("reportId", n.getTargetId());
                return i;
            }

            default:
                return new Intent(ctx, PetListActivity.class);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}