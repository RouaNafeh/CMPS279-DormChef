package com.cookio.app.models;

public class Recipe {

    private int id;
    private int imageResId;
    private boolean favourite;

    private String recipeId;
    private String authorId;
    private String authorName;
    private String title;
    private String description;
    private String time;
    private String budget;
    private String equipment;
    private String ingredients;
    private String steps;
    private String imageUrl;
    private long createdAt;
    private float avgRating;
    private int reviewsCount;

    public Recipe() {
    }

    public Recipe(String recipeId, String authorId, String authorName, String title,
                  String description, String time, String budget, String equipment,
                  String ingredients, String steps, String imageUrl, long createdAt) {
        this.recipeId = recipeId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.title = title;
        this.description = description;
        this.time = time;
        this.budget = budget;
        this.equipment = equipment;
        this.ingredients = ingredients;
        this.steps = steps;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }

    public Recipe(int imageResId, String name, String time, String budget, String equipment,
                  String ingredients, String steps, String imageUri, boolean favourite) {
        this(0, imageResId, name, time, budget, equipment, ingredients, steps, imageUri, favourite);
    }

    public Recipe(int id, int imageResId, String name, String time, String budget, String equipment,
                  String ingredients, String steps, String imageUri, boolean favourite) {
        this.id = id;
        this.imageResId = imageResId;
        this.title = name;
        this.time = time;
        this.budget = budget;
        this.equipment = equipment;
        this.ingredients = ingredients;
        this.steps = steps;
        this.imageUrl = imageUri;
        this.favourite = favourite;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getImageResId() {
        return imageResId;
    }

    public void setImageResId(int imageResId) {
        this.imageResId = imageResId;
    }

    public boolean isFavourite() {
        return favourite;
    }

    public void setFavourite(boolean favourite) {
        this.favourite = favourite;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return title;
    }

    public void setName(String name) {
        this.title = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getBudget() {
        return budget;
    }

    public void setBudget(String budget) {
        this.budget = budget;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageUri() {
        return imageUrl;
    }

    public void setImageUri(String imageUri) {
        this.imageUrl = imageUri;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    public float getAvgRating() { return avgRating; }
    public int getReviewsCount() { return reviewsCount; }

    public void setAvgRating(float avgRating) { this.avgRating = avgRating; }
    public void setReviewsCount(int reviewsCount) { this.reviewsCount = reviewsCount; }
}
