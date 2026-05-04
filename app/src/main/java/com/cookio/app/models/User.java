package com.cookio.app.models;

import java.util.Date;

public class User {
    private String uid;
    private String name;
    private String username;
    private String email;
    private String bio;
    private String profileImageUrl;
    private Date createdAt;

    private int followingCount;
    private int followerCount;

    public User() {

    }

    public User(String uid, String name, String username, String email, String bio, String profileImageUrl, Date createdAt, int followerCount, int followingCount) {
        this.uid = uid;
        this.name = name;
        this.username = username;
        this.bio = bio;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.createdAt = createdAt;
        this.followingCount = followingCount;
        this.followerCount = followerCount;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getBio() {
        return bio;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public int getFollowingCount(){ return followingCount; }

    public int getFollowerCount() { return followerCount; }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setFollowingCount(int followingCount) { this.followingCount = followingCount; }

    public void setFollowerCount(int followerCount) { this.followerCount = followerCount; }
}
