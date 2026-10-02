package com.example.petcareplus.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.petcareplus.R;
import com.example.petcareplus.models.RescueReport;
import com.example.petcareplus.utils.RescueImageUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Creates a new rescue report, or edits an existing one when started with the extra "reportId".
 *
 * - up to 5 photos (camera or gallery, several at once)
 * - location: automatic (GPS) OR typed by the person
 */
public class ReportRescueActivity extends AppCompatActivity {

    private static final int MAX_PHOTOS = RescueReport.MAX_PHOTOS;

    private static class PhotoEntry {
        final String base64;   // full photo, stored in Firestore
        final Bitmap thumb;    // small copy for this screen

        PhotoEntry(String base64, Bitmap thumb) {
            this.base64 = base64;
            this.thumb = thumb;
        }
    }

    private TextView txtTitle, txtPhotoCount, chipCurrent, chipManual, txtLocationStatus, btnSubmit;
    private LinearLayout photoRow;
    private EditText edtDescription, edtLocation;
    private ProgressBar progress;

    private final List<PhotoEntry> photos = new ArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FusedLocationProviderClient fused;

    private String editId;            // null = creating a new report
    private int editOldPhotoCount;    // how many photo documents exist for the report being edited
    private String editReporterName;

    private boolean useCurrent = true;
    private Double lat, lng;
    private String resolvedAddress;
    private boolean locating;

    private Uri cameraUri;

    // ------------------------------------------------------------------ result launchers

    private final ActivityResultLauncher<PickVisualMediaRequest> pickPhotos =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS),
                    this::addUris);

    private final ActivityResultLauncher<Uri> takePhoto =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), ok -> {
                if (Boolean.TRUE.equals(ok) && cameraUri != null) {
                    addUris(Collections.singletonList(cameraUri));
                }
            });

    private final ActivityResultLauncher<String> cameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (Boolean.TRUE.equals(granted)) {
                    launchCamera();
                } else {
                    toast("Camera permission is needed to take a photo");
                }
            });

    private final ActivityResultLauncher<String[]> locationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                        || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (granted) {
                    fetchLocation();
                } else {
                    toast("Location permission denied - you can type the location instead");
                    setMode(false);
                }
            });

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_rescue);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        fused = LocationServices.getFusedLocationProviderClient(this);

        // Guests (anonymous auth) can browse rescues but can't post them
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || currentUser.isAnonymous()) {
            Toast.makeText(this, "Please create an account to report a rescue",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        txtTitle = findViewById(R.id.txtTitle);
        txtPhotoCount = findViewById(R.id.txtPhotoCount);
        chipCurrent = findViewById(R.id.chipCurrent);
        chipManual = findViewById(R.id.chipManual);
        txtLocationStatus = findViewById(R.id.txtLocationStatus);
        btnSubmit = findViewById(R.id.btnSubmit);
        photoRow = findViewById(R.id.photoRow);
        edtDescription = findViewById(R.id.edtDescription);
        edtLocation = findViewById(R.id.edtLocation);
        progress = findViewById(R.id.progress);

        editId = getIntent().getStringExtra("reportId");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        chipCurrent.setOnClickListener(v -> setMode(true));
        chipManual.setOnClickListener(v -> setMode(false));
        txtLocationStatus.setOnClickListener(v -> {
            if (useCurrent && !locating) {
                lat = null;
                lng = null;
                resolvedAddress = null;
                ensureLocation();
            }
        });
        btnSubmit.setOnClickListener(v -> submit());

        renderPhotos();

        if (editId != null) {
            txtTitle.setText("Edit Report");
            btnSubmit.setText("Save changes");
            loadForEdit();
        } else {
            setMode(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    // ------------------------------------------------------------------ photos

    private void showPhotoSourceDialog() {
        if (photos.size() >= MAX_PHOTOS) {
            toast("You can add up to " + MAX_PHOTOS + " photos");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Add photo")
                .setItems(new String[]{"\uD83D\uDCF7  Take a photo", "\uD83D\uDDBC  Choose from gallery"},
                        (d, which) -> {
                            if (which == 0) {
                                startCamera();
                            } else {
                                pickPhotos.launch(new PickVisualMediaRequest.Builder()
                                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                        .build());
                            }
                        })
                .show();
    }

    private void startCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            File dir = new File(getCacheDir(), "rescue_photos");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            File file = File.createTempFile("rescue_", ".jpg", dir);
            cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            takePhoto.launch(cameraUri);
        } catch (Exception e) {
            toast("Couldn't open the camera: " + e.getMessage());
        }
    }

    private void addUris(List<Uri> uris) {
        if (uris == null || uris.isEmpty()) return;

        int room = MAX_PHOTOS - photos.size();
        if (room <= 0) {
            toast("You can add up to " + MAX_PHOTOS + " photos");
            return;
        }

        final List<Uri> use = uris.size() > room ? new ArrayList<>(uris.subList(0, room)) : uris;
        if (uris.size() > room) toast("Only " + MAX_PHOTOS + " photos are allowed");

        setBusy(true);
        executor.execute(() -> {
            List<PhotoEntry> added = new ArrayList<>();
            for (Uri u : use) {
                try {
                    String b64 = RescueImageUtils.uriToBase64(this, u);
                    Bitmap thumb = RescueImageUtils.base64ToThumb(b64, 260);
                    if (thumb != null) added.add(new PhotoEntry(b64, thumb));
                } catch (Exception ignored) {
                    // skip this one
                }
            }
            runOnUiThread(() -> {
                if (isFinishing()) return;
                photos.addAll(added);
                renderPhotos();
                setBusy(false);
                if (added.size() < use.size()) toast("Some photos couldn't be added");
            });
        });
    }

    private void renderPhotos() {
        photoRow.removeAllViews();

        for (int i = 0; i < photos.size(); i++) {
            final int index = i;

            FrameLayout tile = new FrameLayout(this);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(dp(96), dp(96));
            tp.setMarginEnd(dp(10));
            tile.setLayoutParams(tp);

            ImageView img = new ImageView(this);
            img.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setBackgroundResource(R.drawable.bg_rescue_thumb);
            img.setClipToOutline(true);
            img.setImageBitmap(photos.get(i).thumb);
            tile.addView(img);

            TextView remove = new TextView(this);
            remove.setText("\u2715");
            remove.setTextColor(0xFFFFFFFF);
            remove.setTextSize(12);
            remove.setGravity(Gravity.CENTER);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(0xCC000000);
            remove.setBackground(circle);
            FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(dp(24), dp(24),
                    Gravity.TOP | Gravity.END);
            rp.setMargins(0, dp(4), dp(4), 0);
            remove.setLayoutParams(rp);
            remove.setOnClickListener(v -> {
                photos.remove(index);
                renderPhotos();
            });
            tile.addView(remove);

            photoRow.addView(tile);
        }

        if (photos.size() < MAX_PHOTOS) {
            LinearLayout add = new LinearLayout(this);
            add.setOrientation(LinearLayout.VERTICAL);
            add.setGravity(Gravity.CENTER);
            add.setBackgroundResource(R.drawable.bg_dashed_card);
            add.setLayoutParams(new LinearLayout.LayoutParams(dp(96), dp(96)));

            TextView icon = new TextView(this);
            icon.setText("\uD83D\uDCF7");
            icon.setTextSize(22);
            icon.setGravity(Gravity.CENTER);

            TextView label = new TextView(this);
            label.setText(photos.isEmpty() ? "Add photos" : "Add more");
            label.setTextSize(11);
            label.setTextColor(0xFF6B726E);
            label.setGravity(Gravity.CENTER);

            add.addView(icon);
            add.addView(label);
            add.setOnClickListener(v -> showPhotoSourceDialog());
            photoRow.addView(add);
        }

        txtPhotoCount.setText(photos.size() + "/" + MAX_PHOTOS);
    }

    // ------------------------------------------------------------------ location

    private void setMode(boolean current) {
        useCurrent = current;
        styleChip(chipCurrent, current);
        styleChip(chipManual, !current);

        edtLocation.setVisibility(current ? View.GONE : View.VISIBLE);
        txtLocationStatus.setVisibility(current ? View.VISIBLE : View.GONE);

        if (current) {
            if (lat == null && !locating) {
                ensureLocation();
            } else {
                updateLocationStatus();
            }
        }
    }

    private void styleChip(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.bg_button_orange_filled
                : R.drawable.bg_rescue_input);
        chip.setTextColor(selected ? 0xFFFFFFFF : 0xFF1B2B24);
        chip.setPadding(dp(14), dp(10), dp(14), dp(10));
    }

    private void ensureLocation() {
        boolean fine = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (fine || coarse) {
            fetchLocation();
        } else {
            locationPermission.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchLocation() {
        locating = true;
        txtLocationStatus.setText("\uD83D\uDCCD Getting your location...");

        CancellationTokenSource cts = new CancellationTokenSource();
        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        onLocation(loc);
                    } else {
                        fused.getLastLocation()
                                .addOnSuccessListener(last -> {
                                    if (last != null) onLocation(last);
                                    else locationFailed();
                                })
                                .addOnFailureListener(e -> locationFailed());
                    }
                })
                .addOnFailureListener(e -> locationFailed());
    }

    private void onLocation(Location loc) {
        lat = loc.getLatitude();
        lng = loc.getLongitude();
        locating = false;
        resolvedAddress = null;
        updateLocationStatus();
        resolveAddress(lat, lng);
    }

    private void locationFailed() {
        locating = false;
        txtLocationStatus.setText("\u26A0\uFE0F Couldn't get your location. Turn on GPS and tap here to retry, "
                + "or choose \"Type location\".");
    }

    @SuppressWarnings("deprecation")
    private void resolveAddress(double la, double lo) {
        executor.execute(() -> {
            String addr = null;
            try {
                if (Geocoder.isPresent()) {
                    List<Address> list = new Geocoder(this, Locale.getDefault()).getFromLocation(la, lo, 1);
                    if (list != null && !list.isEmpty()) addr = list.get(0).getAddressLine(0);
                }
            } catch (Exception ignored) {
                // fall back to coordinates
            }
            final String result = addr;
            runOnUiThread(() -> {
                if (isFinishing()) return;
                resolvedAddress = result;
                updateLocationStatus();
            });
        });
    }

    private void updateLocationStatus() {
        if (lat == null || lng == null) return;
        String text = resolvedAddress != null
                ? resolvedAddress
                : String.format(Locale.US, "%.5f, %.5f", lat, lng);
        txtLocationStatus.setText("\uD83D\uDCCD " + text + "\nTap to refresh location");
    }

    // ------------------------------------------------------------------ edit mode

    private void loadForEdit() {
        setBusy(true);
        db.collection("rescueReports").document(editId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        toast("This report no longer exists");
                        finish();
                        return;
                    }
                    RescueReport r = RescueReport.fromSnapshot(doc);
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null || !user.getUid().equals(r.reporterId)) {
                        toast("Only the person who posted this report can edit it");
                        finish();
                        return;
                    }

                    editOldPhotoCount = r.photoCount;
                    editReporterName = r.reporterName;

                    edtDescription.setText(r.description);
                    lat = r.latitude;
                    lng = r.longitude;
                    resolvedAddress = r.locationText;

                    if (r.useCurrentLocation && lat != null) {
                        setMode(true);
                    } else {
                        edtLocation.setText(r.locationText);
                        setMode(false);
                    }

                    RescueReport.loadPhotos(editId, new RescueReport.PhotosCallback() {
                        @Override
                        public void onLoaded(List<String> loaded) {
                            executor.execute(() -> {
                                List<PhotoEntry> entries = new ArrayList<>();
                                for (String b64 : loaded) {
                                    Bitmap t = RescueImageUtils.base64ToThumb(b64, 260);
                                    if (t != null) entries.add(new PhotoEntry(b64, t));
                                }
                                runOnUiThread(() -> {
                                    if (isFinishing()) return;
                                    photos.addAll(entries);
                                    renderPhotos();
                                    setBusy(false);
                                });
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() -> {
                                setBusy(false);
                                toast("Couldn't load the photos: " + e.getMessage());
                            });
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    setBusy(false);
                    toast("Failed to load report: " + e.getMessage());
                    finish();
                });
    }

    // ------------------------------------------------------------------ submit

    private void submit() {
        String desc = edtDescription.getText().toString().trim();
        if (desc.isEmpty()) {
            edtDescription.setError("Please describe what happened");
            edtDescription.requestFocus();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            toast("Please sign in first");
            return;
        }

        final Double la;
        final Double lo;
        final String locText;

        if (useCurrent) {
            if (locating) {
                toast("Still getting your location...");
                return;
            }
            if (lat == null || lng == null) {
                toast("Location not available. Tap the location text to retry, or choose \"Type location\".");
                return;
            }
            la = lat;
            lo = lng;
            locText = resolvedAddress != null
                    ? resolvedAddress
                    : String.format(Locale.US, "%.5f, %.5f", lat, lng);
        } else {
            String typed = edtLocation.getText().toString().trim();
            if (typed.isEmpty()) {
                edtLocation.setError("Please enter where the animal is");
                edtLocation.requestFocus();
                return;
            }
            la = null;
            lo = null;
            locText = typed;
        }

        final String uid = user.getUid();
        final long now = System.currentTimeMillis();

        RescueReport r = new RescueReport();
        r.description = desc;
        r.locationText = locText;
        r.latitude = la;
        r.longitude = lo;
        r.useCurrentLocation = useCurrent;
        r.photoCount = photos.size();
        r.thumb = photos.isEmpty() ? "" : RescueImageUtils.bitmapToSmallBase64(photos.get(0).thumb);
        r.updatedAt = now;

        setBusy(true);

        if (editId == null) {
            r.reporterId = uid;
            String name = user.getDisplayName();
            r.reporterName = (name != null && !name.trim().isEmpty()) ? name.trim() : "Guest";
            r.createdAt = now;
            r.stepTimes.put(RescueReport.STEP_REPORTED, now);

            DocumentReference ref = db.collection("rescueReports").document();
            WriteBatch batch = db.batch();
            batch.set(ref, r.toCreateMap());
            for (int i = 0; i < photos.size(); i++) {
                batch.set(ref.collection("photos").document(String.valueOf(i)),
                        photoMap(i, photos.get(i).base64, uid));
            }

            batch.commit()
                    .addOnSuccessListener(v -> {
                        toast("Report submitted");
                        Intent open = new Intent(this, RescueDetailActivity.class);
                        open.putExtra("reportId", ref.getId());
                        startActivity(open);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        setBusy(false);
                        toast("Failed: " + e.getMessage());
                    });

        } else {
            DocumentReference ref = db.collection("rescueReports").document(editId);
            WriteBatch batch = db.batch();
            batch.update(ref, r.toUpdateMap());

            for (int i = 0; i < photos.size(); i++) {
                batch.set(ref.collection("photos").document(String.valueOf(i)),
                        photoMap(i, photos.get(i).base64, uid));
            }
            // remove photo documents that are no longer used
            for (int i = photos.size(); i < editOldPhotoCount; i++) {
                batch.delete(ref.collection("photos").document(String.valueOf(i)));
            }

            batch.commit()
                    .addOnSuccessListener(v -> {
                        toast("Report updated");
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        setBusy(false);
                        toast("Failed: " + e.getMessage());
                    });
        }
    }

    private Map<String, Object> photoMap(int index, String base64, String uid) {
        Map<String, Object> m = new HashMap<>();
        m.put("data", base64);
        m.put("index", index);
        m.put("reporterId", uid);
        return m;
    }

    // ------------------------------------------------------------------ helpers

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!busy);
        btnSubmit.setAlpha(busy ? 0.6f : 1f);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}