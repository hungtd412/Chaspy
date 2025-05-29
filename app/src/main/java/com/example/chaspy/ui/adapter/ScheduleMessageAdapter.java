package com.example.chaspy.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chaspy.R;
import com.example.chaspy.data.model.ScheduleMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScheduleMessageAdapter extends RecyclerView.Adapter<ScheduleMessageAdapter.ViewHolder> {

    private List<ScheduleMessage> scheduleMessages;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onDeleteClick(ScheduleMessage message);
    }

    public ScheduleMessageAdapter() {
        this.scheduleMessages = new ArrayList<>();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setScheduledMessages(List<ScheduleMessage> scheduleMessages) {
        this.scheduleMessages = new ArrayList<>(scheduleMessages);
        notifyDataSetChanged();
    }

    public void removeMessage(ScheduleMessage message) {
        int position = scheduleMessages.indexOf(message);
        if (position != -1) {
            scheduleMessages.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_send_later, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduleMessage message = scheduleMessages.get(position);
        holder.bind(message, listener);
    }

    @Override
    public int getItemCount() {
        return scheduleMessages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMessageContent;
        private final TextView tvScheduleTime;
        private final ImageView btnCancel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageContent = itemView.findViewById(R.id.tvMessageContent);
            tvScheduleTime = itemView.findViewById(R.id.tvScheduleTime);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }

        public void bind(ScheduleMessage message, OnItemClickListener listener) {
            tvMessageContent.setText(message.getMessageContent());

            // Format date as "HH:mm dd/MM/yyyy"
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
            String formattedDate = sdf.format(new Date(message.getSendingTime()));
            tvScheduleTime.setText(formattedDate);

            // Set delete button click listener
            btnCancel.setOnClickListener(v -> {
                if (listener != null && message != null) {
                    listener.onDeleteClick(message);
                }
            });
        }
    }
}