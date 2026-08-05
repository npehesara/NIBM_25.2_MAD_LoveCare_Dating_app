package com.example.lovecare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LoveSpaceMessage extends AppCompatActivity {

    private TextView tvChatUsername;
    private RecyclerView rvMessages;
    private EditText etChatMessage;
    private FloatingActionButton fabSend;

    private ChatMessageAdapter messageAdapter;
    private List<ChatMessage> messageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_love_space_message);

        View root = findViewById(R.id.messageRoot);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        initViews();
        setupUserHeader();
        setupMessageList();
        setupSendButton();
        setupAttachmentButtons();
    }

    private void initViews() {
        tvChatUsername = findViewById(R.id.tvChatUsername);
        rvMessages = findViewById(R.id.rvMessages);
        etChatMessage = findViewById(R.id.etChatMessage);
        fabSend = findViewById(R.id.fabSend);

        View btnBack = findViewById(R.id.btnChatBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnOptions = findViewById(R.id.btnChatOptions);
        if (btnOptions != null) {
            btnOptions.setOnClickListener(v -> Toast.makeText(this, "Chat Options", Toast.LENGTH_SHORT).show());
        }
    }

    private void setupUserHeader() {
        String username = getIntent().getStringExtra("username");
        if (username != null && !username.isEmpty()) {
            tvChatUsername.setText(username);
        } else {
            tvChatUsername.setText("testacc");
        }
    }

    private void setupMessageList() {
        messageList = new ArrayList<>();
        String name = tvChatUsername.getText().toString();

        messageList.add(new ChatMessage("Hey there! Welcome to LoveCare.", "2:40 PM", false));
        messageList.add(new ChatMessage("Hi " + name + "! How are you doing today?", "2:41 PM", true));
        messageList.add(new ChatMessage("Doing great! Glad to connect with you here.", "2:43 PM", false));

        messageAdapter = new ChatMessageAdapter(this, messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(messageAdapter);
    }

    private void setupSendButton() {
        if (fabSend == null || etChatMessage == null) return;

        fabSend.setOnClickListener(v -> {
            String text = etChatMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                String currentTime = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
                messageList.add(new ChatMessage(text, currentTime, true));
                messageAdapter.notifyItemInserted(messageList.size() - 1);
                rvMessages.smoothScrollToPosition(messageList.size() - 1);
                etChatMessage.setText("");
            }
        });
    }

    private void setupAttachmentButtons() {
        View btnGallery = findViewById(R.id.btnAttachGallery);
        if (btnGallery != null) {
            btnGallery.setOnClickListener(v -> Toast.makeText(this, "Select photo from Gallery", Toast.LENGTH_SHORT).show());
        }

        View btnCamera = findViewById(R.id.btnAttachCamera);
        if (btnCamera != null) {
            btnCamera.setOnClickListener(v -> Toast.makeText(this, "Open Camera", Toast.LENGTH_SHORT).show());
        }
    }
}