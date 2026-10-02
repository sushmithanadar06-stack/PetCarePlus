package com.example.petcareplus.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.models.FoodSafetyItem;
import com.example.petcareplus.utils.FoodSafetyDatabase;

public class FoodResultActivity extends AppCompatActivity {

    private TextView btnFoodResultBack;
    private TextView txtFoodIconBig, txtFoodName;
    private TextView txtDogBadge, txtDogReason;
    private TextView txtCatBadge, txtCatReason;
    private TextView btnSearchAnother;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_result);

        btnFoodResultBack = findViewById(R.id.btnFoodResultBack);
        txtFoodIconBig = findViewById(R.id.txtFoodIconBig);
        txtFoodName = findViewById(R.id.txtFoodName);
        txtDogBadge = findViewById(R.id.txtDogBadge);
        txtDogReason = findViewById(R.id.txtDogReason);
        txtCatBadge = findViewById(R.id.txtCatBadge);
        txtCatReason = findViewById(R.id.txtCatReason);
        btnSearchAnother = findViewById(R.id.btnSearchAnother);

        String foodName = getIntent().getStringExtra("foodName");

        if (foodName == null) {
            Toast.makeText(this, "Unable to load food info", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FoodSafetyItem item = FoodSafetyDatabase.findExact(foodName);

        if (item == null) {
            Toast.makeText(this, "Food not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        txtFoodIconBig.setText(item.icon);
        txtFoodName.setText(item.name);

        txtDogBadge.setText(item.dogStatus);
        txtDogBadge.getBackground().setTint(Color.parseColor(colorFor(item.dogStatus)));
        txtDogReason.setText(item.dogReason);

        txtCatBadge.setText(item.catStatus);
        txtCatBadge.getBackground().setTint(Color.parseColor(colorFor(item.catStatus)));
        txtCatReason.setText(item.catReason);

        btnFoodResultBack.setOnClickListener(v -> finish());
        btnSearchAnother.setOnClickListener(v -> finish());
    }

    private String colorFor(String status) {
        switch (status) {
            case "Safe": return "#2F6B52";
            case "Unsafe": return "#F2704A";
            default: return "#E8B24B"; // Caution
        }
    }
}