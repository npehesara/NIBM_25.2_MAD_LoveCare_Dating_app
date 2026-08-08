package com.example.admindashboardactivity;

import com.example.lovecare.R;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Admin dashboard home fragment.
 * Fetches real-time stats from Firestore:
 *   - Total users count from "users" collection
 *   - Couple matches count from "loveSpaces" collection
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private TextView tvTotalUsers;
    private TextView tvCoupleMatches;
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

        // Fetch real data
        loadTotalUsersCount();
        loadCoupleMatchesCount();
    }

    /**
     * Counts all documents in the "users" collection.
     */
    private void loadTotalUsersCount() {
        db.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    if (tvTotalUsers != null) {
                        tvTotalUsers.setText(formatNumber(count));
                    }
                    Log.d(TAG, "Total users: " + count);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user count", e);
                    if (tvTotalUsers != null) {
                        tvTotalUsers.setText("—");
                    }
                });
    }

    /**
     * Counts all documents in the "loveSpaces" collection for couple matches.
     * Falls back to counting users with "partnerId" field if loveSpaces doesn't exist.
     */
    private void loadCoupleMatchesCount() {
        db.collection("loveSpaces")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    if (tvCoupleMatches != null) {
                        tvCoupleMatches.setText(formatNumber(count));
                    }
                    Log.d(TAG, "Couple matches (loveSpaces): " + count);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "loveSpaces collection not found, trying fallback", e);
                    // Fallback: count users with partnerId
                    loadCoupleMatchesFallback();
                });
    }

    /**
     * Fallback: counts users that have a non-null "partnerId" field,
     * divides by 2 to get pair count.
     */
    private void loadCoupleMatchesFallback() {
        db.collection("users")
                .whereNotEqualTo("partnerId", null)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size() / 2; // Each pair has 2 users
                    if (tvCoupleMatches != null) {
                        tvCoupleMatches.setText(formatNumber(count));
                    }
                    Log.d(TAG, "Couple matches (fallback): " + count);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load couple matches", e);
                    if (tvCoupleMatches != null) {
                        tvCoupleMatches.setText("0");
                    }
                });
    }

    /**
     * Formats a number with locale-appropriate grouping (e.g., 1,234).
     */
    private String formatNumber(int number) {
        return NumberFormat.getNumberInstance(Locale.getDefault()).format(number);
    }
}