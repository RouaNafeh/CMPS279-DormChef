package com.cookio.app.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cookio.app.R;
import com.cookio.app.models.Post;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final Context context;
    private List<Post> postList;
    private final Set<String> savedPostIds;
    private final Set<String> likedPostIds;
    private final String currentUid;

    private OnPostUnsavedListener onPostUnsavedListener;

    public PostAdapter(Context context, List<Post> postList,
                       Set<String> savedPostIds, Set<String> likedPostIds) {
        this.context = context;
        this.postList = postList;
        this.savedPostIds = savedPostIds;
        this.likedPostIds = likedPostIds;
        this.currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    public void updateData(List<Post> newList) {
        this.postList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.postTitle.setText(post.getTitle());
        holder.postDescription.setText(post.getDescription());
        holder.postCookTime.setText(post.getCookTime());
        holder.postBudget.setText(post.getBudget());
        holder.likesCount.setText(String.valueOf(post.getLikesCount()));

        if (post.getImageUrl() != null && !post.getImageUrl().trim().isEmpty()) {
            holder.postImage.setImageURI(Uri.parse(post.getImageUrl()));
        } else {
            holder.postImage.setImageResource(R.drawable.logo_cropped);
        }

        boolean isSaved = savedPostIds.contains(post.getPostId());
        boolean isLiked = likedPostIds.contains(post.getPostId());

        holder.saveButton.setImageResource(
                isSaved ? R.drawable.ic_save_filled : R.drawable.ic_save_outline
        );

        holder.likeButton.setImageResource(
                isLiked ? R.drawable.heart_filled : R.drawable.heart
        );

        holder.saveButton.setOnClickListener(v -> toggleSave(post, holder));
        holder.likeButton.setOnClickListener(v -> toggleLike(post, holder));

        holder.itemView.setOnClickListener(v -> {
            // later open social recipe detail if you have one
            Toast.makeText(context, post.getTitle(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    private void toggleSave(Post post, PostViewHolder holder) {
        if (currentUid == null) return;

        String postId = post.getPostId();
        boolean currentlySaved = savedPostIds.contains(postId);

        if (currentlySaved) {
            savedPostIds.remove(postId);
            holder.saveButton.setImageResource(R.drawable.ic_save_outline);
        } else {
            savedPostIds.add(postId);
            holder.saveButton.setImageResource(R.drawable.ic_save_filled);
        }

        DocumentReference savedRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUid)
                .collection("savedPosts")
                .document(postId);

        if (currentlySaved) {
            savedRef.delete()
                    .addOnSuccessListener(unused -> {
                        if (onPostUnsavedListener != null) {
                            onPostUnsavedListener.onPostUnsaved(postId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        savedPostIds.add(postId);
                        holder.saveButton.setImageResource(R.drawable.ic_save_filled);
                        Toast.makeText(context, "Failed to unsave post", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("savedAt", FieldValue.serverTimestamp());

            savedRef.set(data)
                    .addOnFailureListener(e -> {
                        savedPostIds.remove(postId);
                        holder.saveButton.setImageResource(R.drawable.ic_save_outline);
                        Toast.makeText(context, "Failed to save post", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void toggleLike(Post post, PostViewHolder holder) {
        if (currentUid == null) return;

        String postId = post.getPostId();
        boolean currentlyLiked = likedPostIds.contains(postId);

        if (currentlyLiked) {
            likedPostIds.remove(postId);
            holder.likeButton.setImageResource(R.drawable.heart);
            post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
        } else {
            likedPostIds.add(postId);
            holder.likeButton.setImageResource(R.drawable.heart_filled);
            post.setLikesCount(post.getLikesCount() + 1);
        }

        holder.likesCount.setText(String.valueOf(post.getLikesCount()));

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference postRef = db.collection("posts").document(postId);
        DocumentReference likeRef = postRef.collection("likes").document(currentUid);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(postRef);
            Long currentCountObj = snapshot.getLong("likesCount");
            long currentCount = currentCountObj == null ? 0 : currentCountObj;

            if (currentlyLiked) {
                transaction.delete(likeRef);
                transaction.update(postRef, "likesCount", Math.max(0, currentCount - 1));
            } else {
                Map<String, Object> likeData = new HashMap<>();
                likeData.put("likedAt", FieldValue.serverTimestamp());
                transaction.set(likeRef, likeData);
                transaction.update(postRef, "likesCount", currentCount + 1);
            }

            return null;
        }).addOnFailureListener(e -> {
            if (currentlyLiked) {
                likedPostIds.add(postId);
                holder.likeButton.setImageResource(R.drawable.heart_filled);
                post.setLikesCount(post.getLikesCount() + 1);
            } else {
                likedPostIds.remove(postId);
                holder.likeButton.setImageResource(R.drawable.heart);
                post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
            }

            holder.likesCount.setText(String.valueOf(post.getLikesCount()));
            Toast.makeText(context, "Failed to update like", Toast.LENGTH_SHORT).show();
        });
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView postTitle, postDescription, postCookTime, postBudget, likesCount;
        ImageView postImage;
        ImageButton saveButton, likeButton;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            postTitle = itemView.findViewById(R.id.post_title);
            postDescription = itemView.findViewById(R.id.post_description);
            postCookTime = itemView.findViewById(R.id.post_time);
            postBudget = itemView.findViewById(R.id.post_budget);
            likesCount = itemView.findViewById(R.id.likes_count);
            postImage = itemView.findViewById(R.id.post_image);
            saveButton = itemView.findViewById(R.id.btn_save);
            likeButton = itemView.findViewById(R.id.btn_like);
        }
    }
    public interface OnPostUnsavedListener {
        void onPostUnsaved(String postId);
    }

    public void setOnPostUnsavedListener(OnPostUnsavedListener listener) {
        this.onPostUnsavedListener = listener;
    }
}