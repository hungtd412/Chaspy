package com.example.chaspy.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chaspy.R;
import com.example.chaspy.data.model.User;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    Context mainActivity;
    ArrayList<User> usersArrayList;

    public UserAdapter(Context mainActivity, ArrayList<User> usersArrayList) {
        this.mainActivity = mainActivity;
        this.usersArrayList = usersArrayList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mainActivity).inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = usersArrayList.get(position);
        holder.getUserName().setText(user.getUserName());
        holder.getUserStatus().setText(user.getUid());
        Picasso.get().load(user.getProfilePicUrl()).into(holder.getUserImg());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent intent = new Intent(mainActivity, ChatWindow.class);
//                intent.putExtra("nameeee", user.getUserName());
//                intent.putExtra("reciverImg", user.getProfilePicUrl());
//                intent.putExtra("uid", user.getUserId());
//                mainActivity.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return usersArrayList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        private ImageView userImg;
        private TextView userName, userStatus;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userImg = itemView.findViewById(R.id.userImg);
            userName = itemView.findViewById(R.id.userName);
            userStatus = itemView.findViewById(R.id.userStatus);
        }

        public ImageView getUserImg() {
            return userImg;
        }

        public TextView getUserName() {
            return userName;
        }

        public TextView getUserStatus() {
            return userStatus;
        }
    }
}
