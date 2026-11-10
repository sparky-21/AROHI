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
import com.arohi.healthcare.databinding.FragmentDoctorDashboardBinding;
import com.arohi.healthcare.models.DashboardMetrics;
import com.arohi.healthcare.models.DoctorProfile;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.DashboardRepository;
import com.arohi.healthcare.repositories.ProfileRepository;
import com.arohi.healthcare.utils.SessionManager;

/**
 * Doctor Dashboard Fragment displaying live BMDC verification gate status,
 * specialist profile summary, today's appointments, pending patient requests,
 * accepted patients, upcoming operations, pending report reviews, follow-up reminders,
 * and recent patient activity.
 */
public class DoctorDashboardFragment extends Fragment {

    private FragmentDoctorDashboardBinding binding;
    private ProfileRepository profileRepository;
    private DashboardRepository dashboardRepository;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDoctorDashboardBinding.inflate(inflater, container, false);
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
            bindVerificationBanner(account);
            loadDoctorProfileAndMetrics(account.getUid());
        }

        binding.btnDoctorManageProfile.setOnClickListener(v -> {
            if (getActivity() instanceof RolePortalActivity) {
                ((RolePortalActivity) getActivity()).switchToTab(R.id.nav_doctor_profile);
            }
        });
    }

    private void bindVerificationBanner(@NonNull UserAccount account) {
        if (binding == null) return;
        String name = account.getFullName() != null && !account.getFullName().isEmpty()
                ? account.getFullName()
                : "Dr. Specialist";
        binding.tvDoctorGreeting.setText(name);

        VerificationStatus status = account.getVerificationStatusEnum();
        if (status == VerificationStatus.VERIFIED) {
            binding.bannerDoctorVerification.setVisibility(View.GONE);
        } else {
            binding.bannerDoctorVerification.setVisibility(View.VISIBLE);
            if (status == VerificationStatus.REJECTED) {
                binding.tvDoctorVerificationTitle.setText(getString(R.string.doc_dash_declined_title));
                binding.tvDoctorVerificationDesc.setText(getString(R.string.doc_dash_pending_desc));
            } else {
                binding.tvDoctorVerificationTitle.setText(getString(R.string.doc_dash_pending_title));
                binding.tvDoctorVerificationDesc.setText(getString(R.string.doc_dash_pending_desc));
            }
        }
    }

    private void loadDoctorProfileAndMetrics(String uid) {
        profileRepository.loadDoctorProfile(uid, new AuthCallback<DoctorProfile>() {
            @Override
            public void onSuccess(DoctorProfile profile) {
                if (binding == null || !isAdded()) return;
                String name = notEmpty(profile.getFullName(), "Dr. Specialist");
                String arohiId = notEmpty(profile.getArohiId(), "AROHI-DOC-0000");
                String license = notEmpty(profile.getMedicalLicenseNumber(), getString(R.string.status_pending));
                String specialty = notEmpty(profile.getSpecialty(), "General Medicine");
                String qualifications = notEmpty(profile.getQualifications(), "MBBS");
                String department = notEmpty(profile.getDepartment(), "Medicine");
                double fee = profile.getConsultationFeeBdt();

                binding.tvDoctorGreeting.setText(name);
                binding.tvDoctorIdBadge.setText(getString(R.string.doc_dash_id_bmdc, arohiId, license));
                binding.tvDoctorSpecialtySummary.setText(
                        "Specialty: " + specialty + " • Qualifications: " + qualifications);
                binding.tvDoctorDepartmentFee.setText(
                        "Department: " + department + " • Consultation Fee: " + String.format("%.0f", fee) + " BDT");
            }

            @Override
            public void onError(String message) {
                // Keep default UI fallback
            }
        });

        dashboardRepository.loadDoctorDashboardMetrics(uid, new AuthCallback<DashboardMetrics>() {
            @Override
            public void onSuccess(DashboardMetrics metrics) {
                if (binding == null) return;
                binding.tvDocTodayAppointments.setText(String.valueOf(metrics.getTodayAppointmentsCount()));
                binding.tvDocPendingRequests.setText(String.valueOf(metrics.getPendingRequestsCount()));
                binding.tvDocAcceptedPatients.setText(String.valueOf(metrics.getAcceptedPatientsCount()));
                binding.tvDocUpcomingOperations.setText(String.valueOf(metrics.getUpcomingOperationsCount()));
                binding.tvDocPendingReports.setText(String.valueOf(metrics.getPendingReportReviewsCount()));
                binding.tvDocFollowUpReminders.setText(String.valueOf(metrics.getFollowUpRemindersCount()));

                if (metrics.getAcceptedPatientsCount() > 0 || metrics.getPendingRequestsCount() > 0) {
                    binding.tvDoctorRecentActivity.setText(
                            "Live Practice Status: " + metrics.getAcceptedPatientsCount()
                                    + " connected patient(s), " + metrics.getPendingRequestsCount()
                                    + " pending request(s), and " + metrics.getTodayAppointmentsCount()
                                    + " scheduled appointment(s).");
                }
            }

            @Override
            public void onError(String message) {
                // Keep zero-state defaults
            }
        });
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
                loadDoctorProfileAndMetrics(account.getUid());
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && isAdded() && sessionManager != null) {
            UserAccount account = sessionManager.getCurrentSessionAccount();
            if (account != null) {
                loadDoctorProfileAndMetrics(account.getUid());
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
