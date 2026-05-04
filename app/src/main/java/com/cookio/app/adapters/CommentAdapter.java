package com.cookio.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
        Comment comment = list.get(position);

        holder.tvUsername.setText(comment.getDisplayName());
        holder.tvComment.setText(comment.getText());
        holder.ratingBar.setRating(comment.getRating());
        holder.tvLikeCount.setText(String.valueOf(comment.getLikesCount()));

        boolean likedByMe = likedCommentIds.contains(comment.getId());
        int accentColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_pink);
        int mutedColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.textGrey);

        holder.btnLikeComment.setImageResource(likedByMe ? R.drawable.heart_filled : R.drawable.heart);
        holder.btnLikeComment.setColorFilter(likedByMe ? accentColor : mutedColor);
        holder.tvLikeLabel.setText(likedByMe ? "Liked" : "Like");
        holder.tvLikeLabel.setTextColor(likedByMe ? accentColor : mutedColor);

        View.OnClickListener onLikeClick = v -> {
            if (likeClickListener != null) {
                likeClickListener.onCommentLike(comment);
            }
        };
        holder.btnLikeComment.setOnClickListener(onLikeClick);
        holder.likeChip.setOnClickListener(onLikeClick);

        boolean isMyComment = currentUid != null && currentUid.equals(comment.getUserId());
        holder.btnDeleteComment.setVisibility(isMyComment ? View.VISIBLE : View.GONE);
        holder.btnDeleteComment.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onCommentDelete(comment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername;
        TextView tvComment;
        TextView tvLikeCount;
        TextView tvLikeLabel;
        TextView btnDeleteComment;
        ImageButton btnLikeComment;
        View likeChip;
        RatingBar ratingBar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvCommentUsername);
            tvComment = itemView.findViewById(R.id.tvCommentText);
            tvLikeCount = itemView.findViewById(R.id.tvCommentLikeCount);
            tvLikeLabel = itemView.findViewById(R.id.tvLikeLabel);
            btnDeleteComment = itemView.findViewById(R.id.btnDeleteComment);
            btnLikeComment = itemView.findViewById(R.id.btnLikeComment);
            likeChip = itemView.findViewById(R.id.likeChip);
            ratingBar = itemView.findViewById(R.id.ratingBarItem);
        }
    }
}
