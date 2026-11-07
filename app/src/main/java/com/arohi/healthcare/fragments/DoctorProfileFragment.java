package com.arohi.healthcare.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.arohi.healthcare.activities.RolePortalActivity;
import com.arohi.healthcare.databinding.FragmentDoctorProfileBinding;
import com.arohi.healthcare.models.DoctorProfile;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.ProfileRepository;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;
import com.arohi.healthcare.utils.ValidationUtils;

/**
 * Doctor Professional Profile Fragment allowing doctors to manage their professional
 * credentials, specialty, qualifications, department, experience, consultation fee,
 * and availability schedule, subject to Hospital Authority verification.
 */
public class DoctorProfileFragment extends Fragment {

    private FragmentDoctorProfileBinding binding;
    private ProfileRepository profileRepository;
    private SessionManager sessionManager;
    private DoctorProfile currentProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDoctorProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        profileRepository = new ProfileRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        UserAccount account = sessionManager.getCurrentSessionAccount();
        if (account != null) {
            binding.tvDoctorProfileHeaderName.setText(account.getFullName());
            VerificationStatus status = account.getVerificationStatusEnum();
            binding.tvDoctorProfileHeaderMeta.setText(
                    "AROHI ID: " + account.getArohiId() + " • Status: " + status.getKey());

            if (status == VerificationStatus.VERIFIED) {
                binding.etDoctorLicense.setEnabled(false);
                binding.tilDoctorLicense.setHelperText("Verified by Hospital Authority (Locked)");
                binding.tvDoctorProfileVerificationNote.setText(
                        "Your BMDC credentials are VERIFIED. Clinical practice features are unlocked.");
            } else {
                binding.etDoctorLicense.setEnabled(true);
                binding.tilDoctorLicense.setHelperText("Editable while awaiting Hospital Authority verification");
                binding.tvDoctorProfileVerificationNote.setText(
                        "Status: " + status.getDisplayLabel() + ". Ensure your BMDC License and specialty details are accurate for Admin review.");
            }

            loadProfile(account.getUid());
        }

        binding.btnSaveDoctorProfile.setOnClickListener(v -> saveProfile());
        binding.btnDoctorLogout.setOnClickListener(v -> {
            if (getActivity() instanceof RolePortalActivity) {
                ((RolePortalActivity) getActivity()).performLogout();
            }
        });
    }

    private void loadProfile(String uid) {
        binding.progressDoctorProfile.setVisibility(View.VISIBLE);
        profileRepository.loadDoctorProfile(uid, new AuthCallback<DoctorProfile>() {
            @Override
            public void onSuccess(DoctorProfile profile) {
                if (binding == null) return;
                binding.progressDoctorProfile.setVisibility(View.GONE);
                currentProfile = profile;

                binding.etDoctorFullName.setText(profile.getFullName());
                binding.etDoctorPhone.setText(profile.getPhone());
                binding.etDoctorLicense.setText(profile.getMedicalLicenseNumber());
                binding.etDoctorSpecialty.setText(profile.getSpecialty());
                binding.etDoctorQualifications.setText(profile.getQualifications());
                binding.etDoctorDepartment.setText(profile.getDepartment());
                binding.etDoctorExperienceYears.setText(String.valueOf(profile.getExperienceYears()));
                binding.etDoctorConsultationFee.setText(String.format("%.0f", profile.getConsultationFeeBdt()));
                binding.etDoctorAvailability.setText(profile.getAvailabilitySchedule());
                binding.etDoctorAddress.setText(profile.getAddress());
                binding.etDoctorBioNotes.setText(profile.getBioNotes());
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.progressDoctorProfile.setVisibility(View.GONE);
                UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    private void saveProfile() {
        if (currentProfile == null) return;

        String fullName = textOf(binding.etDoctorFullName);
        String phone = textOf(binding.etDoctorPhone);
        String license = textOf(binding.etDoctorLicense);
        String specialty = textOf(binding.etDoctorSpecialty);
        String qualifications = textOf(binding.etDoctorQualifications);
        String department = textOf(binding.etDoctorDepartment);
        String expStr = textOf(binding.etDoctorExperienceYears);
        String feeStr = textOf(binding.etDoctorConsultationFee);
        String availability = textOf(binding.etDoctorAvailability);
        String address = textOf(binding.etDoctorAddress);
        String bio = textOf(binding.etDoctorBioNotes);

        binding.tilDoctorFullName.setError(null);
        binding.tilDoctorPhone.setError(null);
        binding.tilDoctorLicense.setError(null);

        if (!ValidationUtils.isValidFullName(fullName)) {
            binding.tilDoctorFullName.setError("Please enter your full name.");
            return;
        }
        if (!ValidationUtils.isValidPhone(phone)) {
            binding.tilDoctorPhone.setError("Please enter a valid mobile number.");
            return;
        }
        if (!ValidationUtils.isValidLicenseNumber(license)) {
            binding.tilDoctorLicense.setError("Valid BMDC license number is required.");
            return;
        }

        int experienceYears = 0;
        try {
            experienceYears = Integer.parseInt(expStr);
        } catch (NumberFormatException ignored) {
        }

        double feeBdt = 0.0;
        try {
            feeBdt = Double.parseDouble(feeStr);
        } catch (NumberFormatException ignored) {
        }

        currentProfile.setFullName(fullName);
        currentProfile.setPhone(phone);
        currentProfile.setMedicalLicenseNumber(license);
        currentProfile.setSpecialty(specialty);
        currentProfile.setQualifications(qualifications);
        currentProfile.setDepartment(department);
        currentProfile.setExperienceYears(experienceYears);
        currentProfile.setConsultationFeeBdt(feeBdt);
        currentProfile.setAvailabilitySchedule(availability);
        currentProfile.setAddress(address);
        currentProfile.setBioNotes(bio);
        currentProfile.setUpdatedAt(System.currentTimeMillis());

        binding.progressDoctorProfile.setVisibility(View.VISIBLE);
        binding.btnSaveDoctorProfile.setEnabled(false);

        profileRepository.saveDoctorProfile(currentProfile, new AuthCallback<DoctorProfile>() {
            @Override
            public void onSuccess(DoctorProfile saved) {
                if (binding == null) return;
                binding.progressDoctorProfile.setVisibility(View.GONE);
                binding.btnSaveDoctorProfile.setEnabled(true);
                binding.tvDoctorProfileHeaderName.setText(saved.getFullName());
                if (getActivity() instanceof RolePortalActivity) {
                    UserAccount updatedAccount = sessionManager.getCurrentSessionAccount();
                    if (updatedAccount != null) {
                        ((RolePortalActivity) getActivity()).setupTopHeader(updatedAccount);
                    }
                }
                UiStateHelper.showSuccess(binding.getRoot(), "Doctor Professional Profile saved to Cloud Firestore!");
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.progressDoctorProfile.setVisibility(View.GONE);
                binding.btnSaveDoctorProfile.setEnabled(true);
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
