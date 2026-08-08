package com.example.lovecare;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.admindashboardactivity.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUp extends AppCompatActivity {

    private EditText etFullName, etEmail, etPassword;
    private RadioGroup rgRole;
    private RadioButton rbAdmin;
    private AppCompatButton btnSignUp;
    private TextView tvLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainSignUp), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmailSignUp);
        etPassword = findViewById(R.id.etPasswordSignUp);
        btnSignUp = findViewById(R.id.btn_signup);
        tvLogin = findViewById(R.id.tvLogin);

        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(SignUp.this, Login.class);
            startActivity(intent);
            finish();
        });

        btnSignUp.setOnClickListener(v -> performSignUp());
    }

    private void performSignUp() {
        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String role = email.toLowerCase().startsWith("admin") ? "admin" : "user";

        if (TextUtils.isEmpty(name)) {
            etFullName.setError("Full Name is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        btnSignUp.setEnabled(false);
        Toast.makeText(this, "Registering account...", Toast.LENGTH_SHORT).show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestoreAndNavigate(user.getUid(), name, email, role);
                        }
                    } else {
                        btnSignUp.setEnabled(true);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        Toast.makeText(SignUp.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestoreAndNavigate(String uid, String name, String email, String role) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("role", role);
        userData.put("createdAt", System.currentTimeMillis());

        // Save to Firestore
        db.collection("users").document(uid).set(userData);

        Toast.makeText(SignUp.this, "Sign Up Successful as " + role.toUpperCase() + "!", Toast.LENGTH_SHORT).show();
        redirectToDashboard(role);
    }

    private void redirectToDashboard(String role) {
        Intent intent;
        if ("admin".equalsIgnoreCase(role != null ? role.trim() : "")) {
            intent = new Intent(SignUp.this, MainActivity.class);
        } else {
            intent = new Intent(SignUp.this, QuestionnaireActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
