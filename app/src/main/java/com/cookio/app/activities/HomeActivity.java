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
import com.cookio.app.adapters.UserListAdapter;
import com.cookio.app.databinding.ActivityHomeBinding;
import com.cookio.app.models.Post;
import com.cookio.app.models.User;
import com.cookio.app.utils.CookTimeFormatter;
import com.cookio.app.utils.AuthVerificationHelper;
import com.cookio.app.utils.RecipeCategoryHelper;
import com.cookio.app.utils.UserDisplayHelper;
import com.cookio.app.services.CookioMessagingService;
import com.google.android.material.chip.Chip;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private final List<Post> allPosts = new ArrayList<>();
    private final List<Post> filteredPosts = new ArrayList<>();
    private final List<User> matchedUsers = new ArrayList<>();
    private final Set<String> savedPostIds = new HashSet<>();
    private final Set<String> likedPostIds = new HashSet<>();

    private PostAdapter postAdapter;
    private UserListAdapter userListAdapter;
    private String currentQuery = "";
    private String activeCookSearchQuery = "";

    private static final int PAGE_SIZE = 10;
    private static final String SORT_NONE = "";
    private static final String SORT_RATING = "rating";
    private static final String SORT_TIME = "time";
    private DocumentSnapshot lastVisible = null;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private boolean showFollowingOnly = false;
    private final Set<String> followingUserIds = new HashSet<>();
    private String selectedIngredientFilter = "";
    private String selectedBudgetFilter = "";
    private String selectedMaxTimeFilter = "";
    private String selectedCategoryFilter = "";
    private String selectedSort = SORT_NONE;
    private ListenerRegistration unreadNotificationListener;
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
        setupUserResultsRecyclerView();
        setupSearch();
        setupCategoryBrowseChips();
        setupActions();
        updateTabUI(false);
        setupBottomNavigation();
        binding.swipeRefreshLayout.setOnRefreshListener(this::refreshFeed);
        ensureNotificationSetup();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFeedData();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startUnreadNotificationListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopUnreadNotificationListener();
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

    private void setupUserResultsRecyclerView() {
        userListAdapter = new UserListAdapter(this, matchedUsers);
        binding.recyclerCooks.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerCooks.setNestedScrollingEnabled(false);
        binding.recyclerCooks.setAdapter(userListAdapter);
    }

    private void setupSearch() {
        EditText searchInput = binding.searchBar.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchInput != null) {
            searchInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            searchInput.setHintTextColor(getColor(R.color.textGrey));
            searchInput.setTextColor(getColor(R.color.textDark));
            searchInput.setPadding(0, 0, 0, 0);
        }

        binding.searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentQuery = query == null ? "" : query;
                filterAllContent(currentQuery);
                searchCooks(currentQuery);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText == null ? "" : newText;
                filterAllContent(currentQuery);
                searchCooks(currentQuery);
                return true;
            }
        });
    }

    private void setupCategoryBrowseChips() {
        binding.homeCategoryChipGroup.removeAllViews();
        binding.homeCategoryChipGroup.setSingleSelection(true);
        binding.homeCategoryChipGroup.setSelectionRequired(true);

        addBrowseCategoryChip("", getString(R.string.category_all_label));
        for (String category : RecipeCategoryHelper.getAllowedCategories()) {
            addBrowseCategoryChip(category, category);
        }

        binding.homeCategoryChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }

            View selectedView = group.findViewById(checkedIds.get(0));
            if (!(selectedView instanceof Chip)) {
                return;
            }

            Object tag = selectedView.getTag();
            selectedCategoryFilter = tag instanceof String ? (String) tag : "";
            applyCurrentPostFilters();
        });

        checkBrowseCategoryChip(selectedCategoryFilter);
    }

    private void addBrowseCategoryChip(String categoryValue, String label) {
        Chip chip = (Chip) getLayoutInflater().inflate(
                R.layout.item_category_chip,
                binding.homeCategoryChipGroup,
                false
        );
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setTag(categoryValue);
        chip.setCheckedIconVisible(false);
        binding.homeCategoryChipGroup.addView(chip);
    }

    private void checkBrowseCategoryChip(String category) {
        int fallbackChipId = View.NO_ID;

        for (int index = 0; index < binding.homeCategoryChipGroup.getChildCount(); index++) {
            View child = binding.homeCategoryChipGroup.getChildAt(index);
            if (!(child instanceof Chip)) {
                continue;
            }

            Chip chip = (Chip) child;
            if (index == 0) {
                fallbackChipId = chip.getId();
            }

            Object tag = chip.getTag();
            if (tag instanceof String && ((String) tag).equals(category)) {
                binding.homeCategoryChipGroup.check(chip.getId());
                return;
            }
        }

        if (fallbackChipId != View.NO_ID) {
            binding.homeCategoryChipGroup.check(fallbackChipId);
        }
    }

    private void setupActions() {
    binding.filterBtn.setOnClickListener(v -> showFilterSheet());

    binding.btnQuickAddRecipe.setOnClickListener(v ->
            startActivity(new Intent(this, CreatePostActivity.class)));

    binding.btnNotifications.setOnClickListener(v -> {
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
        bottomNavigation.getMenu().findItem(R.id.nav_home).setChecked(true);

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
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_filter, null, false);
        dialog.setContentView(view);

        Button btnSortRating = view.findViewById(R.id.btnSortRating);
        Button btnSortTime = view.findViewById(R.id.btnSortTime);
        Button btnApply = view.findViewById(R.id.btnApply);
        Button btnClear = view.findViewById(R.id.btnClear);
        EditText etIngredient = view.findViewById(R.id.etIngredient);
        EditText etMaxTime = view.findViewById(R.id.etMaxTime);
        RadioGroup radioBudget = view.findViewById(R.id.radioBudget);

        etIngredient.setText(selectedIngredientFilter);
        etMaxTime.setText(selectedMaxTimeFilter);
        syncSortButtons(btnSortRating, btnSortTime, selectedSort);
        syncBudgetSelection(radioBudget, selectedBudgetFilter);

        btnSortRating.setOnClickListener(v -> {
            selectedSort = SORT_RATING.equals(selectedSort) ? SORT_NONE : SORT_RATING;
            syncSortButtons(btnSortRating, btnSortTime, selectedSort);
        });

        btnSortTime.setOnClickListener(v -> {
            selectedSort = SORT_TIME.equals(selectedSort) ? SORT_NONE : SORT_TIME;
            syncSortButtons(btnSortRating, btnSortTime, selectedSort);
        });

        btnApply.setOnClickListener(v -> {
            selectedIngredientFilter = etIngredient.getText().toString().trim();
            selectedMaxTimeFilter = etMaxTime.getText().toString().trim();
            selectedBudgetFilter = resolveSelectedBudget(view, radioBudget);
            applyCurrentPostFilters();
            dialog.dismiss();
        });

        btnClear.setOnClickListener(v -> {
            selectedIngredientFilter = "";
            selectedBudgetFilter = "";
            selectedMaxTimeFilter = "";
            selectedSort = SORT_NONE;
            etIngredient.setText("");
            etMaxTime.setText("");
            radioBudget.clearCheck();
            syncSortButtons(btnSortRating, btnSortTime, selectedSort);
            applyCurrentPostFilters();
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
        loadFeedData();
        searchCooks(currentQuery);
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
        currentQuery = text == null ? "" : text;
        applyCurrentPostFilters();
    }

    private void applyCurrentPostFilters() {
        filteredPosts.clear();

        if (showFollowingOnly && followingUserIds.isEmpty()) {
            postAdapter.updateData(filteredPosts);
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.tvEmptyState.setText("Follow creators to see their recipes here");
            binding.swipeRefreshLayout.setRefreshing(false);
            return;
        }

        String searchQuery = safeLower(currentQuery).trim();
        List<String> ingredientQueries = parseIngredientFilters(selectedIngredientFilter);
        String budgetQuery = safeLower(selectedBudgetFilter).trim();
        int maxTimeMinutes = resolveMaxTimeFilter(selectedMaxTimeFilter);

        for (Post post : allPosts) {
            if (!matchesFollowingFilter(post)) {
                continue;
            }
            if (!matchesCategoryFilter(post)) {
                continue;
            }
            if (!matchesSearchQuery(post, searchQuery)) {
                continue;
            }
            if (!matchesIngredientFilter(post, ingredientQueries)) {
                continue;
            }
            if (!matchesBudgetFilter(post, budgetQuery)) {
                continue;
            }
            if (!matchesMaxTimeFilter(post, maxTimeMinutes)) {
                continue;
            }
            filteredPosts.add(post);
        }

        sortPosts(filteredPosts);
        postAdapter.updateData(filteredPosts);
        updateEmptyState();
        binding.swipeRefreshLayout.setRefreshing(false);
        maybeLoadMoreForDiscovery();
    }

    private boolean matchesFollowingFilter(Post post) {
        if (!showFollowingOnly) {
            return true;
        }
        return post.getUid() != null && followingUserIds.contains(post.getUid());
    }

    private boolean matchesSearchQuery(Post post, String query) {
        if (query.isEmpty()) {
            return true;
        }

        String title = safeLower(post.getTitle());
        String name = safeLower(post.getName());
        String username = safeLower(post.getUsername());
        String description = safeLower(post.getDescription());
        String categories = RecipeCategoryHelper.buildSearchableText(post.getCategories());
        return title.contains(query)
                || name.contains(query)
                || username.contains(query)
                || description.contains(query)
                || categories.contains(query);
    }

    private boolean matchesCategoryFilter(Post post) {
        return RecipeCategoryHelper.matchesCategory(post.getCategories(), selectedCategoryFilter);
    }

    private boolean matchesIngredientFilter(Post post, List<String> ingredientQueries) {
        if (ingredientQueries.isEmpty()) {
            return true;
        }

        if (post.getIngredients() == null || post.getIngredients().isEmpty()) {
            return false;
        }

        for (String query : ingredientQueries) {
            boolean matchedCurrentIngredient = false;
            for (String ingredient : post.getIngredients()) {
                if (safeLower(ingredient).contains(query)) {
                    matchedCurrentIngredient = true;
                    break;
                }
            }
            if (!matchedCurrentIngredient) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesBudgetFilter(Post post, String budgetQuery) {
        if (budgetQuery.isEmpty()) {
            return true;
        }
        return safeLower(post.getBudget()).equals(budgetQuery);
    }

    private boolean matchesMaxTimeFilter(Post post, int maxTimeMinutes) {
        if (maxTimeMinutes <= 0) {
            return true;
        }

        int postMinutes = CookTimeFormatter.toSortMinutes(post.getCookTime());
        if (postMinutes == Integer.MAX_VALUE) {
            return false;
        }
        return postMinutes <= maxTimeMinutes;
    }

    private void sortPosts(List<Post> posts) {
        if (SORT_RATING.equals(selectedSort)) {
            posts.sort((first, second) -> Float.compare(second.getAvgRating(), first.getAvgRating()));
            return;
        }

        if (SORT_TIME.equals(selectedSort)) {
            posts.sort((first, second) -> Integer.compare(
                    CookTimeFormatter.toSortMinutes(first.getCookTime()),
                    CookTimeFormatter.toSortMinutes(second.getCookTime())
            ));
        }
    }

    private void updateEmptyState() {
        if (!filteredPosts.isEmpty()) {
            binding.tvEmptyState.setVisibility(View.GONE);
            return;
        }

        binding.tvEmptyState.setVisibility(View.VISIBLE);
        if (showFollowingOnly && followingUserIds.isEmpty()) {
            binding.tvEmptyState.setText("Follow creators to see their recipes here");
            return;
        }

        if (showFollowingOnly) {
            binding.tvEmptyState.setText("No recipes from creators you follow");
            return;
        }

        if (hasActiveFilters() || !currentQuery.trim().isEmpty()) {
            binding.tvEmptyState.setText("No recipes match your filters");
            return;
        }

        binding.tvEmptyState.setText("No recipes found");
    }

    private boolean hasActiveFilters() {
        boolean hasBrowseCategory = !selectedCategoryFilter.trim().isEmpty();
        return !selectedIngredientFilter.trim().isEmpty()
                || !selectedBudgetFilter.trim().isEmpty()
                || !selectedMaxTimeFilter.trim().isEmpty()
                || hasBrowseCategory
                || !SORT_NONE.equals(selectedSort);
    }

    private boolean hasDiscoveryConstraints() {
        return showFollowingOnly
                || !currentQuery.trim().isEmpty()
                || hasActiveFilters();
    }

    private void maybeLoadMoreForDiscovery() {
        if (isLoading || isLastPage || !hasDiscoveryConstraints()) {
            return;
        }

        if (filteredPosts.size() >= PAGE_SIZE) {
            return;
        }

        loadNextPage(false);
    }

    private int resolveMaxTimeFilter(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return 0;
        }

        try {
            return Math.max(Integer.parseInt(rawValue.trim()), 0);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private List<String> parseIngredientFilters(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> parsed = new ArrayList<>();
        for (String token : Arrays.asList(rawValue.split(","))) {
            String normalized = safeLower(token).trim();
            if (!normalized.isEmpty()) {
                parsed.add(normalized);
            }
        }
        return parsed;
    }

    private void syncSortButtons(Button btnSortRating, Button btnSortTime, String sort) {
        btnSortRating.setAlpha(SORT_RATING.equals(sort) ? 1f : 0.55f);
        btnSortTime.setAlpha(SORT_TIME.equals(sort) ? 1f : 0.55f);
    }

    private void syncBudgetSelection(RadioGroup radioBudget, String budget) {
        radioBudget.clearCheck();
        if (budget.isEmpty()) {
            return;
        }

        for (int index = 0; index < radioBudget.getChildCount(); index++) {
            View child = radioBudget.getChildAt(index);
            if (!(child instanceof android.widget.RadioButton)) {
                continue;
            }

            android.widget.RadioButton button = (android.widget.RadioButton) child;
            if (safeLower(button.getText().toString()).equals(safeLower(budget))) {
                button.setChecked(true);
                return;
            }
        }
    }

    private String resolveSelectedBudget(View root, RadioGroup radioBudget) {
        int selectedId = radioBudget.getCheckedRadioButtonId();
        if (selectedId == -1) {
            return "";
        }

        View selectedView = root.findViewById(selectedId);
        if (!(selectedView instanceof android.widget.RadioButton)) {
            return "";
        }

        return ((android.widget.RadioButton) selectedView).getText().toString().trim();
    }

    private void searchCooks(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        activeCookSearchQuery = query;

        if (query.isEmpty()) {
            matchedUsers.clear();
            if (userListAdapter != null) {
                userListAdapter.notifyDataSetChanged();
            }
            binding.cooksSection.setVisibility(View.GONE);
            binding.tvCooksEmpty.setVisibility(View.GONE);
            binding.recyclerCooks.setVisibility(View.GONE);
            return;
        }

        binding.cooksSection.setVisibility(View.VISIBLE);
        binding.tvCooksTitle.setText(R.string.search_cooks_title);
        binding.tvCooksSubtitle.setText(R.string.search_cooks_subtitle);
        binding.tvCooksEmpty.setVisibility(View.GONE);
        binding.recyclerCooks.setVisibility(View.GONE);

        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!query.equals(activeCookSearchQuery)) {
                        return;
                    }

                    matchedUsers.clear();
                    String currentUid = auth.getCurrentUser() == null ? "" : auth.getCurrentUser().getUid();
                    String normalizedQuery = query.toLowerCase(Locale.getDefault());

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user == null) {
                            continue;
                        }

                        if (user.getUid() == null || user.getUid().trim().isEmpty()) {
                            user.setUid(doc.getId());
                        }

                        if (user.getUid() != null && user.getUid().equals(currentUid)) {
                            continue;
                        }

                        String username = safeLower(user.getUsername());
                        String name = safeLower(user.getName());
                        String bio = safeLower(user.getBio());

                        if (username.contains(normalizedQuery)
                                || name.contains(normalizedQuery)
                                || bio.contains(normalizedQuery)) {
                            matchedUsers.add(user);
                        }
                    }

                    matchedUsers.sort((first, second) -> {
                        int firstRank = userMatchRank(first, normalizedQuery);
                        int secondRank = userMatchRank(second, normalizedQuery);
                        if (firstRank != secondRank) {
                            return Integer.compare(firstRank, secondRank);
                        }
                        return resolveUserSortValue(first).compareTo(resolveUserSortValue(second));
                    });

                    if (matchedUsers.size() > 6) {
                        matchedUsers.subList(6, matchedUsers.size()).clear();
                    }

                    userListAdapter.notifyDataSetChanged();
                    boolean hasMatches = !matchedUsers.isEmpty();
                    binding.recyclerCooks.setVisibility(hasMatches ? View.VISIBLE : View.GONE);
                    binding.tvCooksEmpty.setVisibility(hasMatches ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    if (!query.equals(activeCookSearchQuery)) {
                        return;
                    }
                    matchedUsers.clear();
                    userListAdapter.notifyDataSetChanged();
                    binding.recyclerCooks.setVisibility(View.GONE);
                    binding.tvCooksEmpty.setVisibility(View.VISIBLE);
                    binding.tvCooksEmpty.setText(R.string.search_cooks_empty);
                });
    }

    private int userMatchRank(User user, String query) {
        String username = safeLower(user.getUsername());
        String name = safeLower(user.getName());
        if (username.startsWith(query)) {
            return 0;
        }
        if (name.startsWith(query)) {
            return 1;
        }
        return 2;
    }

    private String resolveUserSortValue(User user) {
        return UserDisplayHelper.resolveDisplayName(
                user.getName(),
                user.getUsername(),
                ""
        ).trim().toLowerCase(Locale.getDefault());
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.getDefault());
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

    private void openPublicProfile(String authorUid) {
        if (authorUid == null || authorUid.isEmpty()) return;
        Intent intent = new Intent(this, PublicProfileActivity.class);
        intent.putExtra(PublicProfileActivity.EXTRA_USER_ID, authorUid);
        startActivity(intent);
    }

    private void startUnreadNotificationListener() {
        if (auth.getCurrentUser() == null) {
            return;
        }

        stopUnreadNotificationListener();

        unreadNotificationListener = db.collection("notifications")
                .document(auth.getCurrentUser().getUid())
                .collection("items")
                .whereEqualTo("isRead", false)
                .limit(1)
                .addSnapshotListener((snap, error) -> {
                    if (error != null || binding == null) {
                        return;
                    }

                    boolean hasUnread = snap != null && !snap.isEmpty();
                    binding.viewNotificationDot.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
                });
    }

    private void stopUnreadNotificationListener() {
        if (unreadNotificationListener != null) {
            unreadNotificationListener.remove();
            unreadNotificationListener = null;
        }
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
