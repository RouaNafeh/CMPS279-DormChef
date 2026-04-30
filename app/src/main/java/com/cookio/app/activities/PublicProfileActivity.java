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
import com.cookio.app.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import database.FirestoreRepository;

public class PublicProfileActivity extends AppCompatActivity {
    public static final String EXTRA_USER_ID = "USER_ID";

    private ActivityPublicProfileBinding binding;
    private FirestoreRepository repository;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private final List<Post> userPosts = new ArrayList<>();
    private final Set<String> savedPostIds = new HashSet<>();
    private final Set<String> likedPostIds = new HashSet<>();

    private PostAdapter postAdapter;
    private String currentUserId;
    private String targetUserId;

    private User currentUser;
    private User targetUser;
    private boolean isFollowing = false;
    private boolean isFollowStateLoaded = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPublicProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new FirestoreRepository();
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            finish();
            return;
        }

        currentUserId = firebaseUser.getUid();
        targetUserId = getIntent().getStringExtra(EXTRA_USER_ID);

        if (TextUtils.isEmpty(targetUserId)) {
            finish();
            return;
        }

        setupRecyclerView();
        setupActions();
        populateFallbackState();
        loadUsers();
        loadUserPosts();
    }

    private void setupRecyclerView() {
        postAdapter = new PostAdapter(
                this,
                userPosts,
                savedPostIds,
                likedPostIds,
                this::openPostDetail
        );

        binding.rvPublicPosts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPublicPosts.setNestedScrollingEnabled(false);
        binding.rvPublicPosts.setAdapter(postAdapter);
    }

    private void setupActions() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnFollow.setOnClickListener(v -> toggleFollow());
        binding.cardFollowers.setOnClickListener(v -> openConnections(UserConnectionsActivity.MODE_FOLLOWERS));
        binding.cardFollowing.setOnClickListener(v -> openConnections(UserConnectionsActivity.MODE_FOLLOWING));
    }

    private void populateFallbackState() {
        binding.tvUsername.setText(getString(R.string.profile_default_username));
        binding.tvEmail.setText("");
        binding.tvBio.setText(getString(R.string.public_profile_bio_empty));
        binding.tvAvatarInitial.setText(resolveInitial(binding.tvUsername.getText().toString()));
        binding.tvPostsCount.setText("0");
        binding.tvFollowersCount.setText("0");
        binding.tvFollowingCount.setText("0");
        binding.tvPostsSectionMeta.setText(getString(R.string.profile_posts_section_meta));
        binding.btnFollow.setEnabled(false);
        binding.btnFollow.setText(R.string.public_profile_follow_loading_button);
    }

    private void loadUsers() {
        binding.progressBar.setVisibility(View.VISIBLE);

        repository.getUser(currentUserId, new FirestoreRepository.OnUserLoadedListener() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                loadTargetUser();
            }

            @Override
            public void onFailure(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(
                        PublicProfileActivity.this,
                        R.string.profile_load_failed,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void loadTargetUser() {
        repository.getUser(targetUserId, new FirestoreRepository.OnUserLoadedListener() {
            @Override
            public void onSuccess(User user) {
                targetUser = user;
                renderUser(user);

                if (currentUserId.equals(targetUserId)) {
                    binding.btnFollow.setVisibility(View.GONE);
                    binding.tvProfileSubtitle.setText(R.string.public_profile_self_subtitle);
                    binding.tvPostsSectionTitle.setText(R.string.public_profile_posts_self_title);
                } else {
                    binding.btnFollow.setVisibility(View.VISIBLE);
                    setFollowButtonLoading(true);
                    loadFollowState();
                }
            }

            @Override
            public void onFailure(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(
                        PublicProfileActivity.this,
                        R.string.public_profile_load_failed,
                        Toast.LENGTH_SHORT
                ).show();
                finish();
            }
        });
    }

    private void renderUser(User user) {
        String displayName = resolveDisplayName(user.getUsername(), user.getEmail());
        binding.tvUsername.setText(displayName);
        binding.tvEmail.setText(valueOrEmpty(user.getEmail()));
        binding.tvBio.setText(resolveBio(user.getBio()));
        binding.tvAvatarInitial.setText(resolveInitial(displayName));
        binding.tvFollowersCount.setText(String.valueOf(Math.max(0, user.getFollowerCount())));
        binding.tvFollowingCount.setText(String.valueOf(Math.max(0, user.getFollowingCount())));
        loadProfilePhoto(user.getProfileImageUrl());
    }

    private void loadFollowState() {
        repository.isFollowing(currentUserId, targetUserId, new FirestoreRepository.OnBooleanResultListener() {
            @Override
            public void onSuccess(boolean result) {
                isFollowing = result;
                isFollowStateLoaded = true;
                setFollowButtonLoading(false);
                updateFollowButton();
            }

            @Override
            public void onFailure(Exception e) {
                isFollowStateLoaded = false;
                setFollowButtonLoading(false);
                updateFollowButton();
                Toast.makeText(
                        PublicProfileActivity.this,
                        R.string.public_profile_follow_state_load_failed,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void updateFollowButton() {
        if (!isFollowStateLoaded) {
            binding.btnFollow.setText(R.string.public_profile_follow_retry_button);
            return;
        }

        binding.btnFollow.setText(
                isFollowing
                        ? getString(R.string.public_profile_following_button)
                        : getString(R.string.public_profile_follow_button)
        );
    }

    private void toggleFollow() {
        if (!isFollowStateLoaded) {
            setFollowButtonLoading(true);
            loadFollowState();
            return;
        }

        if (currentUser == null || targetUser == null || currentUserId.equals(targetUserId)) {
            return;
        }

        setFollowButtonLoading(true);

        if (isFollowing) {
            repository.unfollowUser(currentUserId, targetUserId, new FirestoreRepository.OnActionListener() {
                @Override
                public void onSuccess() {
                    isFollowing = false;
                    setFollowButtonLoading(false);
                    targetUser.setFollowerCount(Math.max(0, targetUser.getFollowerCount() - 1));
                    binding.tvFollowersCount.setText(String.valueOf(targetUser.getFollowerCount()));
                    updateFollowButton();
                    Toast.makeText(
                            PublicProfileActivity.this,
                            R.string.public_profile_unfollow_success,
                            Toast.LENGTH_SHORT
                    ).show();
                }

                @Override
                public void onFailure(Exception e) {
                    setFollowButtonLoading(false);
                    Toast.makeText(
                            PublicProfileActivity.this,
                            R.string.public_profile_unfollow_failed,
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        } else {
            repository.followUser(currentUser, targetUser, new FirestoreRepository.OnActionListener() {
                @Override
                public void onSuccess() {
                    isFollowing = true;
                    setFollowButtonLoading(false);
                    targetUser.setFollowerCount(targetUser.getFollowerCount() + 1);
                    binding.tvFollowersCount.setText(String.valueOf(targetUser.getFollowerCount()));
                    updateFollowButton();
                    Toast.makeText(
                            PublicProfileActivity.this,
                            R.string.public_profile_follow_success,
                            Toast.LENGTH_SHORT
                    ).show();
                }

                @Override
                public void onFailure(Exception e) {
                    setFollowButtonLoading(false);
                    Toast.makeText(
                            PublicProfileActivity.this,
                            R.string.public_profile_follow_failed,
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        }
    }

    private void setFollowButtonLoading(boolean loading) {
        binding.btnFollow.setEnabled(!loading);
        binding.btnFollow.setText(
                loading
                        ? getString(R.string.public_profile_follow_working_button)
                        : binding.btnFollow.getText()
        );
    }

    private void loadUserPosts() {
        binding.progressBar.setVisibility(View.VISIBLE);

        db.collection("posts")
                .whereEqualTo("uid", targetUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userPosts.clear();
                    int totalLikes = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Post post = doc.toObject(Post.class);
                        post.setPostId(doc.getId());
                        userPosts.add(post);
                        totalLikes += post.getLikesCount();
                    }

                    Collections.sort(userPosts, (first, second) -> {
                        if (first.getCreatedAt() == null && second.getCreatedAt() == null) {
                            return 0;
                        }
                        if (first.getCreatedAt() == null) {
                            return 1;
                        }
                        if (second.getCreatedAt() == null) {
                            return -1;
                        }
                        return second.getCreatedAt().compareTo(first.getCreatedAt());
                    });

                    binding.tvPostsCount.setText(String.valueOf(userPosts.size()));
                    binding.tvPostsSectionMeta.setText(
                            getString(R.string.public_profile_posts_meta, userPosts.size(), totalLikes)
                    );

                    if (userPosts.isEmpty()) {
                        likedPostIds.clear();
                        savedPostIds.clear();
                        finishPostLoading();
                    } else {
                        loadSavedPostIds();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.public_profile_posts_failed, Toast.LENGTH_SHORT).show();
                    finishPostLoading();
                });
    }

    private void loadSavedPostIds() {
        db.collection("users")
                .document(currentUserId)
                .collection("savedPosts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    savedPostIds.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        savedPostIds.add(doc.getId());
                    }
                    loadLikedPostIds();
                })
                .addOnFailureListener(e -> {
                    savedPostIds.clear();
                    loadLikedPostIds();
                });
    }

    private void loadLikedPostIds() {
        likedPostIds.clear();
        final int[] remaining = {userPosts.size()};

        for (Post post : userPosts) {
            db.collection("posts")
                    .document(post.getPostId())
                    .collection("likes")
                    .document(currentUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            likedPostIds.add(post.getPostId());
                        }

                        remaining[0]--;
                        if (remaining[0] == 0) {
                            finishPostLoading();
                        }
                    })
                    .addOnFailureListener(e -> {
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            finishPostLoading();
                        }
                    });
        }
    }

    private void finishPostLoading() {
        postAdapter.updateData(userPosts);
        binding.progressBar.setVisibility(View.GONE);

        boolean isEmpty = userPosts.isEmpty();
        binding.emptyStateCard.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.rvPublicPosts.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
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

    private void openPostDetail(Post post) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.getPostId());
        intent.putExtra(PostDetailActivity.EXTRA_POST_TITLE, post.getTitle());
        intent.putExtra(PostDetailActivity.EXTRA_POST_DESCRIPTION, post.getDescription());
        intent.putExtra(PostDetailActivity.EXTRA_POST_IMAGE_URL, post.getImageUrl());
        intent.putExtra(PostDetailActivity.EXTRA_POST_COOK_TIME, post.getCookTime());
        intent.putExtra(PostDetailActivity.EXTRA_POST_BUDGET, post.getBudget());
        intent.putExtra(PostDetailActivity.EXTRA_POST_USERNAME, post.getUsername());
        intent.putExtra(PostDetailActivity.EXTRA_POST_UID, post.getUid());
        intent.putExtra(PostDetailActivity.EXTRA_POST_LIKES_COUNT, post.getLikesCount());

        if (post.getIngredients() != null) {
            intent.putStringArrayListExtra(
                    PostDetailActivity.EXTRA_POST_INGREDIENTS,
                    new ArrayList<>(post.getIngredients())
            );
        }

        if (post.getEquipment() != null) {
            intent.putStringArrayListExtra(
                    PostDetailActivity.EXTRA_POST_EQUIPMENT,
                    new ArrayList<>(post.getEquipment())
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

    private void openConnections(String mode) {
        if (TextUtils.isEmpty(targetUserId)) {
            return;
        }

        Intent intent = new Intent(this, UserConnectionsActivity.class);
        intent.putExtra(UserConnectionsActivity.EXTRA_USER_ID, targetUserId);
        intent.putExtra(UserConnectionsActivity.EXTRA_MODE, mode);
        startActivity(intent);
    }

    private String resolveDisplayName(@Nullable String username, @Nullable String email) {
        if (!TextUtils.isEmpty(username)) {
            return username;
        }

        if (!TextUtils.isEmpty(email) && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }

        return getString(R.string.profile_default_username);
    }

    private String resolveInitial(String value) {
        if (TextUtils.isEmpty(value)) {
            return getString(R.string.profile_default_username)
                    .substring(0, 1)
                    .toUpperCase(Locale.getDefault());
        }
        return value.substring(0, 1).toUpperCase(Locale.getDefault());
    }

    private String resolveBio(@Nullable String bio) {
        if (TextUtils.isEmpty(bio)) {
            return getString(R.string.public_profile_bio_empty);
        }
        return bio;
    }

    private String valueOrEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }
}
