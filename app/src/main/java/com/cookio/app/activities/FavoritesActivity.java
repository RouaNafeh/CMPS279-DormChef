package com.cookio.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cookio.app.R;
import com.cookio.app.adapters.RecipeAdapter;
import com.cookio.app.databinding.ActivityFavoritesBinding;
import com.cookio.app.models.Recipe;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import database.DatabaseHelper;

public class FavoritesActivity extends AppCompatActivity {

    private ActivityFavoritesBinding binding;
    private DatabaseHelper db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(FavoritesActivity.this, LandingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        binding = ActivityFavoritesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = new DatabaseHelper(this);

        List<Recipe> favList = db.getFavouriteRecipes();
        int listSize = favList.size();
        binding.favCount.setText(listSize + (listSize==1 ?" recipe" : " recipes"));
        binding.favCount.setScaleX(0.8f);
        binding.favCount.setScaleY(0.8f);
        binding.favCount.animate().scaleX(1f).scaleY(1f).setDuration(200);

        if (favList.isEmpty()) {
            binding.emptyState.setAlpha(0f);
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.emptyState.animate().alpha(1f).setDuration(300);

            binding.recyclerFavorites.setVisibility(View.GONE);
        } else {
            binding.recyclerFavorites.setAlpha(0f);
            binding.recyclerFavorites.setVisibility(View.VISIBLE);
            binding.recyclerFavorites.animate().alpha(1f).setDuration(300);

            binding.emptyState.setVisibility(View.GONE);
        }


        RecipeAdapter adapter = new RecipeAdapter(favList);
        binding.recyclerFavorites.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerFavorites.setAdapter(adapter);

        BottomNavigationView bottomNavigation = binding.bottomNavigation.bottomNavigation;
        bottomNavigation.setSelectedItemId(R.id.nav_saved);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if(id==R.id.nav_home){
                startActivity(new Intent(FavoritesActivity.this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            else if(id == R.id.nav_saved){
                startActivity(new Intent(FavoritesActivity.this, SavedPostsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            else if(id == R.id.nav_my_recipes){
                startActivity(new Intent(FavoritesActivity.this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }
}
