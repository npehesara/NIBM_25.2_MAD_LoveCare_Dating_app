package com.firstapp.da_messaging_module;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;

import com.firstapp.da_messaging_module.modules.User;

import java.util.ArrayList;
import java.util.List;

public class MessageListFragment extends Fragment {

    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private SearchView searchView;
    private List<User> mockUserList;

    public MessageListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_message_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize XML Views
        searchView = view.findViewById(R.id.search_bar);
        recyclerView = view.findViewById(R.id.chat_recycler_view);

        // 2. Build Mock Data matching your exact mockup screenshots
        mockUserList = new ArrayList<>();
        mockUserList.add(new User("1", "testacc"));
        mockUserList.add(new User("2", "dartb44"));
        mockUserList.add(new User("3", "lljllooo"));
        mockUserList.add(new User("4", "gizemtheeekend"));
        mockUserList.add(new User("5", "KAMİL"));
        mockUserList.add(new User("6", "cryingbaby"));

        // 3. Set up RecyclerView Layout Manager and Adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        userAdapter = new UserAdapter(mockUserList, new UserAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(User user) {
                // This handles what happens when you click a profile account row
                ChatRoomFragment chatRoomFragment = new ChatRoomFragment();
                Bundle args = new Bundle();
                args.putString("username", user.getUsername());
                chatRoomFragment.setArguments(args);

                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, chatRoomFragment)
                        .addToBackStack(null) // Allows pressing back button to return to list
                        .commit();
            }
        });

        recyclerView.setAdapter(userAdapter);

        // 4. Connect the Search Bar to the Adapter Filter
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // This filters your list dynamically as you type characters!
                userAdapter.getFilter().filter(newText);
                return true;
            }
        });
    }
}