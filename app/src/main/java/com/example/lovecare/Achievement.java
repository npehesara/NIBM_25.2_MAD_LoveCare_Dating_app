package com.example.lovecare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Achievement extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_achievement);
        
        View root = findViewById(R.id.achievementRoot);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0); // Bottom padding 0 for nav bar
                return insets;
            });
        }

        View btnBack = findViewById(R.id.btnAchievementBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_profile); // Highlight Profile
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_swipe) {
                    startActivity(new Intent(this, NavigationBar.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_likes) {
                    startActivity(new Intent(this, LoveSpaceActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_chat) {
                    startActivity(new Intent(this, LoveSpaceMessage.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    return true; // Already here
                }
                return false;
            });
        }
    }
}
