package com.arohi.healthcare.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemBedBinding;
import com.arohi.healthcare.models.Bed;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for Beds and Admitted Ward Patients.
 */
public class NurseBedAdapter extends RecyclerView.Adapter<NurseBedAdapter.BedViewHolder> {

    public interface OnBedActionListener {
        void onRecordVitals(@NonNull Bed bed);
        void onBedAction(@NonNull Bed bed);
        void onChangeStatus(@NonNull Bed bed);
    }

    private final OnBedActionListener listener;
    private final boolean isNursePortalMode;
    private final List<Bed> items = new ArrayList<>();

    public NurseBedAdapter(boolean isNursePortalMode, @NonNull OnBedActionListener listener) {
        this.isNursePortalMode = isNursePortalMode;
        this.listener = listener;
    }

    public void submitList(@NonNull List<Bed> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBedBinding binding = ItemBedBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BedViewHolder(binding, isNursePortalMode);
    }

    @Override
    public void onBindViewHolder(@NonNull BedViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class BedViewHolder extends RecyclerView.ViewHolder {
        private final ItemBedBinding binding;
        private final boolean isNursePortalMode;

        BedViewHolder(@NonNull ItemBedBinding binding, boolean isNursePortalMode) {
            super(binding.getRoot());
            this.binding = binding;
            this.isNursePortalMode = isNursePortalMode;
        }

        void bind(@NonNull Bed b, @NonNull OnBedActionListener listener) {
            binding.tvBedNumber.setText(safe(b.getBedNumber(), "Bed") + " • " + safe(b.getBedType(), "General"));
            binding.tvBedWardLocation.setText(safe(b.getWardName(), "Inpatient Ward"));

            String status = safe(b.getStatus(), Bed.STATUS_AVAILABLE);
            binding.tvBedStatusBadge.setText(status);

            if (Bed.STATUS_AVAILABLE.equalsIgnoreCase(status)) {
                binding.tvBedStatusBadge.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_verified));
                binding.layoutBedOccupiedDetails.setVisibility(View.GONE);

                if (isNursePortalMode) {
                    binding.btnBedPrimaryAction.setVisibility(View.GONE);
                } else {
                    binding.btnBedPrimaryAction.setVisibility(View.VISIBLE);
                    binding.btnBedPrimaryAction.setText("Assign Patient");
                    binding.btnBedPrimaryAction.setOnClickListener(v -> listener.onBedAction(b));
                }
            } else if (Bed.STATUS_OCCUPIED.equalsIgnoreCase(status)) {
                binding.tvBedStatusBadge.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_warning));
                binding.layoutBedOccupiedDetails.setVisibility(View.VISIBLE);

                binding.tvBedPatientInfo.setText(
                        "Patient: " + safe(b.getPatientName(), "Admitted Patient")
                                + (b.getPatientId() != null && !b.getPatientId().isEmpty() ? " (" + b.getPatientId() + ")" : ""));
                binding.tvBedCareTeamInfo.setText(
                        "Doctor: " + safe(b.getAssignedDoctorName(), "Attending")
                                + " • Nurse: " + safe(b.getAssignedNurseName(), "Assigned"));
                binding.tvBedClinicalNotes.setText(
                        "Notes: " + safe(b.getClinicalNotes(), "Admitted for clinical monitoring."));

                binding.btnBedPrimaryAction.setVisibility(View.VISIBLE);
                if (isNursePortalMode) {
                    binding.btnBedPrimaryAction.setText("Record Vitals");
                    binding.btnBedPrimaryAction.setOnClickListener(v -> listener.onRecordVitals(b));
                } else {
                    binding.btnBedPrimaryAction.setText("Discharge");
                    binding.btnBedPrimaryAction.setOnClickListener(v -> listener.onBedAction(b));
                }
            } else {
                binding.tvBedStatusBadge.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_emergency));
                binding.layoutBedOccupiedDetails.setVisibility(View.GONE);

                binding.btnBedPrimaryAction.setVisibility(View.VISIBLE);
                binding.btnBedPrimaryAction.setText("Set Available");
                binding.btnBedPrimaryAction.setOnClickListener(v -> listener.onBedAction(b));
            }

            binding.btnBedChangeStatus.setOnClickListener(v -> listener.onChangeStatus(b));
        }

        private String safe(String v, String fallback) {
            return v != null && !v.trim().isEmpty() ? v.trim() : fallback;
        }
    }
}
