package com.example.dormchef.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.appcompat.widget.SearchView;
import android.widget.Toast;

import com.example.dormchef.R;
import com.example.dormchef.adapters.RecipeAdapter;
import com.example.dormchef.databinding.ActivityHomeBinding;
import com.example.dormchef.models.Recipe;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import database.DatabaseHelper;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private DatabaseHelper dbHelper;

    private List<Recipe> recipeList;
    private List<Recipe> filteredList;

    private RecipeAdapter recipeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);
        if (dbHelper.isRecipesTableEmpty()) {
            dbHelper.insertSampleRecipes();
        }

        recipeList = dbHelper.getAllRecipes();
        filteredList = new ArrayList<>(recipeList);

        recipeAdapter = new RecipeAdapter(filteredList);

        binding.recyclerCards.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerCards.setHasFixedSize(true);
        binding.recyclerCards.setAdapter(recipeAdapter);

        binding.searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterRecipes(newText);
                return true;
            }
        });

        binding.filterBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, FilterActivity.class);
            startActivity(intent);
        });

        binding.btnQuickAddRecipe.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddRecipeActivity.class);
            startActivity(intent);
        });

        BottomNavigationView bottomNavigation = binding.bottomNavigation.bottomNavigation;
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_favorites) {
                startActivity(new Intent(HomeActivity.this, FavoritesActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_my_recipes) {
                startActivity(new Intent(HomeActivity.this, MyRecipesActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void filterRecipes(String text) {
        filteredList.clear();

        for (Recipe recipe : recipeList) {
            if (recipe.getName().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(recipe);
            }
        }

        recipeAdapter.notifyDataSetChanged();

        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No recipes found", Toast.LENGTH_SHORT).show();
        }
    }
}
