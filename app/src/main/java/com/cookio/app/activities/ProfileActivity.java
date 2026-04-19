package com.cookio.app.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cookio.app.R;
import com.cookio.app.adapters.RecipeAdapter;
import com.cookio.app.models.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    TextView tvUsername, tvEmpty;
    RecyclerView rvMyRecipes;
    Button btnEdit;

    FirebaseAuth auth;
    FirebaseFirestore db;

    RecipeAdapter adapter;
    List<Recipe> recipeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvUsername = findViewById(R.id.tvUsername);
        rvMyRecipes = findViewById(R.id.rvMyRecipes);
        btnEdit = findViewById(R.id.btnEdit);
        tvEmpty = findViewById(R.id.tvEmpty);

        rvMyRecipes.setLayoutManager(new LinearLayoutManager(this));
        rvMyRecipes.setNestedScrollingEnabled(false);

        recipeList = new ArrayList<>();
        adapter = new RecipeAdapter(recipeList, false);
        rvMyRecipes.setAdapter(adapter);

        loadUserInfo();
        loadMyRecipes();

        btnEdit.setOnClickListener(v -> showEditDialog());
    }

    private void loadUserInfo() {

        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(query -> {

                    if (!query.isEmpty()) {
                        DocumentSnapshot doc = query.getDocuments().get(0);
                        tvUsername.setText(doc.getString("username"));
                    }
                });
    }

    private void loadMyRecipes() {

        String uid = auth.getCurrentUser().getUid();

        db.collection("posts")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(query -> {

                    recipeList.clear();

                    for (DocumentSnapshot doc : query) {
                        Recipe recipe = doc.toObject(Recipe.class);
                        if (recipe != null) {
                            recipeList.add(recipe);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (recipeList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvMyRecipes.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvMyRecipes.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void showEditDialog() {

        EditText input = new EditText(this);

        new AlertDialog.Builder(this)
                .setTitle("Edit Username")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {

                    String newName = input.getText().toString().trim();

                    if (!TextUtils.isEmpty(newName)) {
                        updateUsername(newName);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateUsername(String newName) {

        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(query -> {

                    if (!query.isEmpty()) {
                        String docId = query.getDocuments().get(0).getId();

                        db.collection("users")
                                .document(docId)
                                .update("username", newName);

                        tvUsername.setText(newName);
                    }
                });
    }
}
