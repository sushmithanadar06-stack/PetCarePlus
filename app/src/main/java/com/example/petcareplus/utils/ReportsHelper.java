package com.example.petcareplus.utils;

import com.example.petcareplus.models.Report;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class ReportsHelper {

    public interface SimpleCallback {
        void onDone(boolean success);
    }

    /**
     * Submits a report for manual review. Reports are stored in a
     * top-level "reports" collection — check the Firebase console
     * periodically and take action (delete content, block sender,
     * or ban) manually. No backend function needed.
     */
    public static void submitReport(FirebaseFirestore db,
                                    String reporterId, String reporterName,
                                    String reportedUserId, String reportedUserName,
                                    String contentType, String contentId,
                                    String contentPreview, String reason,
                                    SimpleCallback callback) {

        if (reporterId == null || reportedUserId == null) {
            if (callback != null) callback.onDone(false);
            return;
        }

        DocumentReference newRef = db.collection("reports").document();

        // Keep the preview short — it's just for quick triage in the console.
        String trimmedPreview = contentPreview != null && contentPreview.length() > 200
                ? contentPreview.substring(0, 200) + "…"
                : contentPreview;

        Report report = new Report(
                newRef.getId(), reporterId, reporterName, reportedUserId, reportedUserName,
                contentType, contentId, trimmedPreview, reason,
                System.currentTimeMillis(), false
        );

        newRef.set(report)
                .addOnSuccessListener(unused -> { if (callback != null) callback.onDone(true); })
                .addOnFailureListener(e -> { if (callback != null) callback.onDone(false); });
    }
}