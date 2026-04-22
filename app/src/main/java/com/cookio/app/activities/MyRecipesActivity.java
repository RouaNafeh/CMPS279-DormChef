package com.cookio.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cookio.app.R;
import com.cookio.app.adapters.RecipeAdapter;
import com.cookio.app.databinding.ActivityMyRecipesBinding;
import com.cookio.app.models.Recipe;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import database.DatabaseHelper;

public class MyRecipesActivity extends AppCompatActivity {

    private ActivityMyRecipesBinding binding;
    private DatabaseHelper dbHelper;
    private RecipeAdapter recipeAdapter;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(MyRecipesActivity.this, LandingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        binding = ActivityMyRecipesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);
        binding.recyclerMyRecipes.setLayoutManager(new LinearLayoutManager(this));
        recipeAdapter = new RecipeAdapter(dbHelper.getUserRecipes(), false);
        binding.recyclerMyRecipes.setAdapter(recipeAdapter);

        binding.btnOpenAddRecipe.setOnClickListener(v ->
                startActivity(new Intent(this, AddRecipeActivity.class)));

        BottomNavigationView bottomNavigation = binding.bottomNavigation.bottomNavigation;
        bottomNavigation.setSelectedItemId(R.id.nav_my_recipes);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_my_recipes) {
                return true;
            } else if (id == R.id.nav_home) {
                startActivity(new Intent(this, FeedActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_favorites) {
                startActivity(new Intent(this, FavoritesActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshRecipes();
    }

    private void refreshRecipes() {
        List<Recipe> userRecipes = dbHelper.getUserRecipes();
        recipeAdapter.updateData(userRecipes);
        boolean isEmpty = userRecipes.isEmpty();
        binding.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.recyclerMyRecipes.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}
