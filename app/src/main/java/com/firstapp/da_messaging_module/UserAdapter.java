package com.firstapp.da_messaging_module;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import com.firstapp.da_messaging_module.modules.User;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> implements Filterable {

    private List<User> userList;          // This holds the original complete list
    private List<User> userListFiltered;  // This holds the filtered list for searching
    private OnUserClickListener listener;

    // Interface to handle user clicks and navigate to the chat room
    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public UserAdapter(List<User> userList, OnUserClickListener listener) {
        this.userList = userList;
        this.userListFiltered = userList; // Initially, filtered list is the same as the original
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_preview, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userListFiltered.get(position);
        holder.textUsername.setText(user.getUsername());

        // Handle item click events
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userListFiltered.size();
    }

    // --- Search Filter Logic ---
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String charString = constraint.toString().toLowerCase().trim();
                if (charString.isEmpty()) {
                    userListFiltered = userList;
                } else {
                    List<User> filteredList = new ArrayList<>();
                    for (User row : userList) {
                        // Filter matches against usernames
                        if (row.getUsername().toLowerCase().contains(charString)) {
                            filteredList.add(row);
                        }
                    }
                    userListFiltered = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = userListFiltered;
                return filterResults;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                userListFiltered = (ArrayList<User>) results.values;
                notifyDataSetChanged(); // Instantly refreshes the RecyclerView with matching users
            }
        };
    }

    // --- ViewHolder Class ---
    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textUsername, textLastMessage;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textUsername = itemView.findViewById(R.id.text_username);
            textLastMessage = itemView.findViewById(R.id.text_last_message);
        }
    }
}