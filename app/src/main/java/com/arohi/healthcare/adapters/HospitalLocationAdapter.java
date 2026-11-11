package com.arohi.healthcare.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemHospitalLocationBinding;
import com.arohi.healthcare.models.HospitalLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for hospital department locations in the interactive hospital map.
 */
public class HospitalLocationAdapter extends RecyclerView.Adapter<HospitalLocationAdapter.LocationViewHolder> {

    public interface OnLocationSelectListener {
        void onLocationSelected(@NonNull HospitalLocation location);
        void onShowDirectionsClicked(@NonNull HospitalLocation location);
    }

    private final List<HospitalLocation> items = new ArrayList<>();
    private final OnLocationSelectListener listener;
    private String selectedLocationId = null;

    public HospitalLocationAdapter(@NonNull OnLocationSelectListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<HospitalLocation> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void setSelectedLocationId(String locationId) {
        this.selectedLocationId = locationId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHospitalLocationBinding binding = ItemHospitalLocationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new LocationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        holder.bind(items.get(position), selectedLocationId, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class LocationViewHolder extends RecyclerView.ViewHolder {

        private final ItemHospitalLocationBinding binding;

        public LocationViewHolder(@NonNull ItemHospitalLocationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(@NonNull HospitalLocation loc, String selectedId, @NonNull OnLocationSelectListener listener) {
            binding.tvLocationName.setText(loc.getName());
            binding.tvFloorBadge.setText(loc.getFloor());
            binding.tvWingAndRoom.setText(loc.getWing() + " • " + loc.getRoomNumber());
            binding.tvLocationDescription.setText(loc.getDescription());
            binding.tvWorkingHours.setText("Hours: " + loc.getWorkingHours());
            binding.tvContactExtension.setText(loc.getContactExtension());

            boolean isSelected = loc.getLocationId() != null && loc.getLocationId().equals(selectedId);
            if (isSelected) {
                binding.cardLocationRoot.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.primary_moss_green));
                binding.cardLocationRoot.setStrokeWidth(4);
            } else {
                binding.cardLocationRoot.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.border_stroke));
                binding.cardLocationRoot.setStrokeWidth(1);
            }

            binding.btnViewDirections.setOnClickListener(v -> listener.onShowDirectionsClicked(loc));
            binding.cardLocationRoot.setOnClickListener(v -> listener.onLocationSelected(loc));
        }
    }
}
