package com.arohi.healthcare.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ActivityEmailVerificationBinding;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.AuthRepository;
import com.arohi.healthcare.utils.UiStateHelper;

/**
 * 10. Email Verification Screen
 * Allows newly registered users to verify their email address, resend the verification link,
 * and proceed to Basic Profile Setup.
 */
public class EmailVerificationActivity extends BaseActivity {

    private ActivityEmailVerificationBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEmailVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(this);
        binding.toolbar.tvToolbarBadge.setText("EMAIL VERIFICATION");
        binding.toolbar.btnBack.setOnClickListener(v -> finish());

        UserAccount current = authRepository.getSessionManager().getCurrentSessionAccount();
        if (current != null) {
            binding.tvVerificationEmailTarget.setText(
                    "We sent a verification link to " + current.getEmail() + ". Please verify your email address to protect your AROHI healthcare identity."
            );
        }

        if (!authRepository.isFirebaseConnected()) {
            UiStateHelper.showWarning(
                    binding.stateView,
                    "Local Sandbox Verification",
                    "Firebase google-services.json is not yet attached. Tap 'I Have Verified My Email' to simulate verification in Local Sandbox mode."
            );
        }

        binding.btnCheckVerification.setOnClickListener(v -> checkVerification());
        binding.btnResendVerification.setOnClickListener(v -> resendVerification());
        binding.btnContinueToProfileSetup.setOnClickListener(v -> verifyDemoEmailAndProceed());
        binding.btnSignOut.setOnClickListener(v -> {
            authRepository.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void verifyDemoEmailAndProceed() {
        UiStateHelper.showLoading(binding.stateView, "Activating demo email verification…");
        authRepository.confirmDemoOrManualEmailVerification(new AuthCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                binding.tvCurrentEmailStatus.setText("Status: Demo Email Verified");
                startActivity(new Intent(EmailVerificationActivity.this, RolePortalActivity.class));
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                startActivity(new Intent(EmailVerificationActivity.this, RolePortalActivity.class));
                finish();
            }
        });
    }

    private void checkVerification() {
        UiStateHelper.showLoading(binding.stateView, "Checking email verification status…");
        authRepository.checkEmailVerificationStatus(new AuthCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean verified) {
                if (Boolean.TRUE.equals(verified)) {
                    binding.tvCurrentEmailStatus.setText("Status: Email Verified");
                    UiStateHelper.showSuccess(
                            binding.stateView,
                            "Email Verified",
                            "Your email address has been verified. Redirecting to your Role Dashboard…"
                    );
                    startActivity(new Intent(EmailVerificationActivity.this, RolePortalActivity.class));
                    finish();
                } else {
                    UiStateHelper.showWarning(
                            binding.stateView,
                            "Inbox Link Not Clicked Yet (Demo Email?)",
                            "If you are using a demo email without a real inbox, tap 'Demo Email: Verify & Continue' below to proceed."
                    );
                }
            }

            @Override
            public void onError(String errorMessage) {
                UiStateHelper.showError(binding.stateView, "Verification Check Failed", errorMessage);
            }
        });
    }

    private void resendVerification() {
        UiStateHelper.showLoading(binding.stateView, "Resending verification email…");
        authRepository.resendVerificationEmail(new AuthCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                UiStateHelper.showSuccess(
                        binding.stateView,
                        "Verification Email Sent",
                        getString(R.string.verification_sent_feedback)
                );
            }

            @Override
            public void onError(String errorMessage) {
                UiStateHelper.showError(binding.stateView, "Could Not Resend Email", errorMessage);
            }
        });
    }
}
