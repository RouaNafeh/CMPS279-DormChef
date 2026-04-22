package com.cookio.app.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cookio.app.R;
import com.cookio.app.adapters.PostAdapter;
import com.cookio.app.databinding.ActivityFeedBinding;
import com.cookio.app.models.Post;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FeedActivity extends AppCompatActivity {

    private ActivityFeedBinding binding;
    private List<Post> postList;
    private PostAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private Set<String> savedPostIds;
    private Set<String> likedPostIds;

    private ListenerRegistration feedListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        // Auth guard
        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(FeedActivity.this, LandingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        binding = ActivityFeedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        postList     = new ArrayList<>();
        savedPostIds = new HashSet<>();
        likedPostIds = new HashSet<>();

        adapter = new PostAdapter(this, postList, savedPostIds, likedPostIds,
                post -> openPostDetail(post));

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        // Logout
        binding.btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(FeedActivity.this, LandingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Bottom navigation
        BottomNavigationView bottomNavigation = binding.bottomNavigation.bottomNavigation;
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_favorites) {
                startActivity(new Intent(FeedActivity.this, FavoritesActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_my_recipes) {
                startActivity(new Intent(FeedActivity.this, MyRecipesActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        loadUserSets();
    }

    private void loadUserSets() {
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).collection("savedPosts")
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        savedPostIds.add(doc.getId());
                    }
                    startFeedListener();
                })
                .addOnFailureListener(e -> startFeedListener());
    }

    private void startFeedListener() {
        feedListener = db.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    postList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Post post = doc.toObject(Post.class);
                        if (post != null) {
                            post.setPostId(doc.getId());
                            postList.add(post);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void openPostDetail(Post post) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST_ID,          post.getPostId());
        intent.putExtra(PostDetailActivity.EXTRA_POST_TITLE,       post.getTitle());
        intent.putExtra(PostDetailActivity.EXTRA_POST_DESCRIPTION, post.getDescription());
        intent.putExtra(PostDetailActivity.EXTRA_POST_IMAGE_URL,   post.getImageUrl());
        intent.putExtra(PostDetailActivity.EXTRA_POST_COOK_TIME,   post.getCookTime());
        intent.putExtra(PostDetailActivity.EXTRA_POST_BUDGET,      post.getBudget());
        intent.putExtra(PostDetailActivity.EXTRA_POST_USERNAME,    post.getUsername());
        if (post.getIngredients() != null)
            intent.putStringArrayListExtra(PostDetailActivity.EXTRA_POST_INGREDIENTS,
                    new ArrayList<>(post.getIngredients()));
        if (post.getSteps() != null)
            intent.putStringArrayListExtra(PostDetailActivity.EXTRA_POST_STEPS,
                    new ArrayList<>(post.getSteps()));
        startActivity(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (feedListener != null) {
            feedListener.remove();
        }
    }
}