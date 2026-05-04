package com.cookio.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.cookio.app.R;
import com.cookio.app.adapters.PostAdapter;
import com.cookio.app.databinding.ActivityProfileBinding;
import com.google.android.material.textfield.TextInputEditText;
import com.cookio.app.models.Post;
import com.cookio.app.utils.UserDisplayHelper;
import com.cookio.app.utils.UsernameHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";

    private interface CompletionListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    private interface RefsListener {
        void onSuccess(List<DocumentReference> refs);
        void onFailure(Exception e);
    }

    private interface PostsListener {
        void onSuccess(List<Post> posts);
        void onFailure(Exception e);
    }

    private ActivityProfileBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private final List<Post> myPosts = new ArrayList<>();
    private final Set<String> savedPostIds = new HashSet<>();
    private final Set<String> likedPostIds = new HashSet<>();

    private PostAdapter postAdapter;
    private boolean isGrid = false;
    private String currentDisplayName = "";
    private String currentUsername = "";

    private final ActivityResultLauncher<String> profileImagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadProfilePhoto(uri);
                }
            });
    private final ActivityResultLauncher<String> profilePhotoPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    profileImagePickerLauncher.launch("image/*");
                } else {
                    Toast.makeText(this, R.string.photo_permission_denied, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(this, LandingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        postAdapter = new PostAdapter(
                this,
                myPosts,
                savedPostIds,
                likedPostIds,
                this::openPostDetail
        );
        postAdapter.setOnPostDeleteListener(post -> showDeleteDialog(post));
        postAdapter.setOnPostEditListener(this::showPostActionsDialog);
        binding.rvMyPosts.setNestedScrollingEnabled(false);
        binding.rvMyPosts.setAdapter(postAdapter);

        binding.btnEdit.setOnClickListener(v -> showEditDialog());
        binding.btnCreatePost.setOnClickListener(v ->
                startActivity(new Intent(this, CreatePostActivity.class)));
        binding.btnSavedPosts.setOnClickListener(v ->
                startActivity(new Intent(this, LikedPostsActivity.class)));
        binding.btnProfileMenu.setOnClickListener(v -> showProfileMenu());
        binding.cardFollowers.setOnClickListener(v -> openConnections(UserConnectionsActivity.MODE_FOLLOWERS));
        binding.cardFollowing.setOnClickListener(v -> openConnections(UserConnectionsActivity.MODE_FOLLOWING));
        binding.ivProfilePhoto.setOnClickListener(v -> requestPhotoAccessForProfile());
        binding.tvAvatarInitial.setOnClickListener(v -> requestPhotoAccessForProfile());
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadProfile);

        binding.btnToggleView.setOnClickListener(v -> {
            isGrid = !isGrid;
            setLayoutManager();
            postAdapter.setGridMode(isGrid);
            updateToggleViewLabel();
        });

        setupBottomNavigation();
        populateStaticUserFields();
        setLayoutManager();
        updateToggleViewLabel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
    }

    private void setLayoutManager() {
        if (isGrid) {
            binding.rvMyPosts.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            binding.rvMyPosts.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    private void updateToggleViewLabel() {
        binding.btnToggleView.setText(
                isGrid ? R.string.profile_view_list : R.string.profile_view_grid
        );
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = binding.bottomNavigation.bottomNavigation;
        bottomNavigation.getMenu().findItem(R.id.nav_my_recipes).setChecked(true);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_my_recipes) {
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

    private void populateStaticUserFields() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return;
        }

        String email = user.getEmail();
        binding.tvEmail.setText("");
        binding.tvUsername.setText(getString(R.string.profile_default_username));
        binding.tvAvatarInitial.setText(UserDisplayHelper.resolveInitial(
                binding.tvUsername.getText().toString(),
                getString(R.string.profile_default_username)
        ));
        binding.tvFollowersCount.setText("0");
        binding.tvFollowingCount.setText("0");
    }

    private void showDeleteDialog(Post post) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_destructive_confirm, null, false);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ((TextView) dialogView.findViewById(R.id.tvDialogEyebrow)).setText("Post");
        ((TextView) dialogView.findViewById(R.id.tvDialogTitle)).setText("Delete post?");
        ((TextView) dialogView.findViewById(R.id.tvDialogMessage)).setText("This removes the recipe and its related activity from your profile.");

        dialogView.findViewById(R.id.btnDialogConfirm).setOnClickListener(v -> {
            dialog.dismiss();
            deletePost(post);
        });
        dialogView.findViewById(R.id.btnDialogCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showPostActionsDialog(Post post) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_post_actions, null, false);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView titleView = dialogView.findViewById(R.id.tvPostActionsTitle);
        titleView.setText(post.getTitle());

        dialogView.findViewById(R.id.btnActionEditPost).setOnClickListener(v -> {
            dialog.dismiss();
            openEditPost(post);
        });

        dialogView.findViewById(R.id.btnActionDeletePost).setOnClickListener(v -> {
            dialog.dismiss();
            showDeleteDialog(post);
        });

        dialogView.findViewById(R.id.btnActionCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void openEditPost(Post post) {
        Intent intent = new Intent(this, CreatePostActivity.class);
        intent.putExtra(CreatePostActivity.EXTRA_EDIT_MODE, true);
        intent.putExtra(CreatePostActivity.EXTRA_POST_ID, post.getPostId());
        intent.putExtra(CreatePostActivity.EXTRA_POST_TITLE, post.getTitle());
        intent.putExtra(CreatePostActivity.EXTRA_POST_DESCRIPTION, post.getDescription());
        intent.putExtra(CreatePostActivity.EXTRA_POST_COOK_TIME, post.getCookTime());
        intent.putExtra(CreatePostActivity.EXTRA_POST_BUDGET, post.getBudget());
        intent.putExtra(CreatePostActivity.EXTRA_POST_IMAGE_URL, post.getImageUrl());

        if (post.getIngredients() != null) {
            intent.putStringArrayListExtra(
                    CreatePostActivity.EXTRA_POST_INGREDIENTS,
                    new ArrayList<>(post.getIngredients())
            );
        }

        if (post.getEquipment() != null) {
            intent.putStringArrayListExtra(
                    CreatePostActivity.EXTRA_POST_EQUIPMENT,
                    new ArrayList<>(post.getEquipment())
            );
        }

        if (post.getSteps() != null) {
            intent.putStringArrayListExtra(
                    CreatePostActivity.EXTRA_POST_STEPS,
                    new ArrayList<>(post.getSteps())
            );
        }

        startActivity(intent);
    }

    private void loadProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            binding.swipeRefreshLayout.setRefreshing(false);
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        loadUserInfo(user);
        loadSavedPostIds();
        loadMyPosts(user);
    }

    private void requestPhotoAccessForProfile() {
        String permission = getPhotoPermission();
        if (permission == null
                || ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            profileImagePickerLauncher.launch("image/*");
            return;
        }

        if (shouldShowRequestPermissionRationale(permission)) {
            Toast.makeText(this, R.string.photo_permission_rationale, Toast.LENGTH_SHORT).show();
        }

        profilePhotoPermissionLauncher.launch(permission);
    }

    private String getPhotoPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        }
        return Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private void loadUserInfo(FirebaseUser user) {
        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        createMissingUserProfile(user);
                        return;
                    }

                    String name = documentSnapshot.getString("name");
                    String username = documentSnapshot.getString("username");
                    String bio = documentSnapshot.getString("bio");
                    String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                    Long followerCount = documentSnapshot.getLong("followerCount");
                    Long followingCount = documentSnapshot.getLong("followingCount");

                    currentUsername = UsernameHelper.normalize(username);
                    currentDisplayName = UserDisplayHelper.resolveDisplayName(
                            name,
                            currentUsername,
                            getString(R.string.profile_default_username)
                    );

                    getSharedPreferences("cookio_prefs", MODE_PRIVATE)
                            .edit()
                            .putString("display_name", currentDisplayName)
                            .putString("username", currentUsername)
                            .putString("photoUrl", profileImageUrl == null ? "" : profileImageUrl)
                            .apply();

                    binding.tvUsername.setText(currentDisplayName);
                    binding.tvEmail.setText(UserDisplayHelper.resolveHandle(currentUsername));
                    binding.tvBio.setText(resolveBio(bio));
                    binding.tvAvatarInitial.setText(UserDisplayHelper.resolveInitial(
                            currentDisplayName,
                            getString(R.string.profile_default_username)
                    ));
                    binding.tvFollowersCount.setText(String.valueOf(followerCount == null ? 0 : followerCount));
                    binding.tvFollowingCount.setText(String.valueOf(followingCount == null ? 0 : followingCount));
                    loadProfilePhoto(profileImageUrl);
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        R.string.profile_load_failed,
                        Toast.LENGTH_SHORT
                ).show());
    }

    private void createMissingUserProfile(FirebaseUser user) {
        String fallbackUsername = buildFallbackUsername(user);
        DocumentReference userRef = db.collection("users").document(user.getUid());
        DocumentReference usernameRef = db.collection("usernames").document(fallbackUsername);

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("name", resolveFirebaseUserName(user));
        userData.put("username", fallbackUsername);
        userData.put("email", user.getEmail());
        userData.put("bio", "");
        userData.put("profileImageUrl", user.getPhotoUrl() == null ? "" : user.getPhotoUrl().toString());
        userData.put("followerCount", 0);
        userData.put("followingCount", 0);
        userData.put("createdAt", FieldValue.serverTimestamp());

        Map<String, Object> usernameData = new HashMap<>();
        usernameData.put("uid", user.getUid());
        usernameData.put("username", fallbackUsername);
        usernameData.put("createdAt", FieldValue.serverTimestamp());

        db.runTransaction(transaction -> {
                    transaction.set(userRef, userData);
                    if (!transaction.get(usernameRef).exists()) {
                        transaction.set(usernameRef, usernameData);
                    }
                    return null;
                })
                .addOnSuccessListener(unused -> loadUserInfo(user))
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(this, R.string.profile_load_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private String buildFallbackUsername(FirebaseUser user) {
        String suffix = user.getUid() == null ? "" : user.getUid().toLowerCase(Locale.getDefault());
        if (suffix.length() > 8) {
            suffix = suffix.substring(0, 8);
        }
        return UsernameHelper.normalize("cookio" + suffix);
    }

    private String resolveFirebaseUserName(FirebaseUser user) {
        if (!TextUtils.isEmpty(user.getDisplayName())) {
            return user.getDisplayName().trim();
        }
        if (!TextUtils.isEmpty(user.getEmail()) && user.getEmail().contains("@")) {
            return user.getEmail().substring(0, user.getEmail().indexOf('@'));
        }
        return getString(R.string.profile_default_username);
    }

    private void loadSavedPostIds() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .collection("savedPosts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    savedPostIds.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        savedPostIds.add(doc.getId());
                    }
                    postAdapter.notifyDataSetChanged();
                });
    }

    private void loadMyPosts(FirebaseUser user) {
        db.collection("posts")
                .whereEqualTo("uid", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    myPosts.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Post post = doc.toObject(Post.class);
                        post.setPostId(doc.getId());
                        myPosts.add(post);
                    }

                    Collections.sort(myPosts, (first, second) -> {
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

                    binding.tvPostsCount.setText(String.valueOf(myPosts.size()));
                    binding.tvPostsSectionMeta.setText(
                            String.format(Locale.getDefault(), "%d posts", myPosts.size())
                    );

                    if (myPosts.isEmpty()) {
                        likedPostIds.clear();
                        finishPostLoading();
                    } else {
                        loadLikedStates(user.getUid());
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(this, R.string.profile_load_failed, Toast.LENGTH_SHORT).show();
                    finishPostLoading();
                });
    }

    private void loadLikedStates(String uid) {
        likedPostIds.clear();
        final int[] remaining = {myPosts.size()};

        for (Post post : myPosts) {
            db.collection("posts")
                    .document(post.getPostId())
                    .collection("likes")
                    .document(uid)
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
        postAdapter.updateData(myPosts);
        binding.progressBar.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setRefreshing(false);

        boolean isEmpty = myPosts.isEmpty();
        binding.emptyStateCard.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.rvMyPosts.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
    private void loadProfilePhoto(@Nullable String profileImageUrl) {
        if (!TextUtils.isEmpty(profileImageUrl)) {
            binding.tvAvatarInitial.setVisibility(View.GONE);
            binding.ivProfilePhoto.setVisibility(View.VISIBLE);

            Glide.with(this)
                    .load(profileImageUrl)
                    .placeholder(R.drawable.logo)
                    .error(R.drawable.logo)
                    .centerCrop()
                    .into(binding.ivProfilePhoto);
        } else {
            binding.ivProfilePhoto.setVisibility(View.GONE);
            binding.tvAvatarInitial.setVisibility(View.VISIBLE);
        }
    }

    private void uploadProfilePhoto(Uri imageUri) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        setProfilePhotoUploadEnabled(false);

        StorageReference ref = storage.getReference()
                .child("profileImages")
                .child(user.getUid())
                .child("avatar.jpg");

        ref.putFile(imageUri)
                .addOnSuccessListener(task ->
                        ref.getDownloadUrl().addOnSuccessListener(downloadUri ->
                                db.collection("users")
                                        .document(user.getUid())
                                        .update("profileImageUrl", downloadUri.toString())
                                        .addOnSuccessListener(unused -> {
                                            updateProfileImageOnPosts(user.getUid(), downloadUri.toString(), new CompletionListener() {
                                                @Override
                                                public void onSuccess() {
                                                    setProfilePhotoUploadEnabled(true);
                                                    loadProfilePhoto(downloadUri.toString());
                                                    getSharedPreferences("cookio_prefs", MODE_PRIVATE)
                                                            .edit()
                                                            .putString("photoUrl", downloadUri.toString())
                                                            .apply();

                                                    Toast.makeText(ProfileActivity.this,
                                                            R.string.profile_photo_updated,
                                                            Toast.LENGTH_SHORT).show();
                                                }

                                                @Override
                                                public void onFailure(Exception e) {
                                                    setProfilePhotoUploadEnabled(true);
                                                    Log.e(TAG, "Failed to update post profile images", e);
                                                    showProfilePhotoUploadError(e);
                                                }
                                            });
                                        })
                                        .addOnFailureListener(e -> {
                                            setProfilePhotoUploadEnabled(true);
                                            Log.e(TAG, "Failed to save profileImageUrl to Firestore", e);
                                            showProfilePhotoUploadError(e);
                                        })
                        ).addOnFailureListener(e -> {
                            setProfilePhotoUploadEnabled(true);
                            Log.e(TAG, "Failed to resolve uploaded profile photo download URL", e);
                            showProfilePhotoUploadError(e);
                        })
                )
                .addOnFailureListener(e -> {
                    setProfilePhotoUploadEnabled(true);
                    Log.e(TAG, "Failed to upload profile photo to Firebase Storage", e);
                    showProfilePhotoUploadError(e);
                });
    }

    private void setProfilePhotoUploadEnabled(boolean enabled) {
        binding.ivProfilePhoto.setEnabled(enabled);
        binding.tvAvatarInitial.setEnabled(enabled);
    }

    private void showProfilePhotoUploadError(Exception e) {
        String message = e == null || TextUtils.isEmpty(e.getMessage())
                ? getString(R.string.profile_photo_upload_failed)
                : getString(R.string.profile_photo_upload_failed_with_reason, e.getMessage());
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showProfileMenu() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_profile_actions, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnProfileLogout).setOnClickListener(v -> {
            dialog.dismiss();
            showLogoutConfirmation();
        });
        dialogView.findViewById(R.id.btnProfileDeleteAccount).setOnClickListener(v -> {
            dialog.dismiss();
            showDeleteAccountConfirmation();
        });
        dialogView.findViewById(R.id.btnProfileActionsClose).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void openPostDetail(Post post) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.getPostId());
        intent.putExtra(PostDetailActivity.EXTRA_POST_UID, post.getUid());
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

        startActivity(intent);
    }

    private void openConnections(String mode) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return;
        }

        Intent intent = new Intent(this, UserConnectionsActivity.class);
        intent.putExtra(UserConnectionsActivity.EXTRA_USER_ID, user.getUid());
        intent.putExtra(UserConnectionsActivity.EXTRA_MODE, mode);
        startActivity(intent);
    }

    private void showEditDialog() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_profile_edit, null);
        TextInputEditText nameInput = dialogView.findViewById(R.id.inputName);
        TextInputEditText usernameInput = dialogView.findViewById(R.id.inputUsername);
        TextInputEditText bioInput = dialogView.findViewById(R.id.inputBio);

        nameInput.setText(currentDisplayName);
        usernameInput.setText(currentUsername);
        CharSequence currentBio = binding.tvBio.getText();
        if (!TextUtils.equals(currentBio, getString(R.string.profile_bio_empty))) {
            bioInput.setText(currentBio);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {

            String newDisplayName = nameInput.getText() == null
                    ? ""
                    : nameInput.getText().toString().trim();

            String newUsername = UsernameHelper.normalize(usernameInput.getText() == null
                    ? ""
                    : usernameInput.getText().toString());

            String newBio = bioInput.getText() == null
                    ? ""
                    : bioInput.getText().toString().trim();

            if (TextUtils.isEmpty(newDisplayName)) {
                nameInput.setError(getString(R.string.error_name_required));
                return;
            }

            if (TextUtils.isEmpty(newUsername)) {
                usernameInput.setError(getString(R.string.error_username_required));
                return;
            }

            if (!UsernameHelper.isValid(newUsername)) {
                usernameInput.setError(getString(R.string.error_username_invalid));
                return;
            }

            v.setEnabled(false); // disable button

            updateProfile(user.getUid(), newDisplayName, newUsername, newBio);

            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateProfile(String uid, String newDisplayName, String newUsername, String newBio) {

        if (TextUtils.isEmpty(newDisplayName)) {
            Toast.makeText(this, getString(R.string.error_name_required), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!UsernameHelper.isValid(newUsername)) {
            Toast.makeText(this, getString(R.string.error_username_invalid), Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnEdit.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);
        verifyLegacyUsernameAvailability(newUsername, uid, () -> commitProfileUpdate(uid, newDisplayName, newUsername, newBio));
    }

    private void commitProfileUpdate(String uid, String newDisplayName, String newUsername, String newBio) {
        DocumentReference userRef = db.collection("users").document(uid);
        DocumentReference newUsernameRef = db.collection("usernames").document(newUsername);
        DocumentReference oldUsernameRef = TextUtils.isEmpty(currentUsername)
                ? null
                : db.collection("usernames").document(currentUsername);
        boolean usernameChanged = !newUsername.equals(currentUsername);
        boolean displayNameChanged = !newDisplayName.equals(currentDisplayName);

        db.runTransaction(transaction -> {
                    if (usernameChanged) {
                        DocumentSnapshot usernameSnapshot = transaction.get(newUsernameRef);
                        if (usernameSnapshot.exists()) {
                            String existingUid = usernameSnapshot.getString("uid");
                            if (!uid.equals(existingUid)) {
                                throw new IllegalStateException(getString(R.string.error_username_taken));
                            }
                        }
                    }

                    Map<String, Object> profileUpdates = new HashMap<>();
                    profileUpdates.put("uid", uid);
                    profileUpdates.put("name", newDisplayName);
                    profileUpdates.put("username", newUsername);
                    profileUpdates.put("bio", newBio);
                    profileUpdates.put("email", auth.getCurrentUser() == null ? "" : auth.getCurrentUser().getEmail());
                    profileUpdates.put("profileImageUrl", "");
                    profileUpdates.put("followerCount", 0);
                    profileUpdates.put("followingCount", 0);

                    transaction.set(userRef, profileUpdates, SetOptions.merge());

                    if (usernameChanged) {
                        if (oldUsernameRef != null) {
                            transaction.delete(oldUsernameRef);
                        }

                        transaction.set(newUsernameRef, new java.util.HashMap<String, Object>() {{
                            put("uid", uid);
                            put("username", newUsername);
                            put("createdAt", FieldValue.serverTimestamp());
                        }});
                    }

                    return null;
                })
                .addOnSuccessListener(unused -> {
                    updateAuthorIdentityOnPosts(uid, newDisplayName, newUsername, newBio);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnEdit.setEnabled(true);
                    Toast.makeText(
                            this,
                            e.getMessage() == null ? "Failed to update profile" : e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void verifyLegacyUsernameAvailability(String username, String currentUid, Runnable onAvailable) {
        db.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        onAvailable.run();
                        return;
                    }

                    String existingUid = querySnapshot.getDocuments().get(0).getId();
                    if (currentUid.equals(existingUid)) {
                        onAvailable.run();
                        return;
                    }

                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnEdit.setEnabled(true);
                    Toast.makeText(this, getString(R.string.error_username_taken), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnEdit.setEnabled(true);
                    Toast.makeText(this, R.string.profile_load_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteSavedFromAllUsers(String postId) {
        collectSavedRefsForPost(postId, new RefsListener() {
            @Override
            public void onSuccess(List<DocumentReference> refs) {
                deleteRefsInBatches(refs, new CompletionListener() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failed to clean saved references for post " + postId, e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to collect saved references for post " + postId, e);
            }
        });
    }

    private void deletePost(Post post) {
        setAccountActionsEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        deletePostData(post, new CompletionListener() {
            @Override
            public void onSuccess() {
                postAdapter.removePostFromUI(post.getPostId());
                myPosts.remove(post);
                binding.tvPostsCount.setText(String.valueOf(myPosts.size()));
                binding.tvPostsSectionMeta.setText(
                        String.format(Locale.getDefault(), "%d posts", myPosts.size())
                );
                finishPostLoading();
                setAccountActionsEnabled(true);
                Toast.makeText(ProfileActivity.this, "Post deleted successfully", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                setAccountActionsEnabled(true);
                Toast.makeText(ProfileActivity.this, "Failed to delete post", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateAuthorIdentityOnPosts(String uid, String newDisplayName, String newUsername, String newBio) {
        db.collection("posts")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        DocumentReference ref = db.collection("posts").document(document.getId());
                        batch.update(ref, "name", newDisplayName, "username", newUsername);
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                applyProfileUi(newDisplayName, newUsername, newBio);
                                Toast.makeText(
                                        this,
                                        R.string.profile_username_updated,
                                        Toast.LENGTH_SHORT
                                ).show();
                            })
                            .addOnFailureListener(e -> {
                                binding.btnEdit.setEnabled(true);
                                Toast.makeText(
                                        this,
                                        R.string.profile_load_failed,
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {
                    binding.btnEdit.setEnabled(true);
                    Toast.makeText(this, R.string.profile_load_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private void applyProfileUi(String displayName, String username, String bio) {
        currentDisplayName = displayName;
        currentUsername = username;
        binding.progressBar.setVisibility(View.GONE);
        binding.btnEdit.setEnabled(true);
        binding.tvUsername.setText(displayName);
        binding.tvEmail.setText(UserDisplayHelper.resolveHandle(username));
        binding.tvBio.setText(resolveBio(bio));
        binding.tvAvatarInitial.setText(UserDisplayHelper.resolveInitial(
                displayName,
                getString(R.string.profile_default_username)
        ));

        getSharedPreferences("cookio_prefs", MODE_PRIVATE)
                .edit()
                .putString("display_name", displayName)
                .putString("username", username)
                .apply();
    }

    private void updateProfileImageOnPosts(String uid, String profileImageUrl, CompletionListener listener) {
        db.collection("posts")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        batch.update(document.getReference(), "profileImageUrl", profileImageUrl);
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> listener.onSuccess())
                            .addOnFailureListener(listener::onFailure);
                })
                .addOnFailureListener(listener::onFailure);
    }

    private String resolveBio(@Nullable String bio) {
        if (TextUtils.isEmpty(bio)) {
            return getString(R.string.profile_bio_empty);
        }
        return bio;
    }

    private void showLogoutConfirmation() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_logout_confirm, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnStay).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnLogoutConfirm).setOnClickListener(v -> {
            dialog.dismiss();
            auth.signOut();
            Intent intent = new Intent(this, LandingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        dialog.show();
    }

    private void showDeleteAccountConfirmation() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_destructive_confirm, null, false);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ((TextView) dialogView.findViewById(R.id.tvDialogEyebrow)).setText(getString(R.string.profile_account_label));
        ((TextView) dialogView.findViewById(R.id.tvDialogTitle)).setText(R.string.delete_account_confirm_title);
        ((TextView) dialogView.findViewById(R.id.tvDialogMessage)).setText(R.string.delete_account_confirm_message);

        dialogView.findViewById(R.id.btnDialogConfirm).setOnClickListener(v -> {
            dialog.dismiss();
            promptDeleteAccountPassword();
        });
        dialogView.findViewById(R.id.btnDialogCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void promptDeleteAccountPassword() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_password_confirm, null, false);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextInputEditText passwordInput = dialogView.findViewById(R.id.etPasswordConfirm);

        dialogView.findViewById(R.id.btnPasswordConfirm).setOnClickListener(v -> {
            String password = passwordInput.getText() == null
                    ? ""
                    : passwordInput.getText().toString().trim();

            if (TextUtils.isEmpty(password)) {
                Toast.makeText(this, R.string.delete_account_password_required, Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
            reauthenticateAndDeleteAccount(user, password);
        });

        dialogView.findViewById(R.id.btnPasswordCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void reauthenticateAndDeleteAccount(FirebaseUser user, String password) {
        String email = user.getEmail();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, R.string.delete_account_reauth_required, Toast.LENGTH_LONG).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        user.reauthenticate(credential)
                .addOnSuccessListener(unused -> deleteCurrentAccount())
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        e.getMessage() == null ? getString(R.string.delete_account_reauth_required) : e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }

    private void deleteCurrentAccount() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return;
        }

        setAccountActionsEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(this, R.string.delete_account_progress, Toast.LENGTH_SHORT).show();

        cleanupAccountData(user, new CompletionListener() {
            @Override
            public void onSuccess() {
                user.delete()
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(ProfileActivity.this, R.string.delete_account_success, Toast.LENGTH_SHORT).show();
                            auth.signOut();
                            Intent intent = new Intent(ProfileActivity.this, LandingActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            binding.progressBar.setVisibility(View.GONE);
                            setAccountActionsEnabled(true);
                            if (e instanceof FirebaseAuthRecentLoginRequiredException) {
                                Toast.makeText(ProfileActivity.this, R.string.delete_account_reauth_required, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(ProfileActivity.this, R.string.delete_account_failed, Toast.LENGTH_LONG).show();
                            }
                        });
            }

            @Override
            public void onFailure(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                setAccountActionsEnabled(true);
                Log.e(TAG, "Failed to clean up account data", e);
                Toast.makeText(ProfileActivity.this, R.string.delete_account_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void cleanupAccountData(FirebaseUser user, CompletionListener listener) {
        String uid = user.getUid();
        String profileImageUrl = getSharedPreferences("cookio_prefs", MODE_PRIVATE)
                .getString("photoUrl", "");

        loadPostsByUser(uid, new PostsListener() {
            @Override
            public void onSuccess(List<Post> posts) {
                deleteUserAuthoredPosts(0, posts, new CompletionListener() {
                    @Override
                    public void onSuccess() {
                        collectAccountCleanupRefs(uid, new RefsListener() {
                            @Override
                            public void onSuccess(List<DocumentReference> refs) {
                                deleteRefsInBatches(refs, new CompletionListener() {
                                    @Override
                                    public void onSuccess() {
                                        cleanupConnectionsAndUserDoc(uid, new CompletionListener() {
                                            @Override
                                            public void onSuccess() {
                                                deleteImageByUrl(profileImageUrl, listener);
                                            }

                                            @Override
                                            public void onFailure(Exception e) {
                                                listener.onFailure(e);
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        listener.onFailure(e);
                                    }
                                });
                            }

                            @Override
                            public void onFailure(Exception e) {
                                listener.onFailure(e);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    private void loadPostsByUser(String userId, PostsListener listener) {
        db.collection("posts")
                .whereEqualTo("uid", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Post> posts = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Post post = doc.toObject(Post.class);
                        post.setPostId(doc.getId());
                        posts.add(post);
                    }
                    listener.onSuccess(posts);
                })
                .addOnFailureListener(listener::onFailure);
    }

    private void deleteUserAuthoredPosts(int index, List<Post> posts, CompletionListener listener) {
        if (index >= posts.size()) {
            listener.onSuccess();
            return;
        }

        deletePostData(posts.get(index), new CompletionListener() {
            @Override
            public void onSuccess() {
                deleteUserAuthoredPosts(index + 1, posts, listener);
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    private void deletePostData(Post post, CompletionListener listener) {
        String postId = post.getPostId();
        if (TextUtils.isEmpty(postId)) {
            listener.onSuccess();
            return;
        }

        collectCommentRefsForPost(postId, new RefsListener() {
            @Override
            public void onSuccess(List<DocumentReference> commentRefs) {
                collectPostLikeRefs(postId, new RefsListener() {
                    @Override
                    public void onSuccess(List<DocumentReference> likeRefs) {
                        collectSavedRefsForPost(postId, new RefsListener() {
                            @Override
                            public void onSuccess(List<DocumentReference> savedRefs) {
                                collectNotificationRefsForPost(postId, new RefsListener() {
                                    @Override
                                    public void onSuccess(List<DocumentReference> notificationRefs) {
                                        List<DocumentReference> refs = new ArrayList<>();
                                        refs.addAll(commentRefs);
                                        refs.addAll(likeRefs);
                                        refs.addAll(savedRefs);
                                        refs.addAll(notificationRefs);
                                        refs.add(db.collection("posts").document(postId));

                                        deleteRefsInBatches(refs, new CompletionListener() {
                                            @Override
                                            public void onSuccess() {
                                                deleteImageByUrl(post.getImageUrl(), listener);
                                            }

                                            @Override
                                            public void onFailure(Exception e) {
                                                listener.onFailure(e);
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        listener.onFailure(e);
                                    }
                                });
                            }

                            @Override
                            public void onFailure(Exception e) {
                                listener.onFailure(e);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    private void collectCommentRefsForPost(String postId, RefsListener listener) {
        db.collection("posts")
                .document(postId)
                .collection("comments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentReference> refs = new ArrayList<>();
                    List<DocumentSnapshot> comments = queryDocumentSnapshots.getDocuments();

                    if (comments.isEmpty()) {
                        listener.onSuccess(refs);
                        return;
                    }

                    final int[] remaining = {comments.size()};
                    for (DocumentSnapshot commentDoc : comments) {
                        refs.add(commentDoc.getReference());
                        commentDoc.getReference()
                                .collection("likes")
                                .get()
                                .addOnSuccessListener(likesSnapshot -> {
                                    for (DocumentSnapshot likeDoc : likesSnapshot.getDocuments()) {
                                        refs.add(likeDoc.getReference());
                                    }

                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        listener.onSuccess(refs);
                                    }
                                })
                                .addOnFailureListener(listener::onFailure);
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }

    private void collectPostLikeRefs(String postId, RefsListener listener) {
        db.collection("posts")
                .document(postId)
                .collection("likes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentReference> refs = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        refs.add(doc.getReference());
                    }
                    listener.onSuccess(refs);
                })
                .addOnFailureListener(listener::onFailure);
    }

    private void collectSavedRefsForPost(String postId, RefsListener listener) {
        db.collection("users")
                .get()
                .addOnSuccessListener(usersSnapshot -> {
                    List<DocumentReference> refs = new ArrayList<>();
                    for (DocumentSnapshot userDoc : usersSnapshot.getDocuments()) {
                        refs.add(db.collection("users")
                                .document(userDoc.getId())
                                .collection("savedPosts")
                                .document(postId));
                    }
                    listener.onSuccess(refs);
                })
                .addOnFailureListener(listener::onFailure);
    }

    private void collectNotificationRefsForPost(String postId, RefsListener listener) {
        db.collectionGroup("items")
                .whereEqualTo("postId", postId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentReference> refs = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        refs.add(doc.getReference());
                    }
                    listener.onSuccess(refs);
                })
                .addOnFailureListener(listener::onFailure);
    }

    private void collectAccountCleanupRefs(String uid, RefsListener listener) {
        db.collection("users")
                .document(uid)
                .collection("savedPosts")
                .get()
                .addOnSuccessListener(savedSnapshot -> {
                    List<DocumentReference> refs = new ArrayList<>();
                    for (DocumentSnapshot doc : savedSnapshot.getDocuments()) {
                        refs.add(doc.getReference());
                    }

                    db.collection("notifications")
                            .document(uid)
                            .collection("items")
                            .get()
                            .addOnSuccessListener(ownNotifications -> {
                                for (DocumentSnapshot doc : ownNotifications.getDocuments()) {
                                    refs.add(doc.getReference());
                                }

                                db.collectionGroup("items")
                                        .whereEqualTo("fromUid", uid)
                                        .get()
                                        .addOnSuccessListener(sentNotifications -> {
                                            for (DocumentSnapshot doc : sentNotifications.getDocuments()) {
                                                refs.add(doc.getReference());
                                            }

                                            collectInteractionRefsForUser(uid, refs, listener);
                                        })
                                        .addOnFailureListener(listener::onFailure);
                            })
                            .addOnFailureListener(listener::onFailure);
                })
                .addOnFailureListener(listener::onFailure);
    }

    private void collectInteractionRefsForUser(String uid, List<DocumentReference> baseRefs, RefsListener listener) {
        db.collection("posts")
                .get()
                .addOnSuccessListener(postsSnapshot -> {
                    List<DocumentSnapshot> posts = postsSnapshot.getDocuments();
                    if (posts.isEmpty()) {
                        listener.onSuccess(baseRefs);
                        return;
                    }

                    final int[] remaining = {posts.size()};
                    for (DocumentSnapshot postDoc : posts) {
                        baseRefs.add(postDoc.getReference().collection("likes").document(uid));

                        postDoc.getReference()
                                .collection("comments")
                                .get()
                                .addOnSuccessListener(commentsSnapshot -> {
                                    List<DocumentSnapshot> comments = commentsSnapshot.getDocuments();
                                    if (comments.isEmpty()) {
                                        remaining[0]--;
                                        if (remaining[0] == 0) {
                                            listener.onSuccess(baseRefs);
                                        }
                                        return;
                                    }

                                    final int[] commentRemaining = {comments.size()};
                                    for (DocumentSnapshot commentDoc : comments) {
                                        baseRefs.add(commentDoc.getReference().collection("likes").document(uid));

                                        if (uid.equals(commentDoc.getString("userId"))) {
                                            commentDoc.getReference()
                                                    .collection("likes")
                                                    .get()
                                                    .addOnSuccessListener(commentLikes -> {
                                                        for (DocumentSnapshot likeDoc : commentLikes.getDocuments()) {
                                                            baseRefs.add(likeDoc.getReference());
                                                        }
                                                        baseRefs.add(commentDoc.getReference());

                                                        commentRemaining[0]--;
                                                        if (commentRemaining[0] == 0) {
                                                            remaining[0]--;
                                                            if (remaining[0] == 0) {
                                                                listener.onSuccess(baseRefs);
                                                            }
                                                        }
                                                    })
                                                    .addOnFailureListener(listener::onFailure);
                                        } else {
                                            commentRemaining[0]--;
                                            if (commentRemaining[0] == 0) {
                                                remaining[0]--;
                                                if (remaining[0] == 0) {
                                                    listener.onSuccess(baseRefs);
                                                }
                                            }
                                        }
                                    }
                                })
                                .addOnFailureListener(listener::onFailure);
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }

    private void cleanupConnectionsAndUserDoc(String uid, CompletionListener listener) {
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(userDoc ->
                        db.collection("users")
                                .document(uid)
                                .collection("followers")
                                .get()
                                .addOnSuccessListener(followersSnapshot ->
                                        db.collection("users")
                                                .document(uid)
                                                .collection("following")
                                                .get()
                                                .addOnSuccessListener(followingSnapshot -> {
                                                    WriteBatch batch = db.batch();

                                                    for (DocumentSnapshot followerDoc : followersSnapshot.getDocuments()) {
                                                        String followerId = followerDoc.getId();
                                                        batch.delete(followerDoc.getReference());
                                                        batch.delete(db.collection("users")
                                                                .document(followerId)
                                                                .collection("following")
                                                                .document(uid));
                                                        batch.update(db.collection("users").document(followerId),
                                                                "followingCount", FieldValue.increment(-1));
                                                    }

                                                    for (DocumentSnapshot followingDoc : followingSnapshot.getDocuments()) {
                                                        String followingId = followingDoc.getId();
                                                        batch.delete(followingDoc.getReference());
                                                        batch.delete(db.collection("users")
                                                                .document(followingId)
                                                                .collection("followers")
                                                                .document(uid));
                                                        batch.update(db.collection("users").document(followingId),
                                                                "followerCount", FieldValue.increment(-1));
                                                    }

                                                    String username = userDoc.getString("username");
                                                    if (!TextUtils.isEmpty(username)) {
                                                        batch.delete(db.collection("usernames").document(username));
                                                    }

                                                    batch.delete(db.collection("users").document(uid));
                                                    batch.commit()
                                                            .addOnSuccessListener(unused -> listener.onSuccess())
                                                            .addOnFailureListener(listener::onFailure);
                                                })
                                                .addOnFailureListener(listener::onFailure))
                                .addOnFailureListener(listener::onFailure))
                .addOnFailureListener(listener::onFailure);
    }

    private void deleteRefsInBatches(List<DocumentReference> refs, CompletionListener listener) {
        deleteRefsInBatches(refs, 0, listener);
    }

    private void deleteRefsInBatches(List<DocumentReference> refs, int startIndex, CompletionListener listener) {
        if (startIndex >= refs.size()) {
            listener.onSuccess();
            return;
        }

        int endIndex = Math.min(startIndex + 450, refs.size());
        WriteBatch batch = db.batch();
        for (int i = startIndex; i < endIndex; i++) {
            batch.delete(refs.get(i));
        }

        batch.commit()
                .addOnSuccessListener(unused -> deleteRefsInBatches(refs, endIndex, listener))
                .addOnFailureListener(listener::onFailure);
    }

    private void deleteImageByUrl(@Nullable String imageUrl, CompletionListener listener) {
        if (TextUtils.isEmpty(imageUrl)) {
            listener.onSuccess();
            return;
        }

        try {
            storage.getReferenceFromUrl(imageUrl)
                    .delete()
                    .addOnSuccessListener(unused -> listener.onSuccess())
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to delete image from storage: " + imageUrl, e);
                        listener.onSuccess();
                    });
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Invalid storage URL: " + imageUrl, e);
            listener.onSuccess();
        }
    }

    private void setAccountActionsEnabled(boolean enabled) {
        binding.btnEdit.setEnabled(enabled);
        binding.btnCreatePost.setEnabled(enabled);
        binding.btnSavedPosts.setEnabled(enabled);
        binding.btnProfileMenu.setEnabled(enabled);
        binding.btnToggleView.setEnabled(enabled);
        binding.ivProfilePhoto.setEnabled(enabled);
        binding.tvAvatarInitial.setEnabled(enabled);
    }
}
