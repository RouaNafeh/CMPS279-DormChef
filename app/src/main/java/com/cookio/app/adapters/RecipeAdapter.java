package com.cookio.app.adapters;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cookio.app.R;
import com.cookio.app.activities.RecipeDetailActivity;
import com.cookio.app.models.Recipe;

import java.util.List;

import database.DatabaseHelper;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private List<Recipe> recipeList;
    private final boolean showFavouriteButton;

    public RecipeAdapter(List<Recipe> recipeList) {
        this(recipeList, true);
    }

    public RecipeAdapter(List<Recipe> recipeList, boolean showFavouriteButton) {
        this.recipeList = recipeList;
        this.showFavouriteButton = showFavouriteButton;
    }

    public void updateData(List<Recipe> newList) {
        this.recipeList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipeList.get(position);
        holder.recipeName.setText(recipe.getName());
        holder.recipeTime.setText(recipe.getTime());
        holder.recipeBudget.setText(recipe.getBudget());
        holder.recipeTag.setText(recipe.getEquipment());
        holder.recipeImage.setImageURI(null);
        if (recipe.getImageUri() != null && !recipe.getImageUri().trim().isEmpty()) {
            holder.recipeImage.setImageURI(Uri.parse(recipe.getImageUri()));
        } else {
            holder.recipeImage.setImageResource(recipe.getImageResId());
        }
        holder.heartButton.setImageResource(
                recipe.isFavourite() ? R.drawable.heart_filled : R.drawable.heart
        );
        holder.heartButton.setVisibility(showFavouriteButton ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), RecipeDetailActivity.class);
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_NAME, recipe.getName());
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_TIME, recipe.getTime());
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_BUDGET, recipe.getBudget());
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_EQUIPMENT, recipe.getEquipment());
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_IMAGE, recipe.getImageResId());
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_IMAGE_URI, recipe.getImageUri());
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_INGREDIENTS, recipe.getIngredients());
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_STEPS, recipe.getSteps());
            v.getContext().startActivity(intent);
        });

        holder.heartButton.setOnClickListener(v -> {
            boolean newState = !recipe.isFavourite();
            recipe.setFavourite(newState);

            DatabaseHelper db = new DatabaseHelper(v.getContext());
            db.updateFavourite(recipe.getId(), newState);

            v.animate()
                    .scaleX(1.3f)
                    .scaleY(1.3f)
                    .rotation(15f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .rotation(0f)
                                .setDuration(150);
                    });

            holder.heartButton.setImageResource(
                    newState ? R.drawable.heart_filled : R.drawable.heart
            );
        });

        if (!showFavouriteButton) {
            holder.heartButton.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        TextView recipeName, recipeTime, recipeBudget, recipeTag;
        ImageView recipeImage;
        ImageButton heartButton;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeName = itemView.findViewById(R.id.recipe_name);
            recipeTime = itemView.findViewById(R.id.recipe_time);
            recipeBudget = itemView.findViewById(R.id.recipe_budget);
            recipeTag = itemView.findViewById(R.id.tag);
            recipeImage = itemView.findViewById(R.id.food_img);
            heartButton = itemView.findViewById(R.id.heart);
        }
    }
}
