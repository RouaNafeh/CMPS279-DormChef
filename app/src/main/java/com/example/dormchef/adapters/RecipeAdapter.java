package com.example.dormchef.adapters;

import com.example.dormchef.R;
import com.example.dormchef.models.Recipe;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private List<Recipe> recipeList;

    public RecipeAdapter(List<Recipe> recipeList) {
        this.recipeList = recipeList;
    }

    public void updateData(List<Recipe> newList) {
        this.recipeList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recipe_item, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipeList.get(position);
        holder.titleText.setText(recipe.getName());
        holder.subText.setText(recipe.getTime());

        if (holder.recipeImage != null) {
            holder.recipeImage.setImageResource(recipe.getImageResId());
        }
    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, subText;
        ImageView recipeImage;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText   = itemView.findViewById(R.id.titleText);
            subText     = itemView.findViewById(R.id.subText);

        }
    }
}