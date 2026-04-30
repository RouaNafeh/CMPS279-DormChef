package database;

import com.cookio.app.models.Recipe;
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

    public interface OnRecipeLoadedListener {
        void onSuccess(List<Recipe> recipes);
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

    public void createRecipe(Recipe recipe, OnActionListener listener) {
        String recipeId = recipe.getRecipeId();
        if (recipeId == null || recipeId.trim().isEmpty()) {
            recipeId = db.collection("recipes").document().getId();
            recipe.setRecipeId(recipeId);
        }

        if (recipe.getCreatedAt() == 0L) {
            recipe.setCreatedAt(System.currentTimeMillis());
        }

        db.collection("recipes")
                .document(recipeId)
                .set(recipeToMap(recipe))
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    public void getAllRecipes(OnRecipeLoadedListener listener) {
        db.collection("recipes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Recipe> recipes = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        recipes.add(documentToRecipe(doc));
                    }
                    listener.onSuccess(recipes);
                })
                .addOnFailureListener(listener::onFailure);
    }

    public void getRecipesByUser(String userId, OnRecipeLoadedListener listener) {
        db.collection("recipes")
                .whereEqualTo("authorId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Recipe> recipes = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        recipes.add(documentToRecipe(doc));
                    }
                    listener.onSuccess(recipes);
                })
                .addOnFailureListener(listener::onFailure);
    }

    public void saveRecipe(String userId, Recipe recipe, OnActionListener listener) {
        if (recipe.getRecipeId() == null || recipe.getRecipeId().trim().isEmpty()) {
            listener.onFailure(new Exception("Recipe ID is missing"));
            return;
        }

        db.collection("users")
                .document(userId)
                .collection("savedRecipes")
                .document(recipe.getRecipeId())
                .set(recipeToMap(recipe))
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    public void unsaveRecipe(String userId, String recipeId, OnActionListener listener) {
        db.collection("users")
                .document(userId)
                .collection("savedRecipes")
                .document(recipeId)
                .delete()
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    public void getSavedRecipes(String userId, OnRecipeLoadedListener listener) {
        db.collection("users")
                .document(userId)
                .collection("savedRecipes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Recipe> recipes = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        recipes.add(documentToRecipe(doc));
                    }
                    listener.onSuccess(recipes);
                })
                .addOnFailureListener(listener::onFailure);
    }

    public void isRecipeSaved(String userId, String recipeId, OnBooleanResultListener listener) {
        db.collection("users")
                .document(userId)
                .collection("savedRecipes")
                .document(recipeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> listener.onSuccess(documentSnapshot.exists()))
                .addOnFailureListener(listener::onFailure);
    }

    private Map<String, Object> recipeToMap(Recipe recipe) {
        Map<String, Object> data = new HashMap<>();
        data.put("recipeId", recipe.getRecipeId());
        data.put("authorId", recipe.getAuthorId());
        data.put("authorName", recipe.getAuthorName());
        data.put("title", recipe.getTitle());
        data.put("description", recipe.getDescription());
        data.put("time", recipe.getTime());
        data.put("budget", recipe.getBudget());
        data.put("equipment", recipe.getEquipment());
        data.put("ingredients", splitLines(recipe.getIngredients()));
        data.put("steps", splitLines(recipe.getSteps()));
        data.put("imageUrl", recipe.getImageUrl());
        data.put("createdAt", recipe.getCreatedAt());
        return data;
    }

    private Recipe documentToRecipe(DocumentSnapshot doc) {
        Recipe recipe = new Recipe();
        recipe.setRecipeId(valueOrDefault(doc.getString("recipeId"), doc.getId()));
        recipe.setAuthorId(doc.getString("authorId"));
        recipe.setAuthorName(doc.getString("authorName"));
        recipe.setTitle(valueOrDefault(doc.getString("title"), doc.getString("name")));
        recipe.setDescription(doc.getString("description"));
        recipe.setTime(doc.getString("time"));
        recipe.setBudget(doc.getString("budget"));
        recipe.setEquipment(doc.getString("equipment"));
        recipe.setIngredients(joinLines(readStringList(doc.get("ingredients"), doc.getString("ingredients"))));
        recipe.setSteps(joinLines(readStringList(doc.get("steps"), doc.getString("steps"))));
        recipe.setImageUrl(valueOrDefault(doc.getString("imageUrl"), doc.getString("imageUri")));

        Long createdAt = doc.getLong("createdAt");
        recipe.setCreatedAt(createdAt != null ? createdAt : 0L);
        return recipe;
    }

    private List<String> splitLines(String rawValue) {
        List<String> values = new ArrayList<>();
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return values;
        }

        String[] lines = rawValue.split("\\r?\\n|\\|");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    private List<String> readStringList(Object fieldValue, String fallbackString) {
        List<String> values = new ArrayList<>();
        if (fieldValue instanceof List<?>) {
            for (Object item : (List<?>) fieldValue) {
                if (item != null) {
                    String trimmed = item.toString().trim();
                    if (!trimmed.isEmpty()) {
                        values.add(trimmed);
                    }
                }
            }
            return values;
        }

        if (fallbackString == null || fallbackString.trim().isEmpty()) {
            return values;
        }

        String[] lines = fallbackString.split("\\r?\\n|\\|");
        for (String line : lines) {
            String[] parts = line.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    values.add(trimmed);
                }
            }
        }
        return values;
    }

    private String joinLines(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join("\n", values);
    }

    private String valueOrDefault(String value, String fallback) {
        return value != null ? value : fallback;
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
