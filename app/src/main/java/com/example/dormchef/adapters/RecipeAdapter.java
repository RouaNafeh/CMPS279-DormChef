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
        holder.binding.tag.setText(formatEquipment(recipe.getTags()));
    }

    @Override
    public int getItemCount(){
        return recipeList.size();
    }

    private String formatEquipment(List<String> tags){
        if(tags==null || tags.isEmpty()){
            return "None";
        }

        if(tags.size()==1){
            return tags.get(0);
        }

        if(tags.size()==2){
            return tags.get(0) + ", " + tags.get(1);
        }

        return tags.get(0) + ", " + tags.get(1) + "...";
    }
    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        final ItemRecipeBinding binding;

        public RecipeViewHolder(ItemRecipeBinding binding){
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}