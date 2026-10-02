package com.example.petcareplus.utils;

import com.example.petcareplus.models.ChatRoom;

import java.util.ArrayList;
import java.util.List;

public class ChatRoomsDatabase {

    private static final List<ChatRoom> ROOMS = new ArrayList<>();

    static {
        ROOMS.add(new ChatRoom("dogowners", "Dog Owners", "🐕",
                "Breeds, training, behaviour", 1, false));

        ROOMS.add(new ChatRoom("catowners", "Cat Owners", "🐈",
                "Breeds, litter, behaviour", 1, false));

        ROOMS.add(new ChatRoom("healthvet", "Health & Vet Advice", "🏥",
                "Symptoms, vet recommendations", 1, true));

        ROOMS.add(new ChatRoom("lostfound", "Lost & Found", "🆘",
                "Missing pets and sightings · kept 7 days", 7, false));

        ROOMS.add(new ChatRoom("general", "General", "💬",
                "Everything else", 1, false));
    }

    public static List<ChatRoom> getAll() {
        return ROOMS;
    }

    public static ChatRoom findById(String id) {
        for (ChatRoom room : ROOMS) {
            if (room.id.equals(id)) return room;
        }
        return null;
    }
}