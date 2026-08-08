package com.example.lovecare;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final Context context;
    private final List<NotificationItem> notificationList;

    public NotificationAdapter(Context context, List<NotificationItem> notificationList) {
        this.context = context;
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem item = notificationList.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvText.setText(item.getMessage());
        holder.tvTime.setText(item.getTimeAgo());

        if (item.getIconResId() != 0) {
            holder.ivIcon.setImageResource(item.getIconResId());
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_bell);
        }

        holder.itemView.setOnClickListener(v -> {
            if ("mutual_like_blocked".equals(item.getType()) && item.getFromUid() != null && !item.getFromUid().isEmpty()) {
                handleBlockedNotificationClick(item);
            } else {
                Toast.makeText(context, "Notification from " + item.getTitle(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleBlockedNotificationClick(NotificationItem item) {
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String myUid = currentUser.getUid();
        String targetUid = item.getFromUid();
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        Toast.makeText(context, "Checking availability...", Toast.LENGTH_SHORT).show();

        // Check if a LoveSpace (active or past) already existed between these two users
        db.collection("lovespaces")
                .whereEqualTo("user1Uid", myUid)
                .whereEqualTo("user2Uid", targetUid)
                .get()
                .addOnSuccessListener(ls1 -> {
                    if (!ls1.isEmpty()) {
                        showBlockedDialog(item.getTitle(), "You have already ended a LoveSpace connection with " + item.getTitle() + ".");
                        return;
                    }
                    db.collection("lovespaces")
                            .whereEqualTo("user1Uid", targetUid)
                            .whereEqualTo("user2Uid", myUid)
                            .get()
                            .addOnSuccessListener(ls2 -> {
                                if (!ls2.isEmpty()) {
                                    showBlockedDialog(item.getTitle(), "You have already ended a LoveSpace connection with " + item.getTitle() + ".");
                                    return;
                                }

                                // Check if current user has an active LoveSpace with anyone else
                                db.collection("lovespaces")
                                        .whereEqualTo("user1Uid", myUid)
                                        .whereEqualTo("active", true)
                                        .get()
                                        .addOnSuccessListener(snap1 -> {
                                            if (!snap1.isEmpty()) {
                                                showBlockedDialog(item.getTitle(), "You currently have an active LoveSpace. Please close your current LoveSpace first to connect with " + item.getTitle() + ".");
                                                return;
                                            }
                                            db.collection("lovespaces")
                                                    .whereEqualTo("user2Uid", myUid)
                                                    .whereEqualTo("active", true)
                                                    .get()
                                                    .addOnSuccessListener(snap2 -> {
                                                        if (!snap2.isEmpty()) {
                                                            showBlockedDialog(item.getTitle(), "You currently have an active LoveSpace. Please close your current LoveSpace first to connect with " + item.getTitle() + ".");
                                                        } else {
                                                            // Current user is free, now check target user
                                                            checkTargetUserAndConnect(myUid, targetUid, item.getTitle());
                                                        }
                                                    });
                                        });
                            });
                });
    }

    private void checkTargetUserAndConnect(String myUid, String targetUid, String targetName) {
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        db.collection("lovespaces")
                .whereEqualTo("user1Uid", targetUid)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snap1 -> {
                    if (!snap1.isEmpty()) {
                        showBlockedDialog(targetName, targetName + " currently has an active LoveSpace with someone else. You can connect once they are free!");
                        return;
                    }
                    db.collection("lovespaces")
                            .whereEqualTo("user2Uid", targetUid)
                            .whereEqualTo("active", true)
                            .get()
                            .addOnSuccessListener(snap2 -> {
                                if (!snap2.isEmpty()) {
                                    showBlockedDialog(targetName, targetName + " currently has an active LoveSpace with someone else. You can connect once they are free!");
                                } else {
                                    // Both users are free! Create LoveSpace
                                    createLoveSpaceAndOpen(myUid, targetUid, targetName);
                                }
                            });
                });
    }

    private void createLoveSpaceAndOpen(String myUid, String targetUid, String targetName) {
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        java.util.Map<String, Object> loveSpace = new java.util.HashMap<>();
        loveSpace.put("user1Uid", myUid);
        loveSpace.put("user2Uid", targetUid);
        loveSpace.put("createdAt", System.currentTimeMillis());
        loveSpace.put("active", true);

        db.collection("lovespaces").add(loveSpace)
                .addOnSuccessListener(ref -> {
                    new androidx.appcompat.app.AlertDialog.Builder(context)
                            .setTitle("💕 LoveSpace Created!")
                            .setMessage("You and " + targetName + " are now connected in LoveSpace! 🎉")
                            .setPositiveButton("Open LoveSpace", (dialog, which) -> {
                                Intent intent = new Intent(context, LoveSpaceActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                context.startActivity(intent);
                            })
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Failed to create LoveSpace: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showBlockedDialog(String name, String message) {
        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("LoveSpace Unavailable")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvText, tvTime;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivNotificationIcon);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvText = itemView.findViewById(R.id.tvNotificationText);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
        }
    }
}
