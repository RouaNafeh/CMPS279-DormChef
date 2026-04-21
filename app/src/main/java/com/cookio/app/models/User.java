package com.cookio.app.models;

public class User {
    private String uid;
    private String username;
    private String email;
    private String bio;
    private String profileImageUrl;
    private long createdAt;
    public User(){

    }
    public User(String uid, String username, String email, String bio, String profileImageUrl, long createdAt){
        this.uid = uid;
        this.username = username;
        this.bio = bio;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.createdAt = createdAt;
    }

    public String getUid(){
        return uid;
    }

    public String getUsername(){
        return username;
    }

    public String getEmail(){
        return email;
    }

    public String getBio(){
        return bio;
    }

    public String getProfileImageUrl(){
        return profileImageUrl;
    }

    public long getCreatedAt(){
        return createdAt;
    }

    public void setUid(String uid){
        this.uid = uid;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public void setBio(String bio){
        this.bio = bio;
    }

    public void setProfileImageUrl(String profileImageUrl){
        this.profileImageUrl = profileImageUrl;
    }

    public void setCreatedAt(long createdAt){
        this.createdAt = createdAt;
    }
}
