package com.example.petcareplus.activities;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.petcareplus.R;
import com.example.petcareplus.models.Pet;
import com.example.petcareplus.models.Reminder;
import com.example.petcareplus.utils.ReminderScheduler;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class RemindersActivity extends AppCompatActivity {

    private static final int NOTIF_PERMISSION_CODE = 501;
    private static final String[] TYPES = {"Feeding", "Medication", "Vet Visit", "Grooming", "Other"};
    private static final String[] REPEATS = {"None", "Daily", "Weekly", "Monthly"};

    private TextView btnRemindersBack, txtRemindersTitle, btnAddReminder, txtNoReminders;
    private LinearLayout remindersListContainer;

    private Pet pet;
    private FirebaseFirestore db;
    private final List<Reminder> reminders = new ArrayList<>();

    private long selectedDateTimeMillis = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        db = FirebaseFirestore.getInstance();

        btnRemindersBack = findViewById(R.id.btnRemindersBack);
        txtRemindersTitle = findViewById(R.id.txtRemindersTitle);
        btnAddReminder = findViewById(R.id.btnAddReminder);
        txtNoReminders = findViewById(R.id.txtNoReminders);
        remindersListContainer = findViewById(R.id.remindersListContainer);

        if (Build.VERSION.SDK_INT >= 33) {
            pet = getIntent().getSerializableExtra("pet", Pet.class);
        } else {
            pet = (Pet) getIntent().getSerializableExtra("pet");
        }

        if (pet == null) {
            Toast.makeText(this, "Unable to load pet", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        txtRemindersTitle.setText(pet.getName() + "'s Reminders");

        btnRemindersBack.setOnClickListener(v -> finish());
        btnAddReminder.setOnClickListener(v -> showAddDialog(null));

        requestNotificationPermissionIfNeeded();
        checkExactAlarmPermission();
        loadReminders();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIF_PERMISSION_CODE);
            }
        }
    }

    /**
     * THIS IS LIKELY WHY REMINDERS WEREN'T FIRING.
     *
     * On Android 12+ (API 31+), even though the manifest declares SCHEDULE_EXACT_ALARM,
     * the person still has to turn on "Alarms & reminders" for the app in system Settings.
     * Without it, AlarmManager.canScheduleExactAlarms() is false, ReminderScheduler quietly
     * falls back to an INEXACT alarm, and phones with aggressive battery management (Vivo,
     * Xiaomi, Oppo, Samsung...) can delay or drop inexact alarms entirely - so the reminder
     * never shows, with no error anywhere.
     *
     * This checks once per screen open and, if it's off, explains it and sends the person
     * straight to the exact screen to turn it on.
     */
    private void checkExactAlarmPermission() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return; // not needed before Android 12

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null || alarmManager.canScheduleExactAlarms()) return;

        new AlertDialog.Builder(this)
                .setTitle("Allow exact reminders")
                .setMessage("To make sure feeding, medication and vet reminders arrive on time, "
                        + "please allow PetCare+ to schedule exact alarms.")
                .setPositiveButton("Allow", (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, "Couldn't open settings: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Not now", null)
                .show();
    }

    private void loadReminders() {

        db.collection("pets").document(pet.getId())
                .collection("reminders")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    reminders.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Reminder r = doc.toObject(Reminder.class);
                        r.setId(doc.getId());
                        reminders.add(r);
                    }

                    Collections.sort(reminders, (a, b) ->
                            Long.compare(a.getDateTimeMillis(), b.getDateTimeMillis()));

                    renderList();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void renderList() {

        remindersListContainer.removeAllViews();

        if (reminders.isEmpty()) {
            txtNoReminders.setVisibility(View.VISIBLE);
            return;
        }

        txtNoReminders.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd · h:mm a", Locale.getDefault());

        for (Reminder reminder : reminders) {

            View row = inflater.inflate(R.layout.item_reminder, remindersListContainer, false);

            TextView txtReminderIcon = row.findViewById(R.id.txtReminderIcon);
            TextView txtReminderTitle = row.findViewById(R.id.txtReminderTitle);
            TextView txtReminderSubtitle = row.findViewById(R.id.txtReminderSubtitle);
            TextView txtReminderRepeat = row.findViewById(R.id.txtReminderRepeat);

            txtReminderIcon.setText(getTypeIcon(reminder.getType()));
            txtReminderTitle.setText(reminder.getTitle());
            txtReminderSubtitle.setText(dateFormat.format(new java.util.Date(reminder.getDateTimeMillis())));

            if (reminder.getRepeatType() != null && !reminder.getRepeatType().equals("None")) {
                txtReminderRepeat.setVisibility(View.VISIBLE);
                txtReminderRepeat.setText(reminder.getRepeatType());
            } else {
                txtReminderRepeat.setVisibility(View.GONE);
            }

            row.setOnClickListener(v -> showAddDialog(reminder));

            row.setOnLongClickListener(v -> {
                confirmDelete(reminder);
                return true;
            });

            remindersListContainer.addView(row);
        }
    }

    private String getTypeIcon(String type) {
        if (type == null) return "🔔";
        switch (type) {
            case "Feeding": return "🍽️";
            case "Medication": return "💊";
            case "Vet Visit": return "🩺";
            case "Grooming": return "✂️";
            default: return "🔔";
        }
    }

    private void confirmDelete(Reminder reminder) {
        new AlertDialog.Builder(this)
                .setTitle("Delete this reminder?")
                .setMessage(reminder.getTitle() + " will be removed and its notification cancelled.")
                .setPositiveButton("Delete", (dialog, which) -> deleteReminder(reminder))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteReminder(Reminder reminder) {

        ReminderScheduler.cancel(this, reminder.getId());

        db.collection("pets").document(pet.getId())
                .collection("reminders").document(reminder.getId())
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Reminder deleted", Toast.LENGTH_SHORT).show();
                    loadReminders();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void showAddDialog(Reminder existing) {

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_reminder, null);

        EditText etTitle = dialogView.findViewById(R.id.etReminderTitle);
        Spinner spType = dialogView.findViewById(R.id.spReminderType);
        Button btnPickDateTime = dialogView.findViewById(R.id.btnPickDateTime);
        Spinner spRepeat = dialogView.findViewById(R.id.spReminderRepeat);
        EditText etNotes = dialogView.findViewById(R.id.etReminderNotes);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, TYPES);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(typeAdapter);

        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, REPEATS);
        repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRepeat.setAdapter(repeatAdapter);

        selectedDateTimeMillis = existing != null ? existing.getDateTimeMillis() : 0;

        if (existing != null) {
            etTitle.setText(existing.getTitle());
            int typePos = typeAdapter.getPosition(existing.getType());
            if (typePos >= 0) spType.setSelection(typePos);
            int repeatPos = repeatAdapter.getPosition(existing.getRepeatType());
            if (repeatPos >= 0) spRepeat.setSelection(repeatPos);
            etNotes.setText(existing.getNotes());
            btnPickDateTime.setText(new SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())
                    .format(new java.util.Date(existing.getDateTimeMillis())));
        }

        btnPickDateTime.setOnClickListener(v -> pickDateThenTime(btnPickDateTime));

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Add Reminder" : "Edit Reminder")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

            String title = etTitle.getText().toString().trim();

            if (title.isEmpty()) {
                etTitle.setError("Please enter a title");
                etTitle.requestFocus();
                return;
            }
            if (selectedDateTimeMillis == 0) {
                Toast.makeText(this, "Please pick a date and time", Toast.LENGTH_SHORT).show();
                return;
            }

            String type = spType.getSelectedItem().toString();
            String repeat = spRepeat.getSelectedItem().toString();
            String notes = etNotes.getText().toString().trim();

            saveReminder(existing, title, type, selectedDateTimeMillis, repeat, notes);
            dialog.dismiss();
        });
    }

    private void pickDateThenTime(Button btnPickDateTime) {

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .build();

        datePicker.addOnPositiveButtonClickListener(dateSelection -> {

            Calendar dateCal = Calendar.getInstance();
            dateCal.setTimeInMillis(dateSelection);

            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .setTitleText("Select Time")
                    .build();

            timePicker.addOnPositiveButtonClickListener(v -> {

                Calendar finalCal = Calendar.getInstance();
                finalCal.set(
                        dateCal.get(Calendar.YEAR),
                        dateCal.get(Calendar.MONTH),
                        dateCal.get(Calendar.DAY_OF_MONTH),
                        timePicker.getHour(),
                        timePicker.getMinute(),
                        0
                );

                selectedDateTimeMillis = finalCal.getTimeInMillis();
                btnPickDateTime.setText(new SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())
                        .format(finalCal.getTime()));
            });

            timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void saveReminder(Reminder existing, String title, String type,
                              long dateTimeMillis, String repeat, String notes) {

        String ownerId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        if (existing != null) {

            ReminderScheduler.cancel(this, existing.getId());

            Reminder updated = new Reminder(existing.getId(), pet.getId(), ownerId, title, type, dateTimeMillis, repeat, notes);

            db.collection("pets").document(pet.getId())
                    .collection("reminders").document(existing.getId())
                    .set(updated)
                    .addOnSuccessListener(unused -> {
                        ReminderScheduler.schedule(this, updated, pet.getName());
                        Toast.makeText(this, "Reminder updated", Toast.LENGTH_SHORT).show();
                        loadReminders();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );

        } else {

            DocumentReference newRef = db.collection("pets").document(pet.getId())
                    .collection("reminders").document();

            Reminder reminder = new Reminder(newRef.getId(), pet.getId(), ownerId, title, type, dateTimeMillis, repeat, notes);

            newRef.set(reminder)
                    .addOnSuccessListener(unused -> {
                        ReminderScheduler.schedule(this, reminder, pet.getName());
                        Toast.makeText(this, "Reminder set", Toast.LENGTH_SHORT).show();
                        loadReminders();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        }
    }
}