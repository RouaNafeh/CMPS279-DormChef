package com.cookio.app.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.appcompat.widget.SearchView;

import android.widget.Button;
import android.widget.Toast;

import com.cookio.app.R;
import com.cookio.app.adapters.RecipeAdapter;
import com.cookio.app.databinding.ActivityHomeBinding;
import com.cookio.app.models.Recipe;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import database.DatabaseHelper;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private DatabaseHelper dbHelper;

    private List<Recipe> recipeList;
    private List<Recipe> filteredList;

    private RecipeAdapter recipeAdapter;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(HomeActivity.this, LandingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Button btnLogout = findViewById(R.id.btn_logout);

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(HomeActivity.this, LandingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

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
                filterRecipes(query);
                return true;
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
            Intent intent = new Intent(this, CreatePostActivity.class);
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

        if (text == null || text.trim().isEmpty()) {
            filteredList.addAll(recipeList);
        } else {

            String query = text.toLowerCase().trim();

            for (Recipe recipe : recipeList) {

                if (recipe.getName() != null &&
                        recipe.getName().toLowerCase().contains(query)) {

                    filteredList.add(recipe);
                }
            }
        }

        recipeAdapter.updateData(filteredList);

        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No recipes found", Toast.LENGTH_SHORT).show();
        }
    }
}
