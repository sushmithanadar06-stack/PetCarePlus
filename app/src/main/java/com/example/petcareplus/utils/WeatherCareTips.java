package com.example.petcareplus.utils;

import com.example.petcareplus.models.WeatherInfo;

import java.util.ArrayList;
import java.util.List;

public class WeatherCareTips {

    public static class Tip {
        public String icon;
        public String title;
        public String description;

        public Tip(String icon, String title, String description) {
            this.icon = icon;
            this.title = title;
            this.description = description;
        }
    }

    public static List<Tip> getTips(WeatherInfo weather, String petType) {

        List<Tip> tips = new ArrayList<>();
        boolean isDog = petType != null && petType.equalsIgnoreCase("Dog");

        // ===== RAIN / MONSOON TIPS =====
        if (weather.isRaining) {

            tips.add(new Tip("🌧️", "Rain Precaution",
                    "Dry paws and coat thoroughly after walks to prevent fungal infections."));

            if (isDog) {
                tips.add(new Tip("🦶", "Paw Care",
                        "Check between paw pads for trapped mud or debris after monsoon walks."));
            } else {
                tips.add(new Tip("🐾", "Keep Indoors",
                        "Cats generally avoid rain on their own — keep windows secure so they stay dry."));
            }

            tips.add(new Tip("🦟", "Pest Alert",
                    "Monsoon increases tick and flea activity — check coat regularly."));
        }

        // ===== HOT WEATHER TIPS (33°C+) =====
        else if (weather.temperature >= 33) {

            tips.add(new Tip("🥵", "Hot Weather Precautions",
                    "Avoid walks between 12–4 PM, keep water topped up throughout the day."));

            if (isDog) {
                tips.add(new Tip("🐾", "Paw Care",
                        "Hot pavement can burn paw pads — walk on grass when possible."));
            }

            tips.add(new Tip("💧", "Hydration",
                    "Refill water bowls more frequently — pets dehydrate faster in heat."));

            tips.add(new Tip("🚫", "Never Leave in Vehicles",
                    "Car interiors heat up dangerously fast, even with windows cracked."));
        }

        // ===== COOL WEATHER TIPS (20°C or below) =====
        else if (weather.temperature <= 20) {

            tips.add(new Tip("🧥", "Cold Weather Care",
                    "Short-haired breeds may need a light coat during early morning or night walks."));

            tips.add(new Tip("🛏️", "Warm Bedding",
                    "Keep sleeping areas away from cold floors and drafts."));

            if (!isDog) {
                tips.add(new Tip("🐈", "Indoor Comfort",
                        "Cats naturally seek warm spots — make sure a cozy corner is available."));
            }
        }

        // ===== PLEASANT WEATHER (21-32°C, no rain) =====
        else {

            tips.add(new Tip("☀️", "Great Weather",
                    "Good conditions for regular walks and outdoor playtime."));

            tips.add(new Tip("💧", "Stay Hydrated",
                    "Keep fresh water available as usual, even in mild weather."));
        }

        // ===== HIGH HUMIDITY (common in Mumbai, worth flagging separately) =====
        if (weather.humidity >= 75 && !weather.isRaining) {
            tips.add(new Tip("💦", "High Humidity",
                    "Humid air makes it harder for pets to cool down by panting — watch for excess panting or lethargy."));
        }

        return tips;
    }
}