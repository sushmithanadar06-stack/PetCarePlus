package com.example.petcareplus.utils;

import com.example.petcareplus.models.FoodSafetyItem;

import java.util.ArrayList;
import java.util.List;

public class FoodSafetyDatabase {

    private static final List<FoodSafetyItem> FOODS = new ArrayList<>();

    static {
        FOODS.add(new FoodSafetyItem("Grapes", "🍇",
                "Unsafe", "Toxic to dogs — can cause kidney failure. Keep out of reach.",
                "Unsafe", "Toxic to cats — can cause kidney failure. Keep out of reach."));

        FOODS.add(new FoodSafetyItem("Raisins", "🍇",
                "Unsafe", "Same toxicity as grapes — can cause kidney failure.",
                "Unsafe", "Same toxicity as grapes — can cause kidney failure."));

        FOODS.add(new FoodSafetyItem("Chocolate", "🍫",
                "Unsafe", "Contains theobromine — toxic to dogs, can cause seizures or death.",
                "Unsafe", "Contains theobromine — toxic to cats, cats are especially sensitive."));

        FOODS.add(new FoodSafetyItem("Onion", "🧅",
                "Unsafe", "Damages red blood cells, can cause anemia.",
                "Unsafe", "Damages red blood cells, can cause anemia — cats are more sensitive than dogs."));

        FOODS.add(new FoodSafetyItem("Garlic", "🧄",
                "Unsafe", "More potent than onion — avoid in any form, even powdered.",
                "Unsafe", "More potent than onion — avoid in any form, even powdered."));

        FOODS.add(new FoodSafetyItem("Chicken (cooked, plain)", "🍗",
                "Safe", "Good lean protein source when boneless and unseasoned.",
                "Safe", "Good lean protein source when boneless and unseasoned."));

        FOODS.add(new FoodSafetyItem("Rice (plain, cooked)", "🍚",
                "Safe", "Easy to digest, often used for upset stomachs.",
                "Safe", "Easy to digest in small amounts."));

        FOODS.add(new FoodSafetyItem("Apple (no seeds/core)", "🍎",
                "Safe", "Good source of fiber and vitamins in small amounts.",
                "Caution", "Cats have little nutritional need for fruit — small amounts are fine but not necessary."));

        FOODS.add(new FoodSafetyItem("Avocado", "🥑",
                "Unsafe", "Contains persin — can cause vomiting and diarrhea.",
                "Unsafe", "Contains persin — toxic, especially the pit and skin."));

        FOODS.add(new FoodSafetyItem("Xylitol / sugar-free gum", "🍬",
                "Unsafe", "Extremely toxic — can cause rapid insulin release, liver failure.",
                "Unsafe", "Extremely toxic — avoid completely."));

        FOODS.add(new FoodSafetyItem("Milk", "🥛",
                "Caution", "Many dogs are lactose intolerant — can cause diarrhea.",
                "Caution", "Most cats are lactose intolerant despite the stereotype — avoid regular milk."));

        FOODS.add(new FoodSafetyItem("Cheese", "🧀",
                "Caution", "OK in small amounts, but high in fat — avoid daily feeding.",
                "Caution", "OK in small amounts occasionally, high in fat."));

        FOODS.add(new FoodSafetyItem("Bread (plain)", "🍞",
                "Safe", "Plain bread in small amounts is generally fine, no nutritional benefit.",
                "Safe", "Plain bread in small amounts is generally fine, no nutritional benefit."));

        FOODS.add(new FoodSafetyItem("Peanut Butter (xylitol-free)", "🥜",
                "Safe", "Safe in moderation — always check label for xylitol first.",
                "Caution", "Not toxic, but high fat content — only occasional small amounts."));

        FOODS.add(new FoodSafetyItem("Salmon (cooked, boneless)", "🐟",
                "Safe", "Great source of omega-3s when fully cooked and boneless.",
                "Safe", "Great source of omega-3s when fully cooked and boneless."));

        FOODS.add(new FoodSafetyItem("Eggs (cooked)", "🥚",
                "Safe", "Good protein source when fully cooked.",
                "Safe", "Good protein source when fully cooked."));

        FOODS.add(new FoodSafetyItem("Blueberries", "🫐",
                "Safe", "Antioxidant-rich, safe in small amounts.",
                "Caution", "Not toxic, but no real nutritional need — offer sparingly."));

        FOODS.add(new FoodSafetyItem("Watermelon (no seeds/rind)", "🍉",
                "Safe", "Hydrating treat in small amounts, remove seeds and rind.",
                "Caution", "Not toxic but cats rarely need fruit — small amounts only."));

        FOODS.add(new FoodSafetyItem("Tomato (ripe)", "🍅",
                "Caution", "Ripe tomato flesh is fine; avoid stems/leaves/green tomato.",
                "Caution", "Ripe tomato flesh is fine; avoid stems/leaves/green tomato."));

        FOODS.add(new FoodSafetyItem("Potato (cooked, no skin)", "🥔",
                "Safe", "Cooked and plain is fine; never feed raw potato.",
                "Caution", "Cooked plain potato in tiny amounts only; not a natural food for cats."));

        FOODS.add(new FoodSafetyItem("Mushroom (store-bought)", "🍄",
                "Caution", "Store-bought/cooked plain mushrooms are usually fine; never wild mushrooms.",
                "Caution", "Store-bought/cooked plain mushrooms are usually fine; never wild mushrooms."));

        FOODS.add(new FoodSafetyItem("Macadamia Nuts", "🌰",
                "Unsafe", "Can cause weakness, tremors, and elevated body temperature.",
                "Unsafe", "Can cause similar toxic reactions — avoid completely."));

        FOODS.add(new FoodSafetyItem("Alcohol", "🍺",
                "Unsafe", "Even small amounts can cause serious poisoning.",
                "Unsafe", "Even small amounts can cause serious poisoning."));

        FOODS.add(new FoodSafetyItem("Coffee / Caffeine", "☕",
                "Unsafe", "Contains caffeine — toxic, can affect heart and nervous system.",
                "Unsafe", "Contains caffeine — toxic, cats are especially sensitive."));

        FOODS.add(new FoodSafetyItem("Cooked Bones", "🦴",
                "Unsafe", "Cooked bones splinter easily and can cause internal injury.",
                "Unsafe", "Cooked bones splinter easily and can cause internal injury."));

        FOODS.add(new FoodSafetyItem("Citrus (orange, lemon)", "🍊",
                "Caution", "Small amounts of flesh are OK; peel and pith can cause stomach upset.",
                "Unsafe", "Cats are generally more sensitive to citrus oils — best avoided."));

        FOODS.add(new FoodSafetyItem("Coconut", "🥥",
                "Safe", "Coconut flesh/water in small amounts is generally fine.",
                "Caution", "Not toxic, but no nutritional need — offer sparingly if at all."));

        FOODS.add(new FoodSafetyItem("Spinach", "🥬",
                "Caution", "Safe in small amounts; avoid with pets prone to kidney/bladder stones.",
                "Caution", "Safe in small amounts; avoid with pets prone to kidney/bladder stones."));

        FOODS.add(new FoodSafetyItem("Corn (off the cob)", "🌽",
                "Safe", "Kernels are fine in small amounts; corn cob is a choking/blockage hazard.",
                "Caution", "Not a natural food for cats — occasional small amounts only."));

        FOODS.add(new FoodSafetyItem("Broccoli", "🥦",
                "Safe", "Safe in small, cooked or raw amounts.",
                "Caution", "Not toxic, but limited nutritional value for cats."));

        FOODS.add(new FoodSafetyItem("Carrot", "🥕",
                "Safe", "Great low-calorie crunchy treat, raw or cooked.",
                "Caution", "Not toxic, but cats gain little nutritional benefit."));

        FOODS.add(new FoodSafetyItem("Pumpkin (plain, cooked)", "🎃",
                "Safe", "Great for digestion — plain, unsweetened, no spices.",
                "Safe", "Great for digestion — plain, unsweetened, no spices."));

        FOODS.add(new FoodSafetyItem("Plain Yogurt", "🥣",
                "Safe", "Probiotic benefits in small amounts if lactose-tolerant.",
                "Caution", "Small amounts OK for some cats; many are lactose intolerant."));

        FOODS.add(new FoodSafetyItem("Honey", "🍯",
                "Caution", "Safe in small amounts for adult dogs; avoid for puppies.",
                "Caution", "Not toxic but no real benefit — offer sparingly."));

        FOODS.add(new FoodSafetyItem("Salt / Salty Snacks", "🧂",
                "Unsafe", "Excess salt can cause sodium poisoning — avoid salty snacks.",
                "Unsafe", "Excess salt can cause sodium poisoning — avoid salty snacks."));

        FOODS.add(new FoodSafetyItem("Cinnamon", "🟤",
                "Caution", "Not toxic in small amounts, but can irritate the mouth/stomach.",
                "Caution", "Not toxic in small amounts, but can irritate the mouth/stomach."));

        FOODS.add(new FoodSafetyItem("Nutmeg", "🟠",
                "Unsafe", "Contains myristicin — can cause tremors and seizures in large amounts.",
                "Unsafe", "Contains myristicin — can cause tremors and seizures in large amounts."));

        FOODS.add(new FoodSafetyItem("Cucumber", "🥒",
                "Safe", "Low-calorie, hydrating, safe raw.",
                "Safe", "Low-calorie, hydrating, safe raw."));

        FOODS.add(new FoodSafetyItem("Celery", "🥬",
                "Safe", "Crunchy low-calorie snack in small pieces.",
                "Caution", "Not toxic, but little nutritional benefit for cats."));

        FOODS.add(new FoodSafetyItem("Turkey (cooked, plain)", "🦃",
                "Safe", "Lean protein, avoid skin and seasoning.",
                "Safe", "Lean protein, avoid skin and seasoning."));

        FOODS.add(new FoodSafetyItem("Beef (cooked, lean)", "🥩",
                "Safe", "Good protein source when lean and unseasoned.",
                "Safe", "Good protein source when lean and unseasoned."));

        FOODS.add(new FoodSafetyItem("Shrimp (cooked)", "🍤",
                "Safe", "Low-fat protein in small amounts, remove shell.",
                "Safe", "Low-fat protein in small amounts, remove shell."));

        FOODS.add(new FoodSafetyItem("Tuna (canned, no salt)", "🐟",
                "Caution", "OK occasionally; regular feeding can cause mercury buildup.",
                "Caution", "Cats can develop a strong preference — feed only occasionally."));

        FOODS.add(new FoodSafetyItem("Bacon", "🥓",
                "Caution", "High fat/salt — can trigger pancreatitis if fed often.",
                "Caution", "High fat/salt — best avoided regularly."));

        FOODS.add(new FoodSafetyItem("Peas", "🟢",
                "Safe", "Safe in small amounts, fresh or frozen.",
                "Safe", "Safe in small amounts, fresh or frozen."));

        FOODS.add(new FoodSafetyItem("Green Beans", "🫛",
                "Safe", "Low-calorie, good for weight management.",
                "Caution", "Not toxic, limited nutritional need for cats."));

        FOODS.add(new FoodSafetyItem("Cauliflower", "🥦",
                "Safe", "Safe cooked or raw in small amounts.",
                "Caution", "Not toxic, but no real nutritional value for cats."));

        FOODS.add(new FoodSafetyItem("Strawberries", "🍓",
                "Safe", "Safe in moderation, good source of vitamin C.",
                "Caution", "Not toxic, but cats gain little benefit from fruit."));

        FOODS.add(new FoodSafetyItem("Banana", "🍌",
                "Safe", "High in potassium — feed in moderation due to sugar content.",
                "Caution", "Not toxic, but high sugar — only tiny amounts."));

        FOODS.add(new FoodSafetyItem("Mango (no pit)", "🥭",
                "Safe", "Sweet treat in small amounts, remove pit and skin.",
                "Caution", "High sugar — only occasional tiny amounts."));

        FOODS.add(new FoodSafetyItem("Pineapple", "🍍",
                "Safe", "Safe in small amounts, remove tough skin/core.",
                "Caution", "High sugar for a carnivore diet — minimal amounts only."));

        FOODS.add(new FoodSafetyItem("Ice Cream", "🍦",
                "Unsafe", "High sugar and dairy — can cause GI upset, avoid.",
                "Unsafe", "High sugar and dairy — can cause GI upset, avoid."));

        FOODS.add(new FoodSafetyItem("Raw Meat", "🥩",
                "Caution", "Risk of bacterial contamination — cook thoroughly unless following a vet-guided raw diet.",
                "Caution", "Risk of bacterial contamination — cook thoroughly unless following a vet-guided raw diet."));

        FOODS.add(new FoodSafetyItem("Raw Eggs", "🥚",
                "Unsafe", "Risk of salmonella and biotin deficiency — always cook fully.",
                "Unsafe", "Risk of salmonella and biotin deficiency — always cook fully."));

        FOODS.add(new FoodSafetyItem("Raw Fish", "🐟",
                "Unsafe", "Can contain parasites and destroy thiamine — cook before feeding.",
                "Unsafe", "Can contain parasites and destroy thiamine — cook before feeding."));

        FOODS.add(new FoodSafetyItem("Popcorn (buttered/salted)", "🍿",
                "Unsafe", "High fat and salt — plain, air-popped only.", "Unsafe",
                "High fat and salt — plain, air-popped only."));

        FOODS.add(new FoodSafetyItem("Chips / Salty Snacks", "🍟",
                "Unsafe", "High salt and fat content — not appropriate as pet food.",
                "Unsafe", "High salt and fat content — not appropriate as pet food."));

        FOODS.add(new FoodSafetyItem("Fried Foods", "🍤",
                "Unsafe", "High fat can trigger pancreatitis — avoid feeding regularly.",
                "Unsafe", "High fat can trigger pancreatitis — avoid feeding regularly."));

        FOODS.add(new FoodSafetyItem("Cherries (no pit)", "🍒",
                "Caution", "Flesh is fine in tiny amounts; pits contain cyanide compounds — always remove.",
                "Caution", "Flesh is fine in tiny amounts; pits contain cyanide compounds — always remove."));

        FOODS.add(new FoodSafetyItem("Peach (no pit)", "🍑",
                "Caution", "Flesh is fine occasionally; pit is a choking hazard and mildly toxic.",
                "Caution", "Flesh is fine occasionally; pit is a choking hazard and mildly toxic."));

        FOODS.add(new FoodSafetyItem("Mustard", "🟡",
                "Caution", "Not toxic in tiny amounts but offers no benefit — best avoided.",
                "Caution", "Not toxic in tiny amounts but offers no benefit — best avoided."));

        FOODS.add(new FoodSafetyItem("Ketchup", "🍅",
                "Caution", "Often contains onion/garlic powder and sugar — check ingredients.",
                "Caution", "Often contains onion/garlic powder and sugar — check ingredients."));

        FOODS.add(new FoodSafetyItem("Soy Sauce", "🟤",
                "Unsafe", "Extremely high sodium — avoid entirely.", "Unsafe",
                "Extremely high sodium — avoid entirely."));
    }

    public static List<FoodSafetyItem> search(String query) {
        List<FoodSafetyItem> results = new ArrayList<>();
        String lower = query.trim().toLowerCase();
        if (lower.isEmpty()) return results;

        for (FoodSafetyItem item : FOODS) {
            if (item.name.toLowerCase().contains(lower)) {
                results.add(item);
            }
        }
        return results;
    }

    public static FoodSafetyItem findExact(String name) {
        for (FoodSafetyItem item : FOODS) {
            if (item.name.equalsIgnoreCase(name)) return item;
        }
        return null;
    }
}