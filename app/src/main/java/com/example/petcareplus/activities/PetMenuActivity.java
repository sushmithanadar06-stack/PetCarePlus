package com.example.petcareplus.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.petcareplus.R;
import com.example.petcareplus.models.Pet;
import com.example.petcareplus.utils.ImageUtils;

public class PetMenuActivity extends AppCompatActivity {

    private ImageView imgMenuPetPhoto;
    private TextView txtMenuPetName, txtMenuPetSubtitle;
    private TextView btnMenuClose;
    private TextView txtSwitchProfileLink, txtAddPetLink;

    private CardView cardProfileDetails, cardMedicalRecords, cardReminders, cardWellness, cardHealthReports;

    private Pet pet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_menu);

        imgMenuPetPhoto = findViewById(R.id.imgMenuPetPhoto);
        txtMenuPetName = findViewById(R.id.txtMenuPetName);
        txtMenuPetSubtitle = findViewById(R.id.txtMenuPetSubtitle);
        btnMenuClose = findViewById(R.id.btnMenuClose);
        txtSwitchProfileLink = findViewById(R.id.txtSwitchProfileLink);
        txtAddPetLink = findViewById(R.id.txtAddPetLink);

        cardProfileDetails = findViewById(R.id.cardProfileDetails);
        cardMedicalRecords = findViewById(R.id.cardMedicalRecords);
        cardReminders = findViewById(R.id.cardReminders);
        cardWellness = findViewById(R.id.cardWellness);
        cardHealthReports = findViewById(R.id.cardHealthReports);

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            pet = getIntent().getSerializableExtra("pet", Pet.class);
        } else {
            pet = (Pet) getIntent().getSerializableExtra("pet");
        }

        if (pet == null) {
            finish();
            return;
        }

        txtMenuPetName.setText(pet.getName());
        txtMenuPetSubtitle.setText(pet.getBreed() + " • " + pet.getType() + " • " + pet.getGender());

        String photoUri = pet.getPhotoUri();

        if (photoUri != null && !photoUri.trim().isEmpty()) {
            try {
                if (ImageUtils.isBase64Image(photoUri)) {

                    Bitmap bitmap = ImageUtils.base64ToBitmap(photoUri);

                    if (bitmap != null) {
                        imgMenuPetPhoto.setImageBitmap(bitmap);
                        imgMenuPetPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    } else {
                        setPlaceholderPhoto();
                    }

                } else {
                    imgMenuPetPhoto.setImageURI(Uri.parse(photoUri));
                    imgMenuPetPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
            } catch (Exception e) {
                setPlaceholderPhoto();
            }
        } else {
            setPlaceholderPhoto();
        }

        btnMenuClose.setOnClickListener(v -> finish());

        txtSwitchProfileLink.setOnClickListener(v ->
                startActivity(new Intent(PetMenuActivity.this, SwitchProfileActivity.class))
        );

        txtAddPetLink.setOnClickListener(v ->
                startActivity(new Intent(PetMenuActivity.this, AddPetActivity.class))
        );

        cardProfileDetails.setOnClickListener(v -> {
            Intent intent = new Intent(PetMenuActivity.this, EditPetActivity.class);
            intent.putExtra("pet", pet);
            startActivity(intent);
            finish();
        });

        cardMedicalRecords.setOnClickListener(v -> {
            Intent intent = new Intent(PetMenuActivity.this, MedicalRecordsHubActivity.class);
            intent.putExtra("pet", pet);
            startActivity(intent);
        });

        cardReminders.setOnClickListener(v -> {
            Intent intent = new Intent(PetMenuActivity.this, RemindersActivity.class);
            intent.putExtra("pet", pet);
            startActivity(intent);
        });

        cardWellness.setOnClickListener(v -> {
            Intent intent = new Intent(PetMenuActivity.this, WellnessActivity.class);
            intent.putExtra("pet", pet);
            startActivity(intent);
        });
    }

    private void setPlaceholderPhoto() {
        String type = pet.getType();
        int iconRes = (type != null && type.equalsIgnoreCase("Dog")) ? R.drawable.ic_species_dog : R.drawable.ic_species_cat;
        imgMenuPetPhoto.setImageResource(iconRes);
        imgMenuPetPhoto.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imgMenuPetPhoto.setPadding(20, 20, 20, 20);
    }

    private void showComingSoon() {
        Toast.makeText(this, "Coming in a future module", Toast.LENGTH_SHORT).show();
    }
}