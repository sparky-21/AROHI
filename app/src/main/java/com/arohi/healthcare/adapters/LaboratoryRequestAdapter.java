package com.arohi.healthcare.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemLabRequestBinding;
import com.arohi.healthcare.models.LaboratoryRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying Laboratory Diagnostic Requests & Reports.
 */
public class LaboratoryRequestAdapter extends RecyclerView.Adapter<LaboratoryRequestAdapter.LabViewHolder> {

    public interface OnLabActionListener {
        void onUploadReport(@NonNull LaboratoryRequest request);
        void onDoctorReviewReport(@NonNull LaboratoryRequest request);
        void onViewReportDetails(@NonNull LaboratoryRequest request);
    }

    private final boolean isDoctorViewer;
    private final OnLabActionListener listener;
    private final List<LaboratoryRequest> items = new ArrayList<>();

    public LaboratoryRequestAdapter(boolean isDoctorViewer, @NonNull OnLabActionListener listener) {
        this.isDoctorViewer = isDoctorViewer;
        this.listener = listener;
    }

    public void submitList(@NonNull List<LaboratoryRequest> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLabRequestBinding binding = ItemLabRequestBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new LabViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LabViewHolder holder, int position) {
        holder.bind(items.get(position), isDoctorViewer, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class LabViewHolder extends RecyclerView.ViewHolder {
        private final ItemLabRequestBinding binding;

        LabViewHolder(@NonNull ItemLabRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull LaboratoryRequest lab, boolean isDoctorViewer, @NonNull OnLabActionListener listener) {
            binding.tvLabTestName.setText(safe(lab.getTestName(), "Diagnostic Test"));
            binding.tvLabCategoryAndUrgency.setText(
                    safe(lab.getTestCategory(), "Clinical Laboratory")
                            + " • Urgency: " + safe(lab.getUrgency(), LaboratoryRequest.URGENCY_ROUTINE));

            String status = safe(lab.getStatus(), LaboratoryRequest.STATUS_REQUESTED);
            binding.tvLabStatusBadge.setText(status);

            if (lab.isReviewed()) {
                binding.tvLabStatusBadge.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_verified));
            } else if (lab.isReportReady()) {
                binding.tvLabStatusBadge.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.primary_moss_green));
            } else if (LaboratoryRequest.STATUS_CANCELLED.equalsIgnoreCase(status)) {
                binding.tvLabStatusBadge.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_emergency));
            } else {
                binding.tvLabStatusBadge.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_warning));
            }

            if (isDoctorViewer) {
                binding.tvLabDoctorAndPatient.setText("Patient: " + safe(lab.getPatientName(), "Patient")
                        + " (" + safe(lab.getPatientId(), "AROHI-PAT") + ") • Blood: " + safe(lab.getPatientBloodGroup(), "N/A"));
            } else {
                binding.tvLabDoctorAndPatient.setText("Ordered by Dr. " + safe(lab.getDoctorName(), "Specialist")
                        + " • " + safe(lab.getClinicalIndication(), "Diagnostic Assessment"));
            }

            // Findings summary
            String findings = !isEmpty(lab.getReportFindingsSummary())
                    ? "Findings: " + lab.getReportFindingsSummary().trim()
                    : "Test ordered • Diagnostic sample processing";
            binding.tvLabFindingsSummary.setText(findings);

            if (!isEmpty(lab.getDoctorReviewNotes())) {
                binding.tvLabDoctorReviewNotes.setVisibility(View.VISIBLE);
                binding.tvLabDoctorReviewNotes.setText("Doctor Review: " + lab.getDoctorReviewNotes().trim());
            } else {
                binding.tvLabDoctorReviewNotes.setVisibility(View.GONE);
            }

            // Actions
            // Upload button: shown if report not ready or if user wants to update report
            binding.btnLabUploadReport.setVisibility(View.VISIBLE);
            binding.btnLabUploadReport.setOnClickListener(v -> listener.onUploadReport(lab));

            // Doctor Review button: shown only to Doctor when report is ready
            if (isDoctorViewer && lab.isReportReady()) {
                binding.btnLabDoctorReview.setVisibility(View.VISIBLE);
                binding.btnLabDoctorReview.setOnClickListener(v -> listener.onDoctorReviewReport(lab));
            } else {
                binding.btnLabDoctorReview.setVisibility(View.GONE);
            }

            // View Report button
            binding.btnLabViewReport.setVisibility(View.VISIBLE);
            binding.btnLabViewReport.setOnClickListener(v -> listener.onViewReportDetails(lab));

            binding.getRoot().setOnClickListener(v -> listener.onViewReportDetails(lab));
        }

        private String safe(String s, String fallback) {
            return s != null && !s.trim().isEmpty() ? s.trim() : fallback;
        }

        private boolean isEmpty(String s) {
            return s == null || s.trim().isEmpty();
        }
    }
}
