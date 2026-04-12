package com.example.dormchef.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.dormchef.databinding.ActivityAddRecipeBinding;

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
        String name = binding.etRecipeName.getText().toString().trim();
        String time = binding.etRecipeTime.getText().toString().trim();
        String budget = binding.etRecipeBudget.getText().toString().trim();
        String equipment = binding.etRecipeEquipment.getText().toString().trim();
        String ingredients = binding.etRecipeIngredients.getText().toString().trim();
        String steps = binding.etRecipeSteps.getText().toString().trim();

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
}
