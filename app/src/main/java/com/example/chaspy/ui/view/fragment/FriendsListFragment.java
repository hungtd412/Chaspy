package com.example.chaspy.ui.view.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chaspy.R;
import com.example.chaspy.data.model.FriendItem;
import com.example.chaspy.data.model.User;
import com.example.chaspy.ui.adapter.FriendsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
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
        String currentUserId = auth.getCurrentUser().getUid();
        
        // Show progress
        progressBarFriends.setVisibility(View.VISIBLE);
        
        // Reference to current user's friends
        DatabaseReference userRef = databaseRef.child("users").child(currentUserId);
        
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User currentUser = snapshot.getValue(User.class);
                
                if (currentUser != null && currentUser.getFriends() != null && !currentUser.getFriends().isEmpty()) {
                    // Get friends UIDs from user's friends map
                    Map<String, Boolean> friendsMap = currentUser.getFriends();
                    fetchFriendsData(friendsMap);
                } else {
                    // No friends found
                    progressBarFriends.setVisibility(View.GONE);
                    recyclerViewFriends.setVisibility(View.GONE);
                    tvEmptyFriendsList.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching user data: " + error.getMessage());
                progressBarFriends.setVisibility(View.GONE);
            }
        });
    }
    
    private void fetchFriendsData(Map<String, Boolean> friendsMap) {
        List<FriendItem> friendsList = new ArrayList<>();
        DatabaseReference usersRef = databaseRef.child("users");
        
        // Counter to track loaded friends
        final int[] friendsToLoad = {friendsMap.size()};
        final int[] loadedFriends = {0};
        
        for (String friendId : friendsMap.keySet()) {
            usersRef.child(friendId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User friendUser = snapshot.getValue(User.class);
                    if (friendUser != null) {
                        FriendItem friend = new FriendItem(
                                friendId, // Setting the UID explicitly
                                friendUser.getFullName(),
                                friendUser.getEmail(),
                                friendUser.getProfilePicUrl(),
                                friendUser.isIs_active() // This field name remains unchanged
                        );
                        friendsList.add(friend);
                    }
                    
                    loadedFriends[0]++;
                    
                    // Check if all friends are loaded
                    if (loadedFriends[0] >= friendsToLoad[0]) {
                        updateUI(friendsList);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading friend data: " + error.getMessage());
                    loadedFriends[0]++;
                    
                    // Check if all attempts to load friends are completed
                    if (loadedFriends[0] >= friendsToLoad[0]) {
                        updateUI(friendsList);
                    }
                }
            });
        }
    }
    
    private void updateUI(List<FriendItem> friendsList) {
        progressBarFriends.setVisibility(View.GONE);
        
        if (friendsList.isEmpty()) {
            recyclerViewFriends.setVisibility(View.GONE);
            tvEmptyFriendsList.setVisibility(View.VISIBLE);
        } else {
            recyclerViewFriends.setVisibility(View.VISIBLE);
            tvEmptyFriendsList.setVisibility(View.GONE);
            adapter.setFriendsList(friendsList);
        }
    }
    
    @Override
    public void onChatButtonClick(FriendItem friend) {
        // TODO: Navigate to chat with this friend
        // For now just log the action
        Log.d(TAG, "Opening chat with: " + friend.getName());
    }
    
    public void filterFriends(String query) {
        if (adapter != null) {
            adapter.getFilter().filter(query);
        }
    }
}
