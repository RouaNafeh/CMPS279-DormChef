package com.cookio.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.cookio.app.R;
import com.cookio.app.ai.AiRecipeHelper;
import com.cookio.app.databinding.ActivityCreatePostBinding;
import com.cookio.app.models.CookingStep;
import com.cookio.app.models.CookingStepParser;
import com.cookio.app.models.Post;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CreatePostActivity extends AppCompatActivity {
    private static final String TAG = "CreatePostActivity";

    public static final String EXTRA_EDIT_MODE = "edit_mode";
    public static final String EXTRA_POST_ID = "edit_post_id";
    public static final String EXTRA_POST_TITLE = "edit_post_title";
    public static final String EXTRA_POST_DESCRIPTION = "edit_post_description";
    public static final String EXTRA_POST_COOK_TIME = "edit_post_cook_time";
    public static final String EXTRA_POST_BUDGET = "edit_post_budget";
    public static final String EXTRA_POST_IMAGE_URL = "edit_post_image_url";
    public static final String EXTRA_POST_INGREDIENTS = "edit_post_ingredients";
    public static final String EXTRA_POST_EQUIPMENT = "edit_post_equipment";
    public static final String EXTRA_POST_STEPS = "edit_post_steps";

    private AiRecipeHelper aiRecipeHelper;
    private ActivityCreatePostBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    private Uri selectedImageUri = null;
    private boolean isEditMode = false;
    private String editingPostId = null;
    private String existingImageUrl = "";

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    binding.ivImagePreview.setImageURI(uri);
                    binding.ivImagePreview.setVisibility(View.VISIBLE);
                    binding.imagePlaceholder.setVisibility(View.GONE);
                    binding.tvReselect.setVisibility(View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LandingActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        binding = ActivityCreatePostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        isEditMode = getIntent().getBooleanExtra(EXTRA_EDIT_MODE, false);
        editingPostId = getIntent().getStringExtra(EXTRA_POST_ID);
        existingImageUrl = getIntent().getStringExtra(EXTRA_POST_IMAGE_URL);
        if (existingImageUrl == null) {
            existingImageUrl = "";
        }

        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.imagePickerContainer.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        binding.tvReselect.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        addIngredientRow("");
        addEquipmentRow("");
        addStepRow(new CookingStep("", 0));

        binding.btnAddIngredient.setOnClickListener(v -> addIngredientRow(""));
        binding.btnAddEquipment.setOnClickListener(v -> addEquipmentRow(""));
        binding.btnAddStep.setOnClickListener(v -> addStepRow(new CookingStep("", 0)));
        binding.btnPost.setOnClickListener(v -> validateAndPost());

        aiRecipeHelper = new AiRecipeHelper();
        binding.btnGenerateAi.setOnClickListener(v -> generateWithAi());

        if (isEditMode) {
            binding.btnPost.setText("Save Changes");
            populateEditData();
        }
    }

    private void addIngredientRow(String prefill) {
        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_dynamic_row, binding.ingredientsContainer, false);

        EditText etRow = row.findViewById(R.id.etRowInput);
        ImageButton btnDel = row.findViewById(R.id.btnDeleteRow);

        etRow.setHint("e.g. 2 eggs");
        if (!prefill.isEmpty()) {
            etRow.setText(prefill);
        }

        btnDel.setOnClickListener(v -> {
            if (binding.ingredientsContainer.getChildCount() > 1) {
                binding.ingredientsContainer.removeView(row);
            } else {
                etRow.setText("");
            }
        });

        binding.ingredientsContainer.addView(row);
    }

    private void addEquipmentRow(String prefill) {
        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_dynamic_row, binding.equipmentContainer, false);

        EditText etRow = row.findViewById(R.id.etRowInput);
        ImageButton btnDel = row.findViewById(R.id.btnDeleteRow);

        etRow.setHint("e.g. pan, whisk, oven");
        if (!prefill.isEmpty()) {
            etRow.setText(prefill);
        }

        btnDel.setOnClickListener(v -> {
            if (binding.equipmentContainer.getChildCount() > 1) {
                binding.equipmentContainer.removeView(row);
            } else {
                etRow.setText("");
            }
        });

        binding.equipmentContainer.addView(row);
    }

    private void addStepRow(CookingStep step) {
        int stepNumber = binding.stepsContainer.getChildCount() + 1;

        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_step_timer_row, binding.stepsContainer, false);

        EditText etInstruction = row.findViewById(R.id.etStepInstruction);
        EditText etMinutes = row.findViewById(R.id.etStepMinutes);
        ImageButton btnDelete = row.findViewById(R.id.btnDeleteStepRow);

        etInstruction.setHint("Step " + stepNumber);
        etInstruction.setText(step.getInstruction());
        etMinutes.setText(String.valueOf(step.getMinutes()));

        btnDelete.setOnClickListener(v -> {
            if (binding.stepsContainer.getChildCount() > 1) {
                binding.stepsContainer.removeView(row);
                renumberSteps();
            } else {
                etInstruction.setText("");
                etMinutes.setText("0");
            }
        });

        binding.stepsContainer.addView(row);
    }

    private void renumberSteps() {
        for (int i = 0; i < binding.stepsContainer.getChildCount(); i++) {
            View row = binding.stepsContainer.getChildAt(i);
            EditText etInstruction = row.findViewById(R.id.etStepInstruction);
            if (etInstruction.getText().toString().isEmpty()) {
                etInstruction.setHint("Step " + (i + 1));
            }
        }
    }

    private List<String> collectRows(LinearLayout container) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View row = container.getChildAt(i);
            EditText et = row.findViewById(R.id.etRowInput);
            String val = et.getText().toString().trim();
            if (!val.isEmpty()) {
                result.add(val);
            }
        }
        return result;
    }

    private List<String> collectIngredientRows(LinearLayout container) {
        return collectRows(container);
    }

    private List<CookingStep> collectStepRows() {
        List<CookingStep> steps = new ArrayList<>();
        for (int i = 0; i < binding.stepsContainer.getChildCount(); i++) {
            View row = binding.stepsContainer.getChildAt(i);
            EditText etInstruction = row.findViewById(R.id.etStepInstruction);
            EditText etMinutes = row.findViewById(R.id.etStepMinutes);

            String instruction = etInstruction.getText().toString().trim();
            String minutesRaw = etMinutes.getText().toString().trim();

            if (instruction.isEmpty()) {
                continue;
            }

            int minutes;
            try {
                minutes = minutesRaw.isEmpty() ? 0 : Integer.parseInt(minutesRaw);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(getString(R.string.step_time_invalid));
            }

            if (minutes < 0) {
                throw new IllegalArgumentException(getString(R.string.step_time_invalid));
            }

            steps.add(new CookingStep(instruction, minutes));
        }
        return steps;
    }

    private void validateAndPost() {
        String title = text(binding.etTitle);
        String description = text(binding.etDescription);
        String cookTime = text(binding.etCookTime);
        String budget = text(binding.etBudget);

        if (TextUtils.isEmpty(title)) {
            binding.etTitle.setError("Title is required");
            binding.etTitle.requestFocus();
            return;
        }

        List<String> ingredients = collectIngredientRows(binding.ingredientsContainer);
        if (ingredients.isEmpty()) {
            Toast.makeText(this, "Add at least one ingredient", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> equipment = collectRows(binding.equipmentContainer);

        List<CookingStep> cookingSteps;
        try {
            cookingSteps = collectStepRows();
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        if (cookingSteps.isEmpty()) {
            Toast.makeText(this, R.string.step_instruction_required, Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> encodedSteps = new ArrayList<>();
        for (CookingStep step : cookingSteps) {
            encodedSteps.add(step.encode());
        }

        setLoading(true);
        String imageUrl = selectedImageUri == null ? existingImageUrl : null;
        if (selectedImageUri != null) {
            uploadImageThenSave(title, description, cookTime, budget, ingredients, equipment, encodedSteps);
        } else {
            savePostToFirestore(title, description, cookTime, budget, ingredients, equipment, encodedSteps, imageUrl);
        }
    }

    private void uploadImageThenSave(String title, String description, String cookTime,
                                     String budget, List<String> ingredients, List<String> equipment,
                                     List<String> steps) {
        String uid = auth.getCurrentUser().getUid();
        String filename = UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = storage.getReference()
                .child("posts").child(uid).child(filename);

        binding.uploadProgressBar.setVisibility(View.VISIBLE);
        binding.tvUploadProgress.setVisibility(View.VISIBLE);
        binding.tvUploadProgress.setText("Uploading photo... 0%");

        UploadTask uploadTask = ref.putFile(selectedImageUri);

        uploadTask.addOnProgressListener(snapshot -> {
            double pct = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            int progress = (int) pct;
            binding.uploadProgressBar.setProgress(progress);
            binding.tvUploadProgress.setText("Uploading photo... " + progress + "%");
        });

        uploadTask
                .addOnSuccessListener(snapshot ->
                        ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            binding.uploadProgressBar.setVisibility(View.GONE);
                            binding.tvUploadProgress.setVisibility(View.GONE);
                            savePostToFirestore(title, description, cookTime, budget,
                                    ingredients, equipment, steps, downloadUri.toString());
                        }))
                .addOnFailureListener(e -> {
                    binding.uploadProgressBar.setVisibility(View.GONE);
                    binding.tvUploadProgress.setVisibility(View.GONE);
                    setLoading(false);
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void savePostToFirestore(String title, String description, String cookTime,
                                     String budget, List<String> ingredients, List<String> equipment,
                                     List<String> steps, String imageUrl) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            setLoading(false);
            return;
        }

        firestore.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(userDoc -> {
                    String username = userDoc.getString("username");
                    if (username == null) {
                        username = "Unknown";
                    }

                    Post post = new Post(
                            user.getUid(), username, title, description,
                            cookTime, budget, ingredients, equipment, steps, imageUrl
                    );

                    if (isEditMode && !TextUtils.isEmpty(editingPostId)) {
                        firestore.collection("posts")
                                .document(editingPostId)
                                .update(
                                        "title", post.getTitle(),
                                        "description", post.getDescription(),
                                        "cookTime", post.getCookTime(),
                                        "budget", post.getBudget(),
                                        "ingredients", post.getIngredients(),
                                        "equipment", post.getEquipment(),
                                        "steps", post.getSteps(),
                                        "imageUrl", post.getImageUrl()
                                )
                                .addOnSuccessListener(unused -> {
                                    setLoading(false);
                                    Toast.makeText(this, "Post updated!", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    setLoading(false);
                                    Toast.makeText(this,
                                            "Failed to update: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    } else {
                        firestore.collection("posts")
                                .add(post)
                                .addOnSuccessListener(docRef ->
                                        docRef.update("postId", docRef.getId())
                                                .addOnCompleteListener(t -> {
                                                    setLoading(false);
                                                    Toast.makeText(this,
                                                            "Recipe posted!",
                                                            Toast.LENGTH_SHORT).show();
                                                    finish();
                                                }))
                                .addOnFailureListener(e -> {
                                    setLoading(false);
                                    Toast.makeText(this,
                                            "Failed to post: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Could not load user info",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void populateEditData() {
        binding.etTitle.setText(getIntent().getStringExtra(EXTRA_POST_TITLE));
        binding.etDescription.setText(getIntent().getStringExtra(EXTRA_POST_DESCRIPTION));
        binding.etCookTime.setText(getIntent().getStringExtra(EXTRA_POST_COOK_TIME));
        binding.etBudget.setText(getIntent().getStringExtra(EXTRA_POST_BUDGET));

        ArrayList<String> ingredients = getIntent().getStringArrayListExtra(EXTRA_POST_INGREDIENTS);
        ArrayList<String> equipment = getIntent().getStringArrayListExtra(EXTRA_POST_EQUIPMENT);
        ArrayList<String> steps = getIntent().getStringArrayListExtra(EXTRA_POST_STEPS);

        replaceIngredientRows(ingredients != null ? ingredients : new ArrayList<>());
        replaceEquipmentRows(equipment != null ? equipment : new ArrayList<>());
        replaceStepRows(CookingStepParser.parseList(steps));
    }

    private void setLoading(boolean loading) {
        binding.btnPost.setEnabled(!loading);
        binding.btnPost.setText(loading ? "Posting..." : (isEditMode ? "Save Changes" : "Post Recipe"));
        binding.btnAddIngredient.setEnabled(!loading);
        binding.btnAddEquipment.setEnabled(!loading);
        binding.btnAddStep.setEnabled(!loading);
        binding.btnGenerateAi.setEnabled(!loading);
    }

    private void generateWithAi() {
        List<String> ingredientRows = collectIngredientRows(binding.ingredientsContainer);
        String ingredients = TextUtils.join(", ", ingredientRows);

        if (ingredients.isEmpty()) {
            Toast.makeText(this, "Enter ingredients first", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnGenerateAi.setEnabled(false);
        binding.btnGenerateAi.setText("Generating...");

        aiRecipeHelper.generateRecipe(ingredients, new AiRecipeHelper.AiRecipeCallback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    binding.btnGenerateAi.setEnabled(true);
                    binding.btnGenerateAi.setText("Generate with AI");
                    showAiResult(result);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    binding.btnGenerateAi.setEnabled(true);
                    binding.btnGenerateAi.setText("Generate with AI");
                    Log.e(TAG, "AI generation failed", e);
                    Toast.makeText(CreatePostActivity.this,
                            "AI generation failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showAiResult(String result) {
        applyAiResult(result);
        Toast.makeText(this, "AI draft generated", Toast.LENGTH_SHORT).show();
    }

    private void applyAiResult(String result) {
        binding.etTitle.setText(extractSection(result, "TITLE"));
        binding.etDescription.setText(extractSection(result, "DESCRIPTION"));
        binding.etCookTime.setText(normalizeCookTime(extractSection(result, "TIME")));
        binding.etBudget.setText(normalizeBudget(extractSection(result, "BUDGET")));

        replaceIngredientRows(splitAiList(extractSection(result, "INGREDIENTS"), ","));
        replaceEquipmentRows(splitAiList(extractSection(result, "EQUIPMENT"), ","));
        replaceStepRows(CookingStepParser.parseDelimited(extractSection(result, "STEPS")));
    }

    private void replaceIngredientRows(List<String> values) {
        binding.ingredientsContainer.removeAllViews();

        if (values.isEmpty()) {
            addIngredientRow("");
            return;
        }

        for (String value : values) {
            addIngredientRow(value);
        }
    }

    private void replaceEquipmentRows(List<String> values) {
        binding.equipmentContainer.removeAllViews();

        if (values.isEmpty()) {
            addEquipmentRow("");
            return;
        }

        for (String value : values) {
            addEquipmentRow(value);
        }
    }

    private void replaceStepRows(List<CookingStep> values) {
        binding.stepsContainer.removeAllViews();

        if (values.isEmpty()) {
            addStepRow(new CookingStep("", 0));
            return;
        }

        for (CookingStep step : values) {
            addStepRow(step);
        }
    }

    private String extractSection(String result, String sectionName) {
        String prefix = sectionName + ":";
        for (String line : result.split("\\r?\\n")) {
            if (line.toUpperCase().startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    private List<String> splitAiList(String value, String separatorRegex) {
        List<String> items = new ArrayList<>();
        if (TextUtils.isEmpty(value)) {
            return items;
        }

        for (String item : Arrays.asList(value.split(separatorRegex))) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                items.add(trimmed);
            }
        }
        return items;
    }

    private String normalizeCookTime(String rawTime) {
        if (TextUtils.isEmpty(rawTime)) {
            return "";
        }

        String normalized = rawTime.trim().toLowerCase();
        normalized = normalized.replace("minutes", "min")
                .replace("minute", "min")
                .replace("mins", "min")
                .replace("hours", "hr")
                .replace("hour", "hr")
                .replaceAll("\\s+", " ");

        String[] tokens = normalized.split(" ");
        List<String> compact = new ArrayList<>();

        for (int i = 0; i < tokens.length - 1; i++) {
            if (tokens[i].matches("\\d+")
                    && (tokens[i + 1].equals("min") || tokens[i + 1].equals("hr"))) {
                compact.add(tokens[i] + " " + tokens[i + 1]);
                i++;
            }
        }

        if (!compact.isEmpty()) {
            String joined = TextUtils.join(" ", compact);
            return joined.length() > 14 ? joined.substring(0, 14).trim() : joined;
        }

        return rawTime.length() > 14 ? rawTime.substring(0, 14).trim() : rawTime.trim();
    }

    private String normalizeBudget(String rawBudget) {
        if (TextUtils.isEmpty(rawBudget)) {
            return "";
        }

        String normalized = rawBudget.trim().toLowerCase();
        if (normalized.contains("low")) {
            return "Low";
        }
        if (normalized.contains("high")) {
            return "High";
        }
        if (normalized.contains("medium") || normalized.contains("mid")) {
            return "Medium";
        }
        return "Medium";
    }
}
