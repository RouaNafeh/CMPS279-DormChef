package com.cookio.app.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
                    .placeholder(R.drawable.logo)
                    .centerCrop()
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.logo);
        }

        // Type icon
        switch (n.getType() != null ? n.getType() : "") {
            case Notification.TYPE_LIKE:
                holder.ivTypeIcon.setImageResource(R.drawable.heart_filled);
                holder.ivTypeIcon.setColorFilter(context.getColor(R.color.red));
                holder.typeBadge.setBackgroundResource(R.drawable.bg_notification_type_badge_like);
                break;
            case Notification.TYPE_FOLLOW:
                holder.ivTypeIcon.setImageResource(R.drawable.account_circle);
                holder.ivTypeIcon.setColorFilter(context.getColor(R.color.primary));
                holder.typeBadge.setBackgroundResource(R.drawable.bg_notification_type_badge_follow);
                break;
            case Notification.TYPE_COMMENT:
            case Notification.TYPE_REVIEW:
                holder.ivTypeIcon.setImageResource(R.drawable.menu_book);
                holder.ivTypeIcon.setColorFilter(context.getColor(R.color.accent_pink));
                holder.typeBadge.setBackgroundResource(R.drawable.bg_notification_type_badge_comment);
                break;
            default:
                holder.ivTypeIcon.setImageResource(R.drawable.heart);
                holder.ivTypeIcon.setColorFilter(context.getColor(R.color.textGrey));
                holder.typeBadge.setBackgroundResource(R.drawable.bg_notification_type_badge);
        }

        holder.cardNotification.setCardBackgroundColor(context.getColor(R.color.card_white));
        holder.cardNotification.setStrokeColor(
                context.getColor(n.isRead() ? R.color.borderSoft : R.color.primary)
        );
        holder.cardNotification.setStrokeWidth(n.isRead() ? 1 : 2);
        holder.cardNotification.setAlpha(n.isRead() ? 0.92f : 1f);

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
        FrameLayout typeBadge;
        View unreadDot;
        ImageView ivAvatar, ivTypeIcon;
        TextView tvMessage, tvTime;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardNotification = itemView.findViewById(R.id.cardNotification);
            typeBadge        = itemView.findViewById(R.id.typeBadge);
            unreadDot        = itemView.findViewById(R.id.unreadDot);
            ivAvatar         = itemView.findViewById(R.id.ivAvatar);
            ivTypeIcon       = itemView.findViewById(R.id.ivTypeIcon);
            tvMessage        = itemView.findViewById(R.id.tvMessage);
            tvTime           = itemView.findViewById(R.id.tvTime);
        }
    }
}
