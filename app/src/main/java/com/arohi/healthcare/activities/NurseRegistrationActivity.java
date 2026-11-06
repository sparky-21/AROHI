package com.arohi.healthcare.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.arohi.healthcare.databinding.ActivityNurseRegistrationBinding;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.navigation.AuthNavigationRouter;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.AuthRepository;
import com.arohi.healthcare.utils.UiStateHelper;
import com.arohi.healthcare.utils.ValidationUtils;

/**
 * 6. Nurse Registration Screen
 * Captures Nursing Council license, qualification, preferred ward, and shift preference.
 * Enforces PENDING verification status until Hospital Authority approval.
 */
public class NurseRegistrationActivity extends AppCompatActivity {

    private ActivityNurseRegistrationBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNurseRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(this);
        binding.toolbar.tvToolbarBadge.setText("NURSING CREDENTIALS");
        binding.toolbar.btnBack.setOnClickListener(v -> finish());

        if (!authRepository.isFirebaseConnected()) {
            UiStateHelper.showWarning(
                    binding.stateView,
                    "Local Sandbox Mode Active",
                    "google-services.json is not yet placed in app/. Nurse credentials will be saved locally with PENDING verification status."
            );
        }

        binding.btnRegisterNurse.setOnClickListener(v -> validateAndSubmit());
        binding.btnGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void validateAndSubmit() {
        clearFieldErrors();

        String fullName = getText(binding.etFullName);
        String email = getText(binding.etEmail);
        String phone = getText(binding.etPhone);
        String nursingLicense = getText(binding.etNursingLicense);
        String qualification = getText(binding.etQualification);
        String preferredWard = getText(binding.etPreferredWard);
        String shiftPreference = getText(binding.etShiftPreference);
        String password = getText(binding.etPassword);
        String confirmPassword = getText(binding.etConfirmPassword);

        boolean valid = true;

        if (!ValidationUtils.isValidFullName(fullName)) {
            binding.tilFullName.setError("Enter your full legal name.");
            valid = false;
        }
        if (!ValidationUtils.isValidEmail(email)) {
            binding.tilEmail.setError("Enter a valid email address.");
            valid = false;
        }
        if (!ValidationUtils.isValidPhone(phone)) {
            binding.tilPhone.setError("Enter a valid 10-15 digit phone number.");
            valid = false;
        }
        if (!ValidationUtils.isValidProfessionalLicense(nursingLicense)) {
            binding.tilNursingLicense.setError("Enter a valid Nursing Council Registration No. (5-24 chars).");
            valid = false;
        }
        if (qualification.length() < 2) {
            binding.tilQualification.setError("Enter your nursing qualification (e.g., BSc in Nursing).");
            valid = false;
        }
        if (preferredWard.length() < 2) {
            binding.tilPreferredWard.setError("Specify your primary ward or department.");
            valid = false;
        }
        if (shiftPreference.length() < 2) {
            binding.tilShiftPreference.setError("Enter your duty shift preference (Morning / Evening / Night).");
            valid = false;
        }
        if (!ValidationUtils.isValidPassword(password)) {
            binding.tilPassword.setError("Password must be at least 8 characters with letters and numbers.");
            valid = false;
        }
        if (!ValidationUtils.doPasswordsMatch(password, confirmPassword)) {
            binding.tilConfirmPassword.setError("Passwords do not match.");
            valid = false;
        }

        if (!valid) {
            UiStateHelper.showError(binding.stateView, "Validation Error", "Please complete all nursing registration fields.");
            return;
        }

        setFormEnabled(false);
        UiStateHelper.showLoading(binding.stateView, "Submitting Nurse credentials for verification…");

        authRepository.registerNurse(
                fullName, email, phone, nursingLicense, qualification,
                preferredWard, shiftPreference, password,
                new AuthCallback<UserAccount>() {
                    @Override
                    public void onSuccess(UserAccount result) {
                        setFormEnabled(true);
                        UiStateHelper.hideLoading(binding.stateView);
                        AuthNavigationRouter.routeAuthenticatedUser(NurseRegistrationActivity.this, result);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        setFormEnabled(true);
                        UiStateHelper.showError(binding.stateView, "Nurse Registration Failed", errorMessage);
                    }
                }
        );
    }

    private void clearFieldErrors() {
        binding.tilFullName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPhone.setError(null);
        binding.tilNursingLicense.setError(null);
        binding.tilQualification.setError(null);
        binding.tilPreferredWard.setError(null);
        binding.tilShiftPreference.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);
    }

    private void setFormEnabled(boolean enabled) {
        binding.btnRegisterNurse.setEnabled(enabled);
        binding.btnGoToLogin.setEnabled(enabled);
    }

    private String getText(com.google.android.material.textfield.TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
