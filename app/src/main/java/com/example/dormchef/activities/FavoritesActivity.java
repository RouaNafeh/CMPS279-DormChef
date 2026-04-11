package com.example.dormchef.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.dormchef.R;
import com.example.dormchef.adapters.RecipeAdapter;
import com.example.dormchef.databinding.ActivityFavoritesBinding;
import com.example.dormchef.models.Recipe;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

import database.DatabaseHelper;

public class FavoritesActivity extends AppCompatActivity {

    private ActivityFavoritesBinding binding;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityFavoritesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = new DatabaseHelper(this);

        List<Recipe> favList = db.getFavouriteRecipes();

        RecipeAdapter adapter = new RecipeAdapter(favList);
        binding.recyclerFavorites.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerFavorites.setAdapter(adapter);

        BottomNavigationView bottomNavigation = binding.bottomNavigation.bottomNavigation;
        bottomNavigation.setSelectedItemId(R.id.nav_favorites);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if(id == R.id.nav_favorites){
                return true;
            }
            else if(id==R.id.nav_home){
                startActivity(new Intent(FavoritesActivity.this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            else if(id == R.id.nav_filter){
                startActivity(new Intent(FavoritesActivity.this, FilterActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }
}