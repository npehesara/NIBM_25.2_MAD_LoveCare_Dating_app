package com.example.lovecare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LoveSpaceMessage extends AppCompatActivity {

    private TextView tvChatUsername;
    private RecyclerView rvMessages;
    private EditText etChatMessage;
    private FloatingActionButton fabSend;

    private ChatMessageAdapter messageAdapter;
    private List<ChatMessage> messageList;

    private FirebaseAuth mAuth;
    private DatabaseReference chatRef;
    private String myUid;
    private String partnerUid;
    private String chatId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_love_space_message);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }
        myUid = mAuth.getCurrentUser().getUid();

        partnerUid = getIntent().getStringExtra("userId");
        if (partnerUid == null || partnerUid.isEmpty()) {
            Toast.makeText(this, "Invalid partner details", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Generate unique chatId alphabetically to match both users
        chatId = myUid.compareTo(partnerUid) < 0 ? myUid + "_" + partnerUid : partnerUid + "_" + myUid;
        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("messages");

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
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(partnerUid).get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            String name = userDoc.getString("name");
                            if (name != null && !name.isEmpty()) {
                                tvChatUsername.setText(name);
                            } else {
                                tvChatUsername.setText("Chat Conversation");
                            }
                        } else {
                            tvChatUsername.setText("Chat Conversation");
                        }
                    })
                    .addOnFailureListener(e -> tvChatUsername.setText("Chat Conversation"));
        }
    }

    private void setupMessageList() {
        messageList = new ArrayList<>();
        messageAdapter = new ChatMessageAdapter(this, messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(messageAdapter);

        // Listen for message updates in Firebase Realtime Database
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot msgSnap : snapshot.getChildren()) {
                    String senderId = msgSnap.child("senderId").getValue(String.class);
                    String text = msgSnap.child("messageText").getValue(String.class);
                    if (text == null) {
                        text = msgSnap.child("text").getValue(String.class);
                    }
                    String time = msgSnap.child("time").getValue(String.class);

                    if (senderId != null && text != null) {
                        boolean isSentByMe = senderId.equals(myUid);
                        messageList.add(new ChatMessage(text, time != null ? time : "", isSentByMe));
                    }
                }
                messageAdapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    rvMessages.smoothScrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoveSpaceMessage.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSendButton() {
        if (fabSend == null || etChatMessage == null) return;

        fabSend.setOnClickListener(v -> {
            String text = etChatMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                String currentTime = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());

                Map<String, Object> messageMap = new HashMap<>();
                messageMap.put("senderId", myUid);
                messageMap.put("receiverId", partnerUid);
                messageMap.put("messageText", text);
                messageMap.put("timestamp", System.currentTimeMillis());
                messageMap.put("time", currentTime);

                chatRef.push().setValue(messageMap)
                        .addOnFailureListener(e -> Toast.makeText(LoveSpaceMessage.this, "Message send failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());

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