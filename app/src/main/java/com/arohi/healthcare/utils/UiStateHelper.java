package com.arohi.healthcare.utils;

import android.view.View;
import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.IncludeLoadingErrorStateBinding;

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
}
