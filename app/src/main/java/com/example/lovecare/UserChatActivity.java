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
                final List<ChatItem> tempList = new ArrayList<>();
                final int[] pendingFetches = {0};

                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();
                    if (chatId != null && chatId.contains(myUid)) {
                        String[] parts = chatId.split("_");
                        if (parts.length != 2) continue;
                        String partnerUid = parts[0].equals(myUid) ? parts[1] : parts[0];

                        // Find the last message
                        DataSnapshot messagesSnapshot = chatSnapshot.child("messages");
                        String lastMsg = "No messages yet";
                        String lastTime = "";
                        long maxTimestamp = -1;

                        for (DataSnapshot msgSnap : messagesSnapshot.getChildren()) {
                            Long ts = msgSnap.child("timestamp").getValue(Long.class);
                            if (ts != null && ts > maxTimestamp) {
                                maxTimestamp = ts;
                                lastMsg = msgSnap.child("messageText").getValue(String.class);
                                lastTime = msgSnap.child("time").getValue(String.class);
                            }
                        }

                        final String finalMsg = lastMsg;
                        final String finalTime = lastTime;
                        pendingFetches[0]++;

                        // Fetch details of conversation partner from Firestore
                        FirebaseFirestore.getInstance().collection("users").document(partnerUid).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        String name = userDoc.getString("name");
                                        String photo = userDoc.getString("photoUrl");
                                        if (photo == null || photo.isEmpty()) photo = userDoc.getString("photo");

                                        tempList.add(new ChatItem(partnerUid, name != null ? name : "User", finalMsg, finalTime != null ? finalTime : "", photo, 0, true));
                                    }
                                    pendingFetches[0]--;
                                    if (pendingFetches[0] == 0) {
                                        chatList.clear();
                                        chatList.addAll(tempList);
                                        chatAdapter.filter(etSearchUser.getText().toString());
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    pendingFetches[0]--;
                                    if (pendingFetches[0] == 0) {
                                        chatList.clear();
                                        chatList.addAll(tempList);
                                        chatAdapter.filter(etSearchUser.getText().toString());
                                    }
                                });
                    }
                }

                if (pendingFetches[0] == 0) {
                    chatList.clear();
                    chatAdapter.filter(etSearchUser.getText().toString());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserChatActivity.this, "Failed to load chats: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNotificationList() {
        notificationList = new ArrayList<>();
        notificationList.add(new NotificationItem("System", "Welcome to the messaging module!", "1h ago", R.drawable.ic_bell));

        notificationAdapter = new NotificationAdapter(this, notificationList);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(notificationAdapter);
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
                intent = new Intent(this, NavigationBar.class);
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
