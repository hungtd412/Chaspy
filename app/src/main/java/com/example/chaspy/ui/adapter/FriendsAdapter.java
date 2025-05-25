package com.example.chaspy.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chaspy.R;
import com.example.chaspy.data.model.FriendItem;

import java.util.ArrayList;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> implements Filterable {

    private List<FriendItem> friendsList;
    private List<FriendItem> friendsListFull;
    private Context context;
    private OnFriendClickListener listener;

    public interface OnFriendClickListener {
        void onFriendClickListener(FriendItem friend, int position);
        void onDeleteButtonClick(FriendItem friend, int position);
    }

    public FriendsAdapter(Context context, OnFriendClickListener listener) {
        this.context = context;
        this.friendsList = new ArrayList<>();
        this.friendsListFull = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        FriendItem friend = friendsList.get(position);
        
        holder.tvFriendName.setText(friend.getName());
        // Don't display email as per requirements
        
        // Set online status
        if (friend.isActive()) {
            holder.tvStatus.setText("Online");
            holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            holder.tvStatus.setText("Offline");
            holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
        }
        
        // Load profile image
        if (friend.getProfileImageUrl() != null && !friend.getProfileImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(friend.getProfileImageUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(holder.ivFriendProfilePic);
        } else {
            holder.ivFriendProfilePic.setImageResource(R.drawable.ic_launcher_foreground);
        }
        
        // Set item click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFriendClickListener(friend, holder.getAdapterPosition());
            }
        });
        
        // Set delete button click listener
        holder.ivDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteButtonClick(friend, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return friendsList.size();
    }

    public void setFriendsList(List<FriendItem> friendsList) {
        // Always create new instances to avoid reference issues
        this.friendsList = new ArrayList<>(friendsList); 
        this.friendsListFull = new ArrayList<>(friendsList);
        notifyDataSetChanged();
    }

    public void removeFriend(int position) {
        if (position >= 0 && position < friendsList.size()) {
            FriendItem removedItem = friendsList.get(position);
            friendsList.remove(position);
            
            // Find and remove the item from the full list as well
            for (int i = 0; i < friendsListFull.size(); i++) {
                if (friendsListFull.get(i).getUid().equals(removedItem.getUid())) {
                    friendsListFull.remove(i);
                    break;
                }
            }
            
            notifyItemRemoved(position);
            
            // Notify data set changed only if the list is empty
            if (friendsList.isEmpty()) {
                notifyDataSetChanged();
            }
        }
    }

    @Override
    public Filter getFilter() {
        return friendsFilter;
    }

    private Filter friendsFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<FriendItem> filteredList = new ArrayList<>();
            
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(friendsListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                
                for (FriendItem item : friendsListFull) {
                    // Search only by name (not email)
                    if (item.getName().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }
            
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            friendsList.clear();
            friendsList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView tvFriendName, tvFriendEmail, tvStatus;
        ImageView ivFriendProfilePic;
        ImageView ivDelete;

        FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFriendName = itemView.findViewById(R.id.tvFriendName);
            tvFriendEmail = itemView.findViewById(R.id.tvFriendEmail);
            ivFriendProfilePic = itemView.findViewById(R.id.ivFriendProfilePic);
            ivDelete = itemView.findViewById(R.id.btnRemove);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
