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

public class LoginActivity extends AppCompatActivity {
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private Button loginButton;
    private Button backButton;
    private TextView signupLink;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        emailInputLayout = findViewById(R.id.email_input_layout);
        passwordInputLayout = findViewById(R.id.password_input_layout);
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);
        backButton = findViewById(R.id.back_button);
        signupLink = findViewById(R.id.signup_link);

        backButton.setOnClickListener(v -> finish());
        signupLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
        loginButton.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = readField(emailEditText);
        String password = readField(passwordEditText);

        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);

        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError(getString(R.string.error_email_required));
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError(getString(R.string.error_password_required));
            return;
        }

        setLoading(true);
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(LoginActivity.this, FeedActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
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

    private void setLoading(boolean isLoading) {
        loginButton.setEnabled(!isLoading);
        backButton.setEnabled(!isLoading);
        signupLink.setEnabled(!isLoading);
        loginButton.setText(isLoading ? R.string.logging_in : R.string.log_in);
    }

    private String readField(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
