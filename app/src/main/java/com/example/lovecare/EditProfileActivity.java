package com.example.lovecare;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditProfileActivity extends AppCompatActivity {

    // ── Cloudinary config (Same details) ────────────────────────────────────
    private static final String CLOUDINARY_CLOUD_NAME = "dycgmngs";
    private static final String CLOUDINARY_API_KEY    = "799321769722587";
    private static final String CLOUDINARY_API_SECRET = "VawFzAcil8i3AJQXdr0JRVDrJ7Q";
    private static final String CLOUDINARY_UPLOAD_URL =
            "https://api.cloudinary.com/v1_1/" + CLOUDINARY_CLOUD_NAME + "/image/upload";

    // ── Views ────────────────────────────────────────────────────────────────
    private ImageView ivEditProfilePicture;
    private TextInputEditText etEditName, etEditAge, etEditHeight, etEditWeight;
    private AutoCompleteTextView actvGender, actvLookingFor, actvOrientation, actvGoal, actvStarSign, actvEducation, actvReligion;

    // ── Firebase / Utils ──────────────────────────────────────────────────────
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private OkHttpClient httpClient;
    private Uri selectedPhotoUri = null;
    private String existingPhotoUrl = null;

    // ── Image Pickers ────────────────────────────────────────────────────────
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                selectedPhotoUri = uri;
                Glide.with(this).load(uri).centerCrop().into(ivEditProfilePicture);
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) openPhotoPicker();
                else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();
        httpClient = new OkHttpClient();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rlHeader).getParent() != null ? (View) findViewById(R.id.rlHeader).getParent() : findViewById(android.R.id.content), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        // Bind Views
        ivEditProfilePicture = findViewById(R.id.ivEditProfilePicture);
        etEditName           = findViewById(R.id.etEditName);
        etEditAge            = findViewById(R.id.etEditAge);
        etEditHeight         = findViewById(R.id.etEditHeight);
        etEditWeight         = findViewById(R.id.etEditWeight);

        actvGender      = findViewById(R.id.actvGender);
        actvLookingFor  = findViewById(R.id.actvLookingFor);
        actvOrientation = findViewById(R.id.actvOrientation);
        actvGoal        = findViewById(R.id.actvGoal);
        actvStarSign    = findViewById(R.id.actvStarSign);
        actvEducation   = findViewById(R.id.actvEducation);
        actvReligion    = findViewById(R.id.actvReligion);

        // Bind Buttons
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveProfileData());
        findViewById(R.id.flPhotoContainer).setOnClickListener(v -> requestPhotoPermission());

        setupDropdownAdapters();
        loadCurrentProfileData();
    }

    private void setupDropdownAdapters() {
        setDropdown(actvGender,      "Men", "Women", "Other");
        setDropdown(actvLookingFor,  "Men", "Women", "Both");
        setDropdown(actvOrientation, "Straight", "Gay", "Lesbian", "Bisexual", "Asexual");
        setDropdown(actvGoal,        "Get married", "Find a relationship", "Chat and meet friends", "Learn other cultures", "Travel the world");
        setDropdown(actvStarSign,    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Other");
        setDropdown(actvEducation,   "High school", "Undergraduate", "Postgraduate", "PhD candidate");
        setDropdown(actvReligion,    "Christian", "Buddhist", "Hindu", "Muslim", "Spiritual", "Atheist", "Other");
    }

    private void setDropdown(AutoCompleteTextView view, String... options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, options);
        view.setAdapter(adapter);
    }

    private void loadCurrentProfileData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        etEditName.setText(doc.getString("name"));
                        etEditAge.setText(doc.getString("age"));
                        etEditHeight.setText(doc.getString("height"));
                        etEditWeight.setText(doc.getString("weight"));

                        actvGender.setText(doc.getString("gender"), false);
                        actvLookingFor.setText(doc.getString("lookingFor"), false);
                        actvOrientation.setText(doc.getString("orientation"), false);
                        actvGoal.setText(doc.getString("goal"), false);
                        actvStarSign.setText(doc.getString("starSign"), false);
                        actvEducation.setText(doc.getString("education"), false);
                        actvReligion.setText(doc.getString("religion"), false);

                        existingPhotoUrl = doc.getString("photoUrl");
                        if (existingPhotoUrl == null || existingPhotoUrl.isEmpty()) {
                            existingPhotoUrl = doc.getString("photo");
                        }
                        if (existingPhotoUrl != null && !existingPhotoUrl.isEmpty()) {
                            Glide.with(this).load(existingPhotoUrl).centerCrop().into(ivEditProfilePicture);
                        }
                    }
                });
    }

    // ── Photo Picker ─────────────────────────────────────────────────────────

    private void requestPhotoPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                openPhotoPicker();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                openPhotoPicker();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void openPhotoPicker() {
        imagePickerLauncher.launch("image/*");
    }

    // ── Save profile data ─────────────────────────────────────────────────────

    private void saveProfileData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String name = etEditName.getText().toString().trim();
        if (name.isEmpty()) {
            etEditName.setError("Name is required");
            return;
        }

        Toast.makeText(this, "Saving changes...", Toast.LENGTH_SHORT).show();

        if (selectedPhotoUri != null) {
            uploadPhotoToCloudinary(selectedPhotoUri);
        } else {
            saveToFirestore(existingPhotoUrl);
        }
    }

    private void uploadPhotoToCloudinary(Uri fileUri) {
        try {
            InputStream is = getContentResolver().openInputStream(fileUri);
            if (is == null) throw new IOException("InputStream is null");
            byte[] fileBytes = readBytes(is);

            long timestamp = System.currentTimeMillis() / 1000L;
            String signatureInput = "timestamp=" + timestamp + CLOUDINARY_API_SECRET;
            String signature = sha1(signatureInput);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "image.jpg", RequestBody.create(okhttp3.MediaType.parse("image/jpeg"), fileBytes))
                    .addFormDataPart("api_key", CLOUDINARY_API_KEY)
                    .addFormDataPart("timestamp", String.valueOf(timestamp))
                    .addFormDataPart("signature", signature)
                    .build();

            Request request = new Request.Builder()
                    .url(CLOUDINARY_UPLOAD_URL)
                    .post(requestBody)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "Photo upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String resString = response.body().string();
                            JSONObject json = new JSONObject(resString);
                            String uploadedUrl = json.getString("secure_url");
                            runOnUiThread(() -> saveToFirestore(uploadedUrl));
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "JSON parse error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    } else {
                        runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "Cloudinary upload error code: " + response.code(), Toast.LENGTH_LONG).show());
                    }
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "File read error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveToFirestore(String photoUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("name",           etEditName.getText().toString().trim());
        updates.put("age",            etEditAge.getText().toString().trim());
        updates.put("height",         etEditHeight.getText().toString().trim());
        updates.put("weight",         etEditWeight.getText().toString().trim());
        updates.put("gender",         actvGender.getText().toString().trim());
        updates.put("lookingFor",     actvLookingFor.getText().toString().trim());
        updates.put("orientation",    actvOrientation.getText().toString().trim());
        updates.put("goal",           actvGoal.getText().toString().trim());
        updates.put("starSign",       actvStarSign.getText().toString().trim());
        updates.put("education",      actvEducation.getText().toString().trim());
        updates.put("religion",       actvReligion.getText().toString().trim());

        if (photoUrl != null && !photoUrl.isEmpty()) {
            updates.put("photoUrl", photoUrl);
            updates.put("photo",    photoUrl);
        }

        db.collection("users").document(user.getUid()).set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully! 💕", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ── Utils ────────────────────────────────────────────────────────────────

    private static byte[] readBytes(InputStream is) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    private static String sha1(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
