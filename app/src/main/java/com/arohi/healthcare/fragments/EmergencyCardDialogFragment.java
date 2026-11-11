package com.arohi.healthcare.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.arohi.healthcare.activities.LiveTrackingMapActivity;
import com.arohi.healthcare.activities.UnifiedTrackingActivity;
import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.databinding.DialogEmergencyHealthCardBinding;
import com.arohi.healthcare.models.AmbulanceRequest;
import com.arohi.healthcare.models.EmergencyCardProfile;
import com.arohi.healthcare.models.PatientProfile;
import com.arohi.healthcare.models.TrackingCategory;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.repositories.AmbulanceTrackingRepository;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.ProfileRepository;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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

        // Phase 14 Request Emergency Ambulance & Live Telemetry Tracking
        binding.btnRequestAmbulanceDispatch.setOnClickListener(v -> showAmbulanceRequestDialog());
        binding.btnTrackActiveAmbulance.setOnClickListener(v -> {
            String hospId = sessionManager.getSelectedHospitalId();
            String uid = currentAccount != null ? currentAccount.getUid() : "";
            AmbulanceTrackingRepository ambRepo = new AmbulanceTrackingRepository(requireContext());
            AmbulanceRequest active = ambRepo.getLatestActiveForPatient(hospId, uid);
            if (active != null) {
                startActivity(LiveTrackingMapActivity.newAmbulanceMapIntent(requireContext(), hospId, active.getRequestId()));
            } else {
                startActivity(UnifiedTrackingActivity.newIntent(requireContext(), TrackingCategory.AMBULANCE));
            }
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

    private void showAmbulanceRequestDialog() {
        if (currentAccount == null || getContext() == null) return;

        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(48, 24, 48, 12);

        EditText etPickup = new EditText(requireContext());
        etPickup.setHint("Pickup Street Address / Landmark");
        if (currentAccount.getAddress() != null && !currentAccount.getAddress().isEmpty()) {
            etPickup.setText(currentAccount.getAddress());
        }
        form.addView(etPickup);

        Spinner spPriority = new Spinner(requireContext());
        spPriority.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"CRITICAL", "HIGH", "STANDARD"}));
        form.addView(spPriority);

        EditText etNotes = new EditText(requireContext());
        etNotes.setHint("Emergency symptoms / floor / gate notes");
        form.addView(etNotes);

        new AlertDialog.Builder(requireContext())
                .setTitle("Request Hospital Ambulance Dispatch")
                .setMessage("Sends request to " + sessionManager.getSelectedHospitalName() + " Dispatch Desk.")
                .setView(form)
                .setPositiveButton("Dispatch Request", (d, w) -> {
                    String pickup = etPickup.getText().toString().trim();
                    if (pickup.isEmpty()) pickup = "Current Patient Location (Shared on Dispatch)";
                    String priority = spPriority.getSelectedItem().toString();
                    String notes = etNotes.getText().toString().trim();
                    submitAmbulanceWithOptionalLocation(pickup, priority, notes);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitAmbulanceWithOptionalLocation(String pickupAddress, String priority, String notes) {
        String hospId = sessionManager.getSelectedHospitalId();
        String hospName = sessionManager.getSelectedHospitalName();
        AmbulanceTrackingRepository ambRepo = new AmbulanceTrackingRepository(requireContext());

        boolean hasLoc = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (hasLoc) {
            FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(requireActivity());
            try {
                client.getLastLocation().addOnSuccessListener(loc -> {
                    Double lat = loc != null ? loc.getLatitude() : null;
                    Double lng = loc != null ? loc.getLongitude() : null;
                    dispatchAmbulance(ambRepo, hospId, hospName, pickupAddress, lat, lng, priority, notes);
                }).addOnFailureListener(e ->
                        dispatchAmbulance(ambRepo, hospId, hospName, pickupAddress, null, null, priority, notes));
                return;
            } catch (SecurityException ignored) {
            }
        }
        dispatchAmbulance(ambRepo, hospId, hospName, pickupAddress, null, null, priority, notes);
    }

    private void dispatchAmbulance(AmbulanceTrackingRepository ambRepo, String hospId, String hospName,
                                   String pickupAddress, Double lat, Double lng, String priority, String notes) {
        ambRepo.createAmbulanceRequest(hospId, hospName, currentAccount, pickupAddress, lat, lng,
                priority, notes, new AuthCallback<AmbulanceRequest>() {
                    @Override
                    public void onSuccess(AmbulanceRequest saved) {
                        if (binding != null) {
                            UiStateHelper.showSuccess(binding.getRoot(),
                                    "Ambulance request #" + saved.getRequestId() + " sent to dispatch.");
                        }
                        if (isAdded()) {
                            startActivity(LiveTrackingMapActivity.newAmbulanceMapIntent(
                                    requireContext(), hospId, saved.getRequestId()));
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (binding != null) UiStateHelper.showError(binding.getRoot(), message);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
