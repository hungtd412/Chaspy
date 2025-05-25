package com.example.chaspy.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.chaspy.ui.view.fragment.AddFriendsFragment;
import com.example.chaspy.ui.view.fragment.FriendRequestsFragment;
import com.example.chaspy.ui.view.fragment.FriendsListFragment;

/**
 * Adapter for managing friend-related tabs
 */
public class FriendsTabAdapter extends FragmentStateAdapter {
    
    private static final int TAB_COUNT = 3;
    
    private Fragment[] fragments;
    
    public FriendsTabAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        
        // Initialize fragments
        fragments = new Fragment[TAB_COUNT];
        fragments[0] = new AddFriendsFragment();
        fragments[1] = new FriendRequestsFragment();
        fragments[2] = new FriendsListFragment();
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments[position];
    }
    
    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }
    
    /**
     * Get the fragment at the specified position
     */
    public Fragment getFragmentAt(int position) {
        if (position >= 0 && position < fragments.length) {
            return fragments[position];
        }
        return null;
    }
}
