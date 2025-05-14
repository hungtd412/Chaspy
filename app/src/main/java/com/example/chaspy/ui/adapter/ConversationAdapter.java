package com.example.chaspy.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chaspy.R;
import com.example.chaspy.data.model.Conversation;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends ListAdapter<Conversation, ConversationAdapter.ConversationViewHolder> {

    private OnItemClickListener onItemClickListener;

    // DiffCallback for efficient updates
    private static final DiffUtil.ItemCallback<Conversation> DIFF_CALLBACK = new DiffUtil.ItemCallback<Conversation>() {
        @Override
        public boolean areItemsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
            return oldItem.getConversationId().equals(newItem.getConversationId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
            // Check all relevant fields to avoid unnecessary updates
            return oldItem.getLastMessage().equals(newItem.getLastMessage()) &&
                   oldItem.getLastMessageTime().equals(newItem.getLastMessageTime()) &&
                   oldItem.getFriendUsername().equals(newItem.getFriendUsername()) &&
                   oldItem.getProfilePicUrl().equals(newItem.getProfilePicUrl());
        }

        @Override
        public Object getChangePayload(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
            // Return a non-null value to enable partial view updates
            // This tells RecyclerView that we want to do partial rebinding
            if (!oldItem.getLastMessage().equals(newItem.getLastMessage()) || 
                !oldItem.getLastMessageTime().equals(newItem.getLastMessageTime())) {
                return true;
            }
            return null;
        }
    };

    // Constructor
    public ConversationAdapter() {
        super(DIFF_CALLBACK);
    }
    
    public ConversationAdapter(OnItemClickListener onItemClickListener) {
        super(DIFF_CALLBACK);
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public ConversationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.conversation_item, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ConversationViewHolder holder, int position) {
        Conversation conversation = getItem(position);
        bindViewHolder(holder, conversation);
    }
    
    @Override
    public void onBindViewHolder(ConversationViewHolder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) {
            // Full rebind if no payload
            onBindViewHolder(holder, position);
        } else {
            // Partial rebind
            Conversation conversation = getItem(position);
            
            // Only update the message and time
            holder.lastMessageTextView.setText(conversation.getLastMessage());
            holder.lastMessageTimeTextView.setText(formatTimestamp(conversation.getLastMessageTime()));
        }
    }
    
    private void bindViewHolder(ConversationViewHolder holder, Conversation conversation) {
        holder.friendUsernameTextView.setText(conversation.getFriendUsername());
        holder.lastMessageTextView.setText(conversation.getLastMessage());
        holder.lastMessageTimeTextView.setText(formatTimestamp(conversation.getLastMessageTime()));

        // Load the profile picture using Picasso with caching
        if (conversation.getProfilePicUrl() != null && !conversation.getProfilePicUrl().isEmpty()) {
            Picasso.get()
                .load(conversation.getProfilePicUrl())
                .placeholder(R.drawable.default_profile) // Add a default profile placeholder
                .error(R.drawable.default_profile) // Same for error state
                .into(holder.profilePicImageView);
        } else {
            // Set default image if no profile pic URL
            holder.profilePicImageView.setImageResource(R.drawable.default_profile);
        }

        // Set the click listener for the item
        if (onItemClickListener != null) {
            holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(conversation));
        }
    }
    
    /**
     * Formats a timestamp string to a readable relative time format
     * @param timestamp The timestamp string to format
     * @return Formatted time string in Vietnamese relative format (e.g., "10:30 AM" for today or "2 ngày trước")
     */
    private String formatTimestamp(String timestamp) {
        try {
            // Parse the timestamp string to a Date object
            Date messageDate;
            if (timestamp.matches("\\d+")) {
                // If timestamp is a numeric string (milliseconds)
                long millis = Long.parseLong(timestamp);
                messageDate = new Date(millis);
            } else {
                // If timestamp is in some other format, try to parse it
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                messageDate = inputFormat.parse(timestamp);
            }
            
            // Get current time
            Date currentDate = new Date();
            
            // Calculate time difference in milliseconds
            long diffInMillis = currentDate.getTime() - messageDate.getTime();
            
            // Convert to minutes
            long diffInMinutes = diffInMillis / (60 * 1000);
            
            // If less than 24 hours, show the actual time in h:mm a format (with AM/PM)
            if (diffInMinutes < 24 * 60) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                return timeFormat.format(messageDate);
            } else {
                // Otherwise show as "X ngày trước"
                long diffInDays = diffInMinutes / (24 * 60);
                return diffInDays + " ngày trước";
            }
            
        } catch (ParseException | NumberFormatException e) {
            // If parsing fails, return the original timestamp
            return timestamp;
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public static class ConversationViewHolder extends RecyclerView.ViewHolder {

        ImageView profilePicImageView;
        TextView friendUsernameTextView, lastMessageTextView, lastMessageTimeTextView;

        public ConversationViewHolder(View itemView) {
            super(itemView);
            profilePicImageView = itemView.findViewById(R.id.profilePicImageView);
            friendUsernameTextView = itemView.findViewById(R.id.friendUsernameTextView);
            lastMessageTextView = itemView.findViewById(R.id.lastMessageTextView);
            lastMessageTimeTextView = itemView.findViewById(R.id.lastMessageTimeTextView);
        }
    }

    // Interface for item click listener
    public interface OnItemClickListener {
        void onItemClick(Conversation conversation);
    }
}
