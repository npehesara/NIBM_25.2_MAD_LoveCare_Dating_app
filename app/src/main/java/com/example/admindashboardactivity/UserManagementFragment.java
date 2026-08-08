package com.example.admindashboardactivity;

import com.example.lovecare.R;

import android.graphics.Point;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Admin fragment for user management.
 * Provides full CRUD (Create, Read, Update, Delete) operations
 * against the Firestore "users" collection, plus real-time search filtering.
 */
public class UserManagementFragment extends Fragment implements UserAdapter.OnUserActionListener {

    private static final String TAG = "UserManagement";

    // ── Views ────────────────────────────────────────────────────────────────
    private RecyclerView rvUsers;
    private ProgressBar progressBar;
    private CardView cardEmptyState;
    private TextView tvUserCount;
    private EditText etSearch;
    private FloatingActionButton fabAddUser;

    // ── Data ─────────────────────────────────────────────────────────────────
    private FirebaseFirestore db;
    private UserAdapter adapter;
    private final List<AdminUser> allUsers = new ArrayList<>();
    private final List<AdminUser> filteredUsers = new ArrayList<>();

    public UserManagementFragment() {
        // Required empty constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        // Bind views
        rvUsers        = view.findViewById(R.id.rv_users);
        progressBar    = view.findViewById(R.id.progress_bar);
        cardEmptyState = view.findViewById(R.id.card_empty_state);
        tvUserCount    = view.findViewById(R.id.tv_user_count);
        etSearch       = view.findViewById(R.id.et_search_users);
        fabAddUser     = view.findViewById(R.id.fab_add_user);

        // Setup RecyclerView
        adapter = new UserAdapter(filteredUsers, this);
        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        rvUsers.setAdapter(adapter);

        // Setup search
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        // FAB: Add new user
        fabAddUser.setOnClickListener(v -> showAddUserDialog());

        // Load users from Firestore
        loadUsers();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  READ – Fetch all users from Firestore
    // ══════════════════════════════════════════════════════════════════════════

    private void loadUsers() {
        showLoading(true);

        db.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allUsers.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        AdminUser user = new AdminUser();
                        user.setUid(doc.getId());
                        user.setName(doc.getString("name"));
                        user.setEmail(doc.getString("email"));
                        user.setRole(doc.getString("role"));
                        user.setGender(doc.getString("gender"));
                        user.setAge(doc.getString("age"));

                        // Handle both "photoUrl" and "photo" fields
                        String photoUrl = doc.getString("photoUrl");
                        if (photoUrl == null || photoUrl.isEmpty()) {
                            photoUrl = doc.getString("photo");
                        }
                        user.setPhotoUrl(photoUrl);

                        Long createdAt = doc.getLong("createdAt");
                        if (createdAt != null) {
                            user.setCreatedAt(createdAt);
                        }

                        allUsers.add(user);
                    }

                    // Apply current search filter
                    String currentSearch = etSearch.getText().toString().trim();
                    filterUsers(currentSearch);
                    showLoading(false);

                    Log.d(TAG, "Loaded " + allUsers.size() + " users from Firestore");
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Failed to load users", e);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load users: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    updateEmptyState();
                });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SEARCH – Filter users by name or email
    // ══════════════════════════════════════════════════════════════════════════

    private void filterUsers(String query) {
        filteredUsers.clear();

        if (TextUtils.isEmpty(query)) {
            filteredUsers.addAll(allUsers);
        } else {
            String lowerQuery = query.toLowerCase(Locale.ROOT);
            for (AdminUser user : allUsers) {
                String name = user.getName() != null ? user.getName().toLowerCase(Locale.ROOT) : "";
                String email = user.getEmail() != null ? user.getEmail().toLowerCase(Locale.ROOT) : "";
                if (name.contains(lowerQuery) || email.contains(lowerQuery)) {
                    filteredUsers.add(user);
                }
            }
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
        updateUserCount();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CREATE – Add a new user via dialog
    // ══════════════════════════════════════════════════════════════════════════

    private void showAddUserDialog() {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_user, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etName = dialogView.findViewById(R.id.et_dialog_name);
        EditText etEmail = dialogView.findViewById(R.id.et_dialog_email);
        AutoCompleteTextView actvRole = dialogView.findViewById(R.id.actv_dialog_role);

        tvTitle.setText(R.string.add_new_user);

        // Role dropdown
        String[] roles = {"user", "admin"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, roles);
        actvRole.setAdapter(roleAdapter);
        actvRole.setText("user", false);

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

        // Cancel button
        dialogView.findViewById(R.id.btn_dialog_cancel).setOnClickListener(v -> dialog.dismiss());

        // Save button
        dialogView.findViewById(R.id.btn_dialog_save).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String role = actvRole.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                etName.setError("Name is required");
                return;
            }
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Email is required");
                return;
            }
            if (TextUtils.isEmpty(role)) {
                role = "user";
            }

            createUser(name, email, role);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void createUser(String name, String email, String role) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("role", role);
        userData.put("createdAt", System.currentTimeMillis());

        db.collection("users")
                .add(userData)
                .addOnSuccessListener(docRef -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), R.string.user_added_success, Toast.LENGTH_SHORT).show();
                    }
                    // Add locally and refresh
                    AdminUser newUser = new AdminUser(docRef.getId(), name, email, role);
                    newUser.setCreatedAt(System.currentTimeMillis());
                    allUsers.add(0, newUser);
                    filterUsers(etSearch.getText().toString().trim());

                    Log.d(TAG, "User created with ID: " + docRef.getId());
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), R.string.user_save_error, Toast.LENGTH_SHORT).show();
                    }
                    Log.e(TAG, "Failed to create user", e);
                });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UPDATE – Edit an existing user via dialog
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void onEditUser(AdminUser user, int position) {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_user, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etName = dialogView.findViewById(R.id.et_dialog_name);
        EditText etEmail = dialogView.findViewById(R.id.et_dialog_email);
        AutoCompleteTextView actvRole = dialogView.findViewById(R.id.actv_dialog_role);

        tvTitle.setText(R.string.edit_user);

        // Pre-fill fields
        etName.setText(user.getName());
        etEmail.setText(user.getEmail());

        // Role dropdown
        String[] roles = {"user", "admin"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, roles);
        actvRole.setAdapter(roleAdapter);
        actvRole.setText(user.getRole() != null ? user.getRole() : "user", false);

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

        // Cancel button
        dialogView.findViewById(R.id.btn_dialog_cancel).setOnClickListener(v -> dialog.dismiss());

        // Save button
        dialogView.findViewById(R.id.btn_dialog_save).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String role = actvRole.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                etName.setError("Name is required");
                return;
            }
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Email is required");
                return;
            }

            updateUser(user, name, email, role);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateUser(AdminUser user, String name, String email, String role) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("role", role);

        db.collection("users").document(user.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), R.string.user_updated_success, Toast.LENGTH_SHORT).show();
                    }
                    // Update local data
                    user.setName(name);
                    user.setEmail(email);
                    user.setRole(role);
                    filterUsers(etSearch.getText().toString().trim());

                    Log.d(TAG, "User updated: " + user.getUid());
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), R.string.user_save_error, Toast.LENGTH_SHORT).show();
                    }
                    Log.e(TAG, "Failed to update user", e);
                });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DELETE – Remove a user after confirmation
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void onDeleteUser(AdminUser user, int position) {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.delete_confirm_title)
                .setMessage(getString(R.string.delete_confirm_message))
                .setPositiveButton(R.string.delete_user, (dialog, which) -> deleteUser(user))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteUser(AdminUser user) {
        db.collection("users").document(user.getUid())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), R.string.user_deleted_success, Toast.LENGTH_SHORT).show();
                    }
                    // Remove from local lists
                    allUsers.remove(user);
                    filterUsers(etSearch.getText().toString().trim());

                    Log.d(TAG, "User deleted: " + user.getUid());
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), R.string.user_delete_error, Toast.LENGTH_SHORT).show();
                    }
                    Log.e(TAG, "Failed to delete user", e);
                });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UI Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (rvUsers != null) {
            rvUsers.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
    }

    private void updateEmptyState() {
        boolean isEmpty = filteredUsers.isEmpty();
        if (cardEmptyState != null) {
            cardEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        if (rvUsers != null) {
            rvUsers.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }

    private void updateUserCount() {
        if (tvUserCount != null) {
            if (!filteredUsers.isEmpty()) {
                String countText = filteredUsers.size() + " of " + allUsers.size() + " users";
                tvUserCount.setText(countText);
                tvUserCount.setVisibility(View.VISIBLE);
            } else {
                tvUserCount.setVisibility(View.GONE);
            }
        }
    }
}