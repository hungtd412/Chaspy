package com.example.chaspy.ui.view.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chaspy.R;
import com.example.chaspy.data.model.FriendRequest;
import com.example.chaspy.data.model.User;
import com.example.chaspy.ui.adapter.UserAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddFriendsFragment extends Fragment implements UserAdapter.OnAddFriendClickListener {

    private RecyclerView recyclerViewAddFriends;
    private TextView emptyViewAddFriends;
    private ArrayList<User> usersList;
    private UserAdapter userAdapter;

    // Firebase components
    private FirebaseAuth firebaseAuth;
    private DatabaseReference usersReference;
    private DatabaseReference friendRequestsReference;

    // Current user's friends map and sent requests map
    private Map<String, Boolean> currentUserFriends = new HashMap<>();
    private Map<String, Boolean> sentFriendRequests = new HashMap<>();
    private Map<String, Boolean> receivedFriendRequests = new HashMap<>();
    private Map<String, Boolean> blockedUsers = new HashMap<>();
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase components
        firebaseAuth = FirebaseAuth.getInstance();
        usersReference = FirebaseDatabase.getInstance().getReference().child("users");
        friendRequestsReference = FirebaseDatabase.getInstance().getReference().child("friend_requests");

        System.out.println(friendRequestsReference);

        // Get current user ID
        currentUserId = firebaseAuth.getCurrentUser() != null ? firebaseAuth.getCurrentUser().getUid() : null;

        // Initialize views
        recyclerViewAddFriends = view.findViewById(R.id.recyclerViewAddFriends);
        emptyViewAddFriends = view.findViewById(R.id.emptyViewAddFriends);

        // Initialize users list and adapter
        usersList = new ArrayList<>();
        userAdapter = new UserAdapter(getContext(), usersList);
        userAdapter.setOnAddFriendClickListener(this);

        // Set up RecyclerView
        recyclerViewAddFriends.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewAddFriends.setAdapter(userAdapter);

        // Load current user's friends, sent requests, and block list
        if (currentUserId != null) {
            loadCurrentUserFriends();
            loadSentFriendRequests();
            loadReceivedFriendRequests();
            loadBlockedUsers();
        } else {
            emptyViewAddFriends.setText("You need to be logged in");
        }

        // Show empty view initially
        updateEmptyViewVisibility();
    }

    /**
     * Load the current user's friends from Firebase
     */
    private void loadCurrentUserFriends() {
        if (currentUserId == null) return;

        DatabaseReference userFriendsRef = usersReference.child(currentUserId).child("friends");
        userFriendsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentUserFriends.clear();

                if (dataSnapshot.exists()) {
                    for (DataSnapshot friendSnapshot : dataSnapshot.getChildren()) {
                        String friendId = friendSnapshot.getKey();
                        Boolean value = friendSnapshot.getValue(Boolean.class);
                        if (friendId != null && value != null && value) {
                            currentUserFriends.put(friendId, true);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle the error
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error loading friends list: " + databaseError.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
                emptyViewAddFriends.setText("Error loading friends list");
            }
        });
    }

    /**
     * Load friend requests sent by the current user
     */
    private void loadSentFriendRequests() {
        if (currentUserId == null) return;

        // Clear existing data
        sentFriendRequests.clear();

        // Query for requests where current user is the sender
        friendRequestsReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                sentFriendRequests.clear();

                if (dataSnapshot.exists()) {
                    for (DataSnapshot requestSnapshot : dataSnapshot.getChildren()) {
                        FriendRequest request = requestSnapshot.getValue(FriendRequest.class);
                        if (request != null && currentUserId.equals(request.getSenderId()) && request.getReceiverId() != null && request.getStatus() == "pending") {
                            sentFriendRequests.put(request.getReceiverId(), true);
                        }
                    }
                }

                System.out.println("Sent friend requests: " + sentFriendRequests);

                // Update adapter button states for any visible items
                if (userAdapter != null) {
                    userAdapter.updateButtonStates(sentFriendRequests, receivedFriendRequests);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error loading sent friend requests: " + databaseError.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Load friend requests received by the current user
     */
    private void loadReceivedFriendRequests() {
        if (currentUserId == null) return;

        // Clear existing data
        receivedFriendRequests.clear();

        // Query for requests where current user is the receiver
        friendRequestsReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                receivedFriendRequests.clear();

                if (dataSnapshot.exists()) {
                    for (DataSnapshot requestSnapshot : dataSnapshot.getChildren()) {
                        FriendRequest request = requestSnapshot.getValue(FriendRequest.class);

                        if (request != null && currentUserId.equals(request.getReceiverId()) && request.getSenderId() != null && request.getStatus() != "accepted") {
                            receivedFriendRequests.put(request.getSenderId(), true);
                        }
                    }
                }
                System.out.println("Received friend requests: " + receivedFriendRequests);

                System.out.println("Received friend requests: " + receivedFriendRequests);

                // Update adapter button states for any visible items
                if (userAdapter != null) {
                    userAdapter.updateButtonStates(sentFriendRequests, receivedFriendRequests);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error loading received friend requests: " + databaseError.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Load users that the current user has blocked, or that have blocked the current user
     */
    private void loadBlockedUsers() {
        if (currentUserId == null) return;

        // Get users who have blocked the current user
        Query blockListQuery = usersReference.orderByChild("block_list/" + currentUserId).equalTo(true);
        blockListQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Clear existing blocked users related to other users blocking current user
                for (String key : new ArrayList<>(blockedUsers.keySet())) {
                    if (!key.equals(currentUserId)) {
                        blockedUsers.remove(key);
                    }
                }

                // Add users who have blocked the current user
                if (dataSnapshot.exists()) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String userId = userSnapshot.getKey();
                        if (userId != null) {
                            blockedUsers.put(userId, true);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error loading block list", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Get users blocked by current user
        DatabaseReference currentUserBlockListRef = usersReference.child(currentUserId).child("block_list");
        currentUserBlockListRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Clear existing blocked users by current user
                blockedUsers.put(currentUserId, false);

                if (dataSnapshot.exists()) {
                    for (DataSnapshot blockSnapshot : dataSnapshot.getChildren()) {
                        String blockedId = blockSnapshot.getKey();
                        Boolean value = blockSnapshot.getValue(Boolean.class);
                        if (blockedId != null && value != null && value) {
                            blockedUsers.put(blockedId, true);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error loading your block list", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Search for users based on the provided query
     * This method will be called from FriendsActivity when the search query changes
     *
     * @param query The search query (name)
     */
    public void searchUsers(String query) {
        // Trim the query to remove any leading or trailing whitespace
        String trimmedQuery = (query != null) ? query.trim() : "";

        // Show empty view with loading message when starting search
        if (!trimmedQuery.isEmpty()) {
            emptyViewAddFriends.setText("Searching for users...");
            loadAllUsers(trimmedQuery);
        } else {
            // Clear the list if query is empty
            usersList.clear();
            userAdapter.notifyDataSetChanged();
            emptyViewAddFriends.setText("Search for users to add as friends");
            updateEmptyViewVisibility();
        }
    }

    /**
     * Load all users from Firebase that match the query
     *
     * @param query The search query to filter users by
     */
    private void loadAllUsers(String query) {
        if (currentUserId == null) {
            emptyViewAddFriends.setText("You need to be logged in");
            return;
        }

        usersList.clear();

        String lowercaseQuery = query.toLowerCase();

        usersReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                usersList.clear();

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);

                    // Set the UID for the user based on the key
                    if (user != null) {
                        String userId = userSnapshot.getKey();
                        user.setUid(userId);

                        // Skip if user is current user
                        if (userId.equals(currentUserId)) {
                            continue;
                        }

                        // Skip if user is already a friend
                        if (currentUserFriends.containsKey(userId)) {
                            continue;
                        }

                        // Skip if user is in block list (either direction)
                        if (blockedUsers.containsKey(userId)) {
                            continue;
                        }

                        // Check if user's name contains the query string
                        String firstName = user.getFirstName() != null ? user.getFirstName().toLowerCase() : "";
                        String lastName = user.getLastName() != null ? user.getLastName().toLowerCase() : "";
                        String fullName = (firstName + " " + lastName).toLowerCase();

                        if (fullName.contains(lowercaseQuery)) {
                            usersList.add(user);
                        }
                    }
                }

                // Update the adapter with button states before notifying data changed
                userAdapter.updateButtonStates(sentFriendRequests, receivedFriendRequests);

                // Update UI after search
                userAdapter.notifyDataSetChanged();
                updateEmptyViewVisibility();

                // If no results found, update empty view text
                if (usersList.isEmpty()) {
                    emptyViewAddFriends.setText("No users found matching your search");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle the error
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error searching for users: " + databaseError.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
                emptyViewAddFriends.setText("Error searching for users");
                updateEmptyViewVisibility();
            }
        });
    }

    /**
     * Update the visibility of empty view based on whether there are items in the list
     */
    private void updateEmptyViewVisibility() {
        if (usersList.isEmpty()) {
            recyclerViewAddFriends.setVisibility(View.GONE);
            emptyViewAddFriends.setVisibility(View.VISIBLE);
        } else {
            recyclerViewAddFriends.setVisibility(View.VISIBLE);
            emptyViewAddFriends.setVisibility(View.GONE);
        }
    }

    /**
     * Send a friend request to the user with the given userId
     *
     * @param receiverId The user ID of the request recipient
     */
    private void sendFriendRequest(String receiverId) {
        if (currentUserId == null || receiverId == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error: Invalid user data", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Get a reference to generate a new unique key for the friend request
        DatabaseReference newRequestRef = friendRequestsReference.push();
        String requestId = newRequestRef.getKey();

        if (requestId == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error creating request ID", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Create the friend request with only the required fields
        Map<String, Object> friendRequest = new HashMap<>();
        friendRequest.put("sender_id", currentUserId);
        friendRequest.put("receiver_id", receiverId);
        friendRequest.put("status", "pending");

        // Save the friend request to Firebase
        newRequestRef.setValue(friendRequest)
                .addOnSuccessListener(aVoid -> {
                    // Update local map of sent requests
                    sentFriendRequests.put(receiverId, true);

                    // Update button states in adapter
                    if (userAdapter != null) {
                        userAdapter.updateButtonStates(sentFriendRequests, receivedFriendRequests);
                    }

                    // Show success message
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Friend request sent", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Show error message
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to send request: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                    e.printStackTrace();
                });
    }

    @Override
    public void onAddFriendClick(User user, int position) {
        try {
            // Check if we've received a friend request from this user
            if (user != null && user.getUid() != null && currentUserId != null) {
                String userId = user.getUid();
                System.out.println(receivedFriendRequests);
                // Check if the user is already a friend
                if (currentUserFriends.containsKey(userId)) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(),
                                user.getFullName() + " is already in your friends list",
                                Toast.LENGTH_SHORT).show();
                    }
                } else if (receivedFriendRequests.containsKey(userId)) {
                    // Inform the user that they already have a friend request from this user
                    // and direct them to the Requests tab
                    System.out.println("Received friend requests: " + receivedFriendRequests);
                    if (getContext() != null) {
                        Toast.makeText(getContext(),
                                "You already have a friend request from " + user.getFullName() +
                                        ". Go to Requests tab to accept it.",
                                Toast.LENGTH_LONG).show();
                    }
                } else if (sentFriendRequests.containsKey(userId)) {
                    // Request already sent, just show toast as a reminder
                    if (getContext() != null) {
                        Toast.makeText(getContext(),
                                "Friend request already sent to " + user.getFullName(),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Send friend request directly - we've already checked the necessary conditions
                    sendFriendRequest(userId);
                }
            } else {
                if (getContext() != null) {
                    String errorMsg = user == null ? "Invalid user" :
                            (user.getUid() == null ? "User has no ID" :
                                    "You are not logged in");
                    Toast.makeText(getContext(), "Cannot send request: " + errorMsg, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            e.printStackTrace();
        }
    }
}