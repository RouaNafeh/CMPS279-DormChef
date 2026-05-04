package com.cookio.app.utils;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cookio.app.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class GoogleAuthHelper {

    public interface Callback {
        void onSuccess();
        void onFailure(String message);
        void onCanceled();
    }

    private GoogleAuthHelper() {
    }

    public static Intent createSignInIntent(@NonNull Context context) {
        return getClient(context).getSignInIntent();
    }

    public static void completeSignIn(@NonNull AppCompatActivity activity,
                                      @NonNull Intent data,
                                      @NonNull FirebaseAuth auth,
                                      @NonNull FirebaseFirestore firestore,
                                      @NonNull Callback callback) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener(account -> {
                    String idToken = account.getIdToken();
                    if (TextUtils.isEmpty(idToken)) {
                        callback.onFailure("Google sign-in failed: missing ID token.");
                        return;
                    }

                    AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
                    auth.signInWithCredential(credential)
                            .addOnSuccessListener(result -> {
                                FirebaseUser user = result.getUser();
                                if (user == null) {
                                    callback.onFailure(activity.getString(R.string.auth_failed_message));
                                    return;
                                }

                                ensureUserProfile(firestore, user, account, new Callback() {
                                    @Override
                                    public void onSuccess() {
                                        callback.onSuccess();
                                    }

                                    @Override
                                    public void onFailure(String message) {
                                        callback.onFailure(message);
                                    }

                                    @Override
                                    public void onCanceled() {
                                        callback.onCanceled();
                                    }
                                });
                            })
                            .addOnFailureListener(e -> callback.onFailure(
                                    e.getMessage() == null
                                            ? activity.getString(R.string.auth_failed_message)
                                            : e.getMessage()
                            ));
                })
                .addOnFailureListener(e -> {
                    if (e instanceof ApiException) {
                        ApiException apiException = (ApiException) e;
                        if (apiException.getStatusCode() == 12501 || apiException.getStatusCode() == 16) {
                            callback.onCanceled();
                            return;
                        }
                    }

                    callback.onFailure(
                            e.getMessage() == null
                                    ? activity.getString(R.string.auth_failed_message)
                                    : e.getMessage()
                    );
                });
    }

    private static GoogleSignInClient getClient(@NonNull Context context) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        return GoogleSignIn.getClient(context, gso);
    }

    private static void ensureUserProfile(@NonNull FirebaseFirestore firestore,
                                          @NonNull FirebaseUser user,
                                          @NonNull GoogleSignInAccount account,
                                          @NonNull Callback callback) {
        DocumentReference userRef = firestore.collection("users").document(user.getUid());
        userRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        callback.onSuccess();
                        return;
                    }

                    String displayName = firstNonEmpty(
                            account.getDisplayName(),
                            user.getDisplayName(),
                            "Chef"
                    );

                    String photoUrl = account.getPhotoUrl() == null
                            ? ""
                            : account.getPhotoUrl().toString();

                    reserveGeneratedUsername(
                            firestore,
                            user.getUid(),
                            buildUsernameSeed(displayName, user.getEmail()),
                            0,
                            reservedUsername -> createUserProfile(
                                    firestore,
                                    userRef,
                                    user,
                                    displayName,
                                    reservedUsername,
                                    photoUrl,
                                    callback
                            ),
                            callback::onFailure
                    );
                })
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() == null ? "Failed to load Google profile." : e.getMessage()
                ));
    }

    private static void createUserProfile(@NonNull FirebaseFirestore firestore,
                                          @NonNull DocumentReference userRef,
                                          @NonNull FirebaseUser user,
                                          @NonNull String displayName,
                                          @NonNull String username,
                                          @NonNull String photoUrl,
                                          @NonNull Callback callback) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("name", displayName);
        userData.put("username", username);
        userData.put("email", user.getEmail() == null ? "" : user.getEmail());
        userData.put("bio", "");
        userData.put("profileImageUrl", photoUrl);
        userData.put("followerCount", 0);
        userData.put("followingCount", 0);
        userData.put("createdAt", FieldValue.serverTimestamp());

        userRef.set(userData)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() == null ? "Failed to create Google profile." : e.getMessage()
                ));
    }

    private interface UsernameReservedCallback {
        void onReserved(String username);
    }

    private static void reserveGeneratedUsername(@NonNull FirebaseFirestore firestore,
                                                 @NonNull String uid,
                                                 @NonNull String seed,
                                                 int attempt,
                                                 @NonNull UsernameReservedCallback onReserved,
                                                 @NonNull FailureCallback onFailure) {
        if (attempt > 50) {
            onFailure.onFailure("Could not generate a unique username for Google sign-in.");
            return;
        }

        String candidate = buildUsernameCandidate(seed, attempt);
        DocumentReference usernameRef = firestore.collection("usernames").document(candidate);

        firestore.runTransaction(transaction -> {
                    if (transaction.get(usernameRef).exists()) {
                        throw new IllegalStateException("USERNAME_TAKEN");
                    }

                    Map<String, Object> usernameData = new HashMap<>();
                    usernameData.put("uid", uid);
                    usernameData.put("username", candidate);
                    usernameData.put("createdAt", FieldValue.serverTimestamp());
                    transaction.set(usernameRef, usernameData);
                    return candidate;
                })
                .addOnSuccessListener(result -> onReserved.onReserved(candidate))
                .addOnFailureListener(e -> {
                    if ("USERNAME_TAKEN".equals(e.getMessage())) {
                        reserveGeneratedUsername(
                                firestore,
                                uid,
                                seed,
                                attempt + 1,
                                onReserved,
                                onFailure
                        );
                        return;
                    }

                    onFailure.onFailure(
                            e.getMessage() == null
                                    ? "Failed to reserve a username for Google sign-in."
                                    : e.getMessage()
                    );
                });
    }

    private interface FailureCallback {
        void onFailure(String message);
    }

    private static String buildUsernameSeed(String displayName, String email) {
        String base = sanitizeUsername(firstNonEmpty(displayName, email, "chef"));
        if (base.length() >= 3) {
            return base;
        }

        String fromEmail = sanitizeUsername(email);
        if (fromEmail.length() >= 3) {
            return fromEmail;
        }

        return "chef";
    }

    private static String buildUsernameCandidate(String seed, int attempt) {
        String suffix = attempt == 0 ? "" : String.valueOf(attempt);
        int maxSeedLength = Math.max(3, 20 - suffix.length());
        String trimmedSeed = seed.length() > maxSeedLength
                ? seed.substring(0, maxSeedLength)
                : seed;
        String candidate = trimmedSeed + suffix;
        if (candidate.length() < 3) {
            candidate = (candidate + "chef").substring(0, 3);
        }
        return candidate;
    }

    private static String sanitizeUsername(String rawValue) {
        if (rawValue == null) {
            return "";
        }

        String normalized = rawValue.trim().toLowerCase(Locale.getDefault());
        int atIndex = normalized.indexOf('@');
        if (atIndex > 0) {
            normalized = normalized.substring(0, atIndex);
        }

        normalized = normalized.replaceAll("[^a-z0-9._]", "");
        normalized = normalized.replaceAll("^[._]+|[._]+$", "");
        return UsernameHelper.normalize(normalized);
    }

    private static String firstNonEmpty(String first, String second, String fallback) {
        if (!TextUtils.isEmpty(first)) {
            return first.trim();
        }
        if (!TextUtils.isEmpty(second)) {
            return second.trim();
        }
        return fallback;
    }
}
