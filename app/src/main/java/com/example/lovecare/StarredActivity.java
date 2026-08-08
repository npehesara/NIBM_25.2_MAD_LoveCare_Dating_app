package com.example.lovecare;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StarredActivity extends AppCompatActivity {

    private RecyclerView rvStarredUsers;
    private ProgressBar pbStarredLoading;
    private LinearLayout llStarredEmpty;
    private BottomNavigationView bottomNav;

    private StarredUserAdapter adapter;
    private List<DocumentSnapshot> superLikersList = new ArrayList<>();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_starred);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Intent intent = new Intent(this, Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        currentUid = mAuth.getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        View root = findViewById(R.id.starredRoot);
        bottomNav = findViewById(R.id.bottomNavigation);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            if (bottomNav != null) {
                bottomNav.setPadding(0, 0, 0, systemBars.bottom);
            }
            return insets;
        });

        rvStarredUsers = findViewById(R.id.rvStarredUsers);
        pbStarredLoading = findViewById(R.id.pbStarredLoading);
        llStarredEmpty = findViewById(R.id.llStarredEmpty);

        setupRecyclerView();
        setupBottomNav();
        loadSuperLikers();
    }

    private void setupBottomNav() {
        if (bottomNav == null) return;
        bottomNav.setSelectedItemId(R.id.nav_starred);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_starred) {
                return true; // already here
            } else if (id == R.id.nav_swipe) {
                startActivity(new Intent(this, HomeActivity.class)
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

    private void setupRecyclerView() {
        adapter = new StarredUserAdapter(this, superLikersList, this::onMessageClicked);
        rvStarredUsers.setLayoutManager(new LinearLayoutManager(this));
        rvStarredUsers.setAdapter(adapter);
    }

    private void loadSuperLikers() {
        pbStarredLoading.setVisibility(View.VISIBLE);
        llStarredEmpty.setVisibility(View.GONE);
        rvStarredUsers.setVisibility(View.GONE);

        // Query swipes where fromUid == currentUid AND action == "superlike"
        db.collection("swipes")
                .whereEqualTo("fromUid", currentUid)
                .whereEqualTo("action", "superlike")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Set<String> starredUids = new HashSet<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String toUid = doc.getString("toUid");
                        if (toUid != null && !toUid.isEmpty()) {
                            starredUids.add(toUid);
                        }
                    }

                    if (starredUids.isEmpty()) {
                        pbStarredLoading.setVisibility(View.GONE);
                        llStarredEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    // Load profiles for each starred user
                    db.collection("users")
                            .get()
                            .addOnSuccessListener(usersSnapshot -> {
                                superLikersList.clear();
                                for (DocumentSnapshot userDoc : usersSnapshot.getDocuments()) {
                                    if (starredUids.contains(userDoc.getId())) {
                                        superLikersList.add(userDoc);
                                    }
                                }

                                pbStarredLoading.setVisibility(View.GONE);

                                if (superLikersList.isEmpty()) {
                                    llStarredEmpty.setVisibility(View.VISIBLE);
                                } else {
                                    rvStarredUsers.setVisibility(View.VISIBLE);
                                    adapter.notifyDataSetChanged();
                                }
                            })
                            .addOnFailureListener(e -> {
                                pbStarredLoading.setVisibility(View.GONE);
                                llStarredEmpty.setVisibility(View.VISIBLE);
                            });
                })
                .addOnFailureListener(e -> {
                    pbStarredLoading.setVisibility(View.GONE);
                    llStarredEmpty.setVisibility(View.VISIBLE);
                });
    }

    private void onMessageClicked(DocumentSnapshot targetUser) {
        String targetUid = targetUser.getId();
        String targetName = targetUser.getString("name");
        String photoUrl = targetUser.getString("photoUrl");
        if (photoUrl == null || photoUrl.isEmpty()) photoUrl = targetUser.getString("photo");

        Intent intent = new Intent(this, LoveSpaceMessage.class);
        intent.putExtra("userId", targetUid);
        intent.putExtra("username", targetName);
        intent.putExtra("userPhoto", photoUrl);
        startActivity(intent);
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private static class StarredUserAdapter extends RecyclerView.Adapter<StarredUserAdapter.StarredViewHolder> {

        interface OnStarredClickListener {
            void onStarredClick(DocumentSnapshot user);
        }

        private final Context context;
        private final List<DocumentSnapshot> userList;
        private final OnStarredClickListener listener;

        StarredUserAdapter(Context context, List<DocumentSnapshot> userList, OnStarredClickListener listener) {
            this.context = context;
            this.userList = userList;
            this.listener = listener;
        }

        @NonNull
        @Override
        public StarredViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_starred_user, parent, false);
            return new StarredViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull StarredViewHolder holder, int position) {
            DocumentSnapshot user = userList.get(position);

            String name = user.getString("name");
            if (name == null || name.isEmpty()) name = "Anonymous";
            holder.tvName.setText(name);

            String age = user.getString("age");
            String orientation = user.getString("orientation");
            StringBuilder info = new StringBuilder();
            if (age != null && !age.isEmpty()) info.append("📅 Age: ").append(age);
            if (orientation != null && !orientation.isEmpty()) {
                if (info.length() > 0) info.append("  •  ");
                info.append("❤️ ").append(orientation);
            }
            holder.tvInfo.setText(info.length() > 0 ? info.toString() : "Starred Profile ⭐");

            // Photo / Letter Avatar
            String photoUrl = user.getString("photoUrl");
            if (photoUrl == null || photoUrl.isEmpty()) photoUrl = user.getString("photo");

            if (photoUrl != null && !photoUrl.isEmpty() && photoUrl.startsWith("http")) {
                holder.flLetterAvatar.setVisibility(View.GONE);
                holder.ivPhoto.setVisibility(View.VISIBLE);

                Glide.with(context)
                        .load(photoUrl)
                        .centerCrop()
                        .into(holder.ivPhoto);
            } else {
                holder.ivPhoto.setVisibility(View.GONE);
                holder.flLetterAvatar.setVisibility(View.VISIBLE);
                char letter = name.trim().length() > 0 ? Character.toUpperCase(name.trim().charAt(0)) : '?';
                holder.tvAvatarLetter.setText(String.valueOf(letter));
            }

            holder.btnLikeBack.setOnClickListener(v -> {
                if (listener != null) listener.onStarredClick(user);
            });
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }

        static class StarredViewHolder extends RecyclerView.ViewHolder {
            ShapeableImageView ivPhoto;
            FrameLayout flLetterAvatar;
            TextView tvAvatarLetter, tvName, tvInfo;
            MaterialButton btnLikeBack;

            StarredViewHolder(@NonNull View itemView) {
                super(itemView);
                ivPhoto = itemView.findViewById(R.id.ivStarredPhoto);
                flLetterAvatar = itemView.findViewById(R.id.flStarredLetterAvatar);
                tvAvatarLetter = itemView.findViewById(R.id.tvStarredAvatarLetter);
                tvName = itemView.findViewById(R.id.tvStarredName);
                tvInfo = itemView.findViewById(R.id.tvStarredInfo);
                btnLikeBack = itemView.findViewById(R.id.btnLikeBack);
            }
        }
    }
}
