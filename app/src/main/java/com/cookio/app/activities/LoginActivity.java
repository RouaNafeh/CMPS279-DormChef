package com.cookio.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.cookio.app.R;
import com.cookio.app.utils.AuthVerificationHelper;
import com.cookio.app.utils.GoogleAuthHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private Button loginButton;
    private Button googleSignInButton;
    private Button backButton;
    private TextView forgotPasswordLink;
    private TextView signupLink;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() == null) {
                    setLoading(false);
                    return;
                }

                GoogleAuthHelper.completeSignIn(
                        this,
                        result.getData(),
                        auth,
                        firestore,
                        new GoogleAuthHelper.Callback() {
                            @Override
                            public void onSuccess() {
                                openHome();
                            }

                            @Override
                            public void onFailure(String message) {
                                setLoading(false);
                                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onCanceled() {
                                setLoading(false);
                            }
                        }
                );
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        emailInputLayout = findViewById(R.id.email_input_layout);
        passwordInputLayout = findViewById(R.id.password_input_layout);
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);
        googleSignInButton = findViewById(R.id.google_sign_in_button);
        backButton = findViewById(R.id.back_button);
        forgotPasswordLink = findViewById(R.id.forgot_password_link);
        signupLink = findViewById(R.id.signup_link);

        String prefillEmail = getIntent().getStringExtra("prefill_email");
        if (!TextUtils.isEmpty(prefillEmail)) {
            emailEditText.setText(prefillEmail);
        }

        backButton.setOnClickListener(v -> finish());
        signupLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
        forgotPasswordLink.setOnClickListener(v -> sendPasswordReset());
        loginButton.setOnClickListener(v -> attemptLogin());
        googleSignInButton.setOnClickListener(v -> startGoogleSignIn());
    }

    private void sendPasswordReset() {
        String email = readField(emailEditText);
        emailInputLayout.setError(null);

        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError(getString(R.string.enter_email_for_reset));
            emailEditText.requestFocus();
            return;
        }

        forgotPasswordLink.setEnabled(false);
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> Toast.makeText(
                        this,
                        R.string.password_reset_sent,
                        Toast.LENGTH_LONG
                ).show())
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        e.getMessage() == null ? getString(R.string.password_reset_failed) : e.getMessage(),
                        Toast.LENGTH_LONG
                ).show())
                .addOnCompleteListener(task -> forgotPasswordLink.setEnabled(true));
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
                    if (task.isSuccessful()) {
                        verifyEmailAndContinue();
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

    private void verifyEmailAndContinue() {
        AuthVerificationHelper.verifyBeforeEntering(this, auth, new AuthVerificationHelper.VerificationCallback() {
            @Override
            public void onVerified(FirebaseUser user) {
                openHome();
            }

            @Override
            public void onRejected() {
                setLoading(false);
                showVerificationDialog();
            }
        });
    }

    private void showVerificationDialog() {
        FirebaseUser user = auth.getCurrentUser();

        new AlertDialog.Builder(this)
                .setTitle(R.string.email_verification_dialog_title)
                .setMessage(R.string.email_verification_dialog_message)
                .setNegativeButton(R.string.profile_edit_dialog_cancel, (dialog, which) -> {
                    auth.signOut();
                    Toast.makeText(LoginActivity.this, R.string.email_verification_required, Toast.LENGTH_LONG).show();
                })
                .setPositiveButton(R.string.email_verification_resend, (dialog, which) -> {
                    if (user == null) {
                        Toast.makeText(LoginActivity.this, R.string.email_verification_required, Toast.LENGTH_LONG).show();
                        return;
                    }

                    AuthVerificationHelper.sendVerificationEmail(
                            LoginActivity.this,
                            user,
                            () -> {
                                Toast.makeText(LoginActivity.this, R.string.email_verification_resent, Toast.LENGTH_LONG).show();
                                auth.signOut();
                            },
                            () -> auth.signOut()
                    );
                })
                .setOnDismissListener(dialog -> {
                    if (auth.getCurrentUser() != null && !auth.getCurrentUser().isEmailVerified()) {
                        auth.signOut();
                    }
                })
                .show();
    }

    private void setLoading(boolean isLoading) {
        loginButton.setEnabled(!isLoading);
        googleSignInButton.setEnabled(!isLoading);
        backButton.setEnabled(!isLoading);
        forgotPasswordLink.setEnabled(!isLoading);
        signupLink.setEnabled(!isLoading);
        loginButton.setText(isLoading ? R.string.logging_in : R.string.log_in);
    }

    private void startGoogleSignIn() {
        setLoading(true);
        googleSignInLauncher.launch(GoogleAuthHelper.createSignInIntent(this));
    }

    private void openHome() {
        setLoading(false);
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String readField(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
