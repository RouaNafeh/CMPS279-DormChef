package database;

import com.cookio.app.models.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreRepository {
    private final FirebaseFirestore db;

    public FirestoreRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public interface OnActionListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnUserLoadedListener {
        void onSuccess(User user);
        void onFailure(Exception e);
    }

    public interface OnBooleanResultListener {
        void onSuccess(boolean result);
        void onFailure(Exception e);
    }

    public interface onFollowingIdsLoadedListener{
        void onSuccess(List<String> userIds);
        void onFailure(Exception e);
    }

    public void createUser(User user, OnActionListener listener) {
        db.collection("users")
                .document(user.getUid())
                .set(user)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    public void getUser(String userId, OnUserLoadedListener listener) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        listener.onSuccess(user);
                    } else {
                        listener.onFailure(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }

    private Map<String, Object> buildConnectionData(String userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", userId);
        data.put("createdAt", FieldValue.serverTimestamp());
        return data;
    }

    //---Following system---
    public void followUser(User currentUser, User targetUser, OnActionListener listener){
        if(currentUser.getUid().equals(targetUser.getUid())){
            listener.onFailure(new Exception("You cannot follow yourself"));
            return;
        }

        db.runTransaction(transaction -> {
                    var followingRef = db.collection("users")
                            .document(currentUser.getUid())
                            .collection("following")
                            .document(targetUser.getUid());

                    var followerRef = db.collection("users")
                            .document(targetUser.getUid())
                            .collection("followers")
                            .document(currentUser.getUid());

                    DocumentSnapshot existing = transaction.get(followingRef);
                    if (existing.exists()) {
                        return null;
                    }

                    transaction.set(followingRef, buildConnectionData(targetUser.getUid()));
                    transaction.set(followerRef, buildConnectionData(currentUser.getUid()));
                    transaction.update(
                            db.collection("users").document(currentUser.getUid()),
                            "followingCount",
                            FieldValue.increment(1)
                    );
                    transaction.update(
                            db.collection("users").document(targetUser.getUid()),
                            "followerCount",
                            FieldValue.increment(1)
                    );
                    return null;
                })
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    public void unfollowUser(String currentUserId, String targetUserId, OnActionListener listener){
        if(currentUserId.equals(targetUserId)){
            listener.onFailure(new Exception("You cannot unfollow yourself"));
            return;
        }

        db.runTransaction(transaction -> {
                    var followingRef = db.collection("users")
                            .document(currentUserId)
                            .collection("following")
                            .document(targetUserId);

                    var followerRef = db.collection("users")
                            .document(targetUserId)
                            .collection("followers")
                            .document(currentUserId);

                    DocumentSnapshot existing = transaction.get(followingRef);
                    if (!existing.exists()) {
                        return null;
                    }

                    transaction.delete(followingRef);
                    transaction.delete(followerRef);
                    transaction.update(
                            db.collection("users").document(currentUserId),
                            "followingCount",
                            FieldValue.increment(-1)
                    );
                    transaction.update(
                            db.collection("users").document(targetUserId),
                            "followerCount",
                            FieldValue.increment(-1)
                    );
                    return null;
                })
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    public void isFollowing(String currentUserId, String targetUserId, OnBooleanResultListener listener){
        db.collection("users")
                .document(currentUserId)
                .collection("following")
                .document(targetUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    listener.onSuccess(documentSnapshot.exists());
                })
                .addOnFailureListener(listener::onFailure);
    }

    public void getFollowingIds(String userId, onFollowingIdsLoadedListener listener){
        db.collection("users")
                .document(userId)
                .collection("following")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> ids = new ArrayList<>();
                    for(DocumentSnapshot doc : queryDocumentSnapshots){
                        ids.add(doc.getId());
                    }
                    listener.onSuccess(ids);
                })
                .addOnFailureListener(listener::onFailure);
    }
}
