package com.example.dormchef.models;

import java.util.List;

public class Recipe {
    private int imageResId;
    private String name;
    private String time;
    private String budget;
    private List<String> tags;

    public Recipe(int imageResId, String name, String time, String budget, List<String> tags){
        this.imageResId = imageResId;
        this.name = name;
        this.time = time;
        this.budget = budget;
        this.tags = tags;
    }

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

    public List<String> getTags(){
        return tags;
    }
}
