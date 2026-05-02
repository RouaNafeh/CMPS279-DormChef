package com.cookio.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.cookio.app.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {
    private static final long DEBOUNCE_DELAY_MS = 500;

    private TextInputLayout usernameInputLayout;
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputEditText usernameEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private Button signupButton;
    private Button backButton;
    private TextView loginLink;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingCheck;
    private boolean usernameAvailable = false;
    private String lastCheckedUsernameKey = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        usernameInputLayout = findViewById(R.id.username_input_layout);
        emailInputLayout = findViewById(R.id.email_input_layout);
        passwordInputLayout = findViewById(R.id.password_input_layout);
        usernameEditText = findViewById(R.id.username_edit_text);
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        signupButton = findViewById(R.id.signup_button);
        backButton = findViewById(R.id.back_button);
        loginLink = findViewById(R.id.login_link);

        usernameInputLayout.setHelperTextEnabled(true);

        backButton.setOnClickListener(v -> finish());
        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        });
        signupButton.setOnClickListener(v -> attemptSignup());

        // Live availability check
        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                onUsernameTyped(s.toString().trim());
            }
        });
    }

    private void onUsernameTyped(String username) {
        usernameInputLayout.setError(null);
        usernameAvailable = false;

        // Cancel any previous pending check
        if (pendingCheck != null) {
            debounceHandler.removeCallbacks(pendingCheck);
        }

        if (TextUtils.isEmpty(username)) {
            clearHelper();
            return;
        }

        if (username.length() < 3) {
            showHelperText("Username must be at least 3 characters", R.color.textMuted);
            return;
        }

        showHelperText("Checking availability…", R.color.textMuted);

        // Debounce: wait until user stops typing
        pendingCheck = () -> checkUsernameAvailability(username);
        debounceHandler.postDelayed(pendingCheck, DEBOUNCE_DELAY_MS);
    }

    private void checkUsernameAvailability(String username) {
        String usernameKey = username.toLowerCase(Locale.ROOT);
        lastCheckedUsernameKey = usernameKey;

        firestore.collection("usernames")
                .document(usernameKey)
                .get()
                .addOnSuccessListener(doc -> {
                    // Only act on the most recent check (ignore stale results)
                    String currentText = readField(usernameEditText).toLowerCase(Locale.ROOT);
                    if (!currentText.equals(usernameKey)) {
                        return;
                    }

                    if (doc.exists()) {
                        usernameAvailable = false;
                        showHelperText("✗ Username already taken", R.color.holo_red_dark_safe);
                    } else {
                        usernameAvailable = true;
                        showHelperText("✓ Username available", R.color.holo_green_dark_safe);
                    }
                })
                .addOnFailureListener(e -> {
                    String currentText = readField(usernameEditText).toLowerCase(Locale.ROOT);
                    if (!currentText.equals(usernameKey)) {
                        return;
                    }
                    usernameAvailable = false;
                    showHelperText("Could not verify availability", R.color.textMuted);
                });
    }

    private void showHelperText(String text, int colorRes) {
        usernameInputLayout.setHelperText(text);
        try {
            int color = ContextCompat.getColor(this, colorRes);
            usernameInputLayout.setHelperTextColor(android.content.res.ColorStateList.valueOf(color));
        } catch (Exception ignored) {
            // If color resource doesn't exist, ignore — text will still show in default color
        }
    }

    private void clearHelper() {
        usernameInputLayout.setHelperText(null);
    }

    private void attemptSignup() {
        String username = readField(usernameEditText);
        String email = readField(emailEditText);
        String password = readField(passwordEditText);

        usernameInputLayout.setError(null);
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);

        if (TextUtils.isEmpty(username)) {
            usernameInputLayout.setError(getString(R.string.error_username_required));
            return;
        }

        if (username.length() < 3) {
            usernameInputLayout.setError("Username must be at least 3 characters");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError(getString(R.string.error_email_required));
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError(getString(R.string.error_password_required));
            return;
        }

        if (password.length() < 6) {
            passwordInputLayout.setError(getString(R.string.error_password_length));
            return;
        }

        setLoading(true);

        // Re-check at submit time (don't rely on stale live-check state)
        String usernameKey = username.toLowerCase(Locale.ROOT);
        firestore.collection("usernames")
                .document(usernameKey)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        setLoading(false);
                        usernameInputLayout.setError("Username already taken");
                        return;
                    }
                    createAuthUser(username, email, password);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Failed to verify username. Please try again.",
                            Toast.LENGTH_LONG).show();
                });
    }

    private void createAuthUser(String username, String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveUserDocument(username);
                    } else {
                        setLoading(false);
                        Toast.makeText(
                                this,
                                task.getException() != null
                                        ? task.getException().getMessage()
                                        : getString(R.string.auth_failed_message),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void saveUserDocument(String username) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            setLoading(false);
            Toast.makeText(this, R.string.user_profile_create_failed, Toast.LENGTH_LONG).show();
            return;
        }

        String uid = firebaseUser.getUid();
        String usernameKey = username.toLowerCase(Locale.ROOT);

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("username", username);
        userData.put("usernameKey", usernameKey);
        userData.put("email", firebaseUser.getEmail());
        userData.put("bio", "");
        userData.put("profileImageUrl", "");
        userData.put("createdAt", FieldValue.serverTimestamp());

        Map<String, Object> reservation = new HashMap<>();
        reservation.put("uid", uid);
        reservation.put("createdAt", FieldValue.serverTimestamp());

        DocumentReference userRef = firestore.collection("users").document(uid);
        DocumentReference usernameRef = firestore.collection("usernames").document(usernameKey);

        WriteBatch batch = firestore.batch();
        batch.set(userRef, userData);
        batch.set(usernameRef, reservation);

        batch.commit()
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Intent intent = new Intent(SignupActivity.this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    firebaseUser.delete();
                    Toast.makeText(
                            this,
                            getString(R.string.user_profile_create_failed),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void setLoading(boolean isLoading) {
        signupButton.setEnabled(!isLoading);
        backButton.setEnabled(!isLoading);
        loginLink.setEnabled(!isLoading);
        signupButton.setText(isLoading ? R.string.creating_account : R.string.create_account);
    }

    private String readField(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pendingCheck != null) {
            debounceHandler.removeCallbacks(pendingCheck);
        }
    }
}