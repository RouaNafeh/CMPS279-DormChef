package com.cookio.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.cookio.app.R;
import com.cookio.app.databinding.ActivityCreatePostBinding;
import com.cookio.app.models.Post;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CreatePostActivity extends AppCompatActivity {

    private ActivityCreatePostBinding binding;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    private Uri selectedImageUri = null; // null = no image selected yet

    // ── Image picker launcher ─────────────────────────────────
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    binding.ivImagePreview.setImageURI(uri);
                    binding.ivImagePreview.setVisibility(View.VISIBLE);
                    binding.imagePlaceholder.setVisibility(View.GONE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth      = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage   = FirebaseStorage.getInstance();

        // Redirect to login if not authenticated
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LandingActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        binding = ActivityCreatePostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Tap anywhere on the image container to open gallery
        binding.imagePickerContainer.setOnClickListener(v ->
                imagePickerLauncher.launch("image/*"));

        binding.btnPost.setOnClickListener(v -> validateAndPost());
    }

    // ── Validate then upload or save directly ─────────────────
    private void validateAndPost() {
        String title       = text(binding.etTitle);
        String description = text(binding.etDescription);
        String cookTime    = text(binding.etCookTime);
        String budget      = text(binding.etBudget);
        String ingredients = text(binding.etIngredients);
        String steps       = text(binding.etSteps);

        if (TextUtils.isEmpty(title)) {
            binding.etTitle.setError("Title is required");
            binding.etTitle.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(ingredients)) {
            binding.etIngredients.setError("Add at least one ingredient");
            binding.etIngredients.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(steps)) {
            binding.etSteps.setError("Add at least one step");
            binding.etSteps.requestFocus();
            return;
        }

        setLoading(true);

        if (selectedImageUri != null) {
            uploadImageThenSave(title, description, cookTime, budget, ingredients, steps);
        } else {
            // Post without an image — imageUrl will be empty string
            savePostToFirestore(title, description, cookTime, budget, ingredients, steps, "");
        }
    }

    // ── Step 1: Upload image to Firebase Storage ──────────────
    private void uploadImageThenSave(String title, String description, String cookTime,
                                     String budget, String ingredients, String steps) {

        // Store under posts/{uid}/{randomUUID}.jpg
        String uid      = auth.getCurrentUser().getUid();
        String filename = UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = storage.getReference()
                .child("posts")
                .child(uid)
                .child(filename);

        ref.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            // Step 2: save Firestore doc with the image URL
                            savePostToFirestore(title, description, cookTime, budget,
                                    ingredients, steps, downloadUri.toString());
                        }))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // ── Step 2: Save post document to Firestore ───────────────
    private void savePostToFirestore(String title, String description, String cookTime,
                                     String budget, String ingredients, String steps,
                                     String imageUrl) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { setLoading(false); return; }

        // Read author username from Firestore users/{uid}
        firestore.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(userDoc -> {
                    String username = userDoc.getString("username");
                    if (username == null) username = "Unknown";

                    // Split multiline text into lists (trim blank lines)
                    List<String> ingredientList = splitLines(ingredients);
                    List<String> stepList       = splitLines(steps);

                    Post post = new Post(
                            user.getUid(),
                            username,
                            title,
                            description,
                            cookTime,
                            budget,
                            ingredientList,
                            stepList,
                            imageUrl
                    );

                    // Auto-generate doc ID — save postId back into the document
                    firestore.collection("posts")
                            .add(post)
                            .addOnSuccessListener(docRef -> {
                                // Write the auto-generated ID into the doc itself
                                docRef.update("postId", docRef.getId())
                                        .addOnCompleteListener(t -> {
                                            setLoading(false);
                                            Toast.makeText(this,
                                                    "Recipe posted!", Toast.LENGTH_SHORT).show();
                                            finish(); // go back to feed / home
                                        });
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this,
                                        "Failed to post: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Could not load user info", Toast.LENGTH_SHORT).show();
                });
    }

    // ── Helpers ───────────────────────────────────────────────

    /** Splits a multiline string into a trimmed list, dropping blank lines. */
    private List<String> splitLines(String raw) {
        String[] lines = raw.split("\\n");
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }

    private String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void setLoading(boolean loading) {
        binding.btnPost.setEnabled(!loading);
        binding.btnPost.setText(loading ? "Posting…" : "Post Recipe");
    }
}