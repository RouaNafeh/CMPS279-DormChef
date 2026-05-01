package com.cookio.app.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;


public class Notification {

    public static final String TYPE_LIKE    = "like";
    public static final String TYPE_FOLLOW  = "follow";
    public static final String TYPE_COMMENT = "comment";
    public static final String TYPE_REVIEW  = "review";

    private String notificationId;
    private String type;          // "like", "follow", "comment", "review"
    private String fromUid;       // who triggered it
    private String fromUsername;  // display name of who triggered it
    private String fromPhotoUrl;  // avatar of who triggered it (can be null)
    private String postId;        // relevant post (null for follow)
    private String postTitle;     // for display (null for follow)
    private String message;       // e.g. "liked your post", "started following you"
    private boolean isRead;
    @ServerTimestamp
    private Date createdAt;

    // Required empty constructor for Firestore
    public Notification() {}

    public Notification(String type, String fromUid, String fromUsername,
                        String fromPhotoUrl, String postId, String postTitle,
                        String message) {
        this.type          = type;
        this.fromUid       = fromUid;
        this.fromUsername  = fromUsername;
        this.fromPhotoUrl  = fromPhotoUrl;
        this.postId        = postId;
        this.postTitle     = postTitle;
        this.message       = message;
        this.isRead        = false;
    }

    public String getNotificationId()          { return notificationId; }
    public String getType()                    { return type; }
    public String getFromUid()                 { return fromUid; }
    public String getFromUsername()            { return fromUsername; }
    public String getFromPhotoUrl()            { return fromPhotoUrl; }
    public String getPostId()                  { return postId; }
    public String getPostTitle()               { return postTitle; }
    public String getMessage()                 { return message; }
    public boolean isRead()                    { return isRead; }
    public Date getCreatedAt()                 { return createdAt; }

    public void setNotificationId(String id)   { this.notificationId = id; }
    public void setRead(boolean read)          { this.isRead = read; }
}