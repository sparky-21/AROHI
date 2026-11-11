package com.arohi.healthcare.adapters;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemStaffConversationBinding;
import com.arohi.healthcare.models.StaffConversation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Adapter for rendering staff conversations (Groups and 1-to-1 Direct Chats)
 * in the Authority Communication Hub.
 */
public class StaffConversationAdapter extends RecyclerView.Adapter<StaffConversationAdapter.ConversationViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(@NonNull StaffConversation conversation);
    }

    private final List<StaffConversation> items = new ArrayList<>();
    private final OnConversationClickListener listener;

    public StaffConversationAdapter(@NonNull OnConversationClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<StaffConversation> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStaffConversationBinding binding = ItemStaffConversationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ConversationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        private final ItemStaffConversationBinding binding;

        ConversationViewHolder(@NonNull ItemStaffConversationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull StaffConversation conversation, @NonNull OnConversationClickListener listener) {
            Context context = binding.getRoot().getContext();

            binding.tvConversationTitle.setText(conversation.getTitle());
            binding.tvLastMessagePreview.setText(conversation.getLastMessageText() != null
                    ? conversation.getLastMessageText() : "Tap to open conversation");

            // Type badge & Avatar initials
            if (conversation.isGroup()) {
                boolean isDoctors = StaffConversation.TYPE_GROUP_DOCTORS.equals(conversation.getConversationType());
                binding.tvConversationAvatar.setText(isDoctors ? "🩺" : "💉");
                binding.tvConversationTypeBadge.setText(isDoctors ? "ALL DOCTORS" : "ALL NURSES");
                binding.tvConversationTypeBadge.setTextColor(ContextCompat.getColor(context, R.color.primary_moss_green));
            } else {
                boolean isDoctor = StaffConversation.TYPE_INDIVIDUAL_DOCTOR.equals(conversation.getConversationType());
                binding.tvConversationAvatar.setText(isDoctor ? "DR" : "NR");
                binding.tvConversationTypeBadge.setText(isDoctor ? "DOCTOR" : "NURSE");
                binding.tvConversationTypeBadge.setTextColor(ContextCompat.getColor(context, R.color.accent_heading));
            }

            // Timestamp formatting
            if (conversation.getLastMessageTimestamp() > 0) {
                binding.tvLastMessageTime.setText(DateFormat.format("hh:mm a", new Date(conversation.getLastMessageTimestamp())));
            } else {
                binding.tvLastMessageTime.setText("");
            }

            // Unread count
            if (conversation.getUnreadCount() > 0) {
                binding.tvUnreadBadge.setText(String.valueOf(conversation.getUnreadCount()));
                binding.tvUnreadBadge.setVisibility(View.VISIBLE);
            } else {
                binding.tvUnreadBadge.setVisibility(View.GONE);
            }

            binding.getRoot().setOnClickListener(v -> listener.onConversationClick(conversation));
        }
    }
}
