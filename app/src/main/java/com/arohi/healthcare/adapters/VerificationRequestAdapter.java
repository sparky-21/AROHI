package com.arohi.healthcare.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemVerificationRequestBinding;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VerificationStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for Hospital Administrators to review, approve, or decline
 * Patient, Doctor, and Nurse verification requests in real time.
 */
public class VerificationRequestAdapter extends RecyclerView.Adapter<VerificationRequestAdapter.RequestViewHolder> {

    public interface OnVerificationActionListener {
        void onApprove(@NonNull UserAccount userAccount);
        void onReject(@NonNull UserAccount userAccount);
    }

    private final List<UserAccount> items = new ArrayList<>();
    private final OnVerificationActionListener listener;

    public VerificationRequestAdapter(@NonNull OnVerificationActionListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<UserAccount> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVerificationRequestBinding binding = ItemVerificationRequestBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new RequestViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        UserAccount account = items.get(position);
        holder.bind(account, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        private final ItemVerificationRequestBinding binding;

        RequestViewHolder(@NonNull ItemVerificationRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull UserAccount account, @NonNull OnVerificationActionListener listener) {
            String fullName = account.getFullName() != null && !account.getFullName().isEmpty()
                    ? account.getFullName()
                    : "Unnamed User";
            String arohiId = account.getArohiId() != null && !account.getArohiId().isEmpty()
                    ? account.getArohiId()
                    : "AROHI-ID-PENDING";
            String role = account.getRole() != null ? account.getRole() : "PATIENT";
            String email = account.getEmail() != null ? account.getEmail() : "No email";
            String phone = account.getPhone() != null && !account.getPhone().isEmpty()
                    ? account.getPhone()
                    : "No phone";
            VerificationStatus status = account.getVerificationStatusEnum();

            binding.tvItemUserName.setText(fullName);
            binding.tvItemArohiId.setText(arohiId);
            binding.tvItemRoleBadge.setText(role);
            binding.tvItemEmailPhone.setText(email + " • " + phone);
            binding.tvItemCurrentStatus.setText("Verification Status: " + status.getKey() + " (" + status.getDisplayLabel() + ")");

            if (status == VerificationStatus.VERIFIED) {
                binding.tvItemCurrentStatus.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_verified));
            } else if (status == VerificationStatus.REJECTED) {
                binding.tvItemCurrentStatus.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_emergency));
            } else {
                binding.tvItemCurrentStatus.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_warning));
            }

            if (account.getUserRoleEnum() == UserRole.ADMIN) {
                binding.layoutVerificationActions.setVisibility(View.GONE);
            } else {
                binding.layoutVerificationActions.setVisibility(View.VISIBLE);
                binding.btnApproveAccount.setEnabled(status != VerificationStatus.VERIFIED);
                binding.btnRejectAccount.setEnabled(status != VerificationStatus.REJECTED);

                binding.btnApproveAccount.setOnClickListener(v -> listener.onApprove(account));
                binding.btnRejectAccount.setOnClickListener(v -> listener.onReject(account));
            }
        }
    }
}
