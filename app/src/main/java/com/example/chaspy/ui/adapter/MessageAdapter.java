package com.example.chaspy.ui.adapter;

import android.graphics.Color;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chaspy.R;
import com.example.chaspy.data.model.Message;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messageList = new ArrayList<>();
    private Map<String, Integer> messagePositions = new HashMap<>();
    private String currentUserId;
    private String friendProfilePicUrl;
    private String themeColor = "#A9E7FD";

    public MessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public MessageAdapter(String currentUserId, String friendProfilePicUrl) {
        this.currentUserId = currentUserId;
        this.friendProfilePicUrl = friendProfilePicUrl;
    }

    public void setFriendProfilePicUrl(String url) {
        this.friendProfilePicUrl = url;
    }
    public void setThemeColor(String themeColor) {
        this.themeColor = themeColor;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_send_item, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_receive_item, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);

        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).bind(message);
        } else {
            ReceivedMessageViewHolder receivedHolder = (ReceivedMessageViewHolder) holder;
            receivedHolder.bind(message, friendProfilePicUrl, themeColor);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public void setMessages(List<Message> messages) {
        this.messageList.clear();
        this.messagePositions.clear();

        if (messages != null) {
            this.messageList.addAll(messages);
            sortMessages();

            // Update position map
            for (int i = 0; i < this.messageList.size(); i++) {
                messagePositions.put(this.messageList.get(i).getMessageId(), i);
            }
        }

        notifyDataSetChanged();
    }

    public void addMessage(Message message) {
        if (message == null || messagePositions.containsKey(message.getMessageId())) {
            return; // Skip if null or already exists
        }

        messageList.add(message);
        sortMessages();

        // Update all positions as sorting may have changed positions
        messagePositions.clear();
        for (int i = 0; i < messageList.size(); i++) {
            messagePositions.put(messageList.get(i).getMessageId(), i);
        }

        notifyDataSetChanged();
    }

    /**
     * Sort messages by timestamp to ensure chronological order (oldest first)
     */
    private void sortMessages() {
        Collections.sort(messageList, new Comparator<Message>() {
            @Override
            public int compare(Message m1, Message m2) {
                try {
                    long time1 = Long.parseLong(m1.getTimestamp());
                    long time2 = Long.parseLong(m2.getTimestamp());
                    return Long.compare(time1, time2);
                } catch (NumberFormatException e) {
                    return 0; // Keep original order if parsing fails
                }
            }
        });
    }

    private String formatTime(String timestamp) {
        try {
            // Parse the timestamp string to long
            long timeMillis = Long.parseLong(timestamp);
            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            calendar.setTimeInMillis(timeMillis);
            return DateFormat.format("hh:mm a", calendar).toString();
        } catch (NumberFormatException e) {
            // If parsing fails, return the timestamp as is or a default value
            return "Unknown time";
        }
    }

    // ViewHolder for sent messages
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMessage;
        private TextView tvTimestamp;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }

        void bind(Message message) {
            tvMessage.setText(message.getMessageContent());

            // Format and set timestamp
            try {
                long timeMillis = Long.parseLong(message.getTimestamp());
                Calendar calendar = Calendar.getInstance(Locale.getDefault());
                calendar.setTimeInMillis(timeMillis);
                String formattedTime = DateFormat.format("hh:mm a", calendar).toString();
                tvTimestamp.setText(formattedTime);
            } catch (NumberFormatException e) {
                tvTimestamp.setText("Unknown time");
            }
        }
    }

    // ViewHolder for received messages
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMessage;
        private TextView tvTimestamp;
        private ImageView ivProfilePic;
        private CardView cardView; // Add this field

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            ivProfilePic = itemView.findViewById(R.id.ivProfilePic);
            cardView = itemView.findViewById(R.id.cardView); // Initialize it here
        }

        void bind(Message message, String profilePicUrl, String themeColor) {
            tvMessage.setText(message.getMessageContent());

            // Format and set timestamp
            try {
                long timeMillis = Long.parseLong(message.getTimestamp());
                Calendar calendar = Calendar.getInstance(Locale.getDefault());
                calendar.setTimeInMillis(timeMillis);
                String formattedTime = DateFormat.format("hh:mm a", calendar).toString();
                tvTimestamp.setText(formattedTime);
            } catch (NumberFormatException e) {
                tvTimestamp.setText("Unknown time");
            }

            // Load profile picture
            if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                Picasso.get()
                        .load(profilePicUrl)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(ivProfilePic);
            } else {
                ivProfilePic.setImageResource(R.drawable.default_profile);
            }

            // Apply theme color to the card
            if (cardView != null && themeColor != null) {
                cardView.setCardBackgroundColor(Color.parseColor(themeColor));
            }
        }
    }
}