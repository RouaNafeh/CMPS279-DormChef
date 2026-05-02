package com.cookio.app.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;


public class Post {
    private String postId;
    private String uid;
    private String username;
    private String title;
    private String description;
    private String cookTime;
    private String budget;
    private List<String> ingredients;
    private List<String> equipment;
    private List<String> steps;
    private String imageUrl;
    private String profileImageUrl;
    @ServerTimestamp
    private Date createdAt;
    private int likesCount;
    private float avgRating;
    private int reviewsCount;

    // Required empty constructor for Firestore deserialization
    public Post() {}

    public Post(String uid, String username, String title, String description,
                String cookTime, String budget, List<String> ingredients, List<String> equipment,
                List<String> steps, String imageUrl) {
        this.uid         = uid;
        this.username    = username;
        this.title       = title;
        this.description = description;
        this.cookTime    = cookTime;
        this.budget      = budget;
        this.ingredients = ingredients;
        this.equipment   = equipment;
        this.steps       = steps;
        this.imageUrl    = imageUrl;
        this.likesCount  = 0;
    }

    public String getPostId()           { return postId; }
    public String getUid()              { return uid; }
    public String getUsername()         { return username; }
    public String getTitle()            { return title; }
    public String getDescription()      { return description; }
    public String getCookTime()         { return cookTime; }
    public String getBudget()           { return budget; }
    public List<String> getIngredients(){ return ingredients; }
    public List<String> getEquipment()  { return equipment; }
    public List<String> getSteps()      { return steps; }
    public String getImageUrl()         { return imageUrl; }
    public String getProfileImageUrl()  { return profileImageUrl; }
    public Date getCreatedAt()          { return createdAt; }
    public int getLikesCount()          { return likesCount; }
    public float getAvgRating() { return avgRating; }
    public int getReviewsCount() { return reviewsCount; }
    public void setAvgRating(float avgRating) { this.avgRating = avgRating; }
    public void setReviewsCount(int reviewsCount) { this.reviewsCount = reviewsCount; }

    public void setPostId(String postId)        { this.postId = postId; }
    public void setLikesCount(int likesCount)   { this.likesCount = likesCount; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
}
