package com.arohi.healthcare.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemOtRoomBinding;
import com.arohi.healthcare.models.OTRoom;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for Operation Theatre (OT) Suites.
 */
public class OTRoomAdapter extends RecyclerView.Adapter<OTRoomAdapter.OTViewHolder> {

    public interface OnOTRoomActionListener {
        void onSetAvailable(@NonNull OTRoom room);
        void onSetSanitize(@NonNull OTRoom room);
    }

    private final OnOTRoomActionListener listener;
    private final List<OTRoom> items = new ArrayList<>();

    public OTRoomAdapter(@NonNull OnOTRoomActionListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<OTRoom> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OTViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOtRoomBinding binding = ItemOtRoomBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new OTViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OTViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class OTViewHolder extends RecyclerView.ViewHolder {
        private final ItemOtRoomBinding binding;

        OTViewHolder(@NonNull ItemOtRoomBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull OTRoom room, @NonNull OnOTRoomActionListener listener) {
            binding.tvOtRoomName.setText(safe(room.getRoomName(), "OT Suite"));
            binding.tvOtFloorAndSpecialties.setText(safe(room.getFloor(), "Floor 3") + " • " + safe(room.getSpecialties(), "General"));
            binding.tvOtEquipmentNotes.setText(safe(room.getEquipmentNotes(), "Equipped for surgical interventions"));

            String status = safe(room.getStatus(), OTRoom.STATUS_AVAILABLE);
            binding.tvOtRoomStatus.setText(status);

            if (OTRoom.STATUS_AVAILABLE.equalsIgnoreCase(status)) {
                binding.tvOtRoomStatus.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_verified));
            } else if (OTRoom.STATUS_OCCUPIED.equalsIgnoreCase(status)) {
                binding.tvOtRoomStatus.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_warning));
            } else {
                binding.tvOtRoomStatus.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_emergency));
            }

            binding.btnOtSetAvailable.setOnClickListener(v -> listener.onSetAvailable(room));
            binding.btnOtToggleSanitize.setOnClickListener(v -> listener.onSetSanitize(room));
        }

        private String safe(String v, String fallback) {
            return v != null && !v.trim().isEmpty() ? v.trim() : fallback;
        }
    }
}
