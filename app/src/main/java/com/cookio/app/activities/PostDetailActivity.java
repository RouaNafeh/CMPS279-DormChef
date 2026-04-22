package com.cookio.app.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cookio.app.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;

public class PostDetailActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID          = "post_id";
    public static final String EXTRA_POST_TITLE       = "post_title";
    public static final String EXTRA_POST_DESCRIPTION = "post_description";
    public static final String EXTRA_POST_IMAGE_URL   = "post_image_url";
    public static final String EXTRA_POST_COOK_TIME   = "post_cook_time";
    public static final String EXTRA_POST_BUDGET      = "post_budget";
    public static final String EXTRA_POST_USERNAME    = "post_username";
    public static final String EXTRA_POST_INGREDIENTS = "post_ingredients";
    public static final String EXTRA_POST_STEPS       = "post_steps";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        // ── Back button ───────────────────────────────────────────────────────
        findViewById(R.id.btnBack).setOnClickListener(
                v -> getOnBackPressedDispatcher().onBackPressed());

        // ── Read intent extras ────────────────────────────────────────────────
        String title       = getIntent().getStringExtra(EXTRA_POST_TITLE);
        String description = getIntent().getStringExtra(EXTRA_POST_DESCRIPTION);
        String imageUrl    = getIntent().getStringExtra(EXTRA_POST_IMAGE_URL);
        String cookTime    = getIntent().getStringExtra(EXTRA_POST_COOK_TIME);
        String budget      = getIntent().getStringExtra(EXTRA_POST_BUDGET);
        String username    = getIntent().getStringExtra(EXTRA_POST_USERNAME);
        ArrayList<String> ingredients =
                getIntent().getStringArrayListExtra(EXTRA_POST_INGREDIENTS);
        ArrayList<String> steps =
                getIntent().getStringArrayListExtra(EXTRA_POST_STEPS);

        // ── Bind views ────────────────────────────────────────────────────────
        ImageView  ivImage       = findViewById(R.id.ivPostImage);
        TextView   tvTitle       = findViewById(R.id.tvPostTitle);
        TextView   tvUsername    = findViewById(R.id.tvPostUsername);
        TextView   tvDescription = findViewById(R.id.tvPostDescription);
        TextView   tvCookTime    = findViewById(R.id.tvPostCookTime);
        TextView   tvBudget      = findViewById(R.id.tvPostBudget);
        ChipGroup  cgIngredients = findViewById(R.id.chipGroupIngredients);
        LinearLayout llSteps     = findViewById(R.id.stepsContainer);

        tvTitle.setText(title);
        tvUsername.setText(username != null ? "by " + username : "");
        tvDescription.setText(description);
        tvCookTime.setText(cookTime != null ? "⏱ " + cookTime : "");
        tvBudget.setText(budget != null ? "💰 " + budget : "");

        // Load image with Glide
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

        // ── Populate ingredients as chips ─────────────────────────────────────
        if (ingredients != null) {
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

        // ── Populate steps ────────────────────────────────────────────────────
        if (steps != null) {
            for (int i = 0; i < steps.size(); i++) {
                TextView stepView = new TextView(this);
                stepView.setText((i + 1) + ". " + steps.get(i));
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
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}