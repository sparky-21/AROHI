package com.arohi.healthcare.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemAppointmentBinding;
import com.arohi.healthcare.models.Appointment;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying Appointments in Patient and Doctor modes.
 */
public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ApptViewHolder> {

    public interface OnAppointmentActionListener {
        void onConfirmAppointment(@NonNull Appointment appointment);
        void onRescheduleAppointment(@NonNull Appointment appointment);
        void onCancelAppointment(@NonNull Appointment appointment);
        void onViewAppointmentDetails(@NonNull Appointment appointment);
    }

    private final boolean isDoctorViewer;
    private final OnAppointmentActionListener listener;
    private final List<Appointment> items = new ArrayList<>();

    public AppointmentAdapter(boolean isDoctorViewer, @NonNull OnAppointmentActionListener listener) {
        this.isDoctorViewer = isDoctorViewer;
        this.listener = listener;
    }

    public void submitList(@NonNull List<Appointment> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ApptViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAppointmentBinding binding = ItemAppointmentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ApptViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ApptViewHolder holder, int position) {
        holder.bind(items.get(position), isDoctorViewer, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ApptViewHolder extends RecyclerView.ViewHolder {
        private final ItemAppointmentBinding binding;

        ApptViewHolder(@NonNull ItemAppointmentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Appointment a, boolean isDoctorViewer, @NonNull OnAppointmentActionListener listener) {
            if (isDoctorViewer) {
                binding.tvApptCounterpartyName.setText(safe(a.getPatientName(), "Patient"));
                binding.tvApptSpecialtyOrId.setText("ID: " + safe(a.getPatientId(), "AROHI-PAT")
                        + " • " + safe(a.getPatientPhone(), "No phone"));
            } else {
                binding.tvApptCounterpartyName.setText(safe(a.getDoctorName(), "Dr. Specialist"));
                binding.tvApptSpecialtyOrId.setText(safe(a.getDoctorSpecialty(), "General Medicine")
                        + " • Fee: " + String.format("%.0f", a.getConsultationFee()) + " BDT");
            }

            binding.tvApptStatusBadge.setText(safe(a.getStatus(), Appointment.STATUS_PENDING));
            if (a.isConfirmed()) {
                binding.tvApptStatusBadge.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_verified));
            } else if (a.isCancelled()) {
                binding.tvApptStatusBadge.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_emergency));
            } else {
                binding.tvApptStatusBadge.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_warning));
            }

            binding.tvApptDateTime.setText(safe(a.getAppointmentDate(), "Date") + " • " + safe(a.getAppointmentTime(), "Time"));
            binding.tvApptConsultationType.setText(safe(a.getConsultationType(), Appointment.TYPE_IN_PERSON));
            binding.tvApptReason.setText("Reason: " + safe(a.getReason(), "General Consultation"));

            if (a.getDoctorNotes() != null && !a.getDoctorNotes().trim().isEmpty()) {
                binding.tvApptNotes.setVisibility(View.VISIBLE);
                binding.tvApptNotes.setText("Doctor Notes: " + a.getDoctorNotes().trim());
            } else {
                binding.tvApptNotes.setVisibility(View.GONE);
            }

            // Configure button actions based on status & role
            if (a.isCancelled() || a.isCompleted()) {
                binding.layoutApptActions.setVisibility(View.GONE);
            } else {
                binding.layoutApptActions.setVisibility(View.VISIBLE);
                binding.btnApptSecondaryAction.setText("Reschedule");
                binding.btnApptSecondaryAction.setOnClickListener(v -> listener.onRescheduleAppointment(a));

                binding.btnApptDeclineOrCancel.setText("Cancel");
                binding.btnApptDeclineOrCancel.setOnClickListener(v -> listener.onCancelAppointment(a));

                if (isDoctorViewer && a.isPending()) {
                    binding.btnApptPrimaryAction.setVisibility(View.VISIBLE);
                    binding.btnApptPrimaryAction.setText("Confirm");
                    binding.btnApptPrimaryAction.setOnClickListener(v -> listener.onConfirmAppointment(a));
                } else {
                    binding.btnApptPrimaryAction.setVisibility(View.GONE);
                }
            }

            binding.getRoot().setOnClickListener(v -> listener.onViewAppointmentDetails(a));
        }

        private String safe(String s, String fallback) {
            return s != null && !s.trim().isEmpty() ? s.trim() : fallback;
        }
    }
}
