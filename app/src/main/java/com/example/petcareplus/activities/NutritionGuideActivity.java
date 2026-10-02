package com.example.petcareplus.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.models.NutritionStage;
import com.example.petcareplus.utils.NutritionGuideDatabase;

public class NutritionGuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nutrition_guide);

        TextView btnNutritionBack = findViewById(R.id.btnNutritionBack);
        TextView btnTabChecker = findViewById(R.id.btnTabChecker);
        TextView btnTabSnacks = findViewById(R.id.btnTabSnacks);

        LinearLayout dogContainer = findViewById(R.id.dogNutritionContainer);
        LinearLayout catContainer = findViewById(R.id.catNutritionContainer);
        LinearLayout tipsContainer = findViewById(R.id.generalTipsContainer);

        btnNutritionBack.setOnClickListener(v -> finish());

        btnTabChecker.setOnClickListener(v -> {
            startActivity(new Intent(NutritionGuideActivity.this, FoodSafetyActivity.class));
            finish();
        });

        btnTabSnacks.setOnClickListener(v -> {
            startActivity(new Intent(NutritionGuideActivity.this, PetSnacksActivity.class));
            finish();
        });

        LayoutInflater inflater = LayoutInflater.from(this);

        for (NutritionStage stage : NutritionGuideDatabase.getDogStages()) {
            addStageCard(inflater, dogContainer, stage);
        }

        for (NutritionStage stage : NutritionGuideDatabase.getCatStages()) {
            addStageCard(inflater, catContainer, stage);
        }

        for (String tip : NutritionGuideDatabase.getGeneralTips()) {
            TextView tipView = new TextView(this);
            tipView.setText("• " + tip);
            tipView.setTextSize(12);
            tipView.setTextColor(0xFF4B5C54);
            tipView.setPadding(0, 0, 0, 16);
            tipsContainer.addView(tipView);
        }
    }

    private void addStageCard(LayoutInflater inflater, LinearLayout container, NutritionStage stage) {

        View card = inflater.inflate(R.layout.item_nutrition_card, container, false);

        TextView txtStage = card.findViewById(R.id.txtNutritionStage);
        TextView txtProtein = card.findViewById(R.id.txtNutritionProtein);
        TextView txtFat = card.findViewById(R.id.txtNutritionFat);
        TextView txtFrequency = card.findViewById(R.id.txtNutritionFrequency);
        TextView txtNotes = card.findViewById(R.id.txtNutritionNotes);

        txtStage.setText(stage.stage);
        txtProtein.setText(stage.proteinRange);
        txtFat.setText(stage.fatRange);
        txtFrequency.setText(stage.feedingFrequency);
        txtNotes.setText(stage.notes);

        container.addView(card);
    }
}