package com.example.petcareplus.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.models.Pet;
import com.example.petcareplus.models.WellnessEntry;
import com.example.petcareplus.utils.DailySummaryHelper;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MoodJournalActivity extends AppCompatActivity {

    private TextView btnMoodBack;
    private LinearLayout moodHappy, moodPlayful, moodNeutral, moodTired, moodSad, moodSick;
    private TextView txtMoodEmojiHappy, txtMoodEmojiPlayful, txtMoodEmojiNeutral,
            txtMoodEmojiTired, txtMoodEmojiSad, txtMoodEmojiSick;
    private LinearLayout moodLogList;
    private TextView txtNoMoodLog;

    private Pet pet;
    private FirebaseFirestore db;
    private final List<WellnessEntry> entries = new ArrayList<>();

    // Pure cat-face emoji only — genuine Unicode cat expressions
    private static final Map<String, String> CAT_MOODS = new HashMap<>();
    // Unicode has no full set of "dog expressions" like it does for cats,
    // so these are the closest pure dog/animal glyphs available (no human smileys)
    private static final Map<String, String> DOG_MOODS = new HashMap<>();
    static {
        CAT_MOODS.put("Happy", "😻");
        CAT_MOODS.put("Playful", "😸");
        CAT_MOODS.put("Neutral", "🐱");
        CAT_MOODS.put("Tired", "😽");
        CAT_MOODS.put("Sad", "😿");
        CAT_MOODS.put("Sick", "🙀");

        DOG_MOODS.put("Happy", "🐶");
        DOG_MOODS.put("Playful", "🐕");
        DOG_MOODS.put("Neutral", "🐩");
        DOG_MOODS.put("Tired", "🦮");
        DOG_MOODS.put("Sad", "🐕‍🦺");
        DOG_MOODS.put("Sick", "🐾");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_journal);

        db = FirebaseFirestore.getInstance();

        btnMoodBack = findViewById(R.id.btnMoodBack);
        moodHappy = findViewById(R.id.moodHappy);
        moodPlayful = findViewById(R.id.moodPlayful);
        moodNeutral = findViewById(R.id.moodNeutral);
        moodTired = findViewById(R.id.moodTired);
        moodSad = findViewById(R.id.moodSad);
        moodSick = findViewById(R.id.moodSick);

        txtMoodEmojiHappy = findViewById(R.id.txtMoodEmojiHappy);
        txtMoodEmojiPlayful = findViewById(R.id.txtMoodEmojiPlayful);
        txtMoodEmojiNeutral = findViewById(R.id.txtMoodEmojiNeutral);
        txtMoodEmojiTired = findViewById(R.id.txtMoodEmojiTired);
        txtMoodEmojiSad = findViewById(R.id.txtMoodEmojiSad);
        txtMoodEmojiSick = findViewById(R.id.txtMoodEmojiSick);

        moodLogList = findViewById(R.id.moodLogList);
        txtNoMoodLog = findViewById(R.id.txtNoMoodLog);

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

        setMoodButtonEmojis();

        btnMoodBack.setOnClickListener(v -> finish());

        moodHappy.setOnClickListener(v -> logMood("Happy"));
        moodPlayful.setOnClickListener(v -> logMood("Playful"));
        moodNeutral.setOnClickListener(v -> logMood("Neutral"));
        moodTired.setOnClickListener(v -> logMood("Tired"));
        moodSad.setOnClickListener(v -> logMood("Sad"));
        moodSick.setOnClickListener(v -> logMood("Sick"));

        rollThenLoad();
    }

    private void setMoodButtonEmojis() {
        txtMoodEmojiHappy.setText(getMoodIcon("Happy"));
        txtMoodEmojiPlayful.setText(getMoodIcon("Playful"));
        txtMoodEmojiNeutral.setText(getMoodIcon("Neutral"));
        txtMoodEmojiTired.setText(getMoodIcon("Tired"));
        txtMoodEmojiSad.setText(getMoodIcon("Sad"));
        txtMoodEmojiSick.setText(getMoodIcon("Sick"));
    }

    private String getMoodIcon(String mood) {
        boolean isDog = pet.getType() != null && pet.getType().equalsIgnoreCase("Dog");
        Map<String, String> iconSet = isDog ? DOG_MOODS : CAT_MOODS;
        String icon = iconSet.get(mood);
        return icon != null ? icon : "🐾";
    }

    private void rollThenLoad() {
        DailySummaryHelper.rollPreviousDays(db, pet.getId(), this::loadHistory);
    }

    private void logMood(String mood) {

        DocumentReference newRef = db.collection("pets")
                .document(pet.getId())
                .collection("wellnessEntries")
                .document();

        WellnessEntry entry = new WellnessEntry(
                newRef.getId(), pet.getId(), "mood", mood, "", System.currentTimeMillis()
        );

        newRef.set(entry)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, mood + " logged", Toast.LENGTH_SHORT).show();
                    loadHistory();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to log: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void loadHistory() {

        db.collection("pets")
                .document(pet.getId())
                .collection("wellnessEntries")
                .whereEqualTo("type", "mood")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    entries.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        WellnessEntry entry = doc.toObject(WellnessEntry.class);
                        entry.setId(doc.getId());
                        entries.add(entry);
                    }

                    Collections.sort(entries, (a, b) ->
                            Long.compare(b.getTimestampMillis(), a.getTimestampMillis()));

                    renderHistory();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void renderHistory() {

        moodLogList.removeAllViews();

        if (entries.isEmpty()) {
            txtNoMoodLog.setVisibility(View.VISIBLE);
            return;
        }

        txtNoMoodLog.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault());

        for (WellnessEntry entry : entries) {

            View row = inflater.inflate(R.layout.item_log_row, moodLogList, false);

            TextView txtLogIcon = row.findViewById(R.id.txtLogIcon);
            TextView txtLogTitle = row.findViewById(R.id.txtLogTitle);
            TextView txtLogSubtitle = row.findViewById(R.id.txtLogSubtitle);

            String mood = entry.getValue();

            txtLogIcon.setText(getMoodIcon(mood));
            txtLogTitle.setText(mood == null || mood.trim().isEmpty() ? "Mood" : mood);
            txtLogSubtitle.setText(displayFormat.format(new Date(entry.getTimestampMillis())));

            row.setOnClickListener(v -> confirmDelete(entry));

            moodLogList.addView(row);
        }
    }

    private void confirmDelete(WellnessEntry entry) {

        String mood = entry.getValue() == null ? "Mood" : entry.getValue();

        new AlertDialog.Builder(this)
                .setTitle("Delete this mood entry?")
                .setMessage(mood + " entry will be removed.")
                .setPositiveButton("Delete", (dialog, which) -> deleteMoodEntry(entry))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMoodEntry(WellnessEntry entry) {

        db.collection("pets")
                .document(pet.getId())
                .collection("wellnessEntries")
                .document(entry.getId())
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                    loadHistory();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}