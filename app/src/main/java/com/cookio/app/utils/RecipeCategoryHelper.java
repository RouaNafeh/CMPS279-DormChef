package com.cookio.app.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class RecipeCategoryHelper {
    public static final int MAX_CATEGORIES_PER_RECIPE = 3;

    private static final List<String> ALLOWED_CATEGORIES = Collections.unmodifiableList(Arrays.asList(
            "Breakfast",
            "Lunch",
            "Dinner",
            "Snack",
            "Dessert",
            "Drinks",
            "Pasta",
            "Salad",
            "Soup",
            "Sandwiches"
    ));

    private RecipeCategoryHelper() {}

    public static List<String> getAllowedCategories() {
        return ALLOWED_CATEGORIES;
    }

    public static String normalizeCategory(String rawValue) {
        if (rawValue == null) {
            return "";
        }

        String normalized = rawValue.trim();
        if (normalized.isEmpty()) {
            return "";
        }

        for (String allowedCategory : ALLOWED_CATEGORIES) {
            if (allowedCategory.equalsIgnoreCase(normalized)) {
                return allowedCategory;
            }
        }

        return "";
    }

    public static List<String> sanitizeCategories(List<String> rawCategories) {
        if (rawCategories == null || rawCategories.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> uniqueCategories = new LinkedHashSet<>();
        for (String rawCategory : rawCategories) {
            String normalized = normalizeCategory(rawCategory);
            if (!normalized.isEmpty()) {
                uniqueCategories.add(normalized);
            }
            if (uniqueCategories.size() >= MAX_CATEGORIES_PER_RECIPE) {
                break;
            }
        }

        return new ArrayList<>(uniqueCategories);
    }

    public static boolean matchesCategory(List<String> postCategories, String selectedCategory) {
        String normalizedSelected = normalizeCategory(selectedCategory);
        if (normalizedSelected.isEmpty()) {
            return true;
        }

        for (String category : sanitizeCategories(postCategories)) {
            if (normalizedSelected.equals(category)) {
                return true;
            }
        }

        return false;
    }

    public static String describeAllowedCategories() {
        return String.join(", ", ALLOWED_CATEGORIES);
    }

    public static String buildSearchableText(List<String> categories) {
        return String.join(" ", sanitizeCategories(categories)).toLowerCase(Locale.getDefault());
    }
}
