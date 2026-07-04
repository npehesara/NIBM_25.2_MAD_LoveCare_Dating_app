package com.firstapp.da_messaging_module;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

public class ChatRoomFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat_room, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View headerContainer = view.findViewById(R.id.header_container);
        TextView usernameHeader = view.findViewById(R.id.text_chat_username);
        View btnBack = view.findViewById(R.id.btn_back);

        // 1. Handle Status Bar Insets
        // This ensures the username bar sits below the device icons (status bar)
        ViewCompat.setOnApplyWindowInsetsListener(headerContainer, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });

        // 2. Retrieve the username from the arguments passed from MessageListFragment
        if (getArguments() != null) {
            String name = getArguments().getString("username");
            if (name != null) {
                usernameHeader.setText(name); // Set the username in the top banner
            }
        }

        // 3. Set up the back button to return to the Message List
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }
}