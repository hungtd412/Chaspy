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
import java.util.Locale;

public class ConversationAdapter extends ListAdapter<Conversation, ConversationAdapter.ConversationViewHolder> {

    private OnItemClickListener onItemClickListener;

    // DiffCallback for efficient updates
    private static final DiffUtil.ItemCallback<Conversation> DIFF_CALLBACK = new DiffUtil.ItemCallback<Conversation>() {
        @Override
        public boolean areItemsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
            // Assuming Conversation has an ID that uniquely identifies it
            return oldItem.getConversationId().equals(newItem.getConversationId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
            // Compare all relevant fields to determine if content has changed
            return oldItem.getLastMessage().equals(newItem.getLastMessage()) &&
                   oldItem.getLastMessageTime() == newItem.getLastMessageTime();
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
        holder.friendUsernameTextView.setText(conversation.getFriendUsername());
        holder.lastMessageTextView.setText(conversation.getLastMessage());
        
        // Format the timestamp to relative time format (e.g., "10 phút trước" or "2 ngày trước")
        holder.lastMessageTimeTextView.setText(formatTimestamp(conversation.getLastMessageTime()));

        // Load the profile picture using Picasso
        Picasso.get().load(conversation.getProfilePicUrl()).into(holder.profilePicImageView);

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
