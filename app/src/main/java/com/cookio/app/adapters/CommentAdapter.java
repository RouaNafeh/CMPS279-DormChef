package com.cookio.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cookio.app.R;
import com.cookio.app.models.Comment;

import java.util.List;
import java.util.Set;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    public interface OnCommentLikeClickListener {
        void onCommentLike(Comment comment);
    }

    public interface OnCommentDeleteClickListener {
        void onCommentDelete(Comment comment);
    }

    private final List<Comment> list;
    private final Set<String> likedCommentIds;
    private final String currentUid;
    private final OnCommentLikeClickListener likeClickListener;
    private final OnCommentDeleteClickListener deleteClickListener;

    public CommentAdapter(
            List<Comment> list,
            Set<String> likedCommentIds,
            String currentUid,
            OnCommentLikeClickListener likeClickListener,
            OnCommentDeleteClickListener deleteClickListener
    ) {
        this.list = list;
        this.likedCommentIds = likedCommentIds;
        this.currentUid = currentUid;
        this.likeClickListener = likeClickListener;
        this.deleteClickListener = deleteClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment c = list.get(position);

        holder.tvUsername.setText(c.getUsername());
        holder.tvComment.setText(c.getText());
        holder.ratingBar.setRating(c.getRating());
        holder.tvLikeCount.setText(String.valueOf(c.getLikesCount()));

        boolean likedByMe = likedCommentIds.contains(c.getId());
        holder.btnLikeComment.setText(likedByMe ? "❤️ Liked" : "♡ Like");

        holder.btnLikeComment.setOnClickListener(v -> {
            if (likeClickListener != null) {
                likeClickListener.onCommentLike(c);
            }
        });

        boolean isMyComment = currentUid != null && currentUid.equals(c.getUserId());

        holder.btnDeleteComment.setVisibility(isMyComment ? View.VISIBLE : View.GONE);
        holder.btnDeleteComment.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onCommentDelete(c);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvComment, tvLikeCount, btnLikeComment, btnDeleteComment;
        RatingBar ratingBar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvCommentUsername);
            tvComment = itemView.findViewById(R.id.tvCommentText);
            tvLikeCount = itemView.findViewById(R.id.tvCommentLikeCount);
            btnLikeComment = itemView.findViewById(R.id.btnLikeComment);
            btnDeleteComment = itemView.findViewById(R.id.btnDeleteComment);
            ratingBar = itemView.findViewById(R.id.ratingBarItem);
        }
    }
}