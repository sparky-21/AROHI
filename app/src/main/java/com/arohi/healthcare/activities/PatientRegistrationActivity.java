package com.arohi.healthcare.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.arohi.healthcare.databinding.ActivityPatientRegistrationBinding;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.navigation.AuthNavigationRouter;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.AuthRepository;
import com.arohi.healthcare.utils.DatePickerHelper;
import com.arohi.healthcare.utils.UiStateHelper;
import com.arohi.healthcare.utils.ValidationUtils;

/**
 * 4. Patient Registration Screen
 * Validates personal and emergency health details and creates a verified Patient identity.
 */
public class PatientRegistrationActivity extends BaseActivity {

    private ActivityPatientRegistrationBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPatientRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(this);
        binding.toolbar.tvToolbarBadge.setText("PATIENT PORTAL");
        binding.toolbar.btnBack.setOnClickListener(v -> finish());

        // Attach interactive calendar picker (YYYY/MM/DD) to Date of Birth field
        DatePickerHelper.attachDatePicker(this, binding.tilDob, binding.etDob, true);

        if (!authRepository.isFirebaseConnected()) {
            UiStateHelper.showWarning(
                    binding.stateView,
                    "Local Sandbox Mode Active",
                    "google-services.json is not yet placed in app/. Registration will be validated and saved in local sandbox storage."
            );
        }

        binding.btnRegisterPatient.setOnClickListener(v -> validateAndSubmit());
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
        String dob = getText(binding.etDob);
        String bloodGroup = getText(binding.etBloodGroup);
        String gender = getText(binding.etGender);
        String emergencyName = getText(binding.etEmergencyName);
        String emergencyPhone = getText(binding.etEmergencyPhone);
        String allergies = getText(binding.etAllergies);
        String password = getText(binding.etPassword);
        String confirmPassword = getText(binding.etConfirmPassword);

        boolean valid = true;

        if (!ValidationUtils.isValidFullName(fullName)) {
            binding.tilFullName.setError("Enter your full legal name (at least 3 characters).");
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
        if (!ValidationUtils.isValidDateOfBirth(dob)) {
            binding.tilDob.setError("Use YYYY/MM/DD format (e.g., 1996/05/21) or tap the calendar icon.");
            valid = false;
        }
        if (!ValidationUtils.isValidBloodGroup(bloodGroup)) {
            binding.tilBloodGroup.setError("Valid blood group required (A+, A-, B+, B-, AB+, AB-, O+, O-).");
            valid = false;
        }
        if (gender.isEmpty()) {
            binding.tilGender.setError("Please specify your gender.");
            valid = false;
        }
        if (!ValidationUtils.isValidFullName(emergencyName)) {
            binding.tilEmergencyName.setError("Enter an emergency contact name.");
            valid = false;
        }
        if (!ValidationUtils.isValidPhone(emergencyPhone)) {
            binding.tilEmergencyPhone.setError("Enter a valid emergency contact phone number.");
            valid = false;
        }
        if (!ValidationUtils.isValidPassword(password)) {
            binding.tilPassword.setError("Password must be at least 8 characters and include letters and numbers.");
            valid = false;
        }
        if (!ValidationUtils.doPasswordsMatch(password, confirmPassword)) {
            binding.tilConfirmPassword.setError("Passwords do not match.");
            valid = false;
        }

        if (!valid) {
            UiStateHelper.showError(binding.stateView, "Validation Error", "Please correct the highlighted fields before continuing.");
            return;
        }

        setFormEnabled(false);
        UiStateHelper.showLoading(binding.stateView, "Creating your AROHI Patient Identity…");

        authRepository.registerPatient(
                fullName, email, phone, dob, gender, bloodGroup,
                emergencyName, emergencyPhone, allergies, password,
                new AuthCallback<UserAccount>() {
                    @Override
                    public void onSuccess(UserAccount result) {
                        setFormEnabled(true);
                        UiStateHelper.hideLoading(binding.stateView);
                        AuthNavigationRouter.routeAuthenticatedUser(PatientRegistrationActivity.this, result);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        setFormEnabled(true);
                        UiStateHelper.showError(binding.stateView, "Registration Failed", errorMessage);
                    }
                }
        );
    }

    private void clearFieldErrors() {
        binding.tilFullName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPhone.setError(null);
        binding.tilDob.setError(null);
        binding.tilBloodGroup.setError(null);
        binding.tilGender.setError(null);
        binding.tilEmergencyName.setError(null);
        binding.tilEmergencyPhone.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);
    }

    private void setFormEnabled(boolean enabled) {
        binding.btnRegisterPatient.setEnabled(enabled);
        binding.btnGoToLogin.setEnabled(enabled);
    }

    private String getText(com.google.android.material.textfield.TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
