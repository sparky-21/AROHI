package com.arohi.healthcare.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.adapters.VerificationRequestAdapter;
import com.arohi.healthcare.databinding.FragmentAdminStaffBinding;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.DashboardRepository;
import com.arohi.healthcare.utils.UiStateHelper;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Hospital Staff & User Directory Fragment for Administrators to filter and manage
 * all registered Doctors, Nurses, Patients, and Pending Verification requests.
 */
public class AdminStaffFragment extends Fragment {

    private FragmentAdminStaffBinding binding;
    private DashboardRepository dashboardRepository;
    private VerificationRequestAdapter adapter;
    private final List<UserAccount> allAccounts = new ArrayList<>();
    private String currentFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminStaffBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dashboardRepository = new DashboardRepository(requireContext());

        adapter = new VerificationRequestAdapter(new VerificationRequestAdapter.OnVerificationActionListener() {
            @Override
            public void onApprove(@NonNull UserAccount userAccount) {
                updateAccountStatus(userAccount, VerificationStatus.VERIFIED);
            }

            @Override
            public void onReject(@NonNull UserAccount userAccount) {
                updateAccountStatus(userAccount, VerificationStatus.REJECTED);
            }
        });

        binding.rvStaffAccounts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvStaffAccounts.setAdapter(adapter);

        binding.btnFilterAll.setOnClickListener(v -> applyFilter("ALL"));
        binding.btnFilterPending.setOnClickListener(v -> applyFilter("PENDING"));
        binding.btnFilterDoctors.setOnClickListener(v -> applyFilter("DOCTOR"));
        binding.btnFilterNurses.setOnClickListener(v -> applyFilter("NURSE"));
        binding.btnFilterPatients.setOnClickListener(v -> applyFilter("PATIENT"));

        loadDirectory();
    }

    private void loadDirectory() {
        dashboardRepository.loadAllUsersAndStaff(new AuthCallback<List<UserAccount>>() {
            @Override
            public void onSuccess(List<UserAccount> result) {
                if (binding == null) return;
                allAccounts.clear();
                allAccounts.addAll(result);
                applyFilter(currentFilter);
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    private void applyFilter(String filterKey) {
        this.currentFilter = filterKey;
        if (binding != null) {
            MaterialButton selectedBtn = binding.btnFilterAll;
            if ("PENDING".equals(filterKey)) selectedBtn = binding.btnFilterPending;
            else if ("DOCTOR".equals(filterKey)) selectedBtn = binding.btnFilterDoctors;
            else if ("NURSE".equals(filterKey)) selectedBtn = binding.btnFilterNurses;
            else if ("PATIENT".equals(filterKey)) selectedBtn = binding.btnFilterPatients;

            UiStateHelper.updateToggleButtons(
                    selectedBtn,
                    R.color.white,
                    binding.btnFilterAll,
                    binding.btnFilterPending,
                    binding.btnFilterDoctors,
                    binding.btnFilterNurses,
                    binding.btnFilterPatients
            );
        }
        List<UserAccount> filtered = new ArrayList<>();
        for (UserAccount account : allAccounts) {
            if ("ALL".equals(filterKey)) {
                filtered.add(account);
            } else if ("PENDING".equals(filterKey)) {
                if (account.getVerificationStatusEnum() == VerificationStatus.PENDING) {
                    filtered.add(account);
                }
            } else if ("DOCTOR".equals(filterKey)) {
                if (account.getUserRoleEnum() == UserRole.DOCTOR) {
                    filtered.add(account);
                }
            } else if ("NURSE".equals(filterKey)) {
                if (account.getUserRoleEnum() == UserRole.NURSE) {
                    filtered.add(account);
                }
            } else if ("PATIENT".equals(filterKey)) {
                if (account.getUserRoleEnum() == UserRole.PATIENT) {
                    filtered.add(account);
                }
            }
        }

        adapter.submitList(filtered);
        binding.tvStaffDirectorySummary.setText(
                "Showing " + filtered.size() + " of " + allAccounts.size()
                        + " total Firestore accounts (Filter: " + filterKey + ")");

        if (filtered.isEmpty()) {
            binding.tvEmptyStaffDirectory.setVisibility(View.VISIBLE);
            binding.rvStaffAccounts.setVisibility(View.GONE);
        } else {
            binding.tvEmptyStaffDirectory.setVisibility(View.GONE);
            binding.rvStaffAccounts.setVisibility(View.VISIBLE);
        }
    }

    private void updateAccountStatus(@NonNull UserAccount target, @NonNull VerificationStatus newStatus) {
        dashboardRepository.updateUserVerificationStatus(target, newStatus, new AuthCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (binding == null) return;
                UiStateHelper.showSuccess(binding.getRoot(),
                        "Updated " + target.getFullName() + " to " + newStatus.getKey());
                loadDirectory();
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
