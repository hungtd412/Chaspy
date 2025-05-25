package com.example.chaspy.ui.view.fragment;

import android.os.Bundle;
import android.util.Log;
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
import com.example.chaspy.ui.adapter.FriendRequestAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FriendRequestsFragment extends Fragment implements FriendRequestAdapter.OnRequestActionListener {
    
    private static final String TAG = "FriendRequestsFragment";
    private RecyclerView recyclerViewFriendRequests;
    private TextView emptyViewFriendRequests;
    private View loadingIndicator;
    private FriendRequestAdapter adapter;
    
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friend_requests, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Firebase components
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        
        // Initialize views
        recyclerViewFriendRequests = view.findViewById(R.id.recyclerViewFriendRequests);
        emptyViewFriendRequests = view.findViewById(R.id.emptyViewFriendRequests);
        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        
        // Set up RecyclerView
        setupRecyclerView();
        
        // Load friend requests
        showLoading(true);
        loadFriendRequests();
    }
    
    private void setupRecyclerView() {
        recyclerViewFriendRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FriendRequestAdapter(getContext(), this);
        recyclerViewFriendRequests.setAdapter(adapter);
    }
    
    private void loadFriendRequests() {
        if (firebaseAuth.getCurrentUser() == null) {
            Log.d(TAG, "No current user found");
            updateEmptyView(true);
            showLoading(false);
            return;
        }
        
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        Log.d(TAG, "Current User ID: " + currentUserId);
        DatabaseReference requestsRef = databaseReference.child("friend_requests");
        
        requestsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "Friend requests data changed, snapshot count: " + dataSnapshot.getChildrenCount());
                List<FriendRequest> requests = new ArrayList<>();
                
                for (DataSnapshot requestSnapshot : dataSnapshot.getChildren()) {
                    String requestId = requestSnapshot.getKey();
                    Log.d(TAG, "Processing request ID: " + requestId);
                    
                    if (requestSnapshot.child("receiver_id").exists() && 
                        requestSnapshot.child("sender_id").exists() && 
                        requestSnapshot.child("status").exists()) {
                        
                        String receiverId = requestSnapshot.child("receiver_id").getValue(String.class);
                        String senderId = requestSnapshot.child("sender_id").getValue(String.class);
                        String status = requestSnapshot.child("status").getValue(String.class);
                        
                        Log.d(TAG, "Request details - Receiver: " + receiverId + ", Sender: " + 
                              senderId + ", Status: " + status);
                        
                        // Only show pending requests where current user is receiver
                        if (currentUserId.equals(receiverId) && "pending".equals(status)) {
                            Log.d(TAG, "Found pending request for current user");
                            FriendRequest request = new FriendRequest(requestId, senderId, receiverId, status);
                            
                            // Fetch sender's details
                            loadSenderDetails(request, requests);
                        }
                    } else {
                        Log.w(TAG, "Skipping malformed request: " + requestId);
                    }
                }
                
                if (requests.isEmpty()) {
                    Log.d(TAG, "No pending friend requests found");
                }
                
                // Show empty view if no requests are found immediately 
                // (loadSenderDetails will update this again when all details are loaded)
                updateEmptyView(requests.isEmpty());
                showLoading(false);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load requests: " + databaseError.getMessage(), databaseError.toException());
                Toast.makeText(getContext(), "Failed to load requests: " + databaseError.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                updateEmptyView(true);
                showLoading(false);
            }
        });
    }
    
    private void loadSenderDetails(FriendRequest request, List<FriendRequest> requestsList) {
        DatabaseReference userRef = databaseReference.child("users").child(request.getSenderId());
        Log.d(TAG, "Loading sender details for user ID: " + request.getSenderId());
        
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d(TAG, "Sender user data found");
                    String firstName = dataSnapshot.child("firstName").getValue(String.class);
                    String lastName = dataSnapshot.child("lastName").getValue(String.class);
                    String profilePicUrl = dataSnapshot.child("profilePicUrl").getValue(String.class);
                    
                    request.setSenderName(firstName + " " + lastName);
                    request.setSenderProfilePicUrl(profilePicUrl);
                    
                    // Add to the list and update adapter
                    requestsList.add(request);
                    Log.d(TAG, "Added request to list, current size: " + requestsList.size());
                    
                    // Update the adapter with the current list
                    adapter.setFriendRequests(new ArrayList<>(requestsList));
                    
                    // Update empty view after all requests are processed
                    updateEmptyView(requestsList.isEmpty());
                    showLoading(false);
                } else {
                    Log.w(TAG, "Sender user data not found for ID: " + request.getSenderId());
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load sender details: " + databaseError.getMessage());
                Toast.makeText(getContext(), "Failed to load sender details", Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        });
    }
    
    private void updateEmptyView(boolean isEmpty) {
        if (isEmpty) {
            Log.d(TAG, "Showing empty view");
            recyclerViewFriendRequests.setVisibility(View.GONE);
            emptyViewFriendRequests.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG, "Showing recycler view");
            recyclerViewFriendRequests.setVisibility(View.VISIBLE);
            emptyViewFriendRequests.setVisibility(View.GONE);
        }
    }
    
    private void showLoading(boolean isLoading) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }
    
    public void searchRequests(String query) {
        Log.d(TAG, "Searching friend requests with query: " + query);
        if (adapter != null) {
            adapter.filterRequests(query);
            
            // Update empty view based on filtered results
            updateEmptyView(adapter.getFriendRequests().isEmpty());
        }
    }
    
    @Override
    public void onAcceptRequest(FriendRequest request, int position) {
        // Accept friend request logic
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        String requestId = request.getRequestId();
        String senderId = request.getSenderId();
        
        // Update request status in database
        DatabaseReference requestRef = databaseReference.child("friend_requests").child(requestId);
        requestRef.child("status").setValue("accepted");
        
        // Add to friends lists for both users
        DatabaseReference senderFriendsRef = databaseReference.child("users").child(senderId).child("friends");
        senderFriendsRef.child(currentUserId).setValue(true);
        
        DatabaseReference receiverFriendsRef = databaseReference.child("users").child(currentUserId).child("friends");
        receiverFriendsRef.child(senderId).setValue(true)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Friend request accepted", Toast.LENGTH_SHORT).show();
                adapter.removeFriendRequest(position);
                updateEmptyView(adapter.getFriendRequests().isEmpty());
            })
            .addOnFailureListener(e -> Toast.makeText(getContext(), 
                    "Failed to accept request: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    
    @Override
    public void onRejectRequest(FriendRequest request, int position) {
        // Reject friend request logic
        String requestId = request.getRequestId();
        
        // Update request status in database
        DatabaseReference requestRef = databaseReference.child("friend_requests").child(requestId);
        requestRef.child("status").setValue("rejected")
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Friend request rejected", Toast.LENGTH_SHORT).show();
                adapter.removeFriendRequest(position);
                updateEmptyView(adapter.getFriendRequests().isEmpty());
            })
            .addOnFailureListener(e -> Toast.makeText(getContext(), 
                    "Failed to reject request: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
