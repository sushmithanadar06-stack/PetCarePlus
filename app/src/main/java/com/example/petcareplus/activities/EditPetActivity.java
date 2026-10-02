package com.example.petcareplus.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.models.Pet;
import com.example.petcareplus.utils.ImageUtils;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class EditPetActivity extends AppCompatActivity {

    private ImageView imgEditPetPhoto;
    private TextView txtEditPetPhotoLetter;
    private Button btnEditPetPhoto, btnEditPetDob, btnUpdatePet, btnDeletePetProfile;
    private EditText etEditPetName, etEditCustomBreed, etEditCustomColor, etEditPetWeight;
    private Spinner spEditPetType, spEditPetBreed, spEditPetGender, spEditPetColor;
    private TextView txtEditPetAge;

    private Pet originalPet;
    private String selectedDob = "";
    private String selectedPhotoBase64 = "";
    private final Calendar birthDate = Calendar.getInstance();
    private FirebaseFirestore db;

    private boolean hasUnsavedChanges = false;

    private final TextWatcher dirtyWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            hasUnsavedChanges = true;
        }
        @Override public void afterTextChanged(Editable s) {}
    };

    private static final int PICK_IMAGE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_pet);

        db = FirebaseFirestore.getInstance();

        imgEditPetPhoto = findViewById(R.id.imgEditPetPhoto);
        txtEditPetPhotoLetter = findViewById(R.id.txtEditPetPhotoLetter);
        btnEditPetPhoto = findViewById(R.id.btnEditPetPhoto);
        btnEditPetDob = findViewById(R.id.btnEditPetDob);
        btnUpdatePet = findViewById(R.id.btnUpdatePet);
        btnDeletePetProfile = findViewById(R.id.btnDeletePetProfile);
        etEditPetName = findViewById(R.id.etEditPetName);
        etEditCustomBreed = findViewById(R.id.etEditCustomBreed);
        etEditCustomColor = findViewById(R.id.etEditCustomColor);
        etEditPetWeight = findViewById(R.id.etEditPetWeight);
        spEditPetType = findViewById(R.id.spEditPetType);
        spEditPetBreed = findViewById(R.id.spEditPetBreed);
        spEditPetGender = findViewById(R.id.spEditPetGender);
        spEditPetColor = findViewById(R.id.spEditPetColor);
        txtEditPetAge = findViewById(R.id.txtEditPetAge);

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            originalPet = getIntent().getSerializableExtra("pet", Pet.class);
        } else {
            originalPet = (Pet) getIntent().getSerializableExtra("pet");
        }

        if (originalPet == null) {
            Toast.makeText(this, "Unable to load pet information", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupSpinners();
        loadPetData();

        spEditPetType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String type = parent.getItemAtPosition(position).toString();
                if (!type.equals(originalPet.getType())) {
                    setupBreedSpinner(type);
                }
                hasUnsavedChanges = true;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spEditPetBreed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String breed = parent.getItemAtPosition(position).toString();
                etEditCustomBreed.setVisibility(breed.equalsIgnoreCase("Other") ? View.VISIBLE : View.GONE);
                hasUnsavedChanges = true;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spEditPetGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                hasUnsavedChanges = true;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spEditPetColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String color = parent.getItemAtPosition(position).toString();
                etEditCustomColor.setVisibility(color.equalsIgnoreCase("Other") ? View.VISIBLE : View.GONE);
                hasUnsavedChanges = true;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        etEditPetName.addTextChangedListener(dirtyWatcher);
        etEditPetWeight.addTextChangedListener(dirtyWatcher);
        etEditCustomBreed.addTextChangedListener(dirtyWatcher);
        etEditCustomColor.addTextChangedListener(dirtyWatcher);

        btnEditPetPhoto.setOnClickListener(v -> openImagePicker());
        btnEditPetDob.setOnClickListener(v -> showDatePicker());
        btnUpdatePet.setOnClickListener(v -> confirmSaveChanges());
        btnDeletePetProfile.setOnClickListener(v -> confirmDelete());
    }

    // =========================================================
    // UNSAVED CHANGES HANDLING
    // =========================================================

    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges) {
            showUnsavedChangesDialog();
        } else {
            super.onBackPressed();
        }
    }

    private void showUnsavedChangesDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Unsaved changes")
                .setMessage("You have unsaved changes. Do you want to save them before leaving?")
                .setPositiveButton("Save", (dialog, which) -> updatePet())
                .setNegativeButton("Discard", (dialog, which) -> {
                    hasUnsavedChanges = false;
                    finish();
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void confirmSaveChanges() {
        new AlertDialog.Builder(this)
                .setTitle("Save changes?")
                .setMessage("Do you want to save these changes to " + originalPet.getName() + "'s profile?")
                .setPositiveButton("Save", (dialog, which) -> updatePet())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // =========================================================
    // PHOTO PICKING
    // =========================================================

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {}
            startCrop(imageUri);
            return;
        }

        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK && data != null) {
            Uri croppedUri = UCrop.getOutput(data);
            if (croppedUri != null) {

                String base64 = ImageUtils.uriToCompressedBase64(this, croppedUri);

                if (base64.isEmpty()) {
                    Toast.makeText(this, "Unable to process photo", Toast.LENGTH_SHORT).show();
                    return;
                }

                selectedPhotoBase64 = base64;
                imgEditPetPhoto.setVisibility(View.VISIBLE);
                txtEditPetPhotoLetter.setVisibility(View.GONE);
                imgEditPetPhoto.setImageBitmap(ImageUtils.base64ToBitmap(base64));
                imgEditPetPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                hasUnsavedChanges = true;
                Toast.makeText(this, "Photo updated", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == UCrop.RESULT_ERROR && data != null) {
            Toast.makeText(this, "Unable to crop photo", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCrop(Uri sourceUri) {
        String destinationFileName = "pet_crop_" + System.currentTimeMillis() + ".jpg";
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), destinationFileName));
        UCrop.Options options = new UCrop.Options();
        options.setToolbarTitle("Crop Pet Photo");
        options.setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(90);
        UCrop.of(sourceUri, destinationUri).withAspectRatio(1, 1).withMaxResultSize(1200, 1200).withOptions(options).start(this);
    }

    // =========================================================
    // SPINNERS
    // =========================================================

    private void setupSpinners() {
        String[] types = {"Select Pet Type", "Dog", "Cat"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEditPetType.setAdapter(typeAdapter);

        String[] genders = {"Select Gender", "Male", "Female"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genders);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEditPetGender.setAdapter(genderAdapter);

        String[] colors = {"Select Color","Black","White","Brown","Golden","Gray","Orange","Cream","Black & White","Brown & White","Gray & White","Orange & White","Tricolor","Other"};
        ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, colors);
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEditPetColor.setAdapter(colorAdapter);

        setupBreedSpinner(originalPet.getType());
    }

    private void setupBreedSpinner(String petType) {
        String[] breeds;
        if ("Dog".equals(petType)) {
            breeds = new String[]{"Select Breed","Labrador Retriever","Golden Retriever","German Shepherd","Beagle","Pug","Shih Tzu","Rottweiler","Indie","Dachshund","Boxer","Doberman","Great Dane","Siberian Husky","Pomeranian","Cocker Spaniel","Border Collie","Chihuahua","French Bulldog","German Shorthaired Pointer","Australian Shepherd","Saint Bernard","Cane Corso","Corgi","Dalmatian","Other"};
        } else if ("Cat".equals(petType)) {
            breeds = new String[]{"Select Breed","Persian","Siamese","Maine Coon","British Shorthair","Bengal","Ragdoll","Indian Domestic Shorthair","American Shorthair","Scottish Fold","Sphynx","Russian Blue","Abyssinian","Birman","Norwegian Forest Cat","Oriental Shorthair","Turkish Angora","Himalayan","Burmese","Bombay","Savannah","Manx","Exotic Shorthair","Turkish Van","Cornish Rex","Other"};
        } else {
            breeds = new String[]{"Select Breed"};
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, breeds);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEditPetBreed.setAdapter(adapter);
    }

    // =========================================================
    // LOAD DATA
    // =========================================================

    private void loadPetData() {
        etEditPetName.setText(originalPet.getName());
        etEditPetWeight.setText(originalPet.getWeight());

        ArrayAdapter<String> typeAdapter = (ArrayAdapter<String>) spEditPetType.getAdapter();
        int typePos = typeAdapter.getPosition(originalPet.getType());
        if (typePos >= 0) spEditPetType.setSelection(typePos);

        ArrayAdapter<String> genderAdapter = (ArrayAdapter<String>) spEditPetGender.getAdapter();
        int genderPos = genderAdapter.getPosition(originalPet.getGender());
        if (genderPos >= 0) spEditPetGender.setSelection(genderPos);

        ArrayAdapter<String> colorAdapter = (ArrayAdapter<String>) spEditPetColor.getAdapter();
        int colorPos = colorAdapter.getPosition(originalPet.getColor());
        if (colorPos >= 0) {
            spEditPetColor.setSelection(colorPos);
        } else {
            spEditPetColor.setSelection(colorAdapter.getPosition("Other"));
            etEditCustomColor.setText(originalPet.getColor());
            etEditCustomColor.setVisibility(View.VISIBLE);
        }

        ArrayAdapter<String> breedAdapter = (ArrayAdapter<String>) spEditPetBreed.getAdapter();
        int breedPos = breedAdapter.getPosition(originalPet.getBreed());
        if (breedPos >= 0) {
            spEditPetBreed.setSelection(breedPos);
        } else {
            spEditPetBreed.setSelection(breedAdapter.getPosition("Other"));
            etEditCustomBreed.setText(originalPet.getBreed());
            etEditCustomBreed.setVisibility(View.VISIBLE);
        }

        selectedDob = originalPet.getDateOfBirth();
        if (selectedDob != null && !selectedDob.isEmpty()) {
            btnEditPetDob.setText(selectedDob);
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                birthDate.setTime(sdf.parse(selectedDob));
                calculateAge();
            } catch (Exception e) {
                txtEditPetAge.setText("Age will be calculated from date of birth");
            }
        }

        selectedPhotoBase64 = originalPet.getPhotoUri();

        if (selectedPhotoBase64 != null && !selectedPhotoBase64.isEmpty()) {
            try {
                if (ImageUtils.isBase64Image(selectedPhotoBase64)) {

                    android.graphics.Bitmap bitmap = ImageUtils.base64ToBitmap(selectedPhotoBase64);

                    if (bitmap != null) {
                        imgEditPetPhoto.setVisibility(View.VISIBLE);
                        txtEditPetPhotoLetter.setVisibility(View.GONE);
                        imgEditPetPhoto.setImageBitmap(bitmap);
                        imgEditPetPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    } else {
                        setPlaceholderPhoto();
                    }

                } else {
                    imgEditPetPhoto.setVisibility(View.VISIBLE);
                    txtEditPetPhotoLetter.setVisibility(View.GONE);
                    imgEditPetPhoto.setImageURI(Uri.parse(selectedPhotoBase64));
                    imgEditPetPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
            } catch (Exception e) {
                setPlaceholderPhoto();
            }
        } else {
            setPlaceholderPhoto();
        }
    }

    private void setPlaceholderPhoto() {
        String type = originalPet.getType();
        imgEditPetPhoto.setVisibility(View.GONE);
        txtEditPetPhotoLetter.setVisibility(View.VISIBLE);
        txtEditPetPhotoLetter.setText((type != null && type.equalsIgnoreCase("Dog")) ? "D" : "C");
    }

    // =========================================================
    // DATE PICKER
    // =========================================================

    private void showDatePicker() {
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
        builder.setTitleText("Select Date of Birth");
        builder.setTheme(R.style.GreenMaterialDatePicker);
        long todayMillis = System.currentTimeMillis();

        builder.setCalendarConstraints(
                new CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointBackward.now())
                        .setEnd(todayMillis)
                        .setOpenAt(todayMillis)
                        .build()
        );
        builder.setSelection(birthDate.getTimeInMillis());

        MaterialDatePicker<Long> picker = builder.build();

        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utc.setTimeInMillis(selection);
            birthDate.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH));

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            selectedDob = sdf.format(birthDate.getTime());
            btnEditPetDob.setText(selectedDob);
            calculateAge();
            hasUnsavedChanges = true;
        });

        picker.show(getSupportFragmentManager(), "EDIT_DOB_PICKER");
    }

    private void calculateAge() {
        Calendar today = Calendar.getInstance();
        int years = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR);
        int months = today.get(Calendar.MONTH) - birthDate.get(Calendar.MONTH);
        if (months < 0 || (months == 0 && today.get(Calendar.DAY_OF_MONTH) < birthDate.get(Calendar.DAY_OF_MONTH))) years--;
        if (years < 0) years = 0;

        if (years == 0) {
            int totalMonths = (today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)) * 12 + today.get(Calendar.MONTH) - birthDate.get(Calendar.MONTH);
            if (today.get(Calendar.DAY_OF_MONTH) < birthDate.get(Calendar.DAY_OF_MONTH)) totalMonths--;
            if (totalMonths < 0) totalMonths = 0;
            txtEditPetAge.setText("Age: " + totalMonths + (totalMonths == 1 ? " month" : " months"));
        } else {
            txtEditPetAge.setText("Age: " + years + (years == 1 ? " year" : " years"));
        }
    }

    // =========================================================
    // UPDATE
    // =========================================================

    private void updatePet() {

        try {
            btnUpdatePet.setEnabled(false);

            String name = etEditPetName.getText().toString().trim();
            String type = spEditPetType.getSelectedItem() != null ? spEditPetType.getSelectedItem().toString() : "";
            String breed = spEditPetBreed.getSelectedItem() != null ? spEditPetBreed.getSelectedItem().toString() : "";
            String gender = spEditPetGender.getSelectedItem() != null ? spEditPetGender.getSelectedItem().toString() : "";
            String color = spEditPetColor.getSelectedItem() != null ? spEditPetColor.getSelectedItem().toString() : "";
            String weight = etEditPetWeight.getText().toString().trim();

            if (name.isEmpty()) {
                etEditPetName.setError("Enter pet name");
                etEditPetName.requestFocus();
                Toast.makeText(this, "Please enter your pet's name", Toast.LENGTH_SHORT).show();
                btnUpdatePet.setEnabled(true);
                return;
            }
            if (type.isEmpty() || type.equals("Select Pet Type")) { Toast.makeText(this, "Select pet type", Toast.LENGTH_SHORT).show(); btnUpdatePet.setEnabled(true); return; }
            if (breed.isEmpty() || breed.equals("Select Breed")) { Toast.makeText(this, "Select breed", Toast.LENGTH_SHORT).show(); btnUpdatePet.setEnabled(true); return; }
            if (breed.equals("Other")) {
                breed = etEditCustomBreed.getText().toString().trim();
                if (breed.isEmpty()) { etEditCustomBreed.setError("Enter breed"); etEditCustomBreed.requestFocus(); btnUpdatePet.setEnabled(true); return; }
            }
            if (gender.isEmpty() || gender.equals("Select Gender")) { Toast.makeText(this, "Select gender", Toast.LENGTH_SHORT).show(); btnUpdatePet.setEnabled(true); return; }
            if (selectedDob.isEmpty()) { Toast.makeText(this, "Select date of birth", Toast.LENGTH_SHORT).show(); btnUpdatePet.setEnabled(true); return; }
            if (color.isEmpty() || color.equals("Select Color")) { Toast.makeText(this, "Select color", Toast.LENGTH_SHORT).show(); btnUpdatePet.setEnabled(true); return; }
            if (color.equals("Other")) {
                color = etEditCustomColor.getText().toString().trim();
                if (color.isEmpty()) { etEditCustomColor.setError("Enter color"); etEditCustomColor.requestFocus(); btnUpdatePet.setEnabled(true); return; }
            }
            if (weight.isEmpty()) {
                etEditPetWeight.setError("Enter weight");
                etEditPetWeight.requestFocus();
                Toast.makeText(this, "Please enter your pet's weight", Toast.LENGTH_SHORT).show();
                btnUpdatePet.setEnabled(true);
                return;
            }

            String age = txtEditPetAge.getText().toString().replace("Age: ", "").trim();
            final String finalName = name;

            if (originalPet.getId() == null || originalPet.getId().isEmpty()) {
                Toast.makeText(this, "Cannot update: missing pet ID", Toast.LENGTH_LONG).show();
                btnUpdatePet.setEnabled(true);
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("type", type);
            updates.put("breed", breed);
            updates.put("gender", gender);
            updates.put("dateOfBirth", selectedDob);
            updates.put("age", age);
            updates.put("color", color);
            updates.put("weight", weight);
            updates.put("photoUri", selectedPhotoBase64);

            db.collection("pets").document(originalPet.getId())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        hasUnsavedChanges = false;
                        Intent intent = new Intent(EditPetActivity.this, PetListActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("toastMessage", finalName + "'s profile updated successfully!");
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(EditPetActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnUpdatePet.setEnabled(true);
                    });

        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong: " + e.getMessage(), Toast.LENGTH_LONG).show();
            btnUpdatePet.setEnabled(true);
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete " + originalPet.getName() + "?")
                .setMessage("This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deletePet())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePet() {

        try {
            btnDeletePetProfile.setEnabled(false);

            if (originalPet.getId() == null || originalPet.getId().isEmpty()) {
                Toast.makeText(this, "Cannot delete: missing pet ID", Toast.LENGTH_LONG).show();
                btnDeletePetProfile.setEnabled(true);
                return;
            }

            final String petName = originalPet.getName();

            db.collection("pets").document(originalPet.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        hasUnsavedChanges = false;
                        Intent intent = new Intent(EditPetActivity.this, PetListActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("toastMessage", petName + "'s profile deleted successfully");
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(EditPetActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnDeletePetProfile.setEnabled(true);
                    });

        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong: " + e.getMessage(), Toast.LENGTH_LONG).show();
            btnDeletePetProfile.setEnabled(true);
        }
    }
}