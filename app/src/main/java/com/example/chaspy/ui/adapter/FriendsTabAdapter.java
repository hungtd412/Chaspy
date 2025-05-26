package com.example.chaspy.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.chaspy.ui.view.fragment.AddFriendsFragment;
import com.example.chaspy.ui.view.fragment.FriendRequestsFragment;
import com.example.chaspy.ui.view.fragment.FriendsListFragment;

import java.util.HashMap;
import java.util.Map;

public class FriendsTabAdapter extends FragmentStateAdapter {

    private static final int TAB_COUNT = 3;
    private static final int TAB_ADD_FRIENDS = 0;
    private static final int TAB_FRIEND_REQUESTS = 1;
    private static final int TAB_FRIENDS_LIST = 2;

    // Cache fragments to retain their state and provide access to them
    private final Map<Integer, Fragment> fragmentMap = new HashMap<>();

    public FriendsTabAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        switch (position) {
            case TAB_ADD_FRIENDS:
                fragment = new AddFriendsFragment();
                break;
            case TAB_FRIEND_REQUESTS:
                fragment = new FriendRequestsFragment();
                break;
            case TAB_FRIENDS_LIST:
                fragment = new FriendsListFragment();
                break;
            default:
                throw new IllegalArgumentException("Invalid position: " + position);
        }

        // Store the fragment in the map
        fragmentMap.put(position, fragment);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }

    /**
     * Get the fragment at the specified position
     * @param position The tab position
     * @return The fragment at the position, or null if not yet created
     */
    public Fragment getFragmentAt(int position) {
        return fragmentMap.get(position);
    }
}
