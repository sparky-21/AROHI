package com.arohi.healthcare.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.arohi.healthcare.activities.RolePortalActivity;
import com.arohi.healthcare.databinding.FragmentPatientProfileBinding;
import com.arohi.healthcare.models.PatientProfile;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.ProfileRepository;
import com.arohi.healthcare.utils.DatePickerHelper;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;
import com.arohi.healthcare.utils.ValidationUtils;

/**
 * Patient Profile Management Fragment allowing patients to manage their personal
 * details, emergency contact, blood group, allergies, chronic conditions, and address
 * synchronized with Cloud Firestore (/patients/{uid} and /users/{uid}).
 */
public class PatientProfileFragment extends Fragment {

    private FragmentPatientProfileBinding binding;
    private ProfileRepository profileRepository;
    private SessionManager sessionManager;
    private PatientProfile currentProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPatientProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        profileRepository = new ProfileRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        DatePickerHelper.attachToField(requireContext(), binding.tilPatientDob, binding.etPatientDob);

        UserAccount account = sessionManager.getCurrentSessionAccount();
        if (account != null) {
            binding.tvPatientProfileHeaderName.setText(account.getFullName());
            binding.tvPatientProfileHeaderMeta.setText(
                    "AROHI ID: " + account.getArohiId()
                            + " • Status: " + account.getVerificationStatusEnum().getKey());
            binding.tvPatientProfileHeaderEmail.setText(account.getEmail());

            loadProfile(account.getUid());
        }

        binding.btnSavePatientProfile.setOnClickListener(v -> saveProfile());
        binding.btnPatientOpenSettings.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(com.arohi.healthcare.R.id.fragmentContainer, new SettingsFragment())
                        .addToBackStack("settings")
                        .commit();
            }
        });
        binding.btnPatientLogout.setOnClickListener(v -> {
            if (getActivity() instanceof RolePortalActivity) {
                ((RolePortalActivity) getActivity()).performLogout();
            }
        });
    }

    private void loadProfile(String uid) {
        binding.progressPatientProfile.setVisibility(View.VISIBLE);
        profileRepository.loadPatientProfile(uid, new AuthCallback<PatientProfile>() {
            @Override
            public void onSuccess(PatientProfile profile) {
                if (binding == null) return;
                binding.progressPatientProfile.setVisibility(View.GONE);
                currentProfile = profile;

                binding.etPatientFullName.setText(profile.getFullName());
                binding.etPatientPhone.setText(profile.getPhone());
                binding.etPatientDob.setText(profile.getDateOfBirth());
                binding.etPatientGender.setText(profile.getGender());
                binding.etPatientBloodGroup.setText(profile.getBloodGroup());
                binding.etPatientAllergies.setText(profile.getKnownAllergies());
                binding.etPatientChronicConditions.setText(profile.getChronicConditions());
                binding.etPatientEmergencyName.setText(profile.getEmergencyContactName());
                binding.etPatientEmergencyPhone.setText(profile.getEmergencyContactPhone());
                binding.etPatientAddress.setText(profile.getAddress());
                binding.etPatientBioNotes.setText(profile.getBioNotes());
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.progressPatientProfile.setVisibility(View.GONE);
                UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    private void saveProfile() {
        if (currentProfile == null) return;

        String fullName = textOf(binding.etPatientFullName);
        String phone = textOf(binding.etPatientPhone);
        String dob = textOf(binding.etPatientDob);
        String gender = textOf(binding.etPatientGender);
        String blood = textOf(binding.etPatientBloodGroup);
        String allergies = textOf(binding.etPatientAllergies);
        String chronic = textOf(binding.etPatientChronicConditions);
        String emName = textOf(binding.etPatientEmergencyName);
        String emPhone = textOf(binding.etPatientEmergencyPhone);
        String address = textOf(binding.etPatientAddress);
        String notes = textOf(binding.etPatientBioNotes);

        binding.tilPatientFullName.setError(null);
        binding.tilPatientPhone.setError(null);

        if (!ValidationUtils.isValidFullName(fullName)) {
            binding.tilPatientFullName.setError("Please enter your full name.");
            return;
        }
        if (!ValidationUtils.isValidPhone(phone)) {
            binding.tilPatientPhone.setError("Please enter a valid mobile number.");
            return;
        }

        currentProfile.setFullName(fullName);
        currentProfile.setPhone(phone);
        currentProfile.setDateOfBirth(dob);
        currentProfile.setGender(gender);
        currentProfile.setBloodGroup(blood);
        currentProfile.setKnownAllergies(allergies.isEmpty() ? "None" : allergies);
        currentProfile.setChronicConditions(chronic.isEmpty() ? "None" : chronic);
        currentProfile.setEmergencyContactName(emName);
        currentProfile.setEmergencyContactPhone(emPhone);
        currentProfile.setAddress(address);
        currentProfile.setBioNotes(notes);
        currentProfile.setUpdatedAt(System.currentTimeMillis());

        binding.progressPatientProfile.setVisibility(View.VISIBLE);
        binding.btnSavePatientProfile.setEnabled(false);

        profileRepository.savePatientProfile(currentProfile, new AuthCallback<PatientProfile>() {
            @Override
            public void onSuccess(PatientProfile saved) {
                if (binding == null) return;
                binding.progressPatientProfile.setVisibility(View.GONE);
                binding.btnSavePatientProfile.setEnabled(true);
                binding.tvPatientProfileHeaderName.setText(saved.getFullName());
                if (getActivity() instanceof RolePortalActivity) {
                    UserAccount updatedAccount = sessionManager.getCurrentSessionAccount();
                    if (updatedAccount != null) {
                        ((RolePortalActivity) getActivity()).setupTopHeader(updatedAccount);
                    }
                }
                UiStateHelper.showSuccess(binding.getRoot(), "Patient Health Profile saved to Cloud Firestore!");
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.progressPatientProfile.setVisibility(View.GONE);
                binding.btnSavePatientProfile.setEnabled(true);
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
