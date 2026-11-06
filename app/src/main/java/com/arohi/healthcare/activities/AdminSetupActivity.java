package com.arohi.healthcare.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.arohi.healthcare.databinding.ActivityAdminSetupBinding;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.navigation.AuthNavigationRouter;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.AuthRepository;
import com.arohi.healthcare.utils.UiStateHelper;
import com.arohi.healthcare.utils.ValidationUtils;

/**
 * 7. Authorized Admin Setup Screen
 * Prevents ordinary users from self-registering as administrators.
 * Requires a valid institutional token format (AROHI-AUTH-XXXXXX) and server-side
 * /adminAuthorizations/{uid} verification in Firestore.
 */
public class AdminSetupActivity extends AppCompatActivity {

    private ActivityAdminSetupBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(this);
        binding.toolbar.tvToolbarBadge.setText("RESTRICTED ADMIN SETUP");
        binding.toolbar.btnBack.setOnClickListener(v -> finish());

        binding.btnSetupAdmin.setOnClickListener(v -> validateAndSubmit());
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
        String employeeId = getText(binding.etEmployeeId);
        String designation = getText(binding.etDesignation);
        String authToken = getText(binding.etAuthToken);
        String password = getText(binding.etPassword);
        String confirmPassword = getText(binding.etConfirmPassword);

        boolean valid = true;

        if (!ValidationUtils.isValidFullName(fullName)) {
            binding.tilFullName.setError("Enter full legal name.");
            valid = false;
        }
        if (!ValidationUtils.isValidEmail(email)) {
            binding.tilEmail.setError("Enter institutional email address.");
            valid = false;
        }
        if (!ValidationUtils.isValidPhone(phone)) {
            binding.tilPhone.setError("Enter valid 10-15 digit phone number.");
            valid = false;
        }
        if (!ValidationUtils.isValidProfessionalLicense(employeeId)) {
            binding.tilEmployeeId.setError("Enter a valid Hospital Authority Employee ID.");
            valid = false;
        }
        if (designation.length() < 3) {
            binding.tilDesignation.setError("Enter your official hospital designation.");
            valid = false;
        }
        if (!ValidationUtils.isValidAdminTokenFormat(authToken)) {
            binding.tilAuthToken.setError("Invalid token format. Expected: AROHI-AUTH-[6-16 uppercase alphanumeric].");
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
            UiStateHelper.showError(
                    binding.stateView,
                    "Unauthorized / Invalid Input",
                    "Ordinary users cannot self-register as Hospital Administrators without valid institutional authorization."
            );
            return;
        }

        setFormEnabled(false);
        UiStateHelper.showLoading(binding.stateView, "Verifying Hospital Authority authorization…");

        authRepository.setupAuthorizedAdmin(
                fullName, email, phone, employeeId, designation, authToken, password,
                new AuthCallback<UserAccount>() {
                    @Override
                    public void onSuccess(UserAccount result) {
                        setFormEnabled(true);
                        UiStateHelper.hideLoading(binding.stateView);
                        AuthNavigationRouter.routeAuthenticatedUser(AdminSetupActivity.this, result);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        setFormEnabled(true);
                        UiStateHelper.showError(binding.stateView, "Admin Authorization Denied", errorMessage);
                    }
                }
        );
    }

    private void clearFieldErrors() {
        binding.tilFullName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPhone.setError(null);
        binding.tilEmployeeId.setError(null);
        binding.tilDesignation.setError(null);
        binding.tilAuthToken.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);
    }

    private void setFormEnabled(boolean enabled) {
        binding.btnSetupAdmin.setEnabled(enabled);
        binding.btnGoToLogin.setEnabled(enabled);
    }

    private String getText(com.google.android.material.textfield.TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
