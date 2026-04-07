package com.example.dormchef;

public class Recipe {
    private final String title;
    private final String time;

    public Recipe(String title, String time) {
        this.title = title;
        this.time = time;
    }

    public String getTitle() {
        return title;
    }

    public String getTime() {
        return time;
    }
}