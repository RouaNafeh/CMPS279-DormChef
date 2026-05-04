package com.cookio.app.activities;

import com.cookio.app.utils.NotificationHelper;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RatingBar;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.cookio.app.R;
import com.cookio.app.models.CookingStep;
import com.cookio.app.models.CookingStepParser;
import com.cookio.app.utils.CookTimeFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.cookio.app.models.Comment;
import com.cookio.app.adapters.CommentAdapter;
import com.google.firebase.firestore.Query;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.text.DateFormat;
import java.util.Set;

public class PostDetailActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "post_id";
    public static final String EXTRA_POST_TITLE = "post_title";
    public static final String EXTRA_POST_DESCRIPTION = "post_description";
    public static final String EXTRA_POST_IMAGE_URL = "post_image_url";
    public static final String EXTRA_POST_COOK_TIME = "post_cook_time";
    public static final String EXTRA_POST_BUDGET = "post_budget";
    public static final String EXTRA_POST_AUTHOR_NAME = "post_author_name";
    public static final String EXTRA_POST_USERNAME = "post_username";
    public static final String EXTRA_POST_UID = "post_uid";
    public static final String EXTRA_POST_INGREDIENTS = "post_ingredients";
    public static final String EXTRA_POST_EQUIPMENT = "post_equipment";
    public static final String EXTRA_POST_STEPS = "post_steps";
    public static final String EXTRA_POST_LIKES_COUNT = "post_likes_count";

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String postId;
    private String currentUid;
    private String authorUid;
    private String authorUsername;
    private int likesCount;
    private boolean isSaved;
    private boolean isLiked;

    private ImageView ivImage;
    private ImageView ivAuthorAvatar;
    private TextView tvTitle;
    private TextView tvUsername;
    private TextView tvAuthorAvatarInitial;
    private TextView tvCreatedAt;
    private TextView tvDescription;
    private TextView tvCookTime;
    private TextView tvBudget;
    private TextView tvDetailLikesCount;
    private TextView tvReviewsCount;
    private ChipGroup cgIngredients;
    private ChipGroup cgEquipment;
    private LinearLayout llSteps;
    private MaterialButton btnSavePost;
    private MaterialButton btnLikePost;
    private MaterialButton btnCookingMode;
    private List<CookingStep> cookingSteps = new ArrayList<>();
    private Set<String> likedCommentIds = new HashSet<>();
    private RecyclerView rvComments;
    private EditText etComment;
    private MaterialButton btnSendComment;
    private CommentAdapter commentAdapter;
    private RatingBar ratingBarComment;
    private RatingBar ratingBarAvg;

    private TextView tvAvgRating;

    private List<Comment> commentList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        likesCount = getIntent().getIntExtra(EXTRA_POST_LIKES_COUNT, 0);

        bindViews();
        bindStaticContentFromIntent();
        updateSaveButton();
        updateLikeButton();
        updateLikesCount();

        setupDropdown(findViewById(R.id.ingredientsHeader), findViewById(R.id.chipGroupIngredients), findViewById(R.id.ingredientsArrow));
        setupDropdown(findViewById(R.id.equipmentHeader), findViewById(R.id.chipGroupEquipment), findViewById(R.id.equipmentArrow));
        setupDropdown(findViewById(R.id.stepsHeader), findViewById(R.id.stepsContainer), findViewById(R.id.stepsArrow));

        rvComments = findViewById(R.id.rvComments);
        etComment = findViewById(R.id.etComment);
        btnSendComment = findViewById(R.id.btnSendComment);
        ratingBarComment = findViewById(R.id.ratingBarComment);
        ratingBarAvg = findViewById(R.id.ratingBarAvg);
        tvAvgRating = findViewById(R.id.tvAvgRating);

        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(
                commentList,
                likedCommentIds,
                currentUid,
                this::likeComment,
                this::deleteComment
        );        rvComments.setAdapter(commentAdapter);

        loadComments();
        setupCommentButton();
        updateAverageRating();


        findViewById(R.id.btnBack).setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed());
        tvUsername.setOnClickListener(v -> openPublicProfile());
        btnSavePost.setOnClickListener(v -> toggleSave());
        btnLikePost.setOnClickListener(v -> toggleLike());
        btnCookingMode.setOnClickListener(v -> openCookingMode());
        tvDetailLikesCount.setOnClickListener(v -> openLikesList());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPostDetails();
        loadEngagementState();
    }
    private void setupDropdown(View header, View content, View indicator) {
        header.setOnClickListener(v -> {
            if (content.getVisibility() == View.GONE) {
                content.setVisibility(View.VISIBLE);
                indicator.animate().rotation(180f).setDuration(180).start();
            } else {
                content.setVisibility(View.GONE);
                indicator.animate().rotation(0f).setDuration(180).start();
            }
        });
    }

    private void bindViews() {
        ivImage = findViewById(R.id.ivPostImage);
        ivAuthorAvatar = findViewById(R.id.ivAuthorAvatar);
        tvTitle = findViewById(R.id.tvPostTitle);
        tvUsername = findViewById(R.id.tvPostUsername);
        tvAuthorAvatarInitial = findViewById(R.id.tvAuthorAvatarInitial);
        tvCreatedAt = findViewById(R.id.tvPostCreatedAt);
        tvDescription = findViewById(R.id.tvPostDescription);
        tvCookTime = findViewById(R.id.tvPostCookTime);
        tvBudget = findViewById(R.id.tvPostBudget);
        tvDetailLikesCount = findViewById(R.id.tvDetailLikesCount);
        tvReviewsCount = findViewById(R.id.tvReviewsCount);
        cgIngredients = findViewById(R.id.chipGroupIngredients);
        cgEquipment = findViewById(R.id.chipGroupEquipment);
        llSteps = findViewById(R.id.stepsContainer);
        btnSavePost = findViewById(R.id.btnSavePost);
        btnLikePost = findViewById(R.id.btnLikePost);
        btnCookingMode = findViewById(R.id.btnCookingMode);
    }
    private void setupCommentButton() {
        btnSendComment.setOnClickListener(v -> {
            if (currentUid == null) {
                Toast.makeText(this, getString(R.string.auth_failed_message), Toast.LENGTH_SHORT).show();
                return;
            }

            String text = etComment.getText().toString().trim();

            if (text.isEmpty()) {
                Toast.makeText(this, "Write a comment first", Toast.LENGTH_SHORT).show();
                return;
            }

            String displayName = getSharedPreferences("cookio_prefs", MODE_PRIVATE)
                    .getString("display_name", "Chef");
            String username = getSharedPreferences("cookio_prefs", MODE_PRIVATE)
                    .getString("username", "");

            float rating = ratingBarComment.getRating();

            if (rating == 0) {
                Toast.makeText(this, "Please choose a rating", Toast.LENGTH_SHORT).show();
                return;
            }

            Comment comment = new Comment(currentUid, displayName, username, text, rating);

            db.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .add(comment)
                    .addOnSuccessListener(doc -> {
                        etComment.setText("");
                        ratingBarComment.setRating(0);

                        String myPhotoUrl = getSharedPreferences("cookio_prefs", MODE_PRIVATE)
                                .getString("photoUrl", "");

                        if (authorUid != null && currentUid != null && !authorUid.equals(currentUid)) {
                            NotificationHelper.sendCommentNotification(
                                    authorUid,
                                    currentUid,
                                    displayName,
                                    myPhotoUrl,
                                    postId,
                                    tvTitle.getText().toString(),
                                    text
                            );

                            NotificationHelper.sendReviewNotification(
                                    authorUid,
                                    currentUid,
                                    displayName,
                                    myPhotoUrl,
                                    postId,
                                    tvTitle.getText().toString(),
                                    Math.round(rating)
                            );
                        }

                        Toast.makeText(this, "Review added", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void loadComments() {
        db.collection("posts")
                .document(postId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;

                    commentList.clear();
                    updateReviewsCount(value.size());

                    for (DocumentSnapshot doc : value) {
                        Comment comment = doc.toObject(Comment.class);
                        if (comment != null) {
                            comment.setId(doc.getId());
                            commentList.add(comment);
                        }
                    }

                    loadLikedCommentIds();
                });
    }
    private void loadLikedCommentIds() {
        if (currentUid == null || postId == null || commentList.isEmpty()) {
            likedCommentIds.clear();
            commentAdapter.notifyDataSetChanged();
            return;
        }

        likedCommentIds.clear();

        final int[] remaining = {commentList.size()};

        for (Comment comment : commentList) {
            db.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .document(comment.getId())
                    .collection("likes")
                    .document(currentUid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            likedCommentIds.add(comment.getId());
                        }

                        remaining[0]--;
                        if (remaining[0] == 0) {
                            commentAdapter.notifyDataSetChanged();
                        }
                    })
                    .addOnFailureListener(e -> {
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            commentAdapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    private void likeComment(Comment comment) {
        if (currentUid == null || comment.getId() == null) {
            return;
        }

        DocumentReference commentRef = db.collection("posts")
                .document(postId)
                .collection("comments")
                .document(comment.getId());

        DocumentReference likeRef = commentRef
                .collection("likes")
                .document(currentUid);

        db.runTransaction(transaction -> {
            DocumentSnapshot likeSnap = transaction.get(likeRef);
            DocumentSnapshot commentSnap = transaction.get(commentRef);

            Long currentLikesObj = commentSnap.getLong("likesCount");
            long currentLikes = currentLikesObj == null ? 0 : currentLikesObj;

            if (likeSnap.exists()) {
                transaction.delete(likeRef);
                transaction.update(commentRef, "likesCount", Math.max(0, currentLikes - 1));
            } else {
                Map<String, Object> likeData = new HashMap<>();
                likeData.put("likedAt", FieldValue.serverTimestamp());

                transaction.set(likeRef, likeData);
                transaction.update(commentRef, "likesCount", currentLikes + 1);
            }

            return null;
        });
    }
    private void deleteComment(Comment comment) {
        if (currentUid == null || comment.getId() == null) {
            return;
        }

        if (!currentUid.equals(comment.getUserId())) {
            Toast.makeText(this, "You can only delete your own comment", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("posts")
                .document(postId)
                .collection("comments")
                .document(comment.getId())
                .delete()
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Comment deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete comment", Toast.LENGTH_SHORT).show());
    }

    private void updateAverageRating() {
        db.collection("posts")
                .document(postId)
                .collection("comments")
                .addSnapshotListener((value, error) -> {
                    if (value == null || value.isEmpty()) {
                        ratingBarAvg.setRating(0);
                        tvAvgRating.setText("New");
                        return;
                    }

                    float total = 0;
                    int count = 0;

                    for (DocumentSnapshot doc : value) {
                        Comment c = doc.toObject(Comment.class);
                        if (c != null) {
                            total += c.getRating();
                            count++;
                        }
                    }

                    if (count == 0) {
                        ratingBarAvg.setRating(0);
                        tvAvgRating.setText("New");
                        return;
                    }

                    float avg = total / count;
                    ratingBarAvg.setRating(avg);
                    tvAvgRating.setText(String.format("%.1f (%d reviews)", avg, count));
                });
    }

    private void bindStaticContentFromIntent() {
        authorUid = getIntent().getStringExtra(EXTRA_POST_UID);
        tvTitle.setText(getIntent().getStringExtra(EXTRA_POST_TITLE));
        authorUsername = getIntent().getStringExtra(EXTRA_POST_USERNAME);
        bindUsername(getIntent().getStringExtra(EXTRA_POST_AUTHOR_NAME));
        tvDescription.setText(getIntent().getStringExtra(EXTRA_POST_DESCRIPTION));
        tvCookTime.setText(resolveCookTime(getIntent().getStringExtra(EXTRA_POST_COOK_TIME)));
        tvBudget.setText(resolveBudget(getIntent().getStringExtra(EXTRA_POST_BUDGET)));
        bindImage(getIntent().getStringExtra(EXTRA_POST_IMAGE_URL));

        ArrayList<String> ingredients = getIntent().getStringArrayListExtra(EXTRA_POST_INGREDIENTS);
        ArrayList<String> equipment = getIntent().getStringArrayListExtra(EXTRA_POST_EQUIPMENT);
        ArrayList<String> steps = getIntent().getStringArrayListExtra(EXTRA_POST_STEPS);
        cookingSteps = CookingStepParser.parseList(steps);

        bindIngredients(ingredients);
        bindEquipment(equipment);
        bindSteps(cookingSteps);
    }

    private void loadPostDetails() {
        if (postId == null || postId.trim().isEmpty()) {
            return;
        }

        db.collection("posts")
                .document(postId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        return;
                    }

                    tvTitle.setText(documentSnapshot.getString("title"));
                    authorUid = documentSnapshot.getString("uid");
                    authorUsername = documentSnapshot.getString("username");
                    bindUsername(resolveAuthorDisplayName(
                            documentSnapshot.getString("name"),
                            documentSnapshot.getString("username")
                    ));
                    tvDescription.setText(documentSnapshot.getString("description"));
                    tvCookTime.setText(resolveCookTime(documentSnapshot.getString("cookTime")));
                    tvBudget.setText(resolveBudget(documentSnapshot.getString("budget")));
                    bindCreatedAt(documentSnapshot.getDate("createdAt"));
                    bindImage(documentSnapshot.getString("imageUrl"));

                    Long likesValue = documentSnapshot.getLong("likesCount");
                    likesCount = likesValue == null ? 0 : likesValue.intValue();
                    updateLikesCount();

                    bindIngredients(asStringList(documentSnapshot.get("ingredients")));
                    bindEquipment(asStringList(documentSnapshot.get("equipment")));
                    cookingSteps = CookingStepParser.parseList(asStringList(documentSnapshot.get("steps")));
                    bindSteps(cookingSteps);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.post_detail_loading_failed, Toast.LENGTH_SHORT).show());
    }

    private void loadEngagementState() {
        if (currentUid == null || postId == null || postId.trim().isEmpty()) {
            btnSavePost.setEnabled(false);
            btnLikePost.setEnabled(false);
            return;
        }

        btnSavePost.setEnabled(true);
        btnLikePost.setEnabled(true);

        db.collection("users")
                .document(currentUid)
                .collection("savedPosts")
                .document(postId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    isSaved = documentSnapshot.exists();
                    updateSaveButton();
                });

        db.collection("posts")
                .document(postId)
                .collection("likes")
                .document(currentUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    isLiked = documentSnapshot.exists();
                    updateLikeButton();
                });
    }

    private void toggleSave() {
        if (currentUid == null || postId == null) {
            return;
        }

        boolean previouslySaved = isSaved;
        isSaved = !isSaved;
        updateSaveButton();

        DocumentReference savedRef = db.collection("users")
                .document(currentUid)
                .collection("savedPosts")
                .document(postId);

        if (previouslySaved) {
            savedRef.delete()
                    .addOnSuccessListener(unused ->
                            showUndoSnackbar(R.string.post_unsaved_message, this::restoreSave))
                    .addOnFailureListener(e -> {
                        isSaved = true;
                        updateSaveButton();
                        Toast.makeText(this, R.string.post_detail_save_failed, Toast.LENGTH_SHORT).show();
                    });
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("savedAt", FieldValue.serverTimestamp());

            savedRef.set(data)
                    .addOnFailureListener(e -> {
                        isSaved = false;
                        updateSaveButton();
                        Toast.makeText(this, R.string.post_detail_save_failed, Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void restoreSave() {
        if (currentUid == null || postId == null) {
            return;
        }

        isSaved = true;
        updateSaveButton();

        Map<String, Object> data = new HashMap<>();
        data.put("savedAt", FieldValue.serverTimestamp());

        db.collection("users")
                .document(currentUid)
                .collection("savedPosts")
                .document(postId)
                .set(data)
                .addOnFailureListener(e -> {
                    isSaved = false;
                    updateSaveButton();
                    Toast.makeText(this, R.string.post_detail_save_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private void toggleLike() {
        if (currentUid == null || postId == null) {
            return;
        }

        boolean previouslyLiked = isLiked;
        int previousLikeCount = likesCount;

        isLiked = !isLiked;
        likesCount = previouslyLiked ? Math.max(0, likesCount - 1) : likesCount + 1;
        updateLikeButton();
        updateLikesCount();

        if (!previouslyLiked) {
            animateLikeButton();
        }

        DocumentReference postRef = db.collection("posts").document(postId);
        DocumentReference likeRef = postRef.collection("likes").document(currentUid);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(postRef);
            Long currentCountObj = snapshot.getLong("likesCount");
            long currentCount = currentCountObj == null ? 0 : currentCountObj;

            if (previouslyLiked) {
                transaction.delete(likeRef);
                transaction.update(postRef, "likesCount", Math.max(0, currentCount - 1));
            } else {
                Map<String, Object> likeData = new HashMap<>();
                likeData.put("likedAt", FieldValue.serverTimestamp());
                transaction.set(likeRef, likeData);
                transaction.update(postRef, "likesCount", currentCount + 1);
            }

            return null;
        }).addOnSuccessListener(unused -> {
            if (previouslyLiked) {
                showUndoSnackbar(R.string.post_unliked_message,
                        () -> restoreLike(previousLikeCount));
            } else {
                String postOwnerUid = getIntent().getStringExtra(EXTRA_POST_UID);

                String myUsername = getSharedPreferences("cookio_prefs", MODE_PRIVATE)
                        .getString("display_name", "Chef");

                String myPhotoUrl = getSharedPreferences("cookio_prefs", MODE_PRIVATE)
                        .getString("photoUrl", "");

                if (postOwnerUid != null && !postOwnerUid.equals(currentUid)) {
                    NotificationHelper.sendLikeNotification(
                            postOwnerUid,
                            currentUid,
                            myUsername,
                            myPhotoUrl,
                            postId,
                            tvTitle.getText().toString()
                    );
                }
            }
        }).addOnFailureListener(e -> {
            isLiked = previouslyLiked;
            likesCount = previousLikeCount;
            updateLikeButton();
            updateLikesCount();
            Toast.makeText(this, R.string.post_detail_like_failed, Toast.LENGTH_SHORT).show();
        });
    }

    private void restoreLike(int previousLikeCount) {
        if (currentUid == null || postId == null) {
            return;
        }

        isLiked = true;
        likesCount = previousLikeCount;
        updateLikeButton();
        updateLikesCount();
        animateLikeButton();

        DocumentReference postRef = db.collection("posts").document(postId);
        DocumentReference likeRef = postRef.collection("likes").document(currentUid);

        db.runTransaction(transaction -> {
            Map<String, Object> likeData = new HashMap<>();
            likeData.put("likedAt", FieldValue.serverTimestamp());
            transaction.set(likeRef, likeData);
            transaction.update(postRef, "likesCount", previousLikeCount);
            return null;
        }).addOnFailureListener(e -> {
            isLiked = false;
            likesCount = Math.max(0, previousLikeCount - 1);
            updateLikeButton();
            updateLikesCount();
            Toast.makeText(this, R.string.post_detail_like_failed, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateSaveButton() {
        btnSavePost.setIconResource(isSaved ? R.drawable.ic_save_filled : R.drawable.ic_save_outline);
        btnSavePost.setIconTint(ColorStateList.valueOf(getColor(R.color.primary)));
    }

    private void updateLikeButton() {
        btnLikePost.setIconResource(isLiked ? R.drawable.heart_filled : R.drawable.heart);
        btnLikePost.setIconTint(ColorStateList.valueOf(getColor(isLiked ? R.color.red : R.color.accent_pink)));
    }

    private void updateLikesCount() {

        String text = likesCount == 1 ? "1 like" : likesCount + " likes";
        tvDetailLikesCount.setText(text);
        tvDetailLikesCount.setText(getString(
                likesCount == 1
                        ? R.string.post_detail_like_count_singular
                        : R.string.post_detail_like_count_plural,
                likesCount
        ));
    }

    private void updateReviewsCount(int count) {
        int textRes = count == 1
                ? R.string.post_detail_reviews_count_singular
                : R.string.post_detail_reviews_count_plural;
        tvReviewsCount.setText(getString(textRes, count));
    }

    private void bindImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.logo)
                    .error(R.drawable.logo)
                    .centerCrop()
                    .into(ivImage);
        } else {
            ivImage.setImageResource(R.drawable.logo);
        }
    }

    private void bindCreatedAt(Date createdAt) {
        if (createdAt == null) {
            tvCreatedAt.setVisibility(View.GONE);
            return;
        }

        String formattedDate = DateFormat.getDateInstance(DateFormat.MEDIUM).format(createdAt);
        tvCreatedAt.setText(getString(R.string.post_detail_created_at, formattedDate));
        tvCreatedAt.setVisibility(View.VISIBLE);
    }

    private void bindUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            tvUsername.setText("");
            tvUsername.setOnClickListener(null);
            showAuthorInitial(getString(R.string.profile_default_username));
            return;
        }
        tvAuthorAvatarInitial.setText(resolveAvatarInitial(username));
        tvUsername.setText(getString(R.string.post_detail_author, username));
        tvUsername.setOnClickListener(v -> openAuthorProfile());
        loadAuthorAvatar();
    }

    private void loadAuthorAvatar() {
        if (authorUid == null || authorUid.trim().isEmpty()) {
            loadAuthorAvatarByUsername();
            return;
        }

        db.collection("users")
                .document(authorUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        loadAuthorAvatarByUsername();
                        return;
                    }

                    String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                    if (profileImageUrl == null || profileImageUrl.trim().isEmpty()) {
                        loadAuthorAvatarByUsername();
                        return;
                    }

                    showAuthorImage(profileImageUrl);
                })
                .addOnFailureListener(e -> loadAuthorAvatarByUsername());
    }

    private void loadAuthorAvatarByUsername() {
        if (authorUsername == null || authorUsername.trim().isEmpty()) {
            showAuthorInitial(tvUsername.getText().toString());
            return;
        }

        db.collection("users")
                .whereEqualTo("username", authorUsername)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        showAuthorInitial(tvUsername.getText() == null ? "" : tvUsername.getText().toString());
                        return;
                    }

                    DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
                    String profileImageUrl = userDoc.getString("profileImageUrl");
                    if (profileImageUrl == null || profileImageUrl.trim().isEmpty()) {
                        showAuthorInitial(tvUsername.getText() == null ? "" : tvUsername.getText().toString());
                        return;
                    }

                    if (authorUid == null || authorUid.trim().isEmpty()) {
                        authorUid = userDoc.getId();
                    }
                    showAuthorImage(profileImageUrl);
                })
                .addOnFailureListener(e -> showAuthorInitial(tvUsername.getText() == null ? "" : tvUsername.getText().toString()));
    }

    private void showAuthorImage(String profileImageUrl) {
        ivAuthorAvatar.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(profileImageUrl)
                .centerCrop()
                .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e,
                                                Object model,
                                                Target<android.graphics.drawable.Drawable> target,
                                                boolean isFirstResource) {
                        showAuthorInitial(tvUsername.getText() == null ? "" : tvUsername.getText().toString());
                        return true;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                                                   Object model,
                                                   Target<android.graphics.drawable.Drawable> target,
                                                   com.bumptech.glide.load.DataSource dataSource,
                                                   boolean isFirstResource) {
                        tvAuthorAvatarInitial.setVisibility(View.GONE);
                        ivAuthorAvatar.setVisibility(View.VISIBLE);
                        return false;
                    }
                })
                .into(ivAuthorAvatar);
    }

    private void showAuthorInitial(String username) {
        ivAuthorAvatar.setImageDrawable(null);
        ivAuthorAvatar.setVisibility(View.GONE);
        tvAuthorAvatarInitial.setText(resolveAvatarInitial(username));
        tvAuthorAvatarInitial.setVisibility(View.VISIBLE);
    }

    private String resolveAvatarInitial(String username) {
        if (username == null) {
            return "C";
        }

        String normalized = username.replace("by ", "").trim();
        if (normalized.isEmpty()) {
            return "C";
        }

        return normalized.substring(0, 1).toUpperCase();
    }

    private String resolveAuthorDisplayName(String name, String username) {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        if (username != null && !username.trim().isEmpty()) {
            return username;
        }
        return getString(R.string.profile_default_username);
    }
    private void openAuthorProfile() {
        if (authorUid == null || authorUid.isEmpty()) return;

        if (currentUid != null && authorUid.equals(currentUid)) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else {
            Intent intent = new Intent(this, PublicProfileActivity.class);
            intent.putExtra(PublicProfileActivity.EXTRA_USER_ID, authorUid);
            startActivity(intent);
        }
    }

    private void bindIngredients(List<String> ingredients) {
        cgIngredients.removeAllViews();
        if (ingredients == null) {
            return;
        }

        for (String ingredient : ingredients) {
            Chip chip = buildChip(ingredient);
            cgIngredients.addView(chip);
        }
    }

    private void bindEquipment(List<String> equipment) {
        cgEquipment.removeAllViews();
        if (equipment == null) {
            return;
        }

        for (String item : equipment) {
            Chip chip = buildChip(item);
            cgEquipment.addView(chip);
        }
    }

    private Chip buildChip(String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setClickable(false);
        chip.setCheckable(false);
        chip.setTextSize(13f);
        chip.setChipBackgroundColorResource(R.color.chip_bg_selector);
        chip.setTextColor(getColor(R.color.chip_text));
        return chip;
    }

    private void bindSteps(List<CookingStep> steps) {
        llSteps.removeAllViews();
        if (steps == null) {
            return;
        }

        for (int i = 0; i < steps.size(); i++) {
            CookingStep step = steps.get(i);
            TextView stepView = new TextView(this);
            StringBuilder label = new StringBuilder();
            label.append(i + 1).append(". ").append(stripExistingStepNumber(step.getInstruction()));
            if (step.hasTimer()) {
                label.append("\n").append(getString(R.string.cooking_minutes_label, step.getMinutes()));
            }
            stepView.setText(label.toString());
            stepView.setTextColor(getColor(R.color.textDark));
            stepView.setTextSize(15f);
            stepView.setLineSpacing(0f, 1.25f);
            int pad = dpToPx(14);
            stepView.setPadding(dpToPx(16), pad, dpToPx(16), pad);
            stepView.setBackgroundResource(R.drawable.bg_equip_item);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = dpToPx(10);
            stepView.setLayoutParams(params);
            llSteps.addView(stepView);
        }
    }

    private void openCookingMode() {
        if (cookingSteps.isEmpty()) {
            Toast.makeText(this, R.string.cooking_mode_missing_steps, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, TimerActivity.class);
        intent.putExtra(TimerActivity.EXTRA_RECIPE_NAME, safeText(tvTitle.getText().toString()));
        intent.putStringArrayListExtra(
                TimerActivity.EXTRA_RECIPE_TIMED_STEPS,
                new ArrayList<>(encodeSteps(cookingSteps))
        );
        startActivity(intent);
    }

    private void openPublicProfile() {
        if (authorUid == null || authorUid.trim().isEmpty()) {
            return;
        }

        Intent intent = new Intent(this, PublicProfileActivity.class);
        intent.putExtra(PublicProfileActivity.EXTRA_USER_ID, authorUid);
        startActivity(intent);
    }

    private void openLikesList() {
        if (postId == null || postId.trim().isEmpty()) {
            return;
        }

        Intent intent = new Intent(this, UserConnectionsActivity.class);
        intent.putExtra(UserConnectionsActivity.EXTRA_POST_ID, postId);
        intent.putExtra(UserConnectionsActivity.EXTRA_MODE, UserConnectionsActivity.MODE_LIKES);
        startActivity(intent);
    }

    private void animateLikeButton() {
        btnLikePost.animate().cancel();
        btnLikePost.setScaleX(0.9f);
        btnLikePost.setScaleY(0.9f);
        btnLikePost.animate()
                .scaleX(1.12f)
                .scaleY(1.12f)
                .setDuration(120)
                .withEndAction(() -> btnLikePost.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .start())
                .start();
    }

    private void showUndoSnackbar(int messageRes, Runnable undoAction) {
        View root = findViewById(android.R.id.content);
        Snackbar.make(root, messageRes, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo_action, v -> undoAction.run())
                .show();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String resolveCookTime(String value) {
        String normalized = CookTimeFormatter.normalize(value);
        if (TextUtils.isEmpty(normalized)) {
            return getString(R.string.post_time_placeholder);
        }
        return normalized;
    }

    private String resolveBudget(String value) {
        if (TextUtils.isEmpty(value)) {
            return getString(R.string.post_budget_placeholder);
        }
        return value.trim();
    }

    private ArrayList<String> asStringList(Object value) {
        ArrayList<String> result = new ArrayList<>();
        if (!(value instanceof List<?>)) {
            return result;
        }

        for (Object item : (List<?>) value) {
            if (item instanceof String) {
                result.add((String) item);
            }
        }
        return result;
    }

    private List<String> encodeSteps(List<CookingStep> steps) {
        List<String> encoded = new ArrayList<>();
        for (CookingStep step : steps) {
            encoded.add(step.encode());
        }
        return encoded;
    }

    private String stripExistingStepNumber(String step) {
        if (step == null) {
            return "";
        }
        return step.replaceFirst("^(?i)(step\\s*)?\\d+[\\.)\\-:]*\\s*", "").trim();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
