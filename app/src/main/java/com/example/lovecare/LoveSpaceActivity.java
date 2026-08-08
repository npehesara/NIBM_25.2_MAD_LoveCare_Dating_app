package com.example.lovecare;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Locale;

public class LoveSpaceActivity extends AppCompatActivity {

    private MaterialCardView emojiSelectorCard;
    private MaterialCardView calendarContextMenuCard;
    private View calendarMenuDim;
    private TextView tvUserEmojiOverlay;

    private final Handler longPressHandler = new Handler(Looper.getMainLooper());
    private boolean isLongPressTriggered = false;
    private static final int PROFILE_HOLD_DURATION = 1500;

    private String selectedCalendarDate;

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUid;
    private String activeLoveSpaceId;
    private String partnerUid;

    // ── Views for LoveSpace state ─────────────────────────────────────────────
    private LinearLayout llEmptyState;
    private ImageView btnCloseLoveSpace;
    private View xpCard, coupleVisualContainer, calendarCard, bottomNavContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_love_space);

        View root = findViewById(R.id.loveSpaceRoot);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }
        currentUid = mAuth.getCurrentUser().getUid();

        calendarContextMenuCard = findViewById(R.id.calendarContextMenuCard);
        calendarMenuDim = findViewById(R.id.calendarMenuDim);
        emojiSelectorCard = findViewById(R.id.emojiSelectorCard);
        tvUserEmojiOverlay = findViewById(R.id.tvUserEmojiOverlay);

        // LoveSpace state views
        llEmptyState = findViewById(R.id.llEmptyState);
        btnCloseLoveSpace = findViewById(R.id.btnCloseLoveSpace);
        xpCard = findViewById(R.id.xpCard);
        coupleVisualContainer = findViewById(R.id.coupleVisualContainer);
        calendarCard = findViewById(R.id.calendarCard);
        bottomNavContainer = findViewById(R.id.bottomNavContainer);

        // Calendar
        setupCalendarInteraction();

        View userProfileContainer = findViewById(R.id.userProfileContainer);
        setupUserLongPress(userProfileContainer);
        setupEmojiSelection();

        TextView tvPartnerEmojiOverlay = findViewById(R.id.tvPartnerEmojiOverlay);
        if (tvPartnerEmojiOverlay != null) {
            tvPartnerEmojiOverlay.setText("💖");
            tvPartnerEmojiOverlay.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.btnGallery).setOnClickListener(v -> {
            Intent intent = new Intent(LoveSpaceActivity.this, Gallery.class);
            startActivity(intent);
        });

        findViewById(R.id.btnMessage).setOnClickListener(v -> {
            if (partnerUid != null && !partnerUid.isEmpty()) {
                Intent intent = new Intent(LoveSpaceActivity.this, LoveSpaceMessage.class);
                intent.putExtra("userId", partnerUid);
                startActivity(intent);
            } else {
                Intent intent = new Intent(LoveSpaceActivity.this, UserChatActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btnAchievementIcon).setOnClickListener(v -> {
            Intent intent = new Intent(LoveSpaceActivity.this, Achievement.class);
            startActivity(intent);
        });

        // Close LoveSpace button
        btnCloseLoveSpace.setOnClickListener(v -> showCloseLoveSpaceDialog());

        setupCalendarMenuButtons();

        // Load active LoveSpace from Firebase
        loadActiveLoveSpace();
    }

    // ── Load Active LoveSpace ─────────────────────────────────────────────────

    private void loadActiveLoveSpace() {
        // Check where user is user1Uid
        db.collection("lovespaces")
                .whereEqualTo("user1Uid", currentUid)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snap1 -> {
                    if (!snap1.isEmpty()) {
                        DocumentSnapshot doc = snap1.getDocuments().get(0);
                        activeLoveSpaceId = doc.getId();
                        partnerUid = doc.getString("user2Uid");
                        showActiveLoveSpace();
                        loadPartnerProfile();
                        return;
                    }
                    // Check where user is user2Uid
                    db.collection("lovespaces")
                            .whereEqualTo("user2Uid", currentUid)
                            .whereEqualTo("active", true)
                            .get()
                            .addOnSuccessListener(snap2 -> {
                                if (!snap2.isEmpty()) {
                                    DocumentSnapshot doc = snap2.getDocuments().get(0);
                                    activeLoveSpaceId = doc.getId();
                                    partnerUid = doc.getString("user1Uid");
                                    showActiveLoveSpace();
                                    loadPartnerProfile();
                                } else {
                                    showEmptyState();
                                }
                            })
                            .addOnFailureListener(e -> showEmptyState());
                })
                .addOnFailureListener(e -> showEmptyState());
    }

    private void loadPartnerProfile() {
        if (partnerUid == null || partnerUid.isEmpty()) return;

        db.collection("users").document(partnerUid).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        // Load partner photo
                        String photoUrl = userDoc.getString("photoUrl");
                        if (photoUrl == null || photoUrl.isEmpty()) photoUrl = userDoc.getString("photo");

                        ShapeableImageView ivPartnerProfile = findViewById(R.id.ivPartnerProfile);
                        if (photoUrl != null && !photoUrl.isEmpty() && photoUrl.startsWith("http") && ivPartnerProfile != null) {
                            Glide.with(this)
                                    .load(photoUrl)
                                    .centerCrop()
                                    .into(ivPartnerProfile);
                        }

                        // Load current user photo
                        loadCurrentUserProfile();
                    }
                });
    }

    private void loadCurrentUserProfile() {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String photoUrl = userDoc.getString("photoUrl");
                        if (photoUrl == null || photoUrl.isEmpty()) photoUrl = userDoc.getString("photo");

                        ShapeableImageView ivUserProfile = findViewById(R.id.ivUserProfile);
                        if (photoUrl != null && !photoUrl.isEmpty() && photoUrl.startsWith("http") && ivUserProfile != null) {
                            Glide.with(this)
                                    .load(photoUrl)
                                    .centerCrop()
                                    .into(ivUserProfile);
                        }
                    }
                });
    }

    private void showActiveLoveSpace() {
        llEmptyState.setVisibility(View.GONE);
        btnCloseLoveSpace.setVisibility(View.VISIBLE);
        xpCard.setVisibility(View.VISIBLE);
        coupleVisualContainer.setVisibility(View.VISIBLE);
        calendarCard.setVisibility(View.VISIBLE);
        bottomNavContainer.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        llEmptyState.setVisibility(View.VISIBLE);
        btnCloseLoveSpace.setVisibility(View.GONE);
        xpCard.setVisibility(View.GONE);
        coupleVisualContainer.setVisibility(View.GONE);
        calendarCard.setVisibility(View.GONE);
        bottomNavContainer.setVisibility(View.GONE);
    }

    // ── Close LoveSpace ───────────────────────────────────────────────────────

    private void showCloseLoveSpaceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Close LoveSpace")
                .setMessage("Are you sure you want to close this LoveSpace? This will end the connection for both of you. 💔")
                .setPositiveButton("Close", (dialog, which) -> closeLoveSpace())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void closeLoveSpace() {
        if (activeLoveSpaceId == null || activeLoveSpaceId.isEmpty()) {
            Toast.makeText(this, "No active LoveSpace to close", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("lovespaces").document(activeLoveSpaceId)
                .update("active", false)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "LoveSpace closed 💔", Toast.LENGTH_SHORT).show();
                    activeLoveSpaceId = null;
                    partnerUid = null;
                    showEmptyState();

                    // Check if there's a pending mutual match now that the user is free
                    checkForPendingMutualMatches();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to close LoveSpace: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void checkForPendingMutualMatches() {
        if (currentUid == null || currentUid.isEmpty()) return;

        db.collection("notifications")
                .whereEqualTo("toUid", currentUid)
                .whereEqualTo("type", "mutual_like_blocked")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) return;

                    // Get the latest pending match
                    DocumentSnapshot latestNotif = null;
                    long latestTime = -1;
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Long ts = doc.getLong("timestamp");
                        if (ts != null && ts > latestTime) {
                            latestTime = ts;
                            latestNotif = doc;
                        }
                    }

                    if (latestNotif != null) {
                        String pendingUid = latestNotif.getString("fromUid");
                        String pendingName = latestNotif.getString("fromName");
                        if (pendingName == null) pendingName = "Someone";

                        if (pendingUid != null && !pendingUid.isEmpty()) {
                            checkAndPromptPendingMatch(pendingUid, pendingName);
                        }
                    }
                });
    }

    private void checkAndPromptPendingMatch(String targetUid, String targetName) {
        // Check if targetUid also has no active LoveSpace
        db.collection("lovespaces")
                .whereEqualTo("user1Uid", targetUid)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snap1 -> {
                    if (!snap1.isEmpty()) return; // target user is in another LoveSpace

                    db.collection("lovespaces")
                            .whereEqualTo("user2Uid", targetUid)
                            .whereEqualTo("active", true)
                            .get()
                            .addOnSuccessListener(snap2 -> {
                                if (!snap2.isEmpty()) return; // target user is in another LoveSpace

                                // Both are free! Prompt the user
                                new AlertDialog.Builder(this)
                                        .setTitle("💕 Pending Match Available!")
                                        .setMessage("You have a pending mutual match with " + targetName + "! Would you like to start a LoveSpace with them now?")
                                        .setPositiveButton("Start LoveSpace", (dialog, which) -> {
                                            createLoveSpaceWithPending(targetUid);
                                        })
                                        .setNegativeButton("Not Now", null)
                                        .show();
                            });
                });
    }

    private void createLoveSpaceWithPending(String targetUid) {
        java.util.Map<String, Object> loveSpace = new java.util.HashMap<>();
        loveSpace.put("user1Uid", currentUid);
        loveSpace.put("user2Uid", targetUid);
        loveSpace.put("createdAt", System.currentTimeMillis());
        loveSpace.put("active", true);

        db.collection("lovespaces").add(loveSpace)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "LoveSpace created! 💕", Toast.LENGTH_SHORT).show();
                    loadActiveLoveSpace(); // Reload to display the new active LoveSpace
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to create LoveSpace", Toast.LENGTH_SHORT).show());
    }

    // ── Calendar Interaction ──────────────────────────────────────────────────

    private void setupCalendarInteraction() {
        CalendarView calendarView = findViewById(R.id.calendarView);
        if (calendarView == null) return;

        // live date
        Calendar c = Calendar.getInstance();
        selectedCalendarDate = String.format(Locale.getDefault(), "%02d/%02d/%d",
                c.get(Calendar.DAY_OF_MONTH), (c.get(Calendar.MONTH) + 1), c.get(Calendar.YEAR));

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedCalendarDate = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, (month + 1), year);
            if (calendarContextMenuCard != null && calendarContextMenuCard.getVisibility() == View.VISIBLE) {
                hideCalendarContextMenu();
            }
        });

        // calander long press buttonn
        calendarView.setOnTouchListener(new View.OnTouchListener() {
            private final Runnable longPressRunnable = () -> showCalendarContextMenu();
            private float startX, startY;
            private static final int CALENDAR_HOLD_THRESHOLD = 800; // 0.8s hold to trigger

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        longPressHandler.postDelayed(longPressRunnable, CALENDAR_HOLD_THRESHOLD);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // Cancel hold if finger moves too much (slop)
                        if (Math.abs(event.getX() - startX) > 30 || Math.abs(event.getY() - startY) > 30) {
                            longPressHandler.removeCallbacks(longPressRunnable);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        longPressHandler.removeCallbacks(longPressRunnable);
                        break;
                }
                return false;
            }
        });
    }

    private void showCalendarContextMenu() {
        if (calendarContextMenuCard == null) return;

        calendarContextMenuCard.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

        if (calendarMenuDim != null) {
            calendarMenuDim.setVisibility(View.VISIBLE);
            calendarMenuDim.setAlpha(0f);
            calendarMenuDim.animate().alpha(1f).setDuration(250).start();
        }

        calendarContextMenuCard.setVisibility(View.VISIBLE);
        calendarContextMenuCard.setAlpha(0f);
        calendarContextMenuCard.setScaleX(0.8f);
        calendarContextMenuCard.setScaleY(0.8f);
        calendarContextMenuCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .start();
    }

    private void hideCalendarContextMenu() {
        if (calendarContextMenuCard == null || calendarContextMenuCard.getVisibility() != View.VISIBLE) return;

        if (calendarMenuDim != null) {
            calendarMenuDim.animate().alpha(0f).setDuration(200).withEndAction(() -> calendarMenuDim.setVisibility(View.GONE)).start();
        }

        calendarContextMenuCard.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(200)
                .withEndAction(() -> calendarContextMenuCard.setVisibility(View.GONE))
                .start();
    }

    private void setupCalendarMenuButtons() {
        if (calendarContextMenuCard == null) return;

        findViewById(R.id.btnCalendarAddEvent).setOnClickListener(v -> {
            hideCalendarContextMenu();
            showAddEventDialog(selectedCalendarDate);
        });

        findViewById(R.id.btnCalendarViewDetails).setOnClickListener(v -> {
            hideCalendarContextMenu();
            Toast.makeText(this, "Details for " + selectedCalendarDate, Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnCalendarAddNote).setOnClickListener(v -> {
            hideCalendarContextMenu();
            Toast.makeText(this, "Adding note for " + selectedCalendarDate, Toast.LENGTH_SHORT).show();
        });

        if (calendarMenuDim != null) {
            calendarMenuDim.setOnClickListener(v -> hideCalendarContextMenu());
        }
    }

    private void setupUserLongPress(View container) {
        if (container == null) return;

        final Runnable longPressRunnable = () -> {
            isLongPressTriggered = true;
            container.animate().scaleX(1.15f).scaleY(1.15f).setDuration(150)
                    .withEndAction(() -> container.animate().scaleX(1.0f).scaleY(1.0f).start());

            if (emojiSelectorCard != null) {
                emojiSelectorCard.setVisibility(View.VISIBLE);
                emojiSelectorCard.setAlpha(0f);
                emojiSelectorCard.setScaleX(0.7f);
                emojiSelectorCard.setScaleY(0.7f);
                emojiSelectorCard.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300).start();
                Toast.makeText(this, "Select your mode", Toast.LENGTH_SHORT).show();
            }
        };

        container.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isLongPressTriggered = false;
                    longPressHandler.postDelayed(longPressRunnable, PROFILE_HOLD_DURATION);
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(PROFILE_HOLD_DURATION).start();
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    longPressHandler.removeCallbacks(longPressRunnable);
                    if (!isLongPressTriggered) {
                        v.animate().cancel();
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            v.performClick();
                        }
                    } else {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    }
                    return true;
            }
            return false;
        });

        container.setOnClickListener(v -> {
            if (!isLongPressTriggered) {
                Toast.makeText(this, "Hold profile to change mood", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupEmojiSelection() {
        int[] emojiIds = {R.id.tvEmoji1, R.id.tvEmoji2, R.id.tvEmoji3, R.id.tvEmoji4};
        for (int id : emojiIds) {
            TextView emojiTv = findViewById(id);
            if (emojiTv != null) {
                emojiTv.setOnClickListener(v -> {
                    String selectedEmoji = ((TextView) v).getText().toString();
                    if (tvUserEmojiOverlay != null) {
                        tvUserEmojiOverlay.setText(selectedEmoji);
                        tvUserEmojiOverlay.setVisibility(View.VISIBLE);
                        tvUserEmojiOverlay.setScaleX(0.2f);
                        tvUserEmojiOverlay.setScaleY(0.2f);
                        tvUserEmojiOverlay.animate().scaleX(1f).scaleY(1f).setDuration(300).start();
                    }
                    if (emojiSelectorCard != null) {
                        emojiSelectorCard.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(200)
                                .withEndAction(() -> {
                                    emojiSelectorCard.setVisibility(View.GONE);
                                    emojiSelectorCard.setScaleX(1f);
                                    emojiSelectorCard.setScaleY(1f);
                                }).start();
                    }
                });
            }
        }
    }

    private void showAddEventDialog(String date) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Plan for " + date);
        final TextInputEditText input = new TextInputEditText(this);
        input.setHint("Enter activity...");
        builder.setView(input);
        builder.setPositiveButton("Save", (dialog, which) -> {
            if (input.getText() != null && !input.getText().toString().isEmpty()) {
                Toast.makeText(this, "Activity saved for " + date, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}