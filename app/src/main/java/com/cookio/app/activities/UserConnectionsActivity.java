package com.cookio.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cookio.app.R;
import com.cookio.app.adapters.UserListAdapter;
import com.cookio.app.databinding.ActivityUserConnectionsBinding;
import com.cookio.app.models.User;
import com.cookio.app.utils.UserDisplayHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class UserConnectionsActivity extends AppCompatActivity {
    public static final String EXTRA_USER_ID = "connections_user_id";
    public static final String EXTRA_POST_ID = "connections_post_id";
    public static final String EXTRA_MODE = "connections_mode";

    public static final String MODE_FOLLOWERS = "followers";
    public static final String MODE_FOLLOWING = "following";
    public static final String MODE_LIKES = "likes";

    private ActivityUserConnectionsBinding binding;
    private final List<User> users = new ArrayList<>();
    private UserListAdapter adapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityUserConnectionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        adapter = new UserListAdapter(this, users);
        binding.rvConnections.setLayoutManager(new LinearLayoutManager(this));
        binding.rvConnections.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> finish());

        String userId = getIntent().getStringExtra(EXTRA_USER_ID);
        String postId = getIntent().getStringExtra(EXTRA_POST_ID);
        String mode = getIntent().getStringExtra(EXTRA_MODE);

        if (mode == null) {
            finish();
            return;
        }

        if (MODE_LIKES.equals(mode)) {
            if (postId == null || postId.trim().isEmpty()) {
                finish();
                return;
            }
            binding.swipeRefreshLayout.setOnRefreshListener(() -> loadLikes(postId));
            binding.tvTitle.setText(R.string.connections_likes_title);
            binding.tvSubtitle.setText(R.string.connections_likes_subtitle);
            loadLikes(postId);
            return;
        }

        if (userId == null || userId.trim().isEmpty()) {
            finish();
            return;
        }

        binding.swipeRefreshLayout.setOnRefreshListener(() -> loadConnections(userId, mode));

        binding.tvTitle.setText(
                MODE_FOLLOWERS.equals(mode)
                        ? R.string.connections_followers_title
                        : R.string.connections_following_title
        );
        binding.tvSubtitle.setText(
                MODE_FOLLOWERS.equals(mode)
                        ? R.string.connections_followers_subtitle
                        : R.string.connections_following_subtitle
        );

        loadConnections(userId, mode);
    }

    private void loadConnections(String userId, String mode) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.emptyStateCard.setVisibility(View.GONE);
        binding.rvConnections.setVisibility(View.GONE);

        db.collection("users")
                .document(userId)
                .collection(mode)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->
                        loadFreshUsers(queryDocumentSnapshots.getDocuments(), mode))
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(this, R.string.connections_load_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private void loadLikes(String postId) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.emptyStateCard.setVisibility(View.GONE);
        binding.rvConnections.setVisibility(View.GONE);

        db.collection("posts")
                .document(postId)
                .collection(MODE_LIKES)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->
                        loadUsersByIds(queryDocumentSnapshots.getDocuments(), MODE_LIKES))
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(this, R.string.connections_load_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private void loadFreshUsers(List<DocumentSnapshot> connectionDocs, String mode) {
        users.clear();

        if (connectionDocs.isEmpty()) {
            renderConnections(mode);
            return;
        }

        final int[] remaining = {connectionDocs.size()};

        for (DocumentSnapshot connectionDoc : connectionDocs) {
            String connectionUserId = connectionDoc.getId();

            db.collection("users")
                    .document(connectionUserId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        User user = null;

                        if (userDoc.exists()) {
                            user = userDoc.toObject(User.class);
                        }

                        if (user == null) {
                            user = connectionDoc.toObject(User.class);
                        }

                        if (user != null) {
                            if (user.getUid() == null || user.getUid().trim().isEmpty()) {
                                user.setUid(connectionUserId);
                            }
                            users.add(user);
                        }

                        remaining[0]--;
                        if (remaining[0] == 0) {
                            renderConnections(mode);
                        }
                    })
                    .addOnFailureListener(e -> {
                        User fallbackUser = connectionDoc.toObject(User.class);
                        if (fallbackUser != null) {
                            if (fallbackUser.getUid() == null || fallbackUser.getUid().trim().isEmpty()) {
                                fallbackUser.setUid(connectionUserId);
                            }
                            users.add(fallbackUser);
                        }

                        remaining[0]--;
                        if (remaining[0] == 0) {
                            renderConnections(mode);
                        }
                    });
        }
    }

    private void loadUsersByIds(List<DocumentSnapshot> sourceDocs, String mode) {
        users.clear();

        if (sourceDocs.isEmpty()) {
            renderConnections(mode);
            return;
        }

        final int[] remaining = {sourceDocs.size()};

        for (DocumentSnapshot sourceDoc : sourceDocs) {
            String userId = sourceDoc.getId();

            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            User user = userDoc.toObject(User.class);
                            if (user != null) {
                                if (user.getUid() == null || user.getUid().trim().isEmpty()) {
                                    user.setUid(userId);
                                }
                                users.add(user);
                            }
                        }

                        remaining[0]--;
                        if (remaining[0] == 0) {
                            renderConnections(mode);
                        }
                    })
                    .addOnFailureListener(e -> {
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            renderConnections(mode);
                        }
                    });
        }
    }

    private void renderConnections(String mode) {
        users.sort(Comparator.comparing(
                user -> {
                    return UserDisplayHelper.resolveDisplayName(
                            user.getName(),
                            user.getUsername(),
                            ""
                    ).toLowerCase();
                }
        ));

        binding.progressBar.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setRefreshing(false);
        adapter.notifyDataSetChanged();

        boolean empty = users.isEmpty();
        binding.emptyStateCard.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvConnections.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.tvEmptySubtitle.setText(
                MODE_FOLLOWERS.equals(mode)
                        ? R.string.connections_followers_empty
                        : MODE_FOLLOWING.equals(mode)
                        ? R.string.connections_following_empty
                        : R.string.connections_likes_empty
        );
    }
}
