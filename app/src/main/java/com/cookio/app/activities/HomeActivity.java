package com.cookio.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.Toast;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cookio.app.R;
import com.cookio.app.adapters.PostAdapter;
import com.cookio.app.databinding.ActivityHomeBinding;
import com.cookio.app.models.Post;
import com.cookio.app.utils.CookTimeFormatter;
import com.cookio.app.utils.AuthVerificationHelper;
import com.cookio.app.services.CookioMessagingService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private boolean filterActive = false;
    private List<Post> lastFilteredPosts = new ArrayList<>();
    private boolean showFollowingOnly = false;
    private final Set<String> followingUserIds = new HashSet<>();
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    CookioMessagingService.syncCurrentToken();
                } else {
                    Toast.makeText(this, R.string.notifications_permission_denied, Toast.LENGTH_SHORT).show();
                }
            });

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

        if (!auth.getCurrentUser().isEmailVerified()) {
            auth.signOut();
            AuthVerificationHelper.redirectToLogin(
                    this,
                    true,
                    getString(R.string.email_verification_required)
            );
            return;
        }

        db = FirebaseFirestore.getInstance();

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerView();
        setupSearch();
        setupActions();
        updateTabUI(false);
        setupBottomNavigation();
        binding.swipeRefreshLayout.setOnRefreshListener(this::refreshFeed);
        ensureNotificationSetup();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateNotificationDot();

        if (!filterActive) {
            loadFeedData();
        }

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

                if (dy <= 0) {
                    return;
                }

                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) {
                    return;
                }

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
        EditText searchInput = binding.searchBar.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchInput != null) {
            searchInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            searchInput.setHintTextColor(getColor(R.color.textGrey));
            searchInput.setTextColor(getColor(R.color.textDark));
            searchInput.setPadding(0, 0, 0, 0);
        }

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
    binding.filterBtn.setOnClickListener(v -> showFilterSheet());

    binding.btnQuickAddRecipe.setOnClickListener(v ->
            startActivity(new Intent(this, CreatePostActivity.class)));

    binding.btnNotifications.setOnClickListener(v -> {
        binding.viewNotificationDot.setVisibility(View.GONE);
        startActivity(new Intent(this, NotificationsActivity.class));
    });
    binding.btnAll.setOnClickListener(v -> {
        showFollowingOnly = false;
        updateTabUI(false);
        filterAllContent(currentQuery);
    });

    binding.btnFollowing.setOnClickListener(v -> {
        loadFollowingIds(() -> {
            showFollowingOnly = true;
            updateTabUI(true);
            filterAllContent(currentQuery);
        });
    });
}

    private void ensureNotificationSetup() {
        CookioMessagingService.syncCurrentToken();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            Toast.makeText(this, R.string.notifications_permission_rationale, Toast.LENGTH_SHORT).show();
        }

        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
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
    private void showFilterSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_filter, null, false);        dialog.setContentView(view);

        Button btnSortRating = view.findViewById(R.id.btnSortRating);
        Button btnSortTime = view.findViewById(R.id.btnSortTime);
        Button btnApply = view.findViewById(R.id.btnApply);
        Button btnClear = view.findViewById(R.id.btnClear);
        EditText etIngredient = view.findViewById(R.id.etIngredient);
        RadioGroup radioBudget = view.findViewById(R.id.radioBudget);

        final String[] sort = {""};

        btnSortRating.setAlpha(0.7f);
        btnSortTime.setAlpha(0.7f);

        btnSortRating.setOnClickListener(v -> {
            sort[0] = "rating";
            btnSortRating.setAlpha(1f);
            btnSortTime.setAlpha(0.45f);
        });

        btnSortTime.setOnClickListener(v -> {
            sort[0] = "time";
            btnSortTime.setAlpha(1f);
            btnSortRating.setAlpha(0.45f);
        });

        btnApply.setOnClickListener(v -> {
            String ingredient = etIngredient.getText().toString().toLowerCase();

            List<Post> filtered = new ArrayList<>();

            for (Post p : allPosts) {

                boolean matches = true;

                // INGREDIENT FILTER
                if (!ingredient.isEmpty()) {
                    matches = p.getIngredients().toString().toLowerCase().contains(ingredient);
                }

                // BUDGET FILTER
                int selectedId = radioBudget.getCheckedRadioButtonId();
                if (selectedId != -1) {
                    RadioButton selected = view.findViewById(selectedId);
                    String budget = selected.getText().toString();

                    if (!p.getBudget().equalsIgnoreCase(budget)) {
                        matches = false;
                    }
                }

                if (matches) filtered.add(p);
            }

            // SORT
            if (sort[0].equals("rating")) {
                filtered.sort((p1, p2) -> Float.compare(p2.getAvgRating(), p1.getAvgRating()));
            }

            if (sort[0].equals("time")) {
                filtered.sort((p1, p2) ->
                        Integer.compare(
                                CookTimeFormatter.toSortMinutes(p1.getCookTime()),
                                CookTimeFormatter.toSortMinutes(p2.getCookTime())
                        )
                );
            }

            filterActive = true;
            lastFilteredPosts.clear();
            lastFilteredPosts.addAll(filtered);

            postAdapter.updateData(lastFilteredPosts);
            dialog.dismiss();
        });

        btnClear.setOnClickListener(v -> {
            filterActive = false;
            lastFilteredPosts.clear();

            postAdapter.updateData(allPosts);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void loadFollowingIds(Runnable onComplete) {
        if (auth.getCurrentUser() == null) {
            onComplete.run();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("following")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    followingUserIds.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        followingUserIds.add(doc.getId());
                    }

                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load following list", Toast.LENGTH_SHORT).show();
                    onComplete.run();
                });
    }


    private void refreshFeed() {
        filterActive = false;
        lastFilteredPosts.clear();
        loadFeedData();
    }

    private void loadFeedData() {
        lastVisible = null;
        isLastPage = false;
        allPosts.clear();
        loadFollowingIds(() -> loadNextPage(true));
    }

    private void loadNextPage(boolean isFirstPage) {
        if (isLoading || isLastPage) {
            return;
        }
        isLoading = true;

        Query query = db.collection("posts")
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

    if (showFollowingOnly && followingUserIds.isEmpty()) {

        binding.tvEmptyState.setVisibility(View.VISIBLE);
        binding.tvEmptyState.setText("Follow creators to see their recipes here");

        postAdapter.updateData(filteredPosts);
        binding.swipeRefreshLayout.setRefreshing(false);

        return;
    }

    if (text == null || text.trim().isEmpty()) {

        for (Post post : allPosts) {

            if (showFollowingOnly) {

                if (followingUserIds.isEmpty()) {
                    filteredPosts.add(post);
                    continue;
                }

                if (post.getUid() == null || !followingUserIds.contains(post.getUid())) {
                    continue;
                }
            } 

            filteredPosts.add(post);
        }

    } else {
        String query = text.toLowerCase().trim();

        for (Post post : allPosts) {
            if (showFollowingOnly) {
                if (post.getUid() == null || !followingUserIds.contains(post.getUid())) {
                    continue;
                }
            }

            String title = post.getTitle() == null ? "" : post.getTitle().toLowerCase();
            String username = post.getUsername() == null ? "" : post.getUsername().toLowerCase();
            String description = post.getDescription() == null ? "" : post.getDescription().toLowerCase();

            if (title.contains(query) || username.contains(query) || description.contains(query)) {
                filteredPosts.add(post);
            }
        }
    }

    postAdapter.updateData(filteredPosts);

    if (filteredPosts.isEmpty()) {
        if (showFollowingOnly) {
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.tvEmptyState.setText("Follow creators to see their recipes here");
        } else {
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.tvEmptyState.setText("No recipes found");
        }
    } else {
        binding.tvEmptyState.setVisibility(View.GONE);
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

    private void openPublicProfile(String authorUid) {
        if (authorUid == null || authorUid.isEmpty()) return;
        Intent intent = new Intent(this, PublicProfileActivity.class);
        intent.putExtra(PublicProfileActivity.EXTRA_USER_ID, authorUid);
        startActivity(intent);
    }

    private void updateNotificationDot() {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        NotificationsActivity.getUnreadCount(uid, count -> {
            runOnUiThread(() -> {
                binding.viewNotificationDot.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void updateTabUI(boolean followingSelected) {

    if (followingSelected) {

        binding.btnFollowing.setBackgroundResource(R.drawable.bg_home_toggle_active);
        binding.btnFollowing.setTextColor(getResources().getColor(R.color.textDark));

        binding.btnAll.setBackgroundResource(android.R.color.transparent);
        binding.btnAll.setTextColor(getResources().getColor(R.color.textMuted));

    } else {

        binding.btnAll.setBackgroundResource(R.drawable.bg_home_toggle_active);
        binding.btnAll.setTextColor(getResources().getColor(R.color.textDark));

        binding.btnFollowing.setBackgroundResource(android.R.color.transparent);
        binding.btnFollowing.setTextColor(getResources().getColor(R.color.textMuted));
    }
}
}
