package com.arohi.healthcare.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemOperationBinding;
import com.arohi.healthcare.models.Operation;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for OT Surgical Operations and Status Updates.
 */
public class OperationAdapter extends RecyclerView.Adapter<OperationAdapter.OpViewHolder> {

    public interface OnOperationActionListener {
        void onUpdateStatus(@NonNull Operation operation);
        void onViewChecklist(@NonNull Operation operation);
    }

    private final OnOperationActionListener listener;
    private final List<Operation> items = new ArrayList<>();

    public OperationAdapter(@NonNull OnOperationActionListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<Operation> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OpViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOperationBinding binding = ItemOperationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new OpViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OpViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class OpViewHolder extends RecyclerView.ViewHolder {
        private final ItemOperationBinding binding;

        OpViewHolder(@NonNull ItemOperationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Operation op, @NonNull OnOperationActionListener listener) {
            binding.tvOperationName.setText(safe(op.getProcedureName(), "Surgical Operation"));
            binding.tvOperationDateTimeAndRoom.setText(
                    "📅 " + safe(op.getOperationDate(), "Date TBD") + " at " + safe(op.getOperationTime(), "Time TBD")
                            + " • " + safe(op.getOtRoomName(), "Main OT"));

            binding.tvOperationPatientAndDoctor.setText(
                    "Patient: " + safe(op.getPatientName(), "Patient")
                            + (op.getPatientId() != null ? " (" + op.getPatientId() + ")" : "")
                            + "\nSurgeon: Dr. " + safe(op.getDoctorName(), "Attending Surgeon")
                            + (op.getDoctorSpecialty() != null ? " (" + op.getDoctorSpecialty() + ")" : "")
                            + "\nScrub Nurse: " + safe(op.getNurseName(), "Assigned Nurse"));

            String status = safe(op.getStatus(), Operation.STATUS_SCHEDULED);
            binding.tvOperationStatus.setText(status);

            if (Operation.STATUS_COMPLETED.equalsIgnoreCase(status)) {
                binding.tvOperationStatus.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_verified));
            } else if (Operation.STATUS_IN_PROGRESS.equalsIgnoreCase(status)
                    || Operation.STATUS_PREPARATION.equalsIgnoreCase(status)) {
                binding.tvOperationStatus.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_warning));
            } else {
                binding.tvOperationStatus.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.primary_moss_green));
            }

            StringBuilder sb = new StringBuilder();
            sb.append(op.isNpoConfirmed() ? "✓ NPO " : "✗ NPO ");
            sb.append(op.isAnesthesiaCleared() ? "✓ Anesthesia " : "✗ Anesthesia ");
            sb.append(op.isConsentSigned() ? "✓ Consent " : "✗ Consent ");
            sb.append(op.isBloodArranged() ? "✓ Blood " : "✗ Blood ");
            sb.append(op.isPreOpAntibioticsGiven() ? "✓ Antibiotics" : "✗ Antibiotics");
            binding.tvOperationChecklist.setText(sb.toString());

            if (op.getFollowUpDate() != null && !op.getFollowUpDate().isEmpty()) {
                binding.tvOperationFollowUp.setVisibility(View.VISIBLE);
                binding.tvOperationFollowUp.setText("Follow-up Date: " + op.getFollowUpDate()
                        + (op.getPostOpNotes() != null && !op.getPostOpNotes().isEmpty() ? " • " + op.getPostOpNotes() : ""));
            } else {
                binding.tvOperationFollowUp.setVisibility(View.GONE);
            }

            binding.btnUpdateOperationStatus.setOnClickListener(v -> listener.onUpdateStatus(op));
            binding.btnViewOperationChecklist.setOnClickListener(v -> listener.onViewChecklist(op));
        }

        private String safe(String v, String fallback) {
            return v != null && !v.trim().isEmpty() ? v.trim() : fallback;
        }
    }
}
