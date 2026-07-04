package com.example.lovecareapp_register;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // View Panel References
    private ConstraintLayout layoutRegistration, layoutOnboardingWizards;
    private AppCompatButton btnRegister;

    // Onboarding References
    private ProgressBar progressBar;
    private TextView tvTitle, tvSubtitle, tvSkip;
    private LinearLayout optionsContainer;
    private AppCompatButton btnContinue;

    private int currentStep = 1;
    private final int TOTAL_STEPS = 14; // Increased from 13 to 14
    private ImageView finalPreviewImageView; // Stores reference for displaying image chosen

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind layouts
        layoutRegistration = findViewById(R.id.layoutRegistration);
        layoutOnboardingWizards = findViewById(R.id.layoutOnboardingWizards);
        btnRegister = findViewById(R.id.btn_register);

        // Bind onboarding elements
        progressBar = findViewById(R.id.onboardingProgressBar);
        tvTitle = findViewById(R.id.tvStepTitle);
        tvSubtitle = findViewById(R.id.tvStepSubtitle);
        optionsContainer = findViewById(R.id.optionsContainer);
        btnContinue = findViewById(R.id.btnContinue);
        tvSkip = findViewById(R.id.tvSkipStep);

        // Handle transition from Register -> Onboarding
        btnRegister.setOnClickListener(v -> {
            layoutRegistration.setVisibility(View.GONE);
            layoutOnboardingWizards.setVisibility(View.VISIBLE);
            renderStep();
        });

        tvSkip.setOnClickListener(v -> moveToNextStep());
        btnContinue.setOnClickListener(v -> moveToNextStep());
    }

    private void moveToNextStep() {
        if (currentStep < TOTAL_STEPS) {
            currentStep++;
            renderStep();
        } else {
            // Step 14 Completed!
            android.widget.Toast.makeText(this, "Profile Registration Completed Successfully!", android.widget.Toast.LENGTH_LONG).show();
            // Enter intent transitions here to navigate into your friend's main feed/homepage panel view!
        }
    }

    private void renderStep() {
        progressBar.setProgress(currentStep);
        optionsContainer.removeAllViews();
        btnContinue.setVisibility(View.GONE);
        tvSkip.setVisibility(View.VISIBLE);

        switch (currentStep) {
            case 1:
                setupSelectionScreen("What is your gender?", "Select your profile gender classification identity",
                        Arrays.asList("Male", "Female", "Prefer not to say"), null);
                break;
            case 2:
                setupSelectionScreen("Who are you looking for?", "You'll only see choices selected",
                        Arrays.asList("Men", "Women", "Both"), null);
                break;
            case 3:
                setupSelectionScreen("What is your goal?", "You can change your goal at any time",
                        Arrays.asList("Get married", "Find a relationship", "Chat and meet friends", "Learn other cultures", "Travel the world"),
                        Arrays.asList("Find a person to build a joint future", "Meet a soulmate, start a relationship", "Chat with people without serious plans", "Learn something new about the world", "Meet travel partners and share experiences"));
                break;
            case 4:
                setupInputScreen("Height", "Please type your current height profile parameter", "Enter height in CM");
                break;
            case 5:
                setupInputScreen("Weight", "Please type your current weight profile parameter", "Enter weight in KG");
                break;
            case 6:
                setupSelectionScreen("What is your sexual orientation?", "Select how you identify",
                        Arrays.asList("Straight", "Gay", "Lesbian", "Bisexual", "Asexual"), null);
                break;
            case 7:
                setupSelectionScreen("What is your star sign?", "Helps matches find astrological compatibility",
                        Arrays.asList("Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Other"), null);
                break;
            case 8:
                setupSelectionScreen("What is your educational level?", "Select highest qualification attained",
                        Arrays.asList("High School", "Undergraduate Degree", "Postgraduate Degree", "PhD Candidate"), null);
                break;
            case 9:
                setupSelectionScreen("Do you drink?", "Your lifestyle choices on substance consumption",
                        Arrays.asList("Regularly", "In parties", "Socially", "Never"), null);
                break;
            case 10:
                setupSelectionScreen("Do you smoke?", "Select your smoking status status description",
                        Arrays.asList("Yes, regularly", "Occasionally", "No, never"), null);
                break;
            case 11:
                setupSelectionScreen("Religion", "Select your spiritual views background orientation",
                        Arrays.asList("Christian", "Buddhist", "Hindu", "Muslim", "Spiritual", "Atheist", "Other"), null);
                break;
            case 12:
                setupSelectionScreen("What is the most important thing in life for you?", "Core values alignment metric",
                        Arrays.asList("Beauty & Art", "Career & Money", "Entertainment & Leisure", "Fame & Influence", "Family & Kids", "Science & Research", "Self Realization", "World Improvement"), null);
                break;
            case 13:
                setupInputScreen("Describe yourself", "Type a short profile bio summary text", "Write your bio description text here...");
                break;
            case 14:
                setupPhotoUploadScreen("Add your best photo", "Matches want to see who they are talking to!");
                break;
        }
    }

    private void setupSelectionScreen(String title, String subtitle, List<String> options, List<String> optionSubtitles) {
        tvTitle.setText(title);
        tvSubtitle.setText(subtitle);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < options.size(); i++) {
            View view = inflater.inflate(R.layout.item_onboarding_option, optionsContainer, false);
            TextView mainText = view.findViewById(R.id.tvOptionTitle);
            TextView subText = view.findViewById(R.id.tvOptionSubtitle);
            ImageView circle = view.findViewById(R.id.ivSelectionCircle);

            mainText.setText(options.get(i));

            if (optionSubtitles != null && i < optionSubtitles.size()) {
                subText.setText(optionSubtitles.get(i));
                subText.setVisibility(View.VISIBLE);
            }

            view.setOnClickListener(v -> {
                circle.setImageResource(android.R.drawable.radiobutton_on_background);
                v.postDelayed(this::moveToNextStep, 220); // Adds smooth delay to let them see their selection
            });

            optionsContainer.addView(view);
        }
    }

    private void setupInputScreen(String title, String subtitle, String hint) {
        tvTitle.setText(title);
        tvSubtitle.setText(subtitle);
        btnContinue.setVisibility(View.VISIBLE);

        EditText customField = new EditText(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 150);
        params.setMargins(0, 40, 0, 0);
        customField.setLayoutParams(params);
        customField.setHint(hint);
        customField.setPadding(45, 0, 45, 0);

        customField.setTextColor(getColor(R.color.text_dark_title));
        customField.setHintTextColor(getColor(R.color.text_grey_subtitle));
        customField.setBackgroundResource(R.drawable.bg_onboarding_white_card);

        optionsContainer.addView(customField);
    }

    private void setupPhotoUploadScreen(String title, String subtitle) {
        tvTitle.setText(title);
        tvSubtitle.setText(subtitle);
        btnContinue.setText("Complete Registration ❤");
        btnContinue.setVisibility(View.VISIBLE); // Continue button handles final finish action

        LayoutInflater inflater = LayoutInflater.from(this);
        View photoView = inflater.inflate(R.layout.item_onboarding_photo, optionsContainer, false);

        FrameLayout frameSelectPhoto = photoView.findViewById(R.id.frameSelectPhoto);
        finalPreviewImageView = photoView.findViewById(R.id.ivUploadedPreview);
        ImageView ivPlusIcon = photoView.findViewById(R.id.ivPlusIcon);

        // Simple implicit intent trigger to open the device gallery picker
        frameSelectPhoto.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, 101);
        });

        optionsContainer.addView(photoView);
    }

    // Handles picking the photo from your phone's gallery app screen
    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            android.net.Uri imageUri = data.getData();
            if (finalPreviewImageView != null) {
                finalPreviewImageView.setImageURI(imageUri);
                finalPreviewImageView.setBackgroundColor(Color.TRANSPARENT); // Remove frame color once filled
            }
        }
    }
}
