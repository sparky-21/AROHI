package com.arohi.healthcare.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemCalendarEventBinding;
import com.arohi.healthcare.models.CalendarEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying unified events in the synchronized medical calendar.
 */
public class CalendarEventAdapter extends RecyclerView.Adapter<CalendarEventAdapter.EventViewHolder> {

    public interface OnCalendarEventListener {
        void onEventClicked(@NonNull CalendarEvent event);
    }

    private final OnCalendarEventListener listener;
    private final List<CalendarEvent> items = new ArrayList<>();

    public CalendarEventAdapter(@NonNull OnCalendarEventListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<CalendarEvent> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCalendarEventBinding binding = ItemCalendarEventBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new EventViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final ItemCalendarEventBinding binding;

        EventViewHolder(@NonNull ItemCalendarEventBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull CalendarEvent e, @NonNull OnCalendarEventListener listener) {
            binding.tvEventTitle.setText(safe(e.getTitle(), "Medical Event"));
            binding.tvEventTime.setText(safe(e.getEventTime(), "Time TBD"));
            binding.tvEventDescription.setText(safe(e.getDescription(), "No extra details"));

            String type = safe(e.getEventType(), CalendarEvent.TYPE_APPOINTMENT);
            binding.tvEventTypeBadge.setText(type);

            int indicatorColor;
            switch (type) {
                case CalendarEvent.TYPE_LAB_TEST:
                    indicatorColor = Color.parseColor("#7B527A"); // Purple
                    break;
                case CalendarEvent.TYPE_OPERATION:
                case CalendarEvent.TYPE_OT_SCHEDULE:
                    indicatorColor = Color.parseColor("#C94B4B"); // Crimson
                    break;
                case CalendarEvent.TYPE_MED_REMINDER:
                case CalendarEvent.TYPE_FOLLOW_UP:
                    indicatorColor = Color.parseColor("#D99B45"); // Amber
                    break;
                case CalendarEvent.TYPE_APPOINTMENT:
                default:
                    indicatorColor = Color.parseColor("#557B52"); // Forest / Moss Green
                    break;
            }
            binding.viewEventColorIndicator.setBackgroundColor(indicatorColor);

            binding.tvEventStatusBadge.setText(safe(e.getStatus(), "Scheduled"));
            if (CalendarEvent.STATUS_CONFIRMED.equalsIgnoreCase(e.getStatus())
                    || CalendarEvent.STATUS_COMPLETED.equalsIgnoreCase(e.getStatus())) {
                binding.tvEventStatusBadge.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_verified));
            } else if (CalendarEvent.STATUS_CANCELLED.equalsIgnoreCase(e.getStatus())) {
                binding.tvEventStatusBadge.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_emergency));
            } else {
                binding.tvEventStatusBadge.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_warning));
            }

            binding.getRoot().setOnClickListener(v -> listener.onEventClicked(e));
        }

        private String safe(String s, String fallback) {
            return s != null && !s.trim().isEmpty() ? s.trim() : fallback;
        }
    }
}
