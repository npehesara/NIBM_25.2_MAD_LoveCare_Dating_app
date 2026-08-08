package com.example.admindashboardactivity;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.lovecare.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying user cards in the admin User Management screen.
 * Supports edit and delete actions via a listener interface.
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final List<AdminUser> userList;
    private final OnUserActionListener listener;

    public interface OnUserActionListener {
        void onEditUser(AdminUser user, int position);
        void onDeleteUser(AdminUser user, int position);
    }

    public UserAdapter(List<AdminUser> userList, OnUserActionListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        AdminUser user = userList.get(position);
        holder.bind(user, position);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    /**
     * Replaces the entire data set and refreshes the list.
     */
    public void updateList(List<AdminUser> newList) {
        userList.clear();
        userList.addAll(newList);
        notifyDataSetChanged();
    }

    // ── ViewHolder ───────────────────────────────────────────────────────────

    class UserViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivPhoto;
        private final TextView tvName, tvEmail, tvRole, tvDetails;
        private final ImageButton btnEdit, btnDelete;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto   = itemView.findViewById(R.id.iv_user_photo);
            tvName    = itemView.findViewById(R.id.tv_user_name);
            tvEmail   = itemView.findViewById(R.id.tv_user_email);
            tvRole    = itemView.findViewById(R.id.tv_user_role);
            tvDetails = itemView.findViewById(R.id.tv_user_details);
            btnEdit   = itemView.findViewById(R.id.btn_edit_user);
            btnDelete = itemView.findViewById(R.id.btn_delete_user);
        }

        void bind(AdminUser user, int position) {
            // Name
            tvName.setText(user.getName() != null ? user.getName() : "Unknown");

            // Email
            tvEmail.setText(user.getEmail() != null ? user.getEmail() : "—");

            // Role badge
            String role = user.getRole() != null ? user.getRole() : "user";
            tvRole.setText(role.toUpperCase());

            // Badge colour: admin = pink, user = grey-blue
            GradientDrawable badgeBg = (GradientDrawable) tvRole.getBackground();
            if (badgeBg != null) {
                if ("admin".equalsIgnoreCase(role)) {
                    badgeBg.setColor(0xFFFF4081); // primary_pink
                } else {
                    badgeBg.setColor(0xFF6C63FF); // purple accent
                }
            }

            // Extra details (age / gender)
            String details = buildDetailsString(user);
            if (!details.isEmpty()) {
                tvDetails.setText(details);
                tvDetails.setVisibility(View.VISIBLE);
            } else {
                tvDetails.setVisibility(View.GONE);
            }

            // Profile photo
            String photoUrl = user.getPhotoUrl();
            if (photoUrl != null && !photoUrl.isEmpty()) {
                ivPhoto.setBackground(null);
                ivPhoto.setImageTintList(null);
                Glide.with(itemView.getContext())
                        .load(photoUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile)
                        .into(ivPhoto);
            } else {
                // Reset to default tinted icon
                ivPhoto.setImageResource(R.drawable.ic_profile);
            }

            // Click listeners
            btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditUser(user, position);
            });
            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteUser(user, position);
            });
        }

        private String buildDetailsString(AdminUser user) {
            List<String> parts = new ArrayList<>();
            if (user.getAge() != null && !user.getAge().isEmpty()) {
                parts.add(user.getAge() + " yrs");
            }
            if (user.getGender() != null && !user.getGender().isEmpty()) {
                parts.add(user.getGender());
            }
            return String.join(" • ", parts);
        }
    }
}
