package com.example.lovecare;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class QuestionnaireActivity extends AppCompatActivity {

    // ── Cloudinary credentials ──────────────────────────────────────────────
    private static final String CLOUDINARY_CLOUD_NAME = "dycgmngs";
    private static final String CLOUDINARY_API_KEY    = "799321769722587";
    private static final String CLOUDINARY_API_SECRET = "VawFzAcil8i3AJQXdr0JRVDrJ7Q";
    private static final String CLOUDINARY_UPLOAD_URL =
            "https://api.cloudinary.com/v1_1/" + CLOUDINARY_CLOUD_NAME + "/image/upload";

    // ── Subtitles for each question key ────────────────────────────────────
    private static final Map<String, String> SUBTITLES = new HashMap<>();
    static {
        SUBTITLES.put("gender",        "Tell us who you are");
        SUBTITLES.put("lookingFor",    "Who do you want to meet?");
        SUBTITLES.put("age",           "How old are you?");
        SUBTITLES.put("goal",          "You can change your goal at any time");
        SUBTITLES.put("height",        "Enter your height below");
        SUBTITLES.put("weight",        "Enter your weight below");
        SUBTITLES.put("orientation",   "Be yourself – all orientations are welcome");
        SUBTITLES.put("starSign",      "What's written in the stars?");
        SUBTITLES.put("smoke",         "Be honest, it matters to some people");
        SUBTITLES.put("education",     "What is your highest qualification?");
        SUBTITLES.put("drink",         "Be honest, it matters to some people");
        SUBTITLES.put("mostImportant", "What drives you every day?");
        SUBTITLES.put("religion",      "Faith is an important part of who you are");
        SUBTITLES.put("photo",         "Matches want to see who they are talking to!");
    }

    // ── Option subtitles for goal step ─────────────────────────────────────
    private static final Map<String, String> GOAL_DESCRIPTIONS = new HashMap<>();
    static {
        GOAL_DESCRIPTIONS.put("Get married",           "Find a person to build a joint future");
        GOAL_DESCRIPTIONS.put("Find a relationship",   "Meet a soulmate, start a relationship");
        GOAL_DESCRIPTIONS.put("Chat and meet friends", "Chat with people without serious plans");
        GOAL_DESCRIPTIONS.put("Learn other cultures",  "Learn something new about the world");
        GOAL_DESCRIPTIONS.put("Travel the world",      "Meet travel partners and share experiences");
    }

    // ── Views ───────────────────────────────────────────────────────────────
    private TextView tvQuestionTitle, tvQuestionSubtitle, tvSkip;
    private ProgressBar progressBar;
    private LinearLayout llOptions, llTextInput, llPhotoUpload;
    private EditText etTextInput;
    private AppCompatButton btnNextText, btnCompleteRegistration;
    private FrameLayout flPhotoFrame;
    private ImageView ivSelectedPhoto;
    private TextView tvPlusIcon;

    // ── State ───────────────────────────────────────────────────────────────
    private int currentStep = 0;
    private List<QuestionStep> steps;
    private Map<String, String> answers;
    private Uri selectedPhotoUri;
    private int selectedOptionIndex = -1;

    // ── Firebase / HTTP ─────────────────────────────────────────────────────
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private OkHttpClient httpClient;

    // ── Activity Result launchers ───────────────────────────────────────────
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                // Guard against MIUI null bundle crash
                if (uri == null) return;
                try {
                    selectedPhotoUri = uri;
                    ivSelectedPhoto.setVisibility(View.VISIBLE);
                    tvPlusIcon.setVisibility(View.GONE);
                    Glide.with(this).load(uri).centerCrop().into(ivSelectedPhoto);
                    answers.put("photo", uri.toString());
                    btnCompleteRegistration.setEnabled(true);
                } catch (Exception e) {
                    android.util.Log.e("Questionnaire", "Photo picker result error", e);
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) openPhotoPicker();
                else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainQuestionnaire), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        mAuth      = FirebaseAuth.getInstance();
        db         = FirebaseFirestore.getInstance();
        httpClient = new OkHttpClient();
        answers    = new HashMap<>();

        // Bind views
        tvQuestionTitle         = findViewById(R.id.tvQuestionTitle);
        tvQuestionSubtitle      = findViewById(R.id.tvQuestionSubtitle);
        tvSkip                  = findViewById(R.id.tvSkip);
        progressBar             = findViewById(R.id.progressBar);
        llOptions               = findViewById(R.id.llOptions);
        llTextInput             = findViewById(R.id.llTextInput);
        llPhotoUpload           = findViewById(R.id.llPhotoUpload);
        etTextInput             = findViewById(R.id.etTextInput);
        btnNextText             = findViewById(R.id.btnNextText);
        btnCompleteRegistration = findViewById(R.id.btnCompleteRegistration);
        flPhotoFrame            = findViewById(R.id.flPhotoFrame);
        ivSelectedPhoto         = findViewById(R.id.ivSelectedPhoto);
        tvPlusIcon              = findViewById(R.id.tvPlusIcon);

        setupQuestions();
        loadExistingAnswers();
        updateUIForStep();

        // Skip taps just advance without saving this step's answer
        tvSkip.setOnClickListener(v -> {
            if (currentStep >= steps.size() - 1) {
                finishQuestionnaire(selectedPhotoUri); // complete with current photo
            } else {
                currentStep++;
                selectedOptionIndex = -1;
                updateUIForStep();
            }
        });

        btnNextText.setOnClickListener(v -> {
            answers.put(steps.get(currentStep).key, etTextInput.getText().toString().trim());
            currentStep++;
            selectedOptionIndex = -1;
            updateUIForStep();
        });

        flPhotoFrame.setOnClickListener(v -> requestPhotoPermission());

        btnCompleteRegistration.setOnClickListener(v -> finishQuestionnaire(selectedPhotoUri));

        etTextInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnNextText.setEnabled(s.toString().trim().length() > 0);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }


    // ── Setup ───────────────────────────────────────────────────────────────

    private void setupQuestions() {
        steps = new ArrayList<>();
        steps.add(new QuestionStep("age",           "What is your age?",                      QuestionType.TEXT,          null));
        steps.add(new QuestionStep("gender",        "What is your gender?",                   QuestionType.SINGLE_CHOICE, Arrays.asList("Men", "Women", "Other")));
        steps.add(new QuestionStep("lookingFor",    "What are you looking for?",              QuestionType.SINGLE_CHOICE, Arrays.asList("Men", "Women", "Both")));
        steps.add(new QuestionStep("goal",          "What is your goal?",                     QuestionType.SINGLE_CHOICE, Arrays.asList("Get married", "Find a relationship", "Chat and meet friends", "Learn other cultures", "Travel the world")));
        steps.add(new QuestionStep("height",        "Height",                                 QuestionType.TEXT,          null));
        steps.add(new QuestionStep("weight",        "Weight",                                 QuestionType.TEXT,          null));
        steps.add(new QuestionStep("orientation",   "What is your sexual orientation?",       QuestionType.SINGLE_CHOICE, Arrays.asList("Straight", "Gay", "Lesbian", "Bisexual", "Asexual")));
        steps.add(new QuestionStep("starSign",      "Star sign",                              QuestionType.SINGLE_CHOICE, Arrays.asList("Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Other")));
        steps.add(new QuestionStep("smoke",         "Do you smoke?",                          QuestionType.SINGLE_CHOICE, Arrays.asList("Yes", "Occasionally", "No")));
        steps.add(new QuestionStep("education",     "Education level",                        QuestionType.SINGLE_CHOICE, Arrays.asList("High school", "Undergraduate", "Postgraduate", "PhD candidate")));
        steps.add(new QuestionStep("drink",         "Do you drink?",                          QuestionType.SINGLE_CHOICE, Arrays.asList("Yes", "Occasionally", "No")));
        steps.add(new QuestionStep("mostImportant", "The most important\nthing in your life", QuestionType.SINGLE_CHOICE, Arrays.asList("Beauty & Art", "Career & Money", "Entertainment & Leisure", "Fame & Influence", "Family & Kids", "Science & Research", "Self Realization", "World Improvement")));
        steps.add(new QuestionStep("religion",      "Religion",                               QuestionType.SINGLE_CHOICE, Arrays.asList("Christian", "Buddhist", "Hindu", "Muslim", "Spiritual", "Atheist", "Other")));
        steps.add(new QuestionStep("photo",         "Add your best photo",                    QuestionType.PHOTO,         null));
    }

    // ── UI Update ───────────────────────────────────────────────────────────

    private void updateUIForStep() {
        if (currentStep >= steps.size()) {
            finishQuestionnaire(null);
            return;
        }

        QuestionStep step = steps.get(currentStep);

        // Progress
        progressBar.setProgress((int) (((currentStep + 1) / (float) steps.size()) * 100));

        // Titles
        tvQuestionTitle.setText(step.title);
        String sub = SUBTITLES.get(step.key);
        if (sub != null && !sub.isEmpty()) {
            tvQuestionSubtitle.setText(sub);
            tvQuestionSubtitle.setVisibility(View.VISIBLE);
        } else {
            tvQuestionSubtitle.setVisibility(View.GONE);
        }

        // Hide all panels first
        llOptions.setVisibility(View.GONE);
        llTextInput.setVisibility(View.GONE);
        llPhotoUpload.setVisibility(View.GONE);
        btnCompleteRegistration.setVisibility(View.GONE);

        String existing = answers.get(step.key);

        switch (step.type) {
            case SINGLE_CHOICE:
                llOptions.setVisibility(View.VISIBLE);
                buildOptionCards(step, existing);
                break;

            case TEXT:
                llTextInput.setVisibility(View.VISIBLE);
                etTextInput.setText(existing != null ? existing : "");
                if (step.key.equals("age")) {
                    etTextInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                    etTextInput.setHint("e.g., 24");
                } else {
                    etTextInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
                    etTextInput.setHint(step.key.equals("height")
                            ? "e.g., 170cm or 5'7\""
                            : "e.g., 65kg or 143lbs");
                }
                btnNextText.setEnabled(existing != null && !existing.trim().isEmpty());
                break;

            case PHOTO:
                llPhotoUpload.setVisibility(View.VISIBLE);
                btnCompleteRegistration.setVisibility(View.VISIBLE);
                
                String currentPhoto = answers.get("photo");
                boolean hasPhoto = (selectedPhotoUri != null) || (currentPhoto != null && !currentPhoto.isEmpty());
                btnCompleteRegistration.setEnabled(hasPhoto);
                
                if (selectedPhotoUri != null) {
                    ivSelectedPhoto.setVisibility(View.VISIBLE);
                    tvPlusIcon.setVisibility(View.GONE);
                    Glide.with(this).load(selectedPhotoUri).centerCrop().into(ivSelectedPhoto);
                } else if (currentPhoto != null && !currentPhoto.isEmpty()) {
                    ivSelectedPhoto.setVisibility(View.VISIBLE);
                    tvPlusIcon.setVisibility(View.GONE);
                    Glide.with(this).load(currentPhoto).centerCrop().into(ivSelectedPhoto);
                } else {
                    ivSelectedPhoto.setVisibility(View.GONE);
                    tvPlusIcon.setVisibility(View.VISIBLE);
                }
                break;
        }

    }

    /** Dynamically inflates option cards into llOptions */
    private void buildOptionCards(QuestionStep step, String existingAnswer) {
        llOptions.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < step.options.size(); i++) {
            final int index = i;
            final String option = step.options.get(i);

            View card = inflater.inflate(R.layout.item_question_option, llOptions, false);
            TextView tvTitle    = card.findViewById(R.id.tvOptionTitle);
            TextView tvSubtitle = card.findViewById(R.id.tvOptionSubtitle);
            RadioButton rb      = card.findViewById(R.id.rbOption);

            tvTitle.setText(option);

            // Show descriptions for goal step
            if ("goal".equals(step.key) && GOAL_DESCRIPTIONS.containsKey(option)) {
                tvSubtitle.setText(GOAL_DESCRIPTIONS.get(option));
                tvSubtitle.setVisibility(View.VISIBLE);
            }

            // Restore prior selection
            boolean isSelected = option.equals(existingAnswer);
            if (isSelected) {
                selectedOptionIndex = index;
                card.setBackground(ContextCompat.getDrawable(this, R.drawable.question_option_selected_bg));
                rb.setChecked(true);
            }

            card.setOnClickListener(v -> {
                // Update answers
                answers.put(step.key, option);
                selectedOptionIndex = index;
                // Refresh all card backgrounds
                for (int j = 0; j < llOptions.getChildCount(); j++) {
                    View c = llOptions.getChildAt(j);
                    RadioButton rj = c.findViewById(R.id.rbOption);
                    if (j == index) {
                        c.setBackground(ContextCompat.getDrawable(this, R.drawable.question_option_selected_bg));
                        rj.setChecked(true);
                    } else {
                        c.setBackground(ContextCompat.getDrawable(this, R.drawable.question_option_bg));
                        rj.setChecked(false);
                    }
                }
                // Auto-advance after short delay
                v.postDelayed(() -> {
                    currentStep++;
                    selectedOptionIndex = -1;
                    updateUIForStep();
                }, 300);
            });

            llOptions.addView(card);
        }
    }

    // ── Photo handling ───────────────────────────────────────────────────────

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

    // ── Finish ───────────────────────────────────────────────────────────────

    private void finishQuestionnaire(Uri photoUri) {
        btnCompleteRegistration.setEnabled(false);
        tvSkip.setEnabled(false);

        if (photoUri != null) {
            Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show();
            uploadToCloudinary(photoUri);
        } else {
            saveToFirestoreAndNavigate(null);
        }
    }

    // ── Cloudinary upload (signed) ────────────────────────────────────────────

    private void uploadToCloudinary(Uri photoUri) {
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(photoUri);
                if (is == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Cannot read photo", Toast.LENGTH_SHORT).show();
                        saveToFirestoreAndNavigate(null);
                    });
                    return;
                }

                byte[] bytes = readBytes(is);
                is.close();

                // Build signed upload parameters
                long timestamp = System.currentTimeMillis() / 1000L;
                String folder = "lovecare_profiles";

                // Signature string: folder=X&timestamp=Y + API_SECRET
                String toSign = "folder=" + folder + "&timestamp=" + timestamp + CLOUDINARY_API_SECRET;
                String signature = sha1Hex(toSign);

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", "photo.jpg",
                                RequestBody.create(bytes, MediaType.parse("image/jpeg")))
                        .addFormDataPart("api_key",   CLOUDINARY_API_KEY)
                        .addFormDataPart("timestamp", String.valueOf(timestamp))
                        .addFormDataPart("folder",    folder)
                        .addFormDataPart("signature", signature)
                        .build();

                Request request = new Request.Builder()
                        .url(CLOUDINARY_UPLOAD_URL)
                        .post(requestBody)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    String body = response.body() != null ? response.body().string() : "";
                    android.util.Log.d("CloudinaryUpload", "Response code=" + response.code() + " body=" + body);

                    runOnUiThread(() -> {
                        try {
                            JSONObject json = new JSONObject(body);
                            if (json.has("secure_url")) {
                                saveToFirestoreAndNavigate(json.getString("secure_url"));
                            } else {
                                String errMsg = json.has("error")
                                        ? json.getJSONObject("error").optString("message", "Unknown")
                                        : "No secure_url in response";
                                android.util.Log.e("CloudinaryUpload", "Error: " + errMsg);
                                Toast.makeText(this, "Upload failed: " + errMsg, Toast.LENGTH_LONG).show();
                                saveToFirestoreAndNavigate(null);
                            }
                        } catch (Exception e) {
                            android.util.Log.e("CloudinaryUpload", "Parse error: " + e.getMessage());
                            Toast.makeText(this, "Upload error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            saveToFirestoreAndNavigate(null);
                        }
                    });
                }

            } catch (Exception e) {
                android.util.Log.e("CloudinaryUpload", "Exception: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Upload error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    saveToFirestoreAndNavigate(null);
                });
            }
        }).start();
    }

    /** Compute SHA-1 hex digest */
    private static String sha1Hex(String input) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(input.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // ── Firestore save ────────────────────────────────────────────────────────

    private void saveToFirestoreAndNavigate(String photoUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            navigateToDashboard();
            return;
        }

        Map<String, Object> updates = new HashMap<>(answers);
        updates.put("hasCompletedQuestionnaire", true);
        if (photoUrl != null && !photoUrl.isEmpty()) {
            updates.put("photoUrl", photoUrl);
            updates.put("photo", photoUrl); // also update the photo field
        }

        db.collection("users").document(user.getUid()).set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile completed! Welcome 💕", Toast.LENGTH_SHORT).show();
                    navigateToDashboard();
                })
                .addOnFailureListener(e -> {
                    btnCompleteRegistration.setEnabled(true);
                    tvSkip.setEnabled(true);
                    Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, NavigationBar.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ── Data classes ──────────────────────────────────────────────────────────

    enum QuestionType { SINGLE_CHOICE, TEXT, PHOTO }

    static class QuestionStep {
        String key, title;
        QuestionType type;
        List<String> options;

        QuestionStep(String key, String title, QuestionType type, List<String> options) {
            this.key     = key;
            this.title   = title;
            this.type    = type;
            this.options = options;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static byte[] readBytes(InputStream is) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    private void loadExistingAnswers() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        for (QuestionStep step : steps) {
                            String value = documentSnapshot.getString(step.key);
                            if (value != null) {
                                answers.put(step.key, value);
                            }
                        }
                        String photo = documentSnapshot.getString("photoUrl");
                        if (photo == null || photo.isEmpty()) {
                            photo = documentSnapshot.getString("photo");
                        }
                        if (photo != null && !photo.isEmpty()) {
                            answers.put("photo", photo);
                        }
                        updateUIForStep();
                    }
                });
    }
}

