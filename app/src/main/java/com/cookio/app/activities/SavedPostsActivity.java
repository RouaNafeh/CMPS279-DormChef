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
import com.cookio.app.databinding.ActivitySavedPostsBinding;
import com.cookio.app.models.Post;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SavedPostsActivity extends AppCompatActivity {

    private ActivitySavedPostsBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private final List<Post> savedPostsList = new ArrayList<>();
    private final Set<String> savedPostIds = new HashSet<>();
    private final Set<String> likedPostIds = new HashSet<>();

    private PostAdapter postAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySavedPostsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding.recyclerSavedPosts.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerSavedPosts.setHasFixedSize(true);

        postAdapter = new PostAdapter(
                this,
                savedPostsList,
                savedPostIds,
                likedPostIds,
                this::openPostDetail
        );
        postAdapter.setOnPostUnsavedListener(this::removeUnsavedPost);
        postAdapter.setOnPostSaveStateChangedListener((post, isSaved) -> {
            if (isSaved) {
                if (!savedPostIds.contains(post.getPostId())) {
                    savedPostIds.add(post.getPostId());
                }
                if (!containsSavedPost(post.getPostId())) {
                    savedPostsList.add(post);
                }
            } else {
                savedPostIds.remove(post.getPostId());
                removeUnsavedPost(post.getPostId());
            }
            finishLoading();
        });
        postAdapter.setOnPostLikeStateChangedListener((post, isLiked) -> {
            if (isLiked) {
                likedPostIds.add(post.getPostId());
            } else {
                likedPostIds.remove(post.getPostId());
            }
        });
        binding.recyclerSavedPosts.setAdapter(postAdapter);

        binding.btnBack.setOnClickListener(v -> navigateBack());
        setupBottomNavigation();

        loadSavedPosts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedPosts();
    }

    private void loadSavedPosts() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.emptyState.setVisibility(View.GONE);

        db.collection("users")
                .document(uid)
                .collection("savedPosts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    savedPostIds.clear();
                    List<String> ids = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String postId = doc.getId();
                        savedPostIds.add(postId);
                        ids.add(postId);
                    }

                    if (ids.isEmpty()) {
                        savedPostsList.clear();
                        likedPostIds.clear();
                        finishLoading();
                        return;
                    }

                    fetchSavedPosts(ids);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load saved posts", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchSavedPosts(List<String> ids) {
        db.collection("posts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    savedPostsList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        if (ids.contains(doc.getId())) {
                            Post post = doc.toObject(Post.class);
                            if (post != null) {
                                post.setPostId(doc.getId());
                                savedPostsList.add(post);
                            }
                        }
                    }

                    loadLikedPostIdsForSavedPosts();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to fetch saved posts", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadLikedPostIdsForSavedPosts() {
        if (auth.getCurrentUser() == null) {
            binding.progressBar.setVisibility(View.GONE);
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        likedPostIds.clear();

        if (savedPostsList.isEmpty()) {
            finishLoading();
            return;
        }

        final int[] remaining = {savedPostsList.size()};

        for (Post post : savedPostsList) {
            db.collection("posts")
                    .document(post.getPostId())
                    .collection("likes")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            likedPostIds.add(post.getPostId());
                        }

                        remaining[0]--;
                        if (remaining[0] == 0) {
                            finishLoading();
                        }
                    })
                    .addOnFailureListener(e -> {
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            finishLoading();
                        }
                    });
        }
    }

    private void finishLoading() {
        postAdapter.updateData(savedPostsList);
        int count = savedPostsList.size();
        binding.savedCount.setText(count + (count == 1 ? " post" : " posts"));
        binding.progressBar.setVisibility(View.GONE);
        binding.emptyState.setVisibility(savedPostsList.isEmpty() ? View.VISIBLE : View.GONE);
        binding.recyclerSavedPosts.setVisibility(savedPostsList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void removeUnsavedPost(String postId) {
        for (int i = 0; i < savedPostsList.size(); i++) {
            if (postId.equals(savedPostsList.get(i).getPostId())) {
                savedPostsList.remove(i);
                break;
            }
        }
        savedPostIds.remove(postId);
        likedPostIds.remove(postId);
        finishLoading();
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

    private void navigateBack() {
        if (isTaskRoot()) {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        } else {
            getOnBackPressedDispatcher().onBackPressed();
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = binding.bottomNavigation.bottomNavigation;
        bottomNavigation.setSelectedItemId(R.id.nav_saved);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_saved) {
                return true;
            } else if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_my_recipes) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private boolean containsSavedPost(String postId) {
        for (Post post : savedPostsList) {
            if (postId.equals(post.getPostId())) {
                return true;
            }
        }
        return false;
    }
}
