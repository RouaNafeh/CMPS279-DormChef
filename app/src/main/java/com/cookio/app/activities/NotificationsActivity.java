package com.cookio.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cookio.app.adapters.NotificationAdapter;
import com.cookio.app.databinding.ActivityNotificationsBinding;
import com.cookio.app.models.Notification;
import com.cookio.app.models.Post;
import com.cookio.app.activities.PostDetailActivity;
import com.cookio.app.activities.PublicProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private ActivityNotificationsBinding binding;
    private FirebaseFirestore db;
    private String currentUid;

    private final List<Notification> notifications = new ArrayList<>();
    private NotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        adapter = new NotificationAdapter(this, notifications, this::handleNotificationClick);
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotifications.setNestedScrollingEnabled(false);
        binding.rvNotifications.setAdapter(adapter);

        binding.tvMarkAllRead.setOnClickListener(v -> markAllRead());

        loadNotifications();
    }

    // ── Load notifications from Firestore ─────────────────────
    private void loadNotifications() {
        binding.progressBar.setVisibility(View.VISIBLE);

        db.collection("notifications")
                .document(currentUid)
                .collection("items")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snap -> {
                    notifications.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Notification n = doc.toObject(Notification.class);
                        n.setNotificationId(doc.getId());
                        notifications.add(n);
                    }
                    adapter.updateData(notifications);
                    binding.progressBar.setVisibility(View.GONE);

                    boolean isEmpty = notifications.isEmpty();
                    binding.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    binding.rvNotifications.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
                });
    }

    // ── Handle tap on a notification ──────────────────────────
    private void handleNotificationClick(Notification n) {
        // Mark as read
        if (!n.isRead()) {
            db.collection("notifications")
                    .document(currentUid)
                    .collection("items")
                    .document(n.getNotificationId())
                    .update("isRead", true);
            n.setRead(true);
            adapter.notifyDataSetChanged();
        }

        // Navigate based on type
        switch (n.getType() != null ? n.getType() : "") {
            case Notification.TYPE_LIKE:
            case Notification.TYPE_COMMENT:
            case Notification.TYPE_REVIEW:
                // Open the relevant post if postId is available
                if (n.getPostId() != null && !n.getPostId().isEmpty()) {
                    openPostDetail(n.getPostId(), n.getPostTitle());
                }
                break;

            case Notification.TYPE_FOLLOW:
                // Open the follower's public profile
                if (n.getFromUid() != null) {
                    Intent intent = new Intent(this, PublicProfileActivity.class);
                    intent.putExtra(PublicProfileActivity.EXTRA_USER_ID, n.getFromUid());
                    startActivity(intent);
                }
                break;
        }
    }

    // ── Open post detail by fetching it from Firestore ────────
    private void openPostDetail(String postId, String postTitle) {
        db.collection("posts").document(postId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Post no longer exists", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent intent = new Intent(this, PostDetailActivity.class);
                    intent.putExtra(PostDetailActivity.EXTRA_POST_ID,          doc.getId());
                    intent.putExtra(PostDetailActivity.EXTRA_POST_TITLE,       doc.getString("title"));
                    intent.putExtra(PostDetailActivity.EXTRA_POST_DESCRIPTION, doc.getString("description"));
                    intent.putExtra(PostDetailActivity.EXTRA_POST_IMAGE_URL,   doc.getString("imageUrl"));
                    intent.putExtra(PostDetailActivity.EXTRA_POST_COOK_TIME,   doc.getString("cookTime"));
                    intent.putExtra(PostDetailActivity.EXTRA_POST_BUDGET,      doc.getString("budget"));
                    intent.putExtra(PostDetailActivity.EXTRA_POST_USERNAME,    doc.getString("username"));
                    Long likes = doc.getLong("likesCount");
                    intent.putExtra(PostDetailActivity.EXTRA_POST_LIKES_COUNT,
                            likes != null ? likes.intValue() : 0);

                    // ingredients + steps
                    Object ing = doc.get("ingredients");
                    Object stp = doc.get("steps");
                    if (ing instanceof List) {
                        ArrayList<String> ingList = new ArrayList<>();
                        for (Object o : (List<?>) ing) { if (o instanceof String) ingList.add((String) o); }
                        intent.putStringArrayListExtra(PostDetailActivity.EXTRA_POST_INGREDIENTS, ingList);
                    }
                    if (stp instanceof List) {
                        ArrayList<String> stpList = new ArrayList<>();
                        for (Object o : (List<?>) stp) { if (o instanceof String) stpList.add((String) o); }
                        intent.putStringArrayListExtra(PostDetailActivity.EXTRA_POST_STEPS, stpList);
                    }
                    startActivity(intent);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load post", Toast.LENGTH_SHORT).show());
    }

    // ── Mark all notifications as read ────────────────────────
    private void markAllRead() {
        for (Notification n : notifications) {
            if (!n.isRead()) {
                db.collection("notifications")
                        .document(currentUid)
                        .collection("items")
                        .document(n.getNotificationId())
                        .update("isRead", true);
                n.setRead(true);
            }
        }
        adapter.notifyDataSetChanged();
    }

    // ── Static helper: get unread count for badge ─────────────
    public static void getUnreadCount(String uid, UnreadCountCallback callback) {
        FirebaseFirestore.getInstance()
                .collection("notifications")
                .document(uid)
                .collection("items")
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(snap -> callback.onCount(snap.size()))
                .addOnFailureListener(e -> callback.onCount(0));
    }

    public interface UnreadCountCallback {
        void onCount(int count);
    }
}