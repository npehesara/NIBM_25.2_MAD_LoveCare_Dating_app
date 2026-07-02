package com.example.lovecare;

import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoveSpaceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_love_space);

        // Handle Window Insets for edge-to-edge
        View root = findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        CalendarView calendarView = findViewById(R.id.calendarView);
        MaterialButton btnAchievement = findViewById(R.id.btnAchievement);

        // Logic for tapping on a date to add events
        if (calendarView != null) {
            calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
                String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                showAddEventDialog(selectedDate);
            });
        }

        // Achievement button logic
        if (btnAchievement != null) {
            btnAchievement.setOnClickListener(v -> 
                Toast.makeText(this, "Opening Achievements & Rewards...", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void showAddEventDialog(String date) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Event for " + date);

        final TextInputEditText input = new TextInputEditText(this);
        input.setHint("What's the plan, love?");
        builder.setView(input);

        builder.setPositiveButton("Save Activity", (dialog, which) -> {
            if (input.getText() != null) {
                String event = input.getText().toString();
                if (!event.isEmpty()) {
                    Toast.makeText(this, "Activity saved: " + event, Toast.LENGTH_LONG).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}
