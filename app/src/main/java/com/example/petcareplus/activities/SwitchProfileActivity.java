package com.example.petcareplus.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petcareplus.R;
import com.example.petcareplus.adapters.SwitchProfileAdapter;
import com.example.petcareplus.models.Pet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class SwitchProfileActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET = "target";
    public static final String TARGET_PET_MENU = "petmenu";
    public static final String TARGET_WELLNESS = "wellness";

    private TextView btnSwitchProfileBack;
    private RecyclerView recyclerSwitchProfile;
    private Button btnAddNewPetFromSwitch;
    private ArrayList<Pet> petList;
    private SwitchProfileAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String target = TARGET_PET_MENU;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_switch_profile);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (getIntent().hasExtra(EXTRA_TARGET)) {
            target = getIntent().getStringExtra(EXTRA_TARGET);
        }

        btnSwitchProfileBack = findViewById(R.id.btnSwitchProfileBack);
        recyclerSwitchProfile = findViewById(R.id.recyclerSwitchProfile);
        btnAddNewPetFromSwitch = findViewById(R.id.btnAddNewPetFromSwitch);

        petList = new ArrayList<>();
        recyclerSwitchProfile.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SwitchProfileAdapter(petList, selectedPet -> {

            Intent intent;

            if (TARGET_WELLNESS.equals(target)) {
                intent = new Intent(SwitchProfileActivity.this, WellnessActivity.class);
            } else {
                intent = new Intent(SwitchProfileActivity.this, PetMenuActivity.class);
            }

            intent.putExtra("pet", selectedPet);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        recyclerSwitchProfile.setAdapter(adapter);

        btnSwitchProfileBack.setOnClickListener(v -> finish());
        btnAddNewPetFromSwitch.setOnClickListener(v ->
                startActivity(new Intent(SwitchProfileActivity.this, AddPetActivity.class))
        );

        loadPets();
    }

    private void loadPets() {

        if (auth.getCurrentUser() == null) {
            petList.clear();
            adapter.notifyDataSetChanged();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("pets")
                .whereEqualTo("ownerId", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    petList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Pet pet = doc.toObject(Pet.class);
                        pet.setId(doc.getId());
                        petList.add(pet);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(SwitchProfileActivity.this,
                                "Failed to load pets: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }
}