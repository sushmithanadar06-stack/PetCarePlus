package com.example.petcareplus.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.petcareplus.R;
import com.example.petcareplus.adapters.HealthScorePagerAdapter;
import com.example.petcareplus.models.Pet;
import com.example.petcareplus.models.WeatherInfo;
import com.example.petcareplus.utils.NotificationsHelper;
import com.example.petcareplus.utils.WeatherFetcher;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;

public class PetListActivity extends AppCompatActivity {

    private TextView txtGreeting, txtPetCount;
    private View btnSettings;
    private View viewNotifBadge;
    private LinearLayout petAvatarRow, emptyState;

    private ViewPager2 pagerHealthScore;
    private LinearLayout healthScoreDots;
    private HealthScorePagerAdapter healthScorePagerAdapter;

    private LinearLayout actionAddPet, actionReportRescue, actionFoodCheck;
    private LinearLayout actionVetLocator, actionReminders, actionHealthReport;
    private LinearLayout cardRescueFeed, cardAdoption;

    private LinearLayout navRecords, navRescue, navCommunity, navProfile;
    private LinearLayout weatherBanner;
    private TextView txtWeatherBanner;

    private View fabCommunityChat;

    // Gesture-nav fix: lets the bottom nav row sit above the system gesture bar.
    private View rootPetList, bottomNavContainer;

    private ArrayList<Pet> petList;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Module 13: live "unread notifications" badge on the bell icon
    private ListenerRegistration notifBadgeListener;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                // Nothing to do either way - notifications simply won't show as a
                // phone alert if this is denied; the in-app bell badge still works.
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_list);

        rootPetList = findViewById(R.id.rootPetList);
        bottomNavContainer = findViewById(R.id.bottomNavContainer);

        ViewCompat.setOnApplyWindowInsetsListener(rootPetList, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            bottomNavContainer.setPadding(
                    bottomNavContainer.getPaddingLeft(),
                    bottomNavContainer.getPaddingTop(),
                    bottomNavContainer.getPaddingRight(),
                    12 + systemBars.bottom
            );
            return insets;
        });

        String toastMessage = getIntent().getStringExtra("toastMessage");
        if (toastMessage != null) {
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Module 13: ask for the Android 13+ notification permission once, with a
        // friendly explanation first rather than the bare system dialog.
        // (NotificationWatcher itself is started centrally from PetCareApp - not here.)
        maybeAskForNotificationPermission();

        txtGreeting = findViewById(R.id.txtGreeting);
        txtPetCount = findViewById(R.id.txtPetCount);
        pagerHealthScore = findViewById(R.id.pagerHealthScore);
        healthScoreDots = findViewById(R.id.healthScoreDots);
        btnSettings = findViewById(R.id.btnSettings);
        viewNotifBadge = findViewById(R.id.viewNotifBadge);
        petAvatarRow = findViewById(R.id.petAvatarRow);
        emptyState = findViewById(R.id.emptyState);

        actionAddPet = findViewById(R.id.actionAddPet);
        actionReportRescue = findViewById(R.id.actionReportRescue);
        actionFoodCheck = findViewById(R.id.actionFoodCheck);
        actionVetLocator = findViewById(R.id.actionVetLocator);
        actionReminders = findViewById(R.id.actionReminders);
        actionHealthReport = findViewById(R.id.actionHealthReport);
        cardRescueFeed = findViewById(R.id.cardRescueFeed);
        cardAdoption = findViewById(R.id.cardAdoption);

        navRecords = findViewById(R.id.navRecords);
        navRescue = findViewById(R.id.navRescue);
        navCommunity = findViewById(R.id.navCommunity);
        navProfile = findViewById(R.id.navProfile);
        weatherBanner = findViewById(R.id.weatherBanner);
        txtWeatherBanner = findViewById(R.id.txtWeatherBanner);

        fabCommunityChat = findViewById(R.id.fabCommunityChat);

        petList = new ArrayList<>();

        setGreeting();
        loadRealWeather();

        actionAddPet.setOnClickListener(v -> {
            Intent intent = new Intent(PetListActivity.this, AddPetActivity.class);
            startActivity(intent);
        });

        // Rescue Connect: "Report Rescue" quick action opens the report form directly
        actionReportRescue.setOnClickListener(v ->
                startActivity(new Intent(PetListActivity.this, ReportRescueActivity.class))
        );

        actionFoodCheck.setOnClickListener(v ->
                startActivity(new Intent(PetListActivity.this, FoodSafetyActivity.class))
        );

        actionVetLocator.setOnClickListener(v ->
                startActivity(new Intent(PetListActivity.this, VetNgoLocatorActivity.class))
        );

        actionReminders.setOnClickListener(v -> {
            if (petList.isEmpty()) {
                Toast.makeText(this, "Add a pet first", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(PetListActivity.this, RemindersActivity.class);
            intent.putExtra("pet", petList.get(0));
            startActivity(intent);
        });

        actionHealthReport.setOnClickListener(v ->
                startActivity(new Intent(PetListActivity.this, HealthReportActivity.class))
        );

        // Rescue Connect: rescue feed card opens the rescue reports list
        cardRescueFeed.setOnClickListener(v ->
                startActivity(new Intent(PetListActivity.this, RescueConnectActivity.class))
        );

        cardAdoption.setOnClickListener(v -> {
            if (blockGuestFromCommunity()) return;
            startActivity(new Intent(PetListActivity.this, AdoptionListingsActivity.class));
        });

        // Module 13: the bell now opens the notifications inbox
        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(PetListActivity.this, NotificationsActivity.class))
        );

        navRecords.setOnClickListener(v -> {
            if (petList.isEmpty()) {
                Toast.makeText(this, "Add a pet first", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(PetListActivity.this, MedicalRecordsHubActivity.class);
            intent.putExtra("pet", petList.get(0));
            startActivity(intent);
        });

        // Rescue Connect: bottom-nav "Rescue" tab opens the rescue reports list
        navRescue.setOnClickListener(v ->
                startActivity(new Intent(PetListActivity.this, RescueConnectActivity.class))
        );

        navCommunity.setOnClickListener(v -> {
            if (blockGuestFromCommunity()) return;
            startActivity(new Intent(PetListActivity.this, CommunityRoomsActivity.class));
        });

        fabCommunityChat.setOnClickListener(v -> {
            if (blockGuestFromCommunity()) return;
            startActivity(new Intent(PetListActivity.this, CommunityRoomsActivity.class));
        });

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(PetListActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        weatherBanner.setOnClickListener(v -> {
            Intent intent = new Intent(PetListActivity.this, WeatherCareActivity.class);

            if (!petList.isEmpty()) {
                intent.putExtra("petType", petList.get(0).getType());
            }

            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setGreeting();
        loadPetsFromFirestore();
        startNotifBadge();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopNotifBadge();
    }

    // =============================================================
    // Module 13: unread notifications badge on the bell icon
    // =============================================================

    private void maybeAskForNotificationPermission() {

        if (Build.VERSION.SDK_INT < 33) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) return;

        SharedPreferences prefs = getSharedPreferences("petcare", MODE_PRIVATE);
        if (prefs.getBoolean("asked_notif_permission", false)) return;
        prefs.edit().putBoolean("asked_notif_permission", true).apply();

        new AlertDialog.Builder(this)
                .setTitle("Want to be notified?")
                .setMessage("Get notified when a volunteer helps with a rescue, someone's "
                        + "interested in adopting your pet, or you get a reply in Community. "
                        + "You can turn this off anytime in Notification Preferences.")
                .setPositiveButton("Turn on notifications", (d, w) ->
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS))
                .setNegativeButton("Not now", null)
                .setCancelable(false)
                .show();
    }

    private void startNotifBadge() {

        stopNotifBadge();

        FirebaseUser user = auth.getCurrentUser();
        if (user == null || viewNotifBadge == null) return;

        notifBadgeListener = NotificationsHelper.listenUnreadCount(db, user.getUid(),
                count -> viewNotifBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE));
    }

    private void stopNotifBadge() {
        if (notifBadgeListener != null) {
            notifBadgeListener.remove();
            notifBadgeListener = null;
        }
    }

    private void setGreeting() {

        FirebaseUser user = auth.getCurrentUser();
        String name;

        if (user != null && user.isAnonymous()) {
            name = "Guest";
        } else if (user != null
                && user.getDisplayName() != null
                && !user.getDisplayName().trim().isEmpty()) {
            name = user.getDisplayName().split(" ")[0];
        } else {
            name = "there";
        }

        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        String greetingWord;

        if (hour < 12) {
            greetingWord = "Good Morning";
        } else if (hour < 17) {
            greetingWord = "Good Afternoon";
        } else if (hour < 21) {
            greetingWord = "Good Evening";
        } else {
            greetingWord = "Good Night";
        }

        txtGreeting.setText(greetingWord + ", " + name);
    }

    private void loadRealWeather() {

        WeatherFetcher.fetchMumbaiWeather(new WeatherFetcher.WeatherCallback() {

            @Override
            public void onSuccess(WeatherInfo weather) {

                String message;
                int temp = (int) Math.round(weather.temperature);

                if (weather.isRaining) {
                    message = "Rainy, " + temp + "°C — dry paws after walks";
                } else if (weather.temperature >= 33) {
                    message = temp + "°C — keep pets hydrated today";
                } else if (weather.temperature <= 20) {
                    message = temp + "°C — keep pets warm today";
                } else {
                    message = temp + "°C — pleasant conditions today";
                }

                txtWeatherBanner.setText(message);
            }

            @Override
            public void onFailure(String errorMessage) {
                txtWeatherBanner.setText("Tap for weather care tips");
            }
        });
    }

    private void showComingSoon() {
        Toast.makeText(this, "Coming in a future module", Toast.LENGTH_SHORT).show();
    }

    private boolean blockGuestFromCommunity() {

        FirebaseUser user = auth.getCurrentUser();

        if (user != null && user.isAnonymous()) {
            Toast.makeText(this, "Please create an account to access Community",
                    Toast.LENGTH_LONG).show();
            return true;
        }

        return false;
    }

    private void loadPetsFromFirestore() {

        if (auth.getCurrentUser() == null) {
            petList.clear();
            renderAvatarRow();
            updateHealthScorePager();
            updateEmptyState();
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

                    renderAvatarRow();
                    updateHealthScorePager();
                    updateEmptyState();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(PetListActivity.this,
                                "Failed to load pets: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void renderAvatarRow() {

        petAvatarRow.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);

        for (Pet pet : petList) {

            View circleView = inflater.inflate(R.layout.item_pet_circle, petAvatarRow, false);

            ImageView imgCirclePet = circleView.findViewById(R.id.imgCirclePet);
            TextView txtCirclePetLetter = circleView.findViewById(R.id.txtCirclePetLetter);
            TextView txtCirclePetName = circleView.findViewById(R.id.txtCirclePetName);

            txtCirclePetName.setText(pet.getName());

            String photoUri = pet.getPhotoUri();

            if (photoUri != null && !photoUri.trim().isEmpty()) {

                try {

                    if (com.example.petcareplus.utils.ImageUtils.isBase64Image(photoUri)) {

                        android.graphics.Bitmap bitmap =
                                com.example.petcareplus.utils.ImageUtils.base64ToBitmap(photoUri);

                        if (bitmap != null) {
                            imgCirclePet.setVisibility(View.VISIBLE);
                            if (txtCirclePetLetter != null) txtCirclePetLetter.setVisibility(View.GONE);
                            imgCirclePet.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            imgCirclePet.setImageBitmap(bitmap);
                        } else {
                            setCirclePlaceholder(imgCirclePet, txtCirclePetLetter, pet);
                        }

                    } else {

                        imgCirclePet.setVisibility(View.VISIBLE);
                        if (txtCirclePetLetter != null) txtCirclePetLetter.setVisibility(View.GONE);
                        imgCirclePet.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        imgCirclePet.setImageURI(Uri.parse(photoUri));
                    }

                } catch (Exception e) {
                    setCirclePlaceholder(imgCirclePet, txtCirclePetLetter, pet);
                }

            } else {
                setCirclePlaceholder(imgCirclePet, txtCirclePetLetter, pet);
            }

            circleView.setOnClickListener(v -> {
                Intent intent = new Intent(PetListActivity.this, PetMenuActivity.class);
                intent.putExtra("pet", pet);
                startActivity(intent);
            });

            petAvatarRow.addView(circleView);
        }

        View addCircle = inflater.inflate(R.layout.item_pet_circle, petAvatarRow, false);

        ImageView imgCirclePet = addCircle.findViewById(R.id.imgCirclePet);
        TextView txtCirclePetLetter = addCircle.findViewById(R.id.txtCirclePetLetter);
        TextView txtCirclePetName = addCircle.findViewById(R.id.txtCirclePetName);

        if (txtCirclePetLetter != null) txtCirclePetLetter.setVisibility(View.GONE);

        imgCirclePet.setVisibility(View.VISIBLE);
        imgCirclePet.setImageResource(android.R.drawable.ic_input_add);
        imgCirclePet.setImageTintList(android.content.res.ColorStateList.valueOf(0xFF2F6B52));
        imgCirclePet.setBackgroundResource(R.drawable.bg_circle_dashed);
        imgCirclePet.setPadding(18, 18, 18, 18);
        imgCirclePet.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        txtCirclePetName.setText("Add");

        addCircle.setOnClickListener(v -> {
            Intent intent = new Intent(PetListActivity.this, AddPetActivity.class);
            startActivity(intent);
        });

        petAvatarRow.addView(addCircle);

        txtPetCount.setText(petList.size() + (petList.size() == 1 ? " pet in your care" : " pets in your care"));
    }

    private void setCirclePlaceholder(ImageView img, TextView letter, Pet pet) {

        img.setVisibility(View.GONE);

        if (letter != null) {
            letter.setVisibility(View.VISIBLE);
            String type = pet.getType();
            letter.setText((type != null && type.equalsIgnoreCase("Dog")) ? "D" : "C");
        }
    }

    private void updateHealthScorePager() {

        if (healthScorePagerAdapter == null) {

            healthScorePagerAdapter = new HealthScorePagerAdapter(petList, db, pet -> {
                Intent intent = new Intent(PetListActivity.this, WellnessActivity.class);
                intent.putExtra("pet", pet);
                startActivity(intent);
            });

            pagerHealthScore.setAdapter(healthScorePagerAdapter);

            pagerHealthScore.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    updateDotsActive(position);
                }
            });

        } else {
            healthScorePagerAdapter.notifyDataSetChanged();
        }

        if (petList.isEmpty()) {
            pagerHealthScore.setVisibility(View.GONE);
            healthScoreDots.setVisibility(View.GONE);
            return;
        }

        pagerHealthScore.setVisibility(View.VISIBLE);
        buildDots();
    }

    private void buildDots() {

        healthScoreDots.removeAllViews();

        if (petList.size() <= 1) {
            healthScoreDots.setVisibility(View.GONE);
            return;
        }

        healthScoreDots.setVisibility(View.VISIBLE);

        int size = (int) (8 * getResources().getDisplayMetrics().density);
        int margin = (int) (4 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < petList.size(); i++) {

            View dot = new View(this);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);

            dot.setLayoutParams(params);
            dot.setBackground(makeDotDrawable(i == pagerHealthScore.getCurrentItem()));

            healthScoreDots.addView(dot);
        }
    }

    private void updateDotsActive(int activePosition) {

        for (int i = 0; i < healthScoreDots.getChildCount(); i++) {
            healthScoreDots.getChildAt(i).setBackground(makeDotDrawable(i == activePosition));
        }
    }

    private GradientDrawable makeDotDrawable(boolean active) {

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(active ? 0xFF1E5C45 : 0xFFD9D9D9);

        return shape;
    }

    private void updateEmptyState() {
        emptyState.setVisibility(petList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}