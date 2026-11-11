package com.arohi.healthcare.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemUnifiedTrackingBinding;
import com.arohi.healthcare.models.UnifiedTrackingItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter displaying unified multi-category tracking cards
 * across Pharmacy Orders, Appointments, Operations, Payments, and Ambulance Dispatch.
 */
public class UnifiedTrackingAdapter extends RecyclerView.Adapter<UnifiedTrackingAdapter.ViewHolder> {

    public interface OnTrackingItemClickListener {
        void onViewTimelineClick(UnifiedTrackingItem item);
    }

    private final List<UnifiedTrackingItem> items = new ArrayList<>();
    private final OnTrackingItemClickListener listener;

    public UnifiedTrackingAdapter(@NonNull OnTrackingItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<UnifiedTrackingItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUnifiedTrackingBinding binding = ItemUnifiedTrackingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemUnifiedTrackingBinding b;

        ViewHolder(@NonNull ItemUnifiedTrackingBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(@NonNull UnifiedTrackingItem item) {
            Context ctx = b.getRoot().getContext();

            b.tvTrackingCategoryChip.setText(item.getCategory().getDisplayName().toUpperCase(Locale.US));
            b.tvTrackingLiveGpsChip.setVisibility(item.isHasLiveGps() ? View.VISIBLE : View.GONE);

            b.tvTrackingStatusBadge.setText(item.getCurrentStatus());
            styleStatusBadge(ctx, item);

            b.tvTrackingItemTitle.setText(item.getTitle());
            b.tvTrackingItemSubtitle.setText(item.getSubtitle());

            int pct = item.getProgressPercent();
            b.progressTrackingStep.setProgress(pct);
            if (item.getCurrentStepIndex() >= 0 && item.getTotalSteps() > 0) {
                b.tvTrackingStepCounter.setText(String.format(Locale.US, "Step %d/%d",
                        Math.min(item.getCurrentStepIndex() + 1, item.getTotalSteps()),
                        item.getTotalSteps()));
            } else {
                b.tvTrackingStepCounter.setText(item.getCurrentStatus());
            }

            b.tvTrackingEtaInfo.setText(item.getEtaOrScheduleText());
            b.tvTrackingActorInfo.setText(item.getActorSummary());
            b.tvTrackingDetailSummary.setText(item.getDetailSummary());

            b.btnViewTrackingTimeline.setOnClickListener(v -> listener.onViewTimelineClick(item));
            b.getRoot().setOnClickListener(v -> listener.onViewTimelineClick(item));
        }

        private void styleStatusBadge(@NonNull Context ctx, @NonNull UnifiedTrackingItem item) {
            String status = item.getCurrentStatus() != null ? item.getCurrentStatus().toLowerCase(Locale.US) : "";
            int bgColor;
            int fgColor;
            if (status.contains("cancel") || status.contains("fail") || status.contains("reject")) {
                bgColor = ContextCompat.getColor(ctx, R.color.status_rejected_bg);
                fgColor = ContextCompat.getColor(ctx, R.color.status_rejected);
            } else if (status.contains("complet") || status.contains("deliver")
                    || status.contains("collect") || status.contains("success")
                    || status.contains("follow")) {
                bgColor = ContextCompat.getColor(ctx, R.color.status_verified_bg);
                fgColor = ContextCompat.getColor(ctx, R.color.status_verified);
            } else {
                bgColor = ContextCompat.getColor(ctx, R.color.badge_pending_bg);
                fgColor = ContextCompat.getColor(ctx, R.color.badge_pending_text);
            }
            b.tvTrackingStatusBadge.setBackgroundTintList(ColorStateList.valueOf(bgColor));
            b.tvTrackingStatusBadge.setTextColor(fgColor);
        }
    }
}
