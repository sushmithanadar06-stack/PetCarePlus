package com.example.petcareplus.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.petcareplus.R;
import com.example.petcareplus.adapters.AdoptionPhotoPagerAdapter;
import com.example.petcareplus.models.AdoptionListing;
import com.example.petcareplus.models.AdoptionRequest;
import com.example.petcareplus.utils.ImageUtils;
import com.example.petcareplus.utils.NotificationsHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdoptionListingsActivity extends AppCompatActivity {

    private TextView btnAdoptionBack;
    private TextView btnPostAdoption;
    private TextView txtNoListings;

    private TextView btnTabAdoption;
    private TextView btnTabChatRooms;

    private TextView btnGoToMessages;
    private TextView btnGoToAllRequests;

    // NEW: Available / Adopted switch + "N pets adopted" summary
    private TextView btnFilterAvailable;
    private TextView btnFilterAdopted;
    private TextView txtAdoptedSummary;

    private LinearLayout listingsContainer;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final List<AdoptionListing> listings = new ArrayList<>();

    /** false = show pets still looking for a home, true = show adopted pets. */
    private boolean showAdopted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adoption_listings);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "You must be signed in to use Community", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Guests (anonymous auth) can browse the app but should not have
        // access to Community — its listings and messaging.
        if (currentUser.isAnonymous()) {
            Toast.makeText(this, "Please create an account to access Community",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnAdoptionBack = findViewById(R.id.btnAdoptionBack);
        btnPostAdoption = findViewById(R.id.btnPostAdoption);
        listingsContainer = findViewById(R.id.listingsContainer);
        txtNoListings = findViewById(R.id.txtNoListings);
        btnTabAdoption = findViewById(R.id.btnTabAdoption);
        btnTabChatRooms = findViewById(R.id.btnTabChatRooms);

        btnGoToMessages = findViewById(R.id.btnGoToMessages);
        btnGoToAllRequests = findViewById(R.id.btnGoToAllRequests);

        btnFilterAvailable = findViewById(R.id.btnFilterAvailable);
        btnFilterAdopted = findViewById(R.id.btnFilterAdopted);
        txtAdoptedSummary = findViewById(R.id.txtAdoptedSummary);

        btnAdoptionBack.setOnClickListener(v -> finish());

        btnTabChatRooms.setOnClickListener(v -> {
            startActivity(new Intent(
                    AdoptionListingsActivity.this,
                    CommunityRoomsActivity.class
            ));
            finish();
        });

        btnPostAdoption.setOnClickListener(v ->
                startActivity(new Intent(
                        AdoptionListingsActivity.this,
                        PostAdoptionActivity.class
                ))
        );

        btnGoToMessages.setOnClickListener(v ->
                startActivity(new Intent(
                        AdoptionListingsActivity.this,
                        AdoptionMessagesActivity.class
                ))
        );

        btnGoToAllRequests.setOnClickListener(v -> {
            if (auth.getCurrentUser() == null) return;

            startActivity(new Intent(
                    AdoptionListingsActivity.this,
                    AdoptionAllRequestsActivity.class
            ));
        });

        btnFilterAvailable.setOnClickListener(v -> {
            showAdopted = false;
            renderList();
        });

        btnFilterAdopted.setOnClickListener(v -> {
            showAdopted = true;
            renderList();
        });

        styleFilterTab(btnFilterAvailable, true);
        styleFilterTab(btnFilterAdopted, false);

        loadListings();
        updateRequestsBadge();

        // NOTE: do NOT call markAdoptionNotificationsRead() here — that was a
        // race condition that made the Requests badge always show 0. Marking
        // as read happens only in AdoptionAllRequestsActivity.
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadListings();
        updateRequestsBadge();
    }

    /**
     * Updates the unread adoption request count shown
     * on the All Requests button.
     */
    private void updateRequestsBadge() {

        if (auth.getCurrentUser() == null) return;

        NotificationsHelper.getUnreadAdoptionCount(
                db,
                auth.getCurrentUser().getUid(),
                count -> btnGoToAllRequests.setText(
                        count > 0
                                ? "🎗️ Requests (" + count + ")"
                                : "🎗️ Requests"
                )
        );
    }

    /**
     * Loads all adoption listings from Firestore.
     */
    private void loadListings() {

        db.collection("adoptionListings")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    listings.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {

                        AdoptionListing listing = doc.toObject(AdoptionListing.class);
                        listing.setId(doc.getId());
                        listings.add(listing);
                    }

                    Collections.sort(
                            listings,
                            (a, b) -> Long.compare(b.getTimestampMillis(), a.getTimestampMillis())
                    );

                    renderList();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Failed to load: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    /**
     * Shows either the pets still available or the adopted ones,
     * and keeps the tab counts + the "N pets adopted" summary up to date.
     */
    private void renderList() {

        int availableCount = 0;
        int adoptedCount = 0;

        for (AdoptionListing l : listings) {
            if (l.isAdopted()) adoptedCount++;
            else availableCount++;
        }

        btnFilterAvailable.setText("Available (" + availableCount + ")");
        btnFilterAdopted.setText("🎉 Adopted (" + adoptedCount + ")");
        styleFilterTab(btnFilterAvailable, !showAdopted);
        styleFilterTab(btnFilterAdopted, showAdopted);

        if (showAdopted && adoptedCount > 0) {
            txtAdoptedSummary.setText("🎉 " + adoptedCount
                    + (adoptedCount == 1 ? " pet has" : " pets have")
                    + " found a loving home");
            txtAdoptedSummary.setVisibility(View.VISIBLE);
        } else {
            txtAdoptedSummary.setVisibility(View.GONE);
        }

        List<AdoptionListing> shown = new ArrayList<>();
        for (AdoptionListing l : listings) {
            if (l.isAdopted() == showAdopted) shown.add(l);
        }

        if (showAdopted) {
            Collections.sort(shown, (a, b) ->
                    Long.compare(b.getAdoptedAtMillis(), a.getAdoptedAtMillis()));
        }

        listingsContainer.removeAllViews();

        if (shown.isEmpty()) {
            txtNoListings.setText(showAdopted
                    ? "No pets have been adopted yet"
                    : "No pets listed for adoption nearby");
            txtNoListings.setVisibility(View.VISIBLE);
            return;
        }

        txtNoListings.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);

        FirebaseUser currentUser = auth.getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : "";

        for (AdoptionListing listing : shown) {
            listingsContainer.addView(
                    showAdopted
                            ? buildAdoptedCard(inflater, listing, uid)
                            : buildAvailableCard(inflater, listing, uid)
            );
        }
    }

    private void styleFilterTab(TextView tab, boolean selected) {
        tab.setBackgroundResource(selected
                ? R.drawable.bg_button_green_filled
                : R.drawable.bg_chip_purple);
        tab.setTextColor(selected ? 0xFFFFFFFF : 0xFF2F6B52);
    }

    // ------------------------------------------------------------------
    // Available pets (the original card)
    // ------------------------------------------------------------------

    private View buildAvailableCard(LayoutInflater inflater, AdoptionListing listing, String uid) {

        View card = inflater.inflate(
                R.layout.item_adoption_listing,
                listingsContainer,
                false
        );

        ViewPager2 pager = card.findViewById(R.id.pagerListingPhotos);
        LinearLayout dots = card.findViewById(R.id.dotsListingPhotos);
        TextView txtListingName = card.findViewById(R.id.txtListingName);
        TextView txtListingVerified = card.findViewById(R.id.txtListingVerified);
        TextView btnListingMenu = card.findViewById(R.id.btnListingMenu);
        TextView txtListingDetails = card.findViewById(R.id.txtListingDetails);
        TextView btnListingAction = card.findViewById(R.id.btnListingAction);

        txtListingName.setText(listing.getPetName());

        txtListingDetails.setText(
                listing.getBreed()
                        + " · "
                        + listing.getAge()
                        + " · "
                        + listing.getGender()
                        + " · "
                        + listing.getLocation()
        );

        txtListingVerified.setVisibility(
                listing.isNgoVerified() ? View.VISIBLE : View.GONE
        );

        List<String> photos = listing.getPhotoList();

        if (photos == null || photos.isEmpty()) {
            photos = new ArrayList<>();
            photos.add("");
        }

        pager.setAdapter(new AdoptionPhotoPagerAdapter(photos));

        buildDots(dots, photos.size(), 0);

        if (photos.size() > 1) {

            pager.registerOnPageChangeCallback(
                    new ViewPager2.OnPageChangeCallback() {

                        @Override
                        public void onPageSelected(int position) {

                            buildDots(
                                    dots,
                                    ((AdoptionPhotoPagerAdapter) pager.getAdapter()).getItemCount(),
                                    position
                            );
                        }
                    }
            );

        } else {
            dots.setVisibility(View.GONE);
        }

        boolean isOwner = listing.getOwnerId() != null && listing.getOwnerId().equals(uid);

        if (isOwner) {

            btnListingAction.setText("View Requests");

            btnListingAction.setOnClickListener(v -> {

                Intent intent = new Intent(
                        AdoptionListingsActivity.this,
                        AdoptionRequestsActivity.class
                );

                intent.putExtra("listing", listing);

                startActivity(intent);
            });

            btnListingMenu.setVisibility(View.VISIBLE);
            btnListingMenu.setOnClickListener(v -> showOwnerMenu(listing));

        } else {

            btnListingAction.setText("I'm Interested");
            btnListingAction.setOnClickListener(v -> expressInterest(listing));
            btnListingMenu.setVisibility(View.GONE);
        }

        return card;
    }

    // ------------------------------------------------------------------
    // Adopted pets (compact card)
    // ------------------------------------------------------------------

    private View buildAdoptedCard(LayoutInflater inflater, AdoptionListing listing, String uid) {

        View card = inflater.inflate(R.layout.item_adopted_pet, listingsContainer, false);

        ImageView img = card.findViewById(R.id.imgAdoptedPhoto);
        TextView txtName = card.findViewById(R.id.txtAdoptedName);
        TextView txtDetails = card.findViewById(R.id.txtAdoptedDetails);
        TextView txtDate = card.findViewById(R.id.txtAdoptedDate);

        txtName.setText(listing.getPetName());
        txtDetails.setText(listing.getBreed() + " · " + listing.getAge() + " · " + listing.getGender());

        if (listing.getAdoptedAtMillis() > 0) {
            String date = new SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                    .format(new Date(listing.getAdoptedAtMillis()));
            txtDate.setText("Found a loving home on " + date);
        } else {
            txtDate.setText("Found a loving home");
        }

        final String photo = listing.getFirstPhoto();

        if (photo != null && ImageUtils.isBase64Image(photo)) {
            Bitmap bitmap = ImageUtils.base64ToBitmap(photo);
            if (bitmap != null) img.setImageBitmap(bitmap);
        }

        img.setOnClickListener(v -> {
            if (photo != null && ImageUtils.isBase64Image(photo)) {
                Intent intent = new Intent(AdoptionListingsActivity.this, PhotoViewerActivity.class);
                intent.putExtra("photoBase64", photo);
                startActivity(intent);
            }
        });

        boolean isOwner = listing.getOwnerId() != null && listing.getOwnerId().equals(uid);

        if (isOwner) {
            card.setOnClickListener(v -> showAdoptedOwnerMenu(listing));
        }

        return card;
    }

    /**
     * Creates the small photo indicator dots.
     */
    private void buildDots(
            LinearLayout dotsContainer,
            int count,
            int activeIndex
    ) {

        dotsContainer.removeAllViews();

        dotsContainer.setVisibility(count > 1 ? View.VISIBLE : View.GONE);

        int size = (int) (7 * getResources().getDisplayMetrics().density);
        int margin = (int) (4 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < count; i++) {

            View dot = new View(this);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);

            dot.setLayoutParams(params);

            android.graphics.drawable.GradientDrawable shape =
                    new android.graphics.drawable.GradientDrawable();

            shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            shape.setColor(i == activeIndex ? 0xFF1E5C45 : 0xFFD9D9D9);

            dot.setBackground(shape);

            dotsContainer.addView(dot);
        }
    }

    // ------------------------------------------------------------------
    // Owner actions
    // ------------------------------------------------------------------

    /**
     * Edit / Mark as adopted / Delete menu for the listing owner.
     */
    private void showOwnerMenu(AdoptionListing listing) {

        String[] options = {
                "Edit",
                "Mark as adopted 🎉",
                "Delete"
        };

        new AlertDialog.Builder(this)
                .setTitle(listing.getPetName())
                .setItems(options, (dialog, which) -> {

                    if (which == 0) {

                        Intent intent = new Intent(
                                AdoptionListingsActivity.this,
                                PostAdoptionActivity.class
                        );

                        intent.putExtra("editListing", listing);
                        startActivity(intent);

                    } else if (which == 1) {

                        startMarkAdopted(listing);

                    } else {

                        confirmDelete(listing);
                    }
                })
                .show();
    }

    /** Menu shown when the owner taps one of their adopted pets. */
    private void showAdoptedOwnerMenu(AdoptionListing listing) {

        String[] options = {
                "Mark as available again",
                "Delete"
        };

        new AlertDialog.Builder(this)
                .setTitle(listing.getPetName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) markAvailableAgain(listing);
                    else confirmDelete(listing);
                })
                .show();
    }

    /**
     * Step 1 of "Mark as adopted": ask who adopted the pet. The owner can pick
     * one of the people who sent a request, or "Adopted outside the app".
     */
    private void startMarkAdopted(AdoptionListing listing) {

        db.collection("adoptionListings").document(listing.getId())
                .collection("requests")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    List<AdoptionRequest> requests = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        AdoptionRequest r = doc.toObject(AdoptionRequest.class);
                        r.setId(doc.getId());
                        requests.add(r);
                    }

                    showAdopterDialog(listing, requests);
                })
                .addOnFailureListener(e -> showAdopterDialog(listing, new ArrayList<>()));
    }

    private void showAdopterDialog(AdoptionListing listing, List<AdoptionRequest> requests) {

        List<String> names = new ArrayList<>();
        List<String> requesterIds = new ArrayList<>();

        for (AdoptionRequest r : requests) {
            String name = r.getRequesterName();
            names.add(name != null && !name.isEmpty() ? name : "A user");
            requesterIds.add(r.getRequesterId());
        }

        names.add("Adopted outside the app");

        new AlertDialog.Builder(this)
                .setTitle("Who adopted " + listing.getPetName() + "?")
                .setItems(names.toArray(new String[0]), (dialog, which) -> {

                    if (which < requests.size()) {
                        AdoptionRequest chosen = requests.get(which);
                        saveAdopted(listing, chosen.getRequesterId(), chosen.getRequesterName(), requesterIds);
                    } else {
                        saveAdopted(listing, null, null, requesterIds);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Step 2: save the adopted status. */
    private void saveAdopted(AdoptionListing listing, String adopterId, String adopterName,
                             List<String> allRequesterIds) {

        Map<String, Object> update = new HashMap<>();
        update.put("adopted", true);
        update.put("adoptedAtMillis", System.currentTimeMillis());
        update.put("adoptedById", adopterId);
        update.put("adoptedByName", adopterName);

        db.collection("adoptionListings").document(listing.getId())
                .update(update)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            this,
                            "🎉 " + listing.getPetName() + " marked as adopted",
                            Toast.LENGTH_SHORT
                    ).show();

                    // jump to the Adopted tab so the owner sees it move there
                    showAdopted = true;
                    loadListings();

                    NotificationsHelper.notifyAdoptionResult(
                            db, listing.getId(), listing.getPetName(),
                            NotificationsHelper.currentUserName(), adopterId, allRequesterIds);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void markAvailableAgain(AdoptionListing listing) {

        Map<String, Object> update = new HashMap<>();
        update.put("adopted", false);
        update.put("adoptedAtMillis", 0L);
        update.put("adoptedById", FieldValue.delete());
        update.put("adoptedByName", FieldValue.delete());

        db.collection("adoptionListings").document(listing.getId())
                .update(update)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            this,
                            listing.getPetName() + " is available for adoption again",
                            Toast.LENGTH_SHORT
                    ).show();

                    showAdopted = false;
                    loadListings();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    /**
     * Confirms listing deletion.
     */
    private void confirmDelete(AdoptionListing listing) {

        new AlertDialog.Builder(this)
                .setTitle("Delete this listing?")
                .setMessage(
                        listing.getPetName()
                                + "'s adoption post will be removed. "
                                + "This cannot be undone."
                )
                .setPositiveButton("Delete", (dialog, which) -> doDeleteListing(listing))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Grabs the requester ids first (so we can notify them), then deletes the listing. */
    private void doDeleteListing(AdoptionListing listing) {

        db.collection("adoptionListings").document(listing.getId())
                .collection("requests")
                .get()
                .addOnSuccessListener(requestsSnap -> {

                    List<String> requesterIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : requestsSnap) {
                        AdoptionRequest r = doc.toObject(AdoptionRequest.class);
                        if (r.getRequesterId() != null) requesterIds.add(r.getRequesterId());
                    }

                    db.collection("adoptionListings").document(listing.getId())
                            .delete()
                            .addOnSuccessListener(unused -> {

                                Toast.makeText(this, "Listing deleted", Toast.LENGTH_SHORT).show();
                                NotificationsHelper.notifyAdoptionDeleted(
                                        db, listing.getId(), listing.getPetName(),
                                        NotificationsHelper.currentUserName(), requesterIds);
                                loadListings();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // ------------------------------------------------------------------
    // Interest / chat (unchanged)
    // ------------------------------------------------------------------

    /**
     * Sends an adoption interest request.
     *
     * Before creating a new request, it checks whether
     * the same requester already has a chat for this listing.
     * This prevents duplicate chats.
     */
    private void expressInterest(AdoptionListing listing) {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String requesterId = user.getUid();
        String chatId = listing.getId() + "_" + requesterId;

        db.collection("adoptionChats")
                .document(chatId)
                .get()
                .addOnSuccessListener(existingChat -> {

                    if (existingChat.exists()) {

                        Intent intent = new Intent(
                                AdoptionListingsActivity.this,
                                AdoptionChatActivity.class
                        );

                        intent.putExtra("chatId", chatId);
                        intent.putExtra("petName", listing.getPetName());

                        startActivity(intent);

                        return;
                    }

                    String requesterName =
                            user.isAnonymous()
                                    ? "Guest"
                                    : (
                                    user.getDisplayName() != null
                                            && !user.getDisplayName().isEmpty()
                                    ? user.getDisplayName()
                                    : "A user"
                            );

                    String requestId = listing.getId() + "_" + requesterId;

                    DocumentReference requestRef =
                            db.collection("adoptionListings")
                                    .document(listing.getId())
                                    .collection("requests")
                                    .document(requestId);

                    AdoptionRequest request = new AdoptionRequest(
                            requestId,
                            listing.getId(),
                            requesterId,
                            requesterName,
                            System.currentTimeMillis()
                    );

                    requestRef.set(request)
                            .addOnSuccessListener(unused -> {

                                NotificationsHelper.notifyAdoptionInterest(
                                        db,
                                        listing.getOwnerId(),
                                        listing.getId(),
                                        listing.getPetName(),
                                        requesterName
                                );

                                openChat(listing, requesterId, requesterName);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(
                                            this,
                                            "Failed: " + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Failed to check existing chat: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    /**
     * Creates / updates the adoption chat document
     * and opens the chat screen.
     *
     * Both owner and requester names are stored so
     * the Messages inbox can identify both users.
     */
    private void openChat(AdoptionListing listing, String requesterId, String requesterName) {

        String chatId = listing.getId() + "_" + requesterId;

        Map<String, Object> chat = new HashMap<>();
        chat.put("ownerId", listing.getOwnerId());
        chat.put("ownerName", listing.getOwnerName());
        chat.put("requesterId", requesterId);
        chat.put("requesterName", requesterName);
        chat.put("listingId", listing.getId());
        chat.put("petName", listing.getPetName());

        db.collection("adoptionChats")
                .document(chatId)
                .set(chat, SetOptions.merge())
                .addOnSuccessListener(unused -> {

                    Intent intent = new Intent(
                            AdoptionListingsActivity.this,
                            AdoptionChatActivity.class
                    );

                    intent.putExtra("chatId", chatId);
                    intent.putExtra("petName", listing.getPetName());

                    startActivity(intent);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Failed to open chat: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }
}