package com.arohi.healthcare.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.databinding.ItemFamilyMemberBinding;
import com.arohi.healthcare.models.FamilyMember;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying authorized family health profiles.
 */
public class FamilyMemberAdapter extends RecyclerView.Adapter<FamilyMemberAdapter.FamilyViewHolder> {

    public interface OnFamilyMemberActionListener {
        void onBookAppointmentForMember(@NonNull FamilyMember member);
        void onRemoveFamilyMember(@NonNull FamilyMember member);
    }

    private final List<FamilyMember> items = new ArrayList<>();
    private final OnFamilyMemberActionListener listener;

    public FamilyMemberAdapter(@NonNull OnFamilyMemberActionListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<FamilyMember> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FamilyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFamilyMemberBinding binding = ItemFamilyMemberBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new FamilyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FamilyViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class FamilyViewHolder extends RecyclerView.ViewHolder {

        private final ItemFamilyMemberBinding binding;

        public FamilyViewHolder(@NonNull ItemFamilyMemberBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(@NonNull FamilyMember member, @NonNull OnFamilyMemberActionListener listener) {
            binding.tvFamilyMemberName.setText(member.getFullName());
            binding.tvRelationship.setText("Relationship: " + member.getRelationship()
                    + (member.getGender() != null && !member.getGender().isEmpty() ? " (" + member.getGender() + ")" : ""));

            String blood = member.getBloodGroup() != null && !member.getBloodGroup().isEmpty() ? member.getBloodGroup() : "Unknown";
            binding.tvBloodGroupBadge.setText(blood);

            String dob = member.getDateOfBirth() != null && !member.getDateOfBirth().isEmpty() ? member.getDateOfBirth() : "Not Set";
            String allergies = member.getKnownAllergies() != null && !member.getKnownAllergies().isEmpty() ? member.getKnownAllergies() : "None";
            binding.tvDobAndAllergies.setText("DOB: " + dob + " • Allergies: " + allergies);

            if (member.isHasExplicitAuthorization()) {
                binding.tvAuthorizationNote.setText("✓ Explicitly authorized for appointment bookings & reminders.");
            } else {
                binding.tvAuthorizationNote.setText("⚠️ Awaiting authorization consent.");
            }

            binding.btnBookForMember.setOnClickListener(v -> listener.onBookAppointmentForMember(member));
            binding.btnRemoveFamilyMember.setOnClickListener(v -> listener.onRemoveFamilyMember(member));
        }
    }
}
