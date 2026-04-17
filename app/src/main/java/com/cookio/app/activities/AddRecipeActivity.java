package com.cookio.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.cookio.app.databinding.ActivityAddRecipeBinding;

import database.DatabaseHelper;

public class AddRecipeActivity extends AppCompatActivity {

    private ActivityAddRecipeBinding binding;
    private DatabaseHelper dbHelper;
    private String selectedImageUri = "";

    private final ActivityResultLauncher<String[]> photoPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) {
                    return;
                }
                getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
                selectedImageUri = uri.toString();
                binding.ivSelectedPhoto.setImageURI(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddRecipeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);

        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.btnPickPhoto.setOnClickListener(v -> photoPicker.launch(new String[]{"image/*"}));
        binding.btnAddRecipe.setOnClickListener(v -> saveRecipe());
    }

    private void saveRecipe() {
        String name = normalizeSingleLineInput(binding.etRecipeName.getText().toString());
        String time = normalizeSingleLineInput(binding.etRecipeTime.getText().toString());
        String budget = normalizeSingleLineInput(binding.etRecipeBudget.getText().toString());
        String equipment = normalizeSingleLineInput(binding.etRecipeEquipment.getText().toString());
        String ingredients = normalizeMultilineInput(binding.etRecipeIngredients.getText().toString());
        String steps = normalizeMultilineInput(binding.etRecipeSteps.getText().toString());

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(time) || TextUtils.isEmpty(budget)
                || TextUtils.isEmpty(equipment) || TextUtils.isEmpty(ingredients)
                || TextUtils.isEmpty(steps)) {
            Toast.makeText(this, "Fill in every field before saving the recipe.", Toast.LENGTH_SHORT).show();
            return;
        }

        String formattedTime = time.matches("\\d+") ? time + " min" : time;
        long result = dbHelper.insertUserRecipe(
                name, formattedTime, budget, equipment, ingredients, steps, selectedImageUri
        );
        if (result == -1) {
            Toast.makeText(this, "Could not save the recipe.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Recipe added to My Recipes.", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String normalizeSingleLineInput(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeMultilineInput(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder normalized = new StringBuilder();
        String[] lines = value.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim().replaceAll("\\s+", " ");
            if (trimmed.isEmpty()) {
                continue;
            }
            if (normalized.length() > 0) {
                normalized.append('\n');
            }
            normalized.append(trimmed);
        }
        return normalized.toString();
    }
}
