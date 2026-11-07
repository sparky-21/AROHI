package com.arohi.healthcare.adapters;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemConsultationRequestBinding;
import com.arohi.healthcare.models.ConsultationRequest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * RecyclerView Adapter for displaying ConsultationRequest items in both:
 * - Patient Mode (view status, inspect details, cancel Pending request, or open chat when Accepted)
 * - Doctor Mode (inspect patient request details/profile, Accept/Decline Pending request, or open chat when Accepted)
 */
public class ConsultationRequestAdapter extends RecyclerView.Adapter<ConsultationRequestAdapter.RequestHolder> {

    public interface OnConsultationRequestListener {
        void onInspectDetails(@NonNull ConsultationRequest request);
        void onPrimaryAction(@NonNull ConsultationRequest request);     // Doctor: Accept | Both (Accepted): Open Chat
        void onDeclineOrCancel(@NonNull ConsultationRequest request);   // Doctor: Decline | Patient: Cancel
    }

    private final boolean isDoctorViewer;
    private final OnConsultationRequestListener listener;
    private final List<ConsultationRequest> items = new ArrayList<>();

    public ConsultationRequestAdapter(boolean isDoctorViewer,
                                      @NonNull OnConsultationRequestListener listener) {
        this.isDoctorViewer = isDoctorViewer;
        this.listener = listener;
    }

    public void submitList(@NonNull List<ConsultationRequest> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemConsultationRequestBinding binding = ItemConsultationRequestBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RequestHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestHolder holder, int position) {
        holder.bind(items.get(position), isDoctorViewer, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RequestHolder extends RecyclerView.ViewHolder {
        private final ItemConsultationRequestBinding binding;

        RequestHolder(@NonNull ItemConsultationRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull ConsultationRequest req,
                  boolean isDoctorViewer,
                  @NonNull OnConsultationRequestListener listener) {
            if (isDoctorViewer) {
                binding.tvReqCounterpartyName.setText(notEmpty(req.getPatientName(), "Patient"));
                binding.tvReqCounterpartyMeta.setText(
                        notEmpty(req.getPatientId(), "AROHI-PAT")
                                + " • Blood: " + notEmpty(req.getPatientBloodGroup(), "N/A")
                                + " • " + notEmpty(req.getPatientGender(), ""));
                binding.btnReqSecondaryAction.setText("Patient Details");
            } else {
                binding.tvReqCounterpartyName.setText(notEmpty(req.getDoctorName(), "Dr. Specialist"));
                binding.tvReqCounterpartyMeta.setText(
                        notEmpty(req.getDoctorId(), "AROHI-DOC")
                                + " • " + notEmpty(req.getDoctorSpecialty(), "General Medicine")
                                + " • Fee: " + String.format("%.0f", req.getConsultationFee()) + " BDT");
                binding.btnReqSecondaryAction.setText("Details");
            }

            String status = notEmpty(req.getStatus(), ConsultationRequest.STATUS_PENDING);
            binding.tvReqStatusBadge.setText(status);

            if (req.isAccepted()) {
                binding.tvReqStatusBadge.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_verified));
            } else if (req.isDeclined() || req.isCancelled()) {
                binding.tvReqStatusBadge.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.arohi_crimson_alert));
            } else {
                binding.tvReqStatusBadge.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.arohi_amber_warning));
            }

            binding.tvReqConsultationReason.setText("Reason: " + notEmpty(req.getConsultationReason(), "General Consultation"));

            String timeStr = req.getUpdatedAt() > 0
                    ? DateFormat.format("yyyy/MM/dd hh:mm a", new Date(req.getUpdatedAt())).toString()
                    : "";
            StringBuilder details = new StringBuilder();
            if (req.getPatientNote() != null && !req.getPatientNote().trim().isEmpty()) {
                details.append("Patient Note: ").append(req.getPatientNote().trim()).append("\n");
            }
            if (req.getDoctorResponseNote() != null && !req.getDoctorResponseNote().trim().isEmpty()) {
                details.append("Doctor Response: ").append(req.getDoctorResponseNote().trim()).append("\n");
            }
            details.append("Updated: ").append(timeStr);
            binding.tvReqNotesAndTimestamp.setText(details.toString());

            // Configure action buttons based on state & viewer role
            binding.btnReqSecondaryAction.setOnClickListener(v -> listener.onInspectDetails(req));

            if (isDoctorViewer) {
                if (req.isPending()) {
                    binding.btnReqDeclineOrCancel.setVisibility(View.VISIBLE);
                    binding.btnReqDeclineOrCancel.setText("Decline");
                    binding.btnReqDeclineOrCancel.setOnClickListener(v -> listener.onDeclineOrCancel(req));

                    binding.btnReqPrimaryAction.setVisibility(View.VISIBLE);
                    binding.btnReqPrimaryAction.setText("Accept");
                    binding.btnReqPrimaryAction.setOnClickListener(v -> listener.onPrimaryAction(req));
                } else if (req.isAccepted()) {
                    binding.btnReqDeclineOrCancel.setVisibility(View.GONE);
                    binding.btnReqPrimaryAction.setVisibility(View.VISIBLE);
                    binding.btnReqPrimaryAction.setText("Open Secure Chat");
                    binding.btnReqPrimaryAction.setOnClickListener(v -> listener.onPrimaryAction(req));
                } else {
                    // Declined or Cancelled — cannot open chat
                    binding.btnReqDeclineOrCancel.setVisibility(View.GONE);
                    binding.btnReqPrimaryAction.setVisibility(View.GONE);
                }
            } else {
                // Patient viewer
                if (req.isPending()) {
                    binding.btnReqDeclineOrCancel.setVisibility(View.VISIBLE);
                    binding.btnReqDeclineOrCancel.setText("Cancel Request");
                    binding.btnReqDeclineOrCancel.setOnClickListener(v -> listener.onDeclineOrCancel(req));

                    binding.btnReqPrimaryAction.setVisibility(View.GONE);
                } else if (req.isAccepted()) {
                    binding.btnReqDeclineOrCancel.setVisibility(View.GONE);
                    binding.btnReqPrimaryAction.setVisibility(View.VISIBLE);
                    binding.btnReqPrimaryAction.setText("Open Secure Chat");
                    binding.btnReqPrimaryAction.setOnClickListener(v -> listener.onPrimaryAction(req));
                } else {
                    // Declined or Cancelled — chat is strictly unavailable
                    binding.btnReqDeclineOrCancel.setVisibility(View.GONE);
                    binding.btnReqPrimaryAction.setVisibility(View.GONE);
                }
            }
        }

        private String notEmpty(String val, String fallback) {
            return val != null && !val.trim().isEmpty() ? val.trim() : fallback;
        }
    }
}
