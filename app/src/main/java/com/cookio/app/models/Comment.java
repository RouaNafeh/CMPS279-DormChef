package com.cookio.app.models;

public class Comment {
    private String id;
    private String userId;
    private String username;
    private String text;
    private float rating;
    private int likesCount;
    private long timestamp;

    public Comment() {}

    public Comment(String userId, String username, String text, float rating) {
        this.userId = userId;
        this.username = username;
        this.text = text;
        this.rating = rating;
        this.likesCount = 0;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getText() { return text; }
    public float getRating() { return rating; }
    public int getLikesCount() { return likesCount; }
    public long getTimestamp() { return timestamp; }

    public void setId(String id) { this.id = id; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
}