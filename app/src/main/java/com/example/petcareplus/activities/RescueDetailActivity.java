package com.example.petcareplus.activities;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.petcareplus.R;
import com.example.petcareplus.models.RescueReport;
import com.example.petcareplus.utils.QRCodeUtils;
import com.example.petcareplus.utils.NotificationsHelper;
import com.example.petcareplus.utils.ReportsHelper;
import com.example.petcareplus.utils.RescueImageUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RescueDetailActivity extends AppCompatActivity {

    private TextView txtTitle, txtPhotoHint, txtDescription, txtStatus, txtLocationValue, txtHelpers;
    private TextView btnHelp, btnCantHelp, btnReportPost;
    private View btnHelped, btnRescued, btnEdit, btnDelete;
    private HorizontalScrollView scrollPhotos;
    private LinearLayout photoRow, ownerActions, helpingActions;
    private ProgressBar progress;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private String reportId;
    private RescueReport report;

    // photos are only re-downloaded when the report changed
    private List<String> cachedPhotos;
    private long cachedStamp = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rescue_detail);

        reportId = getIntent().getStringExtra("reportId");
        if (reportId == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        txtTitle = findViewById(R.id.txtTitle);
        txtPhotoHint = findViewById(R.id.txtPhotoHint);
        txtDescription = findViewById(R.id.txtDescription);
        txtStatus = findViewById(R.id.txtStatus);
        txtLocationValue = findViewById(R.id.txtLocationValue);
        btnHelp = findViewById(R.id.btnHelp);
        btnHelped = findViewById(R.id.btnHelped);
        btnCantHelp = findViewById(R.id.btnCantHelp);
        helpingActions = findViewById(R.id.helpingActions);
        txtHelpers = findViewById(R.id.txtHelpers);
        btnRescued = findViewById(R.id.btnRescued);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnReportPost = findViewById(R.id.btnReportPost);
        scrollPhotos = findViewById(R.id.scrollPhotos);
        photoRow = findViewById(R.id.photoRow);
        ownerActions = findViewById(R.id.ownerActions);
        progress = findViewById(R.id.progress);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.rowLocation).setOnClickListener(v -> openMap());
        findViewById(R.id.rowShare).setOnClickListener(v -> shareReport());
        findViewById(R.id.rowQr).setOnClickListener(v -> showQr());
        findViewById(R.id.rowStatus).setOnClickListener(v -> {
            Intent i = new Intent(this, RescueStatusActivity.class);
            i.putExtra("reportId", reportId);
            startActivity(i);
        });

        btnHelp.setOnClickListener(v -> onHelp());
        btnHelped.setOnClickListener(v -> onHelped());
        btnCantHelp.setOnClickListener(v -> confirmCantHelp());
        btnRescued.setOnClickListener(v -> onMarkRescued());
        btnEdit.setOnClickListener(v -> {
            Intent i = new Intent(this, ReportRescueActivity.class);
            i.putExtra("reportId", reportId);
            startActivity(i);
        });
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnReportPost.setOnClickListener(v -> confirmReportPost());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (reportId != null) load();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    // ------------------------------------------------------------------ loading

    private void load() {
        progress.setVisibility(View.VISIBLE);

        db.collection("rescueReports").document(reportId).get()
                .addOnSuccessListener(doc -> {
                    progress.setVisibility(View.GONE);
                    if (!doc.exists()) {
                        toast("This report is no longer available");
                        finish();
                        return;
                    }
                    report = RescueReport.fromSnapshot(doc);
                    bind();
                    loadPhotosIfNeeded();
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    toast("Failed to load: " + e.getMessage());
                });
    }

    private void loadPhotosIfNeeded() {
        if (report.photoCount <= 0) {
            report.photos = new ArrayList<>();
            cachedPhotos = report.photos;
            cachedStamp = report.updatedAt;
            bindPhotos();
            return;
        }

        if (cachedPhotos != null && cachedStamp == report.updatedAt) {
            report.photos = cachedPhotos;
            bindPhotos();
            return;
        }

        final long stamp = report.updatedAt;
        RescueReport.loadPhotos(reportId, new RescueReport.PhotosCallback() {
            @Override
            public void onLoaded(List<String> photos) {
                if (isFinishing() || report == null) return;
                cachedPhotos = photos;
                cachedStamp = stamp;
                report.photos = photos;
                bindPhotos();
            }

            @Override
            public void onError(Exception e) {
                if (!isFinishing()) toast("Couldn't load photos: " + e.getMessage());
            }
        });
    }

    // ------------------------------------------------------------------ binding

    private void bind() {
        txtTitle.setText("Rescue #" + report.shortCode());
        txtDescription.setText(report.description == null ? "" : report.description);
        txtLocationValue.setText(report.displayLocation());

        txtStatus.setText(report.statusLabel());
        if (report.isRescued()) {
            txtStatus.setBackgroundResource(R.drawable.bg_chip_green);
            txtStatus.setTextColor(0xFF1E5C45);
        } else {
            txtStatus.setBackgroundResource(R.drawable.bg_chip_orange);
            txtStatus.setTextColor(0xFFD9622B);
        }
        txtStatus.setPadding(dp(10), dp(4), dp(10), dp(4));

        String uid = currentUid();
        boolean owner = uid != null && uid.equals(report.reporterId);

        ownerActions.setVisibility(owner ? View.VISIBLE : View.GONE);
        btnReportPost.setVisibility(owner ? View.GONE : View.VISIBLE);
        btnRescued.setVisibility(owner && !report.isRescued() ? View.VISIBLE : View.GONE);

        // ---- volunteer buttons -------------------------------------------------
        boolean isHelper = uid != null && report.helperIds.contains(uid);
        boolean hasHelped = uid != null && report.helpedIds.contains(uid);

        helpingActions.setVisibility(View.GONE);
        btnHelp.setEnabled(true);
        btnHelp.setAlpha(1f);

        if (owner) {
            btnHelp.setVisibility(View.GONE);
        } else if (hasHelped) {
            btnHelp.setVisibility(View.VISIBLE);
            btnHelp.setText("You helped with this rescue \u2713");
            btnHelp.setEnabled(false);
            btnHelp.setAlpha(0.7f);
        } else if (report.isRescued()) {
            btnHelp.setVisibility(View.GONE);
        } else if (isHelper) {
            btnHelp.setVisibility(View.GONE);
            helpingActions.setVisibility(View.VISIBLE);
            btnCantHelp.setText("I Can't Help Now");
        } else {
            btnHelp.setVisibility(View.VISIBLE);
            btnHelp.setText("I Can Help");
        }

        // ---- volunteer summary ---------------------------------------------------
        int helping = report.helperIds.size();
        int helped = report.helpedIds.size();
        if (helping + helped > 0) {
            StringBuilder sb = new StringBuilder("\uD83D\uDC65 ");
            if (helping > 0) sb.append(helping).append(helping == 1 ? " volunteer helping" : " volunteers helping");
            if (helping > 0 && helped > 0) sb.append(" \u2022 ");
            if (helped > 0) sb.append(helped).append(" helped");
            txtHelpers.setText(sb.toString());
            txtHelpers.setVisibility(View.VISIBLE);
        } else {
            txtHelpers.setVisibility(View.GONE);
        }

        boolean hasPhotos = report.photoCount > 0;
        scrollPhotos.setVisibility(hasPhotos ? View.VISIBLE : View.GONE);
        txtPhotoHint.setVisibility(hasPhotos ? View.VISIBLE : View.GONE);
    }

    private void bindPhotos() {
        photoRow.removeAllViews();
        if (report == null || report.photos.isEmpty()) {
            scrollPhotos.setVisibility(View.GONE);
            txtPhotoHint.setVisibility(View.GONE);
            return;
        }

        scrollPhotos.setVisibility(View.VISIBLE);
        txtPhotoHint.setVisibility(View.VISIBLE);

        for (int i = 0; i < report.photos.size(); i++) {
            final int index = i;
            final String b64 = report.photos.get(i);

            ImageView img = new ImageView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(260), dp(190));
            lp.setMarginEnd(dp(10));
            img.setLayoutParams(lp);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setBackgroundResource(R.drawable.bg_rescue_thumb);
            img.setClipToOutline(true);
            img.setOnClickListener(v -> openViewer(index));
            photoRow.addView(img);

            executor.execute(() -> {
                Bitmap bmp = RescueImageUtils.base64ToThumb(b64, 720);
                runOnUiThread(() -> {
                    if (!isFinishing() && bmp != null) img.setImageBitmap(bmp);
                });
            });
        }
    }

    private void openViewer(int index) {
        if (report == null || report.photos.isEmpty()) return;
        RescuePhotoViewerActivity.pending = report.photos;
        Intent i = new Intent(this, RescuePhotoViewerActivity.class);
        i.putExtra("reportId", reportId);
        i.putExtra("index", index);
        startActivity(i);
    }

    // ------------------------------------------------------------------ actions

    private void onHelp() {
        String uid = currentUid();
        if (uid == null || report == null) {
            toast("Please sign in first");
            return;
        }

        long now = System.currentTimeMillis();
        btnHelp.setEnabled(false);

        db.collection("rescueReports").document(reportId)
                .update("helperIds", FieldValue.arrayUnion(uid),
                        "stepTimes." + RescueReport.STEP_VOLUNTEER, now,
                        "updatedAt", now)
                .addOnSuccessListener(v -> {
                    toast("Thank you! You're now marked as a volunteer for this rescue.");
                    NotificationsHelper.notifyRescueReporter(db, report, NotificationsHelper.TYPE_RESCUE_VOLUNTEER);
                    load();
                })
                .addOnFailureListener(e -> {
                    btnHelp.setEnabled(true);
                    toast("Failed: " + e.getMessage());
                });
    }

    /** Volunteer finished helping: moves them from "helping" to "helped". */
    private void onHelped() {
        String uid = currentUid();
        if (uid == null || report == null) return;

        long now = System.currentTimeMillis();
        setHelpButtonsEnabled(false);

        Map<String, Object> upd = new HashMap<>();
        upd.put("helperIds", FieldValue.arrayRemove(uid));
        upd.put("helpedIds", FieldValue.arrayUnion(uid));
        upd.put("updatedAt", now);

        db.collection("rescueReports").document(reportId).update(upd)
                .addOnSuccessListener(v -> {
                    toast("Thank you for helping! \uD83D\uDC3E");
                    NotificationsHelper.notifyRescueReporter(db, report, NotificationsHelper.TYPE_RESCUE_HELPED);
                    setHelpButtonsEnabled(true);
                    load();
                })
                .addOnFailureListener(e -> {
                    setHelpButtonsEnabled(true);
                    toast("Failed: " + e.getMessage());
                });
    }

    private void confirmCantHelp() {
        new AlertDialog.Builder(this)
                .setTitle("Can't help anymore?")
                .setMessage("You'll be removed as a volunteer for this rescue, so others know "
                        + "they may need to step in.")
                .setPositiveButton("Yes, withdraw", (d, w) -> onCantHelp())
                .setNegativeButton("Keep helping", null)
                .show();
    }

    /** Volunteer withdraws: removed from "helping" (and can tap "I Can Help" again later). */
    private void onCantHelp() {
        String uid = currentUid();
        if (uid == null || report == null) return;

        long now = System.currentTimeMillis();
        setHelpButtonsEnabled(false);

        Map<String, Object> upd = new HashMap<>();
        upd.put("helperIds", FieldValue.arrayRemove(uid));
        upd.put("updatedAt", now);
        // nobody else is helping and nobody finished helping -> clear the "volunteer" step
        if (report.helperIds.size() <= 1 && report.helpedIds.isEmpty()) {
            upd.put("stepTimes." + RescueReport.STEP_VOLUNTEER, FieldValue.delete());
        }

        db.collection("rescueReports").document(reportId).update(upd)
                .addOnSuccessListener(v -> {
                    toast("You've been removed as a volunteer for this rescue.");
                    NotificationsHelper.notifyRescueReporter(db, report, NotificationsHelper.TYPE_RESCUE_WITHDRAWN);
                    setHelpButtonsEnabled(true);
                    load();
                })
                .addOnFailureListener(e -> {
                    setHelpButtonsEnabled(true);
                    toast("Failed: " + e.getMessage());
                });
    }

    private void setHelpButtonsEnabled(boolean enabled) {
        btnHelp.setEnabled(enabled);
        btnHelped.setEnabled(enabled);
        btnCantHelp.setEnabled(enabled);
    }

    private void onMarkRescued() {
        long now = System.currentTimeMillis();
        btnRescued.setEnabled(false);

        db.collection("rescueReports").document(reportId)
                .update("stepTimes." + RescueReport.STEP_RESCUED, now, "updatedAt", now)
                .addOnSuccessListener(v -> {
                    toast("Marked as rescued and safe \uD83D\uDC3E");
                    NotificationsHelper.notifyRescueVolunteers(db, report);
                    btnRescued.setEnabled(true);
                    load();
                })
                .addOnFailureListener(e -> {
                    btnRescued.setEnabled(true);
                    toast("Failed: " + e.getMessage());
                });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete this report?")
                .setMessage("The report and its photos will be removed for everyone. This can't be undone.")
                .setPositiveButton("Delete", (d, w) -> doDelete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doDelete() {
        if (report == null) return;
        progress.setVisibility(View.VISIBLE);

        DocumentReference ref = db.collection("rescueReports").document(reportId);
        WriteBatch batch = db.batch();
        for (int i = 0; i < report.photoCount; i++) {
            batch.delete(ref.collection("photos").document(String.valueOf(i)));
        }
        batch.delete(ref);

        RescueReport reportBeingDeleted = report;

        batch.commit()
                .addOnSuccessListener(v -> {
                    toast("Report deleted");
                    NotificationsHelper.notifyRescueDeleted(db, reportBeingDeleted);
                    finish();
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    toast("Failed to delete: " + e.getMessage());
                });
    }

    private void openMap() {
        if (report == null) return;

        Uri geo = null;
        if (report.latitude != null && report.longitude != null) {
            geo = Uri.parse("geo:" + report.latitude + "," + report.longitude
                    + "?q=" + report.latitude + "," + report.longitude + "(Rescue%20%23" + report.shortCode() + ")");
        } else if (report.locationText != null && !report.locationText.trim().isEmpty()) {
            geo = Uri.parse("geo:0,0?q=" + Uri.encode(report.locationText.trim()));
        }

        if (geo == null) {
            toast("No location was added to this report");
            return;
        }

        try {
            startActivity(new Intent(Intent.ACTION_VIEW, geo));
        } catch (ActivityNotFoundException e) {
            String web = report.mapUrl();
            if (web == null) {
                toast("No maps app found");
                return;
            }
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(web)));
            } catch (ActivityNotFoundException e2) {
                toast("No maps app or browser found");
            }
        }
    }

    /** Shares the details + a tappable Google Maps link (and the first photo, when available). */
    private void shareReport() {
        if (report == null) return;

        Intent send = new Intent(Intent.ACTION_SEND);
        send.putExtra(Intent.EXTRA_SUBJECT, "PetCare+ Rescue #" + report.shortCode());
        send.putExtra(Intent.EXTRA_TEXT, report.buildShareText());
        send.setType("text/plain");

        if (report.photos != null && !report.photos.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(report.photos.get(0), Base64.DEFAULT);
                File dir = new File(getCacheDir(), "rescue_photos");
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
                File file = new File(dir, "rescue_" + report.shortCode() + ".jpg");
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(bytes);
                }
                Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
                send.setType("image/jpeg");
                send.putExtra(Intent.EXTRA_STREAM, uri);
                send.setClipData(ClipData.newRawUri("rescue photo", uri));
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception e) {
                // fall back to text only
                send.setType("text/plain");
                send.removeExtra(Intent.EXTRA_STREAM);
                send.setClipData(null);
            }
        }

        startActivity(Intent.createChooser(send, "Share rescue report"));
    }

    private void showQr() {
        if (report == null) return;

        Bitmap qr = QRCodeUtils.generate(report.buildQrText(), dp(260));
        if (qr == null) {
            toast("Couldn't create the QR code");
            return;
        }

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(24), dp(16), dp(24), dp(8));

        ImageView iv = new ImageView(this);
        iv.setImageBitmap(qr);
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(260), dp(260)));
        box.addView(iv);

        TextView cap = new TextView(this);
        cap.setText("A volunteer can scan this to see the location and details.");
        cap.setGravity(Gravity.CENTER);
        cap.setTextSize(12);
        cap.setPadding(0, dp(12), 0, 0);
        box.addView(cap);

        new AlertDialog.Builder(this)
                .setTitle("Rescue #" + report.shortCode())
                .setView(box)
                .setPositiveButton("Share", (d, w) -> shareReport())
                .setNegativeButton("Close", null)
                .show();
    }

    private void confirmReportPost() {
        new AlertDialog.Builder(this)
                .setTitle("Report this post?")
                .setMessage("Tell us if this report is fake, spam or inappropriate. Our team will review it.")
                .setPositiveButton("Report", (d, w) -> doReportPost())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doReportPost() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || report == null) return;

        String reporterName = user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                ? user.getDisplayName() : "A user";
        String postedBy = report.reporterName != null ? report.reporterName : "Reporter";

        ReportsHelper.submitReport(
                db, user.getUid(), reporterName,
                report.reporterId, postedBy,
                "rescue", report.id, report.description, "Reported from Rescue Connect",
                success -> toast(success ? "Reported - thank you, we'll review it"
                        : "Failed to submit report")
        );
    }

    // ------------------------------------------------------------------ helpers

    private String currentUid() {
        FirebaseUser u = auth.getCurrentUser();
        return u == null ? null : u.getUid();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}