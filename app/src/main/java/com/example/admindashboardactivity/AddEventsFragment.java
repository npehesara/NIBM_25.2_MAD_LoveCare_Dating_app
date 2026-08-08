package com.example.admindashboardactivity;

import com.example.lovecare.R;

import android.graphics.Point;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin fragment for creating and managing announcements.
 */
public class AddEventsFragment extends Fragment implements AnnouncementAdapter.OnAnnouncementActionListener {

    private static final String TAG = "AddEventsFragment";

    private RecyclerView rvAnnouncements;
    private ProgressBar progressBar;
    private CardView cardEmptyState;
    private TextView tvAnnouncementCount;
    private FloatingActionButton fabAddEvent;

    private FirebaseFirestore db;
    private AnnouncementAdapter adapter;
    private final List<AdminAnnouncement> announcementList = new ArrayList<>();

    public AddEventsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        rvAnnouncements       = view.findViewById(R.id.rv_announcements);
        progressBar           = view.findViewById(R.id.progress_bar_announcements);
        cardEmptyState        = view.findViewById(R.id.card_empty_announcements);
        tvAnnouncementCount   = view.findViewById(R.id.tv_announcement_count);
        fabAddEvent           = view.findViewById(R.id.fab_add_event);

        adapter = new AnnouncementAdapter(announcementList, this);
        rvAnnouncements.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAnnouncements.setAdapter(adapter);

        fabAddEvent.setOnClickListener(v -> showAddAnnouncementDialog());

        loadAnnouncements();
    }

    private void loadAnnouncements() {
        showLoading(true);

        db.collection("announcements")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    announcementList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        AdminAnnouncement item = new AdminAnnouncement();
                        item.setId(doc.getId());
                        item.setTitle(doc.getString("title"));
                        item.setMessage(doc.getString("message"));

                        Long createdAt = doc.getLong("createdAt");
                        if (createdAt != null) {
                            item.setCreatedAt(createdAt);
                        }

                        announcementList.add(item);
                    }

                    adapter.notifyDataSetChanged();
                    showLoading(false);
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Failed to load announcements", e);
                    updateEmptyState();
                });
    }

    private void showAddAnnouncementDialog() {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_announcement, null);
        EditText etTitle = dialogView.findViewById(R.id.et_announcement_title);
        EditText etMessage = dialogView.findViewById(R.id.et_announcement_message);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getDecorView().setPadding(0, 0, 0, 0);
        }

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                Point size = new Point();
                dialog.getWindow().getWindowManager().getDefaultDisplay().getSize(size);
                int width = (int) (size.x * 0.90);
                dialog.getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
            }
        });

        dialogView.findViewById(R.id.btn_announcement_cancel).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btn_announcement_publish).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String message = etMessage.getText().toString().trim();

            if (TextUtils.isEmpty(title)) {
                etTitle.setError("Title is required");
                return;
            }

            if (TextUtils.isEmpty(message)) {
                etMessage.setError("Message is required");
                return;
            }

            publishAnnouncement(title, message);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void publishAnnouncement(String title, String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("message", message);
        data.put("createdAt", System.currentTimeMillis());
        data.put("author", "Admin");

        db.collection("announcements")
                .add(data)
                .addOnSuccessListener(docRef -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), R.string.announcement_created_success, Toast.LENGTH_SHORT).show();
                    }
                    loadAnnouncements();
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to publish announcement", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDeleteAnnouncement(AdminAnnouncement announcement, int position) {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Delete Announcement?")
                .setMessage("Are you sure you want to delete this announcement?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("announcements").document(announcement.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), R.string.announcement_deleted_success, Toast.LENGTH_SHORT).show();
                                }
                                announcementList.remove(position);
                                adapter.notifyItemRemoved(position);
                                updateEmptyState();
                            })
                            .addOnFailureListener(e -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Failed to delete", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (rvAnnouncements != null) {
            rvAnnouncements.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
    }

    private void updateEmptyState() {
        boolean isEmpty = announcementList.isEmpty();
        if (cardEmptyState != null) {
            cardEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        if (rvAnnouncements != null) {
            rvAnnouncements.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
        if (tvAnnouncementCount != null) {
            if (!isEmpty) {
                tvAnnouncementCount.setText(announcementList.size() + " Announcements");
                tvAnnouncementCount.setVisibility(View.VISIBLE);
            } else {
                tvAnnouncementCount.setVisibility(View.GONE);
            }
        }
    }
}