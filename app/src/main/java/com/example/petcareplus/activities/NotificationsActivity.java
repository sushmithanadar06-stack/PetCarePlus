package com.example.petcareplus.activities;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.models.AppNotification;
import com.example.petcareplus.models.RescueReport;
import com.example.petcareplus.utils.NotificationsHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Module 13 - the notifications inbox. Updates live: new notifications appear by themselves.
 * Tap = mark as read and open the related screen. Long-press = delete.
 */
public class NotificationsActivity extends AppCompatActivity {

    private LinearLayout listContainer;
    private TextView txtEmpty, txtMarkAllRead;

    private FirebaseFirestore db;
    private String uid;
    private ListenerRegistration registration;

    private final List<AppNotification> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        uid = user.getUid();
        db = FirebaseFirestore.getInstance();

        listContainer = findViewById(R.id.listContainer);
        txtEmpty = findViewById(R.id.txtEmpty);
        txtMarkAllRead = findViewById(R.id.txtMarkAllRead);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        txtMarkAllRead.setOnClickListener(v -> NotificationsHelper.markAllRead(db, uid));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (uid == null) return;

        registration = db.collection("notifications")
                .whereEqualTo("recipientId", uid)
                .addSnapshotListener((snap, e) -> {

                    if (e != null) {
                        Toast.makeText(this, "Couldn't load notifications: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snap == null) return;

                    items.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        AppNotification n = doc.toObject(AppNotification.class);
                        if (n != null) {
                            n.setId(doc.getId());
                            items.add(n);
                        }
                    }

                    Collections.sort(items, (a, b) ->
                            Long.compare(b.getTimestampMillis(), a.getTimestampMillis()));

                    render();
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private void render() {

        listContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        int unread = 0;

        for (AppNotification n : items) {
            if (!n.isRead()) unread++;
            listContainer.addView(buildItem(inflater, n));
        }

        txtEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        txtMarkAllRead.setVisibility(unread > 0 ? View.VISIBLE : View.INVISIBLE);
    }

    private View buildItem(LayoutInflater inflater, AppNotification n) {

        View v = inflater.inflate(R.layout.item_notification, listContainer, false);

        TextView icon = v.findViewById(R.id.txtNotifIcon);
        TextView title = v.findViewById(R.id.txtNotifTitle);
        TextView message = v.findViewById(R.id.txtNotifMessage);
        TextView time = v.findViewById(R.id.txtNotifTime);
        View dot = v.findViewById(R.id.viewUnreadDot);

        title.setText(NotificationsHelper.titleFor(n));
        message.setText(NotificationsHelper.messageFor(n));
        time.setText(RescueReport.timeAgo(n.getTimestampMillis()));
        icon.setText(NotificationsHelper.iconFor(n));

        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(0xFFE3EFE8);
        icon.setBackground(iconBg);

        if (n.isRead()) {
            v.setBackgroundResource(R.drawable.bg_rescue_card);
            dot.setVisibility(View.INVISIBLE);
        } else {
            v.setBackgroundResource(R.drawable.bg_notification_unread);
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            dotBg.setColor(0xFFE07A3E);
            dot.setBackground(dotBg);
            dot.setVisibility(View.VISIBLE);
        }

        v.setOnClickListener(view -> {
            if (!n.isRead()) NotificationsHelper.markRead(db, n.getId());
            startActivity(NotificationsHelper.targetIntent(this, n));
        });

        v.setOnLongClickListener(view -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete this notification?")
                    .setPositiveButton("Delete", (d, w) ->
                            db.collection("notifications").document(n.getId()).delete()
                                    .addOnFailureListener(e -> Toast.makeText(this,
                                            "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()))
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        return v;
    }
}