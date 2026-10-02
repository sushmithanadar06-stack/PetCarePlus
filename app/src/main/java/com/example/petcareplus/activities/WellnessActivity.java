package com.example.petcareplus.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.models.Pet;
import com.example.petcareplus.utils.TodayScoreHelper;
import com.google.firebase.firestore.FirebaseFirestore;

public class WellnessActivity extends AppCompatActivity {

    private TextView btnWellnessBack, txtWellnessPetName, btnWellnessSwitch;
    private TextView txtOverallScore;
    private LinearLayout rowGrowthTimeline, rowDailyCare, rowMoodJournal, rowWellnessAnalytics;
    private TextView txtGrowthTimelineSubtitle;

    private LinearLayout navBarHome, navBarRecords, navBarRescue, navBarCommunity, navBarProfile;

    private Pet pet;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wellness);

        db = FirebaseFirestore.getInstance();

        btnWellnessBack = findViewById(R.id.btnWellnessBack);
        txtWellnessPetName = findViewById(R.id.txtWellnessPetName);
        btnWellnessSwitch = findViewById(R.id.btnWellnessSwitch);
        txtOverallScore = findViewById(R.id.txtOverallScore);
        rowGrowthTimeline = findViewById(R.id.rowGrowthTimeline);
        txtGrowthTimelineSubtitle = findViewById(R.id.txtGrowthTimelineSubtitle);
        rowDailyCare = findViewById(R.id.rowDailyCare);
        rowMoodJournal = findViewById(R.id.rowMoodJournal);
        rowWellnessAnalytics = findViewById(R.id.rowWellnessAnalytics);

        navBarHome = findViewById(R.id.navBarHome);
        navBarRecords = findViewById(R.id.navBarRecords);
        navBarRescue = findViewById(R.id.navBarRescue);
        navBarCommunity = findViewById(R.id.navBarCommunity);
        navBarProfile = findViewById(R.id.navBarProfile);

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            pet = getIntent().getSerializableExtra("pet", Pet.class);
        } else {
            pet = (Pet) getIntent().getSerializableExtra("pet");
        }

        if (pet == null) {
            Toast.makeText(this, "Unable to load pet", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        txtWellnessPetName.setText(pet.getName() + "'s Wellness");

        btnWellnessBack.setOnClickListener(v -> finish());

        btnWellnessSwitch.setOnClickListener(v -> {
            startActivity(new Intent(WellnessActivity.this, SwitchProfileActivity.class));
            finish();
        });

        rowGrowthTimeline.setOnClickListener(v -> {
            Intent intent = new Intent(WellnessActivity.this, WeightTimelineActivity.class);
            intent.putExtra("pet", pet);
            startActivity(intent);
        });

        rowDailyCare.setOnClickListener(v -> {
            Intent intent = new Intent(WellnessActivity.this, DailyCareActivity.class);
            intent.putExtra("pet", pet);
            startActivity(intent);
        });

        rowMoodJournal.setOnClickListener(v -> {
            Intent intent = new Intent(WellnessActivity.this, MoodJournalActivity.class);
            intent.putExtra("pet", pet);
            startActivity(intent);
        });

        rowWellnessAnalytics.setOnClickListener(v -> {
            Intent intent = new Intent(WellnessActivity.this, WellnessAnalyticsActivity.class);
            intent.putExtra("pet", pet);
            startActivity(intent);
        });

        setupBottomNav();
        loadWellnessScore();
    }

    private void setupBottomNav() {

        navBarHome.setOnClickListener(v -> {
            Intent intent = new Intent(WellnessActivity.this, PetListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        navBarRecords.setOnClickListener(v -> {
            Intent intent = new Intent(WellnessActivity.this, MedicalRecordsHubActivity.class);
            intent.putExtra("pet", pet);
            startActivity(intent);
        });

        navBarRescue.setOnClickListener(v -> Toast.makeText(this, "Coming in a future module", Toast.LENGTH_SHORT).show());
        navBarCommunity.setOnClickListener(v -> Toast.makeText(this, "Coming in a future module", Toast.LENGTH_SHORT).show());

        navBarProfile.setOnClickListener(v ->
                startActivity(new Intent(WellnessActivity.this, ProfileActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pet != null) loadWellnessScore();
    }

    private void loadWellnessScore() {

        TodayScoreHelper.computeTodayScore(db, pet, result -> {

            if (!result.hasAnyData) {
                txtOverallScore.setText("--");
                txtGrowthTimelineSubtitle.setText("Tap to log a weight");
                return;
            }

            txtOverallScore.setText(String.valueOf(result.score));
            txtGrowthTimelineSubtitle.setText(pet.getAge() + " old — updated today");
        });
    }
}