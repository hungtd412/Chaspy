package com.example.chaspy.ui.view;


import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chaspy.R;
import com.example.chaspy.data.model.Conversation;
import com.example.chaspy.ui.adapter.ConversationAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ConversationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ConversationAdapter adapter;
    private List<Conversation> conversationList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        recyclerView = findViewById(R.id.recyclerViewConversations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize conversation list
        conversationList = new ArrayList<>();
        adapter = new ConversationAdapter(conversationList);
        recyclerView.setAdapter(adapter);

        // Load conversations from Firebase
        loadConversations("QwXLLF863qP30xOhpNDt0zzgeLo2"); // Example user ID
    }

    private void loadConversations(final String userId) {
        FirebaseDatabase.getInstance().getReference("conversations")
                .orderByChild("last_message_time")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        conversationList.clear(); // Clear the list before adding new data

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String user1Id = snapshot.child("user1_id").getValue(String.class);
                            String user2Id = snapshot.child("user2_id").getValue(String.class);
                            String lastMessageTime = snapshot.child("last_message_time").getValue(String.class);

                            // Check if the conversation involves the given user
                            if (user1Id.equals(userId) || user2Id.equals(userId)) {
                                String friendId = user1Id.equals(userId) ? user2Id : user1Id;
                                // Fetch friend details like username and profile picture URL
                                FirebaseDatabase.getInstance().getReference("users")
                                        .child(friendId)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot userSnapshot) {
                                                String friendUsername = userSnapshot.child("first_name").getValue(String.class);
                                                String profilePicUrl = userSnapshot.child("profilePicUrl").getValue(String.class);

                                                Conversation conversation = new Conversation(
                                                        snapshot.getKey(),
                                                        friendUsername,
                                                        profilePicUrl,
                                                        snapshot.child("last_message").getValue(String.class),
                                                        lastMessageTime
                                                );

                                                conversationList.add(conversation);
                                                adapter.notifyDataSetChanged(); // Notify adapter to update the UI
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {
                                                Toast.makeText(ConversationsActivity.this, "Error loading data", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(ConversationsActivity.this, "Error loading data", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
