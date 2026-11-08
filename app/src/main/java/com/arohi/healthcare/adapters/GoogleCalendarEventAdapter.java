package com.arohi.healthcare.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.databinding.ItemGoogleCalendarEventBinding;
import com.arohi.healthcare.models.CalendarEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter rendering events in Google Calendar design language:
 * Left-aligned time gutter, colored accent indicators, and Material 3 cards.
 */
public class GoogleCalendarEventAdapter extends RecyclerView.Adapter<GoogleCalendarEventAdapter.GoogleEventViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(@NonNull CalendarEvent event);
    }

    private final List<CalendarEvent> items = new ArrayList<>();
    private final OnEventClickListener listener;

    public GoogleCalendarEventAdapter(@NonNull OnEventClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<CalendarEvent> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GoogleEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGoogleCalendarEventBinding binding = ItemGoogleCalendarEventBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new GoogleEventViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull GoogleEventViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class GoogleEventViewHolder extends RecyclerView.ViewHolder {

        private final ItemGoogleCalendarEventBinding binding;

        public GoogleEventViewHolder(@NonNull ItemGoogleCalendarEventBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(@NonNull CalendarEvent event, @NonNull OnEventClickListener listener) {
            binding.tvGoogleEventTime.setText(event.getEventTime() != null && !event.getEventTime().isEmpty()
                    ? event.getEventTime() : "All Day");
            binding.tvGoogleEventDuration.setText("30 min");

            binding.tvGoogleEventTitle.setText(event.getTitle());
            binding.tvGoogleEventDoctorOrPatient.setText(
                    (event.getDoctorName() != null ? "Dr. " + event.getDoctorName() : "Doctor Consultation")
                            + (event.getDepartmentOrLocation() != null && !event.getDepartmentOrLocation().isEmpty()
                            ? " • " + event.getDepartmentOrLocation() : ""));
            binding.tvGoogleEventNotes.setText(event.getDescription() != null && !event.getDescription().isEmpty()
                    ? event.getDescription() : "Standard scheduled hospital event.");

            // Google Calendar Color Codes
            int color;
            String type = event.getEventType() != null ? event.getEventType().toUpperCase() : "APPOINTMENT";
            if (type.contains("LAB")) {
                color = Color.parseColor("#1E8E3E"); // Google Green
                binding.tvGoogleEventTypeTag.setText("Lab Test");
            } else if (type.contains("OPERATION") || type.contains("SURGERY")) {
                color = Color.parseColor("#9334E6"); // Google Purple
                binding.tvGoogleEventTypeTag.setText("Procedure");
            } else if (type.contains("REMINDER")) {
                color = Color.parseColor("#F9AB00"); // Google Amber
                binding.tvGoogleEventTypeTag.setText("Reminder");
            } else if (type.contains("EMERGENCY")) {
                color = Color.parseColor("#D93025"); // Google Red
                binding.tvGoogleEventTypeTag.setText("Emergency");
            } else {
                color = Color.parseColor("#1A73E8"); // Google Blue
                binding.tvGoogleEventTypeTag.setText("Appointment");
            }

            // Accent Bar Color
            binding.viewGoogleAccentBar.setBackgroundColor(color);

            // Tag background
            GradientDrawable tagBg = new GradientDrawable();
            tagBg.setColor(color);
            tagBg.setCornerRadius(16f);
            binding.tvGoogleEventTypeTag.setBackground(tagBg);

            binding.cardGoogleEvent.setOnClickListener(v -> listener.onEventClick(event));
        }
    }
}
