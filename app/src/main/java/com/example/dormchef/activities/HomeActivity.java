package com.example.dormchef.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.dormchef.R;
import com.example.dormchef.adapters.RecipeAdapter;
import com.example.dormchef.databinding.ActivityHomeBinding;
import com.example.dormchef.models.Recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import database.DatabaseHelper;

public class HomeActivity extends AppCompatActivity {
    private ActivityHomeBinding binding;
    private List<Recipe> recipeList;
    private RecipeAdapter recipeAdapter;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);
        if(dbHelper.isRecipesTableEmpty()){
            dbHelper.insertSampleRecipes();
        }

        recipeList = dbHelper.getAllRecipes();

        binding.filterBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, FilterActivity.class);
            startActivity(intent);
        });

        recipeAdapter = new RecipeAdapter(recipeList);
        
        binding.recyclerCards.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerCards.setHasFixedSize(true);
        binding.recyclerCards.setAdapter(recipeAdapter);
    }
}