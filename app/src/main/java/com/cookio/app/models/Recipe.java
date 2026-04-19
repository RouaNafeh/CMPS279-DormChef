package com.cookio.app.models;

import java.util.List;

public class Recipe {

    private int id;
    private int imageResId;
    private String name;
    private String time;
    private String budget;
    private String equipment;
    private String ingredients;
    private String steps;
    private String imageUri;
    private boolean isFavourite;

    private String userId;

    public Recipe() {}

    public Recipe(int id, int imageResId, String name, String time, String budget,
                  String equipment, String ingredients, String steps, String imageUri, boolean isFavourite){
        this.id = id;
        this.imageResId = imageResId;
        this.name = name;
        this.time = time;
        this.budget = budget;
        this.equipment = equipment;
        this.ingredients = ingredients;
        this.steps = steps;
        this.imageUri = imageUri;
        this.isFavourite = isFavourite;
    }

    public Recipe(int imageResId, String name, String time, String budget,
                  String equipment, String ingredients, String steps, String imageUri, boolean isFavourite){
        this.imageResId = imageResId;
        this.name = name;
        this.time = time;
        this.budget = budget;
        this.equipment = equipment;
        this.ingredients = ingredients;
        this.steps = steps;
        this.imageUri = imageUri;
        this.isFavourite = isFavourite;
    }

    public Recipe(int id, int imageResId, String name, String time, String budget,
                  String equipment, boolean isFavourite){
        this(id, imageResId, name, time, budget, equipment, "", "", "", isFavourite);
    }

    public Recipe(int imageResId, String name, String time, String budget,
                  String equipment, boolean isFavourite){
        this(imageResId, name, time, budget, equipment, "", "", "", isFavourite);
    }

    public int getId(){ return id; }

    public int getImageResId(){
        return imageResId;
    }

    public String getName(){
        return name;
    }

    public String getTime(){
        return time;
    }

    public String getBudget(){
        return budget;
    }

    public String getEquipment(){
        return equipment;
    }

    public String getIngredients() {
        return ingredients;
    }

    public String getSteps() {
        return steps;
    }

    public String getImageUri() {
        return imageUri;
    }

    public boolean isFavourite(){
        return isFavourite;
    }

    public void setFavourite(boolean favourite) {
        this.isFavourite = favourite;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId){
        this.userId = userId;
    }
}
