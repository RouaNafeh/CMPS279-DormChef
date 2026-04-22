package com.cookio.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cookio.app.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {
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

        backButton.setOnClickListener(v -> finish());
        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        });
        signupButton.setOnClickListener(v -> attemptSignup());
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

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", firebaseUser.getUid());
        userData.put("username", username);
        userData.put("email", firebaseUser.getEmail());
        userData.put("createdAt", FieldValue.serverTimestamp());

        firestore.collection("users")
                .document(firebaseUser.getUid())
                .set(userData)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Intent intent = new Intent(SignupActivity.this, FeedActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
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
}
