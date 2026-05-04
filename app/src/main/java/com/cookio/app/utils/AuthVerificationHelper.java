package com.cookio.app.utils;

import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cookio.app.R;
import com.cookio.app.activities.LandingActivity;
import com.cookio.app.activities.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;

public final class AuthVerificationHelper {
    private AuthVerificationHelper() {
    }

    public interface VerificationCallback {
        void onVerified(@NonNull FirebaseUser user);
        void onRejected();
    }

    public static void sendVerificationEmail(
            @NonNull AppCompatActivity activity,
            @NonNull FirebaseUser user,
            @NonNull Runnable onSuccess,
            @NonNull Runnable onFailure
    ) {
        user.sendEmailVerification()
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            activity,
                            e.getMessage() == null
                                    ? activity.getString(R.string.email_verification_send_failed)
                                    : e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                    onFailure.run();
                });
    }

    public static void verifyBeforeEntering(
            @NonNull AppCompatActivity activity,
            @NonNull FirebaseAuth auth,
            @NonNull VerificationCallback callback
    ) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onRejected();
            return;
        }

        user.reload()
                .addOnSuccessListener(unused -> {
                    FirebaseUser refreshedUser = auth.getCurrentUser();
                    if (refreshedUser != null && canEnterApp(refreshedUser)) {
                        callback.onVerified(refreshedUser);
                    } else {
                        callback.onRejected();
                    }
                })
                .addOnFailureListener(e -> {
                    auth.signOut();
                    Toast.makeText(
                            activity,
                            e.getMessage() == null
                                    ? activity.getString(R.string.email_verification_check_failed)
                                    : e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                    callback.onRejected();
                });
    }

    public static void redirectToLogin(AppCompatActivity activity, boolean clearTask, String message) {
        Intent intent = new Intent(activity, LoginActivity.class);
        if (clearTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        activity.startActivity(intent);
        if (message != null && !message.trim().isEmpty()) {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        }
        activity.finish();
    }

    public static void redirectToLanding(AppCompatActivity activity, String message) {
        Intent intent = new Intent(activity, LandingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        if (message != null && !message.trim().isEmpty()) {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        }
        activity.finish();
    }

    public static boolean canEnterApp(@NonNull FirebaseUser user) {
        if (user.isEmailVerified()) {
            return true;
        }

        for (UserInfo info : user.getProviderData()) {
            if (GoogleAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                return true;
            }
        }
        return false;
    }
}
