package com.example.petcareplus.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcareplus.R;
import com.example.petcareplus.models.ChatRoom;
import com.example.petcareplus.utils.ChatRoomsDatabase;

public class CommunityRoomsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_rooms);

        TextView btnCommunityBack = findViewById(R.id.btnCommunityBack);
        TextView btnTabAdoption = findViewById(R.id.btnTabAdoption);
        TextView btnTabChatRooms = findViewById(R.id.btnTabChatRooms);
        LinearLayout roomsListContainer = findViewById(R.id.roomsListContainer);

        btnCommunityBack.setOnClickListener(v -> finish());

        btnTabAdoption.setOnClickListener(v -> {
            startActivity(new Intent(CommunityRoomsActivity.this, AdoptionListingsActivity.class));
            finish();
        });

        LayoutInflater inflater = LayoutInflater.from(this);

        for (ChatRoom room : ChatRoomsDatabase.getAll()) {

            View row = inflater.inflate(R.layout.item_chat_room, roomsListContainer, false);

            TextView txtIcon = row.findViewById(R.id.txtRoomIcon);
            TextView txtName = row.findViewById(R.id.txtRoomName);
            TextView txtDescription = row.findViewById(R.id.txtRoomDescription);

            txtIcon.setText(room.icon);
            txtName.setText(room.name);
            txtDescription.setText(room.description);

            row.setOnClickListener(v -> {
                Intent intent = new Intent(CommunityRoomsActivity.this, ChatRoomActivity.class);
                intent.putExtra("roomId", room.id);
                startActivity(intent);
            });

            roomsListContainer.addView(row);
        }
    }
}