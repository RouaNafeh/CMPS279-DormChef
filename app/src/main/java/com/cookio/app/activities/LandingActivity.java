package com.cookio.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.cookio.app.R;
import com.cookio.app.utils.AuthVerificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LandingActivity extends AppCompatActivity {
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();

        if (handleAuthDeepLink(getIntent())) {
            return;
        }

        if (auth.getCurrentUser() != null) {
            AuthVerificationHelper.verifyBeforeEntering(this, auth, new AuthVerificationHelper.VerificationCallback() {
                @Override
                public void onVerified(FirebaseUser user) {
                    Intent intent = new Intent(LandingActivity.this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onRejected() {
                    auth.signOut();
                    AuthVerificationHelper.redirectToLogin(
                            LandingActivity.this,
                            true,
                            getString(R.string.email_verification_required)
                    );
                }
            });
            return;
        }

        setContentView(R.layout.activity_landing);

        Button loginBtn = findViewById(R.id.login_btn);
        Button signupBtn = findViewById(R.id.signup_btn);

        loginBtn.setOnClickListener(v -> {
            Intent intent = new Intent(LandingActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        signupBtn.setOnClickListener(v -> {
            Intent intent = new Intent(LandingActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    private boolean handleAuthDeepLink(Intent intent) {
        if (intent == null || intent.getData() == null) {
            return false;
        }

        Uri data = intent.getData();
        if (!"cookio".equals(data.getScheme()) || !"auth-complete".equals(data.getHost())) {
            return false;
        }

        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(loginIntent);
        finish();
        return true;
    }
}
