package com.cookio.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cookio.app.R;
import com.cookio.app.adapters.PostAdapter;
import com.cookio.app.databinding.ActivityLikedPostsBinding;
import com.cookio.app.models.Post;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class LikedPostsActivity extends AppCompatActivity {

    private ActivityLikedPostsBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private final List<Post> likedPostsList = new ArrayList<>();
    private final Set<String> likedPostIds = new HashSet<>();
    private final Set<String> savedPostIds = new HashSet<>();

    private PostAdapter postAdapter;
    private int loadVersion = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLikedPostsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding.recyclerLikedPosts.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerLikedPosts.setHasFixedSize(true);

        postAdapter = new PostAdapter(
                this,
                likedPostsList,
                savedPostIds,
                likedPostIds,
                this::openPostDetail
        );
        postAdapter.setOnPostSaveStateChangedListener((post, isSaved) -> {
            if (isSaved) {
                savedPostIds.add(post.getPostId());
            } else {
                savedPostIds.remove(post.getPostId());
            }
            updateCount();
        });
        postAdapter.setOnPostLikeStateChangedListener((post, isLiked) -> {
            if (isLiked) {
                if (!likedPostIds.contains(post.getPostId())) {
                    likedPostIds.add(post.getPostId());
                }
                if (!containsPost(post.getPostId())) {
                    likedPostsList.add(post);
                    sortPostsNewestFirst();
                }
            } else {
                likedPostIds.remove(post.getPostId());
                removePost(post.getPostId());
            }
            finishLoading();
        });
        binding.recyclerLikedPosts.setAdapter(postAdapter);

        binding.btnBack.setOnClickListener(v -> navigateBack());
        setupBottomNavigation();
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadLikedPosts);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLikedPosts();
    }

    private void loadLikedPosts() {
        if (auth.getCurrentUser() == null) {
            binding.swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        loadVersion++;
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.emptyState.setVisibility(View.GONE);
        loadSavedPostIds(loadVersion);
    }

    private void loadSavedPostIds(int requestVersion) {
        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("savedPosts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (requestVersion != loadVersion) {
                        return;
                    }
                    savedPostIds.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        savedPostIds.add(doc.getId());
                    }
                    fetchLikedPosts(uid, requestVersion);
                })
                .addOnFailureListener(e -> {
                    if (requestVersion != loadVersion) {
                        return;
                    }
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(this, "Failed to load saved state", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchLikedPosts(String uid, int requestVersion) {
        db.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (requestVersion != loadVersion) {
                        return;
                    }
                    likedPostsList.clear();
                    likedPostIds.clear();

                    List<Post> allPosts = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Post post = doc.toObject(Post.class);
                        if (post != null) {
                            post.setPostId(doc.getId());
                            allPosts.add(post);
                        }
                    }

                    if (allPosts.isEmpty()) {
                        finishLoading();
                        return;
                    }

                    final int[] remaining = {allPosts.size()};
                    for (Post post : allPosts) {
                        db.collection("posts")
                                .document(post.getPostId())
                                .collection("likes")
                                .document(uid)
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (requestVersion != loadVersion) {
                                        return;
                                    }
                                    if (documentSnapshot.exists()) {
                                        likedPostIds.add(post.getPostId());
                                        if (!containsPost(post.getPostId())) {
                                            likedPostsList.add(post);
                                        }
                                    }
                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        sortPostsNewestFirst();
                                        finishLoading();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (requestVersion != loadVersion) {
                                        return;
                                    }
                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        sortPostsNewestFirst();
                                        finishLoading();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (requestVersion != loadVersion) {
                        return;
                    }
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(this, "Failed to load liked posts", Toast.LENGTH_SHORT).show();
                });
    }

    private void finishLoading() {
        postAdapter.updateData(likedPostsList);
        updateCount();
        binding.progressBar.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setRefreshing(false);
        binding.emptyState.setVisibility(likedPostsList.isEmpty() ? View.VISIBLE : View.GONE);
        binding.recyclerLikedPosts.setVisibility(likedPostsList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void updateCount() {
        int count = likedPostsList.size();
        binding.likedCount.setText(count + (count == 1 ? " post" : " posts"));
    }

    private void sortPostsNewestFirst() {
        Collections.sort(likedPostsList, (first, second) -> {
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
    }

    private boolean containsPost(String postId) {
        for (Post post : likedPostsList) {
            if (postId.equals(post.getPostId())) {
                return true;
            }
        }
        return false;
    }

    private void removePost(String postId) {
        for (int i = 0; i < likedPostsList.size(); i++) {
            if (postId.equals(likedPostsList.get(i).getPostId())) {
                likedPostsList.remove(i);
                break;
            }
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
        intent.putExtra(PostDetailActivity.EXTRA_POST_AUTHOR_NAME, post.getDisplayName());
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

        if (post.getCategories() != null) {
            intent.putStringArrayListExtra(
                    PostDetailActivity.EXTRA_POST_CATEGORIES,
                    new ArrayList<>(post.getCategories())
            );
        }

        startActivity(intent);
    }

    private void navigateBack() {
        if (isTaskRoot()) {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        } else {
            getOnBackPressedDispatcher().onBackPressed();
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = binding.bottomNavigation.bottomNavigation;
        bottomNavigation.getMenu().findItem(R.id.nav_my_recipes).setChecked(true);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_my_recipes) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_saved) {
                startActivity(new Intent(this, SavedPostsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }
}
