package com.arohi.healthcare.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemDutyScheduleBinding;
import com.arohi.healthcare.models.DutySchedule;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for Nurse Shift Rosters and OT Assistance.
 */
public class DutyScheduleAdapter extends RecyclerView.Adapter<DutyScheduleAdapter.DutyViewHolder> {

    private final List<DutySchedule> items = new ArrayList<>();

    public void submitList(@NonNull List<DutySchedule> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DutyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDutyScheduleBinding binding = ItemDutyScheduleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new DutyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DutyViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class DutyViewHolder extends RecyclerView.ViewHolder {
        private final ItemDutyScheduleBinding binding;

        DutyViewHolder(@NonNull ItemDutyScheduleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull DutySchedule d) {
            binding.tvDutyShiftName.setText(safe(d.getShiftType(), DutySchedule.SHIFT_MORNING));
            binding.tvDutyDateAndWard.setText(
                    "📅 " + safe(d.getShiftDate(), "Today") + " • " + safe(d.getWardName(), "Ward Care"));

            binding.tvDutyStatusBadge.setText(safe(d.getStatus(), "Scheduled"));
            binding.tvDutyStatusBadge.setTextColor(
                    ContextCompat.getColor(binding.getRoot().getContext(), R.color.primary_moss_green));

            if (d.isOtAssistance()) {
                binding.layoutOtAssistanceDetails.setVisibility(View.VISIBLE);
                binding.tvOtAssistanceInfo.setText(
                        "🏥 Operation Theatre Assistance Assigned\n"
                                + safe(d.getOtRoomName(), "Main OT") + ": "
                                + safe(d.getOtOperationName(), "Assigned Procedure"));
            } else {
                binding.layoutOtAssistanceDetails.setVisibility(View.GONE);
            }
        }

        private String safe(String v, String fallback) {
            return v != null && !v.trim().isEmpty() ? v.trim() : fallback;
        }
    }
}
