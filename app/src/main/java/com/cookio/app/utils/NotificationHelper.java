package com.cookio.app.utils;

import com.cookio.app.models.Notification;
import com.google.firebase.firestore.FirebaseFirestore;


public class NotificationHelper {

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ── Like ──────────────────────────────────────────────────
    public static void sendLikeNotification(String targetUserId,
                                            String fromUid,
                                            String fromUsername,
                                            String fromPhotoUrl,
                                            String postId,
                                            String postTitle) {
        // Don't notify yourself
        if (targetUserId.equals(fromUid)) return;

        Notification n = new Notification(
                Notification.TYPE_LIKE,
                fromUid,
                fromUsername,
                fromPhotoUrl,
                postId,
                postTitle,
                fromUsername + " liked your post \"" + postTitle + "\""
        );
        writeNotification(targetUserId, n);
    }

    // ── Follow ────────────────────────────────────────────────
    public static void sendFollowNotification(String targetUserId,
                                              String fromUid,
                                              String fromUsername,
                                              String fromPhotoUrl) {
        if (targetUserId.equals(fromUid)) return;

        Notification n = new Notification(
                Notification.TYPE_FOLLOW,
                fromUid,
                fromUsername,
                fromPhotoUrl,
                null,
                null,
                fromUsername + " started following you"
        );
        writeNotification(targetUserId, n);
    }

    // ── Comment ───────────────────────────────────────────────
    public static void sendCommentNotification(String targetUserId,
                                               String fromUid,
                                               String fromUsername,
                                               String fromPhotoUrl,
                                               String postId,
                                               String postTitle,
                                               String commentPreview) {
        if (targetUserId.equals(fromUid)) return;

        Notification n = new Notification(
                Notification.TYPE_COMMENT,
                fromUid,
                fromUsername,
                fromPhotoUrl,
                postId,
                postTitle,
                fromUsername + " commented on \"" + postTitle + "\": " + commentPreview
        );
        writeNotification(targetUserId, n);
    }

    // ── Review / Rating ───────────────────────────────────────
    public static void sendReviewNotification(String targetUserId,
                                              String fromUid,
                                              String fromUsername,
                                              String fromPhotoUrl,
                                              String postId,
                                              String postTitle,
                                              int rating) {
        if (targetUserId.equals(fromUid)) return;

        Notification n = new Notification(
                Notification.TYPE_REVIEW,
                fromUid,
                fromUsername,
                fromPhotoUrl,
                postId,
                postTitle,
                fromUsername + " gave your recipe \"" + postTitle + "\" " + rating + " stars"
        );
        writeNotification(targetUserId, n);
    }

    // ── Write to Firestore ────────────────────────────────────
    private static void writeNotification(String targetUserId, Notification notification) {
        db.collection("notifications")
                .document(targetUserId)
                .collection("items")
                .add(notification)
                .addOnSuccessListener(docRef ->
                        // Write the auto-generated ID back into the doc
                        docRef.update("notificationId", docRef.getId()))
                .addOnFailureListener(e ->
                        android.util.Log.e("NotificationHelper",
                                "Failed to write notification: " + e.getMessage()));
    }
}