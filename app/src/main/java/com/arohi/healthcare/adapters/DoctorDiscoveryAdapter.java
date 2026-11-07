package com.arohi.healthcare.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemDoctorDiscoveryBinding;
import com.arohi.healthcare.models.ConsultationRequest;
import com.arohi.healthcare.models.DoctorProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RecyclerView Adapter for Patient Doctor Discovery, displaying verified doctors,
 * qualifications, experience, consultation fee, availability schedule, and current
 * request/connection status.
 */
public class DoctorDiscoveryAdapter extends RecyclerView.Adapter<DoctorDiscoveryAdapter.DoctorViewHolder> {

    public interface OnDoctorActionListener {
        void onViewDoctorProfile(@NonNull DoctorProfile doctor, @Nullable ConsultationRequest existingRequest);
        void onRequestOrChatClicked(@NonNull DoctorProfile doctor, @Nullable ConsultationRequest existingRequest);
    }

    private final List<DoctorProfile> doctors = new ArrayList<>();
    private final Map<String, ConsultationRequest> requestByDoctorUid = new HashMap<>();
    private final OnDoctorActionListener listener;

    public DoctorDiscoveryAdapter(@NonNull OnDoctorActionListener listener) {
        this.listener = listener;
    }

    public void submitData(@NonNull List<DoctorProfile> doctorList,
                           @NonNull Map<String, ConsultationRequest> requestMap) {
        doctors.clear();
        doctors.addAll(doctorList);
        requestByDoctorUid.clear();
        requestByDoctorUid.putAll(requestMap);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDoctorDiscoveryBinding binding = ItemDoctorDiscoveryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new DoctorViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
        DoctorProfile doctor = doctors.get(position);
        ConsultationRequest existing = requestByDoctorUid.get(doctor.getUid());
        holder.bind(doctor, existing, listener);
    }

    @Override
    public int getItemCount() {
        return doctors.size();
    }

    static class DoctorViewHolder extends RecyclerView.ViewHolder {
        private final ItemDoctorDiscoveryBinding binding;

        DoctorViewHolder(@NonNull ItemDoctorDiscoveryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull DoctorProfile doctor,
                  @Nullable ConsultationRequest existing,
                  @NonNull OnDoctorActionListener listener) {
            String name = notEmpty(doctor.getFullName(), "Dr. Specialist");
            String docId = notEmpty(doctor.getArohiId(), "AROHI-DOC");
            String dept = notEmpty(doctor.getDepartment(), "Medicine");
            String specialty = notEmpty(doctor.getSpecialty(), "General Medicine");
            String quals = notEmpty(doctor.getQualifications(), "MBBS");
            int exp = doctor.getExperienceYears();
            String schedule = notEmpty(doctor.getAvailabilitySchedule(), "Sun - Thu, 5:00 PM - 9:00 PM");
            double fee = doctor.getConsultationFeeBdt();
            String verStatus = "VERIFIED".equalsIgnoreCase(doctor.getVerificationStatus())
                    ? "Verified Specialist"
                    : "Pending Admin Verification";

            binding.tvDocCardName.setText(name);
            binding.tvDocCardIdAndDept.setText(docId + " • " + dept + " • " + verStatus);
            binding.tvDocCardSpecialtyBadge.setText(specialty);
            binding.tvDocCardQualifications.setText(
                    "Qualifications: " + quals + " • Experience: " + exp + " Year" + (exp == 1 ? "" : "s"));
            binding.tvDocCardAvailabilityAndFee.setText(
                    "Availability: " + schedule + " • Fee: " + String.format("%.0f", fee) + " BDT");

            if (existing == null) {
                binding.tvDocCardConnectionState.setText("Status: Available for Consultation Request");
                binding.tvDocCardConnectionState.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.arohi_info_blue));
                binding.btnRequestOrChatDoctor.setText("Send Request");
                binding.btnRequestOrChatDoctor.setEnabled(true);
            } else if (existing.isPending()) {
                binding.tvDocCardConnectionState.setText(
                        "Request Status: Pending Doctor Review (" + existing.getConsultationReason() + ")");
                binding.tvDocCardConnectionState.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.arohi_amber_warning));
                binding.btnRequestOrChatDoctor.setText("Request Pending");
                binding.btnRequestOrChatDoctor.setEnabled(false);
            } else if (existing.isAccepted()) {
                binding.tvDocCardConnectionState.setText("Connection Status: Accepted • Secure Chat Unlocked");
                binding.tvDocCardConnectionState.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_verified));
                binding.btnRequestOrChatDoctor.setText("Open Secure Chat");
                binding.btnRequestOrChatDoctor.setEnabled(true);
            } else if (existing.isDeclined()) {
                binding.tvDocCardConnectionState.setText("Previous Request: Declined • You may submit a new request");
                binding.tvDocCardConnectionState.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.arohi_crimson_alert));
                binding.btnRequestOrChatDoctor.setText("Send New Request");
                binding.btnRequestOrChatDoctor.setEnabled(true);
            } else {
                binding.tvDocCardConnectionState.setText("Previous Request: Cancelled • You may submit a new request");
                binding.tvDocCardConnectionState.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.arohi_slate_muted));
                binding.btnRequestOrChatDoctor.setText("Send Request");
                binding.btnRequestOrChatDoctor.setEnabled(true);
            }

            binding.btnViewDoctorProfile.setOnClickListener(v -> listener.onViewDoctorProfile(doctor, existing));
            binding.btnRequestOrChatDoctor.setOnClickListener(v -> listener.onRequestOrChatClicked(doctor, existing));
        }

        private String notEmpty(String val, String fallback) {
            return val != null && !val.trim().isEmpty() ? val.trim() : fallback;
        }
    }
}
