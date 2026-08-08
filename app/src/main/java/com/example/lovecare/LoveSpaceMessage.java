package com.example.lovecare;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoveSpaceMessage extends AppCompatActivity {

    // ── Cloudinary credentials ─────────────────────────────────────────
    private static final String CLOUDINARY_CLOUD_NAME = "dycgmngs";
    private static final String CLOUDINARY_API_KEY    = "799321769722587";
    private static final String CLOUDINARY_API_SECRET = "VawFzAcil8i3AJQXdr0JRVDrJ7Q";
    private static final String CLOUDINARY_UPLOAD_URL =
            "https://api.cloudinary.com/v1_1/" + CLOUDINARY_CLOUD_NAME + "/image/upload";
    private static final String CHAT_FOLDER = "lovecare_chat";

    // Increased OkHttp timeouts to 60 seconds to prevent upload timeouts
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    // ── Views ──────────────────────────────────────────────────────────
    private TextView tvChatUsername, tvHeaderAvatarLetter;
    private ShapeableImageView ivHeaderPartnerPhoto;
    private RecyclerView rvMessages;
    private EditText etChatMessage;
    private FloatingActionButton fabSend;
    private ProgressBar pbImageUpload;
    private ImageView ivChatWallpaper;
    private View vWallpaperOverlay;

    // ── Data ───────────────────────────────────────────────────────────
    private ChatMessageAdapter messageAdapter;
    private List<ChatMessage> messageList;
    private String partnerName = "?";
    private String partnerPhoto = null;

    // ── Firebase ───────────────────────────────────────────────────────
    private FirebaseAuth mAuth;
    private DatabaseReference chatRef;
    private String myUid;
    private String partnerUid;
    private String chatId;

    // ── Camera state ───────────────────────────────────────────────────
    private Uri cameraImageUri;

    // ── Activity result launchers ──────────────────────────────────────
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadImageToCloudinary(uri);
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraImageUri != null) uploadImageToCloudinary(cameraImageUri);
            });

    private final ActivityResultLauncher<String> wallpaperPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) saveAndApplyWallpaper(uri);
            });

    private final ActivityResultLauncher<String[]> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean camera = result.getOrDefault(Manifest.permission.CAMERA, false);
                if (camera != null && camera) openCamera();
                else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<String[]> galleryPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean storage = result.getOrDefault(
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                                ? Manifest.permission.READ_MEDIA_IMAGES
                                : Manifest.permission.READ_EXTERNAL_STORAGE, false);
                if (storage != null && storage) galleryLauncher.launch("image/*");
                else Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_love_space_message);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) { finish(); return; }
        myUid = mAuth.getCurrentUser().getUid();

        partnerUid = getIntent().getStringExtra("userId");
        if (partnerUid == null || partnerUid.isEmpty()) {
            Toast.makeText(this, "Invalid partner details", Toast.LENGTH_SHORT).show();
            finish(); return;
        }

        // Intent extras
        String intentName = getIntent().getStringExtra("username");
        if (intentName != null && !intentName.isEmpty()) partnerName = intentName;
        partnerPhoto = getIntent().getStringExtra("userPhoto");

        // Build deterministic chatId
        chatId = myUid.compareTo(partnerUid) < 0 ? myUid + "_" + partnerUid : partnerUid + "_" + myUid;
        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("messages");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.messageRoot), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomPadding = Math.max(sb.bottom, ime.bottom);
            v.setPadding(sb.left, sb.top, sb.right, bottomPadding);

            if (ime.bottom > 0 && messageAdapter != null && messageAdapter.getItemCount() > 0) {
                rvMessages.postDelayed(() -> rvMessages.smoothScrollToPosition(messageAdapter.getItemCount() - 1), 100);
            }
            return insets;
        });

        initViews();
        setupHeader();
        setupWallpaper();
        setupMessageList();
        setupSendButton();
        setupAttachmentButtons();
    }

    // ── Init ──────────────────────────────────────────────────────────

    private void initViews() {
        tvChatUsername        = findViewById(R.id.tvChatUsername);
        tvHeaderAvatarLetter  = findViewById(R.id.tvHeaderAvatarLetter);
        ivHeaderPartnerPhoto  = findViewById(R.id.ivHeaderPartnerPhoto);
        rvMessages            = findViewById(R.id.rvMessages);
        etChatMessage         = findViewById(R.id.etChatMessage);
        fabSend               = findViewById(R.id.fabSend);
        pbImageUpload         = findViewById(R.id.pbImageUpload);
        ivChatWallpaper       = findViewById(R.id.ivChatWallpaper);
        vWallpaperOverlay     = findViewById(R.id.vWallpaperOverlay);

        View btnBack = findViewById(R.id.btnChatBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnOptions = findViewById(R.id.btnChatOptions);
        if (btnOptions != null) btnOptions.setOnClickListener(this::showChatOptionsMenu);

        if (etChatMessage != null) {
            etChatMessage.setOnClickListener(v -> scrollToBottom());
            etChatMessage.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) scrollToBottom();
            });
        }
    }

    private void scrollToBottom() {
        if (messageAdapter != null && messageAdapter.getItemCount() > 0 && rvMessages != null) {
            rvMessages.postDelayed(() -> rvMessages.smoothScrollToPosition(messageAdapter.getItemCount() - 1), 150);
        }
    }

    // ── Wallpaper Management ───────────────────────────────────────────

    private void setupWallpaper() {
        File wallpaperFile = getWallpaperFile();
        if (wallpaperFile.exists()) {
            displayWallpaper(wallpaperFile.getAbsolutePath());
        } else {
            if (ivChatWallpaper != null) ivChatWallpaper.setVisibility(View.GONE);
            if (vWallpaperOverlay != null) vWallpaperOverlay.setVisibility(View.GONE);
        }
    }

    private File getWallpaperFile() {
        return new File(getFilesDir(), "chat_wallpaper_" + chatId + ".jpg");
    }

    private void saveAndApplyWallpaper(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return;
            File wallpaperFile = getWallpaperFile();
            FileOutputStream fos = new FileOutputStream(wallpaperFile);
            byte[] buffer = new byte[8192];
            int count;
            while ((count = is.read(buffer)) != -1) {
                fos.write(buffer, 0, count);
            }
            fos.close();
            is.close();

            displayWallpaper(wallpaperFile.getAbsolutePath());
            Toast.makeText(this, "Chat wallpaper set!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to set wallpaper: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void displayWallpaper(String filePath) {
        if (ivChatWallpaper != null) {
            ivChatWallpaper.setVisibility(View.VISIBLE);
            Glide.with(this).load(filePath).centerCrop().into(ivChatWallpaper);
        }
        if (vWallpaperOverlay != null) {
            vWallpaperOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void removeWallpaper() {
        File wallpaperFile = getWallpaperFile();
        if (wallpaperFile.exists()) {
            wallpaperFile.delete();
        }
        if (ivChatWallpaper != null) ivChatWallpaper.setVisibility(View.GONE);
        if (vWallpaperOverlay != null) vWallpaperOverlay.setVisibility(View.GONE);
        Toast.makeText(this, "Wallpaper removed", Toast.LENGTH_SHORT).show();
    }

    private void showChatOptionsMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        boolean hasWallpaper = getWallpaperFile().exists();
        if (hasWallpaper) {
            popup.getMenu().add(0, 1, 0, "Change Chat Wallpaper");
            popup.getMenu().add(0, 2, 1, "Remove Wallpaper");
        } else {
            popup.getMenu().add(0, 1, 0, "Set Chat Wallpaper");
        }

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                wallpaperPickerLauncher.launch("image/*");
                return true;
            } else if (item.getItemId() == 2) {
                removeWallpaper();
                return true;
            }
            return false;
        });
        popup.show();
    }

    // ── Header setup ──────────────────────────────────────────────────

    private void setupHeader() {
        // Set letter avatar immediately
        char letter = partnerName.trim().length() > 0
                ? Character.toUpperCase(partnerName.trim().charAt(0)) : '?';
        if (tvHeaderAvatarLetter != null) tvHeaderAvatarLetter.setText(String.valueOf(letter));
        if (tvChatUsername != null) tvChatUsername.setText(partnerName);

        // Load partner photo if available
        if (partnerPhoto != null && !partnerPhoto.isEmpty() && partnerPhoto.startsWith("http")) {
            if (ivHeaderPartnerPhoto != null) {
                Glide.with(this)
                        .load(partnerPhoto)
                        .centerCrop()
                        .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e,
                                    Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                    boolean isFirstResource) { return false; }

                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                                    Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                    com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                if (tvHeaderAvatarLetter != null)
                                    tvHeaderAvatarLetter.setVisibility(View.GONE);
                                if (ivHeaderPartnerPhoto != null)
                                    ivHeaderPartnerPhoto.setVisibility(View.VISIBLE);
                                return false;
                            }
                        })
                        .into(ivHeaderPartnerPhoto);
            }
        }

        // If name wasn't in intent, fetch from Firestore
        if ("?".equals(partnerName) || partnerName.isEmpty()) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(partnerUid).get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            String name = userDoc.getString("name");
                            if (name != null && !name.isEmpty()) {
                                partnerName = name;
                                char l = Character.toUpperCase(name.trim().charAt(0));
                                if (tvChatUsername != null) tvChatUsername.setText(name);
                                if (tvHeaderAvatarLetter != null)
                                    tvHeaderAvatarLetter.setText(String.valueOf(l));
                                // Refresh adapter with updated name
                                if (messageAdapter != null) {
                                    messageAdapter.setPartnerName(name);
                                    messageAdapter.notifyDataSetChanged();
                                }
                            }
                        }
                    });
        }
    }

    // ── Message list ──────────────────────────────────────────────────

    private void setupMessageList() {
        messageList = new ArrayList<>();
        messageAdapter = new ChatMessageAdapter(this, messageList, partnerName);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(messageAdapter);

        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot msgSnap : snapshot.getChildren()) {
                    String senderId = msgSnap.child("senderId").getValue(String.class);
                    String text = msgSnap.child("messageText").getValue(String.class);
                    if (text == null) text = msgSnap.child("text").getValue(String.class);
                    String time = msgSnap.child("time").getValue(String.class);
                    String imageUrl = msgSnap.child("imageUrl").getValue(String.class);

                    if (senderId == null) continue;
                    boolean isSentByMe = senderId.equals(myUid);

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        messageList.add(new ChatMessage(null, time != null ? time : "", isSentByMe, imageUrl));
                    } else if (text != null && !text.isEmpty()) {
                        messageList.add(new ChatMessage(text, time != null ? time : "", isSentByMe));
                    }
                }
                messageAdapter.notifyDataSetChanged();
                if (!messageList.isEmpty())
                    rvMessages.smoothScrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoveSpaceMessage.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Send text ─────────────────────────────────────────────────────

    private void setupSendButton() {
        if (fabSend == null || etChatMessage == null) return;
        fabSend.setOnClickListener(v -> {
            String text = etChatMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                String currentTime = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
                Map<String, Object> msg = new HashMap<>();
                msg.put("senderId", myUid);
                msg.put("receiverId", partnerUid);
                msg.put("messageText", text);
                msg.put("timestamp", System.currentTimeMillis());
                msg.put("time", currentTime);
                chatRef.push().setValue(msg)
                        .addOnFailureListener(e -> Toast.makeText(LoveSpaceMessage.this,
                                "Send failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                etChatMessage.setText("");
            }
        });
    }

    // ── Attachment buttons ────────────────────────────────────────────

    private void setupAttachmentButtons() {
        View btnGallery = findViewById(R.id.btnAttachGallery);
        if (btnGallery != null) btnGallery.setOnClickListener(v -> requestGallery());

        View btnCamera = findViewById(R.id.btnAttachCamera);
        if (btnCamera != null) btnCamera.setOnClickListener(v -> requestCamera());
    }

    private void requestGallery() {
        String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            galleryLauncher.launch("image/*");
        } else {
            galleryPermissionLauncher.launch(new String[]{perm});
        }
    }

    private void requestCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            cameraPermissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
        }
    }

    private void openCamera() {
        try {
            File imageFile = File.createTempFile("chat_img_", ".jpg",
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            cameraImageUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider", imageFile);
            cameraLauncher.launch(cameraImageUri);
        } catch (IOException e) {
            Toast.makeText(this, "Cannot open camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ── Cloudinary upload with Image Compression ───────────────────────

    private void uploadImageToCloudinary(Uri imageUri) {
        if (pbImageUpload != null) pbImageUpload.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Uploading image…", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                // Compress & downscale image before uploading to prevent timeouts
                byte[] bytes = getCompressedImageBytes(imageUri);
                if (bytes == null || bytes.length == 0) {
                    runOnUiThread(() -> {
                        if (pbImageUpload != null) pbImageUpload.setVisibility(View.GONE);
                        Toast.makeText(this, "Cannot process image", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                long timestamp = System.currentTimeMillis() / 1000L;
                String toSign = "folder=" + CHAT_FOLDER + "&timestamp=" + timestamp + CLOUDINARY_API_SECRET;
                String signature = sha1Hex(toSign);

                RequestBody body = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", "chat.jpg",
                                RequestBody.create(bytes, MediaType.parse("image/jpeg")))
                        .addFormDataPart("api_key", CLOUDINARY_API_KEY)
                        .addFormDataPart("timestamp", String.valueOf(timestamp))
                        .addFormDataPart("folder", CHAT_FOLDER)
                        .addFormDataPart("signature", signature)
                        .build();

                Request request = new Request.Builder()
                        .url(CLOUDINARY_UPLOAD_URL)
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    String respBody = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        if (pbImageUpload != null) pbImageUpload.setVisibility(View.GONE);
                        try {
                            JSONObject json = new JSONObject(respBody);
                            if (json.has("secure_url")) {
                                sendImageMessage(json.getString("secure_url"));
                            } else {
                                String err = json.has("error")
                                        ? json.getJSONObject("error").optString("message", "Unknown")
                                        : "No URL in response";
                                Toast.makeText(this, "Upload failed: " + err, Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "Upload error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (pbImageUpload != null) pbImageUpload.setVisibility(View.GONE);
                    Toast.makeText(this, "Upload error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private byte[] getCompressedImageBytes(Uri uri) {
        try {
            InputStream input = getContentResolver().openInputStream(uri);
            if (input == null) return null;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);
            input.close();

            int originalWidth = options.outWidth;
            int originalHeight = options.outHeight;
            int maxDimension = 1024;

            int inSampleSize = 1;
            if (originalWidth > maxDimension || originalHeight > maxDimension) {
                final int halfWidth = originalWidth / 2;
                final int halfHeight = originalHeight / 2;
                while ((halfWidth / inSampleSize) >= maxDimension && (halfHeight / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2;
                }
            }

            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = inSampleSize;

            input = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, decodeOptions);
            if (input != null) input.close();

            if (bitmap == null) return null;

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            bitmap.recycle();
            return baos.toByteArray();
        } catch (Exception e) {
            android.util.Log.e("ImageCompression", "Error compressing image: " + e.getMessage());
            return null;
        }
    }

    private void sendImageMessage(String imageUrl) {
        String currentTime = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", myUid);
        msg.put("receiverId", partnerUid);
        msg.put("imageUrl", imageUrl);
        msg.put("messageText", "");
        msg.put("timestamp", System.currentTimeMillis());
        msg.put("time", currentTime);
        chatRef.push().setValue(msg)
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Send failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private static String sha1Hex(String input) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(input.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}