package com.example.dormchef.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dormchef.R;
import com.example.dormchef.databinding.ActivityRecipeDetailBinding;
import com.example.dormchef.models.RecipeContent;
import com.google.android.material.chip.Chip;

import java.util.List;

public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_NAME = "recipe_name";
    public static final String EXTRA_RECIPE_TIME = "recipe_time";
    public static final String EXTRA_RECIPE_BUDGET = "recipe_budget";
    public static final String EXTRA_RECIPE_EQUIPMENT = "recipe_equipment";
    public static final String EXTRA_RECIPE_IMAGE = "recipe_image";
    public static final String EXTRA_RECIPE_IMAGE_URI = "recipe_image_uri";
    public static final String EXTRA_RECIPE_INGREDIENTS = "recipe_ingredients";
    public static final String EXTRA_RECIPE_STEPS = "recipe_steps";

    private ActivityRecipeDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecipeDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        String recipeName = getIntent().getStringExtra(EXTRA_RECIPE_NAME);
        String recipeTime = getIntent().getStringExtra(EXTRA_RECIPE_TIME);
        String recipeBudget = getIntent().getStringExtra(EXTRA_RECIPE_BUDGET);
        String recipeEquipment = getIntent().getStringExtra(EXTRA_RECIPE_EQUIPMENT);
        int recipeImage = getIntent().getIntExtra(EXTRA_RECIPE_IMAGE, 0);
        String recipeImageUri = getIntent().getStringExtra(EXTRA_RECIPE_IMAGE_URI);
        String customIngredients = getIntent().getStringExtra(EXTRA_RECIPE_INGREDIENTS);
        String customSteps = getIntent().getStringExtra(EXTRA_RECIPE_STEPS);

        List<String> ingredients = splitMultiline(customIngredients);
        List<String> steps = splitMultiline(customSteps);
        RecipeContent.Details details = RecipeContent.getDetails(recipeName);

        if (!ingredients.isEmpty() || !steps.isEmpty()) {
            details = new RecipeContent.Details(
                    ingredients.isEmpty() ? details.getIngredients() : ingredients,
                    steps.isEmpty() ? details.getSteps() : steps
            );
        }

        binding.tvRecipeTitle.setText(recipeName);
        binding.tvRecipeTime.setText(recipeTime);
        binding.tvRecipeBudget.setText(recipeBudget);
        binding.tvRecipeEquipment.setText(recipeEquipment);
        binding.ivRecipeImage.setImageURI(null);
        if (recipeImageUri != null && !recipeImageUri.trim().isEmpty()) {
            binding.ivRecipeImage.setImageURI(Uri.parse(recipeImageUri));
        } else {
            binding.ivRecipeImage.setImageResource(recipeImage);
        }

        binding.btnCookingMode.setOnClickListener(v -> {
            Intent intent = new Intent(this, TimerActivity.class);
            intent.putExtra(TimerActivity.EXTRA_RECIPE_NAME, recipeName);
            intent.putExtra(TimerActivity.EXTRA_RECIPE_STEPS, customSteps);
            startActivity(intent);
        });

        populateIngredients(details.getIngredients());
        populateSteps(details.getSteps());
    }

    private void populateIngredients(List<String> ingredients) {
        binding.chipGroupIngredients.removeAllViews();
        for (String ingredient : ingredients) {
            Chip chip = new Chip(this);
            chip.setText(ingredient);
            chip.setClickable(false);
            chip.setCheckable(false);
            chip.setTextSize(13f);
            chip.setChipBackgroundColorResource(R.color.chip_bg_selector);
            chip.setTextColor(getColor(R.color.chip_text));
            binding.chipGroupIngredients.addView(chip);
        }
    }

    private void populateSteps(List<String> steps) {
        binding.stepsContainer.removeAllViews();
        for (int i = 0; i < steps.size(); i++) {
            TextView stepView = new TextView(this);
            stepView.setText((i + 1) + ". " + steps.get(i));
            stepView.setTextColor(getColor(R.color.textDark));
            stepView.setTextSize(15f);
            stepView.setLineSpacing(0f, 1.25f);
            int horizontalPadding = dpToPx(16);
            int verticalPadding = dpToPx(14);
            stepView.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
            stepView.setBackgroundResource(R.drawable.bg_equip_item);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = dpToPx(10);
            stepView.setLayoutParams(params);
            binding.stepsContainer.addView(stepView);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private List<String> splitMultiline(String rawValue) {
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return values;
        }
        String[] lines = rawValue.split("\\r?\\n|\\|");
        for (String line : lines) {
            String[] parts = line.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    values.add(trimmed);
                }
            }
        }
        return values;
    }

}
