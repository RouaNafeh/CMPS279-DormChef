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

public class HomeActivity extends AppCompatActivity {
    private ActivityHomeBinding binding;
    private List<Recipe> recipeList;
    private RecipeAdapter recipeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.filterBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, FilterActivity.class);
            startActivity(intent);
        });

        recipeList = new ArrayList<>();
        recipeList.add(new Recipe(R.drawable.salad, "Greek Salad", "15 min", "Low", Arrays.asList("Vegetarian")));
        recipeList.add(new Recipe(R.drawable.sandwich, "Dorm Sandwich", "10 min", "Cheap", Arrays.asList("Quick")));
        recipeList.add(new Recipe(R.drawable.pasta, "Easy Pasta", "20 min", "Low", Arrays.asList("Pasta")));

        recipeAdapter = new RecipeAdapter(recipeList);
        
        binding.recyclerCards.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerCards.setHasFixedSize(true);
        binding.recyclerCards.setAdapter(recipeAdapter);
    }
}