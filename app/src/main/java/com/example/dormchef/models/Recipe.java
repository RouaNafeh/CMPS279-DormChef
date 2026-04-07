package com.example.dormchef.models;

import java.util.List;

public class Recipe {
    private int id;
    private int imageResId;
    private String name;
    private String time;
    private String budget;
    private String equipment;
    private boolean isFavourite;

    public Recipe(int id, int imageResId, String name, String time, String budget, String equipment, boolean isFavourite){
        this.id = id;
        this.imageResId = imageResId;
        this.name = name;
        this.time = time;
        this.budget = budget;
        this.equipment = equipment;
        this.isFavourite = isFavourite;
    }

    public Recipe(int imageResId, String name, String time, String budget, String equipment, boolean isFavourite){
        this.imageResId = imageResId;
        this.name = name;
        this.time = time;
        this.budget = budget;
        this.equipment = equipment;
        this.isFavourite = isFavourite;
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

    public boolean isFavourite(){ return isFavourite; }
    public void setFavourite(boolean favourite) {
        this.isFavourite = favourite;
    }
}
