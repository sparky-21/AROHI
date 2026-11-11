package com.arohi.healthcare.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemStaffChatMessageBinding;
import com.arohi.healthcare.models.StaffMessage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Phase 12 Staff Chat Message Adapter.
 * Renders incoming and outgoing chat bubbles for group and direct staff conversations.
 * Supports sender role badges, attachment previews (PDF, images), timestamps, and read receipts.
 */
public class StaffMessageAdapter extends RecyclerView.Adapter<StaffMessageAdapter.MessageViewHolder> {

    public interface OnAttachmentClickListener {
        void onAttachmentClick(@NonNull StaffMessage message);
    }

    private final List<StaffMessage> messages = new ArrayList<>();
    private final String currentUserId;
    private final OnAttachmentClickListener attachmentClickListener;

    public StaffMessageAdapter(@NonNull String currentUserId, @Nullable OnAttachmentClickListener attachmentClickListener) {
        this.currentUserId = currentUserId;
        this.attachmentClickListener = attachmentClickListener;
    }

    public void submitList(@NonNull List<StaffMessage> newMessages) {
        this.messages.clear();
        this.messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    public void addMessage(@NonNull StaffMessage message) {
        this.messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStaffChatMessageBinding binding = ItemStaffChatMessageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MessageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        holder.bind(messages.get(position), currentUserId, attachmentClickListener);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemStaffChatMessageBinding binding;

        MessageViewHolder(@NonNull ItemStaffChatMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull StaffMessage message, @NonNull String currentUserId,
                  @Nullable OnAttachmentClickListener attachmentClickListener) {
            Context context = binding.getRoot().getContext();
            boolean isOutgoing = currentUserId.equalsIgnoreCase(message.getSenderUid());

            String timeString = message.getTimestamp() > 0
                    ? DateFormat.format("hh:mm a", new Date(message.getTimestamp())).toString()
                    : "";

            boolean hasAttachment = message.getAttachmentName() != null && !message.getAttachmentName().trim().isEmpty();

            if (isOutgoing) {
                // Show Outgoing, hide Incoming
                binding.layoutOutgoingBubble.setVisibility(View.VISIBLE);
                binding.layoutIncomingBubble.setVisibility(View.GONE);

                binding.tvOutgoingMessageText.setText(message.getText());
                binding.tvOutgoingTimestamp.setText(timeString);

                // Read status indicator: ✓✓ if read by others, ✓ if sent
                boolean isReadByOthers = message.getReadByUids() != null && message.getReadByUids().size() > 1;
                binding.tvOutgoingReadStatus.setText(isReadByOthers ? "✓✓" : "✓");

                // Outgoing attachment
                if (hasAttachment) {
                    binding.layoutOutgoingAttachment.setVisibility(View.VISIBLE);
                    binding.tvOutgoingAttachmentName.setText(message.getAttachmentName());
                    binding.tvOutgoingAttachmentIcon.setText(
                            StaffMessage.ATTACHMENT_PDF.equalsIgnoreCase(message.getAttachmentType()) ? "📄 " : "🖼️ ");
                    binding.layoutOutgoingAttachment.setOnClickListener(v -> {
                        if (attachmentClickListener != null) {
                            attachmentClickListener.onAttachmentClick(message);
                        }
                    });
                } else {
                    binding.layoutOutgoingAttachment.setVisibility(View.GONE);
                }

            } else {
                // Show Incoming, hide Outgoing
                binding.layoutIncomingBubble.setVisibility(View.VISIBLE);
                binding.layoutOutgoingBubble.setVisibility(View.GONE);

                binding.tvIncomingSenderName.setText(message.getSenderName() != null ? message.getSenderName() : "Staff Member");
                binding.tvIncomingSenderRole.setText(message.getSenderRole() != null ? message.getSenderRole() : "STAFF");
                binding.tvIncomingMessageText.setText(message.getText());
                binding.tvIncomingTimestamp.setText(timeString);

                // Incoming attachment
                if (hasAttachment) {
                    binding.layoutIncomingAttachment.setVisibility(View.VISIBLE);
                    binding.tvIncomingAttachmentName.setText(message.getAttachmentName());
                    binding.tvIncomingAttachmentIcon.setText(
                            StaffMessage.ATTACHMENT_PDF.equalsIgnoreCase(message.getAttachmentType()) ? "📄 " : "🖼️ ");
                    binding.layoutIncomingAttachment.setOnClickListener(v -> {
                        if (attachmentClickListener != null) {
                            attachmentClickListener.onAttachmentClick(message);
                        }
                    });
                } else {
                    binding.layoutIncomingAttachment.setVisibility(View.GONE);
                }
            }
        }
    }
}
