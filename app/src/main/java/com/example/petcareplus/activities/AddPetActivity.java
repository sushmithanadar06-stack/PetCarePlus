package com.example.petcareplus.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.models.Pet;
import com.example.petcareplus.utils.ImageUtils;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class AddPetActivity extends AppCompatActivity {

    private ImageView imgPetPhoto;
    private TextView txtPetPhotoLetter;
    private Button btnAddPetPhoto;
    private EditText etPetName;
    private Spinner spPetType, spPetBreed, spPetGender, spPetColor;
    private EditText etCustomBreed, etCustomColor, etPetWeight;
    private Button btnPetDob, btnSavePet;
    private TextView btnAddPetBack, txtPetAge;

    private String selectedDateOfBirth = "";
    private String selectedPhotoBase64 = "";
    private final Calendar birthDate = Calendar.getInstance();
    private static final int PICK_IMAGE = 100;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_pet);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("petcare", MODE_PRIVATE);

        imgPetPhoto = findViewById(R.id.imgPetPhoto);
        txtPetPhotoLetter = findViewById(R.id.txtPetPhotoLetter);
        btnAddPetPhoto = findViewById(R.id.btnAddPetPhoto);
        etPetName = findViewById(R.id.etPetName);
        spPetType = findViewById(R.id.spPetType);
        spPetBreed = findViewById(R.id.spPetBreed);
        etCustomBreed = findViewById(R.id.etCustomBreed);
        spPetGender = findViewById(R.id.spPetGender);
        btnPetDob = findViewById(R.id.btnPetDob);
        txtPetAge = findViewById(R.id.txtPetAge);
        spPetColor = findViewById(R.id.spPetColor);
        etCustomColor = findViewById(R.id.etCustomColor);
        etPetWeight = findViewById(R.id.etPetWeight);
        btnSavePet = findViewById(R.id.btnSavePet);
        btnAddPetBack = findViewById(R.id.btnAddPetBack);

        setupPetTypes();
        setupGender();
        setupColors();
        updatePhotoPlaceholder();

        btnAddPetBack.setOnClickListener(v -> confirmDiscardAndExit());

        spPetType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setupBreeds(parent.getItemAtPosition(position).toString());
                updatePhotoPlaceholder();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spPetBreed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String breed = parent.getItemAtPosition(position).toString();
                etCustomBreed.setVisibility(breed.equals("Other") ? View.VISIBLE : View.GONE);
                if (!breed.equals("Other")) etCustomBreed.setText("");
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spPetColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String color = parent.getItemAtPosition(position).toString();
                etCustomColor.setVisibility(color.equals("Other") ? View.VISIBLE : View.GONE);
                if (!color.equals("Other")) etCustomColor.setText("");
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnAddPetPhoto.setOnClickListener(v -> openImagePicker());
        btnPetDob.setOnClickListener(v -> showDatePicker());
        btnSavePet.setOnClickListener(v -> savePetToFirebase());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmDiscardAndExit();
            }
        });
    }

    private void confirmDiscardAndExit() {

        boolean hasUnsavedData =
                !etPetName.getText().toString().trim().isEmpty()
                        || !selectedPhotoBase64.isEmpty()
                        || !selectedDateOfBirth.isEmpty()
                        || !etPetWeight.getText().toString().trim().isEmpty();

        if (!hasUnsavedData) {
            finish();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Discard this pet profile?")
                .setMessage("You have unsaved changes. If you leave now, this information will be lost.")
                .setPositiveButton("Discard", (dialog, which) -> finish())
                .setNegativeButton("Keep Editing", null)
                .show();
    }

    private void updatePhotoPlaceholder() {
        if (selectedPhotoBase64.isEmpty()) {
            String type = spPetType.getSelectedItem() != null ? spPetType.getSelectedItem().toString() : "Cat";
            imgPetPhoto.setVisibility(View.GONE);
            txtPetPhotoLetter.setVisibility(View.VISIBLE);
            txtPetPhotoLetter.setText(type.equalsIgnoreCase("Dog") ? "D" : "C");
        }
    }

    private void setupPetTypes() {
        String[] petTypes = {"Select Pet Type", "Dog", "Cat"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, petTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPetType.setAdapter(adapter);
    }

    private void setupBreeds(String petType) {
        String[] breeds;
        if (petType.equals("Dog")) {
            breeds = new String[]{"Select Breed","Labrador Retriever","Golden Retriever","German Shepherd","Beagle","Pug","Shih Tzu","Rottweiler","Indie","Dachshund","Boxer","Doberman","Great Dane","Siberian Husky","Pomeranian","Cocker Spaniel","Border Collie","Chihuahua","French Bulldog","German Shorthaired Pointer","Australian Shepherd","Saint Bernard","Cane Corso","Corgi","Dalmatian","Other"};
        } else if (petType.equals("Cat")) {
            breeds = new String[]{"Select Breed","Persian","Siamese","Maine Coon","British Shorthair","Bengal","Ragdoll","Indian Domestic Shorthair","American Shorthair","Scottish Fold","Sphynx","Russian Blue","Abyssinian","Birman","Norwegian Forest Cat","Oriental Shorthair","Turkish Angora","Himalayan","Burmese","Bombay","Savannah","Manx","Exotic Shorthair","Turkish Van","Cornish Rex","Other"};
        } else {
            breeds = new String[]{"Select Breed"};
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, breeds);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPetBreed.setAdapter(adapter);
        etCustomBreed.setVisibility(View.GONE);
        etCustomBreed.setText("");
    }

    private void setupGender() {
        String[] genders = {"Select Gender", "Male", "Female"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genders);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPetGender.setAdapter(adapter);
    }

    private void setupColors() {
        String[] colors = {"Select Color","Black","White","Brown","Golden","Gray","Orange","Cream","Black & White","Brown & White","Gray & White","Orange & White","Tricolor","Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, colors);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPetColor.setAdapter(adapter);
        etCustomColor.setVisibility(View.GONE);
    }

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
                imgPetPhoto.setVisibility(View.VISIBLE);
                txtPetPhotoLetter.setVisibility(View.GONE);
                imgPetPhoto.setImageBitmap(ImageUtils.base64ToBitmap(base64));
                imgPetPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Toast.makeText(this, "Photo added", Toast.LENGTH_SHORT).show();
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

        if (!selectedDateOfBirth.isEmpty()) {
            builder.setSelection(birthDate.getTimeInMillis());
        }

        MaterialDatePicker<Long> picker = builder.build();

        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utc.setTimeInMillis(selection);
            birthDate.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH));

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            selectedDateOfBirth = dateFormat.format(birthDate.getTime());
            btnPetDob.setText(selectedDateOfBirth);
            calculateAge();
        });

        picker.show(getSupportFragmentManager(), "DOB_PICKER");
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
            txtPetAge.setText("Age: " + totalMonths + (totalMonths == 1 ? " month" : " months"));
        } else {
            txtPetAge.setText("Age: " + years + (years == 1 ? " year" : " years"));
        }
    }

    private void savePetToFirebase() {

        if (!btnSavePet.isEnabled()) {
            return;
        }

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be logged in to add a pet", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            btnSavePet.setEnabled(false);
            btnSavePet.setText("Saving...");

            String petName = etPetName.getText().toString().trim();
            String petType = spPetType.getSelectedItem() != null ? spPetType.getSelectedItem().toString() : "";
            String selectedBreed = spPetBreed.getSelectedItem() != null ? spPetBreed.getSelectedItem().toString() : "";
            String petGender = spPetGender.getSelectedItem() != null ? spPetGender.getSelectedItem().toString() : "";
            String petColor = spPetColor.getSelectedItem() != null ? spPetColor.getSelectedItem().toString() : "";
            String customBreed = etCustomBreed.getText().toString().trim();
            String customColor = etCustomColor.getText().toString().trim();
            String petWeight = etPetWeight.getText().toString().trim();

            if (petName.isEmpty()) { etPetName.setError("Please enter your pet's name"); etPetName.requestFocus(); resetSaveButton(); return; }
            if (petType.isEmpty() || petType.equals("Select Pet Type")) { Toast.makeText(this, "Please select Dog or Cat", Toast.LENGTH_SHORT).show(); resetSaveButton(); return; }
            if (selectedBreed.isEmpty() || selectedBreed.equals("Select Breed")) { Toast.makeText(this, "Please select your pet's breed", Toast.LENGTH_SHORT).show(); resetSaveButton(); return; }
            if (selectedBreed.equals("Other")) {
                if (customBreed.isEmpty()) { etCustomBreed.setError("Please enter your pet's breed"); etCustomBreed.requestFocus(); resetSaveButton(); return; }
                selectedBreed = customBreed;
            }
            if (petGender.isEmpty() || petGender.equals("Select Gender")) { Toast.makeText(this, "Please select your pet's gender", Toast.LENGTH_SHORT).show(); resetSaveButton(); return; }
            if (selectedDateOfBirth.isEmpty()) { Toast.makeText(this, "Please select your pet's date of birth", Toast.LENGTH_SHORT).show(); resetSaveButton(); return; }
            if (petColor.isEmpty() || petColor.equals("Select Color")) { Toast.makeText(this, "Please select your pet's color", Toast.LENGTH_SHORT).show(); resetSaveButton(); return; }
            if (petColor.equals("Other")) {
                if (customColor.isEmpty()) { etCustomColor.setError("Please enter the color"); etCustomColor.requestFocus(); resetSaveButton(); return; }
                petColor = customColor;
            }
            if (petWeight.isEmpty()) { etPetWeight.setError("Please enter your pet's weight"); etPetWeight.requestFocus(); resetSaveButton(); return; }

            String petAge = txtPetAge.getText().toString().replace("Age: ", "").trim();
            final String finalBreed = selectedBreed;
            final String finalColor = petColor;
            final String finalName = petName;
            final String finalType = petType;
            final String finalGender = petGender;
            final String finalWeight = petWeight;
            final String ownerId = auth.getCurrentUser().getUid();

            DocumentReference newDocRef = db.collection("pets").document();
            String newPetId = newDocRef.getId();

            Pet newPet = new Pet(
                    newPetId,
                    finalName,
                    finalType,
                    finalBreed,
                    finalGender,
                    selectedDateOfBirth,
                    petAge,
                    finalColor,
                    finalWeight,
                    selectedPhotoBase64,
                    ownerId
            );

            newDocRef.set(newPet)
                    .addOnSuccessListener(unused -> {

                        prefs.edit().putString("current_pet_id", newPetId).apply();

                        Intent intent = new Intent(AddPetActivity.this, PetListActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("toastMessage", finalName + "'s profile created successfully!");
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddPetActivity.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        resetSaveButton();
                    });

        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong: " + e.getMessage(), Toast.LENGTH_LONG).show();
            resetSaveButton();
        }
    }

    private void resetSaveButton() {
        btnSavePet.setEnabled(true);
        btnSavePet.setText("Create Pet Profile");
    }
}