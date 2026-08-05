package com.example.lovecare;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final Context context;
    private final List<ChatItem> chatList;
    private List<ChatItem> filteredList;

    public ChatAdapter(Context context, List<ChatItem> chatList) {
        this.context = context;
        this.chatList = chatList;
        this.filteredList = new ArrayList<>(chatList);
    }

    public void filter(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(chatList);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (ChatItem item : chatList) {
                if (item.getUsername().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatItem item = filteredList.get(position);
        holder.tvUsername.setText(item.getUsername());
        holder.tvLastMessage.setText(item.getLastMessage());
        holder.tvTime.setText(item.getTime());
        
        if (item.getAvatarResId() != 0) {
            holder.ivAvatar.setImageResource(item.getAvatarResId());
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_launcher_foreground);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, LoveSpaceMessage.class);
            intent.putExtra("username", item.getUsername());
            intent.putExtra("avatarResId", item.getAvatarResId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvUsername, tvLastMessage, tvTime;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}
