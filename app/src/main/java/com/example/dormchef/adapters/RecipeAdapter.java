package com.example.dormchef.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dormchef.databinding.ItemRecipeBinding;
import com.example.dormchef.models.Recipe;

import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {
    private List<Recipe> recipeList;

    public RecipeAdapter(List<Recipe> recipeList){
        this.recipeList = recipeList;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemRecipeBinding binding = ItemRecipeBinding.inflate(inflater, parent, false);
        return new RecipeViewHolder(binding);
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position){
        Recipe recipe = recipeList.get(position);
        
        holder.binding.foodImg.setImageResource(recipe.getImageResId());
        holder.binding.recipeName.setText(recipe.getName());
        holder.binding.recipeTime.setText(recipe.getTime());
        holder.binding.recipeBudget.setText(recipe.getBudget());
        holder.binding.tag.setText(recipe.getEquipment());
    }

    @Override
    public int getItemCount(){
        return recipeList.size();
    }

    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        final ItemRecipeBinding binding;

        public RecipeViewHolder(ItemRecipeBinding binding){
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}