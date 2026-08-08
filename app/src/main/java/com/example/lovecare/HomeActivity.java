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
import androidx.appcompat.app.AlertDialog;
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

    private FloatingActionButton btnUndo, btnSuperLike, btnLike, btnMessage;

    // ── State ─────────────────────────────────────────────────────────────────
    private List<DocumentSnapshot> suggestedUsers = new ArrayList<>();
    private int currentIndex = 0;

    // ── Like priority: UIDs of users who have liked the current user ──────────
    private Set<String> usersWhoLikedMe = new HashSet<>();

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
        db = FirebaseFirestore.getInstance();

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
        btnSuperLike = findViewById(R.id.btnSuperLike);
        btnLike      = findViewById(R.id.btnLike);
        btnMessage   = findViewById(R.id.btnMessage);

        // Wire up bottom navigation
        setupBottomNav();

        // Wire up swipe gestures
        setupSwipeGesture();

        // Hide profile UI while loading
        setProfileVisible(false);

        View btnRefreshSwipe = findViewById(R.id.btnRefreshSwipe);
        if (btnRefreshSwipe != null) {
            btnRefreshSwipe.setOnClickListener(v -> {
                v.animate().rotationBy(360f).setDuration(400).start();
                Toast.makeText(this, "Refreshing feed...", Toast.LENGTH_SHORT).show();
                loadSuggestedUsers();
            });
        }

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
            } else if (id == R.id.nav_starred) {
                startActivity(new Intent(this, StarredActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
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

        // 1. Fetch users who have liked/superliked the current user (for priority)
        db.collection("swipes")
                .whereEqualTo("toUid", currentUid)
                .get()
                .addOnSuccessListener(incomingLikesSnapshot -> {
                    usersWhoLikedMe.clear();
                    for (DocumentSnapshot likeDoc : incomingLikesSnapshot.getDocuments()) {
                        String action = likeDoc.getString("action");
                        if ("like".equals(action) || "superlike".equals(action)) {
                            String fromUid = likeDoc.getString("fromUid");
                            if (fromUid != null) {
                                usersWhoLikedMe.add(fromUid);
                            }
                        }
                    }

                    // 2. Fetch already swiped user UIDs to filter suggestions
                    db.collection("swipes")
                            .whereEqualTo("fromUid", currentUid)
                            .get()
                            .addOnSuccessListener(swipesSnapshot -> {
                                HashSet<String> swipedUids = new HashSet<>();
                                for (DocumentSnapshot swipeDoc : swipesSnapshot.getDocuments()) {
                                    String toUid = swipeDoc.getString("toUid");
                                    if (toUid != null) {
                                        swipedUids.add(toUid);
                                    }
                                }

                                // 3. Fetch current logged-in user's profile details to calculate match scores
                                db.collection("users").document(currentUid).get()
                                        .addOnSuccessListener(currentUserDoc -> {
                                            // 4. Fetch all users from Firestore
                                            db.collection("users")
                                                    .get()
                                                    .addOnSuccessListener(usersSnapshot -> {
                                                        List<UserWithScore> scoredUsers = new ArrayList<>();

                                                        for (DocumentSnapshot doc : usersSnapshot.getDocuments()) {
                                                            String docId = doc.getId();

                                                            // Exclude already swiped profiles
                                                            if (swipedUids.contains(docId)) continue;

                                                            // Exclude current user (logged-in user)
                                                            if (docId.equalsIgnoreCase(currentUid)) continue;

                                                            // Exclude admin role
                                                            String role = doc.getString("role");
                                                            if ("admin".equalsIgnoreCase(role)) continue;

                                                            // Calculate match score
                                                            int score = calculateMatchScore(currentUserDoc, doc);

                                                            // ★ Like priority boost: users who liked me appear first
                                                            if (usersWhoLikedMe.contains(docId)) {
                                                                score += 1000;
                                                            }

                                                            scoredUsers.add(new UserWithScore(doc, score));
                                                        }

                                                        // Sort in descending order of match score
                                                        Collections.sort(scoredUsers, (u1, u2) ->
                                                                Integer.compare(u2.score, u1.score));

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

                                                        // Check if there are any unlinked mutual matches ready to connect
                                                        checkForPendingLoveSpaceNotification(currentUid);
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
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("HomeActivity", "Failed to fetch swipes: " + e.getMessage(), e);
                                pbLoading.setVisibility(View.GONE);
                                showEmptyState();
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("HomeActivity", "Failed to fetch incoming likes: " + e.getMessage(), e);
                    pbLoading.setVisibility(View.GONE);
                    showEmptyState();
                });
    }

    // ── Questionnaire Match Scoring ──────────────────────────────────────────

    private int calculateMatchScore(DocumentSnapshot current, DocumentSnapshot other) {
        int score = 0;

        // Gender & Looking For Alignment
        String myGender = current.getString("gender");
        String myLookingFor = current.getString("lookingFor");
        String otherGender = other.getString("gender");
        String otherLookingFor = other.getString("lookingFor");

        if (myLookingFor != null && otherGender != null) {
            if (myLookingFor.equalsIgnoreCase("Both") || myLookingFor.equalsIgnoreCase(otherGender)) {
                score += 15;
            } else {
                score -= 30;
            }
        }
        if (otherLookingFor != null && myGender != null) {
            if (otherLookingFor.equalsIgnoreCase("Both") || otherLookingFor.equalsIgnoreCase(myGender)) {
                score += 15;
            } else {
                score -= 30;
            }
        }

        // Goal Match
        String myGoal = current.getString("goal");
        String otherGoal = other.getString("goal");
        if (myGoal != null && myGoal.equalsIgnoreCase(otherGoal)) score += 10;

        // Religion Match
        String myReligion = current.getString("religion");
        String otherReligion = other.getString("religion");
        if (myReligion != null && myReligion.equalsIgnoreCase(otherReligion)) score += 8;

        // Sexual Orientation Match
        String myOrientation = current.getString("orientation");
        String otherOrientation = other.getString("orientation");
        if (myOrientation != null && myOrientation.equalsIgnoreCase(otherOrientation)) score += 5;

        // Most Important Life Aspect Match
        String myImp = current.getString("mostImportant");
        String otherImp = other.getString("mostImportant");
        if (myImp != null && myImp.equalsIgnoreCase(otherImp)) score += 7;

        // Lifestyle Match (Smoke)
        String mySmoke = current.getString("smoke");
        String otherSmoke = other.getString("smoke");
        if (mySmoke != null && mySmoke.equalsIgnoreCase(otherSmoke)) score += 4;

        // Lifestyle Match (Drink)
        String myDrink = current.getString("drink");
        String otherDrink = other.getString("drink");
        if (myDrink != null && myDrink.equalsIgnoreCase(otherDrink)) score += 4;

        // Education Match
        String myEdu = current.getString("education");
        String otherEdu = other.getString("education");
        if (myEdu != null && myEdu.equalsIgnoreCase(otherEdu)) score += 3;

        // Star Sign Match
        String mySign = current.getString("starSign");
        String otherSign = other.getString("starSign");
        if (mySign != null && mySign.equalsIgnoreCase(otherSign)) score += 2;

        // Age Compatibility
        String myAgeStr = current.getString("age");
        String otherAgeStr = other.getString("age");
        if (myAgeStr != null && otherAgeStr != null) {
            try {
                int myAge = Integer.parseInt(myAgeStr.trim());
                int otherAge = Integer.parseInt(otherAgeStr.trim());
                int ageDiff = Math.abs(myAge - otherAge);
                if (ageDiff <= 3) score += 12;
                else if (ageDiff <= 5) score += 8;
                else if (ageDiff <= 8) score += 4;
            } catch (Exception e) { /* ignore */ }
        }

        return score;
    }

    // ── Display ───────────────────────────────────────────────────────────────

    private void displayUser(DocumentSnapshot user) {
        String name = user.getString("name");
        if (name == null || name.isEmpty()) name = "Anonymous";
        tvNameAge.setText("Name : " + name);

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

        tvActiveBadge.setVisibility(View.VISIBLE);

        String photoUrl = user.getString("photoUrl");
        if (photoUrl == null || photoUrl.isEmpty()) photoUrl = user.getString("photo");

        if (photoUrl != null && !photoUrl.isEmpty() && photoUrl.startsWith("http")) {
            flLetterAvatar.setVisibility(View.VISIBLE);
            ivProfileImage.setVisibility(View.INVISIBLE);
            char firstLetter = name.trim().length() > 0 ? Character.toUpperCase(name.trim().charAt(0)) : '?';
            tvAvatarLetter.setText(String.valueOf(firstLetter));

            Glide.with(this)
                    .load(photoUrl)
                    .centerCrop()
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e,
                                                    Object model,
                                                    com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                    boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                                                       Object model,
                                                       com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                       com.bumptech.glide.load.DataSource dataSource,
                                                       boolean isFirstResource) {
                            flLetterAvatar.setVisibility(View.GONE);
                            ivProfileImage.setVisibility(View.VISIBLE);
                            return false;
                        }
                    })
                    .into(ivProfileImage);
        } else {
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
        if (currentIndex < suggestedUsers.size()) {
            currentIndex++;
        }
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

        btnUndo.setOnClickListener(v -> {
            applyClickEffect(v);
            if (!suggestedUsers.isEmpty() && currentIndex > 0) {
                currentIndex--;
                setProfileVisible(true);
                displayUser(suggestedUsers.get(currentIndex));
            } else {
                Toast.makeText(this, "No previous profiles", Toast.LENGTH_SHORT).show();
            }
        });

        btnSuperLike.setOnClickListener(v -> {
            applyClickEffect(v);
            recordSwipe("superlike");
            advanceToNextUser();
            Toast.makeText(this, "Super Liked! ⭐", Toast.LENGTH_SHORT).show();
        });

        btnLike.setOnClickListener(v -> {
            applyClickEffect(v);
            showHeartPopEffect();
            recordSwipe("like");
            advanceToNextUser();
            Toast.makeText(this, "Liked! 💚", Toast.LENGTH_SHORT).show();
        });

        btnMessage.setOnClickListener(v -> {
            applyClickEffect(v);
            if (!suggestedUsers.isEmpty() && currentIndex < suggestedUsers.size()) {
                DocumentSnapshot targetUser = suggestedUsers.get(currentIndex);
                String photoUrl = targetUser.getString("photoUrl");
                if (photoUrl == null || photoUrl.isEmpty()) photoUrl = targetUser.getString("photo");

                Intent intent = new Intent(this, LoveSpaceMessage.class);
                intent.putExtra("userId",    targetUser.getId());
                intent.putExtra("username",  targetUser.getString("name"));
                intent.putExtra("userPhoto", photoUrl);
                startActivity(intent);
            }
        });
    }

    // ── Swipe recording + Mutual Like Detection ──────────────────────────────

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

        // ── Mutual Like Detection ──────────────────────────────────────
        if (usersWhoLikedMe.contains(targetUid)) {
            checkAndCreateLoveSpace(myUid, targetUid);
        }
    }

    /**
     * Check if the current user already has an active LoveSpace.
     * If not, create one with the matched user.
     * If yes, save a notification instead.
     */
    private void checkAndCreateLoveSpace(String myUid, String matchedUid) {
        db.collection("lovespaces")
                .whereEqualTo("user1Uid", myUid)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snap1 -> {
                    if (!snap1.isEmpty()) {
                        saveMutualLikeNotification(myUid, matchedUid);
                        return;
                    }
                    db.collection("lovespaces")
                            .whereEqualTo("user2Uid", myUid)
                            .whereEqualTo("active", true)
                            .get()
                            .addOnSuccessListener(snap2 -> {
                                if (!snap2.isEmpty()) {
                                    saveMutualLikeNotification(myUid, matchedUid);
                                } else {
                                    checkMatchedUserLoveSpace(myUid, matchedUid);
                                }
                            });
                });
    }

    /**
     * Check if the matched user also has an active LoveSpace.
     * If not, create the LoveSpace. If yes, save notification to the correct person.
     */
    private void checkMatchedUserLoveSpace(String myUid, String matchedUid) {
        db.collection("lovespaces")
                .whereEqualTo("user1Uid", matchedUid)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snap1 -> {
                    if (!snap1.isEmpty()) {
                        // matchedUid has the active LoveSpace — notify THEM, not me
                        saveBlockedNotificationForBoth(myUid, matchedUid);
                        return;
                    }
                    db.collection("lovespaces")
                            .whereEqualTo("user2Uid", matchedUid)
                            .whereEqualTo("active", true)
                            .get()
                            .addOnSuccessListener(snap2 -> {
                                if (!snap2.isEmpty()) {
                                    // matchedUid has the active LoveSpace — notify THEM, not me
                                    saveBlockedNotificationForBoth(myUid, matchedUid);
                                } else {
                                    createLoveSpace(myUid, matchedUid);
                                }
                            });
                });
    }

    /**
     * Create a new LoveSpace document and show the match celebration dialog.
     */
    private void createLoveSpace(String myUid, String matchedUid) {
        Map<String, Object> loveSpace = new HashMap<>();
        loveSpace.put("user1Uid", myUid);
        loveSpace.put("user2Uid", matchedUid);
        loveSpace.put("createdAt", System.currentTimeMillis());
        loveSpace.put("active", true);

        db.collection("lovespaces").add(loveSpace)
                .addOnSuccessListener(ref -> {
                    db.collection("users").document(matchedUid).get()
                            .addOnSuccessListener(userDoc -> {
                                String matchName = "Someone";
                                if (userDoc.exists()) {
                                    String name = userDoc.getString("name");
                                    if (name != null && !name.isEmpty()) matchName = name;
                                }
                                showMatchDialog(matchName);
                            })
                            .addOnFailureListener(e -> showMatchDialog("Someone"));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to create LoveSpace", Toast.LENGTH_SHORT).show());
    }

    /**
     * Show a "It's a Match!" celebration dialog.
     */
    private void showMatchDialog(String matchName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("💕 It's a Match!");
        builder.setMessage("You and " + matchName + " liked each other!\nYour LoveSpace has been created. 🎉");
        builder.setCancelable(false);
        builder.setPositiveButton("Go to LoveSpace", (dialog, which) -> {
            dialog.dismiss();
            startActivity(new Intent(this, LoveSpaceActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        });
        builder.setNegativeButton("Keep Swiping", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    /**
     * Check if the current user has any pending mutual matches (blocked earlier)
     * where both users are now free.
     */
    private void checkForPendingLoveSpaceNotification(String currentUid) {
        db.collection("lovespaces")
                .whereEqualTo("user1Uid", currentUid)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snap1 -> {
                    if (!snap1.isEmpty()) return;
                    db.collection("lovespaces")
                            .whereEqualTo("user2Uid", currentUid)
                            .whereEqualTo("active", true)
                            .get()
                            .addOnSuccessListener(snap2 -> {
                                if (!snap2.isEmpty()) return;

                                db.collection("notifications")
                                        .whereEqualTo("toUid", currentUid)
                                        .whereEqualTo("type", "mutual_like_blocked")
                                        .get()
                                        .addOnSuccessListener(notifSnap -> {
                                            if (notifSnap.isEmpty()) return;

                                            DocumentSnapshot latestDoc = null;
                                            long maxTs = -1;
                                            for (DocumentSnapshot doc : notifSnap.getDocuments()) {
                                                Long ts = doc.getLong("timestamp");
                                                if (ts != null && ts > maxTs) {
                                                    maxTs = ts;
                                                    latestDoc = doc;
                                                }
                                            }

                                            if (latestDoc != null) {
                                                String targetUid = latestDoc.getString("fromUid");
                                                String targetName = latestDoc.getString("fromName");
                                                if (targetName == null) targetName = "Someone";

                                                if (targetUid != null && !targetUid.isEmpty()) {
                                                    checkTargetAndPromptMatch(currentUid, targetUid, targetName, latestDoc);
                                                }
                                            }
                                        });
                            });
                });
    }

    private void checkTargetAndPromptMatch(String myUid, String targetUid, String targetName, DocumentSnapshot notifDoc) {
        // Check if there is already an existing (active or past) LoveSpace between these two users
        db.collection("lovespaces")
                .whereEqualTo("user1Uid", myUid)
                .whereEqualTo("user2Uid", targetUid)
                .get()
                .addOnSuccessListener(ls1 -> {
                    if (!ls1.isEmpty()) {
                        // Past or active LoveSpace already existed! Delete stale notification.
                        if (notifDoc != null) notifDoc.getReference().delete();
                        return;
                    }
                    db.collection("lovespaces")
                            .whereEqualTo("user1Uid", targetUid)
                            .whereEqualTo("user2Uid", myUid)
                            .get()
                            .addOnSuccessListener(ls2 -> {
                                if (!ls2.isEmpty()) {
                                    // Past or active LoveSpace already existed! Delete stale notification.
                                    if (notifDoc != null) notifDoc.getReference().delete();
                                    return;
                                }

                                db.collection("lovespaces")
                                        .whereEqualTo("user1Uid", targetUid)
                                        .whereEqualTo("active", true)
                                        .get()
                                        .addOnSuccessListener(snap1 -> {
                                            if (!snap1.isEmpty()) return;
                                            db.collection("lovespaces")
                                                    .whereEqualTo("user2Uid", targetUid)
                                                    .whereEqualTo("active", true)
                                                    .get()
                                                    .addOnSuccessListener(snap2 -> {
                                                        if (!snap2.isEmpty()) return;

                                                        if (isFinishing() || isDestroyed()) return;
                                                        new AlertDialog.Builder(this)
                                                                .setTitle("💕 Pending Match Ready!")
                                                                .setMessage("You and " + targetName + " mutually matched! Since both of you are now free, would you like to start your LoveSpace now?")
                                                                .setPositiveButton("Start LoveSpace", (dialog, which) -> {
                                                                    if (notifDoc != null) notifDoc.getReference().delete();
                                                                    createLoveSpace(myUid, targetUid);
                                                                })
                                                                .setNegativeButton("Not Now", null)
                                                                .show();
                                                    });
                                        });
                            });
                });
    }

    /**
     * Save a notification when the CURRENT USER (myUid) has the active LoveSpace.
     * Notification goes to myUid telling them they matched but already have a LoveSpace.
     */
    private void saveMutualLikeNotification(String myUid, String matchedUid) {
        db.collection("users").document(matchedUid).get()
                .addOnSuccessListener(userDoc -> {
                    String matchName = "Someone";
                    if (userDoc.exists()) {
                        String name = userDoc.getString("name");
                        if (name != null && !name.isEmpty()) matchName = name;
                    }

                    // Notification for current user (who has the LoveSpace)
                    Map<String, Object> notif = new HashMap<>();
                    notif.put("toUid", myUid);
                    notif.put("fromUid", matchedUid);
                    notif.put("fromName", matchName);
                    notif.put("type", "mutual_like_blocked");
                    notif.put("message", "You and " + matchName + " liked each other! 💕 But you already have an active LoveSpace.");
                    notif.put("timestamp", System.currentTimeMillis());
                    notif.put("read", false);
                    db.collection("notifications").add(notif);

                    Toast.makeText(this,
                            "Mutual match with " + matchName + "! 💕\nBut you already have an active LoveSpace.",
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Save notifications when the MATCHED USER (matchedUid) has the active LoveSpace,
     * but the current user (myUid) does NOT.
     * - matchedUid gets: "You and X liked each other! But you already have an active LoveSpace."
     * - myUid gets: "You and Y liked each other! But Y already has an active LoveSpace."
     */
    private void saveBlockedNotificationForBoth(String myUid, String matchedUid) {
        // Get both user names
        db.collection("users").document(matchedUid).get()
                .addOnSuccessListener(matchedDoc -> {
                    String matchedName = "Someone";
                    if (matchedDoc.exists()) {
                        String name = matchedDoc.getString("name");
                        if (name != null && !name.isEmpty()) matchedName = name;
                    }

                    final String finalMatchedName = matchedName;

                    db.collection("users").document(myUid).get()
                            .addOnSuccessListener(myDoc -> {
                                String myName = "Someone";
                                if (myDoc.exists()) {
                                    String name = myDoc.getString("name");
                                    if (name != null && !name.isEmpty()) myName = name;
                                }

                                long now = System.currentTimeMillis();

                                // Notification for the MATCHED user (who has the active LoveSpace)
                                Map<String, Object> notifForMatched = new HashMap<>();
                                notifForMatched.put("toUid", matchedUid);
                                notifForMatched.put("fromUid", myUid);
                                notifForMatched.put("fromName", myName);
                                notifForMatched.put("type", "mutual_like_blocked");
                                notifForMatched.put("message", "You and " + myName + " liked each other! 💕 But you already have an active LoveSpace.");
                                notifForMatched.put("timestamp", now);
                                notifForMatched.put("read", false);
                                db.collection("notifications").add(notifForMatched);

                                // Notification for the CURRENT user (who does NOT have a LoveSpace)
                                Map<String, Object> notifForMe = new HashMap<>();
                                notifForMe.put("toUid", myUid);
                                notifForMe.put("fromUid", matchedUid);
                                notifForMe.put("fromName", finalMatchedName);
                                notifForMe.put("type", "mutual_like_blocked");
                                notifForMe.put("message", "You and " + finalMatchedName + " liked each other! 💕 But " + finalMatchedName + " already has an active LoveSpace.");
                                notifForMe.put("timestamp", now);
                                notifForMe.put("read", false);
                                db.collection("notifications").add(notifForMe);

                                Toast.makeText(this,
                                        "Mutual match with " + finalMatchedName + "! 💕\nBut " + finalMatchedName + " already has an active LoveSpace.",
                                        Toast.LENGTH_LONG).show();
                            });
                });
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
                            onSwipeRight();
                        } else {
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
        if (!suggestedUsers.isEmpty() && currentIndex > 0) {
            currentIndex--;
            setProfileVisible(true);
            displayUser(suggestedUsers.get(currentIndex));
        } else {
            Toast.makeText(this, "No previous profiles", Toast.LENGTH_SHORT).show();
        }
    }

    private void onSwipeLeft() {
        if (!suggestedUsers.isEmpty()) {
            if (currentIndex < suggestedUsers.size()) {
                currentIndex++;
            }
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
