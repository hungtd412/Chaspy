package com.example.chaspy.ui.adapter;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chaspy.R;
import com.example.chaspy.data.model.Message;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    
    private List<Message> messageList = new ArrayList<>();
    private Map<String, Integer> messagePositions = new HashMap<>();
    private String currentUserId;
    
    public MessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }
    
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        
        boolean isCurrentUserMessage = message.getSenderId().equals(currentUserId);
        
        // Show the appropriate layout based on who sent the message
        holder.layoutSent.setVisibility(isCurrentUserMessage ? View.VISIBLE : View.GONE);
        holder.layoutReceived.setVisibility(isCurrentUserMessage ? View.GONE : View.VISIBLE);
        
        // Set the message content
        if (isCurrentUserMessage) {
            holder.textViewSentMessage.setText(message.getMessageContent());
            holder.textViewSentTime.setText(formatTime(message.getTimestamp()));
        } else {
            holder.textViewReceivedMessage.setText(message.getMessageContent());
            holder.textViewReceivedTime.setText(formatTime(message.getTimestamp()));
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
    
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout layoutSent, layoutReceived;
        TextView textViewSentMessage, textViewSentTime;
        TextView textViewReceivedMessage, textViewReceivedTime;
        
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutSent = itemView.findViewById(R.id.layoutSent);
            layoutReceived = itemView.findViewById(R.id.layoutReceived);
            textViewSentMessage = itemView.findViewById(R.id.textViewSentMessage);
            textViewSentTime = itemView.findViewById(R.id.textViewSentTime);
            textViewReceivedMessage = itemView.findViewById(R.id.textViewReceivedMessage);
            textViewReceivedTime = itemView.findViewById(R.id.textViewReceivedTime);
        }
    }
}
