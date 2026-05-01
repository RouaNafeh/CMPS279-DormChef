package com.cookio.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.cookio.app.R;
import com.cookio.app.adapters.PostAdapter;
import com.cookio.app.databinding.ActivityPublicProfileBinding;
import com.cookio.app.models.Post;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PublicProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "extra_user_id";

    private ActivityPublicProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final List<Post> userPosts = new ArrayList<>();
    private final Set<String> savedPostIds = new HashSet<>();
    private final Set<String> likedPostIds = new HashSet<>();

    private PostAdapter postAdapter;
    private String targetUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPublicProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        targetUserId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (TextUtils.isEmpty(targetUserId)) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // If the user clicks on their OWN profile, redirect to ProfileActivity
        if (auth.getCurrentUser() != null
                && targetUserId.equals(auth.getCurrentUser().getUid())) {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
            return;
        }

        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        postAdapter = new PostAdapter(
                this,
                userPosts,
                savedPostIds,
                likedPostIds,
                this::openPostDetail
        );
        binding.rvUserPosts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvUserPosts.setAdapter(postAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
    }

    private void loadProfile() {
        binding.progressBar.setVisibility(View.VISIBLE);
        loadUserInfo();
        loadCurrentUserSavedAndLiked();
        loadUserPosts();
    }

    private void loadUserInfo() {
        db.collection("users")
                .document(targetUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String username = doc.getString("username");
                    String bio = doc.getString("bio");
                    String profileImageUrl = doc.getString("profileImageUrl");

                    binding.tvUsername.setText(
                            TextUtils.isEmpty(username) ? "User" : username
                    );
                    binding.tvBio.setText(
                            TextUtils.isEmpty(bio) ? "No bio yet." : bio
                    );
                    binding.tvAvatarInitial.setText(resolveInitial(username));

                    loadProfilePhoto(profileImageUrl);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadProfilePhoto(@Nullable String profileImageUrl) {
        if (!TextUtils.isEmpty(profileImageUrl)) {
            binding.tvAvatarInitial.setVisibility(View.GONE);
            binding.ivProfilePhoto.setVisibility(View.VISIBLE);

            Glide.with(this)
                    .load(profileImageUrl)
                    .placeholder(R.drawable.logo_cropped)
                    .error(R.drawable.logo_cropped)
                    .centerCrop()
                    .into(binding.ivProfilePhoto);
        } else {
            binding.ivProfilePhoto.setVisibility(View.GONE);
            binding.tvAvatarInitial.setVisibility(View.VISIBLE);
        }
    }

    private void loadCurrentUserSavedAndLiked() {
        if (auth.getCurrentUser() == null) return;
        String currentUid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(currentUid)
                .collection("savedPosts")
                .get()
                .addOnSuccessListener(snap -> {
                    savedPostIds.clear();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        savedPostIds.add(d.getId());
                    }
                    postAdapter.notifyDataSetChanged();
                });
    }

    private void loadUserPosts() {
        db.collection("posts")
                .whereEqualTo("uid", targetUserId)
                .get()
                .addOnSuccessListener(snap -> {
                    userPosts.clear();
                    int totalLikes = 0;

                    for (QueryDocumentSnapshot doc : snap) {
                        Post post = doc.toObject(Post.class);
                        post.setPostId(doc.getId());
                        userPosts.add(post);
                        totalLikes += post.getLikesCount();
                    }

                    Collections.sort(userPosts, (a, b) -> {
                        if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });

                    binding.tvPostsCount.setText(String.valueOf(userPosts.size()));
                    binding.tvLikesCount.setText(String.valueOf(totalLikes));

                    if (userPosts.isEmpty()) {
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                        binding.rvUserPosts.setVisibility(View.GONE);
                    } else {
                        binding.tvEmptyState.setVisibility(View.GONE);
                        binding.rvUserPosts.setVisibility(View.VISIBLE);
                    }

                    if (userPosts.isEmpty()) {
                        finishLoading();
                    } else {
                        loadLikedStates();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load posts", Toast.LENGTH_SHORT).show();
                    finishLoading();
                });
    }

    private void loadLikedStates() {
        if (auth.getCurrentUser() == null) {
            finishLoading();
            return;
        }

        String currentUid = auth.getCurrentUser().getUid();
        likedPostIds.clear();
        final int[] remaining = {userPosts.size()};

        for (Post post : userPosts) {
            db.collection("posts")
                    .document(post.getPostId())
                    .collection("likes")
                    .document(currentUid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) likedPostIds.add(post.getPostId());
                        remaining[0]--;
                        if (remaining[0] == 0) finishLoading();
                    })
                    .addOnFailureListener(e -> {
                        remaining[0]--;
                        if (remaining[0] == 0) finishLoading();
                    });
        }
    }

    private void finishLoading() {
        postAdapter.updateData(userPosts);
        binding.progressBar.setVisibility(View.GONE);
    }

    private String resolveInitial(@Nullable String value) {
        if (TextUtils.isEmpty(value)) return "U";
        return value.substring(0, 1).toUpperCase(Locale.getDefault());
    }

    private void openPostDetail(Post post) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.getPostId());
        intent.putExtra(PostDetailActivity.EXTRA_POST_TITLE, post.getTitle());
        intent.putExtra(PostDetailActivity.EXTRA_POST_DESCRIPTION, post.getDescription());
        intent.putExtra(PostDetailActivity.EXTRA_POST_IMAGE_URL, post.getImageUrl());
        intent.putExtra(PostDetailActivity.EXTRA_POST_COOK_TIME, post.getCookTime());
        intent.putExtra(PostDetailActivity.EXTRA_POST_BUDGET, post.getBudget());
        intent.putExtra(PostDetailActivity.EXTRA_POST_USERNAME, post.getUsername());
        intent.putExtra(PostDetailActivity.EXTRA_POST_LIKES_COUNT, post.getLikesCount());

        if (post.getIngredients() != null) {
            intent.putStringArrayListExtra(
                    PostDetailActivity.EXTRA_POST_INGREDIENTS,
                    new ArrayList<>(post.getIngredients())
            );
        }
        if (post.getSteps() != null) {
            intent.putStringArrayListExtra(
                    PostDetailActivity.EXTRA_POST_STEPS,
                    new ArrayList<>(post.getSteps())
            );
        }

        startActivity(intent);
    }
}