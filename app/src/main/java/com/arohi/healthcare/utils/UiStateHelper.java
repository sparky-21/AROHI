package com.arohi.healthcare.utils;

import android.content.res.ColorStateList;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.IncludeLoadingErrorStateBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

/**
 * Reusable UI controller for Authentication Loading, Error, Warning, and Success states.
 */
public final class UiStateHelper {

    private UiStateHelper() {
        // Prevent instantiation
    }

    public static void showLoading(IncludeLoadingErrorStateBinding binding, String loadingMessage) {
        if (binding == null) {
            return;
        }
        binding.bannerFeedback.setVisibility(View.GONE);
        binding.cardLoading.setVisibility(View.VISIBLE);
        if (loadingMessage != null && !loadingMessage.isEmpty()) {
            binding.tvLoadingMessage.setText(loadingMessage);
        }
    }

    public static void hideLoading(IncludeLoadingErrorStateBinding binding) {
        if (binding == null) {
            return;
        }
        binding.cardLoading.setVisibility(View.GONE);
    }

    public static void showError(IncludeLoadingErrorStateBinding binding, String title, String message) {
        if (binding == null) {
            return;
        }
        binding.cardLoading.setVisibility(View.GONE);
        binding.bannerFeedback.setBackgroundResource(R.drawable.bg_status_error);
        binding.ivFeedbackIcon.setImageResource(R.drawable.ic_warning_alert);
        binding.tvFeedbackTitle.setText(title != null ? title : "Authentication Error");
        binding.tvFeedbackMessage.setText(message != null ? message : "An unexpected error occurred.");
        binding.bannerFeedback.setVisibility(View.VISIBLE);
    }

    public static void showWarning(IncludeLoadingErrorStateBinding binding, String title, String message) {
        if (binding == null) {
            return;
        }
        binding.cardLoading.setVisibility(View.GONE);
        binding.bannerFeedback.setBackgroundResource(R.drawable.bg_status_pending);
        binding.ivFeedbackIcon.setImageResource(R.drawable.ic_pending_clock);
        binding.tvFeedbackTitle.setText(title != null ? title : "Notice");
        binding.tvFeedbackMessage.setText(message != null ? message : "");
        binding.bannerFeedback.setVisibility(View.VISIBLE);
    }

    public static void showSuccess(IncludeLoadingErrorStateBinding binding, String title, String message) {
        if (binding == null) {
            return;
        }
        binding.cardLoading.setVisibility(View.GONE);
        binding.bannerFeedback.setBackgroundResource(R.drawable.bg_status_verified);
        binding.ivFeedbackIcon.setImageResource(R.drawable.ic_verified_badge);
        binding.tvFeedbackTitle.setText(title != null ? title : "Success");
        binding.tvFeedbackMessage.setText(message != null ? message : "");
        binding.bannerFeedback.setVisibility(View.VISIBLE);
    }

    public static void clearState(IncludeLoadingErrorStateBinding binding) {
        if (binding == null) {
            return;
        }
        binding.cardLoading.setVisibility(View.GONE);
        binding.bannerFeedback.setVisibility(View.GONE);
    }

    public static void showSuccess(View anchorView, String message) {
        if (anchorView == null || message == null) {
            return;
        }
        Snackbar.make(anchorView, message, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(anchorView.getContext().getColor(R.color.status_verified))
                .setTextColor(anchorView.getContext().getColor(R.color.white))
                .show();
    }

    public static void showError(View anchorView, String message) {
        if (anchorView == null || message == null) {
            return;
        }
        Snackbar.make(anchorView, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(anchorView.getContext().getColor(R.color.status_emergency))
                .setTextColor(anchorView.getContext().getColor(R.color.white))
                .show();
    }

    public static void showWarning(View anchorView, String message) {
        if (anchorView == null || message == null) {
            return;
        }
        Snackbar.make(anchorView, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(anchorView.getContext().getColor(R.color.status_warning))
                .setTextColor(anchorView.getContext().getColor(R.color.white))
                .show();
    }

    /**
     * Moves the active visual highlight box across segmented mode buttons or filter chips.
     */
    public static void updateToggleButtons(MaterialButton selected, int unselectedBgColorRes, MaterialButton... allButtons) {
        if (selected == null || allButtons == null) {
            return;
        }
        int activeBg = ContextCompat.getColor(selected.getContext(), R.color.primary_moss_green);
        int activeText = ContextCompat.getColor(selected.getContext(), R.color.white);
        int inactiveBg = ContextCompat.getColor(selected.getContext(), unselectedBgColorRes);
        int inactiveText = ContextCompat.getColor(selected.getContext(), R.color.deep_forest_green);

        for (MaterialButton btn : allButtons) {
            if (btn == null) continue;
            boolean isSelected = (btn == selected);
            btn.setBackgroundTintList(ColorStateList.valueOf(isSelected ? activeBg : inactiveBg));
            btn.setTextColor(isSelected ? activeText : inactiveText);
        }
    }
}
