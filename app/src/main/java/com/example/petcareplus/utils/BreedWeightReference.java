package com.example.petcareplus.utils;

import java.util.HashMap;
import java.util.Map;

public class BreedWeightReference {

    // Adult target weight in kg, per breed. Extend this table as needed.
    private static final Map<String, Double> DOG_ADULT_WEIGHTS = new HashMap<>();
    private static final Map<String, Double> CAT_ADULT_WEIGHTS = new HashMap<>();

    static {
        // ===== DOG BREEDS (adult average weight, kg) =====
        DOG_ADULT_WEIGHTS.put("Labrador Retriever", 30.0);
        DOG_ADULT_WEIGHTS.put("Golden Retriever", 30.0);
        DOG_ADULT_WEIGHTS.put("German Shepherd", 32.0);
        DOG_ADULT_WEIGHTS.put("Beagle", 11.0);
        DOG_ADULT_WEIGHTS.put("Pug", 7.0);
        DOG_ADULT_WEIGHTS.put("Shih Tzu", 6.0);
        DOG_ADULT_WEIGHTS.put("Rottweiler", 45.0);
        DOG_ADULT_WEIGHTS.put("Indie", 20.0);
        DOG_ADULT_WEIGHTS.put("Dachshund", 8.0);
        DOG_ADULT_WEIGHTS.put("Boxer", 28.0);
        DOG_ADULT_WEIGHTS.put("Doberman", 35.0);
        DOG_ADULT_WEIGHTS.put("Great Dane", 60.0);
        DOG_ADULT_WEIGHTS.put("Siberian Husky", 21.0);
        DOG_ADULT_WEIGHTS.put("Pomeranian", 3.0);
        DOG_ADULT_WEIGHTS.put("Cocker Spaniel", 13.0);
        DOG_ADULT_WEIGHTS.put("Border Collie", 18.0);
        DOG_ADULT_WEIGHTS.put("Chihuahua", 2.5);
        DOG_ADULT_WEIGHTS.put("French Bulldog", 11.0);
        DOG_ADULT_WEIGHTS.put("German Shorthaired Pointer", 27.0);
        DOG_ADULT_WEIGHTS.put("Australian Shepherd", 23.0);
        DOG_ADULT_WEIGHTS.put("Saint Bernard", 65.0);
        DOG_ADULT_WEIGHTS.put("Cane Corso", 45.0);
        DOG_ADULT_WEIGHTS.put("Corgi", 12.0);
        DOG_ADULT_WEIGHTS.put("Dalmatian", 27.0);

        // ===== CAT BREEDS (adult average weight, kg) =====
        CAT_ADULT_WEIGHTS.put("Persian", 4.5);
        CAT_ADULT_WEIGHTS.put("Siamese", 4.0);
        CAT_ADULT_WEIGHTS.put("Maine Coon", 7.0);
        CAT_ADULT_WEIGHTS.put("British Shorthair", 5.5);
        CAT_ADULT_WEIGHTS.put("Bengal", 5.0);
        CAT_ADULT_WEIGHTS.put("Ragdoll", 6.0);
        CAT_ADULT_WEIGHTS.put("Indian Domestic Shorthair", 4.0);
        CAT_ADULT_WEIGHTS.put("American Shorthair", 5.0);
        CAT_ADULT_WEIGHTS.put("Scottish Fold", 4.5);
        CAT_ADULT_WEIGHTS.put("Sphynx", 4.0);
        CAT_ADULT_WEIGHTS.put("Russian Blue", 4.5);
        CAT_ADULT_WEIGHTS.put("Abyssinian", 4.0);
        CAT_ADULT_WEIGHTS.put("Birman", 5.0);
        CAT_ADULT_WEIGHTS.put("Norwegian Forest Cat", 6.5);
        CAT_ADULT_WEIGHTS.put("Oriental Shorthair", 4.0);
        CAT_ADULT_WEIGHTS.put("Turkish Angora", 4.0);
        CAT_ADULT_WEIGHTS.put("Himalayan", 4.5);
        CAT_ADULT_WEIGHTS.put("Burmese", 4.5);
        CAT_ADULT_WEIGHTS.put("Bombay", 4.5);
        CAT_ADULT_WEIGHTS.put("Savannah", 8.0);
        CAT_ADULT_WEIGHTS.put("Manx", 4.5);
        CAT_ADULT_WEIGHTS.put("Exotic Shorthair", 4.5);
        CAT_ADULT_WEIGHTS.put("Turkish Van", 6.0);
        CAT_ADULT_WEIGHTS.put("Cornish Rex", 3.5);
    }

    // Fallback averages for breeds not in the table (e.g. custom "Other" entries)
    private static final double DOG_FALLBACK_ADULT_WEIGHT = 20.0;
    private static final double CAT_FALLBACK_ADULT_WEIGHT = 4.5;

    public static class WeightStatus {
        public String verdict;      // "Underweight", "Perfect weight", "Overweight"
        public double targetWeight; // expected weight at this age, in kg
        public double lowerBound;
        public double upperBound;

        public WeightStatus(String verdict, double targetWeight, double lowerBound, double upperBound) {
            this.verdict = verdict;
            this.targetWeight = targetWeight;
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }
    }

    /**
     * @param petType    "Dog" or "Cat"
     * @param breed      breed name (matches Add/Edit Pet spinner values)
     * @param ageMonths  pet's age in total months
     * @param actualKg   weight the user entered
     */
    public static WeightStatus evaluate(String petType, String breed, double ageMonths, double actualKg) {

        boolean isDog = petType != null && petType.equalsIgnoreCase("Dog");

        Double adultWeight = isDog
                ? DOG_ADULT_WEIGHTS.get(breed)
                : CAT_ADULT_WEIGHTS.get(breed);

        if (adultWeight == null) {
            adultWeight = isDog ? DOG_FALLBACK_ADULT_WEIGHT : CAT_FALLBACK_ADULT_WEIGHT;
        }

        double growthFraction = getGrowthFraction(isDog, ageMonths);
        double targetWeight = adultWeight * growthFraction;

        // Cushion band: ±15% of target counts as "Perfect weight"
        double lowerBound = targetWeight * 0.85;
        double upperBound = targetWeight * 1.15;

        String verdict;
        if (actualKg < lowerBound) {
            verdict = "Underweight";
        } else if (actualKg > upperBound) {
            verdict = "Overweight";
        } else {
            verdict = "Perfect weight";
        }

        return new WeightStatus(verdict, targetWeight, lowerBound, upperBound);
    }

    /**
     * Returns what fraction of adult weight a pet of this age is expected
     * to have reached. Approximated growth curve — cats/small dogs mature
     * faster than large dog breeds, but this simplified curve is close
     * enough for a wellness indicator (not a veterinary diagnosis).
     */
    private static double getGrowthFraction(boolean isDog, double ageMonths) {

        if (ageMonths >= 18) return 1.0; // fully grown

        if (isDog) {
            if (ageMonths <= 1) return 0.10;
            if (ageMonths <= 2) return 0.20;
            if (ageMonths <= 3) return 0.30;
            if (ageMonths <= 4) return 0.45;
            if (ageMonths <= 6) return 0.60;
            if (ageMonths <= 8) return 0.75;
            if (ageMonths <= 12) return 0.90;
            return 0.97; // 12-18 months
        } else {
            // Cats mature faster than most dogs
            if (ageMonths <= 1) return 0.15;
            if (ageMonths <= 2) return 0.30;
            if (ageMonths <= 3) return 0.45;
            if (ageMonths <= 4) return 0.60;
            if (ageMonths <= 6) return 0.75;
            if (ageMonths <= 8) return 0.85;
            if (ageMonths <= 12) return 0.95;
            return 1.0; // 12-18 months
        }
    }
}