package com.example.petcareplus.utils;

import com.example.petcareplus.models.SnackItem;

import java.util.ArrayList;
import java.util.List;

public class SnackDatabase {

    private static final List<SnackItem> SNACKS = new ArrayList<>();

    static {
        SNACKS.add(new SnackItem("Carrot Sticks", "🥕", "Dog", "Low-calorie, crunchy — great for teeth."));
        SNACKS.add(new SnackItem("Plain Popcorn", "🍿", "Dog", "No butter, salt, or seasoning."));
        SNACKS.add(new SnackItem("Frozen Blueberries", "🫐", "Dog", "A cool, antioxidant-rich summer treat."));
        SNACKS.add(new SnackItem("Peanut Butter Lick Mat", "🥜", "Dog", "Use only xylitol-free peanut butter."));
        SNACKS.add(new SnackItem("Boiled Chicken Bits", "🍗", "Dog", "Plain, boneless, unseasoned."));
        SNACKS.add(new SnackItem("Green Beans", "🫛", "Dog", "Low-calorie filler, good for weight management."));
        SNACKS.add(new SnackItem("Pumpkin Puree", "🎃", "Dog", "Plain, unsweetened — supports digestion."));
        SNACKS.add(new SnackItem("Apple Slices", "🍎", "Dog", "Remove seeds and core first."));
        SNACKS.add(new SnackItem("Dental Chews", "🦴", "Dog", "Choose a size appropriate for your dog's breed."));

        SNACKS.add(new SnackItem("Cooked Salmon Flakes", "🐟", "Cat", "Fully cooked, boneless, unseasoned."));
        SNACKS.add(new SnackItem("Freeze-Dried Chicken Treats", "🍗", "Cat", "Single-ingredient treats are easiest on digestion."));
        SNACKS.add(new SnackItem("Catnip", "🌿", "Cat", "Not all cats respond — safe either way in small amounts."));
        SNACKS.add(new SnackItem("Plain Yogurt (small amount)", "🥣", "Cat", "Only if your cat tolerates dairy well."));
        SNACKS.add(new SnackItem("Cooked Egg Bits", "🥚", "Cat", "Fully cooked only — never raw egg."));
        SNACKS.add(new SnackItem("Cat Grass", "🌱", "Cat", "Aids digestion and gives cats something safe to chew."));
        SNACKS.add(new SnackItem("Tuna Water (occasional)", "🐟", "Cat", "No added salt — treat as an occasional flavor boost, not a meal."));

        SNACKS.add(new SnackItem("Watermelon (no seeds/rind)", "🍉", "Both", "Hydrating, low-calorie treat in small amounts."));
        SNACKS.add(new SnackItem("Cooked Sweet Potato", "🍠", "Both", "Plain and mashed or in small cubes."));
    }

    public static List<SnackItem> getForSpecies(String species) {
        List<SnackItem> result = new ArrayList<>();
        for (SnackItem item : SNACKS) {
            if (item.species.equals(species) || item.species.equals("Both")) {
                result.add(item);
            }
        }
        return result;
    }
}