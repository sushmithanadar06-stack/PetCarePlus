package com.example.petcareplus.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Basic client-side profanity filter. This is a word-list check, not a
 * sophisticated moderation system — it catches casual bad language before
 * it's posted. It won't catch deliberate evasion (misspellings, symbols),
 * but it's the right scope for a client-only app with no backend.
 *
 * Add/remove words in BLOCKED_WORDS as needed. Keep everything lowercase.
 */
public class ProfanityFilter {

    private static final Set<String> BLOCKED_WORDS = new HashSet<>(Arrays.asList(
            "fuck", "shit", "bitch", "asshole", "bastard", "dick", "cunt",
            "whore", "slut", "nigger", "faggot", "retard"
            // Add more terms here as needed — keep lowercase, single words.
    ));

    /**
     * Returns true if the text contains a blocked word as a whole word
     * (not a substring match, to avoid false positives like "classic").
     */
    public static boolean containsProfanity(String text) {

        if (text == null || text.trim().isEmpty()) return false;

        String lower = text.toLowerCase();
        String[] words = lower.split("[^a-z']+");

        for (String word : words) {
            if (BLOCKED_WORDS.contains(word)) {
                return true;
            }
        }
        return false;
    }
}