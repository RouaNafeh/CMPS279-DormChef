package com.cookio.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cookio.app.R;
import com.cookio.app.adapters.PostAdapter;
import com.cookio.app.databinding.ActivityHomeBinding;
import com.cookio.app.models.Post;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private final List<Post> allPosts = new ArrayList<>();
    private final List<Post> filteredPosts = new ArrayList<>();
    private final Set<String> savedPostIds = new HashSet<>();
    private final Set<String> likedPostIds = new HashSet<>();

    private PostAdapter postAdapter;
    private String currentQuery = "";

    private static final int PAGE_SIZE = 10;
    private DocumentSnapshot lastVisible = null;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(HomeActivity.this, LandingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }


        db = FirebaseFirestore.getInstance();

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerView();
        setupSearch();
        setupActions();
        setupBottomNavigation();
        binding.swipeRefreshLayout.setOnRefreshListener(this::refreshFeed);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFeedData();
    }

    private void setupRecyclerView() {
        postAdapter = new PostAdapter(
                this,
                filteredPosts,
                savedPostIds,
                likedPostIds,
                this::openPostDetail
        );
        binding.recyclerCards.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerCards.setHasFixedSize(true);
        binding.recyclerCards.setAdapter(postAdapter);
        binding.recyclerCards.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy <= 0) return;

                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) return;

                int visibleCount = lm.getChildCount();
                int totalCount = lm.getItemCount();
                int firstVisible = lm.findFirstVisibleItemPosition();

                if (!isLoading && !isLastPage
                        && (visibleCount + firstVisible + 3) >= totalCount
                        && firstVisible >= 0) {
                    loadNextPage(false);
                }
            }
        });
    }

    private void setupSearch() {
        binding.searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentQuery = query == null ? "" : query;
                filterAllContent(currentQuery);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText == null ? "" : newText;
                filterAllContent(currentQuery);
                return true;
            }
        });
    }

    private void setupActions() {
        binding.filterBtn.setOnClickListener(v ->
                startActivity(new Intent(this, FilterActivity.class)));

        binding.btnQuickAddRecipe.setOnClickListener(v ->
                startActivity(new Intent(this, CreatePostActivity.class)));
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = binding.bottomNavigation.bottomNavigation;
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_saved) {
                startActivity(new Intent(HomeActivity.this, SavedPostsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_my_recipes) {
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void refreshFeed() {
        loadFeedData();
    }

    private void loadFeedData() {
        // Reset pagination state — this is page 1
        lastVisible = null;
        isLastPage = false;
        allPosts.clear();
        loadNextPage(true);
    }

    private void loadNextPage(boolean isFirstPage) {
        if (isLoading || isLastPage) return;
        isLoading = true;

        com.google.firebase.firestore.Query query = db.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE);

        if (lastVisible != null) {
            query = query.startAfter(lastVisible);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();

                    if (docs.isEmpty()) {
                        isLastPage = true;
                        isLoading = false;
                        if (isFirstPage) {
                            loadSavedPostIds();
                        } else {
                            binding.swipeRefreshLayout.setRefreshing(false);
                        }
                        return;
                    }

                    for (DocumentSnapshot doc : docs) {
                        Post post = doc.toObject(Post.class);
                        if (post != null) {
                            post.setPostId(doc.getId());
                            allPosts.add(post);
                        }
                    }

                    lastVisible = docs.get(docs.size() - 1);
                    if (docs.size() < PAGE_SIZE) {
                        isLastPage = true;
                    }

                    isLoading = false;

                    if (isFirstPage) {
                        loadSavedPostIds();
                    } else {
                        filterAllContent(currentQuery);
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    Toast.makeText(this, "Failed to load posts", Toast.LENGTH_SHORT).show();
                    binding.swipeRefreshLayout.setRefreshing(false);
                });
    }



    private void loadSavedPostIds() {
        if (auth.getCurrentUser() == null) {
            filterAllContent(currentQuery);
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
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
                    Toast.makeText(this, "Failed to sync saved posts", Toast.LENGTH_SHORT).show();
                    filterAllContent(currentQuery);
                });
    }

    private void loadLikedPostIds() {
        if (auth.getCurrentUser() == null) {
            filterAllContent(currentQuery);
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        likedPostIds.clear();

        if (allPosts.isEmpty()) {
            filterAllContent(currentQuery);
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
                        }

                        remaining[0]--;
                        if (remaining[0] == 0) {
                            filterAllContent(currentQuery);
                        }
                    })
                    .addOnFailureListener(e -> {
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            filterAllContent(currentQuery);
                        }
                    });
        }
    }

    private void filterAllContent(String text) {
        filteredPosts.clear();

        if (text == null || text.trim().isEmpty()) {
            filteredPosts.addAll(allPosts);
        } else {
            String query = text.toLowerCase().trim();

            for (Post post : allPosts) {
                String title = post.getTitle() == null ? "" : post.getTitle().toLowerCase();
                String username = post.getUsername() == null ? "" : post.getUsername().toLowerCase();

                if (title.contains(query) || username.contains(query)) {
                    filteredPosts.add(post);
                }
        }   }

        postAdapter.updateData(filteredPosts);

        if (filteredPosts.isEmpty()) {
            Toast.makeText(this, "No recipes found", Toast.LENGTH_SHORT).show();
        }
        binding.swipeRefreshLayout.setRefreshing(false);
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
