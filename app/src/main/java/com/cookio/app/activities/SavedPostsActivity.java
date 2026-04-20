package com.cookio.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cookio.app.adapters.PostAdapter;
import com.cookio.app.databinding.ActivitySavedPostsBinding;
import com.cookio.app.models.Post;
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

        postAdapter = new PostAdapter(this, savedPostsList, savedPostIds, likedPostIds);
        binding.recyclerSavedPosts.setAdapter(postAdapter);

        binding.btnBack.setOnClickListener(v -> finish());

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
        binding.tvEmpty.setVisibility(View.GONE);

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
                        postAdapter.updateData(savedPostsList);
                        binding.progressBar.setVisibility(View.GONE);
                        binding.tvEmpty.setVisibility(View.VISIBLE);
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
            postAdapter.updateData(savedPostsList);
            binding.progressBar.setVisibility(View.GONE);
            binding.tvEmpty.setVisibility(View.VISIBLE);
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
        binding.progressBar.setVisibility(View.GONE);
        binding.tvEmpty.setVisibility(savedPostsList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}