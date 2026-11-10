package com.arohi.healthcare.adapters;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.databinding.ItemChatMessageBinding;
import com.arohi.healthcare.models.ChatMessage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * RecyclerView Adapter for rendering real-time 1-to-1 doctor-patient chat messages
 * with message timestamps and read status indicators.
 */
public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageHolder> {

    private final String currentUserUid;
    private final List<ChatMessage> messages = new ArrayList<>();

    public ChatMessageAdapter(@NonNull String currentUserUid) {
        this.currentUserUid = currentUserUid;
    }

    public void submitMessages(@NonNull List<ChatMessage> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        java.util.Collections.sort(messages, (a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
        notifyDataSetChanged();
    }

    public void addOrUpdateMessage(@NonNull ChatMessage msg) {
        String id = msg.getMessageId();
        int existingIndex = -1;
        if (id != null && !id.isEmpty()) {
            for (int i = 0; i < messages.size(); i++) {
                if (id.equals(messages.get(i).getMessageId())) {
                    existingIndex = i;
                    break;
                }
            }
        }
        if (existingIndex >= 0) {
            messages.set(existingIndex, msg);
        } else {
            messages.add(msg);
        }
        java.util.Collections.sort(messages, (a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChatMessageBinding binding = ItemChatMessageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MessageHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageHolder holder, int position) {
        holder.bind(messages.get(position), currentUserUid);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageHolder extends RecyclerView.ViewHolder {
        private final ItemChatMessageBinding binding;

        MessageHolder(@NonNull ItemChatMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull ChatMessage msg, @NonNull String currentUserUid) {
            boolean isOutgoing = currentUserUid.equals(msg.getSenderUid());
            String timeStr = msg.getTimestamp() > 0
                    ? DateFormat.format("MMM dd, hh:mm a", new Date(msg.getTimestamp())).toString()
                    : "";

            if (isOutgoing) {
                binding.layoutOutgoingBubble.setVisibility(View.VISIBLE);
                binding.layoutIncomingBubble.setVisibility(View.GONE);
                binding.tvOutgoingMessageText.setText(msg.getMessageText());
                String readStatus = msg.isRead() ? "Read ✓✓" : "Sent ✓";
                binding.tvOutgoingMetaAndRead.setText(timeStr + " • " + readStatus);
            } else {
                binding.layoutIncomingBubble.setVisibility(View.VISIBLE);
                binding.layoutOutgoingBubble.setVisibility(View.GONE);
                binding.tvIncomingSenderName.setText(
                        msg.getSenderName() != null ? msg.getSenderName() : "Participant");
                binding.tvIncomingMessageText.setText(msg.getMessageText());
                binding.tvIncomingTimestamp.setText(timeStr);
            }
        }
    }
}
