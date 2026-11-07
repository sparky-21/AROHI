package com.arohi.healthcare.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.arohi.healthcare.R;
import com.arohi.healthcare.activities.RolePortalActivity;
import com.arohi.healthcare.databinding.FragmentPatientDashboardBinding;
import com.arohi.healthcare.models.DashboardMetrics;
import com.arohi.healthcare.models.PatientProfile;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.DashboardRepository;
import com.arohi.healthcare.repositories.ProfileRepository;
import com.arohi.healthcare.utils.SessionManager;

/**
 * Personalized Patient Dashboard Fragment displaying live Firestore patient profile,
 * verification status banner, Emergency Health Card shortcut, live clinical metrics,
 * Smart Health Journey preview, quick-access healthcare services, and Hospital Announcements.
 */
public class PatientDashboardFragment extends Fragment {

    private FragmentPatientDashboardBinding binding;
    private ProfileRepository profileRepository;
    private DashboardRepository dashboardRepository;
    private SessionManager sessionManager;
    private PatientProfile loadedProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPatientDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        profileRepository = new ProfileRepository(requireContext());
        dashboardRepository = new DashboardRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        UserAccount account = sessionManager.getCurrentSessionAccount();
        if (account != null) {
            bindAccountHeader(account);
            loadPatientProfileAndMetrics(account.getUid());
        }

        setupClickListeners();
    }

    private void bindAccountHeader(@NonNull UserAccount account) {
        String name = account.getFullName() != null && !account.getFullName().isEmpty()
                ? account.getFullName()
                : "Patient";
        String arohiId = account.getArohiId() != null ? account.getArohiId() : "AROHI-PAT-0000";

        binding.tvPatientGreeting.setText("Hello, " + name);
        binding.tvPatientIdBadge.setText("Patient ID: " + arohiId);

        VerificationStatus status = account.getVerificationStatusEnum();
        if (status == VerificationStatus.VERIFIED) {
            binding.bannerPatientVerification.setVisibility(View.GONE);
        } else {
            binding.bannerPatientVerification.setVisibility(View.VISIBLE);
            if (status == VerificationStatus.REJECTED) {
                binding.tvPatientVerificationTitle.setText("Account Verification Declined");
                binding.tvPatientVerificationDesc.setText(
                        "Please review your profile details and contact Hospital Administration.");
            } else {
                binding.tvPatientVerificationTitle.setText("Account Verification Pending");
                binding.tvPatientVerificationDesc.setText(
                        "Your Patient registration is awaiting Hospital Authority approval. You can already complete your Health Profile and Emergency Card.");
            }
        }
    }

    private void loadPatientProfileAndMetrics(String uid) {
        profileRepository.loadPatientProfile(uid, new AuthCallback<PatientProfile>() {
            @Override
            public void onSuccess(PatientProfile profile) {
                if (binding == null) return;
                loadedProfile = profile;
                String blood = notEmpty(profile.getBloodGroup(), "Not Set");
                String allergies = notEmpty(profile.getKnownAllergies(), "None");
                String emergencyContact = notEmpty(profile.getEmergencyContactName(), "Not Set");
                String emergencyPhone = notEmpty(profile.getEmergencyContactPhone(), "---");

                binding.tvPatientMedicalSummary.setText(
                        "Blood Group: " + blood + " • Allergies: " + allergies);
                binding.tvEmergencyCardPreview.setText(
                        "Blood: " + blood + " • Emergency: " + emergencyContact + " (" + emergencyPhone + ")");
            }

            @Override
            public void onError(String message) {
                // Keep default fallback text
            }
        });

        dashboardRepository.loadPatientDashboardMetrics(uid, new AuthCallback<DashboardMetrics>() {
            @Override
            public void onSuccess(DashboardMetrics metrics) {
                if (binding == null) return;
                int appts = metrics.getUpcomingAppointmentsCount();
                int rx = metrics.getRecentPrescriptionsCount();
                int reports = metrics.getPendingReportsCount();
                int meds = metrics.getActiveMedicationsCount();
                int docs = metrics.getConnectedDoctorsCount();
                int notifs = metrics.getUnreadNotificationsCount();

                binding.tvUpcomingAppointmentsCount.setText(String.valueOf(appts));
                binding.tvAppointmentsStatusSub.setText(appts > 0
                        ? appts + " scheduled consultation(s)"
                        : "No scheduled appointments");

                binding.tvRecentPrescriptionsCount.setText(String.valueOf(rx));
                binding.tvPrescriptionsStatusSub.setText(rx > 0
                        ? rx + " digital prescription(s)"
                        : "No digital prescriptions yet");

                binding.tvPendingReportsCount.setText(String.valueOf(reports));
                binding.tvMedicineRemindersCount.setText(String.valueOf(meds));
                binding.tvDoctorConnectionsCount.setText(String.valueOf(docs));
                binding.tvNotificationsCount.setText(String.valueOf(notifs));

                binding.tvPatientAnnouncementTitle.setText(metrics.getLatestAnnouncementTitle());
                binding.tvPatientAnnouncementBody.setText(metrics.getLatestAnnouncementBody());

                if (appts > 0 || rx > 0 || docs > 0) {
                    binding.tvPatientJourneyCurrentState.setText(
                            "Current Stage: Active Clinical Consultation (" + docs + " Connected Doctor(s))");
                } else {
                    binding.tvPatientJourneyCurrentState.setText(
                            "Current Stage: Step 1 — Identity Registered (Connect with a Doctor in Phase 3 to advance)");
                }
            }

            @Override
            public void onError(String message) {
                // Keep zero-state defaults
            }
        });
    }

    private void setupClickListeners() {
        binding.cardEmergencyShortcut.setOnClickListener(v -> showEmergencyCardDialog());

        binding.btnQuickFindDoctors.setOnClickListener(v -> switchTab(R.id.nav_patient_doctors));
        binding.cardPatientDoctorConnections.setOnClickListener(v -> switchTab(R.id.nav_patient_doctors));

        binding.btnQuickMedicalRecords.setOnClickListener(v -> switchTab(R.id.nav_patient_records));
        binding.cardPatientPrescriptions.setOnClickListener(v -> switchTab(R.id.nav_patient_records));
        binding.cardPatientReports.setOnClickListener(v -> switchTab(R.id.nav_patient_records));

        binding.btnQuickCalendar.setOnClickListener(v -> switchTab(R.id.nav_patient_calendar));
        binding.cardPatientAppointments.setOnClickListener(v -> switchTab(R.id.nav_patient_calendar));
        binding.cardPatientMedReminders.setOnClickListener(v -> switchTab(R.id.nav_patient_calendar));

        binding.btnQuickEditProfile.setOnClickListener(v -> switchTab(R.id.nav_patient_profile));
    }

    private void showEmergencyCardDialog() {
        String name = loadedProfile != null ? notEmpty(loadedProfile.getFullName(), "Patient") : "Patient";
        String id = loadedProfile != null ? notEmpty(loadedProfile.getArohiId(), "AROHI-PAT") : "AROHI-PAT";
        String dob = loadedProfile != null ? notEmpty(loadedProfile.getDateOfBirth(), "Not Set") : "Not Set";
        String blood = loadedProfile != null ? notEmpty(loadedProfile.getBloodGroup(), "Not Set") : "Not Set";
        String allergies = loadedProfile != null ? notEmpty(loadedProfile.getKnownAllergies(), "None") : "None";
        String chronic = loadedProfile != null ? notEmpty(loadedProfile.getChronicConditions(), "None") : "None";
        String emName = loadedProfile != null ? notEmpty(loadedProfile.getEmergencyContactName(), "Not Set") : "Not Set";
        String emPhone = loadedProfile != null ? notEmpty(loadedProfile.getEmergencyContactPhone(), "Not Set") : "Not Set";

        String message = "Patient: " + name + "\n"
                + "AROHI ID: " + id + "\n"
                + "Date of Birth: " + dob + "\n"
                + "Blood Group: " + blood + "\n"
                + "Known Allergies: " + allergies + "\n"
                + "Chronic Conditions: " + chronic + "\n"
                + "Emergency Contact: " + emName + "\n"
                + "Emergency Phone: " + emPhone;

        new AlertDialog.Builder(requireContext())
                .setTitle("AROHI Emergency Health Card")
                .setMessage(message)
                .setPositiveButton("Update Health Card", (d, w) -> switchTab(R.id.nav_patient_profile))
                .setNegativeButton("Close", null)
                .show();
    }

    private void switchTab(int tabId) {
        if (getActivity() instanceof RolePortalActivity) {
            ((RolePortalActivity) getActivity()).switchToTab(tabId);
        }
    }

    private String notEmpty(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value.trim() : fallback;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
