package com.example.lovecare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class NavigationBar extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_navigation_bar);

        View root = findViewById(R.id.navRoot);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);

        // Handle Window Insets for Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            
            if (bottomNav != null) {
                bottomNav.setPadding(0, 0, 0, systemBars.bottom);
            }
            return insets;
        });

        // Like Button Logic
        FloatingActionButton btnLike = findViewById(R.id.btnLike);
        if (btnLike != null) {
            btnLike.setOnClickListener(v -> {
                applyClickEffect(v);
                showHeartPopEffect();
                Toast.makeText(this, "Liked!", Toast.LENGTH_SHORT).show();
            });
        }

        // Setup other swipe action buttons
        setupActionButtons();

        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                Intent intent = null;

                if (itemId == R.id.nav_swipe) {
                    return true;
                } else if (itemId == R.id.nav_likes) {
                    intent = new Intent(this, LoveSpaceActivity.class);
                } else if (itemId == R.id.nav_chat) {
                    intent = new Intent(this, UserChatActivity.class);
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

            bottomNav.setSelectedItemId(R.id.nav_swipe);
        }
    }

    private void setupActionButtons() {
        // Find views by their IDs as defined in activity_navigation_bar.xml
        View btnUndo = findViewById(R.id.btnUndo);
        View btnDislike = findViewById(R.id.btnDislike);
        View btnSuperLike = findViewById(R.id.btnSuperLike);
        View btnBoost = findViewById(R.id.btnBoost);

        if (btnUndo != null) btnUndo.setOnClickListener(this::applyClickEffect);
        if (btnDislike != null) btnDislike.setOnClickListener(this::applyClickEffect);
        if (btnSuperLike != null) btnSuperLike.setOnClickListener(this::applyClickEffect);
        if (btnBoost != null) btnBoost.setOnClickListener(this::applyClickEffect);
    }

    private void applyClickEffect(View view) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                1.0f, 1.2f, 1.0f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(150);
        scaleAnimation.setRepeatCount(1);
        scaleAnimation.setRepeatMode(Animation.REVERSE);
        view.startAnimation(scaleAnimation);
    }

    private void showHeartPopEffect() {
        final ImageView heart = findViewById(R.id.ivHeartEffect);
        if (heart == null) return;

        heart.setVisibility(View.VISIBLE);
        heart.setAlpha(0f);
        heart.setScaleX(0.5f);
        heart.setScaleY(0.5f);

        heart.animate()
                .alpha(1f)
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(300)
                .withEndAction(() -> {
                    heart.animate()
                            .alpha(0f)
                            .scaleX(1.5f)
                            .scaleY(1.5f)
                            .setDuration(300)
                            .withEndAction(() -> heart.setVisibility(View.GONE))
                            .start();
                })
                .start();
    }
}
