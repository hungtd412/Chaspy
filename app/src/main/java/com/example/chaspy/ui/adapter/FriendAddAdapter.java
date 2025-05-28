package com.example.chaspy.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chaspy.R;
import com.example.chaspy.data.model.User;
import com.squareup.picasso.Picasso;


import java.util.ArrayList;
import java.util.Map;

public class FriendAddAdapter extends RecyclerView.Adapter<FriendAddAdapter.UserViewHolder> {
    Context mainActivity;
    ArrayList<User> usersArrayList;
    private OnAddFriendClickListener addFriendListener;
    
    // Maps to track friend request states
    private Map<String, Boolean> sentFriendRequests;
    private Map<String, Boolean> receivedFriendRequests;
    
    // Interface for handling friend request button clicks
    public interface OnAddFriendClickListener {
        void onAddFriendClick(User user, int position);
        void onCancelFriendRequestClick(User user, int position);
    }
    
    public FriendAddAdapter(Context mainActivity, ArrayList<User> usersArrayList) {
        this.mainActivity = mainActivity;
        this.usersArrayList = usersArrayList;
    }
    
    // Set the listener for add friend button clicks
    public void setOnAddFriendClickListener(OnAddFriendClickListener listener) {
        this.addFriendListener = listener;
    }
    
    /**
     * Update the button states based on friend request status
     * @param sent Map of sent friend requests
     * @param received Map of received friend requests
     */
    public void updateButtonStates(Map<String, Boolean> sent, Map<String, Boolean> received) {
        this.sentFriendRequests = sent;
        this.receivedFriendRequests = received;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mainActivity).inflate(R.layout.item_add_friends, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = usersArrayList.get(position);
        holder.getUserName().setText(user.getUserName());

        // Hide or repurpose userStatus - we don't show email anymore
        holder.getUserStatus().setVisibility(View.GONE);

        // Load the user's profile image
        Picasso.get().load(user.getProfilePicUrl()).into(holder.getUserImg());

        // Set button state based on friend request status
        if (sentFriendRequests != null && sentFriendRequests.containsKey(user.getUid())) {
            // Request already sent - show cancel option
            Button btnAddFriend = holder.getAddFriendButton();
            btnAddFriend.setText("Cancel");
            btnAddFriend.setEnabled(true);

            // Set cancel request click listener
            btnAddFriend.setOnClickListener(view -> {
                if (addFriendListener != null) {
                    addFriendListener.onCancelFriendRequestClick(user, position);
                }
            });
        } else if (receivedFriendRequests != null && receivedFriendRequests.containsKey(user.getUid())) {
            // Request received from this user
            Button btnAddFriend = holder.getAddFriendButton();
            btnAddFriend.setText("Respond");
            btnAddFriend.setEnabled(false); // Disable direct add - they should go to requests tab
        } else {
            // No requests, normal state
            Button btnAddFriend = holder.getAddFriendButton();
            btnAddFriend.setText("Add");
            btnAddFriend.setEnabled(true);

            // Setup add friend button click
            btnAddFriend.setOnClickListener(view -> {
                if (addFriendListener != null) {
                    addFriendListener.onAddFriendClick(user, position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return usersArrayList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        private ImageView userImg;
        private TextView userName, userStatus;
        private Button btnAddFriend;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userImg = itemView.findViewById(R.id.userImg);
            userName = itemView.findViewById(R.id.userName);
            userStatus = itemView.findViewById(R.id.userStatus);
            btnAddFriend = itemView.findViewById(R.id.btnAddFriend);
        }

        public ImageView getUserImg() {
            return userImg;
        }

        public TextView getUserName() {
            return userName;
        }

        public TextView getUserStatus() {
            return userStatus;
        }

        public Button getAddFriendButton() {
            return btnAddFriend;
        }
    }
}
