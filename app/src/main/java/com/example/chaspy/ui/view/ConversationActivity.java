package com.example.chaspy.ui.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.chaspy.R;
import com.example.chaspy.data.model.Conversation;
import com.example.chaspy.ui.adapter.ConversationAdapter;
import com.example.chaspy.ui.viewmodel.ConversationViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class ConversationActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    private RecyclerView recyclerView;
    private ConversationAdapter adapter;
    private ConversationViewModel mainViewModel;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SearchView searchView;
    private String currentUserId;
    private boolean isFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerViewConversations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        
        // Initialize SearchView with improved configuration
        searchView = findViewById(R.id.searchView);
        searchView.setIconifiedByDefault(false);
        searchView.setClickable(true);
        searchView.setOnQueryTextListener(this);
        
        // Make entire search view area clickable
        searchView.setOnClickListener(v -> searchView.setIconified(false));

        // Initialize ViewModel
        mainViewModel = new ViewModelProvider(this).get(ConversationViewModel.class);

        // Initialize Adapter with click listener
        adapter = new ConversationAdapter(new ConversationAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Conversation conversation) {
                // Start ChatActivity with conversation data
                Intent intent = new Intent(ConversationActivity.this, ChatActivity.class);
                intent.putExtra("conversationId", conversation.getConversationId());
                intent.putExtra("friendUsername", conversation.getFriendUsername());
                intent.putExtra("friendProfilePicUrl", conversation.getProfilePicUrl());
                intent.putExtra("friendId", conversation.getFriendId());
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);

        // Observe LiveData from ViewModel
        mainViewModel.getConversations().observe(this, new Observer<List<Conversation>>() {
            @Override
            public void onChanged(List<Conversation> conversations) {
                if (conversations != null) {
                    // Only force clear adapter on first load to prevent flashing
                    if (isFirstLoad) {
                        adapter.submitList(null);
                        isFirstLoad = false;
                    }
                    adapter.submitList(conversations);
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
        
        // Observe filtered conversations
        mainViewModel.getFilteredConversations().observe(this, new Observer<List<Conversation>>() {
            @Override
            public void onChanged(List<Conversation> filteredConversations) {
                adapter.submitList(filteredConversations);
            }
        });

        mainViewModel.getError().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String error) {
                Toast.makeText(ConversationActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        // Get current user ID
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = user != null ? user.getUid() : null;
        
        // Set up swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshConversations();
            }
        });
        
        // Load initial conversations
        loadConversations();
    }
    
    private void loadConversations() {
        if (currentUserId != null) {
            swipeRefreshLayout.setRefreshing(true);
            mainViewModel.loadConversations(currentUserId);
        } else {
            Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void refreshConversations() {
        if (currentUserId != null) {
            // Reset first load flag to prevent flickering during manual refresh
            isFirstLoad = false;
            mainViewModel.loadConversations(currentUserId);
            
            // Clear search
            searchView.setQuery("", false);
            searchView.clearFocus();
        } else {
            swipeRefreshLayout.setRefreshing(false);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Only do a full refresh if we've been away from the activity
        if (!isFirstLoad) {
            refreshConversations();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // The ViewModel will continue to listen for updates
    }
    
    // SearchView query text listener implementations
    @Override
    public boolean onQueryTextSubmit(String query) {
        mainViewModel.filterConversations(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mainViewModel.filterConversations(newText);
        return true;
    }
}
