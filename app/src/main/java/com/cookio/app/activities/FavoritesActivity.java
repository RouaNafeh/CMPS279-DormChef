package com.cookio.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cookio.app.R;
import com.cookio.app.adapters.PostAdapter;
import com.cookio.app.databinding.ActivityFavoritesBinding;
import com.cookio.app.models.Post;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoritesActivity extends AppCompatActivity {

    private ActivityFavoritesBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private final List<Post> likedPostsList = new ArrayList<>();
    private final Set<String> likedPostIds = new HashSet<>();
    private final Set<String> savedPostIds = new HashSet<>();

    private PostAdapter postAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(FavoritesActivity.this, LandingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        binding = ActivityFavoritesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.favTitle.setText(R.string.liked_posts_title);
        binding.favSubtitle.setText(R.string.liked_posts_subtitle);
        binding.btnSavedPosts.setVisibility(View.GONE);

        binding.recyclerFavorites.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerFavorites.setHasFixedSize(true);
        postAdapter = new PostAdapter(this, likedPostsList, savedPostIds, likedPostIds);
        binding.recyclerFavorites.setAdapter(postAdapter);

        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLikedPosts();
    }

    private void loadLikedPosts() {
        if (auth.getCurrentUser() == null) {
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        binding.emptyState.setVisibility(View.GONE);

        db.collection("users")
                .document(uid)
                .collection("savedPosts")
                .get()
                .addOnSuccessListener(savedSnapshots -> {
                    savedPostIds.clear();
                    for (DocumentSnapshot savedDoc : savedSnapshots.getDocuments()) {
                        savedPostIds.add(savedDoc.getId());
                    }

                    fetchLikedPosts(uid);
                })
                .addOnFailureListener(e -> {
                    savedPostIds.clear();
                    fetchLikedPosts(uid);
                });
    }

    private void fetchLikedPosts(String uid) {
        db.collection("posts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    likedPostsList.clear();
                    likedPostIds.clear();

                    List<Post> allPosts = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
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
                                .addOnSuccessListener(doc -> {
                                    if (doc.exists()) {
                                        likedPostIds.add(post.getPostId());
                                        likedPostsList.add(post);
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
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load liked posts", Toast.LENGTH_SHORT).show();
                    finishLoading();
                });
    }

    private void finishLoading() {
        int count = likedPostsList.size();
        binding.favCount.setText(count + (count == 1 ? " post" : " posts"));
        postAdapter.updateData(likedPostsList);

        boolean isEmpty = likedPostsList.isEmpty();
        binding.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.recyclerFavorites.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = binding.bottomNavigation.bottomNavigation;
        bottomNavigation.setSelectedItemId(R.id.nav_favorites);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_favorites) {
                return true;
            } else if (id == R.id.nav_home) {
                startActivity(new Intent(FavoritesActivity.this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_saved) {
                startActivity(new Intent(FavoritesActivity.this, SavedPostsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_my_recipes) {
                startActivity(new Intent(FavoritesActivity.this, MyRecipesActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }
}
