package com.example.lovecare;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_chat);

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
        setupChatList();
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

    private void setupChatList() {
        chatList = new ArrayList<>();
        chatList.add(new ChatItem("testacc", "Tap to view message...", "2m ago", R.drawable.ic_launcher_foreground, true));
        chatList.add(new ChatItem("dartb44", "Tap to view message...", "15m ago", R.drawable.ic_menu_gallery_boy, true));
        chatList.add(new ChatItem("lljllooo", "Tap to view message...", "1h ago", R.drawable.ic_menu_gallery_girl, false));
        chatList.add(new ChatItem("gizemtheeeekend", "Tap to view message...", "2h ago", R.drawable.ic_launcher_foreground, true));
        chatList.add(new ChatItem("KAMIL", "Tap to view message...", "5h ago", R.drawable.ic_menu_gallery_boy, false));
        chatList.add(new ChatItem("cryingbaby", "Tap to view message...", "1d ago", R.drawable.ic_menu_gallery_girl, true));

        chatAdapter = new ChatAdapter(this, chatList);
        rvChats.setLayoutManager(new LinearLayoutManager(this));
        rvChats.setAdapter(chatAdapter);
    }

    private void setupNotificationList() {
        notificationList = new ArrayList<>();
        notificationList.add(new NotificationItem("testacc", "Replied to your message", "2m ago", R.drawable.ic_bell));
        notificationList.add(new NotificationItem("dartb44", "Sent you a new photo", "15m ago", R.drawable.ic_bell));
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
