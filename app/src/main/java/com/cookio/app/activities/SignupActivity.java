package com.cookio.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cookio.app.R;
import com.cookio.app.utils.UsernameHelper;
import com.cookio.app.utils.AuthVerificationHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {
    private TextInputLayout nameInputLayout;
    private TextInputLayout usernameInputLayout;
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputLayout confirmPasswordInputLayout;
    private TextInputEditText nameEditText;
    private TextInputEditText usernameEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText confirmPasswordEditText;
    private Button signupButton;
    private Button backButton;
    private TextView loginLink;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        nameInputLayout = findViewById(R.id.name_input_layout);
        usernameInputLayout = findViewById(R.id.username_input_layout);
        emailInputLayout = findViewById(R.id.email_input_layout);
        passwordInputLayout = findViewById(R.id.password_input_layout);
        confirmPasswordInputLayout = findViewById(R.id.confirm_password_input_layout);
        nameEditText = findViewById(R.id.name_edit_text);
        usernameEditText = findViewById(R.id.username_edit_text);
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        confirmPasswordEditText = findViewById(R.id.confirm_password_edit_text);
        signupButton = findViewById(R.id.signup_button);
        backButton = findViewById(R.id.back_button);
        loginLink = findViewById(R.id.login_link);

        backButton.setOnClickListener(v -> finish());
        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        });
        signupButton.setOnClickListener(v -> attemptSignup());
    }

    private void attemptSignup() {
        String name = readField(nameEditText);
        String username = UsernameHelper.normalize(readField(usernameEditText));
        String email = readField(emailEditText);
        String password = readField(passwordEditText);
        String confirmPassword = readField(confirmPasswordEditText);

        nameInputLayout.setError(null);
        usernameInputLayout.setError(null);
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);

        if (TextUtils.isEmpty(name)) {
            nameInputLayout.setError(getString(R.string.error_name_required));
            return;
        }

        if (TextUtils.isEmpty(username)) {
            usernameInputLayout.setError(getString(R.string.error_username_required));
            return;
        }

        if (!UsernameHelper.isValid(username)) {
            usernameInputLayout.setError(getString(R.string.error_username_invalid));
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

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInputLayout.setError(getString(R.string.error_confirm_password_required));
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInputLayout.setError(getString(R.string.error_password_mismatch));
            return;
        }

        setLoading(true);
        verifyLegacyUsernameAvailability(username, null, () ->
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                saveUserDocument(name, username);
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
                        }));
    }

    private void saveUserDocument(String name, String username) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            setLoading(false);
            Toast.makeText(this, R.string.user_profile_create_failed, Toast.LENGTH_LONG).show();
            return;
        }

        DocumentReference userRef = firestore.collection("users").document(firebaseUser.getUid());
        DocumentReference usernameRef = firestore.collection("usernames").document(username);

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", firebaseUser.getUid());
        userData.put("name", name);
        userData.put("username", username);
        userData.put("email", firebaseUser.getEmail());
        userData.put("bio", "");
        userData.put("profileImageUrl", "");
        userData.put("followerCount", 0);
        userData.put("followingCount", 0);
        userData.put("createdAt", FieldValue.serverTimestamp());

        Map<String, Object> usernameData = new HashMap<>();
        usernameData.put("uid", firebaseUser.getUid());
        usernameData.put("username", username);
        usernameData.put("createdAt", FieldValue.serverTimestamp());

        firestore.runTransaction(transaction -> {
                    if (transaction.get(usernameRef).exists()) {
                        throw new IllegalStateException(getString(R.string.error_username_taken));
                    }
                    transaction.set(userRef, userData);
                    transaction.set(usernameRef, usernameData);
                    return null;
                })
                .addOnSuccessListener(unused -> {
                    AuthVerificationHelper.sendVerificationEmail(
                            this,
                            firebaseUser,
                            () -> {
                                auth.signOut();
                                setLoading(false);

                                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                                intent.putExtra("prefill_email", firebaseUser.getEmail());
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                Toast.makeText(this, R.string.email_verification_sent, Toast.LENGTH_LONG).show();
                                finish();
                            },
                            () -> setLoading(false)
                    );
                })
                .addOnFailureListener(e -> {
                    cleanupFailedSignup(firebaseUser, e);
                });
    }

    private void setLoading(boolean isLoading) {
        signupButton.setEnabled(!isLoading);
        backButton.setEnabled(!isLoading);
        loginLink.setEnabled(!isLoading);
        signupButton.setText(isLoading ? R.string.creating_account : R.string.create_account);
    }

    private void cleanupFailedSignup(FirebaseUser firebaseUser, Exception error) {
        if (firebaseUser == null) {
            setLoading(false);
            Toast.makeText(this, getReadableSignupError(error), Toast.LENGTH_LONG).show();
            return;
        }

        firebaseUser.delete()
                .addOnCompleteListener(task -> {
                    auth.signOut();
                    setLoading(false);
                    String message = getReadableSignupError(error);
                    if (TextUtils.equals(message, getString(R.string.error_username_taken))) {
                        usernameInputLayout.setError(message);
                        usernameEditText.requestFocus();
                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String getReadableSignupError(Exception error) {
        if (error != null && !TextUtils.isEmpty(error.getMessage())) {
            return error.getMessage();
        }
        return getString(R.string.user_profile_create_failed);
    }

    private void verifyLegacyUsernameAvailability(String username, String currentUid, Runnable onAvailable) {
        firestore.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        onAvailable.run();
                        return;
                    }

                    String existingUid = querySnapshot.getDocuments().get(0).getId();
                    if (currentUid != null && currentUid.equals(existingUid)) {
                        onAvailable.run();
                        return;
                    }

                    setLoading(false);
                    usernameInputLayout.setError(getString(R.string.error_username_taken));
                    usernameEditText.requestFocus();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, getString(R.string.user_profile_create_failed), Toast.LENGTH_LONG).show();
                });
    }

    private String readField(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
