package com.cookio.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cookio.app.R;
import com.cookio.app.adapters.RecipeAdapter;
import com.cookio.app.models.Recipe;

import java.util.ArrayList;
import java.util.List;

import database.DatabaseHelper;

public class RecipeListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private TextView tvNoResults;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_list);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        recyclerView = findViewById(R.id.recyclerViewRecipes);
        tvNoResults = findViewById(R.id.tvNoResults);
        dbHelper = new DatabaseHelper(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Intent intent = getIntent();

        ArrayList<String> ingredients = intent.getStringArrayListExtra("ingredients");
        ArrayList<String> equipment = intent.getStringArrayListExtra("equipment");
        int maxTime = intent.getIntExtra("maxTime", -1);
        String budget = intent.getStringExtra("budget");
        boolean includeUserRecipes = intent.getBooleanExtra("includeUserRecipes", true);
        boolean sortByRating = intent.getBooleanExtra("sortByRating", false);

        List<Recipe> results = dbHelper.getFilteredRecipes(
                ingredients != null ? ingredients : new ArrayList<>(),
                equipment != null ? equipment : new ArrayList<>(),
                maxTime,
                budget != null ? budget : "",
                includeUserRecipes
        );

        if (sortByRating) {
            results.sort((r1, r2) -> {
                int ratingCompare = Float.compare(r2.getAvgRating(), r1.getAvgRating());

                if (ratingCompare != 0) {
                    return ratingCompare;
                }

                return Integer.compare(r2.getReviewsCount(), r1.getReviewsCount());
            });
        }

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