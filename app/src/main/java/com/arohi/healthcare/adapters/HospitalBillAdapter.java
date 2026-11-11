package com.arohi.healthcare.adapters;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemHospitalBillBinding;
import com.arohi.healthcare.models.BillCategory;
import com.arohi.healthcare.models.HospitalBill;
import com.arohi.healthcare.models.PaymentStatus;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for rendering hospital bills in patient history and authority dashboard.
 */
public class HospitalBillAdapter extends RecyclerView.Adapter<HospitalBillAdapter.BillViewHolder> {

    public interface OnBillClickListener {
        void onBillClick(@NonNull HospitalBill bill);
    }

    private final List<HospitalBill> bills = new ArrayList<>();
    private final OnBillClickListener listener;

    public HospitalBillAdapter(@NonNull OnBillClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<HospitalBill> newBills) {
        this.bills.clear();
        this.bills.addAll(newBills);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHospitalBillBinding binding = ItemHospitalBillBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BillViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BillViewHolder holder, int position) {
        holder.bind(bills.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return bills.size();
    }

    static class BillViewHolder extends RecyclerView.ViewHolder {
        private final ItemHospitalBillBinding binding;

        BillViewHolder(@NonNull ItemHospitalBillBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull HospitalBill bill, @NonNull OnBillClickListener listener) {
            Context context = binding.getRoot().getContext();

            binding.tvBillNumber.setText(bill.getBillNumber());
            binding.tvBillPatientName.setText(bill.getPatientName() != null ? bill.getPatientName() : "Patient");
            binding.tvBillTotalAmount.setText(String.format(Locale.US, "৳ %.2f", bill.getTotalAmountBdt()));

            // Category Avatar & Name
            BillCategory cat = BillCategory.fromKey(bill.getCategory());
            binding.tvBillCategoryName.setText(cat.getDisplayName());
            switch (cat) {
                case CONSULTATION:
                case APPOINTMENT:
                    binding.tvBillCategoryAvatar.setText("🩺");
                    break;
                case PHARMACY:
                    binding.tvBillCategoryAvatar.setText("💊");
                    break;
                case LAB_TEST:
                    binding.tvBillCategoryAvatar.setText("🔬");
                    break;
                case OPERATION:
                    binding.tvBillCategoryAvatar.setText("🔪");
                    break;
                case HOSPITAL_SERVICE:
                default:
                    binding.tvBillCategoryAvatar.setText("🏥");
                    break;
            }

            // Status Badge
            PaymentStatus status = PaymentStatus.fromKey(bill.getStatus());
            binding.tvBillStatusBadge.setText(status.getLabel().toUpperCase());
            if (status == PaymentStatus.SUCCESSFUL) {
                binding.tvBillStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.primary_moss_green));
                binding.tvBillStatusBadge.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.pale_mint));
            } else if (status == PaymentStatus.PENDING) {
                binding.tvBillStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.badge_pending_text));
                binding.tvBillStatusBadge.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.badge_pending_bg));
            } else if (status == PaymentStatus.REFUNDED) {
                binding.tvBillStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.badge_rejected_text));
                binding.tvBillStatusBadge.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.status_emergency_bg));
            } else {
                binding.tvBillStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            }

            // Date
            long date = bill.getPaidAt() > 0 ? bill.getPaidAt() : bill.getCreatedAt();
            if (date > 0) {
                binding.tvBillDate.setText(DateFormat.format("MMM dd, yyyy • hh:mm a", new Date(date)));
            } else {
                binding.tvBillDate.setText("");
            }

            // Payment Action Hint
            if (status == PaymentStatus.PENDING) {
                binding.layoutBillActionHint.setVisibility(android.view.View.VISIBLE);
                binding.layoutBillActionHint.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.pale_mint));
                binding.tvBillActionIcon.setText("💳");
                binding.tvBillActionText.setText("bKash, Nagad, Card, Cash Counter");
                binding.tvBillActionText.setTextColor(ContextCompat.getColor(context, R.color.primary_moss_green));
                binding.tvBillActionArrow.setText("Pay Now →");
                binding.tvBillActionArrow.setTextColor(ContextCompat.getColor(context, R.color.primary_moss_green));
            } else if (status == PaymentStatus.SUCCESSFUL) {
                binding.layoutBillActionHint.setVisibility(android.view.View.VISIBLE);
                binding.layoutBillActionHint.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.surface_subtle));
                binding.tvBillActionIcon.setText("🧾");
                binding.tvBillActionText.setText("Receipt Available • Paid " + (bill.getPaymentGateway() != null ? bill.getPaymentGateway() : ""));
                binding.tvBillActionText.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                binding.tvBillActionArrow.setText("View Receipt →");
                binding.tvBillActionArrow.setTextColor(ContextCompat.getColor(context, R.color.primary_moss_green));
            } else {
                binding.layoutBillActionHint.setVisibility(android.view.View.GONE);
            }

            binding.getRoot().setOnClickListener(v -> listener.onBillClick(bill));
        }
    }
}
