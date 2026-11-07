package com.arohi.healthcare.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ActivityBasicProfileSetupBinding;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.AuthRepository;
import com.arohi.healthcare.utils.UiStateHelper;
import com.arohi.healthcare.utils.ValidationUtils;

/**
 * 11. Basic Profile Setup & Role Verification Gate
 * Displays the user's unique AROHI ID, authenticated role, and verification status,
 * enforces unverified professional restrictions for Doctor/Nurse/Admin accounts,
 * and allows completing core profile details and testing session persistence / logout.
 */
public class BasicProfileSetupActivity extends AppCompatActivity {

    private ActivityBasicProfileSetupBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBasicProfileSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(this);
        populateUserIdentity();

        binding.btnSaveProfile.setOnClickListener(v -> validateAndSaveProfile());
        binding.btnLogout.setOnClickListener(v -> performLogout());
    }

    private void populateUserIdentity() {
        UserAccount account = authRepository.getSessionManager().getCurrentSessionAccount();
        if (account == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        UserRole role = account.getUserRoleEnum();
        VerificationStatus status = account.getVerificationStatusEnum();

        binding.tvRoleBadge.setText(role.getKey());
        binding.tvIdentityName.setText(account.getFullName());
        binding.tvIdentityUniqueId.setText("ID: " + account.getArohiId());
        binding.tvIdentityEmail.setText(account.getEmail());
        binding.tvIdentityRoleSummary.setText(
                "Role: " + role.getDisplayName() + " • Status: " + status.getKey()
        );

        binding.etFullName.setText(account.getFullName());
        binding.etPhone.setText(account.getPhone());
        binding.etAddress.setText(account.getAddress());
        binding.etBioNotes.setText(account.getBioNotes());

        // Enforce unverified (PENDING), REJECTED/SUSPENDED, vs VERIFIED status
        if (status == VerificationStatus.VERIFIED) {
            binding.bannerRoleVerification.setBackgroundResource(R.drawable.bg_status_verified);
            binding.ivVerificationIcon.setImageResource(R.drawable.ic_verified_badge);
            binding.tvVerificationStatusTitle.setText(R.string.status_banner_verified_title);
            binding.tvVerificationStatusDesc.setText(R.string.status_banner_verified_desc);
        } else if (status == VerificationStatus.REJECTED || status == VerificationStatus.SUSPENDED) {
            binding.bannerRoleVerification.setBackgroundResource(R.drawable.bg_status_error);
            binding.ivVerificationIcon.setImageResource(R.drawable.ic_warning_alert);
            binding.tvVerificationStatusTitle.setText("Account " + status.getKey());
            binding.tvVerificationStatusDesc.setText(status.getDescription() + ". Please contact Hospital Authority.");
        } else {
            binding.bannerRoleVerification.setBackgroundResource(R.drawable.bg_status_pending);
            binding.ivVerificationIcon.setImageResource(R.drawable.ic_pending_clock);
            binding.tvVerificationStatusTitle.setText(R.string.status_banner_pending_title);
            binding.tvVerificationStatusDesc.setText(R.string.status_banner_pending_desc);
        }
    }

    private void validateAndSaveProfile() {
        binding.tilFullName.setError(null);
        binding.tilPhone.setError(null);
        binding.tilAddress.setError(null);

        String fullName = binding.etFullName.getText() != null ? binding.etFullName.getText().toString().trim() : "";
        String phone = binding.etPhone.getText() != null ? binding.etPhone.getText().toString().trim() : "";
        String address = binding.etAddress.getText() != null ? binding.etAddress.getText().toString().trim() : "";
        String bioNotes = binding.etBioNotes.getText() != null ? binding.etBioNotes.getText().toString().trim() : "";

        boolean valid = true;
        if (!ValidationUtils.isValidFullName(fullName)) {
            binding.tilFullName.setError("Enter your full legal name.");
            valid = false;
        }
        if (!ValidationUtils.isValidPhone(phone)) {
            binding.tilPhone.setError("Enter a valid 10-15 digit phone number.");
            valid = false;
        }
        if (address.length() < 4) {
            binding.tilAddress.setError("Please enter your residential or chamber address.");
            valid = false;
        }

        if (!valid) {
            UiStateHelper.showError(binding.stateView, "Validation Error", "Please complete your basic profile fields.");
            return;
        }

        binding.btnSaveProfile.setEnabled(false);
        UiStateHelper.showLoading(binding.stateView, "Saving basic profile to AROHI identity record…");

        authRepository.updateBasicProfile(fullName, phone, address, bioNotes, new AuthCallback<UserAccount>() {
            @Override
            public void onSuccess(UserAccount result) {
                binding.btnSaveProfile.setEnabled(true);
                populateUserIdentity();
                UiStateHelper.showSuccess(
                        binding.stateView,
                        "Profile Saved Successfully",
                        "Your AROHI basic profile and session persistence are updated."
                );
                startActivity(new Intent(BasicProfileSetupActivity.this, RolePortalActivity.class));
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                binding.btnSaveProfile.setEnabled(true);
                UiStateHelper.showError(binding.stateView, "Profile Update Failed", errorMessage);
            }
        });
    }

    private void performLogout() {
        authRepository.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
