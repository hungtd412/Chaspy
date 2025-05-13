package com.example.chaspy.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chaspy.R;
import com.example.chaspy.data.model.Conversation;
import com.squareup.picasso.Picasso;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private List<Conversation> conversationList;

    public ConversationAdapter(List<Conversation> conversationList) {
        this.conversationList = conversationList;
    }

    @Override
    public ConversationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.conversation_item, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ConversationViewHolder holder, int position) {
        Conversation conversation = conversationList.get(position);
        holder.friendUsernameTextView.setText(conversation.getFriendUsername());
        holder.lastMessageTextView.setText(conversation.getLastMessage());
        holder.lastMessageTimeTextView.setText(String.valueOf(conversation.getLastMessageTime()));

        // Load the profile picture using Picasso
        Picasso.get().load(conversation.getProfilePicUrl()).into(holder.profilePicImageView);
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
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
}
