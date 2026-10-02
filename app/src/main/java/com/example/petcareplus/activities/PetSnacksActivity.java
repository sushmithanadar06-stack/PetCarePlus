package com.example.petcareplus.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.models.SnackItem;
import com.example.petcareplus.utils.SnackDatabase;

import java.util.List;

public class PetSnacksActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_snacks);

        TextView btnSnacksBack = findViewById(R.id.btnSnacksBack);
        TextView btnTabChecker = findViewById(R.id.btnTabChecker);
        TextView btnTabNutrition = findViewById(R.id.btnTabNutrition);

        LinearLayout dogContainer = findViewById(R.id.dogSnacksContainer);
        LinearLayout catContainer = findViewById(R.id.catSnacksContainer);

        btnSnacksBack.setOnClickListener(v -> finish());

        btnTabChecker.setOnClickListener(v -> {
            startActivity(new Intent(PetSnacksActivity.this, FoodSafetyActivity.class));
            finish();
        });

        btnTabNutrition.setOnClickListener(v -> {
            startActivity(new Intent(PetSnacksActivity.this, NutritionGuideActivity.class));
            finish();
        });

        LayoutInflater inflater = LayoutInflater.from(this);
        List<SnackItem> dogSnacks = SnackDatabase.getForSpecies("Dog");
        List<SnackItem> catSnacks = SnackDatabase.getForSpecies("Cat");

        for (SnackItem item : dogSnacks) {
            addSnackRow(inflater, dogContainer, item);
        }
        for (SnackItem item : catSnacks) {
            addSnackRow(inflater, catContainer, item);
        }
    }

    private void addSnackRow(LayoutInflater inflater, LinearLayout container, SnackItem item) {

        View row = inflater.inflate(R.layout.item_snack, container, false);

        TextView txtIcon = row.findViewById(R.id.txtSnackIcon);
        TextView txtName = row.findViewById(R.id.txtSnackName);
        TextView txtNote = row.findViewById(R.id.txtSnackNote);

        txtIcon.setText(item.icon);
        txtName.setText(item.name);
        txtNote.setText(item.note);

        container.addView(row);
    }
}