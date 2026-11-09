package com.arohi.healthcare.fragments;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.arohi.healthcare.databinding.FragmentAdminAnalyticsBinding;
import com.arohi.healthcare.models.HospitalAnalytics;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.EcosystemIntegrationRepository;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;

import java.util.Date;

/**
 * Phase 7 Administrator Hospital Analytics Dashboard.
 * Displays real clinical statistics computed strictly from available application records
 * (Bed occupancy, OT capacity, medical staff ratios, consultation volumes, and diagnostic throughput).
 */
public class AdminAnalyticsFragment extends Fragment {

    private FragmentAdminAnalyticsBinding binding;
    private EcosystemIntegrationRepository ecosystemRepository;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminAnalyticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ecosystemRepository = new EcosystemIntegrationRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        UserAccount current = sessionManager.getCurrentSessionAccount();
        if (current == null || current.getUserRoleEnum() != UserRole.ADMIN) {
            UiStateHelper.showError(binding.getRoot(), "Unauthorized: Administrator credentials required.");
        }

        binding.btnRefreshAnalytics.setOnClickListener(v -> loadAnalytics());
        loadAnalytics();
    }

    private void loadAnalytics() {
        if (binding == null) return;

        ecosystemRepository.computeHospitalAnalytics(new AuthCallback<HospitalAnalytics>() {
            @Override
            public void onSuccess(HospitalAnalytics a) {
                if (binding == null) return;

                // Timestamp
                binding.tvAnalyticsTimestamp.setText("Updated " + DateFormat.format("yyyy-MM-dd • hh:mm:ss a", new Date(a.getComputedAt())));

                // 1. Bed Occupancy
                binding.tvBedOccupancyPercent.setText(String.format(java.util.Locale.US, "%.1f%%", a.getBedOccupancyRatePercent()));
                binding.progressBedOccupancy.setProgress((int) Math.min(100, Math.round(a.getBedOccupancyRatePercent())));
                binding.tvTotalBeds.setText(String.valueOf(a.getTotalBedsCount()));
                binding.tvOccupiedBeds.setText(String.valueOf(a.getOccupiedBedsCount()));
                binding.tvAvailableBeds.setText(String.valueOf(a.getAvailableBedsCount()));
                binding.tvCleaningBeds.setText(String.valueOf(a.getCleaningBedsCount()));

                // 2. OT Capacity
                binding.tvOtUtilizationPercent.setText(String.format(java.util.Locale.US, "%.1f%% Utilized", a.getOtUtilizationRatePercent()));
                binding.tvTotalOtRooms.setText(String.valueOf(a.getTotalOtRoomsCount()));
                binding.tvInProgressOps.setText(String.valueOf(a.getInProgressOperationsCount()));
                binding.tvScheduledOps.setText(String.valueOf(a.getScheduledOperationsCount()));
                binding.tvCompletedOps.setText(String.valueOf(a.getCompletedOperationsCount()));

                // 3. Staff Coverage
                binding.tvTotalDoctors.setText(String.valueOf(a.getTotalDoctorsCount()));
                binding.tvTotalNurses.setText(String.valueOf(a.getTotalNursesCount()));
                binding.tvDocPatientRatio.setText(a.getDoctorPatientRatioString());
                binding.tvTotalDepts.setText(String.valueOf(a.getTotalDepartmentsCount()));

                // 4. Clinical Consultations & Labs
                binding.tvCompletedAppts.setText(String.valueOf(a.getCompletedAppointmentsCount()));
                binding.tvActiveRx.setText(String.valueOf(a.getActivePrescriptionsCount()));
                binding.tvCompletedLabs.setText(String.valueOf(a.getCompletedLabRequestsCount()));
                binding.tvCommunityPostsCount.setText(String.valueOf(a.getTotalCommunityPostsCount()));
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
