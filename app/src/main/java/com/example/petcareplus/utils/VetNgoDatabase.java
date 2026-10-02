package com.example.petcareplus.utils;

import com.example.petcareplus.models.VetNgoLocation;

import java.util.ArrayList;
import java.util.List;

public class VetNgoDatabase {

    private static final List<VetNgoLocation> SEED = new ArrayList<>();

    static {
        // --- Vets (Mumbai) ---
        SEED.add(new VetNgoLocation("seed_vet1", "Bai Sakarbai Dinshaw Petit Hospital for Animals",
                "Vet", "Lala Lajpatrai Marg, Parel, Mumbai", "9:00 AM – 6:00 PM",
                "+91 22 2413 0738", 19.0017, 72.8362, false, null, 0));

        SEED.add(new VetNgoLocation("seed_vet2", "Vets & Pets Clinic",
                "Vet", "Andheri West, Mumbai", "10:00 AM – 8:00 PM",
                "+91 90000 11001", 19.1364, 72.8296, false, null, 0));

        SEED.add(new VetNgoLocation("seed_vet3", "Petsfolio Veterinary Clinic",
                "Vet", "Bandra West, Mumbai", "9:30 AM – 9:00 PM",
                "+91 90000 11002", 19.0596, 72.8295, false, null, 0));

        SEED.add(new VetNgoLocation("seed_vet4", "Dr. Rohan's Pet Clinic",
                "Vet", "Dadar West, Mumbai", "10:00 AM – 7:00 PM",
                "+91 90000 11003", 19.0178, 72.8478, false, null, 0));

        // --- NGOs (Mumbai) ---
        SEED.add(new VetNgoLocation("seed_ngo1", "The Welfare of Stray Dogs (WSD)",
                "NGO", "Mahim, Mumbai", "9:00 AM – 6:00 PM",
                "+91 22 2446 2394", 19.0415, 72.8400, true, null, 0));

        SEED.add(new VetNgoLocation("seed_ngo2", "Bombay Society for Prevention of Cruelty to Animals (BSPCA)",
                "NGO", "Parel, Mumbai", "9:00 AM – 6:00 PM",
                "+91 22 2413 6008", 19.0015, 72.8365, true, null, 0));

        SEED.add(new VetNgoLocation("seed_ngo3", "In Defence of Animals (IDA)",
                "NGO", "Andheri East, Mumbai", "10:00 AM – 6:00 PM",
                "+91 90000 22001", 19.1197, 72.8697, true, null, 0));

        SEED.add(new VetNgoLocation("seed_ngo4", "World for Pets",
                "NGO", "Powai, Mumbai", "24 hours (emergency line)",
                "+91 90000 22002", 19.1176, 72.9060, true, null, 0));
    }

    public static List<VetNgoLocation> getSeedByType(String type) {
        List<VetNgoLocation> result = new ArrayList<>();
        for (VetNgoLocation loc : SEED) {
            if (loc.type.equals(type)) result.add(loc);
        }
        return result;
    }
}