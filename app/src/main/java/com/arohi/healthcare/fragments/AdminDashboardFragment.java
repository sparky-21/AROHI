package com.arohi.healthcare.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.activities.RolePortalActivity;
import com.arohi.healthcare.adapters.VerificationRequestAdapter;
import com.arohi.healthcare.databinding.FragmentAdminDashboardBinding;
import com.arohi.healthcare.models.AdminProfile;
import com.arohi.healthcare.models.DashboardMetrics;
import com.arohi.healthcare.models.HospitalAnnouncement;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.DashboardRepository;
import com.arohi.healthcare.repositories.ProfileRepository;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;

import java.util.List;

/**
 * Hospital Administrator Dashboard Fragment displaying live hospital statistics
 * (Total Patients, Doctors & Nurses, Pending Verification Requests, Departments,
 * Ward & Bed Overview, OT Overview), Hospital Announcement publishing, and
 * real-time account approval/rejection for pending registrations.
 */
public class AdminDashboardFragment extends Fragment {

    private FragmentAdminDashboardBinding binding;
    private ProfileRepository profileRepository;
    private DashboardRepository dashboardRepository;
    private SessionManager sessionManager;
    private VerificationRequestAdapter pendingAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        profileRepository = new ProfileRepository(requireContext());
        dashboardRepository = new DashboardRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        setupPendingRecyclerView();

        UserAccount account = sessionManager.getCurrentSessionAccount();
        if (account != null) {
            loadAdminHeaderAndMetrics(account.getUid());
        }

        binding.btnViewAnalytics.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new AdminAnalyticsFragment())
                    .addToBackStack("admin_analytics")
                    .commit();
        });
        binding.cardAuthorityCommunicationHub.setOnClickListener(v -> {
            Intent intent = com.arohi.healthcare.activities.AuthorityCommunicationActivity.newIntent(
                    requireContext(),
                    sessionManager.getSelectedHospitalId(),
                    sessionManager.getSelectedHospitalCode()
            );
            startActivity(intent);
        });
        binding.cardHospitalBillingDashboard.setOnClickListener(v -> {
            Intent intent = com.arohi.healthcare.activities.HospitalBillingDashboardActivity.newIntent(
                    requireContext(),
                    sessionManager.getSelectedHospitalId()
            );
            startActivity(intent);
        });
        binding.cardAdminUnifiedTracking.setOnClickListener(v -> {
            startActivity(com.arohi.healthcare.activities.UnifiedTrackingActivity.newIntent(requireContext()));
        });
        binding.btnPublishAnnouncement.setOnClickListener(v -> showPublishAnnouncementDialog());
        binding.tvViewAllStaffLink.setOnClickListener(v -> switchTab(R.id.nav_admin_staff));
        binding.cardAdminDoctorsAndNurses.setOnClickListener(v -> switchTab(R.id.nav_admin_staff));
        binding.cardAdminTotalPatients.setOnClickListener(v -> switchTab(R.id.nav_admin_staff));
        binding.cardAdminPendingVerifications.setOnClickListener(v -> switchTab(R.id.nav_admin_staff));
        binding.cardAdminDepartments.setOnClickListener(v -> switchTab(R.id.nav_admin_departments));
        binding.cardAdminWardsAndBeds.setOnClickListener(v -> switchTab(R.id.nav_admin_operations));
        binding.cardAdminOtOverview.setOnClickListener(v -> switchTab(R.id.nav_admin_operations));
    }

    private void setupPendingRecyclerView() {
        pendingAdapter = new VerificationRequestAdapter(new VerificationRequestAdapter.OnVerificationActionListener() {
            @Override
            public void onApprove(@NonNull UserAccount userAccount) {
                handleVerificationAction(userAccount, VerificationStatus.VERIFIED);
            }

            @Override
            public void onReject(@NonNull UserAccount userAccount) {
                handleVerificationAction(userAccount, VerificationStatus.REJECTED);
            }
        });
        binding.rvPendingVerifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPendingVerifications.setAdapter(pendingAdapter);
    }

    private void handleVerificationAction(@NonNull UserAccount target,
                                          @NonNull VerificationStatus newStatus) {
        target.setVerificationStatus(newStatus.getKey());
        target.setUpdatedAt(System.currentTimeMillis());

        dashboardRepository.updateUserVerificationStatus(target, newStatus, new AuthCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (binding == null) return;
                String actionWord = newStatus == VerificationStatus.VERIFIED ? "Approved & Verified" : "Declined";
                UiStateHelper.showSuccess(binding.getRoot(),
                        actionWord + ": " + target.getFullName() + " (" + target.getRole() + ")");
                refreshDashboardData();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                UiStateHelper.showError(binding.getRoot(), message);
                refreshDashboardData();
            }
        });
    }

    private void loadAdminHeaderAndMetrics(String uid) {
        profileRepository.loadAdminProfile(uid, new AuthCallback<AdminProfile>() {
            @Override
            public void onSuccess(AdminProfile profile) {
                if (binding == null) return;
                binding.tvAdminHospitalName.setText(notEmpty(profile.getHospitalName(), "AROHI Central Hospital"));
                binding.tvAdminName.setText(notEmpty(profile.getFullName(), "Hospital Administrator"));
                binding.tvAdminDesignationAndId.setText(
                        notEmpty(profile.getDesignation(), "Hospital Authority")
                                + " • ID: " + notEmpty(profile.getArohiId(), "AROHI-ADM"));
            }

            @Override
            public void onError(String message) {
                // Keep default fallback
            }
        });

        refreshDashboardData();
    }

    private void refreshDashboardData() {
        dashboardRepository.loadAdminDashboardMetrics(new AuthCallback<DashboardMetrics>() {
            @Override
            public void onSuccess(DashboardMetrics metrics) {
                if (binding == null) return;
                binding.tvAdminTotalPatientsCount.setText(String.valueOf(metrics.getTotalPatientsCount()));

                int docs = metrics.getTotalDoctorsCount();
                int nurses = metrics.getTotalNursesCount();
                binding.tvAdminDoctorsAndNursesCount.setText(docs + " / " + nurses);
                binding.tvAdminStaffNote.setText(docs + " Doctors • " + nurses + " Nurses");

                binding.tvAdminPendingVerificationsCount.setText(
                        String.valueOf(metrics.getPendingVerificationsCount()));
                binding.tvAdminDepartmentsCount.setText(
                        String.valueOf(metrics.getDepartmentsCount()));

                int wards = metrics.getWardsCount();
                int beds = metrics.getTotalBedsCount();
                binding.tvAdminWardBedCount.setText(wards + " / " + beds);
                binding.tvAdminWardBedNote.setText(wards + " Wards • " + beds + " Beds");

                int otRooms = metrics.getOtRoomsCount();
                int surgeries = metrics.getScheduledSurgeriesCount();
                binding.tvAdminOtCount.setText(otRooms + " / " + surgeries);
                binding.tvAdminOtNote.setText(otRooms + " OT Rooms • " + surgeries + " Surgeries");

                binding.tvAdminLatestAnnouncementTitle.setText(metrics.getLatestAnnouncementTitle());
                binding.tvAdminLatestAnnouncementBody.setText(metrics.getLatestAnnouncementBody());
            }

            @Override
            public void onError(String message) {
                // Keep defaults
            }
        });

        dashboardRepository.loadPendingVerifications(new AuthCallback<List<UserAccount>>() {
            @Override
            public void onSuccess(List<UserAccount> pendingList) {
                if (binding == null) return;
                pendingAdapter.submitList(pendingList);
                if (pendingList.isEmpty()) {
                    binding.tvEmptyPendingVerifications.setVisibility(View.VISIBLE);
                    binding.rvPendingVerifications.setVisibility(View.GONE);
                } else {
                    binding.tvEmptyPendingVerifications.setVisibility(View.GONE);
                    binding.rvPendingVerifications.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.tvEmptyPendingVerifications.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showPublishAnnouncementDialog() {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);

        EditText etTitle = new EditText(requireContext());
        etTitle.setHint("Announcement Title (e.g., Vaccination Drive)");
        etTitle.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        container.addView(etTitle);

        EditText etContent = new EditText(requireContext());
        etContent.setHint("Official notice details for Patients, Doctors & Nurses...");
        etContent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        etContent.setMinLines(3);
        container.addView(etContent);

        new AlertDialog.Builder(requireContext())
                .setTitle("Publish Hospital Announcement")
                .setView(container)
                .setPositiveButton("Publish", (dialog, which) -> {
                    String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                    String body = etContent.getText() != null ? etContent.getText().toString().trim() : "";
                    if (title.isEmpty() || body.isEmpty()) {
                        if (binding != null) {
                            UiStateHelper.showError(binding.getRoot(), "Please enter both a title and message.");
                        }
                        return;
                    }
                    dashboardRepository.publishHospitalAnnouncement(
                            title, body, "HOSPITAL_NOTICE",
                            new AuthCallback<HospitalAnnouncement>() {
                                @Override
                                public void onSuccess(HospitalAnnouncement result) {
                                    if (binding == null) return;
                                    UiStateHelper.showSuccess(binding.getRoot(), "Hospital announcement published!");
                                    refreshDashboardData();
                                }

                                @Override
                                public void onError(String message) {
                                    if (binding == null) return;
                                    UiStateHelper.showError(binding.getRoot(), message);
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
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
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && binding != null && isAdded()) {
            refreshDashboardData();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && isAdded()) {
            refreshDashboardData();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
