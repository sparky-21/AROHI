package com.arohi.healthcare.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.databinding.DialogEmergencyHealthCardBinding;
import com.arohi.healthcare.models.EmergencyCardProfile;
import com.arohi.healthcare.models.PatientProfile;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.ProfileRepository;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Quick-Access Patient-Controlled Emergency Health Card Dialog.
 * Provides high-contrast immediate emergency data access, direct emergency phone dialing,
 * and patient-controlled visibility toggles for medical privacy.
 */
public class EmergencyCardDialogFragment extends DialogFragment {

    private DialogEmergencyHealthCardBinding binding;
    private SessionManager sessionManager;
    private ProfileRepository profileRepository;
    private FirebaseFirestore firestore;

    private UserAccount currentAccount;
    private EmergencyCardProfile emergencyProfile;

    public static EmergencyCardDialogFragment newInstance() {
        return new EmergencyCardDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogEmergencyHealthCardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        profileRepository = new ProfileRepository(requireContext());
        firestore = FirebaseFirestore.getInstance();
        currentAccount = sessionManager.getCurrentSessionAccount();

        setupListeners();
        loadEmergencyProfile();
    }

    private void setupListeners() {
        binding.btnCloseEmergencyCard.setOnClickListener(v -> dismiss());

        // Direct Call Emergency Contact
        binding.btnCallEmergencyContact.setOnClickListener(v -> {
            String phone = emergencyProfile != null && emergencyProfile.getEmergencyContactPhone() != null
                    ? emergencyProfile.getEmergencyContactPhone() : "";
            if (!phone.isEmpty() && !phone.contains("Not Set")) {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                startActivity(dialIntent);
            } else {
                UiStateHelper.showWarning(binding.getRoot(), "No emergency contact phone number configured.");
            }
        });

        // Direct Call Hospital Helpline / Ambulance
        binding.btnCallHospitalAmbulance.setOnClickListener(v -> {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:1066"));
            startActivity(dialIntent);
        });

        // Save Patient-Controlled Privacy Settings
        binding.btnSaveEmergencySettings.setOnClickListener(v -> savePrivacySettings());
    }

    private void loadEmergencyProfile() {
        if (currentAccount == null) return;
        String uid = currentAccount.getUid();

        // 1. Check local emergency card cache
        emergencyProfile = sessionManager.getEmergencyCardLocal(uid);

        // 2. If not yet created, build from Patient Profile
        if (emergencyProfile == null) {
            profileRepository.loadPatientProfile(uid, new AuthCallback<PatientProfile>() {
                @Override
                public void onSuccess(PatientProfile p) {
                    emergencyProfile = new EmergencyCardProfile(
                            uid,
                            currentAccount.getArohiId(),
                            p.getFullName(),
                            p.getDateOfBirth(),
                            p.getGender(),
                            p.getBloodGroup(),
                            p.getPhone(),
                            p.getKnownAllergies(),
                            p.getChronicConditions(),
                            "None recorded",
                            p.getEmergencyContactName(),
                            p.getEmergencyContactPhone(),
                            "Family"
                    );
                    sessionManager.saveEmergencyCardLocal(emergencyProfile);
                    bindProfileToUi();
                }

                @Override
                public void onError(String message) {
                    emergencyProfile = new EmergencyCardProfile(
                            uid, currentAccount.getArohiId(), currentAccount.getFullName(),
                            "", "", "O+", currentAccount.getPhone(),
                            "None", "None", "None", "Emergency Contact", "+8801700000000", "Family"
                    );
                    bindProfileToUi();
                }
            });
        } else {
            bindProfileToUi();
        }
    }

    private void bindProfileToUi() {
        if (binding == null || emergencyProfile == null) return;

        binding.tvEmergencyPatientName.setText("Patient: " + emergencyProfile.getFullName());
        binding.tvEmergencyArohiId.setText("AROHI ID: " + emergencyProfile.getArohiId()
                + " • DOB: " + (emergencyProfile.getDateOfBirth() != null ? emergencyProfile.getDateOfBirth() : "Not Set"));

        // Blood Group
        binding.switchBloodGroup.setChecked(emergencyProfile.isShowBloodGroup());
        binding.tvCardBloodGroup.setText(emergencyProfile.isShowBloodGroup()
                ? emergencyProfile.getBloodGroup() : "[Hidden by Patient]");

        // Allergies
        binding.switchAllergies.setChecked(emergencyProfile.isShowAllergies());
        binding.tvCardAllergies.setText(emergencyProfile.isShowAllergies()
                ? (emergencyProfile.getKnownAllergies() != null && !emergencyProfile.getKnownAllergies().isEmpty()
                ? emergencyProfile.getKnownAllergies() : "None Recorded")
                : "[Hidden by Patient]");

        // Conditions
        binding.switchConditions.setChecked(emergencyProfile.isShowChronicConditions());
        binding.tvCardConditions.setText(emergencyProfile.isShowChronicConditions()
                ? (emergencyProfile.getChronicConditions() != null && !emergencyProfile.getChronicConditions().isEmpty()
                ? emergencyProfile.getChronicConditions() : "None Recorded")
                : "[Hidden by Patient]");

        // Medications
        binding.switchMedications.setChecked(emergencyProfile.isShowCurrentMedications());
        binding.tvCardMedications.setText(emergencyProfile.isShowCurrentMedications()
                ? (emergencyProfile.getCurrentMedications() != null && !emergencyProfile.getCurrentMedications().isEmpty()
                ? emergencyProfile.getCurrentMedications() : "None Recorded")
                : "[Hidden by Patient]");

        // Emergency Contact
        binding.switchContact.setChecked(emergencyProfile.isShowEmergencyContact());
        binding.tvCardEmergencyContact.setText(emergencyProfile.isShowEmergencyContact()
                ? (emergencyProfile.getEmergencyContactName() + " (" + emergencyProfile.getEmergencyContactRelation() + ") • " + emergencyProfile.getEmergencyContactPhone())
                : "[Hidden by Patient]");
    }

    private void savePrivacySettings() {
        if (emergencyProfile == null || currentAccount == null) return;

        emergencyProfile.setShowBloodGroup(binding.switchBloodGroup.isChecked());
        emergencyProfile.setShowAllergies(binding.switchAllergies.isChecked());
        emergencyProfile.setShowChronicConditions(binding.switchConditions.isChecked());
        emergencyProfile.setShowCurrentMedications(binding.switchMedications.isChecked());
        emergencyProfile.setShowEmergencyContact(binding.switchContact.isChecked());
        emergencyProfile.setLastUpdatedAt(System.currentTimeMillis());

        // Save local
        sessionManager.saveEmergencyCardLocal(emergencyProfile);

        // Sync Firestore
        firestore.collection(FirestoreCollections.EMERGENCY_PROFILES)
                .document(currentAccount.getUid())
                .set(emergencyProfile.toMap());

        bindProfileToUi();
        UiStateHelper.showSuccess(binding.getRoot(), "Emergency Card privacy preferences saved successfully.");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
