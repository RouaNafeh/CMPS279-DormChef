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
import com.google.android.material.snackbar.Snackbar;
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

    private static final int VIEW_TYPE_LIST = 0;
    private static final int VIEW_TYPE_GRID = 1;

    public interface OnPostClickListener {
        void onPostClick(Post post);
    }

    public interface OnPostUnsavedListener {
        void onPostUnsaved(String postId);
    }

    public interface OnPostSaveStateChangedListener {
        void onPostSaveStateChanged(Post post, boolean isSaved);
    }

    public interface OnPostDeleteListener {
        void onPostDelete(Post post);
    }

    public interface OnPostEditListener {
        void onPostEdit(Post post);
    }

    public interface OnPostLikeStateChangedListener {
        void onPostLikeStateChanged(Post post, boolean isLiked);
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
    private OnPostEditListener onPostEditListener;
    private OnPostSaveStateChangedListener onPostSaveStateChangedListener;
    private OnPostLikeStateChangedListener onPostLikeStateChangedListener;

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
                .inflate(viewType == VIEW_TYPE_GRID ? R.layout.item_post_grid : R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        return isGridMode ? VIEW_TYPE_GRID : VIEW_TYPE_LIST;
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.postTitle.setText(post.getTitle());
        holder.postUsername.setText(post.getUsername() == null || post.getUsername().trim().isEmpty()
                ? "by Chef"
                : "by " + post.getUsername());
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

        if (onPostDeleteListener != null || onPostEditListener != null) {
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setOnClickListener(v -> {
                if (onPostEditListener != null) {
                    onPostEditListener.onPostEdit(post);
                } else if (onPostDeleteListener != null) {
                    onPostDeleteListener.onPostDelete(post);
                }
            });
        } else {
            holder.deleteButton.setVisibility(View.GONE);
            holder.deleteButton.setOnClickListener(null);
        }

        if (isGridMode) {
            holder.postDescription.setVisibility(View.GONE);
            holder.likeButton.setVisibility(View.GONE);
            holder.saveButton.setVisibility(View.GONE);
            holder.likesCount.setVisibility(View.GONE);
            holder.postTitle.setMaxLines(2);
            holder.postTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        } else {
            holder.postDescription.setVisibility(View.VISIBLE);
            holder.likeButton.setVisibility(View.VISIBLE);
            holder.saveButton.setVisibility(View.VISIBLE);
            holder.likesCount.setVisibility(View.VISIBLE);
            holder.postTitle.setMaxLines(2);
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
                        if (onPostSaveStateChangedListener != null) {
                            onPostSaveStateChangedListener.onPostSaveStateChanged(post, false);
                        }
                        showUndoSnackbar(
                                holder.itemView,
                                R.string.post_unsaved_message,
                                () -> restoreSavedPost(post, holder, savedRef)
                        );
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
                    .addOnSuccessListener(unused -> {
                        if (onPostSaveStateChangedListener != null) {
                            onPostSaveStateChangedListener.onPostSaveStateChanged(post, true);
                        }
                    })
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
            animateLikeButton(holder.likeButton);
        }

        holder.likesCount.setText(String.valueOf(post.getLikesCount()));

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference postRef = db.collection("posts").document(postId);
        DocumentReference likeRef = postRef.collection("likes").document(currentUid);
        int updatedLikeCount = post.getLikesCount();

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
        }).addOnSuccessListener(unused -> {
            if (onPostLikeStateChangedListener != null) {
                onPostLikeStateChangedListener.onPostLikeStateChanged(post, !currentlyLiked);
            }
            if (currentlyLiked) {
                showUndoSnackbar(
                        holder.itemView,
                        R.string.post_unliked_message,
                        () -> restoreLikedPost(post, holder, postRef, likeRef, updatedLikeCount)
                );
            }
        }).addOnFailureListener(e -> {
            if (currentlyLiked) {
                likedPostIds.add(postId);
                holder.likeButton.setImageResource(R.drawable.heart_filled);
                post.setLikesCount(updatedLikeCount + 1);
            } else {
                likedPostIds.remove(postId);
                holder.likeButton.setImageResource(R.drawable.heart);
                post.setLikesCount(Math.max(0, updatedLikeCount - 1));
            }
            holder.likesCount.setText(String.valueOf(post.getLikesCount()));
            Toast.makeText(context, "Failed to update like", Toast.LENGTH_SHORT).show();
        });
    }

    private void restoreSavedPost(Post post, PostViewHolder holder, DocumentReference savedRef) {
        savedPostIds.add(post.getPostId());
        holder.saveButton.setImageResource(R.drawable.ic_save_filled);

        Map<String, Object> data = new HashMap<>();
        data.put("savedAt", FieldValue.serverTimestamp());

        savedRef.set(data)
                .addOnSuccessListener(unused -> {
                    if (onPostSaveStateChangedListener != null) {
                        onPostSaveStateChangedListener.onPostSaveStateChanged(post, true);
                    }
                })
                .addOnFailureListener(e -> {
                    savedPostIds.remove(post.getPostId());
                    holder.saveButton.setImageResource(R.drawable.ic_save_outline);
                    if (onPostSaveStateChangedListener != null) {
                        onPostSaveStateChangedListener.onPostSaveStateChanged(post, false);
                    }
                    Toast.makeText(context, "Failed to restore saved post", Toast.LENGTH_SHORT).show();
                });
    }

    private void restoreLikedPost(Post post, PostViewHolder holder, DocumentReference postRef,
                                  DocumentReference likeRef, int previousLikeCount) {
        likedPostIds.add(post.getPostId());
        post.setLikesCount(previousLikeCount + 1);
        holder.likeButton.setImageResource(R.drawable.heart_filled);
        holder.likesCount.setText(String.valueOf(post.getLikesCount()));
        animateLikeButton(holder.likeButton);

        FirebaseFirestore.getInstance().runTransaction(transaction -> {
            Map<String, Object> likeData = new HashMap<>();
            likeData.put("likedAt", FieldValue.serverTimestamp());
            transaction.set(likeRef, likeData);
            transaction.update(postRef, "likesCount", previousLikeCount + 1);
            return null;
        }).addOnSuccessListener(unused -> {
            if (onPostLikeStateChangedListener != null) {
                onPostLikeStateChangedListener.onPostLikeStateChanged(post, true);
            }
        }).addOnFailureListener(e -> {
            likedPostIds.remove(post.getPostId());
            post.setLikesCount(previousLikeCount);
            holder.likeButton.setImageResource(R.drawable.heart);
            holder.likesCount.setText(String.valueOf(post.getLikesCount()));
            if (onPostLikeStateChangedListener != null) {
                onPostLikeStateChangedListener.onPostLikeStateChanged(post, false);
            }
            Toast.makeText(context, "Failed to restore like", Toast.LENGTH_SHORT).show();
        });
    }

    private void showUndoSnackbar(View sourceView, int messageResId, Runnable undoAction) {
        View snackbarHost = sourceView.getRootView().findViewById(android.R.id.content);
        if (snackbarHost == null) {
            snackbarHost = sourceView;
        }

        Snackbar.make(snackbarHost, messageResId, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo_action, v -> undoAction.run())
                .show();
    }

    private void animateLikeButton(View button) {
        button.animate().cancel();
        button.setScaleX(0.88f);
        button.setScaleY(0.88f);
        button.animate()
                .scaleX(1.18f)
                .scaleY(1.18f)
                .setDuration(120)
                .withEndAction(() -> button.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .start())
                .start();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView postTitle, postUsername, postDescription, postCookTime, postBudget, likesCount;
        ImageView postImage;
        ImageButton saveButton, likeButton;
        ImageButton deleteButton;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            postTitle = itemView.findViewById(R.id.post_title);
            postUsername = itemView.findViewById(R.id.post_username);
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

    public void setOnPostEditListener(OnPostEditListener listener) {
        this.onPostEditListener = listener;
    }

    public void setOnPostSaveStateChangedListener(OnPostSaveStateChangedListener listener) {
        this.onPostSaveStateChangedListener = listener;
    }

    public void setOnPostLikeStateChangedListener(OnPostLikeStateChangedListener listener) {
        this.onPostLikeStateChangedListener = listener;
    }

    public void setGridMode(boolean isGridMode) {
        this.isGridMode = isGridMode;
        notifyDataSetChanged();
    }
}
