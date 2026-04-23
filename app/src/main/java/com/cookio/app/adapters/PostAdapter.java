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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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

    public interface OnPostClickListener {
        void onPostClick(Post post);
    }

    public interface OnPostUnsavedListener {
        void onPostUnsaved(String postId);
    }

    public interface OnPostDeleteListener {
        void onPostDelete(Post post);
    }

    private final Context context;
    private List<Post> postList;
    private final Set<String> savedPostIds;
    private final Set<String> likedPostIds;
    private final String currentUid;
    private boolean isGridMode = false;

    private OnPostUnsavedListener onPostUnsavedListener;
    private OnPostClickListener onPostClickListener;

    private OnPostDeleteListener onPostDeleteListener;

    public PostAdapter(Context context, List<Post> postList,
                       Set<String> savedPostIds, Set<String> likedPostIds,
                       OnPostClickListener clickListener) {
        this(context, postList, savedPostIds, likedPostIds);
        this.onPostClickListener = clickListener;
    }

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

    public void removePostFromUI(String postId) {
        for (int i = 0; i < postList.size(); i++) {
            if (postList.get(i).getPostId().equals(postId)) {
                postList.remove(i);
                notifyItemRemoved(i);
                notifyItemRangeChanged(i, postList.size());
                return;
            }
        }
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

        String imageUrl = post.getImageUrl();
        Glide.with(holder.itemView)
                .load(imageUrl != null && !imageUrl.trim().isEmpty()
                        ? Uri.parse(imageUrl)
                        : R.drawable.logo_cropped)
                .placeholder(R.drawable.logo_cropped)
                .error(R.drawable.logo_cropped)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.postImage);

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
            if (onPostClickListener != null) {
                onPostClickListener.onPostClick(post);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (onPostDeleteListener != null) {
                onPostDeleteListener.onPostDelete(post);
            }
        });

        if (isGridMode) {

            holder.postDescription.setVisibility(View.GONE);
            holder.deleteButton.setVisibility(View.GONE);

            holder.postCookTime.setVisibility(View.GONE);
            holder.postBudget.setVisibility(View.GONE);
            holder.likesCount.setVisibility(View.GONE);
            holder.likeButton.setVisibility(View.GONE);
            holder.saveButton.setVisibility(View.GONE);

            holder.postTitle.setMaxLines(1);
            holder.postTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);

            ViewGroup.LayoutParams params = holder.postImage.getLayoutParams();
            params.height = 320;
            holder.postImage.setLayoutParams(params);

        } else {

            holder.postDescription.setVisibility(View.VISIBLE);
            holder.deleteButton.setVisibility(View.VISIBLE);

            holder.postCookTime.setVisibility(View.VISIBLE);
            holder.postBudget.setVisibility(View.VISIBLE);
            holder.likesCount.setVisibility(View.VISIBLE);
            holder.likeButton.setVisibility(View.VISIBLE);
            holder.saveButton.setVisibility(View.VISIBLE);

            holder.postTitle.setMaxLines(2);

            ViewGroup.LayoutParams params = holder.postImage.getLayoutParams();
            params.height = 104;
            holder.postImage.setLayoutParams(params);
        }
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
            Toast.makeText(context, "Failed to update like", Toast.LENGTH_SHORT).show();
        });
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView postTitle, postDescription, postCookTime, postBudget, likesCount;
        ImageView postImage;
        ImageButton saveButton, likeButton;

        ImageButton deleteButton;

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

            deleteButton = itemView.findViewById(R.id.btn_delete);
        }
    }

    public void setOnPostUnsavedListener(OnPostUnsavedListener listener) {
        this.onPostUnsavedListener = listener;
    }

    public void setOnPostDeleteListener(OnPostDeleteListener listener) {
        this.onPostDeleteListener = listener;
    }

    public void setGridMode(boolean isGridMode) {
        this.isGridMode = isGridMode;
        notifyDataSetChanged();
    }
}
