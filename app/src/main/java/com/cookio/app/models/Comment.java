package com.cookio.app.models;

public class Comment {
    private String id;
    private String userId;
    private String name;
    private String username;
    private String text;
    private float rating;
    private int likesCount;
    private long timestamp;

    public Comment() {}

    public Comment(String userId, String name, String username, String text, float rating) {
        this.userId = userId;
        this.name = name;
        this.username = username;
        this.text = text;
        this.rating = rating;
        this.likesCount = 0;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getText() { return text; }
    public float getRating() { return rating; }
    public int getLikesCount() { return likesCount; }
    public long getTimestamp() { return timestamp; }
    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        if (username != null && !username.trim().isEmpty()) {
            return username;
        }
        return "";
    }

    public void setId(String id) { this.id = id; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
}
