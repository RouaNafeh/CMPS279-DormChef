package com.example.dormchef;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dormchef.adapters.RecipeAdapter;
import com.example.dormchef.activities.FilterActivity;
import com.example.dormchef.models.Recipe;

import database.DatabaseHelper;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recipesRecyclerView;
    private RecipeAdapter recipeAdapter;
    private AppCompatButton filterAll;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        if (dbHelper.isRecipesTableEmpty()) {
            dbHelper.insertSampleRecipes();
        }

        List<Recipe> recipeList = dbHelper.getAllRecipes();

        recipesRecyclerView = findViewById(R.id.recipesRecyclerView);
        recipesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recipeAdapter = new RecipeAdapter(recipeList);
        recipesRecyclerView.setAdapter(recipeAdapter);

        filterAll = findViewById(R.id.filterAll);
        filterAll.setSelected(true);
        filterAll.setOnClickListener(v -> {
            Intent intent = new Intent(this, FilterActivity.class);
            startActivity(intent);
        });
    }
}