package com.arohi.healthcare.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.arohi.healthcare.activities.RolePortalActivity;
import com.arohi.healthcare.databinding.FragmentNurseProfileBinding;
import com.arohi.healthcare.models.NurseProfile;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.ProfileRepository;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;
import com.arohi.healthcare.utils.ValidationUtils;

/**
 * Nurse Professional Profile Fragment allowing nurses to manage their nursing
 * credentials, qualifications, preferred ward, and duty shift preferences,
 * subject to Hospital Authority verification.
 */
public class NurseProfileFragment extends Fragment {

    private FragmentNurseProfileBinding binding;
    private ProfileRepository profileRepository;
    private SessionManager sessionManager;
    private NurseProfile currentProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNurseProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        profileRepository = new ProfileRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        UserAccount account = sessionManager.getCurrentSessionAccount();
        if (account != null) {
            binding.tvNurseProfileHeaderName.setText(account.getFullName());
            VerificationStatus status = account.getVerificationStatusEnum();
            binding.tvNurseProfileHeaderMeta.setText(
                    "AROHI ID: " + account.getArohiId() + " • Status: " + status.getKey());

            if (status == VerificationStatus.VERIFIED) {
                binding.etNurseLicense.setEnabled(false);
                binding.tilNurseLicense.setHelperText("Verified by Hospital Authority (Locked)");
                binding.tvNurseProfileVerificationNote.setText(
                        "Your BNMC Nursing License is VERIFIED. Ward and task assignments are unlocked.");
            } else {
                binding.etNurseLicense.setEnabled(true);
                binding.tilNurseLicense.setHelperText("Editable while awaiting Hospital Authority verification");
                binding.tvNurseProfileVerificationNote.setText(
                        "Status: " + status.getDisplayLabel() + ". Ensure your BNMC License and nursing details are accurate for Admin review.");
            }

            loadProfile(account.getUid());
        }

        binding.btnSaveNurseProfile.setOnClickListener(v -> saveProfile());
        binding.btnNurseLogout.setOnClickListener(v -> {
            if (getActivity() instanceof RolePortalActivity) {
                ((RolePortalActivity) getActivity()).performLogout();
            }
        });
    }

    private void loadProfile(String uid) {
        binding.progressNurseProfile.setVisibility(View.VISIBLE);
        profileRepository.loadNurseProfile(uid, new AuthCallback<NurseProfile>() {
            @Override
            public void onSuccess(NurseProfile profile) {
                if (binding == null) return;
                binding.progressNurseProfile.setVisibility(View.GONE);
                currentProfile = profile;

                binding.etNurseFullName.setText(profile.getFullName());
                binding.etNursePhone.setText(profile.getPhone());
                binding.etNurseLicense.setText(profile.getNursingLicenseNumber());
                binding.etNurseQualification.setText(profile.getQualification());
                binding.etNursePreferredWard.setText(profile.getPreferredWard());
                binding.etNurseAssignedWard.setText(profile.getAssignedWardId());
                binding.etNurseShiftPreference.setText(profile.getShiftPreference());
                binding.etNurseAddress.setText(profile.getAddress());
                binding.etNurseBioNotes.setText(profile.getBioNotes());
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.progressNurseProfile.setVisibility(View.GONE);
                UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    private void saveProfile() {
        if (currentProfile == null) return;

        String fullName = textOf(binding.etNurseFullName);
        String phone = textOf(binding.etNursePhone);
        String license = textOf(binding.etNurseLicense);
        String qualification = textOf(binding.etNurseQualification);
        String preferredWard = textOf(binding.etNursePreferredWard);
        String shiftPreference = textOf(binding.etNurseShiftPreference);
        String address = textOf(binding.etNurseAddress);
        String bio = textOf(binding.etNurseBioNotes);

        binding.tilNurseFullName.setError(null);
        binding.tilNursePhone.setError(null);
        binding.tilNurseLicense.setError(null);

        if (!ValidationUtils.isValidFullName(fullName)) {
            binding.tilNurseFullName.setError("Please enter your full name.");
            return;
        }
        if (!ValidationUtils.isValidPhone(phone)) {
            binding.tilNursePhone.setError("Please enter a valid mobile number.");
            return;
        }
        if (!ValidationUtils.isValidLicenseNumber(license)) {
            binding.tilNurseLicense.setError("Valid BNMC license number is required.");
            return;
        }

        currentProfile.setFullName(fullName);
        currentProfile.setPhone(phone);
        currentProfile.setNursingLicenseNumber(license);
        currentProfile.setQualification(qualification);
        currentProfile.setPreferredWard(preferredWard);
        currentProfile.setShiftPreference(shiftPreference);
        currentProfile.setAddress(address);
        currentProfile.setBioNotes(bio);
        currentProfile.setUpdatedAt(System.currentTimeMillis());

        binding.progressNurseProfile.setVisibility(View.VISIBLE);
        binding.btnSaveNurseProfile.setEnabled(false);

        profileRepository.saveNurseProfile(currentProfile, new AuthCallback<NurseProfile>() {
            @Override
            public void onSuccess(NurseProfile saved) {
                if (binding == null) return;
                binding.progressNurseProfile.setVisibility(View.GONE);
                binding.btnSaveNurseProfile.setEnabled(true);
                binding.tvNurseProfileHeaderName.setText(saved.getFullName());
                if (getActivity() instanceof RolePortalActivity) {
                    UserAccount updatedAccount = sessionManager.getCurrentSessionAccount();
                    if (updatedAccount != null) {
                        ((RolePortalActivity) getActivity()).setupTopHeader(updatedAccount);
                    }
                }
                UiStateHelper.showSuccess(binding.getRoot(), "Nurse Professional Profile saved to Cloud Firestore!");
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.progressNurseProfile.setVisibility(View.GONE);
                binding.btnSaveNurseProfile.setEnabled(true);
                UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    private String textOf(com.google.android.material.textfield.TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
