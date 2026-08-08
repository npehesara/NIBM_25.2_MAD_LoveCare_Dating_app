package com.example.lovecare;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class UserChatActivity extends AppCompatActivity {

    private View tabChatsLayout, tabNotificationsLayout;
    private TextView tvTabChats, tvTabNotifications;
    private View indicatorChats, indicatorNotifications;
    private View searchContainer;
    private EditText etSearchUser;
    private RecyclerView rvChats, rvNotifications;

    private ChatAdapter chatAdapter;
    private NotificationAdapter notificationAdapter;
    private List<ChatItem> chatList;
    private List<NotificationItem> notificationList;

    private FirebaseAuth mAuth;
    private DatabaseReference chatsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_chat);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(this, Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        chatsRef = FirebaseDatabase.getInstance().getReference("chats");

        View root = findViewById(R.id.chatRoot);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            if (bottomNav != null) {
                bottomNav.setPadding(0, 0, 0, systemBars.bottom);
            }
            return insets;
        });

        initViews();
        setupTabs();
        setupChatList(currentUser.getUid());
        setupNotificationList();
        setupSearch();
        setupBottomNavigation(bottomNav);
    }

    private void initViews() {
        tabChatsLayout = findViewById(R.id.tabChatsLayout);
        tabNotificationsLayout = findViewById(R.id.tabNotificationsLayout);
        tvTabChats = findViewById(R.id.tvTabChats);
        tvTabNotifications = findViewById(R.id.tvTabNotifications);
        indicatorChats = findViewById(R.id.indicatorChats);
        indicatorNotifications = findViewById(R.id.indicatorNotifications);
        searchContainer = findViewById(R.id.searchContainer);
        etSearchUser = findViewById(R.id.etSearchUser);
        rvChats = findViewById(R.id.rvChats);
        rvNotifications = findViewById(R.id.rvNotifications);
    }

    private void setupTabs() {
        tabChatsLayout.setOnClickListener(v -> selectTab(true));
        tabNotificationsLayout.setOnClickListener(v -> selectTab(false));
    }

    private void selectTab(boolean isChatsSelected) {
        if (isChatsSelected) {
            tvTabChats.setTextColor(0xFF1A1A1A);
            indicatorChats.setVisibility(View.VISIBLE);

            tvTabNotifications.setTextColor(0x661A1A1A);
            indicatorNotifications.setVisibility(View.INVISIBLE);

            searchContainer.setVisibility(View.VISIBLE);
            rvChats.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
        } else {
            tvTabNotifications.setTextColor(0xFF1A1A1A);
            indicatorNotifications.setVisibility(View.VISIBLE);

            tvTabChats.setTextColor(0x661A1A1A);
            indicatorChats.setVisibility(View.INVISIBLE);

            searchContainer.setVisibility(View.GONE);
            rvChats.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
        }
    }

    private void setupChatList(String myUid) {
        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatList);
        rvChats.setLayoutManager(new LinearLayoutManager(this));
        rvChats.setAdapter(chatAdapter);

        // Fetch real-time chat nodes from Firebase Realtime Database
        chatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<TempChatInfo> matchingChats = new ArrayList<>();

                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();
                    if (chatId == null || !chatId.contains("_")) continue;

                    String partnerUid = null;
                    if (chatId.startsWith(myUid + "_")) {
                        partnerUid = chatId.substring(myUid.length() + 1);
                    } else if (chatId.endsWith("_" + myUid)) {
                        partnerUid = chatId.substring(0, chatId.length() - myUid.length() - 1);
                    } else {
                        String[] parts = chatId.split("_");
                        if (parts.length == 2) {
                            if (parts[0].equalsIgnoreCase(myUid)) {
                                partnerUid = parts[1];
                            } else if (parts[1].equalsIgnoreCase(myUid)) {
                                partnerUid = parts[0];
                            }
                        }
                    }
                    if (partnerUid == null || partnerUid.isEmpty()) continue;

                    DataSnapshot messagesSnapshot = chatSnapshot.hasChild("messages") ?
                            chatSnapshot.child("messages") : chatSnapshot;

                    String lastMsg = "No messages yet";
                    String lastTime = "";
                    long maxTimestamp = -1;

                    for (DataSnapshot msgSnap : messagesSnapshot.getChildren()) {
                        if ("messages".equals(msgSnap.getKey())) continue;

                        Object tsObj = msgSnap.child("timestamp").getValue();
                        long ts = -1;
                        if (tsObj instanceof Number) {
                            ts = ((Number) tsObj).longValue();
                        } else if (tsObj instanceof String) {
                            try { ts = Long.parseLong((String) tsObj); } catch (Exception ignored) {}
                        }

                        String text = msgSnap.child("messageText").getValue(String.class);
                        if (text == null) text = msgSnap.child("text").getValue(String.class);
                        if (text == null) text = msgSnap.child("message").getValue(String.class);

                        String timeStr = msgSnap.child("time").getValue(String.class);

                        if (ts >= maxTimestamp || maxTimestamp == -1) {
                            if (ts > -1) maxTimestamp = ts;
                            if (text != null && !text.isEmpty()) lastMsg = text;
                            if (timeStr != null && !timeStr.isEmpty()) lastTime = timeStr;
                        }
                    }

                    matchingChats.add(new TempChatInfo(partnerUid, lastMsg, lastTime, maxTimestamp));
                }

                if (matchingChats.isEmpty()) {
                    chatList.clear();
                    chatAdapter.filter(etSearchUser != null && etSearchUser.getText() != null ? etSearchUser.getText().toString() : "");
                    return;
                }

                final List<ChatItemWithTs> tempList = java.util.Collections.synchronizedList(new ArrayList<>());
                final java.util.concurrent.atomic.AtomicInteger pending = new java.util.concurrent.atomic.AtomicInteger(matchingChats.size());

                for (TempChatInfo info : matchingChats) {
                    FirebaseFirestore.getInstance().collection("users").document(info.partnerUid).get()
                            .addOnSuccessListener(userDoc -> {
                                String name = null;
                                String photo = null;
                                if (userDoc != null && userDoc.exists()) {
                                    name = userDoc.getString("name");
                                    photo = userDoc.getString("photoUrl");
                                    if (photo == null || photo.isEmpty()) photo = userDoc.getString("photo");
                                }
                                if (name == null || name.isEmpty()) {
                                    name = "User (" + (info.partnerUid.length() > 4 ? info.partnerUid.substring(0, 4) : info.partnerUid) + ")";
                                }

                                tempList.add(new ChatItemWithTs(
                                        new ChatItem(info.partnerUid, name, info.lastMsg, info.lastTime != null ? info.lastTime : "", photo, 0, true),
                                        info.maxTimestamp
                                ));

                                if (pending.decrementAndGet() == 0) {
                                    updateAndSortChatList(tempList);
                                }
                            })
                            .addOnFailureListener(e -> {
                                String fallbackName = "User (" + (info.partnerUid.length() > 4 ? info.partnerUid.substring(0, 4) : info.partnerUid) + ")";
                                tempList.add(new ChatItemWithTs(
                                        new ChatItem(info.partnerUid, fallbackName, info.lastMsg, info.lastTime != null ? info.lastTime : "", null, 0, true),
                                        info.maxTimestamp
                                ));
                                if (pending.decrementAndGet() == 0) {
                                    updateAndSortChatList(tempList);
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserChatActivity.this, "Failed to load chats: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateAndSortChatList(List<ChatItemWithTs> tempList) {
        java.util.Collections.sort(tempList, (c1, c2) -> Long.compare(c2.timestamp, c1.timestamp));
        chatList.clear();
        for (ChatItemWithTs itemWithTs : tempList) {
            chatList.add(itemWithTs.chatItem);
        }
        chatAdapter.filter(etSearchUser.getText().toString());
    }

    private static class TempChatInfo {
        final String partnerUid;
        final String lastMsg;
        final String lastTime;
        final long maxTimestamp;

        TempChatInfo(String partnerUid, String lastMsg, String lastTime, long maxTimestamp) {
            this.partnerUid = partnerUid;
            this.lastMsg = lastMsg;
            this.lastTime = lastTime;
            this.maxTimestamp = maxTimestamp;
        }
    }

    private static class ChatItemWithTs {
        final ChatItem chatItem;
        final long timestamp;

        ChatItemWithTs(ChatItem chatItem, long timestamp) {
            this.chatItem = chatItem;
            this.timestamp = timestamp;
        }
    }

    private void setupNotificationList() {
        notificationList = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(this, notificationList);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(notificationAdapter);

        // Load real notifications from Firestore
        String myUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        if (myUid.isEmpty()) return;

        FirebaseFirestore.getInstance().collection("notifications")
                .whereEqualTo("toUid", myUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    notificationList.clear();

                    // Sort by timestamp descending
                    java.util.List<com.google.firebase.firestore.DocumentSnapshot> docs =
                            new java.util.ArrayList<>(querySnapshot.getDocuments());
                    docs.sort((a, b) -> {
                        Long tsA = a.getLong("timestamp");
                        Long tsB = b.getLong("timestamp");
                        if (tsA == null) tsA = 0L;
                        if (tsB == null) tsB = 0L;
                        return Long.compare(tsB, tsA);
                    });

                    for (com.google.firebase.firestore.DocumentSnapshot doc : docs) {
                        String fromName = doc.getString("fromName");
                        String message = doc.getString("message");
                        String type = doc.getString("type");
                        String fromUid = doc.getString("fromUid");
                        Long timestamp = doc.getLong("timestamp");

                        if (fromName == null) fromName = "System";
                        if (message == null) message = "New notification";

                        String timeAgo = getTimeAgo(timestamp != null ? timestamp : 0L);
                        int icon = R.drawable.ic_heart;
                        if ("mutual_like_blocked".equals(type)) {
                            icon = R.drawable.ic_bell;
                        }

                        notificationList.add(new NotificationItem(fromName, message, timeAgo, icon, type, fromUid));
                    }

                    if (notificationList.isEmpty()) {
                        notificationList.add(new NotificationItem("System", "No notifications yet. Start swiping! 💕", "now", R.drawable.ic_bell));
                    }

                    notificationAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    notificationList.clear();
                    notificationList.add(new NotificationItem("System", "Welcome to the messaging module!", "now", R.drawable.ic_bell));
                    notificationAdapter.notifyDataSetChanged();
                });
    }

    private String getTimeAgo(long timestamp) {
        if (timestamp == 0) return "now";
        long diff = System.currentTimeMillis() - timestamp;
        long minutes = diff / (1000 * 60);
        long hours = diff / (1000 * 60 * 60);
        long days = diff / (1000 * 60 * 60 * 24);

        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + "m ago";
        if (hours < 24) return hours + "h ago";
        if (days < 7) return days + "d ago";
        return days / 7 + "w ago";
    }

    private void setupSearch() {
        etSearchUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (chatAdapter != null) {
                    chatAdapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupBottomNavigation(BottomNavigationView bottomNav) {
        if (bottomNav == null) return;
        bottomNav.setSelectedItemId(R.id.nav_chat);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Intent intent = null;

            if (itemId == R.id.nav_chat) {
                return true;
            } else if (itemId == R.id.nav_swipe) {
                intent = new Intent(this, HomeActivity.class);
            } else if (itemId == R.id.nav_starred) {
                intent = new Intent(this, StarredActivity.class);
            } else if (itemId == R.id.nav_likes) {
                intent = new Intent(this, LoveSpaceActivity.class);
            } else if (itemId == R.id.nav_profile) {
                intent = new Intent(this, Profile.class);
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }
}
