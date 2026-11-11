package com.arohi.healthcare.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemTrackingTimelineStepBinding;
import com.arohi.healthcare.models.TrackingStatusEvent;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Renders chronological milestone timeline steps inside Tracking detail dialogs.
 */
public class TrackingTimelineAdapter extends RecyclerView.Adapter<TrackingTimelineAdapter.ViewHolder> {

    private final List<TrackingStatusEvent> events = new ArrayList<>();

    public void submitList(@NonNull List<TrackingStatusEvent> newEvents) {
        events.clear();
        events.addAll(newEvents);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTrackingTimelineStepBinding binding = ItemTrackingTimelineStepBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        boolean isLast = position == events.size() - 1;
        holder.bind(events.get(position), isLast);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTrackingTimelineStepBinding b;

        ViewHolder(@NonNull ItemTrackingTimelineStepBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(@NonNull TrackingStatusEvent ev, boolean isLast) {
            Context ctx = b.getRoot().getContext();
            b.tvTimelineStepStatus.setText(ev.getStatus());
            if (ev.getTimestamp() > 0) {
                b.tvTimelineStepTime.setText(DateFormat.format("MMM dd • hh:mm a", new Date(ev.getTimestamp())));
            } else {
                b.tvTimelineStepTime.setText("");
            }

            String actor = ev.getUpdatedByName() != null && !ev.getUpdatedByName().isEmpty()
                    ? ev.getUpdatedByName() : "AROHI System";
            String note = ev.getNotes() != null && !ev.getNotes().isEmpty()
                    ? ev.getNotes() + " (" + actor + ")"
                    : "Updated by " + actor;
            b.tvTimelineStepNote.setText(note);

            int dotColor = isLast
                    ? ContextCompat.getColor(ctx, R.color.status_verified)
                    : ContextCompat.getColor(ctx, R.color.primary_moss_green);
            b.viewStepDot.setBackgroundTintList(ColorStateList.valueOf(dotColor));
            b.viewStepConnector.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);
        }
    }
}
