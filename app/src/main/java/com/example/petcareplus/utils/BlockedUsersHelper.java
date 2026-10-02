package com.example.petcareplus.utils;

import com.example.petcareplus.models.BlockedUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashSet;
import java.util.Set;

public class BlockedUsersHelper {

    public interface BlockedIdsCallback {
        void onLoaded(Set<String> blockedUids);
    }

    public interface SimpleCallback {
        void onDone(boolean success);
    }

    /**
     * Blocks a user. Stored under:
     * users/{myUid}/blockedUsers/{blockedUid}
     */
    public static void blockUser(FirebaseFirestore db, String myUid,
                                 String blockedUid, String blockedName,
                                 SimpleCallback callback) {

        if (myUid == null || blockedUid == null) {
            if (callback != null) callback.onDone(false);
            return;
        }

        BlockedUser blocked = new BlockedUser(blockedUid, blockedName, System.currentTimeMillis());

        db.collection("users").document(myUid)
                .collection("blockedUsers").document(blockedUid)
                .set(blocked)
                .addOnSuccessListener(unused -> { if (callback != null) callback.onDone(true); })
                .addOnFailureListener(e -> { if (callback != null) callback.onDone(false); });
    }

    public static void unblockUser(FirebaseFirestore db, String myUid,
                                   String blockedUid, SimpleCallback callback) {

        if (myUid == null || blockedUid == null) {
            if (callback != null) callback.onDone(false);
            return;
        }

        db.collection("users").document(myUid)
                .collection("blockedUsers").document(blockedUid)
                .delete()
                .addOnSuccessListener(unused -> { if (callback != null) callback.onDone(true); })
                .addOnFailureListener(e -> { if (callback != null) callback.onDone(false); });
    }

    /**
     * Loads the full set of UIDs the current user has blocked.
     * Call this once when a chat/list screen opens, then use the
     * returned set to filter out messages/listings client-side.
     */
    public static void getBlockedUids(FirebaseFirestore db, String myUid,
                                      BlockedIdsCallback callback) {

        if (myUid == null) {
            callback.onLoaded(new HashSet<>());
            return;
        }

        db.collection("users").document(myUid)
                .collection("blockedUsers")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    Set<String> ids = new HashSet<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        ids.add(doc.getId());
                    }
                    callback.onLoaded(ids);
                })
                .addOnFailureListener(e -> callback.onLoaded(new HashSet<>()));
    }

    public static void isBlocked(FirebaseFirestore db, String myUid, String otherUid,
                                 SimpleCallback callback) {

        if (myUid == null || otherUid == null) {
            callback.onDone(false);
            return;
        }

        db.collection("users").document(myUid)
                .collection("blockedUsers").document(otherUid)
                .get()
                .addOnSuccessListener(doc -> callback.onDone(doc.exists()))
                .addOnFailureListener(e -> callback.onDone(false));
    }
}