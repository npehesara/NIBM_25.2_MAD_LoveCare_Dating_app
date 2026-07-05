package com.example.lovecare;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
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


        calendarContextMenuCard = findViewById(R.id.calendarContextMenuCard);
        calendarMenuDim = findViewById(R.id.calendarMenuDim);
        emojiSelectorCard = findViewById(R.id.emojiSelectorCard);
        tvUserEmojiOverlay = findViewById(R.id.tvUserEmojiOverlay);


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
            Intent intent = new Intent(LoveSpaceActivity.this, LoveSpaceMessage.class);
            startActivity(intent);
        });

        findViewById(R.id.btnAchievementIcon).setOnClickListener(v -> {
            Intent intent = new Intent(LoveSpaceActivity.this, Achievement.class);
            startActivity(intent);
        });

        setupCalendarMenuButtons();
    }

    private void setupCalendarInteraction() {
        CalendarView calendarView = findViewById(R.id.calendarView);
        if (calendarView == null) return;


        Calendar c = Calendar.getInstance();
        selectedCalendarDate = String.format(Locale.getDefault(), "%02d/%02d/%d",
                c.get(Calendar.DAY_OF_MONTH), (c.get(Calendar.MONTH) + 1), c.get(Calendar.YEAR));


        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedCalendarDate = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, (month + 1), year);
            // Ensure contextual menu is closed if a new date is picked
            if (calendarContextMenuCard != null && calendarContextMenuCard.getVisibility() == View.VISIBLE) {
                hideCalendarContextMenu();
            }
        });


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