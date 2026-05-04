package com.cookio.app.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class RecipeCategoryHelperTest {

    @Test
    public void sanitizeCategories_deduplicatesNormalizesAndCapsSelection() {
        assertEquals(
                Arrays.asList("Dinner", "Pasta", "Salad"),
                RecipeCategoryHelper.sanitizeCategories(Arrays.asList(
                        " dinner ",
                        "Pasta",
                        "SALAD",
                        "Dinner",
                        "NotReal"
                ))
        );
    }

    @Test
    public void matchesCategory_returnsTrueOnlyForApprovedMatches() {
        assertTrue(RecipeCategoryHelper.matchesCategory(
                Arrays.asList("Lunch", "Sandwiches"),
                "lunch"
        ));

        assertTrue(!RecipeCategoryHelper.matchesCategory(
                Collections.singletonList("Dessert"),
                "Breakfast"
        ));
    }
}
