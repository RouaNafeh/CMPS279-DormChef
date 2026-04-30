const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendPushNotification = onDocumentCreated(
  "notifications/{targetUserId}/items/{notificationId}",
  async (event) => {
    const snap = event.data;

    if (!snap) {
      console.log("No notification snapshot");
      return null;
    }

    const notification = snap.data();
    const targetUserId = event.params.targetUserId;

    if (!notification) {
      console.log("No notification data");
      return null;
    }

    const userDoc = await admin
      .firestore()
      .collection("users")
      .doc(targetUserId)
      .get();

    if (!userDoc.exists) {
      console.log("Target user not found:", targetUserId);
      return null;
    }

    const fcmToken = userDoc.data().fcmToken;

    if (!fcmToken) {
      console.log("No FCM token for user:", targetUserId);
      return null;
    }

    let title = "Cookio";

    switch (notification.type) {
      case "like":
        title = "❤️ New Like";
        break;
      case "follow":
        title = "👤 New Follower";
        break;
      case "comment":
        title = "💬 New Comment";
        break;
      case "review":
        title = "⭐ New Review";
        break;
    }

    const message = {
      token: fcmToken,
      notification: {
        title,
        body: notification.message || "You have a new notification",
      },
      data: {
        type: notification.type || "",
        postId: notification.postId || "",
        fromUid: notification.fromUid || "",
      },
      android: {
        priority: "high",
        notification: {
          channelId: "cookio_notifications",
        },
      },
    };

    try {
      const response = await admin.messaging().send(message);
      console.log("Push sent successfully:", response);
      return null;
    } catch (error) {
      console.error("Error sending push:", error);

      if (error.code === "messaging/registration-token-not-registered") {
        await admin
          .firestore()
          .collection("users")
          .doc(targetUserId)
          .update({
            fcmToken: admin.firestore.FieldValue.delete(),
          });
      }

      return null;
    }
  }
);