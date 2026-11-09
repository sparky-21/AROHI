package com.arohi.healthcare.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.databinding.ItemHospitalServiceBinding;
import com.arohi.healthcare.models.HospitalService;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for Hospital Services (Emergency, ICU, Ambulance, Pathology, Blood Bank, Dialysis).
 */
public class HospitalServiceAdapter extends RecyclerView.Adapter<HospitalServiceAdapter.ServiceViewHolder> {

    public interface OnServiceToggleListener {
        void onToggleService(@NonNull HospitalService service, boolean active);
    }

    private final OnServiceToggleListener listener;
    private final List<HospitalService> items = new ArrayList<>();

    public HospitalServiceAdapter(@NonNull OnServiceToggleListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<HospitalService> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHospitalServiceBinding binding = ItemHospitalServiceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ServiceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ServiceViewHolder extends RecyclerView.ViewHolder {
        private final ItemHospitalServiceBinding binding;

        ServiceViewHolder(@NonNull ItemHospitalServiceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull HospitalService s, @NonNull OnServiceToggleListener listener) {
            binding.tvServiceName.setText(safe(s.getServiceName(), "Hospital Service"));
            binding.tvServiceHoursAndExt.setText(
                    safe(s.getOperatingHours(), "24/7") + " • "
                            + safe(s.getContactExtension(), "Ext: 100") + " • Lead: "
                            + safe(s.getLeadDoctorOrHead(), "Clinical Lead"));
            binding.tvServiceDescription.setText(safe(s.getDescription(), "Clinical hospital service"));

            binding.switchServiceActive.setOnCheckedChangeListener(null);
            binding.switchServiceActive.setChecked(s.isActive());
            binding.switchServiceActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                s.setActive(isChecked);
                listener.onToggleService(s, isChecked);
            });
        }

        private String safe(String v, String fallback) {
            return v != null && !v.trim().isEmpty() ? v.trim() : fallback;
        }
    }
}
