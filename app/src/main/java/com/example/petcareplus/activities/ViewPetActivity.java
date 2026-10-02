package com.example.petcareplus.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.models.Pet;

public class ViewPetActivity extends AppCompatActivity {

    private ImageView imgPetView;

    private TextView txtPetNameView;
    private TextView txtPetTypeView;
    private TextView txtPetBreedView;
    private TextView txtPetGenderView;
    private TextView txtPetDobView;
    private TextView txtPetAgeView;
    private TextView txtPetColorView;
    private TextView txtPetWeightView;

    private TextView btnClose;
    private TextView btnEditFromView;

    private Pet pet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_view_pet);

        imgPetView = findViewById(R.id.imgPetView);

        txtPetNameView = findViewById(R.id.txtPetNameView);
        txtPetTypeView = findViewById(R.id.txtPetTypeView);
        txtPetBreedView = findViewById(R.id.txtPetBreedView);
        txtPetGenderView = findViewById(R.id.txtPetGenderView);
        txtPetDobView = findViewById(R.id.txtPetDobView);
        txtPetAgeView = findViewById(R.id.txtPetAgeView);
        txtPetColorView = findViewById(R.id.txtPetColorView);
        txtPetWeightView = findViewById(R.id.txtPetWeightView);

        btnClose = findViewById(R.id.btnClose);
        btnEditFromView = findViewById(R.id.btnEditFromView);

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            pet = getIntent().getSerializableExtra("pet", Pet.class);
        } else {
            pet = (Pet) getIntent().getSerializableExtra("pet");
        }

        if (pet == null) {
            finish();
            return;
        }

        String photoUri = pet.getPhotoUri();

        if (photoUri != null && !photoUri.trim().isEmpty()) {

            try {
                Uri imageUri = Uri.parse(photoUri);
                imgPetView.setBackground(null);
                imgPetView.setPadding(0, 0, 0, 0);
                imgPetView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imgPetView.setImageURI(imageUri);
            } catch (Exception e) {
                setPlaceholderPhoto();
            }

        } else {
            setPlaceholderPhoto();
        }

        txtPetNameView.setText(pet.getName());
        txtPetTypeView.setText("Pet Type: " + pet.getType());
        txtPetBreedView.setText("Breed: " + pet.getBreed());
        txtPetGenderView.setText("Gender: " + pet.getGender());
        txtPetDobView.setText("Date of Birth: " + pet.getDateOfBirth());
        txtPetAgeView.setText("Age: " + pet.getAge());
        txtPetColorView.setText("Fur / Coat Color: " + pet.getColor());
        txtPetWeightView.setText("Weight: " + pet.getWeight() + " kg");

        btnClose.setOnClickListener(v -> finish());

        btnEditFromView.setOnClickListener(v -> {

            Intent intent = new Intent(ViewPetActivity.this, EditPetActivity.class);
            intent.putExtra("pet", pet);

            startActivity(intent);
            finish();
        });
    }

    // =====================================================
    // UPDATED REPLACEMENT METHOD
    // =====================================================
    private void setPlaceholderPhoto() {
        String type = pet.getType();
        int iconRes = (type != null && type.equalsIgnoreCase("Dog")) ? R.drawable.ic_species_dog : R.drawable.ic_species_cat;
        imgPetView.setImageResource(iconRes);
        imgPetView.setBackgroundResource(R.drawable.bg_avatar_placeholder);
        imgPetView.setPadding(20, 20, 20, 20);
        imgPetView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }
}
