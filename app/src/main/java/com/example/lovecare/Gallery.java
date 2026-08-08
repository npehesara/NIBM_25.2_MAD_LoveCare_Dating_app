package com.example.lovecare;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Gallery extends AppCompatActivity {

    private RecyclerView rvGallery;
    private ProgressBar pbGalleryLoading;
    private LinearLayout llGalleryEmpty;
    private TextView tvPhotoCount;

    private GalleryPhotoAdapter photoAdapter;
    private List<String> photoUrls = new ArrayList<>();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String myUid;
    private String partnerUid;
    private String chatId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gallery);

        View root = findViewById(R.id.galleryRoot);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        View btnBack = findViewById(R.id.btnGalleryBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        rvGallery = findViewById(R.id.rvGallery);
        pbGalleryLoading = findViewById(R.id.pbGalleryLoading);
        llGalleryEmpty = findViewById(R.id.llGalleryEmpty);
        tvPhotoCount = findViewById(R.id.tvPhotoCount);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }
        myUid = mAuth.getCurrentUser().getUid();

        partnerUid = getIntent().getStringExtra("partnerUid");

        setupRecyclerView();

        if (partnerUid != null && !partnerUid.isEmpty()) {
            loadChatPhotos();
        } else {
            findActiveLoveSpaceAndLoad();
        }
    }

    private void setupRecyclerView() {
        photoAdapter = new GalleryPhotoAdapter(this, photoUrls, this::showFullScreenPhoto);
        rvGallery.setLayoutManager(new GridLayoutManager(this, 3));
        rvGallery.setAdapter(photoAdapter);
    }

    private void findActiveLoveSpaceAndLoad() {
        db.collection("lovespaces")
                .whereEqualTo("user1Uid", myUid)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snap1 -> {
                    if (!snap1.isEmpty()) {
                        DocumentSnapshot doc = snap1.getDocuments().get(0);
                        partnerUid = doc.getString("user2Uid");
                        loadChatPhotos();
                        return;
                    }
                    db.collection("lovespaces")
                            .whereEqualTo("user2Uid", myUid)
                            .whereEqualTo("active", true)
                            .get()
                            .addOnSuccessListener(snap2 -> {
                                if (!snap2.isEmpty()) {
                                    DocumentSnapshot doc = snap2.getDocuments().get(0);
                                    partnerUid = doc.getString("user1Uid");
                                    loadChatPhotos();
                                } else {
                                    showEmptyState();
                                }
                            })
                            .addOnFailureListener(e -> showEmptyState());
                })
                .addOnFailureListener(e -> showEmptyState());
    }

    private void loadChatPhotos() {
        if (partnerUid == null || partnerUid.isEmpty()) {
            showEmptyState();
            return;
        }

        // Build deterministic chatId
        chatId = myUid.compareTo(partnerUid) < 0 ? myUid + "_" + partnerUid : partnerUid + "_" + myUid;

        DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                .getReference("chats")
                .child(chatId)
                .child("messages");

        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                photoUrls.clear();
                for (DataSnapshot msgSnap : snapshot.getChildren()) {
                    String imageUrl = msgSnap.child("imageUrl").getValue(String.class);
                    if (imageUrl != null && !imageUrl.isEmpty() && imageUrl.startsWith("http")) {
                        photoUrls.add(imageUrl);
                    }
                }

                // Show latest photos first
                Collections.reverse(photoUrls);

                pbGalleryLoading.setVisibility(View.GONE);

                if (photoUrls.isEmpty()) {
                    showEmptyState();
                } else {
                    showPhotos();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                pbGalleryLoading.setVisibility(View.GONE);
                Toast.makeText(Gallery.this, "Failed to load gallery", Toast.LENGTH_SHORT).show();
                if (photoUrls.isEmpty()) showEmptyState();
            }
        });
    }

    private void showPhotos() {
        pbGalleryLoading.setVisibility(View.GONE);
        llGalleryEmpty.setVisibility(View.GONE);
        rvGallery.setVisibility(View.VISIBLE);
        tvPhotoCount.setText(photoUrls.size() + (photoUrls.size() == 1 ? " photo" : " photos"));
        photoAdapter.notifyDataSetChanged();
    }

    private void showEmptyState() {
        pbGalleryLoading.setVisibility(View.GONE);
        rvGallery.setVisibility(View.GONE);
        llGalleryEmpty.setVisibility(View.VISIBLE);
        tvPhotoCount.setText("0 photos");
    }

    private void showFullScreenPhoto(String url) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_fullscreen_photo);

        ImageView ivFull = dialog.findViewById(R.id.ivFullscreenPhoto);
        View btnClose = dialog.findViewById(R.id.btnCloseFullscreen);

        if (ivFull != null) {
            Glide.with(this)
                    .load(url)
                    .fitCenter()
                    .into(ivFull);
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private static class GalleryPhotoAdapter extends RecyclerView.Adapter<GalleryPhotoAdapter.PhotoViewHolder> {

        public interface OnPhotoClickListener {
            void onPhotoClick(String url);
        }

        private final Context context;
        private final List<String> urls;
        private final OnPhotoClickListener listener;

        GalleryPhotoAdapter(Context context, List<String> urls, OnPhotoClickListener listener) {
            this.context = context;
            this.urls = urls;
            this.listener = listener;
        }

        @NonNull
        @Override
        public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_gallery_photo, parent, false);
            return new PhotoViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
            String url = urls.get(position);
            Glide.with(context)
                    .load(url)
                    .centerCrop()
                    .into(holder.ivPhoto);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onPhotoClick(url);
            });
        }

        @Override
        public int getItemCount() {
            return urls.size();
        }

        static class PhotoViewHolder extends RecyclerView.ViewHolder {
            ImageView ivPhoto;

            PhotoViewHolder(@NonNull View itemView) {
                super(itemView);
                ivPhoto = itemView.findViewById(R.id.ivGalleryPhoto);
            }
        }
    }
}