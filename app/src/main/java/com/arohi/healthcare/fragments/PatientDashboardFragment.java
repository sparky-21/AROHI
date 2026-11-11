package com.arohi.healthcare.fragments;

import android.content.Intent;
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
        if (binding == null) return;
        String name = account.getFullName() != null && !account.getFullName().isEmpty()
                ? account.getFullName()
                : getString(R.string.role_patient_title);
        String arohiId = account.getArohiId() != null ? account.getArohiId() : "AROHI-PAT-0000";

        binding.tvPatientGreeting.setText(getString(R.string.pat_dash_hello, name));
        binding.tvPatientIdBadge.setText(getString(R.string.pat_dash_id_prefix, arohiId));

        VerificationStatus status = account.getVerificationStatusEnum();
        if (status == VerificationStatus.VERIFIED) {
            binding.bannerPatientVerification.setVisibility(View.GONE);
        } else {
            binding.bannerPatientVerification.setVisibility(View.VISIBLE);
            if (status == VerificationStatus.REJECTED) {
                binding.tvPatientVerificationTitle.setText(getString(R.string.pat_dash_declined_title));
                binding.tvPatientVerificationDesc.setText(
                        getString(R.string.pat_dash_pending_desc));
            } else {
                binding.tvPatientVerificationTitle.setText(getString(R.string.pat_dash_pending_title));
                binding.tvPatientVerificationDesc.setText(
                        getString(R.string.pat_dash_pending_desc));
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
        binding.cardPatientSmartJourney.setOnClickListener(v -> switchTab(R.id.nav_patient_records));

        binding.btnQuickFindDoctors.setOnClickListener(v -> switchTab(R.id.nav_patient_doctors));
        binding.cardPatientDoctorConnections.setOnClickListener(v -> switchTab(R.id.nav_patient_doctors));

        binding.btnQuickMedicalRecords.setOnClickListener(v -> switchTab(R.id.nav_patient_records));
        binding.cardPatientPrescriptions.setOnClickListener(v -> switchTab(R.id.nav_patient_records));
        binding.cardPatientReports.setOnClickListener(v -> switchTab(R.id.nav_patient_records));

        binding.btnQuickCalendar.setOnClickListener(v -> switchTab(R.id.nav_patient_calendar));
        binding.cardPatientAppointments.setOnClickListener(v -> switchTab(R.id.nav_patient_calendar));
        binding.cardPatientMedReminders.setOnClickListener(v -> switchTab(R.id.nav_patient_calendar));

        binding.btnQuickEditProfile.setOnClickListener(v -> switchTab(R.id.nav_patient_profile));

        // Phase 13 Patient Billing & Receipts
        binding.cardPatientBillsAndPayments.setOnClickListener(v -> {
            Intent intent = com.arohi.healthcare.activities.PatientBillingHistoryActivity.newIntent(requireContext());
            startActivity(intent);
        });

        // Phase 5 Quick Services
        binding.btnQuickPharmacy.setOnClickListener(v -> {
            if (getActivity() instanceof RolePortalActivity) {
                ((RolePortalActivity) getActivity()).openSubFragment(PharmacyFragment.newInstance(null), "pharmacy");
            }
        });

        binding.btnQuickAiAssistant.setOnClickListener(v -> {
            if (getActivity() instanceof RolePortalActivity) {
                ((RolePortalActivity) getActivity()).openSubFragment(AiAssistantFragment.newInstance(), "ai_assistant");
            }
        });

        binding.btnQuickHospitalMap.setOnClickListener(v -> {
            if (getActivity() instanceof RolePortalActivity) {
                ((RolePortalActivity) getActivity()).openSubFragment(HospitalMapFragment.newInstance(), "hospital_map");
            }
        });

        binding.btnQuickFamilyHealth.setOnClickListener(v -> {
            if (getActivity() instanceof RolePortalActivity) {
                ((RolePortalActivity) getActivity()).openSubFragment(FamilyProfilesFragment.newInstance(), "family_health");
            }
        });
    }

    private void showEmergencyCardDialog() {
        EmergencyCardDialogFragment.newInstance().show(getParentFragmentManager(), "emergency_card_dialog");
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
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && binding != null && isAdded() && sessionManager != null) {
            UserAccount account = sessionManager.getCurrentSessionAccount();
            if (account != null) {
                bindAccountHeader(account);
                loadPatientProfileAndMetrics(account.getUid());
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && isAdded() && sessionManager != null) {
            UserAccount account = sessionManager.getCurrentSessionAccount();
            if (account != null) {
                bindAccountHeader(account);
                loadPatientProfileAndMetrics(account.getUid());
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
