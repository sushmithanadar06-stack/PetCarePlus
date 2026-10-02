package com.example.petcareplus.activities;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.models.VetNgoLocation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class AddVetNgoActivity extends AppCompatActivity {

    private Spinner spAddType;
    private EditText etAddName, etAddAddress, etAddHours, etAddPhone;
    private Button btnSubmitVetNgo;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Mumbai fallback — used only if the address can't be resolved at all
    private static final double MUMBAI_LAT = 19.0760;
    private static final double MUMBAI_LNG = 72.8777;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_vet_ngo);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        TextView btnBack = findViewById(R.id.btnAddVetBack);
        spAddType = findViewById(R.id.spAddType);
        etAddName = findViewById(R.id.etAddName);
        etAddAddress = findViewById(R.id.etAddAddress);
        etAddHours = findViewById(R.id.etAddHours);
        etAddPhone = findViewById(R.id.etAddPhone);
        btnSubmitVetNgo = findViewById(R.id.btnSubmitVetNgo);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"Vet", "NGO"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAddType.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnSubmitVetNgo.setOnClickListener(v -> submit());
    }

    private void submit() {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = spAddType.getSelectedItem().toString();
        String name = etAddName.getText().toString().trim();
        String address = etAddAddress.getText().toString().trim();
        String hours = etAddHours.getText().toString().trim();
        String phone = etAddPhone.getText().toString().trim();

        if (name.isEmpty()) { etAddName.setError("Enter a name"); return; }
        if (address.isEmpty()) { etAddAddress.setError("Enter an address"); return; }

        btnSubmitVetNgo.setEnabled(false);
        btnSubmitVetNgo.setText("Finding location...");

        resolveAddressToCoordinates(address, (lat, lng, resolved) -> {

            if (!resolved) {
                Toast.makeText(this,
                        "Couldn't pin-point that address exactly — saved near central Mumbai instead. You can edit it later.",
                        Toast.LENGTH_LONG).show();
            }

            saveToFirestore(user, type, name, address, hours, phone, lat, lng);
        });
    }

    private interface GeocodeCallback {
        void onResult(double lat, double lng, boolean resolved);
    }

    /**
     * Uses Android's built-in Geocoder — free, no API key, no extra network
     * dependency. Runs on a background thread since Geocoder does a lookup
     * that can block.
     */
    private void resolveAddressToCoordinates(String address, GeocodeCallback callback) {

        new Thread(() -> {

            double lat = MUMBAI_LAT;
            double lng = MUMBAI_LNG;
            boolean resolved = false;

            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                // Append "Mumbai" so short area names (e.g. "Andheri West")
                // resolve correctly instead of matching some other city.
                String query = address.toLowerCase(Locale.ROOT).contains("mumbai")
                        ? address : address + ", Mumbai, India";

                List<Address> results = geocoder.getFromLocationName(query, 1);

                if (results != null && !results.isEmpty()) {
                    lat = results.get(0).getLatitude();
                    lng = results.get(0).getLongitude();
                    resolved = true;
                }

            } catch (IOException | IllegalArgumentException e) {
                // Geocoder service unavailable or address unparseable —
                // fall back to Mumbai center, handled below.
            }

            final double finalLat = lat;
            final double finalLng = lng;
            final boolean finalResolved = resolved;

            runOnUiThread(() -> callback.onResult(finalLat, finalLng, finalResolved));

        }).start();
    }

    private void saveToFirestore(FirebaseUser user, String type, String name, String address,
                                 String hours, String phone, double lat, double lng) {

        DocumentReference newRef = db.collection("vetNgoLocations").document();

        VetNgoLocation location = new VetNgoLocation(
                newRef.getId(), name, type, address,
                hours.isEmpty() ? "Not specified" : hours,
                phone.isEmpty() ? "Not specified" : phone,
                lat, lng, false, user.getUid(), System.currentTimeMillis()
        );

        newRef.set(location)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Added to directory", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSubmitVetNgo.setEnabled(true);
                    btnSubmitVetNgo.setText("Add to Directory");
                });
    }
}