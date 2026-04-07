package com.example.dormchef;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recipesRecyclerView;
    private RecipeAdapter recipeAdapter;
    private List<Recipe> recipeList;
    private AppCompatButton filterAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recipesRecyclerView = findViewById(R.id.recipesRecyclerView);
        recipesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        recipeList = new ArrayList<>();
        recipeList.add(new Recipe("Chicken Pasta", "Under 30 mins"));
        recipeList.add(new Recipe("Grilled Cheese Sandwich", "Under 15 mins"));
        recipeList.add(new Recipe("Beef Burger", "Under 20 mins"));
        recipeList.add(new Recipe("Veggie Omelette", "Under 10 mins"));
        recipeList.add(new Recipe("Homemade Pizza", "Under 60 mins"));

        recipeAdapter = new RecipeAdapter(recipeList);
        recipesRecyclerView.setAdapter(recipeAdapter);

        filterAll = findViewById(R.id.filterAll);
        filterAll.setSelected(true);
    }
}