package com.example.lovecare;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private static final int TYPE_TEXT_SENT     = 1;
    private static final int TYPE_TEXT_RECEIVED = 2;
    private static final int TYPE_IMAGE_SENT    = 3;
    private static final int TYPE_IMAGE_RECEIVED = 4;

    private final Context context;
    private final List<ChatMessage> messageList;
    private String partnerName;

    public ChatMessageAdapter(Context context, List<ChatMessage> messageList, String partnerName) {
        this.context = context;
        this.messageList = messageList;
        this.partnerName = partnerName != null ? partnerName : "?";
    }

    public void setPartnerName(String name) {
        this.partnerName = name != null ? name : "?";
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = messageList.get(position);
        if (msg.isImageMessage()) {
            return msg.isSentByMe() ? TYPE_IMAGE_SENT : TYPE_IMAGE_RECEIVED;
        } else {
            return msg.isSentByMe() ? TYPE_TEXT_SENT : TYPE_TEXT_RECEIVED;
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId;
        switch (viewType) {
            case TYPE_TEXT_SENT:     layoutId = R.layout.item_message_sent;           break;
            case TYPE_TEXT_RECEIVED: layoutId = R.layout.item_message_received;       break;
            case TYPE_IMAGE_SENT:    layoutId = R.layout.item_message_image_sent;     break;
            case TYPE_IMAGE_RECEIVED:layoutId = R.layout.item_message_image_received; break;
            default:                 layoutId = R.layout.item_message_sent;           break;
        }
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);

        // Partner letter (received types)
        if (holder.tvPartnerAvatarLetter != null) {
            String letter = partnerName.trim().length() > 0
                    ? String.valueOf(Character.toUpperCase(partnerName.trim().charAt(0))) : "?";
            holder.tvPartnerAvatarLetter.setText(letter);
        }

        int viewType = getItemViewType(position);

        if (viewType == TYPE_TEXT_SENT || viewType == TYPE_TEXT_RECEIVED) {
            // ── TEXT message ──────────────────────────────────────────────
            if (holder.tvMessageText != null) holder.tvMessageText.setText(message.getText());
            if (holder.tvMessageTime != null) holder.tvMessageTime.setText(message.getTime());

        } else {
            // ── IMAGE message ─────────────────────────────────────────────
            if (holder.tvMessageTime != null) holder.tvMessageTime.setText(message.getTime());

            // Set placeholder letter
            String letter = partnerName.trim().length() > 0
                    ? String.valueOf(Character.toUpperCase(partnerName.trim().charAt(0))) : "?";
            if (holder.tvImagePlaceholderLetter != null)
                holder.tvImagePlaceholderLetter.setText(letter);

            // Show placeholder overlay; ivMessageImage stays VISIBLE so Glide can load into it
            if (holder.flImagePlaceholder != null)
                holder.flImagePlaceholder.setVisibility(View.VISIBLE);

            String imageUrl = message.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty() && holder.ivMessageImage != null) {
                final FrameLayout placeholderView = holder.flImagePlaceholder;

                Glide.with(context)
                        .load(imageUrl)
                        .centerCrop()
                        .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(GlideException e, Object model,
                                    Target<android.graphics.drawable.Drawable> target,
                                    boolean isFirstResource) {
                                // Keep placeholder on failure
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                                    Object model,
                                    Target<android.graphics.drawable.Drawable> target,
                                    com.bumptech.glide.load.DataSource dataSource,
                                    boolean isFirstResource) {
                                // Hide letter placeholder — image is now drawn into ivMessageImage below it
                                if (placeholderView != null)
                                    placeholderView.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(holder.ivMessageImage);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageText, tvMessageTime, tvPartnerAvatarLetter, tvImagePlaceholderLetter;
        ImageView ivMessageImage;
        FrameLayout flImagePlaceholder;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText            = itemView.findViewById(R.id.tvMessageText);
            tvMessageTime            = itemView.findViewById(R.id.tvMessageTime);
            tvPartnerAvatarLetter    = itemView.findViewById(R.id.tvPartnerAvatarLetter);
            tvImagePlaceholderLetter = itemView.findViewById(R.id.tvImagePlaceholderLetter);
            ivMessageImage           = itemView.findViewById(R.id.ivMessageImage);
            flImagePlaceholder       = itemView.findViewById(R.id.flImagePlaceholder);
        }
    }
}
