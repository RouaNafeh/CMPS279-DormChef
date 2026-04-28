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
                        "Wash and chop the tomatoes and cucumber into bite-sized pieces.|5",
                        "Add the vegetables to a bowl with olives and crumbled feta.|2",
                        "Drizzle with olive oil and season with oregano, salt, and pepper.|1",
                        "Toss lightly and serve fresh.|0"
                )
        ));
        detailsMap.put("Easy Pasta", new Details(
                Arrays.asList("Pasta", "Garlic", "Olive oil", "Parmesan", "Salt", "Pepper"),
                Arrays.asList(
                        "Boil the pasta in salted water until tender, then drain.|10",
                        "Warm olive oil in a pot and cook the garlic for 1 minute.|1",
                        "Return the pasta to the pot and toss with the garlic oil.|2",
                        "Season with salt and pepper, then finish with parmesan.|0"
                )
        ));
        detailsMap.put("Dorm Sandwich", new Details(
                Arrays.asList("Bread", "Turkey", "Cheese", "Lettuce", "Tomato", "Mayo"),
                Arrays.asList(
                        "Lay out the bread slices and spread mayo on one side.|1",
                        "Layer turkey, cheese, lettuce, and tomato on the bread.|2",
                        "Close the sandwich and press lightly.|0",
                        "Slice in half and serve.|0"
                )
        ));
        detailsMap.put("Mug Cake", new Details(
                Arrays.asList("Flour", "Sugar", "Cocoa powder", "Milk", "Oil", "Baking powder"),
                Arrays.asList(
                        "Mix all ingredients in a microwave-safe mug until smooth.|3",
                        "Microwave for 2 minutes until the cake rises.|2",
                        "Let it rest before eating.|1",
                        "Top with chocolate chips or powdered sugar if available.|0"
                )
        ));
        detailsMap.put("Omelette", new Details(
                Arrays.asList("Eggs", "Milk", "Butter", "Cheese", "Salt", "Pepper"),
                Arrays.asList(
                        "Whisk the eggs with a splash of milk, salt, and pepper.|2",
                        "Heat butter in a pan over medium heat.|1",
                        "Pour in the eggs and gently pull the edges inward as they set.|3",
                        "Add cheese, fold the omelette, and cook for one more minute.|1"
                )
        ));

        Details details = detailsMap.get(recipeName);
        if (details != null) {
            return details;
        }

        return new Details(
                Arrays.asList("Ingredients coming soon"),
                Arrays.asList("Recipe steps are not available yet.|0")
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
