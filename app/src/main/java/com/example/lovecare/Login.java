package com.example.lovecare;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Login extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private AppCompatButton btnLogin;
    private TextView tvSignUp;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btn_login);
        tvSignUp = findViewById(R.id.tvSignUp);

        if (tvSignUp != null) {
            tvSignUp.setOnClickListener(v -> {
                Intent intent = new Intent(Login.this, SignUp.class);
                startActivity(intent);
            });
        }

        btnLogin.setOnClickListener(v -> performLogin());
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        // Support shorthand "admin" -> "admin@lovecare.com" for default admin convenience
        final String finalEmail = email.equalsIgnoreCase("admin") ? "admin@lovecare.com" : email;

        btnLogin.setEnabled(false);
        Toast.makeText(Login.this, "Signing in...", Toast.LENGTH_SHORT).show();

        mAuth.signInWithEmailAndPassword(finalEmail, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        fetchUserRoleAndNavigate(mAuth.getCurrentUser().getUid());
                    } else {
                        // Check if it's default admin login attempt to seed default admin user
                        if (isDefaultAdminCredentials(finalEmail, password)) {
                            seedDefaultAdmin(finalEmail, password);
                        } else {
                            btnLogin.setEnabled(true);
                            String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                            Toast.makeText(Login.this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private boolean isDefaultAdminCredentials(String email, String password) {
        return ("admin@lovecare.com".equalsIgnoreCase(email) || "admin".equalsIgnoreCase(email)) && "admin123".equals(password);
    }

    private void seedDefaultAdmin(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();
                        Map<String, Object> adminData = new HashMap<>();
                        adminData.put("name", "System Admin");
                        adminData.put("email", email);
                        adminData.put("role", "admin");
                        adminData.put("createdAt", System.currentTimeMillis());

                        db.collection("users").document(uid).set(adminData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(Login.this, "Default Admin Account Created & Signed In!", Toast.LENGTH_SHORT).show();
                                    navigateToDashboard("admin");
                                })
                                .addOnFailureListener(e -> {
                                    btnLogin.setEnabled(true);
                                    Toast.makeText(Login.this, "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        btnLogin.setEnabled(true);
                        Toast.makeText(Login.this, "Login failed. Invalid credentials.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchUserRoleAndNavigate(String uid) {
        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    btnLogin.setEnabled(true);
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        String role = doc.getString("role");
                        if (role == null) {
                            role = "user";
                        }
                        navigateToDashboard(role);
                    } else {
                        // Fallback navigation to User dashboard if role read fails
                        navigateToDashboard("user");
                    }
                });
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        if ("admin".equalsIgnoreCase(role)) {
            intent = new Intent(Login.this, MainActivity.class);
        } else {
            intent = new Intent(Login.this, NavigationBar.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}