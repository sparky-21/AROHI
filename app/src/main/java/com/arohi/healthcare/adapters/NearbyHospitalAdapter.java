package com.arohi.healthcare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemNearbyHospitalBinding;
import com.arohi.healthcare.models.Hospital;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for rendering nearby hospitals with distance calculations,
 * verification indicators, quick directions, and institutional selection.
 */
public class NearbyHospitalAdapter extends RecyclerView.Adapter<NearbyHospitalAdapter.HospitalViewHolder> {

    public interface OnNearbyHospitalClickListener {
        void onHospitalClick(@NonNull Hospital hospital);
        void onDirectionsClick(@NonNull Hospital hospital);
        void onSelectHospitalClick(@NonNull Hospital hospital);
    }

    private final List<Hospital> items = new ArrayList<>();
    private final OnNearbyHospitalClickListener listener;
    private double userLatitude = 0.0;
    private double userLongitude = 0.0;

    public NearbyHospitalAdapter(@NonNull OnNearbyHospitalClickListener listener) {
        this.listener = listener;
    }

    public void updateData(@NonNull List<Hospital> newItems, double userLat, double userLng) {
        this.items.clear();
        this.items.addAll(newItems);
        this.userLatitude = userLat;
        this.userLongitude = userLng;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HospitalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNearbyHospitalBinding binding = ItemNearbyHospitalBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new HospitalViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HospitalViewHolder holder, int position) {
        holder.bind(items.get(position), userLatitude, userLongitude, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HospitalViewHolder extends RecyclerView.ViewHolder {
        private final ItemNearbyHospitalBinding binding;

        HospitalViewHolder(@NonNull ItemNearbyHospitalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Hospital hospital, double userLat, double userLng,
                  @NonNull OnNearbyHospitalClickListener listener) {
            Context context = binding.getRoot().getContext();

            binding.tvHospitalName.setText(hospital.getHospitalName());
            binding.tvHospitalCode.setText("Code: " + hospital.getHospitalCode());
            binding.tvHospitalAddress.setText(hospital.getAddress() != null ? hospital.getAddress() : hospital.getCity());

            // Distance calculation
            if (userLat != 0.0 && userLng != 0.0 && hospital.getLatitude() != 0.0 && hospital.getLongitude() != 0.0) {
                double distKm = hospital.calculateDistanceKm(userLat, userLng);
                if (distKm >= 0.0) {
                    binding.tvDistanceBadge.setText(String.format(Locale.getDefault(), "%.1f km away", distKm));
                    binding.tvDistanceBadge.setVisibility(View.VISIBLE);
                } else {
                    binding.tvDistanceBadge.setVisibility(View.GONE);
                }
            } else {
                binding.tvDistanceBadge.setVisibility(View.GONE);
            }

            // Verification status
            if (hospital.isVerified()) {
                binding.tvVerificationBadge.setText("✓ " + context.getString(R.string.status_verified));
                binding.tvVerificationBadge.setTextColor(ContextCompat.getColor(context, R.color.status_verified));
            } else {
                binding.tvVerificationBadge.setText(context.getString(R.string.status_pending));
                binding.tvVerificationBadge.setTextColor(ContextCompat.getColor(context, R.color.status_warning));
            }

            // Beds count
            binding.tvBedCount.setText(context.getString(R.string.label_operational_beds, hospital.getTotalBeds()));

            // Hotline
            if (hospital.getEmergencyHotline() != null && !hospital.getEmergencyHotline().trim().isEmpty()) {
                binding.tvHotline.setText("Hotline: " + hospital.getEmergencyHotline());
                binding.tvHotline.setVisibility(View.VISIBLE);
            } else {
                binding.tvHotline.setVisibility(View.GONE);
            }

            // Departments preview
            if (hospital.getSupportedDepartments() != null && !hospital.getSupportedDepartments().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (String dept : hospital.getSupportedDepartments()) {
                    if (count > 0) sb.append(" • ");
                    sb.append(dept);
                    count++;
                    if (count >= 4) break;
                }
                binding.tvDepartmentsPreview.setText(sb.toString());
                binding.tvDepartmentsPreview.setVisibility(View.VISIBLE);
            } else {
                binding.tvDepartmentsPreview.setVisibility(View.GONE);
            }

            // Click Handlers
            binding.getRoot().setOnClickListener(v -> listener.onHospitalClick(hospital));
            binding.btnItemDirections.setOnClickListener(v -> listener.onDirectionsClick(hospital));
            binding.btnItemSelect.setOnClickListener(v -> listener.onSelectHospitalClick(hospital));
        }
    }
}
