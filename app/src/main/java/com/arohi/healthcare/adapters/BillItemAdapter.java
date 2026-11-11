package com.arohi.healthcare.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.databinding.ItemBillLineItemBinding;
import com.arohi.healthcare.models.BillItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for rendering bill line items in checkout and receipt screens.
 */
public class BillItemAdapter extends RecyclerView.Adapter<BillItemAdapter.BillItemViewHolder> {

    private final List<BillItem> items = new ArrayList<>();

    public void submitList(@NonNull List<BillItem> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BillItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBillLineItemBinding binding = ItemBillLineItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BillItemViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BillItemViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class BillItemViewHolder extends RecyclerView.ViewHolder {
        private final ItemBillLineItemBinding binding;

        BillItemViewHolder(@NonNull ItemBillLineItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull BillItem item) {
            binding.tvItemDescription.setText(item.getDescription());
            binding.tvItemQty.setText("x" + item.getQuantity());
            binding.tvItemTotalPrice.setText(String.format(Locale.US, "৳ %.2f", item.getTotalPriceBdt()));
        }
    }
}
