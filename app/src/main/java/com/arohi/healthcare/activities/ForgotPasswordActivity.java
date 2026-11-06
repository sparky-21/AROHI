package com.arohi.healthcare.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ActivityForgotPasswordBinding;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.AuthRepository;
import com.arohi.healthcare.utils.UiStateHelper;
import com.arohi.healthcare.utils.ValidationUtils;

/**
 * 9. Forgot Password Screen
 * Dispatches a secure password reset email via Firebase Authentication.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(this);
        binding.toolbar.tvToolbarBadge.setText("ACCOUNT RECOVERY");
        binding.toolbar.btnBack.setOnClickListener(v -> finish());

        binding.btnSendResetLink.setOnClickListener(v -> validateAndSendReset());
        binding.btnBackToLogin.setOnClickListener(v -> finish());
    }

    private void validateAndSendReset() {
        binding.tilEmail.setError(null);
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";

        if (!ValidationUtils.isValidEmail(email)) {
            binding.tilEmail.setError("Please enter a valid email address.");
            UiStateHelper.showError(binding.stateView, "Invalid Email", "Enter the email address registered with your AROHI account.");
            return;
        }

        binding.btnSendResetLink.setEnabled(false);
        UiStateHelper.showLoading(binding.stateView, "Sending password reset instructions…");

        authRepository.sendPasswordResetEmail(email, new AuthCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                binding.btnSendResetLink.setEnabled(true);
                UiStateHelper.showSuccess(
                        binding.stateView,
                        "Reset Link Dispatched",
                        getString(R.string.reset_link_sent_message)
                );
            }

            @Override
            public void onError(String errorMessage) {
                binding.btnSendResetLink.setEnabled(true);
                UiStateHelper.showError(binding.stateView, "Password Reset Failed", errorMessage);
            }
        });
    }
}
