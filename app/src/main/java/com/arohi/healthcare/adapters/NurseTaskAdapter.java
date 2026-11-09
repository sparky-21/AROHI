package com.arohi.healthcare.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemNursingTaskBinding;
import com.arohi.healthcare.models.NursingTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter for Nurse Care Tasks, Medications, Vitals, and Doctor Instructions.
 */
public class NurseTaskAdapter extends RecyclerView.Adapter<NurseTaskAdapter.TaskViewHolder> {

    public interface OnTaskActionListener {
        void onCompleteTask(@NonNull NursingTask task);
    }

    private final OnTaskActionListener listener;
    private final List<NursingTask> items = new ArrayList<>();

    public NurseTaskAdapter(@NonNull OnTaskActionListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<NursingTask> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNursingTaskBinding binding = ItemNursingTaskBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TaskViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final ItemNursingTaskBinding binding;

        TaskViewHolder(@NonNull ItemNursingTaskBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull NursingTask t, @NonNull OnTaskActionListener listener) {
            binding.tvTaskTitle.setText(safe(t.getTitle(), "Nursing Task"));
            binding.tvTaskTypeAndTime.setText(
                    t.getTaskType() + " • Scheduled: " + safe(t.getScheduledTime(), "TBD")
                            + " • Priority: " + safe(t.getPriority(), "Normal"));

            binding.tvTaskPatientAndLocation.setText(
                    "Patient: " + safe(t.getPatientName(), "Patient")
                            + (t.getPatientId() != null ? " (" + t.getPatientId() + ")" : "")
                            + " • " + safe(t.getWardName(), "Ward") + " " + safe(t.getBedNumber(), "Bed"));

            binding.tvTaskDescription.setText(safe(t.getDescription(), "No details"));

            if (t.getDoctorInstruction() != null && !t.getDoctorInstruction().trim().isEmpty()) {
                binding.layoutDoctorInstruction.setVisibility(View.VISIBLE);
                binding.tvDoctorInstructionText.setText(
                        safe(t.getDoctorName(), "Doctor") + " Orders: " + t.getDoctorInstruction());
            } else {
                binding.layoutDoctorInstruction.setVisibility(View.GONE);
            }

            if (t.isCompleted()) {
                binding.tvTaskStatusBadge.setText("Completed");
                binding.tvTaskStatusBadge.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_verified));
                binding.btnCompleteTask.setVisibility(View.GONE);

                binding.tvNurseCompletionNote.setVisibility(View.VISIBLE);
                String timeStr = t.getCompletedAt() > 0
                        ? new SimpleDateFormat("hh:mm a", Locale.US).format(new Date(t.getCompletedAt()))
                        : "Done";
                binding.tvNurseCompletionNote.setText(
                        "✓ Completed at " + timeStr + ": " + safe(t.getNurseCompletionNotes(), "Administered."));
            } else {
                binding.tvTaskStatusBadge.setText("Pending");
                binding.tvTaskStatusBadge.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_warning));
                binding.btnCompleteTask.setVisibility(View.VISIBLE);
                binding.tvNurseCompletionNote.setVisibility(View.GONE);

                binding.btnCompleteTask.setOnClickListener(v -> listener.onCompleteTask(t));
            }
        }

        private String safe(String v, String fallback) {
            return v != null && !v.trim().isEmpty() ? v.trim() : fallback;
        }
    }
}
