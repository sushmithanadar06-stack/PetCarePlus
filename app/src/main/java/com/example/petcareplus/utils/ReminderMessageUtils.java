package com.example.petcareplus.utils;

import java.util.Random;

public class ReminderMessageUtils {

    private static final Random random = new Random();

    // Feeding reminders
    private static final String[] FEEDING_MESSAGES = {
            "%s is waiting for food 🍽️🐾",
            "Time to feed %s! Someone's getting hungry 🐶🍖",
            "%s's tummy is growling — feeding time! 🐱🥣",
            "Dinner bell! %s is ready to eat 🍽️❤️"
    };

    // Medication reminders
    private static final String[] MEDICATION_MESSAGES = {
            "Time for %s's medicine 💊🐾",
            "Don't forget %s's meds today 💊❤️",
            "%s needs their medication now 💊🐶",
            "Medicine time for %s! Stay healthy 💊✨"
    };

    // Walk reminders
    private static final String[] WALK_MESSAGES = {
            "%s wants a walk! 🐕🚶",
            "Time to stretch those paws, %s! 🐾🌳",
            "%s is asking for a walk outside 🐶☀️",
            "Fresh air time for %s! 🐕‍🦺🍃"
    };

    // Grooming reminders
    private static final String[] GROOMING_MESSAGES = {
            "%s could use a little grooming today 🛁🐾",
            "Bath time for %s! 🧼🐶",
            "Time to brush %s's coat ✨🐾",
            "%s is due for a grooming session 💇🐱"
    };

    // Vet appointment reminders
    private static final String[] VET_MESSAGES = {
            "%s has a vet appointment coming up 🏥🐾",
            "Don't forget %s's vet visit today 🩺❤️",
            "Time to take %s to the vet 🚗🐶",
            "%s's checkup is today! 🏥✨"
    };

    // Generic fallback
    private static final String[] GENERIC_MESSAGES = {
            "%s has a reminder for you 🐾",
            "Don't forget about %s today ❤️",
            "%s needs your attention 🐶🐱"
    };

    /**
     * Builds a friendly, emoji-filled message for a reminder notification.
     *
     * @param petName      the pet's name, e.g. "Chellu"
     * @param reminderType e.g. "Feeding", "Medication", "Walk", "Grooming", "Vet"
     * @return a formatted, randomly-varied message
     */
    public static String buildMessage(String petName, String reminderType) {
        String[] pool;

        if (reminderType == null) {
            pool = GENERIC_MESSAGES;
        } else {
            switch (reminderType.trim().toLowerCase()) {
                case "feeding":
                case "food":
                    pool = FEEDING_MESSAGES;
                    break;
                case "medication":
                case "medicine":
                    pool = MEDICATION_MESSAGES;
                    break;
                case "walk":
                case "exercise":
                    pool = WALK_MESSAGES;
                    break;
                case "grooming":
                case "bath":
                    pool = GROOMING_MESSAGES;
                    break;
                case "vet":
                case "vet appointment":
                case "checkup":
                    pool = VET_MESSAGES;
                    break;
                default:
                    pool = GENERIC_MESSAGES;
            }
        }

        String template = pool[random.nextInt(pool.length)];
        return String.format(template, petName);
    }

    /**
     * Builds the notification title based on reminder type, e.g. "🍽️ Feeding Time".
     */
    public static String buildTitle(String reminderType) {
        if (reminderType == null) return "🐾 Pet Reminder";

        switch (reminderType.trim().toLowerCase()) {
            case "feeding":
            case "food":
                return "🍽️ Feeding Time";
            case "medication":
            case "medicine":
                return "💊 Medication Reminder";
            case "walk":
            case "exercise":
                return "🐕 Walk Time";
            case "grooming":
            case "bath":
                return "🛁 Grooming Time";
            case "vet":
            case "vet appointment":
            case "checkup":
                return "🏥 Vet Appointment";
            default:
                return "🐾 Pet Reminder";
        }
    }
}