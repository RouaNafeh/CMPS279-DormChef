package com.cookio.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cookio.app.R;
import com.cookio.app.models.CookingStep;
import com.cookio.app.models.CookingStepParser;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostDetailActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "post_id";
    public static final String EXTRA_POST_TITLE = "post_title";
    public static final String EXTRA_POST_DESCRIPTION = "post_description";
    public static final String EXTRA_POST_IMAGE_URL = "post_image_url";
    public static final String EXTRA_POST_COOK_TIME = "post_cook_time";
    public static final String EXTRA_POST_BUDGET = "post_budget";
    public static final String EXTRA_POST_USERNAME = "post_username";
    public static final String EXTRA_POST_INGREDIENTS = "post_ingredients";
    public static final String EXTRA_POST_STEPS = "post_steps";
    public static final String EXTRA_POST_LIKES_COUNT = "post_likes_count";

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String postId;
    private String currentUid;
    private int likesCount;
    private boolean isSaved;
    private boolean isLiked;

    private ImageView ivImage;
    private TextView tvTitle;
    private TextView tvUsername;
    private TextView tvDescription;
    private TextView tvCookTime;
    private TextView tvBudget;
    private TextView tvDetailLikesCount;
    private ChipGroup cgIngredients;
    private LinearLayout llSteps;
    private MaterialButton btnSavePost;
    private MaterialButton btnLikePost;
    private MaterialButton btnCookingMode;
    private List<CookingStep> cookingSteps = new ArrayList<>();

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

        findViewById(R.id.btnBack).setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed());

        btnSavePost.setOnClickListener(v -> toggleSave());
        btnLikePost.setOnClickListener(v -> toggleLike());
        btnCookingMode.setOnClickListener(v -> openCookingMode());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPostDetails();
        loadEngagementState();
    }

    private void bindViews() {
        ivImage = findViewById(R.id.ivPostImage);
        tvTitle = findViewById(R.id.tvPostTitle);
        tvUsername = findViewById(R.id.tvPostUsername);
        tvDescription = findViewById(R.id.tvPostDescription);
        tvCookTime = findViewById(R.id.tvPostCookTime);
        tvBudget = findViewById(R.id.tvPostBudget);
        tvDetailLikesCount = findViewById(R.id.tvDetailLikesCount);
        cgIngredients = findViewById(R.id.chipGroupIngredients);
        llSteps = findViewById(R.id.stepsContainer);
        btnSavePost = findViewById(R.id.btnSavePost);
        btnLikePost = findViewById(R.id.btnLikePost);
        btnCookingMode = findViewById(R.id.btnCookingMode);
    }

    private void bindStaticContentFromIntent() {
        tvTitle.setText(getIntent().getStringExtra(EXTRA_POST_TITLE));
        bindUsername(getIntent().getStringExtra(EXTRA_POST_USERNAME));
        tvDescription.setText(getIntent().getStringExtra(EXTRA_POST_DESCRIPTION));
        tvCookTime.setText(safeText(getIntent().getStringExtra(EXTRA_POST_COOK_TIME)));
        tvBudget.setText(safeText(getIntent().getStringExtra(EXTRA_POST_BUDGET)));
        bindImage(getIntent().getStringExtra(EXTRA_POST_IMAGE_URL));

        ArrayList<String> ingredients = getIntent().getStringArrayListExtra(EXTRA_POST_INGREDIENTS);
        ArrayList<String> steps = getIntent().getStringArrayListExtra(EXTRA_POST_STEPS);
        cookingSteps = CookingStepParser.parseList(steps);
        bindIngredients(ingredients);
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
                    bindUsername(documentSnapshot.getString("username"));
                    tvDescription.setText(documentSnapshot.getString("description"));
                    tvCookTime.setText(safeText(documentSnapshot.getString("cookTime")));
                    tvBudget.setText(safeText(documentSnapshot.getString("budget")));
                    bindImage(documentSnapshot.getString("imageUrl"));

                    Long likesValue = documentSnapshot.getLong("likesCount");
                    likesCount = likesValue == null ? 0 : likesValue.intValue();
                    updateLikesCount();

                    bindIngredients(asStringList(documentSnapshot.get("ingredients")));
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
        likesCount = previousLikeCount + 1;
        updateLikeButton();
        updateLikesCount();
        animateLikeButton();

        DocumentReference postRef = db.collection("posts").document(postId);
        DocumentReference likeRef = postRef.collection("likes").document(currentUid);

        db.runTransaction(transaction -> {
            Map<String, Object> likeData = new HashMap<>();
            likeData.put("likedAt", FieldValue.serverTimestamp());
            transaction.set(likeRef, likeData);
            transaction.update(postRef, "likesCount", previousLikeCount + 1);
            return null;
        }).addOnFailureListener(e -> {
            isLiked = false;
            likesCount = previousLikeCount;
            updateLikeButton();
            updateLikesCount();
            Toast.makeText(this, R.string.post_detail_like_failed, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateSaveButton() {
        btnSavePost.setIconResource(isSaved ? R.drawable.ic_save_filled : R.drawable.ic_save_outline);
    }

    private void updateLikeButton() {
        btnLikePost.setIconResource(isLiked ? R.drawable.heart_filled : R.drawable.heart);
    }

    private void updateLikesCount() {
        tvDetailLikesCount.setText(getString(R.string.post_detail_likes_count, likesCount));
    }

    private void bindImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.logo_cropped)
                    .error(R.drawable.logo_cropped)
                    .centerCrop()
                    .into(ivImage);
        } else {
            ivImage.setImageResource(R.drawable.logo_cropped);
        }
    }

    private void bindUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            tvUsername.setText("");
            return;
        }
        tvUsername.setText(getString(R.string.post_detail_author, username));
    }

    private void bindIngredients(List<String> ingredients) {
        cgIngredients.removeAllViews();
        if (ingredients == null) {
            return;
        }

        for (String ingredient : ingredients) {
            Chip chip = new Chip(this);
            chip.setText(ingredient);
            chip.setClickable(false);
            chip.setCheckable(false);
            chip.setTextSize(13f);
            chip.setChipBackgroundColorResource(R.color.chip_bg_selector);
            chip.setTextColor(getColor(R.color.chip_text));
            cgIngredients.addView(chip);
        }
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
            label.append(i + 1).append(". ").append(step.getInstruction());
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

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
