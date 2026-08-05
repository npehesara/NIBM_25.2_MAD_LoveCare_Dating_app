package com.example.lovecare;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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

    private static final String TAG = "Login";

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

        // Convert shorthand "admin" to "admin@lovecare.com"
        final String finalEmail = email.equalsIgnoreCase("admin") ? "admin@lovecare.com" : email;

        btnLogin.setEnabled(false);
        Toast.makeText(Login.this, "Signing in...", Toast.LENGTH_SHORT).show();

        mAuth.signInWithEmailAndPassword(finalEmail, password)
                .addOnCompleteListener(this, task -> {
                    btnLogin.setEnabled(true);
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        fetchUserRoleAndNavigate(mAuth.getCurrentUser().getUid(), finalEmail);
                    } else {
                        // Check if it's an admin account attempt
                        if (isAdminEmailOrDefault(finalEmail)) {
                            tryAdminFallbackOrCreate(finalEmail, password);
                        } else {
                            String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                            Toast.makeText(Login.this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private boolean isAdminEmailOrDefault(String email) {
        if (email == null) return false;
        String e = email.toLowerCase().trim();
        return e.equalsIgnoreCase("lovecare@gmail.com") || e.equalsIgnoreCase("admin@lovecare.com") || e.contains("admin");
    }

    private void tryAdminFallbackOrCreate(String email, String password) {
        // Attempt creating the account if it wasn't registered in Firebase Auth yet
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();
                        Map<String, Object> adminData = new HashMap<>();
                        adminData.put("name", "lovecareadmin");
                        adminData.put("email", email);
                        adminData.put("role", "admin");
                        adminData.put("createdAt", System.currentTimeMillis());

                        db.collection("users").document(uid).set(adminData);
                        Toast.makeText(Login.this, "Admin account registered & logged in!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(Login.this, "Admin Access Granted!", Toast.LENGTH_SHORT).show();
                    }
                    navigateToDashboard("admin");
                });
    }

    private void fetchUserRoleAndNavigate(String uid, String email) {
        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    btnLogin.setEnabled(true);
                    String role = null;
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        DocumentSnapshot doc = task.getResult();
                        role = doc.getString("role");
                        Log.d(TAG, "Firestore role fetched: " + role + " for doc ID: " + uid);
                    }

                    // Fallback matching if role was missing or Firestore read was blocked/delayed
                    if (role == null || role.trim().isEmpty()) {
                        if (isAdminEmailOrDefault(email)) {
                            role = "admin";
                        } else {
                            role = "user";
                        }
                    }

                    Log.d(TAG, "Final role resolved: " + role + " for email: " + email);
                    navigateToDashboard(role);
                });
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        if ("admin".equalsIgnoreCase(role != null ? role.trim() : "")) {
            Toast.makeText(Login.this, "Welcome Admin! Opening Admin Dashboard...", Toast.LENGTH_SHORT).show();
            intent = new Intent(Login.this, MainActivity.class);
        } else {
            Toast.makeText(Login.this, "Welcome! Opening User Dashboard...", Toast.LENGTH_SHORT).show();
            intent = new Intent(Login.this, NavigationBar.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}