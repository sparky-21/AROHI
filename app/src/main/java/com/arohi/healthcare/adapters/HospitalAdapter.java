package com.arohi.healthcare.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.databinding.ItemHospitalCardBinding;
import com.arohi.healthcare.models.Hospital;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for presenting accredited hospitals and facilities in the pre-login discovery catalog.
 */
public class HospitalAdapter extends RecyclerView.Adapter<HospitalAdapter.HospitalViewHolder> {

    public interface OnHospitalActionListener {
        void onExploreHospital(@NonNull Hospital hospital);
        void onSelectHospital(@NonNull Hospital hospital);
    }

    private final List<Hospital> hospitals = new ArrayList<>();
    private final OnHospitalActionListener listener;

    public HospitalAdapter(@NonNull OnHospitalActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Hospital> list) {
        hospitals.clear();
        if (list != null) {
            hospitals.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HospitalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHospitalCardBinding binding = ItemHospitalCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new HospitalViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HospitalViewHolder holder, int position) {
        holder.bind(hospitals.get(position));
    }

    @Override
    public int getItemCount() {
        return hospitals.size();
    }

    class HospitalViewHolder extends RecyclerView.ViewHolder {

        private final ItemHospitalCardBinding binding;

        HospitalViewHolder(ItemHospitalCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Hospital hospital) {
            binding.tvHospitalName.setText(hospital.getHospitalName());
            binding.tvHospitalCity.setText(hospital.getCity() != null ? hospital.getCity() : "Bangladesh");
            binding.tvHospitalCode.setText(hospital.getHospitalCode());
            binding.tvHospitalAddress.setText(hospital.getAddress() != null ? hospital.getAddress() : "");
            binding.tvBedCount.setText("🛏️ " + hospital.getTotalBeds() + " Beds");

            if (hospital.getEmergencyHotline() != null && !hospital.getEmergencyHotline().isEmpty()) {
                binding.tvHotline.setText("📞 " + hospital.getEmergencyHotline());
                binding.tvHotline.setVisibility(View.VISIBLE);
            } else {
                binding.tvHotline.setVisibility(View.GONE);
            }

            if (hospital.isVerified()) {
                binding.tvVerificationStatus.setText("✓ VERIFIED");
                binding.tvVerificationStatus.setVisibility(View.VISIBLE);
            } else {
                binding.tvVerificationStatus.setText("PENDING REVIEW");
                binding.tvVerificationStatus.setVisibility(View.VISIBLE);
            }

            if (hospital.getSupportedDepartments() != null && !hospital.getSupportedDepartments().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < hospital.getSupportedDepartments().size(); i++) {
                    sb.append(hospital.getSupportedDepartments().get(i));
                    if (i < hospital.getSupportedDepartments().size() - 1) {
                        sb.append(" • ");
                    }
                }
                binding.tvDepartmentsPreview.setText(sb.toString());
                binding.tvDepartmentsPreview.setVisibility(View.VISIBLE);
            } else {
                binding.tvDepartmentsPreview.setVisibility(View.GONE);
            }

            binding.btnExploreHospital.setOnClickListener(v -> listener.onExploreHospital(hospital));
            binding.btnSelectHospital.setOnClickListener(v -> listener.onSelectHospital(hospital));
        }
    }
}
