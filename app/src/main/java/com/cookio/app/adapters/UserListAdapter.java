package com.cookio.app.adapters;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cookio.app.R;
import com.cookio.app.activities.PublicProfileActivity;
import com.cookio.app.models.User;
import com.cookio.app.utils.UserDisplayHelper;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;
public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.UserViewHolder> {
    private final Context context;
    private final List<User> users;

    public UserListAdapter(Context context, List<User> users) {
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_list, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        String displayName = resolveDisplayName(user);

        holder.tvName.setText(displayName);
        holder.tvEmail.setText(UserDisplayHelper.resolveHandle(user.getUsername()));
        holder.tvAvatarInitial.setText(resolveInitial(displayName));

        if (!TextUtils.isEmpty(user.getProfileImageUrl())) {
            holder.tvAvatarInitial.setVisibility(View.GONE);
            holder.ivAvatar.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.logo)
                    .error(R.drawable.logo)
                    .centerCrop()
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setVisibility(View.GONE);
            holder.tvAvatarInitial.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (TextUtils.isEmpty(user.getUid())) {
                return;
            }
            Intent intent = new Intent(context, PublicProfileActivity.class);
            intent.putExtra(PublicProfileActivity.EXTRA_USER_ID, user.getUid());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    private String resolveDisplayName(User user) {
        return UserDisplayHelper.resolveDisplayName(
                user.getName(),
                user.getUsername(),
                context.getString(R.string.profile_default_username)
        );
    }

    private String resolveInitial(String value) {
        return UserDisplayHelper.resolveInitial(
                value,
                context.getString(R.string.profile_default_username)
        );
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView ivAvatar;
        final TextView tvAvatarInitial;
        final TextView tvName;
        final TextView tvEmail;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvAvatarInitial = itemView.findViewById(R.id.tvUserAvatarInitial);
            tvName = itemView.findViewById(R.id.tvUserName);
            tvEmail = itemView.findViewById(R.id.tvUserEmail);
        }
    }
}
