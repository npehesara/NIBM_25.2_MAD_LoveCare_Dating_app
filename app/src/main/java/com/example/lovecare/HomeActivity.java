package com.example.lovecare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import android.view.GestureDetector;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HomeActivity extends AppCompatActivity {

    // ── Views ────────────────────────────────────────────────────────────────
    private ImageView ivProfileImage, ivHeartEffect;
    private TextView tvNameAge, tvActiveBadge;
    private TextView tvAgeChip, tvOrientationChip, tvHeightChip, tvWeightChip;
    private ProgressBar pbLoading;
    private TextView tvEmptyState;
    private LinearLayout llActionButtons, llProfileInfo;
    private android.widget.FrameLayout flLetterAvatar;
    private TextView tvAvatarLetter;

    private FloatingActionButton btnUndo, btnDislike, btnSuperLike, btnLike, btnMessage;

    // ── State ─────────────────────────────────────────────────────────────────
    private List<DocumentSnapshot> suggestedUsers = new ArrayList<>();
    private int currentIndex = 0;

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homeRoot), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, 0);
            // Let bottom nav handle its own bottom padding
            BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
            if (bottomNav != null) bottomNav.setPadding(0, 0, 0, sb.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Intent intent = new Intent(this, Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        db    = FirebaseFirestore.getInstance();

        // Bind views
        ivProfileImage  = findViewById(R.id.ivProfileImage);
        ivHeartEffect   = findViewById(R.id.ivHeartEffect);
        tvNameAge       = findViewById(R.id.tvNameAge);
        tvActiveBadge   = findViewById(R.id.tvActiveBadge);
        pbLoading       = findViewById(R.id.pbLoading);
        tvEmptyState    = findViewById(R.id.tvEmptyState);
        llActionButtons = findViewById(R.id.llActionButtons);
        llProfileInfo   = findViewById(R.id.llProfileInfo);
        flLetterAvatar  = findViewById(R.id.flLetterAvatar);
        tvAvatarLetter  = findViewById(R.id.tvAvatarLetter);

        tvAgeChip         = findViewById(R.id.tvAgeChip);
        tvOrientationChip = findViewById(R.id.tvOrientationChip);
        tvHeightChip      = findViewById(R.id.tvHeightChip);
        tvWeightChip      = findViewById(R.id.tvWeightChip);

        btnUndo      = findViewById(R.id.btnUndo);
        btnDislike   = findViewById(R.id.btnDislike);
        btnSuperLike = findViewById(R.id.btnSuperLike);
        btnLike      = findViewById(R.id.btnLike);
        btnMessage   = findViewById(R.id.btnMessage);

        // Wire up bottom navigation
        setupBottomNav();

        // Wire up swipe gestures
        setupSwipeGesture();

        // Hide profile UI while loading
        setProfileVisible(false);

        setupActionButtons();
        loadSuggestedUsers();
    }


    // ── Bottom Navigation ─────────────────────────────────────────────────────

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav == null) return;
        bottomNav.setSelectedItemId(R.id.nav_swipe);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_swipe) {
                return true; // already here
            } else if (id == R.id.nav_likes) {
                startActivity(new Intent(this, LoveSpaceActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            } else if (id == R.id.nav_chat) {
                startActivity(new Intent(this, UserChatActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, Profile.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            }
            return true;
        });
    }

    private void loadSuggestedUsers() {
        pbLoading.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);

        String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        if (currentUid.isEmpty()) {
            pbLoading.setVisibility(View.GONE);
            showEmptyState();
            return;
        }

        // 1. Fetch current logged-in user's profile details to calculate match scores
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(currentUserDoc -> {
                    // 2. Fetch all users from Firestore
                    db.collection("users")
                            .get()
                            .addOnSuccessListener(usersSnapshot -> {
                                List<UserWithScore> scoredUsers = new ArrayList<>();

                                for (DocumentSnapshot doc : usersSnapshot.getDocuments()) {
                                    String docId = doc.getId();

                                    // Filter checks:
                                    // - Exclude current user (logged-in user)
                                    if (docId.equalsIgnoreCase(currentUid)) continue;

                                    // - Exclude admin role
                                    String role = doc.getString("role");
                                    if ("admin".equalsIgnoreCase(role)) continue;

                                    // Calculate match score
                                    int score = calculateMatchScore(currentUserDoc, doc);
                                    scoredUsers.add(new UserWithScore(doc, score));
                                }

                                // Sort in descending order of match score
                                Collections.sort(scoredUsers, new Comparator<UserWithScore>() {
                                    @Override
                                    public int compare(UserWithScore u1, UserWithScore u2) {
                                        return Integer.compare(u2.score, u1.score);
                                    }
                                });

                                // Populate suggestedUsers list
                                suggestedUsers.clear();
                                for (UserWithScore u : scoredUsers) {
                                    suggestedUsers.add(u.doc);
                                }

                                pbLoading.setVisibility(View.GONE);

                                if (suggestedUsers.isEmpty()) {
                                    showEmptyState();
                                } else {
                                    currentIndex = 0;
                                    setProfileVisible(true);
                                    displayUser(suggestedUsers.get(0));
                                }
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("HomeActivity", "Failed to fetch users: " + e.getMessage(), e);
                                pbLoading.setVisibility(View.GONE);
                                showEmptyState();
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("HomeActivity", "Failed to fetch currentUserDoc: " + e.getMessage(), e);
                    pbLoading.setVisibility(View.GONE);
                    showEmptyState();
                });
    }


    // ── Questionnaire Match Scoring ──────────────────────────────────────────

    private int calculateMatchScore(DocumentSnapshot current, DocumentSnapshot other) {
        int score = 0;

        // Gender & Looking For Alignment (Crucial dating compatibility)
        String myGender = current.getString("gender");
        String myLookingFor = current.getString("lookingFor");
        String otherGender = other.getString("gender");
        String otherLookingFor = other.getString("lookingFor");

        if (myLookingFor != null && otherGender != null) {
            if (myLookingFor.equalsIgnoreCase("Both") || myLookingFor.equalsIgnoreCase(otherGender)) {
                score += 15;
            } else {
                score -= 30; // Mismatch on gender preference
            }
        }
        if (otherLookingFor != null && myGender != null) {
            if (otherLookingFor.equalsIgnoreCase("Both") || otherLookingFor.equalsIgnoreCase(myGender)) {
                score += 15;
            } else {
                score -= 30;
            }
        }

        // Goal Match (e.g. both want Relationship/Marriage vs. Chatting/Travel)
        String myGoal = current.getString("goal");
        String otherGoal = other.getString("goal");
        if (myGoal != null && myGoal.equalsIgnoreCase(otherGoal)) {
            score += 10;
        }

        // Religion Match
        String myReligion = current.getString("religion");
        String otherReligion = other.getString("religion");
        if (myReligion != null && myReligion.equalsIgnoreCase(otherReligion)) {
            score += 8;
        }

        // Sexual Orientation Match
        String myOrientation = current.getString("orientation");
        String otherOrientation = other.getString("orientation");
        if (myOrientation != null && myOrientation.equalsIgnoreCase(otherOrientation)) {
            score += 5;
        }

        // Most Important Life Aspect Match
        String myImp = current.getString("mostImportant");
        String otherImp = other.getString("mostImportant");
        if (myImp != null && myImp.equalsIgnoreCase(otherImp)) {
            score += 7;
        }

        // Lifestyle Match (Smoke)
        String mySmoke = current.getString("smoke");
        String otherSmoke = other.getString("smoke");
        if (mySmoke != null && mySmoke.equalsIgnoreCase(otherSmoke)) {
            score += 4;
        }

        // Lifestyle Match (Drink)
        String myDrink = current.getString("drink");
        String otherDrink = other.getString("drink");
        if (myDrink != null && myDrink.equalsIgnoreCase(otherDrink)) {
            score += 4;
        }

        // Education Match
        String myEdu = current.getString("education");
        String otherEdu = other.getString("education");
        if (myEdu != null && myEdu.equalsIgnoreCase(otherEdu)) {
            score += 3;
        }

        // Star Sign Match
        String mySign = current.getString("starSign");
        String otherSign = other.getString("starSign");
        if (mySign != null && mySign.equalsIgnoreCase(otherSign)) {
            score += 2;
        }

        // Age Compatibility (within 5 years is prioritized)
        String myAgeStr = current.getString("age");
        String otherAgeStr = other.getString("age");
        if (myAgeStr != null && otherAgeStr != null) {
            try {
                int myAge = Integer.parseInt(myAgeStr.trim());
                int otherAge = Integer.parseInt(otherAgeStr.trim());
                int ageDiff = Math.abs(myAge - otherAge);
                if (ageDiff <= 3) {
                    score += 12;
                } else if (ageDiff <= 5) {
                    score += 8;
                } else if (ageDiff <= 8) {
                    score += 4;
                }
            } catch (Exception e) {
                // ignore parsing exceptions
            }
        }

        return score;
    }

    // ── Display ───────────────────────────────────────────────────────────────

    private void displayUser(DocumentSnapshot user) {
        // Name
        String name = user.getString("name");
        if (name == null || name.isEmpty()) name = "Anonymous";
        tvNameAge.setText("Name : " + name);

        // Populate dynamic specs tags with visibility toggle
        String age         = user.getString("age");
        String orientation = user.getString("orientation");
        String height      = user.getString("height");
        String weight      = user.getString("weight");

        if (age != null && !age.isEmpty()) {
            tvAgeChip.setText("📅 Age : " + age);
            tvAgeChip.setVisibility(View.VISIBLE);
        } else {
            tvAgeChip.setVisibility(View.GONE);
        }

        if (orientation != null && !orientation.isEmpty()) {
            tvOrientationChip.setText("❤️ " + orientation);
            tvOrientationChip.setVisibility(View.VISIBLE);
        } else {
            tvOrientationChip.setVisibility(View.GONE);
        }

        if (height != null && !height.isEmpty()) {
            tvHeightChip.setText("📏 Height : " + height);
            tvHeightChip.setVisibility(View.VISIBLE);
        } else {
            tvHeightChip.setVisibility(View.GONE);
        }

        if (weight != null && !weight.isEmpty()) {
            tvWeightChip.setText("⚖️ Weight : " + weight);
            tvWeightChip.setVisibility(View.VISIBLE);
        } else {
            tvWeightChip.setVisibility(View.GONE);
        }



        // Show active badge
        tvActiveBadge.setVisibility(View.VISIBLE);

        // Profile photo / Letter avatar
        String photoUrl = user.getString("photoUrl");
        if (photoUrl == null || photoUrl.isEmpty()) photoUrl = user.getString("photo");

        if (photoUrl != null && !photoUrl.isEmpty() && photoUrl.startsWith("http")) {
            flLetterAvatar.setVisibility(View.GONE);
            ivProfileImage.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(photoUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_menu_gallery_girl)
                    .error(R.drawable.ic_menu_gallery_boy)
                    .into(ivProfileImage);
        } else {
            // fallback text-based letter avatar
            ivProfileImage.setVisibility(View.GONE);
            flLetterAvatar.setVisibility(View.VISIBLE);
            char firstLetter = name.trim().length() > 0 ? Character.toUpperCase(name.trim().charAt(0)) : '?';
            tvAvatarLetter.setText(String.valueOf(firstLetter));
        }
    }


    private void advanceToNextUser() {
        if (suggestedUsers.isEmpty()) {
            showEmptyState();
            return;
        }
        currentIndex++;
        if (currentIndex >= suggestedUsers.size()) {
            showEmptyState();
        } else {
            displayUser(suggestedUsers.get(currentIndex));
        }
    }

    private void showEmptyState() {
        setProfileVisible(false);
        tvEmptyState.setVisibility(View.VISIBLE);
        llActionButtons.setVisibility(View.GONE);
    }

    private void setProfileVisible(boolean visible) {
        int vis = visible ? View.VISIBLE : View.GONE;
        llProfileInfo.setVisibility(vis);
        llActionButtons.setVisibility(vis);
        tvEmptyState.setVisibility(View.GONE);
        if (!visible) {
            if (ivProfileImage != null) ivProfileImage.setVisibility(View.GONE);
            if (flLetterAvatar != null) flLetterAvatar.setVisibility(View.GONE);
        }
    }

    // ── Action buttons ────────────────────────────────────────────────────────

    private void setupActionButtons() {

        // ← Back/Undo: go to previous user
        btnUndo.setOnClickListener(v -> {
            applyClickEffect(v);
            if (!suggestedUsers.isEmpty() && currentIndex > 0) {
                currentIndex--;
                setProfileVisible(true); // Restore profile layout and hide empty state
                displayUser(suggestedUsers.get(currentIndex));
            } else {
                Toast.makeText(this, "No previous profiles", Toast.LENGTH_SHORT).show();
            }
        });

        // 🔥 Dislike / Pass
        btnDislike.setOnClickListener(v -> {
            applyClickEffect(v);
            recordSwipe("dislike");
            advanceToNextUser();
            Toast.makeText(this, "Passed 👋", Toast.LENGTH_SHORT).show();
        });

        // ⭐ Super Like
        btnSuperLike.setOnClickListener(v -> {
            applyClickEffect(v);
            recordSwipe("superlike");
            advanceToNextUser();
            Toast.makeText(this, "Super Liked ⭐", Toast.LENGTH_SHORT).show();
        });

        // ♥ Like
        btnLike.setOnClickListener(v -> {
            applyClickEffect(v);
            showHeartPopEffect();
            recordSwipe("like");
            advanceToNextUser();
            Toast.makeText(this, "Liked! 💚", Toast.LENGTH_SHORT).show();
        });

        // ✉ Message — open chat directly with this user
        btnMessage.setOnClickListener(v -> {
            applyClickEffect(v);
            if (!suggestedUsers.isEmpty() && currentIndex < suggestedUsers.size()) {
                DocumentSnapshot targetUser = suggestedUsers.get(currentIndex);
                Intent intent = new Intent(this, UserChatActivity.class);
                intent.putExtra("userId",    targetUser.getId());
                intent.putExtra("userName",  targetUser.getString("name"));
                intent.putExtra("userPhoto", targetUser.getString("photoUrl"));
                startActivity(intent);
            }
        });
    }

    // ── Swipe recording ───────────────────────────────────────────────────────

    private void recordSwipe(String action) {
        if (suggestedUsers.isEmpty() || currentIndex >= suggestedUsers.size()) return;
        String myUid     = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        String targetUid = suggestedUsers.get(currentIndex).getId();
        if (myUid.isEmpty()) return;

        Map<String, Object> data = new HashMap<>();
        data.put("action",    action);
        data.put("fromUid",   myUid);
        data.put("toUid",     targetUid);
        data.put("timestamp", System.currentTimeMillis());
        db.collection("swipes").add(data);
    }

    // ── Animations ────────────────────────────────────────────────────────────

    private void applyClickEffect(View view) {
        ScaleAnimation anim = new ScaleAnimation(
                1.0f, 1.2f, 1.0f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        anim.setDuration(150);
        anim.setRepeatCount(1);
        anim.setRepeatMode(Animation.REVERSE);
        view.startAnimation(anim);
    }

    private void showHeartPopEffect() {
        if (ivHeartEffect == null) return;
        ivHeartEffect.setVisibility(View.VISIBLE);
        ivHeartEffect.setAlpha(0f);
        ivHeartEffect.setScaleX(0.5f);
        ivHeartEffect.setScaleY(0.5f);
        ivHeartEffect.animate()
                .alpha(1f).scaleX(1.2f).scaleY(1.2f).setDuration(300)
                .withEndAction(() -> ivHeartEffect.animate()
                        .alpha(0f).scaleX(1.5f).scaleY(1.5f).setDuration(300)
                        .withEndAction(() -> ivHeartEffect.setVisibility(View.GONE))
                        .start())
                .start();
    }

    // ── Swipe Gestures ────────────────────────────────────────────────────────

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private void setupSwipeGesture() {
        final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // Swipe Right -> Show Previous User
                            onSwipeRight();
                        } else {
                            // Swipe Left -> Show Next User
                            onSwipeLeft();
                        }
                        return true;
                    }
                }
                return false;
            }
        });

        View root = findViewById(R.id.homeRoot);
        if (root != null) {
            root.setOnTouchListener((v, event) -> {
                gestureDetector.onTouchEvent(event);
                return true;
            });
        }
    }

    private void onSwipeRight() {
        // Go back to previous user (navigation only — no likes registered)
        if (!suggestedUsers.isEmpty() && currentIndex > 0) {
            currentIndex--;
            setProfileVisible(true); // Restore profile layout and hide empty state
            displayUser(suggestedUsers.get(currentIndex));
        } else {
            Toast.makeText(this, "No previous profiles", Toast.LENGTH_SHORT).show();
        }
    }

    private void onSwipeLeft() {
        // Advance to next user (navigation only — no dislikes registered)
        if (!suggestedUsers.isEmpty()) {
            currentIndex++;
            if (currentIndex >= suggestedUsers.size()) {
                showEmptyState();
            } else {
                displayUser(suggestedUsers.get(currentIndex));
            }
        }
    }


    // ── Helper Class for scoring ──────────────────────────────────────────────

    private static class UserWithScore {
        final DocumentSnapshot doc;
        final int score;

        UserWithScore(DocumentSnapshot doc, int score) {
            this.doc   = doc;
            this.score = score;
        }
    }
}

