package com.arohi.healthcare.adapters;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemPatientJourneyStepBinding;
import com.arohi.healthcare.models.PatientJourney;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * RecyclerView Adapter displaying the 7 visual milestones of the Smart Patient Journey:
 * Consultation -> Diagnosis -> Testing -> Treatment -> Operation -> Recovery -> Follow-up.
 */
public class PatientJourneyStepAdapter extends RecyclerView.Adapter<PatientJourneyStepAdapter.JourneyViewHolder> {

    private final List<PatientJourney.JourneyMilestone> items = new ArrayList<>();

    public void submitList(@NonNull List<PatientJourney.JourneyMilestone> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public JourneyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPatientJourneyStepBinding binding = ItemPatientJourneyStepBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new JourneyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull JourneyViewHolder holder, int position) {
        holder.bind(items.get(position), position, items.size());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class JourneyViewHolder extends RecyclerView.ViewHolder {
        private final ItemPatientJourneyStepBinding binding;

        JourneyViewHolder(@NonNull ItemPatientJourneyStepBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull PatientJourney.JourneyMilestone milestone, int position, int totalCount) {
            binding.tvStepNumber.setText(String.valueOf(position + 1));
            binding.tvStepTitle.setText(milestone.getTitle());
            binding.tvStepDescription.setText(milestone.getDescription());

            // Last item hides connector line
            binding.viewStepLine.setVisibility(position == totalCount - 1 ? View.GONE : View.VISIBLE);

            String status = milestone.getStatus() != null ? milestone.getStatus() : PatientJourney.STATUS_PENDING;
            binding.tvStepStatusBadge.setText(status);

            if (PatientJourney.STATUS_COMPLETED.equalsIgnoreCase(status)) {
                binding.tvStepNumber.setText("✓");
                binding.tvStepNumber.setBackgroundResource(R.drawable.bg_badge_pill);
                binding.tvStepNumber.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.white));
                binding.tvStepStatusBadge.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_verified));
                binding.tvStepStatusBadge.setBackgroundResource(R.drawable.bg_badge_pill);
                binding.cardStepContainer.setStrokeColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.primary_moss_green));
            } else if (PatientJourney.STATUS_IN_PROGRESS.equalsIgnoreCase(status)) {
                binding.tvStepNumber.setBackgroundResource(R.drawable.bg_badge_pill);
                binding.tvStepNumber.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.primary_moss_green));
                binding.tvStepStatusBadge.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.primary_moss_green));
                binding.tvStepStatusBadge.setBackgroundResource(R.drawable.bg_badge_pill);
                binding.cardStepContainer.setStrokeColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.secondary_sage));
            } else {
                binding.tvStepNumber.setBackgroundResource(R.drawable.bg_white_pill);
                binding.tvStepNumber.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.text_muted));
                binding.tvStepStatusBadge.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.text_muted));
                binding.tvStepStatusBadge.setBackgroundResource(R.drawable.bg_white_pill);
                binding.cardStepContainer.setStrokeColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.border_stroke));
            }

            if (milestone.getClinicalSummary() != null && !milestone.getClinicalSummary().isEmpty()) {
                binding.tvStepSummary.setVisibility(View.VISIBLE);
                binding.tvStepSummary.setText(milestone.getClinicalSummary());
            } else {
                binding.tvStepSummary.setVisibility(View.GONE);
            }

            if (milestone.getPerformerName() != null && !milestone.getPerformerName().isEmpty()) {
                binding.tvStepPerformer.setVisibility(View.VISIBLE);
                String role = milestone.getPerformerRole() != null ? " (" + milestone.getPerformerRole() + ")" : "";
                binding.tvStepPerformer.setText("Provider: " + milestone.getPerformerName() + role);
            } else {
                binding.tvStepPerformer.setVisibility(View.GONE);
            }

            if (milestone.getCompletedAt() > 0) {
                binding.tvStepTime.setVisibility(View.VISIBLE);
                binding.tvStepTime.setText("Completed " + DateFormat.format("yyyy-MM-dd", new Date(milestone.getCompletedAt())));
            } else if (milestone.getUpdatedAt() > 0 && !PatientJourney.STATUS_PENDING.equalsIgnoreCase(status)) {
                binding.tvStepTime.setVisibility(View.VISIBLE);
                binding.tvStepTime.setText("Updated " + DateFormat.format("yyyy-MM-dd", new Date(milestone.getUpdatedAt())));
            } else {
                binding.tvStepTime.setVisibility(View.GONE);
            }
        }
    }
}
