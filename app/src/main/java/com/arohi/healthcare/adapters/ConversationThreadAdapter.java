package com.arohi.healthcare.adapters;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.databinding.ItemConversationThreadBinding;
import com.arohi.healthcare.models.ConversationThread;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * RecyclerView Adapter for displaying authorized one-to-one doctor-patient conversations.
 */
public class ConversationThreadAdapter extends RecyclerView.Adapter<ConversationThreadAdapter.ThreadHolder> {

    public interface OnConversationClickListener {
        void onConversationSelected(@NonNull ConversationThread thread);
    }

    private final boolean isDoctorViewer;
    private final OnConversationClickListener listener;
    private final List<ConversationThread> items = new ArrayList<>();

    public ConversationThreadAdapter(boolean isDoctorViewer,
                                     @NonNull OnConversationClickListener listener) {
        this.isDoctorViewer = isDoctorViewer;
        this.listener = listener;
    }

    public void submitList(@NonNull List<ConversationThread> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ThreadHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemConversationThreadBinding binding = ItemConversationThreadBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ThreadHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ThreadHolder holder, int position) {
        holder.bind(items.get(position), isDoctorViewer, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ThreadHolder extends RecyclerView.ViewHolder {
        private final ItemConversationThreadBinding binding;

        ThreadHolder(@NonNull ItemConversationThreadBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull ConversationThread thread,
                  boolean isDoctorViewer,
                  @NonNull OnConversationClickListener listener) {
            boolean isUnread;
            if (isDoctorViewer) {
                binding.tvConvParticipantName.setText(notEmpty(thread.getPatientName(), "Patient"));
                binding.tvConvSubtitle.setText(
                        "Patient ID: " + notEmpty(thread.getPatientId(), "AROHI-PAT")
                                + " • Connection: " + notEmpty(thread.getConnectionStatus(), "Accepted"));
                isUnread = thread.isUnreadByDoctor();
            } else {
                binding.tvConvParticipantName.setText(notEmpty(thread.getDoctorName(), "Dr. Specialist"));
                binding.tvConvSubtitle.setText(
                        "Specialty: " + notEmpty(thread.getDoctorSpecialty(), "Medicine")
                                + " • Connection: " + notEmpty(thread.getConnectionStatus(), "Accepted"));
                isUnread = thread.isUnreadByPatient();
            }

            binding.tvConvUnreadBadge.setVisibility(isUnread ? View.VISIBLE : View.GONE);
            binding.tvConvLastMessage.setText(
                    notEmpty(thread.getLastMessagePreview(), "Tap to open secure clinical conversation"));

            android.content.Context ctx = binding.getRoot().getContext();
            int dp = (int) ctx.getResources().getDisplayMetrics().density;
            if (isUnread) {
                binding.tvConvLastMessage.setTypeface(null, android.graphics.Typeface.BOLD);
                binding.tvConvLastMessage.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, com.arohi.healthcare.R.color.text_primary));
                binding.cardConversationItem.setStrokeColor(androidx.core.content.ContextCompat.getColor(ctx, com.arohi.healthcare.R.color.primary_moss_green));
                binding.cardConversationItem.setStrokeWidth(Math.max(2, 2 * dp));
                binding.cardConversationItem.setCardBackgroundColor(0xFFF3F8EF);
            } else {
                binding.tvConvLastMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
                binding.tvConvLastMessage.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, com.arohi.healthcare.R.color.arohi_slate_muted));
                binding.cardConversationItem.setStrokeColor(androidx.core.content.ContextCompat.getColor(ctx, com.arohi.healthcare.R.color.arohi_border_subtle));
                binding.cardConversationItem.setStrokeWidth(Math.max(1, dp));
                binding.cardConversationItem.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(ctx, com.arohi.healthcare.R.color.arohi_pure_white));
            }

            if (thread.getLastMessageTimestamp() > 0) {
                binding.tvConvTimestamp.setText(
                        DateFormat.format("MMM dd, hh:mm a", new Date(thread.getLastMessageTimestamp())));
            } else {
                binding.tvConvTimestamp.setText("");
            }

            binding.cardConversationItem.setOnClickListener(v -> listener.onConversationSelected(thread));
        }

        private String notEmpty(String val, String fallback) {
            return val != null && !val.trim().isEmpty() ? val.trim() : fallback;
        }
    }
}
