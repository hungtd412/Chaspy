package com.example.chaspy.ui.view;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chaspy.R;
import com.example.chaspy.data.model.Conversation;
import com.example.chaspy.ui.adapter.ConversationAdapter;
import com.example.chaspy.ui.viewmodel.ConversationViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class ConversationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ConversationAdapter adapter;
    private ConversationViewModel mainViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerViewConversations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize ViewModel
        mainViewModel = new ViewModelProvider(this).get(ConversationViewModel.class);

        // Initialize Adapter with click listener
        adapter = new ConversationAdapter(new ConversationAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Conversation conversation) {
                Toast.makeText(ConversationActivity.this, 
                    "Clicked on conversation with " + conversation.getFriendUsername(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(adapter);

        // Observe LiveData from ViewModel
        mainViewModel.getConversations().observe(this, new Observer<List<Conversation>>() {
            @Override
            public void onChanged(List<Conversation> conversations) {
                if (conversations != null) {
                    adapter.submitList(conversations);
                }
            }
        });

        mainViewModel.getError().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String error) {
                Toast.makeText(ConversationActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        // Load conversations for a user (use the actual user ID here)
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//        String userId = user != null ? user.getUid() : null;
        String userId = "jveRWKkaEVcbdiQuw9MsF4Rfobm2";
        mainViewModel.loadConversations(userId);
    }
}
