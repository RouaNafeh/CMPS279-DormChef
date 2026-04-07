package com.example.dormchef.activities;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.dormchef.adapters.RecipeAdapter;
import com.example.dormchef.databinding.ActivityFavoritesBinding;
import com.example.dormchef.models.Recipe;

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
    }
}
