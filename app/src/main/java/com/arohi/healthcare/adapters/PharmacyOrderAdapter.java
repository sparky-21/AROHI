package com.arohi.healthcare.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemPharmacyOrderBinding;
import com.arohi.healthcare.models.PharmacyOrder;
import com.arohi.healthcare.models.PharmacyOrderItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter for displaying Pharmacy Orders with a 5-step status lifecycle tracker
 * (Received -> Verified -> Preparing -> Ready -> Collected).
 */
public class PharmacyOrderAdapter extends RecyclerView.Adapter<PharmacyOrderAdapter.OrderViewHolder> {

    public interface OnOrderActionListener {
        void onViewOrderDetails(@NonNull PharmacyOrder order);
        void onManageOrderStatus(@NonNull PharmacyOrder order);
    }

    private final boolean isStaffViewer;
    private final OnOrderActionListener listener;
    private final List<PharmacyOrder> items = new ArrayList<>();

    public PharmacyOrderAdapter(boolean isStaffViewer, @NonNull OnOrderActionListener listener) {
        this.isStaffViewer = isStaffViewer;
        this.listener = listener;
    }

    public void submitList(@NonNull List<PharmacyOrder> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPharmacyOrderBinding binding = ItemPharmacyOrderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new OrderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        holder.bind(items.get(position), isStaffViewer, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {

        private final ItemPharmacyOrderBinding binding;

        public OrderViewHolder(@NonNull ItemPharmacyOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(@NonNull PharmacyOrder order, boolean isStaff, @NonNull OnOrderActionListener listener) {
            binding.tvOrderId.setText("Order #" + order.getOrderId()
                    + (isStaff && order.getPatientName() != null ? " • " + order.getPatientName() : ""));
            binding.tvOrderDate.setText("Placed: " + order.getOrderDate());
            binding.tvOrderStatusBadge.setText(order.getStatus());
            binding.tvOrderTotal.setText(String.format(Locale.ROOT, "৳ %.2f", order.getTotalAmount()));
            binding.tvDeliveryDetails.setText("Delivery: " + order.getDeliveryType()
                    + (order.getDeliveryAddress() != null && !order.getDeliveryAddress().isEmpty() ? " (" + order.getDeliveryAddress() + ")" : ""));

            // Prescription Link
            if (order.isPrescriptionLinked()) {
                binding.tvPrescriptionLinkBanner.setVisibility(View.VISIBLE);
                binding.tvPrescriptionLinkBanner.setText("✓ Verified Prescription #" + order.getPrescriptionId()
                        + (order.getPrescriptionDoctorName() != null && !order.getPrescriptionDoctorName().isEmpty() ? " • Dr. " + order.getPrescriptionDoctorName() : ""));
            } else {
                binding.tvPrescriptionLinkBanner.setVisibility(View.GONE);
            }

            // Items Summary
            StringBuilder sb = new StringBuilder();
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                for (PharmacyOrderItem item : order.getItems()) {
                    sb.append("• ").append(item.getMedicineName()).append(" (").append(item.getStrength()).append(") x ")
                            .append(item.getQuantity()).append("\n");
                }
                binding.tvOrderItemsSummary.setText(sb.toString().trim());
            } else {
                binding.tvOrderItemsSummary.setText("• Standard Dispensary Package");
            }

            // Pharmacist Notes
            if (order.getPharmacistNotes() != null && !order.getPharmacistNotes().trim().isEmpty()) {
                binding.tvPharmacistNotes.setVisibility(View.VISIBLE);
                binding.tvPharmacistNotes.setText("Pharmacist: " + order.getPharmacistNotes().trim());
            } else {
                binding.tvPharmacistNotes.setVisibility(View.GONE);
            }

            // 5-Step Progress Tracker Highlighting
            int stepIndex = order.getStatusStepIndex();
            highlightSteps(stepIndex);

            // Actions
            if (isStaff) {
                binding.btnManageOrderStatus.setVisibility(View.VISIBLE);
                binding.btnManageOrderStatus.setOnClickListener(v -> listener.onManageOrderStatus(order));
            } else {
                binding.btnManageOrderStatus.setVisibility(View.GONE);
            }

            binding.btnViewOrderDetails.setOnClickListener(v -> listener.onViewOrderDetails(order));
        }

        private void highlightSteps(int stepIndex) {
            TextView[] steps = new TextView[]{
                    binding.stepReceived,
                    binding.stepVerified,
                    binding.stepPreparing,
                    binding.stepReady,
                    binding.stepCollected
            };

            int activeColor = ContextCompat.getColor(itemView.getContext(), R.color.status_verified);
            int inactiveColor = ContextCompat.getColor(itemView.getContext(), R.color.text_muted);

            for (int i = 0; i < steps.length; i++) {
                if (i <= stepIndex) {
                    steps[i].setTextColor(activeColor);
                    steps[i].setTypeface(null, android.graphics.Typeface.BOLD);
                } else {
                    steps[i].setTextColor(inactiveColor);
                    steps[i].setTypeface(null, android.graphics.Typeface.NORMAL);
                }
            }
        }
    }
}
