package com.example.petcareplus.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.petcareplus.models.Pet;
import com.example.petcareplus.models.Reminder;
import com.example.petcareplus.utils.ReminderScheduler;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collectionGroup("reminders")
                .whereEqualTo("ownerId", uid)
                .whereGreaterThan("dateTimeMillis", System.currentTimeMillis())
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {

                        Reminder r = doc.toObject(Reminder.class);
                        r.setId(doc.getId());

                        String petId = r.getPetId();
                        if (petId == null || petId.isEmpty()) {
                            ReminderScheduler.schedule(context, r, "Your pet");
                            continue;
                        }

                        db.collection("pets").document(petId)
                                .get()
                                .addOnSuccessListener(petSnapshot -> {
                                    Pet pet = petSnapshot.toObject(Pet.class);
                                    String petName = (pet != null && pet.getName() != null)
                                            ? pet.getName() : "Your pet";
                                    ReminderScheduler.schedule(context, r, petName);
                                })
                                .addOnFailureListener(e ->
                                        ReminderScheduler.schedule(context, r, "Your pet")
                                );
                    }
                });
    }
}