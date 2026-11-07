package com.arohi.healthcare.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.databinding.ItemAiChatMessageBinding;
import com.arohi.healthcare.models.AiChatMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter for displaying chat messages between User and AROHI AI Healthcare Assistant.
 */
public class AiChatAdapter extends RecyclerView.Adapter<AiChatAdapter.AiChatViewHolder> {

    private final List<AiChatMessage> items = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public void addMessage(@NonNull AiChatMessage message) {
        items.add(message);
        notifyItemInserted(items.size() - 1);
    }

    public void submitList(@NonNull List<AiChatMessage> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AiChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAiChatMessageBinding binding = ItemAiChatMessageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new AiChatViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AiChatViewHolder holder, int position) {
        holder.bind(items.get(position), timeFormat);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AiChatViewHolder extends RecyclerView.ViewHolder {

        private final ItemAiChatMessageBinding binding;

        public AiChatViewHolder(@NonNull ItemAiChatMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(@NonNull AiChatMessage msg, @NonNull SimpleDateFormat timeFormat) {
            String timeStr = timeFormat.format(new Date(msg.getTimestamp()));

            if (msg.isAi()) {
                binding.layoutUserMessage.setVisibility(View.GONE);
                binding.layoutAiMessage.setVisibility(View.VISIBLE);

                binding.tvAiMessageText.setText(msg.getText());
                binding.tvAiTimestamp.setText(timeStr);

                if (msg.isDemoMode()) {
                    binding.tvAiDemoBadge.setVisibility(View.VISIBLE);
                } else {
                    binding.tvAiDemoBadge.setVisibility(View.GONE);
                }

                if (msg.getDisclaimer() != null && !msg.getDisclaimer().isEmpty()) {
                    binding.tvAiDisclaimer.setVisibility(View.VISIBLE);
                    binding.tvAiDisclaimer.setText(msg.getDisclaimer());
                } else {
                    binding.tvAiDisclaimer.setVisibility(View.GONE);
                }
            } else {
                binding.layoutAiMessage.setVisibility(View.GONE);
                binding.layoutUserMessage.setVisibility(View.VISIBLE);

                binding.tvUserMessageText.setText(msg.getText());
                binding.tvUserTimestamp.setText(timeStr);
            }
        }
    }
}
