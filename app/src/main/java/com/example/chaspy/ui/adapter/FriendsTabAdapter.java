package com.example.chaspy.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.chaspy.ui.view.fragment.FriendsListFragment;

public class FriendsTabAdapter extends FragmentStateAdapter {
    private static final int NUM_TABS = 3;
    private static final int TAB_ADD = 0;
    private static final int TAB_REQUESTS = 1;
    private static final int TAB_FRIENDS = 2;
    
    // Cache the fragments for search functionality
    private Fragment[] fragments = new Fragment[NUM_TABS];

    public FriendsTabAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        
        switch (position) {
            case TAB_ADD:
                // TODO: Implement AddFriendFragment
                fragment = new Fragment(); // Placeholder
                break;
            case TAB_REQUESTS:
                // TODO: Implement FriendRequestsFragment
                fragment = new Fragment(); // Placeholder
                break;
            case TAB_FRIENDS:
                fragment = new FriendsListFragment();
                break;
            default:
                fragment = new Fragment(); // Placeholder
        }
        
        // Cache the fragment
        fragments[position] = fragment;
        return fragment;
    }

    @Override
    public int getItemCount() {
        return NUM_TABS;
    }
    
    /**
     * Get the specific fragment at the position
     * Used for communicating with fragments (e.g., search functionality)
     */
    public Fragment getFragmentAt(int position) {
        return fragments[position];
    }
}
