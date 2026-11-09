package com.arohi.healthcare.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemMedicalTimelineBinding;
import com.arohi.healthcare.models.MedicalRecordEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying the chronological Medical History Timeline.
 */
public class MedicalTimelineAdapter extends RecyclerView.Adapter<MedicalTimelineAdapter.TimelineViewHolder> {

    public interface OnTimelineActionListener {
        void onInspectTimelineEntry(@NonNull MedicalRecordEntry entry);
    }

    private final OnTimelineActionListener listener;
    private final List<MedicalRecordEntry> items = new ArrayList<>();

    public MedicalTimelineAdapter(@NonNull OnTimelineActionListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<MedicalRecordEntry> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TimelineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMedicalTimelineBinding binding = ItemMedicalTimelineBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TimelineViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TimelineViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TimelineViewHolder extends RecyclerView.ViewHolder {
        private final ItemMedicalTimelineBinding binding;

        TimelineViewHolder(@NonNull ItemMedicalTimelineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull MedicalRecordEntry entry, @NonNull OnTimelineActionListener listener) {
            binding.tvTimelineTitle.setText(safe(entry.getTitle(), "Medical Event"));
            binding.tvTimelineDate.setText(safe(entry.getDateString(), "Date"));
            binding.tvTimelineSubtitle.setText(safe(entry.getSubtitle(), "Clinical Provider"));
            binding.tvTimelineSummary.setText(safe(entry.getSummary(), "Summary"));
            binding.tvTimelineStatusBadge.setText(safe(entry.getStatus(), "Recorded"));

            // Icon by category
            String cat = safe(entry.getCategory(), MedicalRecordEntry.CATEGORY_CONSULTATION);
            switch (cat) {
                case MedicalRecordEntry.CATEGORY_PRESCRIPTION:
                    binding.tvTimelineCategoryIcon.setText("Rx");
                    binding.tvTimelineCategoryIcon.setTextColor(
                            ContextCompat.getColor(binding.getRoot().getContext(), R.color.primary_moss_green));
                    break;
                case MedicalRecordEntry.CATEGORY_LAB_REPORT:
                    binding.tvTimelineCategoryIcon.setText("Lab");
                    binding.tvTimelineCategoryIcon.setTextColor(
                            ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_warning));
                    break;
                case MedicalRecordEntry.CATEGORY_APPOINTMENT:
                case MedicalRecordEntry.CATEGORY_CONSULTATION:
                    binding.tvTimelineCategoryIcon.setText("App");
                    binding.tvTimelineCategoryIcon.setTextColor(
                            ContextCompat.getColor(binding.getRoot().getContext(), R.color.deep_forest_green));
                    break;
                case MedicalRecordEntry.CATEGORY_OPERATION:
                    binding.tvTimelineCategoryIcon.setText("OT");
                    binding.tvTimelineCategoryIcon.setTextColor(
                            ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_rejected));
                    break;
                case MedicalRecordEntry.CATEGORY_ADMISSION:
                    binding.tvTimelineCategoryIcon.setText("Bed");
                    binding.tvTimelineCategoryIcon.setTextColor(
                            ContextCompat.getColor(binding.getRoot().getContext(), R.color.deep_forest_green));
                    break;
                case MedicalRecordEntry.CATEGORY_VITALS:
                    binding.tvTimelineCategoryIcon.setText("Vit");
                    binding.tvTimelineCategoryIcon.setTextColor(
                            ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_warning));
                    break;
                case MedicalRecordEntry.CATEGORY_FOLLOW_UP:
                    binding.tvTimelineCategoryIcon.setText("Fol");
                    binding.tvTimelineCategoryIcon.setTextColor(
                            ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_verified));
                    break;
                default:
                    binding.tvTimelineCategoryIcon.setText("Med");
                    binding.tvTimelineCategoryIcon.setTextColor(
                            ContextCompat.getColor(binding.getRoot().getContext(), R.color.secondary_sage));
                    break;
            }

            binding.tvTimelineInspectAction.setOnClickListener(v -> listener.onInspectTimelineEntry(entry));
            binding.getRoot().setOnClickListener(v -> listener.onInspectTimelineEntry(entry));
        }

        private String safe(String s, String fallback) {
            return s != null && !s.trim().isEmpty() ? s.trim() : fallback;
        }
    }
}
