package com.example.lovecare;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Set;
import java.util.HashSet;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LoveSpaceActivity extends AppCompatActivity {

    private MaterialCardView emojiSelectorCard;
    private MaterialCardView calendarContextMenuCard;
    private View calendarMenuDim;
    private TextView tvUserEmojiOverlay;

    private final Handler longPressHandler = new Handler(Looper.getMainLooper());
    private boolean isLongPressTriggered = false;
    private static final int PROFILE_HOLD_DURATION = 1500;

    private String selectedCalendarDate;
    private GridView calendarGridView;
    private TextView tvMonthYear;
    private Calendar calendarInstance;
    private final Set<String> datesWithEvents = new HashSet<>();
    private final List<String> gridDays = new ArrayList<>();
    private CalendarAdapter calendarAdapter;
    private com.google.firebase.firestore.ListenerRegistration eventsListenerRegistration;

    // XP & Level Views
    private TextView tvLevelInfo;
    private TextView tvXpRatio;
    private LinearProgressIndicator xpProgress;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DatabaseReference chatRef;
    private ValueEventListener chatMessageListener;

    private String currentUid;
    private String activeLoveSpaceId;
    private String partnerUid;

    // Views for LoveSpace state
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

        tvLevelInfo = findViewById(R.id.tvLevelInfo);
        tvXpRatio = findViewById(R.id.tvXpRatio);
        xpProgress = findViewById(R.id.xpProgress);

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
            if (partnerUid != null && !partnerUid.isEmpty()) {
                intent.putExtra("partnerUid", partnerUid);
            }
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

        // Close LoveSpace button
        btnCloseLoveSpace.setOnClickListener(v -> showCloseLoveSpaceDialog());

        setupCalendarMenuButtons();

        // Load active LoveSpace from Firebase
        loadActiveLoveSpace();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatRef != null && chatMessageListener != null) {
            chatRef.removeEventListener(chatMessageListener);
        }
        if (loveSpaceListenerRegistration != null) {
            loveSpaceListenerRegistration.remove();
        }
        if (eventsListenerRegistration != null) {
            eventsListenerRegistration.remove();
        }
    }

    // ── Load Active LoveSpace ─────────────────────────────────────────────────

    private boolean isUser1 = false;
    private com.google.firebase.firestore.ListenerRegistration loveSpaceListenerRegistration;

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
                        isUser1 = true;
                        showActiveLoveSpace();
                        loadPartnerProfile();
                        setupRealtimeXpAndStreak();
                        setupRealtimeLoveSpaceListener();
                        setupRealtimeEventsListener();
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
                                    isUser1 = false;
                                    showActiveLoveSpace();
                                    loadPartnerProfile();
                                    setupRealtimeXpAndStreak();
                                    setupRealtimeLoveSpaceListener();
                                    setupRealtimeEventsListener();
                                } else {
                                    showEmptyState();
                                }
                            })
                            .addOnFailureListener(e -> showEmptyState());
                })
                .addOnFailureListener(e -> showEmptyState());
    }

    private void setupRealtimeLoveSpaceListener() {
        if (activeLoveSpaceId == null || activeLoveSpaceId.isEmpty())
            return;

        if (loveSpaceListenerRegistration != null) {
            loveSpaceListenerRegistration.remove();
        }

        loveSpaceListenerRegistration = db.collection("lovespaces")
                .document(activeLoveSpaceId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists())
                        return;

                    String myMood = isUser1 ? snapshot.getString("user1Mood") : snapshot.getString("user2Mood");
                    String partnerMood = isUser1 ? snapshot.getString("user2Mood") : snapshot.getString("user1Mood");

                    TextView tvPartnerEmojiOverlay = findViewById(R.id.tvPartnerEmojiOverlay);

                    if (tvUserEmojiOverlay != null) {
                        if (myMood != null && !myMood.isEmpty()) {
                            tvUserEmojiOverlay.setText(myMood);
                            tvUserEmojiOverlay.setVisibility(View.VISIBLE);
                        }
                    }

                    if (tvPartnerEmojiOverlay != null) {
                        if (partnerMood != null && !partnerMood.isEmpty()) {
                            tvPartnerEmojiOverlay.setText(partnerMood);
                            tvPartnerEmojiOverlay.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    // ── Realtime Message Count & XP / Level Streak ───────────────────────────

    private void setupRealtimeXpAndStreak() {
        if (currentUid == null || partnerUid == null)
            return;
        String chatId = currentUid.compareTo(partnerUid) < 0 ? currentUid + "_" + partnerUid
                : partnerUid + "_" + currentUid;

        if (chatRef != null && chatMessageListener != null) {
            chatRef.removeEventListener(chatMessageListener);
        }

        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("messages");
        chatMessageListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long msgCount = snapshot.getChildrenCount();
                long totalXp = msgCount * 10L; // 10 XP per message sent
                int level = (int) (totalXp / 1000) + 1;
                int currentLevelXp = (int) (totalXp % 1000);

                if (tvLevelInfo != null) {
                    tvLevelInfo.setText("LEVEL " + String.format(Locale.getDefault(), "%02d", level) + " 🔥 ("
                            + msgCount + " msgs)");
                }
                if (tvXpRatio != null) {
                    tvXpRatio.setText(currentLevelXp + " / 1000 XP");
                }
                if (xpProgress != null) {
                    xpProgress.setMax(1000);
                    xpProgress.setProgress(currentLevelXp);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        chatRef.addValueEventListener(chatMessageListener);
    }

    private void loadPartnerProfile() {
        if (partnerUid == null || partnerUid.isEmpty())
            return;

        db.collection("users").document(partnerUid).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String name = userDoc.getString("name");
                        if (name == null || name.isEmpty())
                            name = "Partner";

                        View flPartnerLetterAvatar = findViewById(R.id.flPartnerLetterAvatar);
                        TextView tvPartnerAvatarLetter = findViewById(R.id.tvPartnerAvatarLetter);
                        ShapeableImageView ivPartnerProfile = findViewById(R.id.ivPartnerProfile);

                        if (flPartnerLetterAvatar != null && tvPartnerAvatarLetter != null) {
                            flPartnerLetterAvatar.setVisibility(View.VISIBLE);
                            char letter = name.trim().length() > 0 ? Character.toUpperCase(name.trim().charAt(0)) : '?';
                            tvPartnerAvatarLetter.setText(String.valueOf(letter));
                        }
                        if (ivPartnerProfile != null) {
                            ivPartnerProfile.setVisibility(View.INVISIBLE);
                        }

                        String photoUrl = userDoc.getString("photoUrl");
                        if (photoUrl == null || photoUrl.isEmpty())
                            photoUrl = userDoc.getString("photo");

                        if (photoUrl != null && !photoUrl.isEmpty() && photoUrl.startsWith("http")
                                && ivPartnerProfile != null) {
                            Glide.with(this)
                                    .load(photoUrl)
                                    .centerCrop()
                                    .listener(
                                            new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                                                @Override
                                                public boolean onLoadFailed(
                                                        @androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e,
                                                        Object model,
                                                        com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                        boolean isFirstResource) {
                                                    return false;
                                                }

                                                @Override
                                                public boolean onResourceReady(
                                                        android.graphics.drawable.Drawable resource,
                                                        Object model,
                                                        com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                        com.bumptech.glide.load.DataSource dataSource,
                                                        boolean isFirstResource) {
                                                    if (flPartnerLetterAvatar != null)
                                                        flPartnerLetterAvatar.setVisibility(View.GONE);
                                                    if (ivPartnerProfile != null)
                                                        ivPartnerProfile.setVisibility(View.VISIBLE);
                                                    return false;
                                                }
                                            })
                                    .into(ivPartnerProfile);
                        }

                        loadCurrentUserProfile();
                    }
                });
    }

    private void loadCurrentUserProfile() {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String name = userDoc.getString("name");
                        if (name == null || name.isEmpty())
                            name = "User";

                        View flUserLetterAvatar = findViewById(R.id.flUserLetterAvatar);
                        TextView tvUserAvatarLetter = findViewById(R.id.tvUserAvatarLetter);
                        ShapeableImageView ivUserProfile = findViewById(R.id.ivUserProfile);

                        if (flUserLetterAvatar != null && tvUserAvatarLetter != null) {
                            flUserLetterAvatar.setVisibility(View.VISIBLE);
                            char letter = name.trim().length() > 0 ? Character.toUpperCase(name.trim().charAt(0)) : '?';
                            tvUserAvatarLetter.setText(String.valueOf(letter));
                        }
                        if (ivUserProfile != null) {
                            ivUserProfile.setVisibility(View.INVISIBLE);
                        }

                        String photoUrl = userDoc.getString("photoUrl");
                        if (photoUrl == null || photoUrl.isEmpty())
                            photoUrl = userDoc.getString("photo");

                        if (photoUrl != null && !photoUrl.isEmpty() && photoUrl.startsWith("http")
                                && ivUserProfile != null) {
                            Glide.with(this)
                                    .load(photoUrl)
                                    .centerCrop()
                                    .listener(
                                            new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                                                @Override
                                                public boolean onLoadFailed(
                                                        @androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e,
                                                        Object model,
                                                        com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                        boolean isFirstResource) {
                                                    return false;
                                                }

                                                @Override
                                                public boolean onResourceReady(
                                                        android.graphics.drawable.Drawable resource,
                                                        Object model,
                                                        com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                        com.bumptech.glide.load.DataSource dataSource,
                                                        boolean isFirstResource) {
                                                    if (flUserLetterAvatar != null)
                                                        flUserLetterAvatar.setVisibility(View.GONE);
                                                    if (ivUserProfile != null)
                                                        ivUserProfile.setVisibility(View.VISIBLE);
                                                    return false;
                                                }
                                            })
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
                .setMessage(
                        "Are you sure you want to close this LoveSpace? This will end the connection for both of you. 💔")
                .setPositiveButton("Close", (dialog, which) -> closeLoveSpace())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void closeLoveSpace() {
        if (activeLoveSpaceId == null || activeLoveSpaceId.isEmpty()) {
            Toast.makeText(this, "No active LoveSpace to close", Toast.LENGTH_SHORT).show();
            return;
        }

        final String oldPartnerUid = partnerUid;

        db.collection("lovespaces").document(activeLoveSpaceId)
                .update("active", false)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "LoveSpace closed 💔", Toast.LENGTH_SHORT).show();
                    activeLoveSpaceId = null;
                    partnerUid = null;
                    if (eventsListenerRegistration != null) {
                        eventsListenerRegistration.remove();
                        eventsListenerRegistration = null;
                    }
                    datesWithEvents.clear();
                    showEmptyState();

                    // Delete stale mutual match notifications between these two users
                    if (oldPartnerUid != null && !oldPartnerUid.isEmpty()) {
                        cleanupNotificationsBetweenUsers(currentUid, oldPartnerUid);
                    }
                })
                .addOnFailureListener(e -> Toast
                        .makeText(this, "Failed to close LoveSpace: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void cleanupNotificationsBetweenUsers(String uid1, String uid2) {
        if (uid1 == null || uid2 == null)
            return;
        db.collection("notifications")
                .whereEqualTo("toUid", uid1)
                .whereEqualTo("fromUid", uid2)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        doc.getReference().delete();
                    }
                });
        db.collection("notifications")
                .whereEqualTo("toUid", uid2)
                .whereEqualTo("fromUid", uid1)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        doc.getReference().delete();
                    }
                });
    }

    // ── Calendar Interaction & Event Backend Integration ─────────────────────

    private void setupCalendarInteraction() {
        calendarGridView = findViewById(R.id.calendarGridView);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        View btnPrevMonth = findViewById(R.id.btnPrevMonth);
        View btnNextMonth = findViewById(R.id.btnNextMonth);

        calendarInstance = Calendar.getInstance();
        Calendar today = Calendar.getInstance();
        selectedCalendarDate = String.format(Locale.getDefault(), "%02d/%02d/%d",
                today.get(Calendar.DAY_OF_MONTH), (today.get(Calendar.MONTH) + 1), today.get(Calendar.YEAR));

        if (btnPrevMonth != null) {
            btnPrevMonth.setOnClickListener(v -> {
                calendarInstance.add(Calendar.MONTH, -1);
                updateCalendarGrid();
            });
        }

        if (btnNextMonth != null) {
            btnNextMonth.setOnClickListener(v -> {
                calendarInstance.add(Calendar.MONTH, 1);
                updateCalendarGrid();
            });
        }

        if (calendarGridView != null) {
            calendarGridView.setOnItemClickListener((parent, view, position, id) -> {
                String dateStr = gridDays.get(position);
                if (!dateStr.isEmpty()) {
                    selectedCalendarDate = dateStr;
                    updateCalendarGrid();
                    if (calendarContextMenuCard != null && calendarContextMenuCard.getVisibility() == View.VISIBLE) {
                        hideCalendarContextMenu();
                    }
                }
            });

            calendarGridView.setOnItemLongClickListener((parent, view, position, id) -> {
                String dateStr = gridDays.get(position);
                if (!dateStr.isEmpty()) {
                    selectedCalendarDate = dateStr;
                    updateCalendarGrid();
                    showCalendarContextMenu();
                    return true;
                }
                return false;
            });
        }

        View btnCalendarAddEventDirect = findViewById(R.id.btnCalendarAddEventDirect);
        View btnCalendarAddNoteDirect = findViewById(R.id.btnCalendarAddNoteDirect);
        View btnCalendarViewDetailsDirect = findViewById(R.id.btnCalendarViewDetailsDirect);

        if (btnCalendarAddEventDirect != null) {
            btnCalendarAddEventDirect.setOnClickListener(v -> {
                showAddEventDialog(selectedCalendarDate);
            });
        }

        if (btnCalendarAddNoteDirect != null) {
            btnCalendarAddNoteDirect.setOnClickListener(v -> {
                showAddNoteDialog(selectedCalendarDate);
            });
        }

        if (btnCalendarViewDetailsDirect != null) {
            btnCalendarViewDetailsDirect.setOnClickListener(v -> {
                showViewDetailsDialog(selectedCalendarDate);
            });
        }

        updateCalendarGrid();
    }

    private void updateCalendarGrid() {
        if (calendarGridView == null || tvMonthYear == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(calendarInstance.getTime()));

        gridDays.clear();

        Calendar monthCal = (Calendar) calendarInstance.clone();
        monthCal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK);
        int maxDays = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        int emptyCells = firstDayOfWeek - 1;
        for (int i = 0; i < emptyCells; i++) {
            gridDays.add("");
        }

        int currentYear = monthCal.get(Calendar.YEAR);
        int currentMonthValue = monthCal.get(Calendar.MONTH) + 1;
        for (int day = 1; day <= maxDays; day++) {
            gridDays.add(String.format(Locale.getDefault(), "%02d/%02d/%d", day, currentMonthValue, currentYear));
        }

        if (calendarAdapter == null) {
            calendarAdapter = new CalendarAdapter();
            calendarGridView.setAdapter(calendarAdapter);
        } else {
            calendarAdapter.notifyDataSetChanged();
        }
    }

    private void setupRealtimeEventsListener() {
        if (activeLoveSpaceId == null || activeLoveSpaceId.isEmpty()) return;

        if (eventsListenerRegistration != null) {
            eventsListenerRegistration.remove();
        }

        eventsListenerRegistration = db.collection("lovespace_events")
                .whereEqualTo("loveSpaceId", activeLoveSpaceId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        return;
                    }
                    datesWithEvents.clear();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            String dateStr = doc.getString("date");
                            if (dateStr != null) {
                                datesWithEvents.add(dateStr);
                            }
                        }
                    }
                    updateCalendarGrid();
                });
    }

    private void showCalendarContextMenu() {
        if (calendarContextMenuCard == null)
            return;

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
        if (calendarContextMenuCard == null || calendarContextMenuCard.getVisibility() != View.VISIBLE)
            return;

        if (calendarMenuDim != null) {
            calendarMenuDim.animate().alpha(0f).setDuration(200)
                    .withEndAction(() -> calendarMenuDim.setVisibility(View.GONE)).start();
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
        if (calendarContextMenuCard == null)
            return;

        findViewById(R.id.btnCalendarAddEvent).setOnClickListener(v -> {
            hideCalendarContextMenu();
            showAddEventDialog(selectedCalendarDate);
        });

        findViewById(R.id.btnCalendarViewDetails).setOnClickListener(v -> {
            hideCalendarContextMenu();
            showViewDetailsDialog(selectedCalendarDate);
        });

        findViewById(R.id.btnCalendarAddNote).setOnClickListener(v -> {
            hideCalendarContextMenu();
            showAddNoteDialog(selectedCalendarDate);
        });

        if (calendarMenuDim != null) {
            calendarMenuDim.setOnClickListener(v -> hideCalendarContextMenu());
        }
    }

    private void setupUserLongPress(View container) {
        if (container == null)
            return;

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
        int[] emojiIds = { R.id.tvEmoji1, R.id.tvEmoji2, R.id.tvEmoji3, R.id.tvEmoji4 };
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

                    // Save mood emoji to active LoveSpace in Firestore
                    if (activeLoveSpaceId != null && !activeLoveSpaceId.isEmpty()) {
                        String moodField = isUser1 ? "user1Mood" : "user2Mood";
                        db.collection("lovespaces").document(activeLoveSpaceId)
                                .update(moodField, selectedEmoji)
                                .addOnSuccessListener(aVoid -> Toast
                                        .makeText(this, "Mood updated! " + selectedEmoji, Toast.LENGTH_SHORT).show());
                    }
                });
            }
        }
    }

    // ── Firestore Event Operations ───────────────────────────────────────────

    private void showAddEventDialog(String date) {
        if (activeLoveSpaceId == null || activeLoveSpaceId.isEmpty()) {
            Toast.makeText(this, "No active LoveSpace connection", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📅 Add Event for " + date);
        final TextInputEditText input = new TextInputEditText(this);
        input.setHint("Enter event/activity name...");
        builder.setView(input);

        builder.setPositiveButton("Save Event", (dialog, which) -> {
            String title = input.getText() != null ? input.getText().toString().trim() : "";
            if (!title.isEmpty()) {
                saveLoveSpaceEvent(date, title, "Event");
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAddNoteDialog(String date) {
        if (activeLoveSpaceId == null || activeLoveSpaceId.isEmpty()) {
            Toast.makeText(this, "No active LoveSpace connection", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📝 Add Note for " + date);
        final TextInputEditText input = new TextInputEditText(this);
        input.setHint("Enter your special note...");
        builder.setView(input);

        builder.setPositiveButton("Save Note", (dialog, which) -> {
            String note = input.getText() != null ? input.getText().toString().trim() : "";
            if (!note.isEmpty()) {
                saveLoveSpaceEvent(date, note, "Note");
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveLoveSpaceEvent(String date, String title, String type) {
        Map<String, Object> event = new HashMap<>();
        event.put("loveSpaceId", activeLoveSpaceId);
        event.put("date", date);
        event.put("title", title);
        event.put("type", type);
        event.put("createdBy", currentUid);
        event.put("createdAt", System.currentTimeMillis());

        db.collection("lovespace_events")
                .add(event)
                .addOnSuccessListener(
                        docRef -> Toast.makeText(this, type + " saved for " + date + " 💕", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast
                        .makeText(this, "Failed to save " + type + ": " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showViewDetailsDialog(String date) {
        if (activeLoveSpaceId == null || activeLoveSpaceId.isEmpty()) {
            Toast.makeText(this, "No active LoveSpace connection", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Loading details for " + date + "...", Toast.LENGTH_SHORT).show();

        db.collection("lovespace_events")
                .whereEqualTo("loveSpaceId", activeLoveSpaceId)
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        new AlertDialog.Builder(this)
                                .setTitle("📅 Plans for " + date)
                                .setMessage(
                                        "No events or notes added for this date yet. Tap 'Add Event' or 'Add Note' to create one! 💕")
                                .setPositiveButton("OK", null)
                                .show();
                        return;
                    }

                    List<QueryDocumentSnapshot> docs = new ArrayList<>();
                    List<String> displayItems = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        docs.add(doc);
                        String type = doc.getString("type");
                        String title = doc.getString("title");
                        String icon = "Note".equalsIgnoreCase(type) ? "📝" : "📅";
                        displayItems.add(icon + " " + (title != null ? title : "Untitled"));
                    }

                    String[] itemsArray = displayItems.toArray(new String[0]);

                    new AlertDialog.Builder(this)
                            .setTitle("✨ Plans for " + date)
                            .setItems(itemsArray, (dialog, which) -> {
                                QueryDocumentSnapshot selectedDoc = docs.get(which);
                                showEventActionDialog(selectedDoc, date);
                            })
                            .setPositiveButton("Close", null)
                            .show();
                })
                .addOnFailureListener(e -> Toast
                        .makeText(this, "Error fetching events: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showEventActionDialog(QueryDocumentSnapshot doc, String date) {
        String type = doc.getString("type");
        String currentTitle = doc.getString("title");
        String docId = doc.getId();

        String[] options = {"✏️ Edit " + type, "🗑️ Delete " + type};

        new AlertDialog.Builder(this)
                .setTitle("Options for " + (currentTitle != null ? currentTitle : "Untitled"))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditEventDialog(docId, currentTitle, type, date);
                    } else if (which == 1) {
                        showDeleteEventConfirmDialog(docId, date);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditEventDialog(String docId, String currentTitle, String type, String date) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("✏️ Edit " + type + " for " + date);

        final TextInputEditText input = new TextInputEditText(this);
        input.setText(currentTitle);
        if ("Note".equalsIgnoreCase(type)) {
            input.setHint("Enter your special note...");
        } else {
            input.setHint("Enter event/activity name...");
        }

        FrameLayout container = new FrameLayout(this);
        int paddingPx = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(paddingPx, paddingPx / 2, paddingPx, paddingPx / 2);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String updatedTitle = input.getText() != null ? input.getText().toString().trim() : "";
            if (!updatedTitle.isEmpty()) {
                db.collection("lovespace_events").document(docId)
                        .update("title", updatedTitle)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, type + " updated successfully! 💕", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to update " + type + ": " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteEventConfirmDialog(String eventDocId, String date) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item?")
                .setMessage("Do you want to delete this event/note?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("lovespace_events").document(eventDocId)
                            .delete()
                            .addOnSuccessListener(
                                    aVoid -> Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(
                                    e -> Toast.makeText(this, "Failed to delete item", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private class CalendarAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return gridDays.size();
        }

        @Override
        public Object getItem(int position) {
            return gridDays.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean isEnabled(int position) {
            return !gridDays.get(position).isEmpty();
        }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_calendar_day, parent, false);
            }

            TextView tvDayNumber = convertView.findViewById(R.id.tvDayNumber);
            View viewDaySelection = convertView.findViewById(R.id.viewDaySelection);
            View viewDayIndicator = convertView.findViewById(R.id.viewDayIndicator);

            String dateStr = gridDays.get(position);

            if (dateStr.isEmpty()) {
                tvDayNumber.setText("");
                viewDaySelection.setVisibility(View.GONE);
                viewDayIndicator.setVisibility(View.GONE);
            } else {
                String[] parts = dateStr.split("/");
                String dayNum = String.valueOf(Integer.parseInt(parts[0]));
                tvDayNumber.setText(dayNum);

                if (dateStr.equals(selectedCalendarDate)) {
                    viewDaySelection.setVisibility(View.VISIBLE);
                } else {
                    viewDaySelection.setVisibility(View.GONE);
                }

                if (datesWithEvents.contains(dateStr)) {
                    viewDayIndicator.setVisibility(View.VISIBLE);
                    if (dateStr.equals(selectedCalendarDate)) {
                        viewDayIndicator.setBackgroundResource(R.drawable.bg_white_circle);
                    } else {
                        viewDayIndicator.setBackgroundResource(R.drawable.bg_pink_circle);
                    }
                } else {
                    viewDayIndicator.setVisibility(View.INVISIBLE);
                }
            }

            return convertView;
        }
    }
}