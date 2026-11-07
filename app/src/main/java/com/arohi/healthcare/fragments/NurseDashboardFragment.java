package com.arohi.healthcare.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.arohi.healthcare.R;
import com.arohi.healthcare.activities.RolePortalActivity;
import com.arohi.healthcare.databinding.FragmentNurseDashboardBinding;
import com.arohi.healthcare.models.DashboardMetrics;
import com.arohi.healthcare.models.NurseProfile;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.DashboardRepository;
import com.arohi.healthcare.repositories.ProfileRepository;
import com.arohi.healthcare.utils.SessionManager;

/**
 * Nurse Dashboard Fragment displaying BNMC verification gate status,
 * assigned patients, assigned ward, today's nursing tasks, medication schedules,
 * upcoming procedures, operation assistance, duty schedule, and task completion overview.
 */
public class NurseDashboardFragment extends Fragment {

    private FragmentNurseDashboardBinding binding;
    private ProfileRepository profileRepository;
    private DashboardRepository dashboardRepository;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNurseDashboardBinding.inflate(inflater, container, false);
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
            bindVerificationState(account);
            loadNurseProfileAndMetrics(account.getUid());
        }

        binding.btnEditNurseProfileQuick.setOnClickListener(v -> switchTab(R.id.nav_nurse_profile));
        binding.cardNurseAssignedPatients.setOnClickListener(v -> switchTab(R.id.nav_nurse_patients));
        binding.cardNurseAssignedWard.setOnClickListener(v -> switchTab(R.id.nav_nurse_patients));
        binding.cardNurseTodayTasks.setOnClickListener(v -> switchTab(R.id.nav_nurse_tasks));
        binding.cardNurseMedicationSchedules.setOnClickListener(v -> switchTab(R.id.nav_nurse_tasks));
        binding.cardNurseProcedures.setOnClickListener(v -> switchTab(R.id.nav_nurse_schedule));
        binding.cardNurseOperationAssistance.setOnClickListener(v -> switchTab(R.id.nav_nurse_schedule));
    }

    private void bindVerificationState(@NonNull UserAccount account) {
        String name = account.getFullName() != null && !account.getFullName().isEmpty()
                ? account.getFullName()
                : "Registered Nurse";
        binding.tvNurseName.setText(name);

        VerificationStatus status = account.getVerificationStatusEnum();
        binding.tvNurseVerificationPill.setText(status.getKey());

        if (status == VerificationStatus.VERIFIED) {
            binding.cardNurseVerificationBanner.setVisibility(View.GONE);
        } else {
            binding.cardNurseVerificationBanner.setVisibility(View.VISIBLE);
            if (status == VerificationStatus.REJECTED) {
                binding.tvNurseVerificationBannerTitle.setText("NURSING CREDENTIAL REVIEW DECLINED");
                binding.tvNurseVerificationBannerDesc.setText(
                        "Hospital Administration declined your credential verification. Please review your BNMC Nursing License in your Profile.");
            } else {
                binding.tvNurseVerificationBannerTitle.setText("NURSING CREDENTIAL REVIEW PENDING");
                binding.tvNurseVerificationBannerDesc.setText(
                        "Your BNMC Nursing License is awaiting Hospital Administrator approval. Ward and task assignments unlock immediately after verification.");
            }
        }
    }

    private void loadNurseProfileAndMetrics(String uid) {
        profileRepository.loadNurseProfile(uid, new AuthCallback<NurseProfile>() {
            @Override
            public void onSuccess(NurseProfile profile) {
                if (binding == null) return;
                String name = notEmpty(profile.getFullName(), "Registered Nurse");
                String qual = notEmpty(profile.getQualification(), "BSc in Nursing");
                String license = notEmpty(profile.getNursingLicenseNumber(), "Pending");
                String prefWard = notEmpty(profile.getPreferredWard(), "General Ward");
                String assignedWard = notEmpty(profile.getAssignedWardId(), "UNASSIGNED");
                String shift = notEmpty(profile.getShiftPreference(), "Morning");

                binding.tvNurseName.setText(name);
                binding.tvNurseQualAndLicense.setText(qual + " • License: " + license);

                String activeWardDisplay = "UNASSIGNED".equalsIgnoreCase(assignedWard)
                        ? prefWard
                        : assignedWard;
                binding.tvNurseWardSummary.setText("Ward: " + activeWardDisplay);
                binding.tvNurseShiftBadge.setText("Shift: " + shift);
                binding.tvNurseAssignedWardName.setText(activeWardDisplay);
                binding.tvNursePreferredWardNote.setText("Preferred: " + prefWard);

                binding.tvNurseDutyScheduleDetails.setText(
                        "Nurse ID: " + notEmpty(profile.getArohiId(), "AROHI-NRS")
                                + "\nPreferred Shift: " + shift
                                + "\nPreferred Ward: " + prefWard
                                + "\nAdmin Assigned Ward: " + assignedWard);
            }

            @Override
            public void onError(String message) {
                // Keep default fallback
            }
        });

        dashboardRepository.loadNurseDashboardMetrics(uid, new AuthCallback<DashboardMetrics>() {
            @Override
            public void onSuccess(DashboardMetrics metrics) {
                if (binding == null) return;
                int totalTasks = metrics.getTodayTasksCount();
                int completedTasks = metrics.getCompletedTasksCount();
                int assignedPatients = metrics.getAssignedPatientsCount();

                binding.tvNurseAssignedPatientsCount.setText(String.valueOf(assignedPatients));
                binding.tvNurseTodayTasksCount.setText(String.valueOf(totalTasks));
                binding.tvNurseMedicationCount.setText(String.valueOf(metrics.getMedicationSchedulesCount()));
                binding.tvNurseProceduresCount.setText(String.valueOf(metrics.getUpcomingProceduresCount()));
                binding.tvNurseOperationAssistCount.setText(String.valueOf(metrics.getOperationAssistanceCount()));

                binding.tvNurseCompletionRatio.setText(completedTasks + " / " + totalTasks + " Completed");
                int progress = totalTasks > 0 ? (completedTasks * 100) / totalTasks : 0;
                binding.progressNurseTasks.setProgress(progress);

                if (totalTasks == 0) {
                    binding.tvNurseCompletionHint.setText(
                            "No bedside or medication tasks assigned for today in /nurseAssignments.");
                } else {
                    binding.tvNurseCompletionHint.setText(
                            progress + "% of today's assigned nursing care tasks completed.");
                }
            }

            @Override
            public void onError(String message) {
                // Keep zero-state defaults
            }
        });
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
        if (!hidden && sessionManager != null) {
            UserAccount account = sessionManager.getCurrentSessionAccount();
            if (account != null) {
                loadNurseProfileAndMetrics(account.getUid());
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sessionManager != null) {
            UserAccount account = sessionManager.getCurrentSessionAccount();
            if (account != null) {
                loadNurseProfileAndMetrics(account.getUid());
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
