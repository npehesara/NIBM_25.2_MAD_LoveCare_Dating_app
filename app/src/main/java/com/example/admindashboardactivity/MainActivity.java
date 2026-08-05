package com.example.admindashboardactivity;

import com.example.lovecare.R;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        ImageButton btnLogout = findViewById(R.id.btn_logout);

        btnLogout.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();

            finish();
        });
        

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_reported_users) {
                selectedFragment = new ReportedUsersFragment();
            } else if (itemId == R.id.nav_user_management) {
                selectedFragment = new UserManagementFragment();
            } else if (itemId == R.id.nav_add_events) {
                selectedFragment = new AddEventsFragment();
            } else {
                return false;
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            return true;
        });
    }
}