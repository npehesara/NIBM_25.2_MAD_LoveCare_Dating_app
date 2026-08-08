package com.example.admindashboardactivity;

import com.example.lovecare.R;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Admin dashboard home fragment.
 * Fetches real-time stats from Firestore:
 *   - Total users count from "users" collection
 *   - Couple matches count (active loveSpaces)
 *   - Breakups count (inactive/closed loveSpaces)
 *   - Dynamic User Growth bar chart for the last 7 days
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private TextView tvTotalUsers;
    private TextView tvCoupleMatches;
    private TextView tvBreakups;

    private View[] barViews = new View[7];
    private TextView[] tvBarCounts = new TextView[7];
    private TextView[] tvDayLabels = new TextView[7];

    private FirebaseFirestore db;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        tvTotalUsers    = view.findViewById(R.id.tv_total_users_count);
        tvCoupleMatches = view.findViewById(R.id.tv_couple_matches_count);
        tvBreakups      = view.findViewById(R.id.tv_breakups_count);

        // Bind bar chart views
        barViews[0] = view.findViewById(R.id.bar_0);
        barViews[1] = view.findViewById(R.id.bar_1);
        barViews[2] = view.findViewById(R.id.bar_2);
        barViews[3] = view.findViewById(R.id.bar_3);
        barViews[4] = view.findViewById(R.id.bar_4);
        barViews[5] = view.findViewById(R.id.bar_5);
        barViews[6] = view.findViewById(R.id.bar_6);

        tvBarCounts[0] = view.findViewById(R.id.tv_bar_count_0);
        tvBarCounts[1] = view.findViewById(R.id.tv_bar_count_1);
        tvBarCounts[2] = view.findViewById(R.id.tv_bar_count_2);
        tvBarCounts[3] = view.findViewById(R.id.tv_bar_count_3);
        tvBarCounts[4] = view.findViewById(R.id.tv_bar_count_4);
        tvBarCounts[5] = view.findViewById(R.id.tv_bar_count_5);
        tvBarCounts[6] = view.findViewById(R.id.tv_bar_count_6);

        tvDayLabels[0] = view.findViewById(R.id.tv_day_0);
        tvDayLabels[1] = view.findViewById(R.id.tv_day_1);
        tvDayLabels[2] = view.findViewById(R.id.tv_day_2);
        tvDayLabels[3] = view.findViewById(R.id.tv_day_3);
        tvDayLabels[4] = view.findViewById(R.id.tv_day_4);
        tvDayLabels[5] = view.findViewById(R.id.tv_day_5);
        tvDayLabels[6] = view.findViewById(R.id.tv_day_6);

        setupDayLabels();

        // Fetch real data
        loadUserDataAndGrowth();
        loadCoupleMatchesAndBreakups();
    }

    /**
     * Sets day label strings for the last 7 days (day 0 = 6 days ago, day 6 = today).
     */
    private void setupDayLabels() {
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);

        for (int i = 0; i < 7; i++) {
            if (tvDayLabels[i] != null) {
                tvDayLabels[i].setText(dayFormat.format(cal.getTime()));
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    /**
     * Loads total user count and calculates user growth across the last 7 days based on createdAt timestamp.
     */
    private void loadUserDataAndGrowth() {
        db.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalCount = querySnapshot.size();
                    if (tvTotalUsers != null) {
                        tvTotalUsers.setText(formatNumber(totalCount));
                    }

                    int[] dailyCounts = new int[7];
                    Calendar cal = Calendar.getInstance();

                    // Set calendar to start of today (00:00:00)
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);

                    long startOfToday = cal.getTimeInMillis();
                    long oneDayMs = 24 * 60 * 60 * 1000L;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Long createdAt = doc.getLong("createdAt");
                        if (createdAt != null && createdAt > 0) {
                            long diffMs = createdAt - startOfToday;
                            int dayOffset;
                            if (diffMs >= 0) {
                                dayOffset = 6; // Today
                            } else {
                                int daysAgo = (int) Math.floor((double) Math.abs(diffMs) / oneDayMs);
                                dayOffset = 6 - (daysAgo + 1);
                            }

                            if (dayOffset >= 0 && dayOffset <= 6) {
                                dailyCounts[dayOffset]++;
                            }
                        }
                    }

                    updateGrowthChart(dailyCounts);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user data", e);
                    if (tvTotalUsers != null) {
                        tvTotalUsers.setText("—");
                    }
                });
    }

    /**
     * Dynamically renders the growth chart bars based on daily registration counts.
     */
    private void updateGrowthChart(int[] counts) {
        int max = 1;
        for (int c : counts) {
            if (c > max) max = c;
        }

        int maxBarHeightDp = 130; // max height in dp
        int minBarHeightDp = 8;   // min height in dp for zero or small counts

        float density = getResources().getDisplayMetrics().density;

        for (int i = 0; i < 7; i++) {
            int count = counts[i];

            if (tvBarCounts[i] != null) {
                tvBarCounts[i].setText(count > 0 ? String.valueOf(count) : "0");
            }

            if (barViews[i] != null) {
                int heightDp = count > 0 ? Math.max(minBarHeightDp, (int) ((float) count / max * maxBarHeightDp)) : minBarHeightDp;
                int heightPx = (int) (heightDp * density);

                ViewGroup.LayoutParams params = barViews[i].getLayoutParams();
                if (params != null) {
                    params.height = heightPx;
                    barViews[i].setLayoutParams(params);
                }
            }
        }
    }

    /**
     * Counts open (active=true) and closed (active=false) loveSpaces.
     */
    private void loadCoupleMatchesAndBreakups() {
        // Try 'lovespaces' collection first
        db.collection("lovespaces")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        processLoveSpacesSnapshot(querySnapshot);
                    } else {
                        // Fallback try 'loveSpaces'
                        loadFromLoveSpacesCamelCase();
                    }
                })
                .addOnFailureListener(e -> loadFromLoveSpacesCamelCase());
    }

    private void loadFromLoveSpacesCamelCase() {
        db.collection("loveSpaces")
                .get()
                .addOnSuccessListener(this::processLoveSpacesSnapshot)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch loveSpaces collection", e);
                    if (tvCoupleMatches != null) tvCoupleMatches.setText("0");
                    if (tvBreakups != null) tvBreakups.setText("0");
                });
    }

    private void processLoveSpacesSnapshot(QuerySnapshot snapshot) {
        int activeCount = 0;
        int closedCount = 0;

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Boolean active = doc.getBoolean("active");
            if (Boolean.TRUE.equals(active)) {
                activeCount++;
            } else if (Boolean.FALSE.equals(active)) {
                closedCount++;
            }
        }

        if (tvCoupleMatches != null) {
            tvCoupleMatches.setText(formatNumber(activeCount));
        }

        if (tvBreakups != null) {
            tvBreakups.setText(formatNumber(closedCount));
        }

        Log.d(TAG, "Couple matches: " + activeCount + ", Breakups: " + closedCount);
    }

    /**
     * Formats a number with locale-appropriate grouping (e.g., 1,234).
     */
    private String formatNumber(int number) {
        return NumberFormat.getNumberInstance(Locale.getDefault()).format(number);
    }
}