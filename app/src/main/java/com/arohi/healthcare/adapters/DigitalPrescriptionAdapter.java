package com.arohi.healthcare.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.databinding.ItemPrescriptionBinding;
import com.arohi.healthcare.models.DigitalPrescription;
import com.arohi.healthcare.models.PrescriptionMedicine;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying Digital Prescriptions in Patient Records and Doctor Portals.
 */
public class DigitalPrescriptionAdapter extends RecyclerView.Adapter<DigitalPrescriptionAdapter.RxViewHolder> {

    public interface OnPrescriptionActionListener {
        void onViewPrescriptionDetails(@NonNull DigitalPrescription prescription);
        void onExportPrescriptionPdf(@NonNull DigitalPrescription prescription);
    }

    private final boolean isDoctorViewer;
    private final OnPrescriptionActionListener listener;
    private final List<DigitalPrescription> items = new ArrayList<>();

    public DigitalPrescriptionAdapter(boolean isDoctorViewer, @NonNull OnPrescriptionActionListener listener) {
        this.isDoctorViewer = isDoctorViewer;
        this.listener = listener;
    }

    public void submitList(@NonNull List<DigitalPrescription> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RxViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPrescriptionBinding binding = ItemPrescriptionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RxViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RxViewHolder holder, int position) {
        holder.bind(items.get(position), isDoctorViewer, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RxViewHolder extends RecyclerView.ViewHolder {
        private final ItemPrescriptionBinding binding;

        RxViewHolder(@NonNull ItemPrescriptionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull DigitalPrescription rx, boolean isDoctorViewer, @NonNull OnPrescriptionActionListener listener) {
            binding.tvRxDiagnosis.setText(safe(rx.getDiagnosis(), "General Consultation Assessment"));

            if (isDoctorViewer) {
                binding.tvRxDoctorName.setText("Patient: " + safe(rx.getPatientName(), "Patient")
                        + " (" + safe(rx.getPatientId(), "AROHI-PAT") + ")");
            } else {
                binding.tvRxDoctorName.setText("Prescribed by: " + safe(rx.getDoctorName(), "Dr. Specialist")
                        + " • " + safe(rx.getDoctorSpecialty(), "Medicine"));
            }

            binding.tvRxDate.setText(safe(rx.getFollowUpDate(), "Active"));

            // Medicines summary
            List<PrescriptionMedicine> meds = rx.getMedicines();
            if (meds != null && !meds.isEmpty()) {
                StringBuilder sb = new StringBuilder("Medicines: ");
                for (int i = 0; i < meds.size(); i++) {
                    PrescriptionMedicine m = meds.get(i);
                    if (m != null) {
                        sb.append(m.getMedicineName());
                        if (m.getDosage() != null && !m.getDosage().isEmpty()) {
                            sb.append(" ").append(m.getDosage());
                        }
                        if (i < meds.size() - 1) sb.append(", ");
                    }
                }
                binding.tvRxMedicinesSummary.setText(sb.toString());
            } else {
                binding.tvRxMedicinesSummary.setText("Medicines: As clinically instructed");
            }

            // Advice & follow-up
            String advice = safe(rx.getAdviceInstructions(), "Rest and follow prescribed instructions.");
            String followUp = (rx.getFollowUpDate() != null && !rx.getFollowUpDate().isEmpty())
                    ? " • Follow-up: " + rx.getFollowUpDate()
                    : "";
            binding.tvRxAdviceAndFollowUp.setText("Advice: " + advice + followUp);

            binding.btnRxViewDetails.setOnClickListener(v -> listener.onViewPrescriptionDetails(rx));
            binding.btnRxExportPdf.setOnClickListener(v -> listener.onExportPrescriptionPdf(rx));
            binding.getRoot().setOnClickListener(v -> listener.onViewPrescriptionDetails(rx));
        }

        private String safe(String s, String fallback) {
            return s != null && !s.trim().isEmpty() ? s.trim() : fallback;
        }
    }
}
