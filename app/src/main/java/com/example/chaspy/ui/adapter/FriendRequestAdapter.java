package com.example.chaspy.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chaspy.R;
import com.example.chaspy.data.model.FriendRequest;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.FriendRequestViewHolder> {
    
    private List<FriendRequest> friendRequests;
    private List<FriendRequest> filteredFriendRequests;
    private Context context;
    private OnRequestActionListener actionListener;
    
    public interface OnRequestActionListener {
        void onAcceptRequest(FriendRequest request, int position);
        void onRejectRequest(FriendRequest request, int position);
    }
    
    public FriendRequestAdapter(Context context, OnRequestActionListener listener) {
        this.context = context;
        this.friendRequests = new ArrayList<>();
        this.filteredFriendRequests = new ArrayList<>();
        this.actionListener = listener;
    }
    
    public void setFriendRequests(List<FriendRequest> friendRequests) {
        this.friendRequests = friendRequests;
        this.filteredFriendRequests = new ArrayList<>(friendRequests);
        notifyDataSetChanged();
    }
    
    public void addFriendRequest(FriendRequest friendRequest) {
        this.friendRequests.add(friendRequest);
        this.filteredFriendRequests.add(friendRequest);
        notifyItemInserted(filteredFriendRequests.size() - 1);
    }
    
    public void removeFriendRequest(int position) {
        if (position >= 0 && position < filteredFriendRequests.size()) {
            FriendRequest requestToRemove = filteredFriendRequests.get(position);
            filteredFriendRequests.remove(position);
            friendRequests.remove(requestToRemove);
            notifyItemRemoved(position);
        }
    }
    
    public List<FriendRequest> getFriendRequests() {
        return filteredFriendRequests;
    }
    
    @NonNull
    @Override
    public FriendRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request, parent, false);
        return new FriendRequestViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FriendRequestViewHolder holder, int position) {
        FriendRequest request = filteredFriendRequests.get(position);
        holder.bind(request, position);
    }
    
    @Override
    public int getItemCount() {
        return filteredFriendRequests.size();
    }
    
    public void filterRequests(String query) {
        filteredFriendRequests.clear();
        
        if (query == null || query.isEmpty()) {
            filteredFriendRequests.addAll(friendRequests);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (FriendRequest request : friendRequests) {
                if (request.getSenderName() != null && 
                    request.getSenderName().toLowerCase().contains(lowerCaseQuery)) {
                    filteredFriendRequests.add(request);
                }
            }
        }
        
        notifyDataSetChanged();
    }
    
    class FriendRequestViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivProfilePic;
        TextView tvName;
        Button btnAccept, btnReject;
        
        FriendRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfilePic = itemView.findViewById(R.id.ivRequestProfilePic);
            tvName = itemView.findViewById(R.id.tvRequestName);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
        
        void bind(final FriendRequest request, final int position) {
            tvName.setText(request.getSenderName());
            
            // Load profile image using Glide
            if (request.getSenderProfilePicUrl() != null && !request.getSenderProfilePicUrl().isEmpty()) {
                Glide.with(context)
                        .load(request.getSenderProfilePicUrl())
                        .placeholder(R.drawable.default_profile_image)
                        .error(R.drawable.default_profile_image)
                        .into(ivProfilePic);
            } else {
                ivProfilePic.setImageResource(R.drawable.default_profile_image);
            }
            
            // Set up button click listeners
            btnAccept.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onAcceptRequest(request, position);
                }
            });
            
            btnReject.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onRejectRequest(request, position);
                }
            });
        }
    }
}
