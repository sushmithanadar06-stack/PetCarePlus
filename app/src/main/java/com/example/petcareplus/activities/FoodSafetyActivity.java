package com.example.petcareplus.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.models.FoodSafetyItem;
import com.example.petcareplus.utils.FoodSafetyDatabase;
import com.example.petcareplus.utils.RecentFoodCheckHelper;

import java.util.List;

public class FoodSafetyActivity extends AppCompatActivity {

    private TextView btnFoodSafetyBack;
    private EditText etFoodSearch;
    private TextView btnCheckFood;

    // Tabs
    private TextView btnTabChecker;
    private TextView btnTabNutrition;
    private TextView btnTabSnacks;

    private LinearLayout suggestionsContainer;
    private LinearLayout recentListContainer;
    private TextView txtNoRecent;
    private TextView txtRecentLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_safety);

        // Existing views
        btnFoodSafetyBack = findViewById(R.id.btnFoodSafetyBack);
        etFoodSearch = findViewById(R.id.etFoodSearch);
        btnCheckFood = findViewById(R.id.btnCheckFood);
        suggestionsContainer = findViewById(R.id.suggestionsContainer);
        recentListContainer = findViewById(R.id.recentListContainer);
        txtNoRecent = findViewById(R.id.txtNoRecent);
        txtRecentLabel = findViewById(R.id.txtRecentLabel);

        // Tab views
        btnTabChecker = findViewById(R.id.btnTabChecker);
        btnTabNutrition = findViewById(R.id.btnTabNutrition);
        btnTabSnacks = findViewById(R.id.btnTabSnacks);

        // Back button
        btnFoodSafetyBack.setOnClickListener(v -> finish());

        // Check food button
        btnCheckFood.setOnClickListener(v -> checkTypedFood());

        // Checker tab
        btnTabChecker.setOnClickListener(v -> {
            // Already on Food Safety Checker
        });

        // Nutrition tab
        btnTabNutrition.setOnClickListener(v -> {
            Intent intent = new Intent(
                    FoodSafetyActivity.this,
                    NutritionGuideActivity.class
            );
            startActivity(intent);
            finish();
        });

        // Snacks tab
        btnTabSnacks.setOnClickListener(v -> {
            Intent intent = new Intent(
                    FoodSafetyActivity.this,
                    PetSnacksActivity.class
            );
            startActivity(intent);
            finish();
        });

        // Search suggestions
        etFoodSearch.addTextChangedListener(new TextWatcher() {

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
                renderSuggestions(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Load recently checked foods
        renderRecent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderRecent();
    }

    private void checkTypedFood() {

        String query = etFoodSearch.getText().toString().trim();

        if (query.isEmpty()) {
            Toast.makeText(
                    this,
                    "Type a food name first",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        FoodSafetyItem exact = FoodSafetyDatabase.findExact(query);

        if (exact != null) {
            openResult(exact);
            return;
        }

        List<FoodSafetyItem> matches =
                FoodSafetyDatabase.search(query);

        if (!matches.isEmpty()) {
            openResult(matches.get(0));
        } else {
            Toast.makeText(
                    this,
                    "\"" + query + "\" isn't in our food list yet",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void renderSuggestions(String query) {

        suggestionsContainer.removeAllViews();

        if (query.trim().isEmpty()) {
            return;
        }

        List<FoodSafetyItem> matches =
                FoodSafetyDatabase.search(query);

        if (matches.isEmpty()) {
            return;
        }

        LayoutInflater inflater =
                LayoutInflater.from(this);

        int shown = 0;

        for (FoodSafetyItem item : matches) {

            if (shown >= 5) {
                break;
            }

            View row = inflater.inflate(
                    R.layout.item_food_suggestion,
                    suggestionsContainer,
                    false
            );

            TextView txtIcon =
                    row.findViewById(R.id.txtSuggestionIcon);

            TextView txtName =
                    row.findViewById(R.id.txtSuggestionName);

            txtIcon.setText(item.icon);
            txtName.setText(item.name);

            row.setOnClickListener(v -> openResult(item));

            suggestionsContainer.addView(row);

            shown++;
        }
    }

    private void openResult(FoodSafetyItem item) {

        // Save to recently checked
        RecentFoodCheckHelper.addRecent(
                this,
                item.name
        );

        Intent intent = new Intent(
                FoodSafetyActivity.this,
                FoodResultActivity.class
        );

        intent.putExtra(
                "foodName",
                item.name
        );

        startActivity(intent);
    }

    private void renderRecent() {

        recentListContainer.removeAllViews();

        List<String> recentNames =
                RecentFoodCheckHelper.getRecent(this);

        if (recentNames.isEmpty()) {

            txtNoRecent.setVisibility(View.VISIBLE);
            txtRecentLabel.setVisibility(View.GONE);

            return;
        }

        txtNoRecent.setVisibility(View.GONE);
        txtRecentLabel.setVisibility(View.VISIBLE);

        LayoutInflater inflater =
                LayoutInflater.from(this);

        for (String name : recentNames) {

            FoodSafetyItem item =
                    FoodSafetyDatabase.findExact(name);

            if (item == null) {
                continue;
            }

            View row = inflater.inflate(
                    R.layout.item_food_recent,
                    recentListContainer,
                    false
            );

            TextView txtIcon =
                    row.findViewById(R.id.txtRecentIcon);

            TextView txtName =
                    row.findViewById(R.id.txtRecentName);

            TextView txtSubtitle =
                    row.findViewById(R.id.txtRecentSubtitle);

            TextView txtBadge =
                    row.findViewById(R.id.txtRecentBadge);

            txtIcon.setText(item.icon);
            txtName.setText(item.name);

            boolean bothSafe =
                    "Safe".equals(item.dogStatus)
                            && "Safe".equals(item.catStatus);

            boolean eitherUnsafe =
                    "Unsafe".equals(item.dogStatus)
                            || "Unsafe".equals(item.catStatus);

            String overallLabel;
            String badgeColor;

            if (eitherUnsafe) {

                overallLabel = "Unsafe";
                badgeColor = "#F2704A";

            } else if (bothSafe) {

                overallLabel = "Safe";
                badgeColor = "#2F6B52";

            } else {

                overallLabel = "Caution";
                badgeColor = "#E8B24B";
            }

            txtSubtitle.setText(
                    "Marked " + overallLabel.toLowerCase()
            );

            txtBadge.setText(overallLabel);

            txtBadge.getBackground().setTint(
                    android.graphics.Color.parseColor(
                            badgeColor
                    )
            );

            row.setOnClickListener(
                    v -> openResult(item)
            );

            recentListContainer.addView(row);
        }
    }
}
