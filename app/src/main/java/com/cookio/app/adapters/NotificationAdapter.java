package com.cookio.app.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cookio.app.R;
import com.cookio.app.models.Notification;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class NotificationAdapter extends
        RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    private final Context context;
    private List<Notification> notifications;
    private final OnNotificationClickListener listener;

    public NotificationAdapter(Context context, List<Notification> notifications,
                               OnNotificationClickListener listener) {
        this.context       = context;
        this.notifications = notifications;
        this.listener      = listener;
    }

    public void updateData(List<Notification> newList) {
        this.notifications = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification n = notifications.get(position);

        holder.tvMessage.setText(n.getMessage());

        // Relative time e.g. "3 minutes ago"
        if (n.getCreatedAt() != null) {
            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    n.getCreatedAt().getTime(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
            );
            holder.tvTime.setText(relativeTime);
        } else {
            holder.tvTime.setText("Just now");
        }

        // Unread dot
        holder.unreadDot.setVisibility(n.isRead() ? View.INVISIBLE : View.VISIBLE);

        // Avatar
        if (n.getFromPhotoUrl() != null && !n.getFromPhotoUrl().isEmpty()) {
            Glide.with(context)
                    .load(n.getFromPhotoUrl())
                    .placeholder(R.drawable.logo_cropped)
                    .centerCrop()
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.logo_cropped);
        }

        // Type icon
        switch (n.getType() != null ? n.getType() : "") {
            case Notification.TYPE_LIKE:
                holder.ivTypeIcon.setImageResource(R.drawable.heart_filled);
                break;
            case Notification.TYPE_FOLLOW:
                holder.ivTypeIcon.setImageResource(R.drawable.ic_save_filled);
                break;
            case Notification.TYPE_COMMENT:
            case Notification.TYPE_REVIEW:
                holder.ivTypeIcon.setImageResource(R.drawable.logo_cropped);
                break;
            default:
                holder.ivTypeIcon.setImageResource(R.drawable.heart);
        }

        // Unread background tint
        holder.cardNotification.setCardBackgroundColor(
                context.getColor(n.isRead() ? R.color.card_white : R.color.secondary)
        );

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNotificationClick(n);
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardNotification;
        View unreadDot;
        ImageView ivAvatar, ivTypeIcon;
        TextView tvMessage, tvTime;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardNotification = itemView.findViewById(R.id.cardNotification);
            unreadDot        = itemView.findViewById(R.id.unreadDot);
            ivAvatar         = itemView.findViewById(R.id.ivAvatar);
            ivTypeIcon       = itemView.findViewById(R.id.ivTypeIcon);
            tvMessage        = itemView.findViewById(R.id.tvMessage);
            tvTime           = itemView.findViewById(R.id.tvTime);
        }
    }
}