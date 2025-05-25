package com.example.chaspy.ui.view.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chaspy.R;
import com.example.chaspy.data.model.FriendItem;
import com.example.chaspy.data.model.User;
import com.example.chaspy.ui.adapter.FriendsAdapter;
import com.example.chaspy.ui.view.ChatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendsListFragment extends Fragment implements FriendsAdapter.OnFriendClickListener {
    private static final String TAG = "FriendsListFragment";
    
    private RecyclerView recyclerViewFriends;
    private TextView tvEmptyFriendsList;
    private ProgressBar progressBarFriends;
    private FriendsAdapter adapter;
    
    private FirebaseAuth auth;
    private DatabaseReference databaseRef;
    private List<FriendItem> currentFriendsList;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friends_list, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Firebase components
        auth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference();
        currentFriendsList = new ArrayList<>();
        
        // Initialize UI components
        initializeViews(view);
        
        // Set up RecyclerView
        setupRecyclerView();
        
        // Load friends
        loadFriends();
    }

    private void initializeViews(View view) {
        recyclerViewFriends = view.findViewById(R.id.recyclerViewFriends);
        tvEmptyFriendsList = view.findViewById(R.id.tvEmptyFriendsList);
        progressBarFriends = view.findViewById(R.id.progressBarFriends);
    }
    
    private void setupRecyclerView() {
        adapter = new FriendsAdapter(getContext(), this);
        recyclerViewFriends.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewFriends.setAdapter(adapter);
    }
    
    private void loadFriends() {
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "User not logged in");
            progressBarFriends.setVisibility(View.GONE);
            tvEmptyFriendsList.setVisibility(View.VISIBLE);
            tvEmptyFriendsList.setText("Please log in to view friends");
            return;
        }
        
        String currentUserId = auth.getCurrentUser().getUid();
        Log.d(TAG, "Loading friends for user: " + currentUserId);
        
        // Show progress
        progressBarFriends.setVisibility(View.VISIBLE);
        
        // Reference to current user's friends
        DatabaseReference userRef = databaseRef.child("users").child(currentUserId);
        
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "User data snapshot received: " + snapshot.exists());
                
                if (snapshot.exists()) {
                    // Get the friends map directly from the snapshot
                    DataSnapshot friendsSnapshot = snapshot.child("friends");
                    Log.d(TAG, "Friends snapshot exists: " + friendsSnapshot.exists() + 
                              ", has children: " + friendsSnapshot.hasChildren() + 
                              ", children count: " + friendsSnapshot.getChildrenCount());
                    
                    if (friendsSnapshot.exists() && friendsSnapshot.hasChildren()) {
                        Map<String, Object> friendsMap = (Map<String, Object>) friendsSnapshot.getValue();
                        fetchFriendsData(friendsMap);
                    } else {
                        // No friends found
                        Log.d(TAG, "No friends found for user");
                        progressBarFriends.setVisibility(View.GONE);
                        recyclerViewFriends.setVisibility(View.GONE);
                        tvEmptyFriendsList.setVisibility(View.VISIBLE);
                    }
                } else {
                    // User data not found
                    Log.e(TAG, "User data not found");
                    progressBarFriends.setVisibility(View.GONE);
                    tvEmptyFriendsList.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching user data: " + error.getMessage());
                progressBarFriends.setVisibility(View.GONE);
                tvEmptyFriendsList.setVisibility(View.VISIBLE);
                tvEmptyFriendsList.setText("Error loading friends: " + error.getMessage());
            }
        });
    }
    
    private void fetchFriendsData(Map<String, Object> friendsMap) {
        List<FriendItem> friendsList = new ArrayList<>();
        DatabaseReference usersRef = databaseRef.child("users");
        
        Log.d(TAG, "Fetching data for " + friendsMap.size() + " friends");
        
        // Counter to track loaded friends
        final int[] friendsToLoad = {friendsMap.size()};
        final int[] loadedFriends = {0};
        
        for (String friendId : friendsMap.keySet()) {
            Log.d(TAG, "Fetching data for friend: " + friendId);
            
            usersRef.child(friendId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.d(TAG, "Friend data received for " + friendId + ", exists: " + snapshot.exists());
                    
                    if (snapshot.exists()) {
                        // Extract individual fields directly from the snapshot
                        String firstName = snapshot.child("firstName").getValue(String.class);
                        String lastName = snapshot.child("lastName").getValue(String.class);
                        String fullName = firstName + " " + lastName;
                        String email = snapshot.child("email").getValue(String.class);
                        String profilePicUrl = snapshot.child("profilePicUrl").getValue(String.class);
                        Boolean isActive = snapshot.child("isActive").getValue(Boolean.class);
                        
                        Log.d(TAG, "Friend data: name=" + fullName + ", email=" + email + 
                                  ", profilePic=" + (profilePicUrl != null) + 
                                  ", isActive=" + isActive);
                        
                        FriendItem friend = new FriendItem(
                                friendId,
                                fullName,
                                email,
                                profilePicUrl,
                                isActive != null && isActive
                        );
                        friendsList.add(friend);
                        Log.d(TAG, "Added friend to list: " + fullName);
                    } else {
                        Log.e(TAG, "Friend data not found for ID: " + friendId);
                    }
                    
                    loadedFriends[0]++;
                    
                    // Check if all friends are loaded
                    if (loadedFriends[0] >= friendsToLoad[0]) {
                        Log.d(TAG, "All friends loaded. Total: " + friendsList.size());
                        currentFriendsList = friendsList;
                        updateUI(friendsList);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading friend data for " + friendId + ": " + error.getMessage());
                    loadedFriends[0]++;
                    
                    // Check if all attempts to load friends are completed
                    if (loadedFriends[0] >= friendsToLoad[0]) {
                        Log.d(TAG, "All friend loading attempts completed. Successful loads: " + friendsList.size());
                        currentFriendsList = friendsList;
                        updateUI(friendsList);
                    }
                }
            });
        }
    }
    
    private void updateUI(List<FriendItem> friendsList) {
        progressBarFriends.setVisibility(View.GONE);
        
        if (friendsList.isEmpty()) {
            Log.d(TAG, "No friends to display");
            recyclerViewFriends.setVisibility(View.GONE);
            tvEmptyFriendsList.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG, "Displaying " + friendsList.size() + " friends");
            recyclerViewFriends.setVisibility(View.VISIBLE);
            tvEmptyFriendsList.setVisibility(View.GONE);
            adapter.setFriendsList(friendsList);
        }
    }
    
    /**
     * Filters the friends list based on the search query
     * 
     * @param query The search query string
     */
    public void filterFriends(String query) {
        Log.d(TAG, "Filtering friends with query: " + query);
        
        if (adapter == null) {
            Log.e(TAG, "Adapter is null, cannot filter");
            return;
        }
        
        // If the query is empty, show all friends
        if (query == null || query.isEmpty()) {
            // Make sure we're not setting an empty list
            if (currentFriendsList != null && !currentFriendsList.isEmpty()) {
                recyclerViewFriends.setVisibility(View.VISIBLE);
                tvEmptyFriendsList.setVisibility(View.GONE);
                adapter.setFriendsList(currentFriendsList);
            } else {
                recyclerViewFriends.setVisibility(View.GONE);
                tvEmptyFriendsList.setVisibility(View.VISIBLE);
                tvEmptyFriendsList.setText("You don't have any friends yet");
            }
            return;
        }
        
        // Use the adapter's filter
        adapter.getFilter().filter(query);
        
        // Update UI based on filter results
        // We'll let the adapter's filter notify data set changed
    }
    
    @Override
    public void onFriendClickListener(FriendItem friend, int position) {
        // Check if authenticated
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "You need to be logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading
        progressBarFriends.setVisibility(View.VISIBLE);
        
        // Get current user ID
        String currentUserId = auth.getCurrentUser().getUid();
        String friendId = friend.getUid();
        
        // Find existing conversation or create new one
        findOrCreateConversation(currentUserId, friendId, friend);
    }
    
    private void findOrCreateConversation(String currentUserId, String friendId, FriendItem friend) {
        // Check conversations in Firebase to see if one already exists between these users
        DatabaseReference conversationsRef = FirebaseDatabase.getInstance().getReference("conversations");
        
        conversationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String existingConversationId = null;
                
                // Look for an existing conversation between these two users
                for (DataSnapshot conversationSnap : snapshot.getChildren()) {
                    String conversationId = conversationSnap.getKey();
                    String user1Id = conversationSnap.child("user1_id").getValue(String.class);
                    String user2Id = conversationSnap.child("user2_id").getValue(String.class);
                    System.out.println();
                    // Check if this conversation is between the current user and the selected friend
                    // Need to check both possibilities: (currentUser=user1 & friend=user2) OR (currentUser=user2 & friend=user1)
                    if ((currentUserId.equals(user1Id) && friendId.equals(user2Id)) || 
                        (currentUserId.equals(user2Id) && friendId.equals(user1Id))) {
                        existingConversationId = conversationId;
                        Log.d(TAG, "Found existing conversation: " + existingConversationId);
                        break;
                    }
                }
                
                if (existingConversationId != null) {
                    // Existing conversation found, navigate to ChatActivity
                    navigateToChatActivity(existingConversationId, friend);
                } else {
                    // No existing conversation, create a new one
                    Log.d(TAG, "No existing conversation found, creating new one");
                    createNewConversation(currentUserId, friendId, friend);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBarFriends.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error checking conversations: " + error.getMessage());
            }
        });
    }
    
    private void createNewConversation(String currentUserId, String friendId, FriendItem friend) {
        DatabaseReference conversationsRef = FirebaseDatabase.getInstance().getReference("conversations");
        
        // Generate a new conversation ID
        String newConversationId = conversationsRef.push().getKey();
        
        if (newConversationId != null) {
            // Create conversation data
            Map<String, Object> conversationData = new HashMap<>();
            conversationData.put("user1_id", currentUserId);
            conversationData.put("user2_id", friendId);
            conversationData.put("last_message", ""); // No messages yet
            conversationData.put("last_message_time", String.valueOf(System.currentTimeMillis()));
            
            // Save to Firebase
            conversationsRef.child(newConversationId).setValue(conversationData)
                .addOnSuccessListener(aVoid -> {
                    // Navigate to chat activity with the new conversation
                    navigateToChatActivity(newConversationId, friend);
                })
                .addOnFailureListener(e -> {
                    progressBarFriends.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Failed to create conversation: " + e.getMessage(), 
                                  Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error creating conversation: " + e.getMessage());
                });
        } else {
            progressBarFriends.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Failed to generate conversation ID", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Failed to generate conversation ID");
        }
    }
    
    private void navigateToChatActivity(String conversationId, FriendItem friend) {
        progressBarFriends.setVisibility(View.GONE);
        
        // Navigate to ChatActivity with necessary data
        Intent chatIntent = new Intent(getActivity(), ChatActivity.class);
        chatIntent.putExtra("conversationId", conversationId);
        chatIntent.putExtra("friendId", friend.getUid());
        chatIntent.putExtra("friendUsername", friend.getName());
        chatIntent.putExtra("friendProfilePicUrl", friend.getProfileImageUrl());
        startActivity(chatIntent);
    }
    
    @Override
    public void onDeleteButtonClick(FriendItem friend, int position) {
        // Show confirmation dialog before deleting
        new AlertDialog.Builder(requireContext())
            .setTitle("Remove Friend")
            .setMessage("Are you sure you want to remove " + friend.getName() + " from your friends list?")
            .setPositiveButton("Remove", (dialog, which) -> {
                removeFriend(friend, position);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void removeFriend(FriendItem friend, int position) {
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "User not logged in");
            return;
        }

        String currentUserId = auth.getCurrentUser().getUid();
        String friendId = friend.getUid();
        
        // Delete from both users' friends lists
        DatabaseReference currentUserFriendsRef = databaseRef.child("users").child(currentUserId).child("friends").child(friendId);
        DatabaseReference friendUserFriendsRef = databaseRef.child("users").child(friendId).child("friends").child(currentUserId);
        
        // Add loading indicator or disable UI to prevent multiple clicks
        progressBarFriends.setVisibility(View.VISIBLE);
        
        // Store a local copy of the friend to be removed
        final FriendItem friendToRemove = friend;
        
        // Remove from current user's friends
        currentUserFriendsRef.removeValue()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Friend removed from current user's list");
                
                // Remove from friend's friends list
                friendUserFriendsRef.removeValue()
                    .addOnSuccessListener(aVoid2 -> {
                        Log.d(TAG, "Current user removed from friend's list");
                        
                        // First, remove from our cached list to maintain data consistency
                        int indexToRemove = -1;
                        for (int i = 0; i < currentFriendsList.size(); i++) {
                            if (currentFriendsList.get(i).getUid().equals(friendId)) {
                                indexToRemove = i;
                                break;
                            }
                        }
                        
                        if (indexToRemove != -1) {
                            currentFriendsList.remove(indexToRemove);
                        }
                        
                        // Now update the adapter with the full current list
                        // This is safer than trying to remove by position which may have changed
                        if (currentFriendsList.isEmpty()) {
                            recyclerViewFriends.setVisibility(View.GONE);
                            tvEmptyFriendsList.setVisibility(View.VISIBLE);
                            tvEmptyFriendsList.setText("You don't have any friends yet");
                            adapter.setFriendsList(new ArrayList<>());
                        } else {
                            // Update adapter with the current list
                            adapter.setFriendsList(new ArrayList<>(currentFriendsList));
                        }
                        
                        // Hide progress indicator
                        progressBarFriends.setVisibility(View.GONE);

                        // Show success message
                        Toast.makeText(getContext(), "Friend removed successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        // Hide progress indicator
                        progressBarFriends.setVisibility(View.GONE);
                        
                        Log.e(TAG, "Error removing current user from friend's list", e);
                        Toast.makeText(getContext(), "Error removing friend: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                // Hide progress indicator
                progressBarFriends.setVisibility(View.GONE);
                
                Log.e(TAG, "Error removing friend from current user's list", e);
                Toast.makeText(getContext(), "Error removing friend: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
}
