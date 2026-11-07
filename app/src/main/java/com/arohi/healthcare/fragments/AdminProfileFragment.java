package com.arohi.healthcare.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.arohi.healthcare.activities.RolePortalActivity;
import com.arohi.healthcare.databinding.FragmentAdminProfileBinding;
import com.arohi.healthcare.models.AdminProfile;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.ProfileRepository;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;
import com.arohi.healthcare.utils.ValidationUtils;

/**
 * Hospital Authority Profile & Configuration Fragment allowing Administrators
 * to manage authorized hospital information, emergency hotline, campus address,
 * and administrative identity synchronized with Cloud Firestore.
 */
public class AdminProfileFragment extends Fragment {

    private FragmentAdminProfileBinding binding;
    private ProfileRepository profileRepository;
    private SessionManager sessionManager;
    private AdminProfile currentProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        profileRepository = new ProfileRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        UserAccount account = sessionManager.getCurrentSessionAccount();
        if (account != null) {
            binding.tvAdminProfileHeaderName.setText(account.getFullName());
            binding.tvAdminProfileHeaderMeta.setText(
                    "AROHI ID: " + account.getArohiId() + " • Status: VERIFIED AUTHORITY");
            binding.tvAdminProfileHeaderEmail.setText(account.getEmail());

            loadProfile(account.getUid());
        }

        binding.btnSaveAdminProfile.setOnClickListener(v -> saveProfile());
        binding.btnAdminLogout.setOnClickListener(v -> {
            if (getActivity() instanceof RolePortalActivity) {
                ((RolePortalActivity) getActivity()).performLogout();
            }
        });
    }

    private void loadProfile(String uid) {
        binding.progressAdminProfile.setVisibility(View.VISIBLE);
        profileRepository.loadAdminProfile(uid, new AuthCallback<AdminProfile>() {
            @Override
            public void onSuccess(AdminProfile profile) {
                if (binding == null) return;
                binding.progressAdminProfile.setVisibility(View.GONE);
                currentProfile = profile;

                binding.etAdminFullName.setText(profile.getFullName());
                binding.etAdminPhone.setText(profile.getPhone());
                binding.etAdminEmployeeId.setText(profile.getEmployeeId());
                binding.etAdminDesignation.setText(profile.getDesignation());
                binding.etAdminHospitalName.setText(profile.getHospitalName());
                binding.etAdminEmergencyHotline.setText(profile.getEmergencyHotline());
                binding.etAdminHospitalAddress.setText(profile.getHospitalAddress());
                binding.etAdminBioNotes.setText(profile.getBioNotes());
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.progressAdminProfile.setVisibility(View.GONE);
                UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    private void saveProfile() {
        if (currentProfile == null) return;

        String fullName = textOf(binding.etAdminFullName);
        String phone = textOf(binding.etAdminPhone);
        String empId = textOf(binding.etAdminEmployeeId);
        String designation = textOf(binding.etAdminDesignation);
        String hospitalName = textOf(binding.etAdminHospitalName);
        String hotline = textOf(binding.etAdminEmergencyHotline);
        String address = textOf(binding.etAdminHospitalAddress);
        String notes = textOf(binding.etAdminBioNotes);

        binding.tilAdminFullName.setError(null);
        binding.tilAdminPhone.setError(null);

        if (!ValidationUtils.isValidFullName(fullName)) {
            binding.tilAdminFullName.setError("Please enter administrator full name.");
            return;
        }
        if (!ValidationUtils.isValidPhone(phone)) {
            binding.tilAdminPhone.setError("Please enter a valid mobile number.");
            return;
        }

        currentProfile.setFullName(fullName);
        currentProfile.setPhone(phone);
        currentProfile.setEmployeeId(empId);
        currentProfile.setDesignation(designation);
        currentProfile.setHospitalName(hospitalName.isEmpty() ? "AROHI Central Hospital" : hospitalName);
        currentProfile.setEmergencyHotline(hotline.isEmpty() ? "10666" : hotline);
        currentProfile.setHospitalAddress(address);
        currentProfile.setBioNotes(notes);
        currentProfile.setUpdatedAt(System.currentTimeMillis());

        binding.progressAdminProfile.setVisibility(View.VISIBLE);
        binding.btnSaveAdminProfile.setEnabled(false);

        profileRepository.saveAdminProfile(currentProfile, new AuthCallback<AdminProfile>() {
            @Override
            public void onSuccess(AdminProfile saved) {
                if (binding == null) return;
                binding.progressAdminProfile.setVisibility(View.GONE);
                binding.btnSaveAdminProfile.setEnabled(true);
                binding.tvAdminProfileHeaderName.setText(saved.getFullName());
                if (getActivity() instanceof RolePortalActivity) {
                    UserAccount updatedAccount = sessionManager.getCurrentSessionAccount();
                    if (updatedAccount != null) {
                        ((RolePortalActivity) getActivity()).setupTopHeader(updatedAccount);
                    }
                }
                UiStateHelper.showSuccess(binding.getRoot(), "Hospital & Authority Settings saved to Cloud Firestore!");
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.progressAdminProfile.setVisibility(View.GONE);
                binding.btnSaveAdminProfile.setEnabled(true);
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
