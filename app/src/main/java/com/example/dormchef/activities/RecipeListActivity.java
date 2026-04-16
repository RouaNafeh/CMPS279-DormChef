package com.example.dormchef.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dormchef.R;
import com.example.dormchef.models.Recipe;
import com.example.dormchef.adapters.RecipeAdapter;

import database.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class RecipeListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private TextView tvNoResults;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_list);

        recyclerView = findViewById(R.id.recyclerViewRecipes);
        tvNoResults  = findViewById(R.id.tvNoResults);
        dbHelper     = new DatabaseHelper(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Intent intent = getIntent();
        ArrayList<String> ingredients = intent.getStringArrayListExtra("ingredients");
        ArrayList<String> equipment = intent.getStringArrayListExtra("equipment");
        int maxTime    = intent.getIntExtra("maxTime", -1);
        String budget  = intent.getStringExtra("budget");
        boolean includeUserRecipes         = getIntent().getBooleanExtra("includeUserRecipes", true);


        List<Recipe> results = dbHelper.getFilteredRecipes(
                ingredients != null ? ingredients : new ArrayList<>(),
                equipment != null ? equipment : new ArrayList<>(),
                maxTime,
                budget != null ? budget : "",
                includeUserRecipes
        );

        if (results.isEmpty()) {
            tvNoResults.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvNoResults.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter = new RecipeAdapter(results);
            recyclerView.setAdapter(adapter);
        }
    }
}