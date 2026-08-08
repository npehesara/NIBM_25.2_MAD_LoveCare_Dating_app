package com.example.lovecare;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    private final OkHttpClient httpClient = new OkHttpClient();

    // ── Views ──────────────────────────────────────────────────────────
    private TextView tvChatUsername, tvHeaderAvatarLetter;
    private ShapeableImageView ivHeaderPartnerPhoto;
    private RecyclerView rvMessages;
    private EditText etChatMessage;
    private FloatingActionButton fabSend;
    private ProgressBar pbImageUpload;

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
        String chatId = myUid.compareTo(partnerUid) < 0 ? myUid + "_" + partnerUid : partnerUid + "_" + myUid;
        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("messages");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.messageRoot), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        initViews();
        setupHeader();
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

        View btnBack = findViewById(R.id.btnChatBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnOptions = findViewById(R.id.btnChatOptions);
        if (btnOptions != null) btnOptions.setOnClickListener(v ->
                Toast.makeText(this, "Chat Options", Toast.LENGTH_SHORT).show());
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

    // ── Cloudinary upload ─────────────────────────────────────────────

    private void uploadImageToCloudinary(Uri imageUri) {
        if (pbImageUpload != null) pbImageUpload.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Uploading image…", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(imageUri);
                if (is == null) {
                    runOnUiThread(() -> {
                        if (pbImageUpload != null) pbImageUpload.setVisibility(View.GONE);
                        Toast.makeText(this, "Cannot read image", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                byte[] bytes = readAllBytes(is);
                is.close();

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

    private static byte[] readAllBytes(InputStream is) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = is.read(chunk)) != -1) buffer.write(chunk, 0, n);
        return buffer.toByteArray();
    }

    private static String sha1Hex(String input) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(input.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}