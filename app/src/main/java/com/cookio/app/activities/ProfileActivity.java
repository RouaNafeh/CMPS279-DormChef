package com.cookio.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.cookio.app.R;
import com.cookio.app.adapters.PostAdapter;
import com.cookio.app.databinding.ActivityProfileBinding;
import com.google.android.material.textfield.TextInputEditText;
import com.cookio.app.models.Post;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";

    private ActivityProfileBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private final List<Post> myPosts = new ArrayList<>();
    private final Set<String> savedPostIds = new HashSet<>();
    private final Set<String> likedPostIds = new HashSet<>();

    private PostAdapter postAdapter;
    private boolean isGrid = false;

    private final ActivityResultLauncher<String> profileImagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadProfilePhoto(uri);
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
        binding.btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        binding.cardFollowers.setOnClickListener(v -> openConnections(UserConnectionsActivity.MODE_FOLLOWERS));
        binding.cardFollowing.setOnClickListener(v -> openConnections(UserConnectionsActivity.MODE_FOLLOWING));
        binding.ivProfilePhoto.setOnClickListener(v -> profileImagePickerLauncher.launch("image/*"));
        binding.tvAvatarInitial.setOnClickListener(v -> profileImagePickerLauncher.launch("image/*"));

        binding.btnToggleView.setOnClickListener(v -> {
            isGrid = !isGrid;
            setLayoutManager();
            postAdapter.setGridMode(isGrid);
            binding.btnToggleView.setText(isGrid ? "List View" : "Grid View");
        });

        setupBottomNavigation();
        populateStaticUserFields();
        setLayoutManager();
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

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = binding.bottomNavigation.bottomNavigation;
        bottomNavigation.setSelectedItemId(R.id.nav_my_recipes);
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
        binding.tvEmail.setText(email);
        binding.tvUsername.setText(resolveDisplayName(null, email));
        binding.tvAvatarInitial.setText(resolveInitial(binding.tvUsername.getText().toString()));
        binding.tvFollowersCount.setText("0");
        binding.tvFollowingCount.setText("0");
    }

    private void showDeleteDialog(Post post) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post?")
                .setPositiveButton("Delete", (dialog, which) -> deletePost(post))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPostActionsDialog(Post post) {
        String[] options = {"Edit Post", "Delete Post"};

        new AlertDialog.Builder(this)
                .setTitle(post.getTitle())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openEditPost(post);
                    } else if (which == 1) {
                        showDeleteDialog(post);
                    }
                })
                .show();
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
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        loadUserInfo(user);
        loadSavedPostIds();
        loadMyPosts(user);
    }

    private void loadUserInfo(FirebaseUser user) {
        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String email = user.getEmail();
                    String username = documentSnapshot.getString("username");
                    String bio = documentSnapshot.getString("bio");
                    String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                    Long followerCount = documentSnapshot.getLong("followerCount");
                    Long followingCount = documentSnapshot.getLong("followingCount");


                    getSharedPreferences("cookio_prefs", MODE_PRIVATE)
                            .edit()
                            .putString("username", resolveDisplayName(username, email))
                            .putString("photoUrl", profileImageUrl == null ? "" : profileImageUrl)
                            .apply();

                    binding.tvUsername.setText(resolveDisplayName(username, email));
                    binding.tvEmail.setText(email);
                    binding.tvBio.setText(resolveBio(bio));
                    binding.tvAvatarInitial.setText(resolveInitial(binding.tvUsername.getText().toString()));
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
                    .placeholder(R.drawable.logo_cropped)
                    .error(R.drawable.logo_cropped)
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
                                            setProfilePhotoUploadEnabled(true);
                                            loadProfilePhoto(downloadUri.toString());

                                            Toast.makeText(this,
                                                    R.string.profile_photo_updated,
                                                    Toast.LENGTH_SHORT).show();
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

    private void openPostDetail(Post post) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.getPostId());
        intent.putExtra(PostDetailActivity.EXTRA_POST_UID, post.getUid());
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
        TextInputEditText usernameInput = dialogView.findViewById(R.id.inputUsername);
        TextInputEditText bioInput = dialogView.findViewById(R.id.inputBio);

        usernameInput.setText(binding.tvUsername.getText());
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

            String newName = usernameInput.getText() == null
                    ? ""
                    : usernameInput.getText().toString().trim();

            String newBio = bioInput.getText() == null
                    ? ""
                    : bioInput.getText().toString().trim();

            if (TextUtils.isEmpty(newName)) {
                usernameInput.setError("Username required");
                return;
            }

            if (newName.length() < 3) {
                usernameInput.setError("Min 3 characters");
                return;
            }

            v.setEnabled(false); // disable button

            updateProfile(user.getUid(), newName, newBio);

            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateProfile(String uid, String newName, String newBio) {

        // 1. VALIDATION
        if (TextUtils.isEmpty(newName)) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newName.length() < 3) {
            Toast.makeText(this, "Username must be at least 3 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. UI LOCK
        binding.btnEdit.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        db.collection("users")
                .document(uid)
                .update("username", newName, "bio", newBio)
                .addOnSuccessListener(unused -> {

                    updateUsernameOnPosts(uid, newName, newBio);

                    // UI restore
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnEdit.setEnabled(true);

                    binding.tvUsername.setText(newName);
                    binding.tvBio.setText(resolveBio(newBio));
                    binding.tvAvatarInitial.setText(resolveInitial(newName));

                    Toast.makeText(this,
                            "Profile updated successfully",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {

                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnEdit.setEnabled(true);

                    Toast.makeText(this,
                            "Failed to update profile",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteSavedFromAllUsers(String postId) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .get()
                .addOnSuccessListener(usersSnapshot -> {

                    WriteBatch batch = db.batch();

                    for (DocumentSnapshot userDoc : usersSnapshot) {

                        DocumentReference savedRef = db.collection("users")
                                .document(userDoc.getId())
                                .collection("savedPosts")
                                .document(postId);

                        batch.delete(savedRef);
                    }

                    batch.commit();
                });
    }

    private void deletePost(Post post) {
        String postId = post.getPostId();
        String uid = auth.getCurrentUser().getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference postRef = db.collection("posts").document(postId);

        // 1. Delete likes first
        db.collection("posts")
                .document(postId)
                .collection("likes")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    WriteBatch batch = db.batch();

                    for (DocumentSnapshot doc : querySnapshot) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> {

                                // 2. Delete saved post for CURRENT user
                                db.collection("users")
                                        .document(uid)
                                        .collection("savedPosts")
                                        .document(postId)
                                        .delete()
                                        .addOnSuccessListener(unused2 -> {

                                            // 3. DELETE SAVED POST FROM ALL USERS (OPTION 1 FIX)
                                            deleteSavedFromAllUsers(postId);

                                            // 4. Delete post itself
                                            postRef.delete()
                                                    .addOnSuccessListener(unused3 -> {

                                                        // 5. Update UI
                                                        postAdapter.removePostFromUI(postId);
                                                        myPosts.remove(post);

                                                        binding.tvPostsCount.setText(
                                                                String.valueOf(myPosts.size())
                                                        );

                                                        Toast.makeText(
                                                                this,
                                                                "Post deleted successfully",
                                                                Toast.LENGTH_SHORT
                                                        ).show();

                                                    })
                                                    .addOnFailureListener(e ->
                                                            Toast.makeText(this,
                                                                    "Failed to delete post",
                                                                    Toast.LENGTH_SHORT).show()
                                                    );

                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this,
                                                        "Failed to remove saved post",
                                                        Toast.LENGTH_SHORT).show()
                                        );

                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Failed to delete likes",
                                            Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load likes",
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void updateUsernameOnPosts(String uid, String newName, String newBio) {
        db.collection("posts")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        DocumentReference ref = db.collection("posts").document(document.getId());
                        batch.update(ref, "username", newName);
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                binding.btnEdit.setEnabled(true);
                                binding.tvUsername.setText(newName);
                                binding.tvBio.setText(resolveBio(newBio));
                                binding.tvAvatarInitial.setText(resolveInitial(newName));
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
            return getString(R.string.profile_default_username).substring(0, 1).toUpperCase(Locale.getDefault());
        }
        return value.substring(0, 1).toUpperCase(Locale.getDefault());
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
}
