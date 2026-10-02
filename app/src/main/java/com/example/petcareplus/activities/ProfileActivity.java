package com.example.petcareplus.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Dialog;
import android.view.Window;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;

import com.example.petcareplus.R;
import com.example.petcareplus.utils.ImageUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView txtProfileName, txtProfileEmail, btnProfileBack;
    private EditText etProfileFullName, etProfilePhone, etProfileCity;
    private CardView cardSettingsFromProfile;
    private Button btnSaveProfile, btnProfileLogout;
    private ImageView imgProfilePhoto;
    private TextView txtProfilePhotoPlaceholder;
    private View btnEditProfilePhoto;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String selectedPhotoBase64 = "";
    private Uri cameraImageUri;

    private boolean hasUnsavedChanges = false;

    private final TextWatcher dirtyWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(
                CharSequence s,
                int start,
                int count,
                int after
        ) {
        }

        @Override
        public void onTextChanged(
                CharSequence s,
                int start,
                int before,
                int count
        ) {
            hasUnsavedChanges = true;
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    private static final int PICK_IMAGE = 300;
    private static final int CAPTURE_IMAGE = 301;
    private static final int CAMERA_PERMISSION_CODE = 302;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        txtProfileName = findViewById(R.id.txtProfileName);
        txtProfileEmail = findViewById(R.id.txtProfileEmail);
        btnProfileBack = findViewById(R.id.btnProfileBack);
        etProfileFullName = findViewById(R.id.etProfileFullName);
        etProfilePhone = findViewById(R.id.etProfilePhone);
        etProfileCity = findViewById(R.id.etProfileCity);
        cardSettingsFromProfile = findViewById(R.id.cardSettingsFromProfile);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnProfileLogout = findViewById(R.id.btnProfileLogout);
        imgProfilePhoto = findViewById(R.id.imgProfilePhoto);
        txtProfilePhotoPlaceholder = findViewById(R.id.txtProfilePhotoPlaceholder);
        btnEditProfilePhoto = findViewById(R.id.btnEditProfilePhoto);

        etProfilePhone.setEnabled(false);
        etProfilePhone.setFocusable(false);

        etProfileCity.setEnabled(false);
        etProfileCity.setFocusable(false);

        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {

            String name = user.getDisplayName();

            txtProfileName.setText(
                    (name != null && !name.trim().isEmpty())
                            ? name
                            : "Not set"
            );

            txtProfileEmail.setText(user.getEmail());

            etProfileFullName.setText(
                    (name != null && !name.trim().isEmpty())
                            ? name
                            : ""
            );

            loadExtraProfileData(user.getUid());
        }

        btnProfileBack.setOnClickListener(v ->
                handleBackNavigation()
        );

        imgProfilePhoto.setOnClickListener(v ->
                showFullScreenPhoto()
        );

        txtProfilePhotoPlaceholder.setOnClickListener(v ->
                showPhotoSourceDialog()
        );

        btnEditProfilePhoto.setOnClickListener(v ->
                showPhotoSourceDialog()
        );

        cardSettingsFromProfile.setOnClickListener(v ->
                startActivity(
                        new Intent(
                                ProfileActivity.this,
                                SettingsActivity.class
                        )
                )
        );

        btnSaveProfile.setOnClickListener(v ->
                confirmSaveChanges()
        );

        btnProfileLogout.setOnClickListener(v ->
                showLogoutConfirmation()
        );

        // Attached after initial setText() calls above, so loading the
        // current name doesn't itself mark the form as "changed".
        etProfileFullName.addTextChangedListener(dirtyWatcher);
    }

    // =========================================================
    // UNSAVED CHANGES HANDLING
    // =========================================================

    @Override
    public void onBackPressed() {
        handleBackNavigation();
    }

    private void handleBackNavigation() {

        if (hasUnsavedChanges) {

            showUnsavedChangesDialog();

        } else {

            finish();
        }
    }

    private void showUnsavedChangesDialog() {

        new AlertDialog.Builder(this)
                .setTitle("Unsaved changes")
                .setMessage(
                        "You have unsaved changes. Do you want to save them before leaving?"
                )
                .setPositiveButton(
                        "Save",
                        (dialog, which) -> saveProfile()
                )
                .setNegativeButton(
                        "Discard",
                        (dialog, which) -> {
                            hasUnsavedChanges = false;
                            finish();
                        }
                )
                .setNeutralButton(
                        "Cancel",
                        null
                )
                .show();
    }

    private void confirmSaveChanges() {

        new AlertDialog.Builder(this)
                .setTitle("Save changes?")
                .setMessage(
                        "Do you want to save these changes to your profile?"
                )
                .setPositiveButton(
                        "Save",
                        (dialog, which) -> saveProfile()
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .show();
    }

    private void showLogoutConfirmation() {

        new AlertDialog.Builder(this)
                .setTitle("Log out?")
                .setMessage(
                        "Are you sure you want to end your session?"
                )
                .setPositiveButton(
                        "Log Out",
                        (dialog, which) -> {

                            auth.signOut();

                            Intent intent = new Intent(
                                    ProfileActivity.this,
                                    LoginActivity.class
                            );

                            intent.addFlags(
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                            Intent.FLAG_ACTIVITY_NEW_TASK
                            );

                            startActivity(intent);
                            finish();
                        }
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .show();
    }

    // =========================================================
    // FULL-SCREEN PROFILE PHOTO VIEWER
    // =========================================================

    private void showFullScreenPhoto() {

        if (selectedPhotoBase64.isEmpty()) {

            showPhotoSourceDialog();
            return;
        }

        Bitmap bitmap =
                ImageUtils.base64ToBitmap(selectedPhotoBase64);

        if (bitmap == null) {

            Toast.makeText(
                    this,
                    "Unable to load photo",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Dialog dialog = new Dialog(
                this,
                android.R.style.Theme_Black_NoTitleBar_Fullscreen
        );

        dialog.requestWindowFeature(
                Window.FEATURE_NO_TITLE
        );

        ImageView fullImage =
                new ImageView(this);

        fullImage.setLayoutParams(
                new android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
        );

        fullImage.setScaleType(
                ImageView.ScaleType.FIT_CENTER
        );

        fullImage.setBackgroundColor(
                0xFF000000
        );

        fullImage.setImageBitmap(bitmap);

        fullImage.setOnClickListener(
                v -> dialog.dismiss()
        );

        dialog.setContentView(fullImage);
        dialog.show();
    }

    // =========================================================
    // PHOTO SOURCE CHOOSER
    // =========================================================

    private void showPhotoSourceDialog() {

        String[] options = {
                "Take Photo",
                "Choose from Gallery"
        };

        new AlertDialog.Builder(this)
                .setTitle("Update Profile Photo")
                .setItems(
                        options,
                        (dialog, which) -> {

                            if (which == 0) {

                                checkCameraPermissionAndOpen();

                            } else {

                                openImagePicker();
                            }
                        }
                )
                .show();
    }

    // =========================================================
    // CAMERA PERMISSION
    // =========================================================

    private void checkCameraPermissionAndOpen() {

        if (checkSelfPermission(
                android.Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            android.Manifest.permission.CAMERA
                    },
                    CAMERA_PERMISSION_CODE
            );

        } else {

            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == CAMERA_PERMISSION_CODE) {

            if (grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {

                openCamera();

            } else {

                Toast.makeText(
                        this,
                        "Camera permission is required to take a photo",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    // =========================================================
    // CAMERA
    // =========================================================

    private void openCamera() {

        try {

            File photoFile = File.createTempFile(
                    "profile_capture_" +
                            System.currentTimeMillis(),
                    ".jpg",
                    getCacheDir()
            );

            cameraImageUri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName() +
                                    ".fileprovider",
                            photoFile
                    );

            Intent cameraIntent = new Intent(
                    android.provider.MediaStore.ACTION_IMAGE_CAPTURE
            );

            cameraIntent.putExtra(
                    android.provider.MediaStore.EXTRA_OUTPUT,
                    cameraImageUri
            );

            cameraIntent.addFlags(
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );

            startActivityForResult(
                    cameraIntent,
                    CAPTURE_IMAGE
            );

        } catch (IOException e) {

            Toast.makeText(
                    this,
                    "Unable to open camera: " +
                            e.getMessage(),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================================
    // GALLERY
    // =========================================================

    private void openImagePicker() {

        Intent intent = new Intent(
                Intent.ACTION_OPEN_DOCUMENT
        );

        intent.setType("image/*");

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        startActivityForResult(
                intent,
                PICK_IMAGE
        );
    }

    // =========================================================
    // ACTIVITY RESULT
    // =========================================================

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        // Gallery
        if (requestCode == PICK_IMAGE &&
                resultCode == RESULT_OK &&
                data != null &&
                data.getData() != null) {

            Uri imageUri = data.getData();

            try {

                getContentResolver()
                        .takePersistableUriPermission(
                                imageUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );

            } catch (SecurityException ignored) {
            }

            startCrop(imageUri);
            return;
        }

        // Camera
        if (requestCode == CAPTURE_IMAGE &&
                resultCode == RESULT_OK &&
                cameraImageUri != null) {

            startCrop(cameraImageUri);
            return;
        }

        // Crop success
        if (requestCode == UCrop.REQUEST_CROP &&
                resultCode == RESULT_OK &&
                data != null) {

            Uri croppedUri =
                    UCrop.getOutput(data);

            if (croppedUri != null) {

                String base64 =
                        ImageUtils.uriToCompressedBase64(
                                this,
                                croppedUri
                        );

                if (base64.isEmpty()) {

                    Toast.makeText(
                            this,
                            "Unable to process photo",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                selectedPhotoBase64 = base64;

                Bitmap bitmap =
                        ImageUtils.base64ToBitmap(base64);

                if (bitmap != null) {
                    showPhoto(bitmap);
                }

                hasUnsavedChanges = true;

                Toast.makeText(
                        this,
                        "Photo updated",
                        Toast.LENGTH_SHORT
                ).show();
            }

        } else if (
                requestCode == UCrop.REQUEST_CROP &&
                        resultCode == UCrop.RESULT_ERROR &&
                        data != null
        ) {

            Toast.makeText(
                    this,
                    "Unable to crop photo",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================================
    // IMAGE CROPPING
    // =========================================================

    private void startCrop(Uri sourceUri) {

        String destinationFileName =
                "profile_crop_" +
                        System.currentTimeMillis() +
                        ".jpg";

        Uri destinationUri = Uri.fromFile(
                new File(
                        getCacheDir(),
                        destinationFileName
                )
        );

        UCrop.Options options =
                new UCrop.Options();

        options.setToolbarTitle(
                "Crop Profile Photo"
        );

        options.setCompressionFormat(
                android.graphics.Bitmap.CompressFormat.JPEG
        );

        options.setCompressionQuality(90);

        UCrop.of(
                        sourceUri,
                        destinationUri
                )
                .withAspectRatio(1, 1)
                .withMaxResultSize(600, 600)
                .withOptions(options)
                .start(this);
    }

    // =========================================================
    // SHOW PROFILE PHOTO
    // =========================================================

    private void showPhoto(Bitmap bitmap) {

        imgProfilePhoto.setVisibility(
                View.VISIBLE
        );

        txtProfilePhotoPlaceholder.setVisibility(
                View.GONE
        );

        imgProfilePhoto.setImageBitmap(bitmap);
    }

    // =========================================================
    // PROFILE DATA
    // =========================================================

    private void loadExtraProfileData(String uid) {

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(
                        this::applyProfileSnapshot
                )
                .addOnFailureListener(e ->
                        Toast.makeText(
                                ProfileActivity.this,
                                "Failed to load profile: " +
                                        e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    private void applyProfileSnapshot(
            DocumentSnapshot doc
    ) {

        if (!doc.exists()) {
            return;
        }

        String phone =
                doc.getString("phone");

        String city =
                doc.getString("city");

        String state =
                doc.getString("state");

        String photoUri =
                doc.getString("photoUri");

        if (phone != null) {

            etProfilePhone.setText(phone);
        }

        if (city != null) {

            etProfileCity.setText(city);

        } else if (state != null) {

            etProfileCity.setText(state);
        }

        if (photoUri != null &&
                !photoUri.isEmpty()) {

            selectedPhotoBase64 =
                    photoUri;

            if (ImageUtils.isBase64Image(
                    photoUri
            )) {

                Bitmap bitmap =
                        ImageUtils.base64ToBitmap(
                                photoUri
                        );

                if (bitmap != null) {
                    showPhoto(bitmap);
                }

            } else {

                try {

                    imgProfilePhoto.setVisibility(
                            View.VISIBLE
                    );

                    txtProfilePhotoPlaceholder
                            .setVisibility(
                                    View.GONE
                            );

                    imgProfilePhoto.setImageURI(
                            Uri.parse(photoUri)
                    );

                } catch (Exception ignored) {
                }
            }
        }
    }

    // =========================================================
    // SAVE PROFILE
    // =========================================================

    private void saveProfile() {

        try {

            FirebaseUser user =
                    auth.getCurrentUser();

            if (user == null) {

                Toast.makeText(
                        this,
                        "Not signed in",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            btnSaveProfile.setEnabled(false);

            String fullName =
                    etProfileFullName
                            .getText()
                            .toString()
                            .trim();

            if (fullName.isEmpty()) {

                etProfileFullName.setError(
                        "Enter your name"
                );

                etProfileFullName.requestFocus();

                btnSaveProfile.setEnabled(true);

                return;
            }

            UserProfileChangeRequest profileUpdate =
                    new UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build();

            user.updateProfile(profileUpdate)
                    .addOnSuccessListener(unused -> {

                        Map<String, Object> extraData =
                                new HashMap<>();

                        extraData.put(
                                "name",
                                fullName
                        );

                        extraData.put(
                                "photoUri",
                                selectedPhotoBase64
                        );

                        db.collection("users")
                                .document(user.getUid())
                                .set(
                                        extraData,
                                        SetOptions.merge()
                                )
                                .addOnSuccessListener(unused2 -> {

                                    // =================================================
                                    // PUBLIC PROFILE
                                    // =================================================
                                    // Mirror only the public-safe fields.
                                    //
                                    // name + photoUri are readable by other
                                    // authenticated users.
                                    //
                                    // Private fields such as phone/city remain
                                    // inside /users/{uid}.
                                    // =================================================

                                    Map<String, Object> publicData =
                                            new HashMap<>();

                                    publicData.put(
                                            "name",
                                            fullName
                                    );

                                    publicData.put(
                                            "photoUri",
                                            selectedPhotoBase64
                                    );

                                    db.collection("publicProfiles")
                                            .document(user.getUid())
                                            .set(
                                                    publicData,
                                                    SetOptions.merge()
                                            )
                                            .addOnSuccessListener(
                                                    publicUnused -> {

                                                        hasUnsavedChanges =
                                                                false;

                                                        Toast.makeText(
                                                                ProfileActivity.this,
                                                                "Profile updated successfully",
                                                                Toast.LENGTH_SHORT
                                                        ).show();

                                                        txtProfileName.setText(
                                                                fullName
                                                        );

                                                        btnSaveProfile
                                                                .setEnabled(true);
                                                    }
                                            )
                                            .addOnFailureListener(
                                                    publicError -> {

                                                        // The private profile
                                                        // was already saved.
                                                        // Tell the user that the
                                                        // public mirror failed.
                                                        hasUnsavedChanges =
                                                                false;

                                                        Toast.makeText(
                                                                ProfileActivity.this,
                                                                "Profile saved, but public profile could not be updated: " +
                                                                        publicError.getMessage(),
                                                                Toast.LENGTH_LONG
                                                        ).show();

                                                        txtProfileName.setText(
                                                                fullName
                                                        );

                                                        btnSaveProfile
                                                                .setEnabled(true);
                                                    }
                                            );
                                })
                                .addOnFailureListener(e -> {

                                    Toast.makeText(
                                            ProfileActivity.this,
                                            "Failed to save: " +
                                                    e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();

                                    btnSaveProfile.setEnabled(
                                            true
                                    );
                                });
                    })
                    .addOnFailureListener(e -> {

                        Toast.makeText(
                                ProfileActivity.this,
                                "Failed to update name: " +
                                        e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                        btnSaveProfile.setEnabled(true);
                    });

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Something went wrong: " +
                            e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

            btnSaveProfile.setEnabled(true);
        }
    }
}
