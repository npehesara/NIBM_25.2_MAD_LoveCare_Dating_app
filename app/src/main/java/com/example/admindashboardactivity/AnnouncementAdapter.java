package com.example.admindashboardactivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lovecare.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {

    public interface OnAnnouncementActionListener {
        void onDeleteAnnouncement(AdminAnnouncement announcement, int position);
    }

    private final List<AdminAnnouncement> announcementList;
    private final OnAnnouncementActionListener listener;

    public AnnouncementAdapter(List<AdminAnnouncement> announcementList, OnAnnouncementActionListener listener) {
        this.announcementList = announcementList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_announcement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdminAnnouncement item = announcementList.get(position);
        holder.tvTitle.setText(item.getTitle() != null ? item.getTitle() : "Announcement");
        holder.tvMessage.setText(item.getMessage() != null ? item.getMessage() : "");

        if (item.getCreatedAt() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
            holder.tvTime.setText(sdf.format(new Date(item.getCreatedAt())));
        } else {
            holder.tvTime.setText("Recently");
        }

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteAnnouncement(item, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return announcementList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTime;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_announcement_title);
            tvMessage = itemView.findViewById(R.id.tv_announcement_message);
            tvTime = itemView.findViewById(R.id.tv_announcement_time);
            btnDelete = itemView.findViewById(R.id.btn_delete_announcement);
        }
    }
}
