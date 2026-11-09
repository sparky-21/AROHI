package com.arohi.healthcare.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.adapters.VerificationRequestAdapter;
import com.arohi.healthcare.databinding.FragmentAdminStaffBinding;
import com.arohi.healthcare.models.DoctorProfile;
import com.arohi.healthcare.models.NurseProfile;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.DashboardRepository;
import com.arohi.healthcare.repositories.ProfileRepository;
import com.arohi.healthcare.utils.SessionManager;
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
    private ProfileRepository profileRepository;
    private SessionManager sessionManager;
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
        profileRepository = new ProfileRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        adapter = new VerificationRequestAdapter(new VerificationRequestAdapter.OnVerificationActionListener() {
            @Override
            public void onApprove(@NonNull UserAccount userAccount) {
                updateAccountStatus(userAccount, VerificationStatus.VERIFIED);
            }

            @Override
            public void onReject(@NonNull UserAccount userAccount) {
                updateAccountStatus(userAccount, VerificationStatus.REJECTED);
            }

            @Override
            public void onAssignStaff(@NonNull UserAccount userAccount) {
                showStaffAssignmentDialog(userAccount);
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
        if (binding == null) return;
        this.currentFilter = filterKey;
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

    private void showStaffAssignmentDialog(@NonNull UserAccount target) {
        if (target.getUserRoleEnum() == UserRole.DOCTOR) {
            String[] depts = new String[]{
                    "Cardiology & Vascular Center",
                    "General, Laparoscopic & GI Surgery",
                    "Orthopedics & Joint Reconstruction",
                    "Critical Care & Anesthesiology",
                    "Internal Medicine & Diabetology",
                    "Pediatrics & Neonatology"
            };
            new AlertDialog.Builder(requireContext())
                    .setTitle("Assign Department: Dr. " + target.getFullName())
                    .setItems(depts, (dialog, which) -> {
                        String chosenDept = depts[which];
                        profileRepository.loadDoctorProfile(target.getUid(), new AuthCallback<DoctorProfile>() {
                            @Override
                            public void onSuccess(DoctorProfile profile) {
                                if (profile == null) profile = new DoctorProfile();
                                profile.setDepartment(chosenDept);
                                profile.setSpecialty(chosenDept);
                                profile.setUid(target.getUid());
                                profile.setFullName(target.getFullName());
                                profile.setDoctorId(target.getArohiId());

                                profileRepository.saveDoctorProfile(profile, new AuthCallback<DoctorProfile>() {
                                    @Override
                                    public void onSuccess(DoctorProfile result) {
                                        if (binding == null) return;
                                        UiStateHelper.showSuccess(binding.getRoot(),
                                                "Dr. " + target.getFullName() + " assigned to " + chosenDept + ".");
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
                            public void onError(String message) {}
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else if (target.getUserRoleEnum() == UserRole.NURSE) {
            String[] wards = new String[]{
                    "Ward A - Male Surgical (Floor 2)",
                    "Ward B - Female Medical (Floor 2)",
                    "ICU / CCU Critical Care (Floor 3)",
                    "Pediatric Inpatient Ward (Floor 1)"
            };
            new AlertDialog.Builder(requireContext())
                    .setTitle("Assign Ward: Nurse " + target.getFullName())
                    .setItems(wards, (dialog, which) -> {
                        String chosenWard = wards[which];
                        profileRepository.loadNurseProfile(target.getUid(), new AuthCallback<NurseProfile>() {
                            @Override
                            public void onSuccess(NurseProfile profile) {
                                if (profile == null) profile = new NurseProfile();
                                profile.setAssignedWardId(chosenWard);
                                profile.setPreferredWard(chosenWard);
                                profile.setUid(target.getUid());
                                profile.setFullName(target.getFullName());
                                profile.setNurseId(target.getArohiId());

                                profileRepository.saveNurseProfile(profile, new AuthCallback<NurseProfile>() {
                                    @Override
                                    public void onSuccess(NurseProfile result) {
                                        if (binding == null) return;
                                        UiStateHelper.showSuccess(binding.getRoot(),
                                                "Nurse " + target.getFullName() + " assigned to " + chosenWard + ".");
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
                            public void onError(String message) {}
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            new AlertDialog.Builder(requireContext())
                    .setTitle("User Profile: " + target.getFullName())
                    .setMessage("AROHI ID: " + target.getArohiId() + "\n"
                            + "Role: " + target.getRole() + "\n"
                            + "Email: " + target.getEmail() + "\n"
                            + "Status: " + target.getVerificationStatus())
                    .setPositiveButton("Close", null)
                    .show();
        }
    }

    private void updateAccountStatus(@NonNull UserAccount target, @NonNull VerificationStatus newStatus) {
        // Optimistically update target in memory immediately so UI reflects change with zero lag
        target.setVerificationStatus(newStatus.getKey());
        target.setUpdatedAt(System.currentTimeMillis());
        applyFilter(currentFilter);

        dashboardRepository.updateUserVerificationStatus(target, newStatus, new AuthCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (binding == null) return;
                String actionWord = newStatus == VerificationStatus.VERIFIED ? "Approved & Verified" : "Declined / Revoked";
                UiStateHelper.showSuccess(binding.getRoot(),
                        actionWord + ": " + target.getFullName() + " (" + target.getRole() + ")");
                loadDirectory();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                UiStateHelper.showError(binding.getRoot(), message);
                loadDirectory();
            }
        });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && binding != null && isAdded()) {
            loadDirectory();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && isAdded()) {
            loadDirectory();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
