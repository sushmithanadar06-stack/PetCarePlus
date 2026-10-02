package com.example.petcareplus.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.petcareplus.R;
import com.example.petcareplus.models.VetNgoLocation;
import com.example.petcareplus.utils.VetNgoDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class VetNgoLocatorActivity extends AppCompatActivity {

    private TextView btnTabVets, btnTabNgos, btnAddVetNgo, txtLocatorTitle;
    private LinearLayout locationsContainer;
    private View rootVetLocator, topBarContainer;

    private FirebaseFirestore db;
    private String currentType = "Vet";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_ngo_locator);

        db = FirebaseFirestore.getInstance();

        rootVetLocator = findViewById(R.id.rootVetLocator);
        topBarContainer = findViewById(R.id.topBarContainer);

        // keep the header below the status bar
        int baseTopPaddingPx = topBarContainer.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(rootVetLocator, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            topBarContainer.setPadding(
                    topBarContainer.getPaddingLeft(),
                    systemBars.top + baseTopPaddingPx,
                    topBarContainer.getPaddingRight(),
                    topBarContainer.getPaddingBottom());
            return insets;
        });

        TextView btnBack = findViewById(R.id.btnLocatorBack);
        txtLocatorTitle = findViewById(R.id.txtLocatorTitle);
        btnTabVets = findViewById(R.id.btnTabVets);
        btnTabNgos = findViewById(R.id.btnTabNgos);
        btnAddVetNgo = findViewById(R.id.btnAddVetNgo);
        locationsContainer = findViewById(R.id.locationsContainer);

        btnBack.setOnClickListener(v -> finish());
        btnTabVets.setOnClickListener(v -> switchTab("Vet"));
        btnTabNgos.setOnClickListener(v -> switchTab("NGO"));
        btnAddVetNgo.setOnClickListener(v ->
                startActivity(new Intent(VetNgoLocatorActivity.this, AddVetNgoActivity.class)));

        switchTab("Vet");
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderList();
    }

    private void switchTab(String type) {

        currentType = type;
        boolean isVet = type.equals("Vet");

        btnTabVets.setBackgroundResource(isVet ? R.drawable.bg_button_green_filled : R.drawable.bg_chip_purple);
        btnTabVets.setTextColor(isVet ? 0xFFFFFFFF : 0xFF2F6B52);
        btnTabNgos.setBackgroundResource(!isVet ? R.drawable.bg_button_green_filled : R.drawable.bg_chip_purple);
        btnTabNgos.setTextColor(!isVet ? 0xFFFFFFFF : 0xFF2F6B52);

        txtLocatorTitle.setText(isVet ? "Vet & NGO Locator" : "NGO Directory");

        renderList();
    }

    private void renderList() {

        final String typeAtRequest = currentType;
        List<VetNgoLocation> combined = new ArrayList<>(VetNgoDatabase.getSeedByType(typeAtRequest));

        db.collection("vetNgoLocations")
                .whereEqualTo("type", typeAtRequest)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        VetNgoLocation location = doc.toObject(VetNgoLocation.class);
                        location.id = doc.getId();
                        combined.add(location);
                    }
                    if (typeAtRequest.equals(currentType)) drawList(combined);
                })
                .addOnFailureListener(e -> {
                    if (typeAtRequest.equals(currentType)) drawList(combined);
                    Toast.makeText(VetNgoLocatorActivity.this,
                            "Showing offline directory only", Toast.LENGTH_SHORT).show();
                });
    }

    private void drawList(List<VetNgoLocation> locations) {

        locationsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (VetNgoLocation location : locations) {

            View row = inflater.inflate(R.layout.item_vet_ngo_location, locationsContainer, false);

            TextView txtIcon = row.findViewById(R.id.txtLocIcon);
            TextView txtName = row.findViewById(R.id.txtLocName);
            TextView txtSubtitle = row.findViewById(R.id.txtLocSubtitle);
            TextView txtVerified = row.findViewById(R.id.txtLocVerified);

            boolean isVet = "Vet".equalsIgnoreCase(location.type);

            txtIcon.setText(isVet ? "🏥" : "🏢");
            txtName.setText(location.name != null ? location.name : "Unknown location");

            String subtitle = location.address != null ? location.address : "Mumbai";

            if (!isVet && location.verified) {
                subtitle = subtitle + " · Verified rescue NGO";
                txtVerified.setVisibility(View.VISIBLE);
            } else {
                txtVerified.setVisibility(View.GONE);
            }

            txtSubtitle.setText(subtitle);

            row.setOnClickListener(v -> {
                Intent intent = new Intent(VetNgoLocatorActivity.this, LocationDetailActivity.class);
                intent.putExtra(LocationDetailActivity.EXTRA_TITLE, location.name);
                intent.putExtra(LocationDetailActivity.EXTRA_ADDRESS, location.address);
                intent.putExtra(LocationDetailActivity.EXTRA_HOURS, location.hours);
                intent.putExtra(LocationDetailActivity.EXTRA_PHONE, location.phone);
                intent.putExtra("latitude", location.latitude);
                intent.putExtra("longitude", location.longitude);
                startActivity(intent);
            });

            locationsContainer.addView(row);
        }
    }
}