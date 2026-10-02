package com.example.petcareplus.utils;

import com.example.petcareplus.models.NutritionStage;

import java.util.ArrayList;
import java.util.List;

public class NutritionGuideDatabase {

    public static List<NutritionStage> getDogStages() {
        List<NutritionStage> list = new ArrayList<>();
        list.add(new NutritionStage("Dog", "Puppy (0–12 months)", "22–32%", "10–25%", "3–4 meals/day",
                "High-calorie growth formula. Small, frequent meals support steady development."));
        list.add(new NutritionStage("Dog", "Adult (1–7 years)", "18–25%", "8–15%", "2 meals/day",
                "Maintain a lean body weight. Adjust portions to activity level and breed size."));
        list.add(new NutritionStage("Dog", "Senior (7+ years)", "18–25%", "7–12%", "2 meals/day",
                "Lower calorie needs overall. Joint-support and easy-digest formulas are often helpful."));
        return list;
    }

    public static List<NutritionStage> getCatStages() {
        List<NutritionStage> list = new ArrayList<>();
        list.add(new NutritionStage("Cat", "Kitten (0–12 months)", "30–40%", "18–25%", "3–4 meals/day",
                "High protein and fat for rapid growth. Always keep fresh water available."));
        list.add(new NutritionStage("Cat", "Adult (1–10 years)", "26–35%", "9–15%", "2 meals/day",
                "Cats are obligate carnivores — prioritize animal-based protein. Watch portion sizes."));
        list.add(new NutritionStage("Cat", "Senior (10+ years)", "26–35%", "9–15%", "2–3 smaller meals/day",
                "May benefit from kidney-friendly formulas. Monitor hydration closely."));
        return list;
    }

    public static List<String> getGeneralTips() {
        List<String> tips = new ArrayList<>();
        tips.add("Fresh water should always be available, regardless of species or age.");
        tips.add("Sudden diet changes can upset digestion — transition over 5–7 days by mixing old and new food.");
        tips.add("Treats should make up no more than 10% of daily calorie intake.");
        tips.add("Overweight pets are at higher risk for joint, heart, and metabolic problems — monitor body condition, not just the scale.");
        tips.add("This guide is general information only. For a diet plan tailored to your pet's breed, weight, and health history, consult your veterinarian.");
        return tips;
    }
}