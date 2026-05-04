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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.cookio.app.R;
import com.cookio.app.ai.AiRecipeHelper;
import com.cookio.app.databinding.ActivityCreatePostBinding;
import com.cookio.app.models.CookingStep;
import com.cookio.app.models.CookingStepParser;
import com.cookio.app.models.Post;
import com.cookio.app.utils.CookTimeFormatter;
import com.cookio.app.utils.UserDisplayHelper;
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
    private final ActivityResultLauncher<String> photoPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    imagePickerLauncher.launch("image/*");
                } else {
                    Toast.makeText(this, R.string.photo_permission_denied, Toast.LENGTH_SHORT).show();
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
        binding.imagePickerContainer.setOnClickListener(v -> requestPhotoAccessAndPickImage());
        binding.tvReselect.setOnClickListener(v -> requestPhotoAccessAndPickImage());

        addIngredientRow("");
        addEquipmentRow("");
        addStepRow(new CookingStep("", 0));

        binding.btnAddIngredient.setOnClickListener(v -> addIngredientRow(""));
        binding.btnAddEquipment.setOnClickListener(v -> addEquipmentRow(""));
        binding.btnAddStep.setOnClickListener(v -> addStepRow(new CookingStep("", 0)));
        binding.btnPost.setOnClickListener(v -> validateAndPost());
        binding.btnClearForm.setOnClickListener(v -> confirmClearForm());

        aiRecipeHelper = new AiRecipeHelper();
        binding.btnGenerateAi.setOnClickListener(v -> generateWithAi());

        if (isEditMode) {
            binding.tvPageLabel.setText("Edit recipe");
            binding.tvPageTitle.setText("Polish your post");
            binding.tvIntroCopy.setText("Update the details, swap the photo if needed, and keep your recipe looking sharp.");
            binding.tvSubmitTitle.setText("Ready to update?");
            binding.tvSubmitHint.setText("Your changes will refresh this recipe in the community feed and on your profile.");
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
        String cookTime = CookTimeFormatter.normalize(text(binding.etCookTime));
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

    private void requestPhotoAccessAndPickImage() {
        String permission = getPhotoPermission();
        if (permission == null
                || ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            imagePickerLauncher.launch("image/*");
            return;
        }

        if (shouldShowRequestPermissionRationale(permission)) {
            Toast.makeText(this, R.string.photo_permission_rationale, Toast.LENGTH_SHORT).show();
        }

        photoPermissionLauncher.launch(permission);
    }

    private String getPhotoPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        }
        return Manifest.permission.READ_EXTERNAL_STORAGE;
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
                    String displayName = UserDisplayHelper.resolveDisplayName(
                            userDoc.getString("name"),
                            userDoc.getString("username"),
                            getString(R.string.profile_default_username)
                    );
                    String username = userDoc.getString("username");
                    String profileImageUrl = userDoc.getString("profileImageUrl");

                    Post post = new Post(
                            user.getUid(), displayName, username, title, description,
                            cookTime, budget, ingredients, equipment, steps, imageUrl
                    );
                    post.setProfileImageUrl(profileImageUrl);

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
                                        "imageUrl", post.getImageUrl(),
                                        "profileImageUrl", post.getProfileImageUrl()
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
        binding.btnClearForm.setEnabled(!loading);
        binding.btnAddIngredient.setEnabled(!loading);
        binding.btnAddEquipment.setEnabled(!loading);
        binding.btnAddStep.setEnabled(!loading);
        binding.btnGenerateAi.setEnabled(!loading);
    }

    private void confirmClearForm() {
        new AlertDialog.Builder(this)
                .setTitle("Clear draft?")
                .setMessage("This will remove the current photo, text, ingredients, equipment, and steps.")
                .setNegativeButton(R.string.profile_edit_dialog_cancel, null)
                .setPositiveButton("Clear", (dialog, which) -> clearForm())
                .show();
    }

    private void clearForm() {
        selectedImageUri = null;
        existingImageUrl = "";

        binding.etTitle.setText("");
        binding.etDescription.setText("");
        binding.etCookTime.setText("");
        binding.etBudget.setText("");
        binding.etTitle.setError(null);

        binding.ivImagePreview.setImageDrawable(null);
        binding.ivImagePreview.setVisibility(View.GONE);
        binding.imagePlaceholder.setVisibility(View.VISIBLE);
        binding.tvReselect.setVisibility(View.GONE);
        binding.uploadProgressBar.setProgress(0);
        binding.uploadProgressBar.setVisibility(View.GONE);
        binding.tvUploadProgress.setVisibility(View.GONE);

        replaceIngredientRows(new ArrayList<>());
        replaceEquipmentRows(new ArrayList<>());
        replaceStepRows(new ArrayList<>());

        Toast.makeText(this, "Draft cleared", Toast.LENGTH_SHORT).show();
    }

    private void generateWithAi() {
        String title = text(binding.etTitle);
        List<String> ingredientRows = collectIngredientRows(binding.ingredientsContainer);
        String ingredients = TextUtils.join(", ", ingredientRows);

        if (title.isEmpty() && ingredients.isEmpty()) {
            Toast.makeText(this, "Enter a recipe title, ingredients, or both", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnGenerateAi.setEnabled(false);
        binding.btnGenerateAi.setText("Generating...");

        aiRecipeHelper.generateRecipe(title, ingredients, new AiRecipeHelper.AiRecipeCallback() {
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
        binding.etCookTime.setText(CookTimeFormatter.normalize(extractSection(result, "TIME")));
        binding.etBudget.setText(normalizeBudget(extractSection(result, "BUDGET")));

        replaceIngredientRows(splitAiList(extractSection(result, "INGREDIENTS"), "\\|\\|"));
        replaceEquipmentRows(splitAiList(extractSection(result, "EQUIPMENT"), "\\|\\|"));
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

        String normalized = value.replace("\r", "").trim();

        if (normalized.contains("||")) {
            separatorRegex = "\\|\\|";
        } else if (normalized.contains("\n")) {
            separatorRegex = "\\n";
        }

        for (String item : Arrays.asList(normalized.split(separatorRegex))) {
            String trimmed = item.trim();
            if (trimmed.startsWith("- ")) {
                trimmed = trimmed.substring(2).trim();
            }
            if (!trimmed.isEmpty()) {
                items.add(trimmed);
            }
        }
        return items;
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
