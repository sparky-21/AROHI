package com.arohi.healthcare.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemMedicineBinding;
import com.arohi.healthcare.models.Medicine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter for displaying medicines in the AROHI Smart E-Pharmacy catalogue.
 */
public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder> {

    public interface OnMedicineActionListener {
        void onShowDetails(@NonNull Medicine medicine);
        void onPlaceOrder(@NonNull Medicine medicine);
    }

    private final List<Medicine> items = new ArrayList<>();
    private final OnMedicineActionListener listener;

    public MedicineAdapter(@NonNull OnMedicineActionListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<Medicine> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MedicineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMedicineBinding binding = ItemMedicineBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MedicineViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicineViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MedicineViewHolder extends RecyclerView.ViewHolder {

        private final ItemMedicineBinding binding;

        public MedicineViewHolder(@NonNull ItemMedicineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(@NonNull Medicine medicine, @NonNull OnMedicineActionListener listener) {
            binding.tvMedicineName.setText(medicine.getName());
            binding.tvMedicineCategory.setText(medicine.getCategory());
            binding.tvGenericName.setText("Generic: " + medicine.getGenericName() + " • " + medicine.getDosageForm());
            binding.tvManufacturer.setText(medicine.getManufacturer());
            binding.tvUnitPrice.setText(String.format(Locale.ROOT, "৳ %.2f", medicine.getUnitPrice()));

            // Rx Required Badge
            if (medicine.isRequiresPrescription()) {
                binding.tvRxBadge.setVisibility(View.VISIBLE);
                binding.tvRxBadge.setText("Rx Required");
                binding.tvRxBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_warning));
            } else {
                binding.tvRxBadge.setVisibility(View.VISIBLE);
                binding.tvRxBadge.setText("OTC Medicine");
                binding.tvRxBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_verified));
            }

            // Stock Status
            binding.tvStockStatus.setText(medicine.getStockStatusLabel());
            if (!medicine.isAvailable()) {
                binding.tvStockStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_emergency));
                binding.btnOrder.setEnabled(false);
                binding.btnOrder.setText("Out of Stock");
            } else if (medicine.isLowStock()) {
                binding.tvStockStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_warning));
                binding.btnOrder.setEnabled(true);
                binding.btnOrder.setText("Order");
            } else {
                binding.tvStockStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_verified));
                binding.btnOrder.setEnabled(true);
                binding.btnOrder.setText("Order");
            }

            binding.btnDetails.setOnClickListener(v -> listener.onShowDetails(medicine));
            binding.btnOrder.setOnClickListener(v -> listener.onPlaceOrder(medicine));
        }
    }
}
