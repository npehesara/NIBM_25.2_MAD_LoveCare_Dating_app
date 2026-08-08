package com.example.lovecare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Profile extends AppCompatActivity {

    private ImageView ivProfilePicture;
    private TextView tvProfileName, tvProfileBio;
    private View flProfileLetterAvatar;
    private TextView tvProfileAvatarLetter;


    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(Profile.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        View root = findViewById(R.id.main);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);

        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        tvProfileName    = findViewById(R.id.tvProfileName);
        tvProfileBio     = findViewById(R.id.tvProfileBio);
        flProfileLetterAvatar = findViewById(R.id.flProfileLetterAvatar);
        tvProfileAvatarLetter = findViewById(R.id.tvProfileAvatarLetter);


        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            if (bottomNav != null) {
                bottomNav.setPadding(0, 0, 0, systemBars.bottom);
            }
            return insets;
        });

        // Profile loading handled in onResume to sync edits

        // Logout functionality
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                mAuth.signOut();
                Toast.makeText(Profile.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Profile.this, Login.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // Edit Profile functionality
        MaterialButton btnEditProfile = findViewById(R.id.btnEditProfile);
        com.google.android.material.floatingactionbutton.FloatingActionButton fabEditPhoto = findViewById(R.id.fabEditPhoto);
        View.OnClickListener editClickListener = v -> {
            Intent intent = new Intent(Profile.this, EditProfileActivity.class);
            startActivity(intent);
        };
        if (btnEditProfile != null) btnEditProfile.setOnClickListener(editClickListener);
        if (fabEditPhoto != null) fabEditPhoto.setOnClickListener(editClickListener);

        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_profile);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                Intent intent = null;

                if (itemId == R.id.nav_profile) {
                    return true;
                } else if (itemId == R.id.nav_swipe) {
                    intent = new Intent(this, HomeActivity.class);
                } else if (itemId == R.id.nav_starred) {
                    intent = new Intent(this, StarredActivity.class);
                } else if (itemId == R.id.nav_likes) {
                    intent = new Intent(this, LoveSpaceActivity.class);
                } else if (itemId == R.id.nav_chat) {
                    intent = new Intent(this, UserChatActivity.class);
                }

                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.google.firebase.auth.FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            loadUserProfile(user.getUid());
        }
    }

    private void loadUserProfile(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Display name & age
                        String name = documentSnapshot.getString("name");
                        if (name == null || name.isEmpty()) name = "User";

                        String age = documentSnapshot.getString("age");
                        if (age != null && !age.isEmpty()) {
                            tvProfileName.setText(name + ", " + age);
                        } else {
                            tvProfileName.setText(name);
                        }

                        // Display bio summary based on questionnaire
                        String goal        = documentSnapshot.getString("goal");
                        String orientation = documentSnapshot.getString("orientation");
                        String religion    = documentSnapshot.getString("religion");
                        String height      = documentSnapshot.getString("height");
                        String starSign    = documentSnapshot.getString("starSign");
                        String education   = documentSnapshot.getString("education");

                        StringBuilder bio = new StringBuilder();
                        if (goal        != null && !goal.isEmpty())        bio.append("🎯 Goal: ").append(goal).append("\n");
                        if (orientation != null && !orientation.isEmpty()) bio.append("❤️ Orientation: ").append(orientation).append("\n");
                        if (religion    != null && !religion.isEmpty())    bio.append("🙏 Religion: ").append(religion).append("\n");
                        if (height      != null && !height.isEmpty())      bio.append("📏 Height: ").append(height).append("\n");
                        if (starSign    != null && !starSign.isEmpty())    bio.append("⭐ Star Sign: ").append(starSign).append("\n");
                        if (education   != null && !education.isEmpty())   bio.append("🎓 Education: ").append(education);

                        String bioText = bio.toString().trim();
                        tvProfileBio.setText(bioText.isEmpty() ? "No details provided yet." : bioText);

                        // Load profile picture
                        String photoUrl = documentSnapshot.getString("photoUrl");
                        if (photoUrl == null || photoUrl.isEmpty()) photoUrl = documentSnapshot.getString("photo");

                        if (photoUrl != null && !photoUrl.isEmpty() && photoUrl.startsWith("http")) {
                            flProfileLetterAvatar.setVisibility(View.VISIBLE);
                            ivProfilePicture.setVisibility(View.INVISIBLE);
                            char firstLetter = name.trim().length() > 0 ? Character.toUpperCase(name.trim().charAt(0)) : '?';
                            tvProfileAvatarLetter.setText(String.valueOf(firstLetter));

                            Glide.with(Profile.this)
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
                                            flProfileLetterAvatar.setVisibility(View.GONE);
                                            ivProfilePicture.setVisibility(View.VISIBLE);
                                            return false;
                                        }
                                    })
                                    .into(ivProfilePicture);
                        } else {
                            ivProfilePicture.setVisibility(View.GONE);
                            flProfileLetterAvatar.setVisibility(View.VISIBLE);
                            char firstLetter = name.trim().length() > 0 ? Character.toUpperCase(name.trim().charAt(0)) : '?';
                            tvProfileAvatarLetter.setText(String.valueOf(firstLetter));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Profile.this, "Failed to load profile details", Toast.LENGTH_SHORT).show();
                });
    }
}
