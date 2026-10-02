package com.example.petcareplus.models;

import android.net.Uri;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A rescue report.
 *
 * Firestore layout:
 *   rescueReports/{id}                 -> text fields, small "thumb" (for the list), photoCount, helperIds, stepTimes
 *   rescueReports/{id}/photos/{0..4}   -> { data: base64 jpeg, index, reporterId }
 *
 * Photos live in a sub-collection so the list screen stays fast and no document gets near
 * Firestore's 1 MB limit.
 */
public class RescueReport {

    public static final String STEP_REPORTED = "reported";
    public static final String STEP_NGO = "ngo";
    public static final String STEP_VOLUNTEER = "volunteer";
    public static final String STEP_RESCUED = "rescued";

    public static final int MAX_PHOTOS = 5;

    public String id;
    public String reporterId;
    public String reporterName;
    public String description;
    public String locationText;
    public Double latitude;
    public Double longitude;
    public boolean useCurrentLocation;
    public String thumb = "";
    public int photoCount;
    /** People who are currently helping (can withdraw). */
    public List<String> helperIds = new ArrayList<>();
    /** People who finished helping ("I have helped"). */
    public List<String> helpedIds = new ArrayList<>();
    public Map<String, Long> stepTimes = new HashMap<>();
    public long createdAt;
    public long updatedAt;

    /** Loaded separately (see loadPhotos). Not stored on the main document. */
    public List<String> photos = new ArrayList<>();

    public interface PhotosCallback {
        void onLoaded(List<String> photos);

        void onError(Exception e);
    }

    // ---------------------------------------------------------------- reading

    @SuppressWarnings("unchecked")
    public static RescueReport fromSnapshot(DocumentSnapshot d) {
        RescueReport r = new RescueReport();
        r.id = d.getId();
        r.reporterId = d.getString("reporterId");
        r.reporterName = d.getString("reporterName");
        r.description = d.getString("description");
        r.locationText = d.getString("locationText");
        r.latitude = d.getDouble("latitude");
        r.longitude = d.getDouble("longitude");

        Boolean cur = d.getBoolean("useCurrentLocation");
        r.useCurrentLocation = cur != null && cur;

        String t = d.getString("thumb");
        r.thumb = t == null ? "" : t;

        Long pc = d.getLong("photoCount");
        r.photoCount = pc == null ? 0 : pc.intValue();

        Object h = d.get("helperIds");
        if (h instanceof List) {
            for (Object o : (List<Object>) h) {
                if (o instanceof String) r.helperIds.add((String) o);
            }
        }

        Object hd = d.get("helpedIds");
        if (hd instanceof List) {
            for (Object o : (List<Object>) hd) {
                if (o instanceof String) r.helpedIds.add((String) o);
            }
        }

        Object s = d.get("stepTimes");
        if (s instanceof Map) {
            for (Map.Entry<String, Object> e : ((Map<String, Object>) s).entrySet()) {
                if (e.getValue() instanceof Number) {
                    r.stepTimes.put(e.getKey(), ((Number) e.getValue()).longValue());
                }
            }
        }

        Long c = d.getLong("createdAt");
        r.createdAt = c == null ? 0 : c;
        Long u = d.getLong("updatedAt");
        r.updatedAt = u == null ? 0 : u;
        return r;
    }

    public static void loadPhotos(String reportId, PhotosCallback cb) {
        FirebaseFirestore.getInstance()
                .collection("rescueReports").document(reportId)
                .collection("photos").orderBy("index").get()
                .addOnSuccessListener(qs -> {
                    List<String> out = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        String data = d.getString("data");
                        if (data != null && !data.isEmpty()) out.add(data);
                    }
                    cb.onLoaded(out);
                })
                .addOnFailureListener(cb::onError);
    }

    // ---------------------------------------------------------------- writing

    public Map<String, Object> toCreateMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("reporterId", reporterId);
        m.put("reporterName", reporterName);
        m.put("description", description);
        m.put("locationText", locationText);
        m.put("latitude", latitude);
        m.put("longitude", longitude);
        m.put("useCurrentLocation", useCurrentLocation);
        m.put("thumb", thumb == null ? "" : thumb);
        m.put("photoCount", photoCount);
        m.put("helperIds", new ArrayList<String>());
        m.put("helpedIds", new ArrayList<String>());
        m.put("stepTimes", stepTimes);
        m.put("createdAt", createdAt);
        m.put("updatedAt", updatedAt);
        return m;
    }

    /** Fields the poster may change when editing. */
    public Map<String, Object> toUpdateMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("description", description);
        m.put("locationText", locationText);
        m.put("latitude", latitude);
        m.put("longitude", longitude);
        m.put("useCurrentLocation", useCurrentLocation);
        m.put("thumb", thumb == null ? "" : thumb);
        m.put("photoCount", photoCount);
        m.put("updatedAt", updatedAt);
        return m;
    }

    // ---------------------------------------------------------------- state

    public String shortCode() {
        if (id == null || id.length() < 4) return id == null ? "----" : id.toUpperCase(Locale.ROOT);
        return id.substring(0, 4).toUpperCase(Locale.ROOT);
    }

    public boolean isRescued() {
        return stepTimes.containsKey(STEP_RESCUED);
    }

    public boolean hasVolunteer() {
        return !helperIds.isEmpty() || !helpedIds.isEmpty();
    }

    public String statusLabel() {
        if (isRescued()) return "Rescued";
        if (!helperIds.isEmpty()) return "Volunteer en route";
        if (!helpedIds.isEmpty()) return "Volunteer helped";
        return "Open";
    }

    // ---------------------------------------------------------------- location / sharing

    public String displayLocation() {
        if (locationText != null && !locationText.trim().isEmpty()) return locationText.trim();
        if (latitude != null && longitude != null) {
            return String.format(Locale.US, "%.5f, %.5f", latitude, longitude);
        }
        return "Location not provided";
    }

    /** A tappable Google Maps link (coordinates if we have them, otherwise the typed place). */
    public String mapUrl() {
        String base = "https://www.google.com/maps/search/?api=1&query=";
        if (latitude != null && longitude != null) {
            return base + String.format(Locale.US, "%.6f,%.6f", latitude, longitude);
        }
        if (locationText != null && !locationText.trim().isEmpty()) {
            return base + Uri.encode(locationText.trim());
        }
        return null;
    }

    public String buildShareText() {
        StringBuilder sb = new StringBuilder();
        sb.append("\uD83D\uDC3E PetCare+ Rescue Alert #").append(shortCode()).append("\n\n");
        sb.append("\uD83D\uDCCD Location: ").append(displayLocation()).append("\n");
        String map = mapUrl();
        if (map != null) sb.append("\uD83D\uDDFA Open in Maps: ").append(map).append("\n");
        sb.append("\n\uD83D\uDCDD ").append(description == null ? "" : description.trim()).append("\n");
        sb.append("\nStatus: ").append(statusLabel());
        if (photoCount > 0) {
            sb.append(" \u2022 ").append(photoCount).append(photoCount == 1 ? " photo" : " photos");
        }
        sb.append("\nCase ID: ").append(id);
        sb.append("\n\nCan you help? Open PetCare+ \u2192 Rescue Connect and tap \"I Can Help\".");
        return sb.toString();
    }

    /** Plain text for the QR code (kept short so the code stays easy to scan). */
    public String buildQrText() {
        StringBuilder sb = new StringBuilder();
        sb.append("PetCare+ Rescue #").append(shortCode()).append("\n");
        sb.append("Location: ").append(displayLocation()).append("\n");
        String map = mapUrl();
        if (map != null) sb.append(map).append("\n");
        sb.append("ID: ").append(id);
        return sb.toString();
    }

    // ---------------------------------------------------------------- time helpers

    public static String formatTime(long ms) {
        return new SimpleDateFormat("EEE, h:mm a", Locale.getDefault()).format(new Date(ms));
    }

    public static String timeAgo(long ms) {
        if (ms <= 0) return "";
        long min = (System.currentTimeMillis() - ms) / 60000L;
        if (min < 1) return "just now";
        if (min < 60) return min + " min ago";
        long hr = min / 60;
        if (hr < 24) return hr + (hr == 1 ? " hr ago" : " hrs ago");
        long d = hr / 24;
        return d + (d == 1 ? " day ago" : " days ago");
    }
}