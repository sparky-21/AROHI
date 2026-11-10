package com.arohi.healthcare.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.arohi.healthcare.databinding.ActivityLoginBinding;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.navigation.AuthNavigationRouter;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.AuthRepository;
import com.arohi.healthcare.utils.UiStateHelper;
import com.arohi.healthcare.utils.ValidationUtils;

/**
 * 8. Login Screen
 * Authenticates users, resolves their authoritative role (PATIENT, DOCTOR, NURSE, ADMIN)
 * and verificationStatus, and navigates appropriately.
 */
public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(this);
        binding.toolbar.tvToolbarBadge.setText("SECURE SIGN IN");
        binding.toolbar.btnBack.setOnClickListener(v -> finish());

        if (!authRepository.isFirebaseConnected()) {
            UiStateHelper.showWarning(
                    binding.stateView,
                    "Local Sandbox Mode Active",
                    "google-services.json is not yet placed in app/. Sign in with an account registered in this local session or add google-services.json for live Firebase Auth."
            );
        }

        binding.btnSignIn.setOnClickListener(v -> validateAndLogin());
        binding.btnForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class))
        );
        binding.btnGoToRoleSelection.setOnClickListener(v -> {
            startActivity(new Intent(this, RoleSelectionActivity.class));
            finish();
        });
    }

    private void validateAndLogin() {
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString() : "";

        boolean valid = true;
        if (!ValidationUtils.isValidEmail(email)) {
            binding.tilEmail.setError("Enter a valid registered email address.");
            valid = false;
        }
        if (password.isEmpty()) {
            binding.tilPassword.setError("Enter your password.");
            valid = false;
        }

        if (!valid) {
            UiStateHelper.showError(binding.stateView, "Invalid Credentials", "Please enter both a valid email address and password.");
            return;
        }

        binding.btnSignIn.setEnabled(false);
        UiStateHelper.showLoading(binding.stateView, "Verifying identity and role permissions…");

        authRepository.login(email, password, new AuthCallback<UserAccount>() {
            @Override
            public void onSuccess(UserAccount result) {
                binding.btnSignIn.setEnabled(true);
                UiStateHelper.hideLoading(binding.stateView);
                AuthNavigationRouter.routeAuthenticatedUser(LoginActivity.this, result);
            }

            @Override
            public void onError(String errorMessage) {
                binding.btnSignIn.setEnabled(true);
                UiStateHelper.showError(binding.stateView, "Sign-In Failed", errorMessage);
            }
        });
    }
}
