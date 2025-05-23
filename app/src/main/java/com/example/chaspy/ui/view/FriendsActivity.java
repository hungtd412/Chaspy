package com.example.chaspy.ui.view;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.chaspy.R;
import com.example.chaspy.ui.adapter.FriendsTabAdapter;
import com.example.chaspy.ui.view.fragment.FriendsListFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Activity for managing user's friends list
 */
public class FriendsActivity extends AppCompatActivity {

    private TextView tvFriendsManagerTitle;
    private SearchView searchViewFriends;
    private TabLayout tabLayoutFriends;
    private ViewPager2 viewPagerFriends;
    private FriendsTabAdapter tabAdapter;
    
    // Firebase components
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);
        
        // Initialize Firebase components
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        
        // Initialize UI components
        initializeViews();
        
        // Set up tabs and ViewPager
        setupTabsAndViewPager();
        
        // Set up search functionality
        setupSearchView();
        
        // Prevent SearchView from automatically gaining focus
        preventSearchViewAutoFocus();
    }
    
    private void initializeViews() {
        tvFriendsManagerTitle = findViewById(R.id.tvFriendsManagerTitle);
        searchViewFriends = findViewById(R.id.searchViewFriends);
        tabLayoutFriends = findViewById(R.id.tabLayoutFriends);
        viewPagerFriends = findViewById(R.id.viewPagerFriends);
    }
    
    private void setupTabsAndViewPager() {
        // Initialize adapter and set it to ViewPager
        tabAdapter = new FriendsTabAdapter(this);
        viewPagerFriends.setAdapter(tabAdapter);
        
        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayoutFriends, viewPagerFriends, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Add");
                    break;
                case 1:
                    tab.setText("Requests");
                    break;
                case 2:
                    tab.setText("Friends");
                    break;
            }
        }).attach();
    }
    
    private void setupSearchView() {
        searchViewFriends.setQueryHint("Search by name");  // Updated hint
        
        searchViewFriends.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Perform search when user submits
                searchFriends(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Update search results as user types
                searchFriends(newText);
                return true;
            }
        });
    }
    
    private void preventSearchViewAutoFocus() {
        // Clear focus from SearchView and set focus to the parent layout
        searchViewFriends.clearFocus();
        
        // Optional: Set focus to the root view
        final View rootView = findViewById(android.R.id.content);
        rootView.requestFocus();
    }
    
    private void searchFriends(String query) {
        // Get current fragment and pass the query
        int currentTabPosition = viewPagerFriends.getCurrentItem();
        
        // Pass search query to appropriate fragment
        if (currentTabPosition == 2) { // Friends tab
            FriendsListFragment friendsFragment = 
                    (FriendsListFragment) tabAdapter.getFragmentAt(currentTabPosition);
            if (friendsFragment != null) {
                friendsFragment.filterFriends(query);
            }
        }
        // Add search handling for other tabs as needed
    }
}
