package com.arohi.healthcare.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.arohi.healthcare.databinding.ActivityDoctorRegistrationBinding;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.navigation.AuthNavigationRouter;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.AuthRepository;
import com.arohi.healthcare.utils.UiStateHelper;
import com.arohi.healthcare.utils.ValidationUtils;

/**
 * 5. Doctor Registration Screen
 * Captures medical license, specialty, qualifications, department, and experience.
 * Enforces PENDING verification status until Hospital Authority approval.
 */
public class DoctorRegistrationActivity extends AppCompatActivity {

    private ActivityDoctorRegistrationBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDoctorRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(this);
        binding.toolbar.tvToolbarBadge.setText("DOCTOR CREDENTIALS");
        binding.toolbar.btnBack.setOnClickListener(v -> finish());

        if (!authRepository.isFirebaseConnected()) {
            UiStateHelper.showWarning(
                    binding.stateView,
                    "Local Sandbox Mode Active",
                    "google-services.json is not yet placed in app/. Doctor credentials will be saved locally with PENDING verification status."
            );
        }

        binding.btnRegisterDoctor.setOnClickListener(v -> validateAndSubmit());
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
        String license = getText(binding.etMedicalLicense);
        String specialty = getText(binding.etSpecialty);
        String qualifications = getText(binding.etQualifications);
        String department = getText(binding.etDepartment);
        String experienceStr = getText(binding.etExperienceYears);
        String feeStr = getText(binding.etConsultationFee);
        String password = getText(binding.etPassword);
        String confirmPassword = getText(binding.etConfirmPassword);

        boolean valid = true;

        if (!ValidationUtils.isValidFullName(fullName)) {
            binding.tilFullName.setError("Enter your full legal name.");
            valid = false;
        }
        if (!ValidationUtils.isValidEmail(email)) {
            binding.tilEmail.setError("Enter a valid professional email address.");
            valid = false;
        }
        if (!ValidationUtils.isValidPhone(phone)) {
            binding.tilPhone.setError("Enter a valid 10-15 digit phone number.");
            valid = false;
        }
        if (!ValidationUtils.isValidProfessionalLicense(license)) {
            binding.tilMedicalLicense.setError("Enter a valid BMDC / Medical License number (5-24 alphanumeric characters).");
            valid = false;
        }
        if (specialty.length() < 3) {
            binding.tilSpecialty.setError("Specify your primary medical specialty (e.g., Cardiology).");
            valid = false;
        }
        if (qualifications.length() < 2) {
            binding.tilQualifications.setError("Enter your medical qualifications (e.g., MBBS, FCPS).");
            valid = false;
        }
        if (department.length() < 2) {
            binding.tilDepartment.setError("Enter your hospital department.");
            valid = false;
        }
        if (!ValidationUtils.isValidExperienceYears(experienceStr)) {
            binding.tilExperienceYears.setError("Enter valid years (0-65).");
            valid = false;
        }
        if (!ValidationUtils.isValidConsultationFee(feeStr)) {
            binding.tilConsultationFee.setError("Enter a valid consultation fee.");
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
            UiStateHelper.showError(binding.stateView, "Validation Error", "Please verify all professional credential fields.");
            return;
        }

        int experienceYears = Integer.parseInt(experienceStr);
        double consultationFee = Double.parseDouble(feeStr);

        setFormEnabled(false);
        UiStateHelper.showLoading(binding.stateView, "Submitting Doctor credentials for verification…");

        authRepository.registerDoctor(
                fullName, email, phone, license, specialty, qualifications,
                department, experienceYears, consultationFee, password,
                new AuthCallback<UserAccount>() {
                    @Override
                    public void onSuccess(UserAccount result) {
                        setFormEnabled(true);
                        UiStateHelper.hideLoading(binding.stateView);
                        AuthNavigationRouter.routeAuthenticatedUser(DoctorRegistrationActivity.this, result);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        setFormEnabled(true);
                        UiStateHelper.showError(binding.stateView, "Doctor Registration Failed", errorMessage);
                    }
                }
        );
    }

    private void clearFieldErrors() {
        binding.tilFullName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPhone.setError(null);
        binding.tilMedicalLicense.setError(null);
        binding.tilSpecialty.setError(null);
        binding.tilQualifications.setError(null);
        binding.tilDepartment.setError(null);
        binding.tilExperienceYears.setError(null);
        binding.tilConsultationFee.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);
    }

    private void setFormEnabled(boolean enabled) {
        binding.btnRegisterDoctor.setEnabled(enabled);
        binding.btnGoToLogin.setEnabled(enabled);
    }

    private String getText(com.google.android.material.textfield.TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
