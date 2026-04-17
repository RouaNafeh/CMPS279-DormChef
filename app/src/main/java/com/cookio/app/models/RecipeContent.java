package com.cookio.app.models;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecipeContent {

    private RecipeContent() {}

    public static Details getDetails(String recipeName) {
        Map<String, Details> detailsMap = new LinkedHashMap<>();
        detailsMap.put("Greek Salad", new Details(
                Arrays.asList("Tomatoes", "Cucumber", "Feta", "Olives", "Olive oil", "Oregano"),
                Arrays.asList(
                        "Wash and chop the tomatoes and cucumber into bite-sized pieces.",
                        "Add the vegetables to a bowl with olives and crumbled feta.",
                        "Drizzle with olive oil and season with oregano, salt, and pepper.",
                        "Toss lightly and serve fresh."
                )
        ));
        detailsMap.put("Easy Pasta", new Details(
                Arrays.asList("Pasta", "Garlic", "Olive oil", "Parmesan", "Salt", "Pepper"),
                Arrays.asList(
                        "Boil the pasta in salted water until tender, then drain.",
                        "Warm olive oil in a pot and cook the garlic for 30 seconds.",
                        "Return the pasta to the pot and toss with the garlic oil.",
                        "Season with salt and pepper, then finish with parmesan."
                )
        ));
        detailsMap.put("Dorm Sandwich", new Details(
                Arrays.asList("Bread", "Turkey", "Cheese", "Lettuce", "Tomato", "Mayo"),
                Arrays.asList(
                        "Lay out the bread slices and spread mayo on one side.",
                        "Layer turkey, cheese, lettuce, and tomato on the bread.",
                        "Close the sandwich and press lightly.",
                        "Slice in half and serve."
                )
        ));
        detailsMap.put("Mug Cake", new Details(
                Arrays.asList("Flour", "Sugar", "Cocoa powder", "Milk", "Oil", "Baking powder"),
                Arrays.asList(
                        "Mix all ingredients in a microwave-safe mug until smooth.",
                        "Microwave for 60 to 90 seconds until the cake rises.",
                        "Let it rest for a minute before eating.",
                        "Top with chocolate chips or powdered sugar if available."
                )
        ));
        detailsMap.put("Omelette", new Details(
                Arrays.asList("Eggs", "Milk", "Butter", "Cheese", "Salt", "Pepper"),
                Arrays.asList(
                        "Whisk the eggs with a splash of milk, salt, and pepper.",
                        "Heat butter in a pan over medium heat.",
                        "Pour in the eggs and gently pull the edges inward as they set.",
                        "Add cheese, fold the omelette, and cook for one more minute."
                )
        ));

        Details details = detailsMap.get(recipeName);
        if (details != null) {
            return details;
        }

        return new Details(
                Arrays.asList("Ingredients coming soon"),
                Arrays.asList("Recipe steps are not available yet.")
        );
    }

    public static final class Details {
        private final List<String> ingredients;
        private final List<String> steps;

        public Details(List<String> ingredients, List<String> steps) {
            this.ingredients = ingredients;
            this.steps = steps;
        }

        public List<String> getIngredients() {
            return ingredients;
        }

        public List<String> getSteps() {
            return steps;
        }
    }
}
