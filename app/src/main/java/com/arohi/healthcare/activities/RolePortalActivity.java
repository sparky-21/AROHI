package com.arohi.healthcare.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ActivityRolePortalBinding;
import com.arohi.healthcare.fragments.AdminDashboardFragment;
import com.arohi.healthcare.fragments.AdminProfileFragment;
import com.arohi.healthcare.fragments.AdminStaffFragment;
import com.arohi.healthcare.fragments.ConversationsListFragment;
import com.arohi.healthcare.fragments.DoctorDashboardFragment;
import com.arohi.healthcare.fragments.DoctorPatientsFragment;
import com.arohi.healthcare.fragments.DoctorProfileFragment;
import com.arohi.healthcare.fragments.NurseDashboardFragment;
import com.arohi.healthcare.fragments.NurseProfileFragment;
import com.arohi.healthcare.fragments.PatientDashboardFragment;
import com.arohi.healthcare.fragments.PatientDoctorsFragment;
import com.arohi.healthcare.fragments.PatientProfileFragment;
import com.arohi.healthcare.fragments.PatientRecordsFragment;
import com.arohi.healthcare.fragments.RoleSectionInfoFragment;
import com.arohi.healthcare.fragments.UnifiedCalendarFragment;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.AuthRepository;
import com.arohi.healthcare.repositories.ProfileRepository;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;

/**
 * Main Role-Specific Portal Activity hosting the 5-tab BottomNavigationView
 * and switching fragments for Patient, Doctor, Nurse, and Hospital Administrator roles.
 */
public class RolePortalActivity extends AppCompatActivity {

    private ActivityRolePortalBinding binding;
    private SessionManager sessionManager;
    private ProfileRepository profileRepository;
    private AuthRepository authRepository;
    private UserAccount currentAccount;
    private int currentTabId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRolePortalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        profileRepository = new ProfileRepository(this);
        authRepository = new AuthRepository(this);

        currentAccount = sessionManager.getCurrentSessionAccount();
        if (currentAccount == null) {
            navigateToRoleSelection();
            return;
        }

        setupTopHeader(currentAccount);
        setupBottomNavigationForRole(currentAccount.getUserRoleEnum());

        binding.btnHeaderRefresh.setOnClickListener(v -> refreshHeaderAndCurrentTab(true));

        // Silently sync latest verification status from Firestore on portal launch
        refreshHeaderAndCurrentTab(false);
    }

    public void setupTopHeader(@NonNull UserAccount account) {
        this.currentAccount = account;
        UserRole role = account.getUserRoleEnum();
        VerificationStatus status = account.getVerificationStatusEnum();

        String portalTitle;
        switch (role) {
            case DOCTOR:
                portalTitle = "AROHI DOCTOR PORTAL";
                break;
            case NURSE:
                portalTitle = "AROHI NURSE PORTAL";
                break;
            case ADMIN:
                portalTitle = "AROHI HOSPITAL AUTHORITY";
                break;
            case PATIENT:
            default:
                portalTitle = "AROHI PATIENT PORTAL";
                break;
        }

        binding.tvPortalHeaderTitle.setText(portalTitle);
        String arohiId = account.getArohiId() != null ? account.getArohiId() : "AROHI-USER";
        String name = account.getFullName() != null ? account.getFullName() : "";
        binding.tvPortalHeaderSub.setText("ID: " + arohiId + (name.isEmpty() ? "" : " • " + name));

        binding.tvPortalStatusBadge.setText(status.getKey());
        if (status == VerificationStatus.VERIFIED) {
            binding.tvPortalStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_verified));
        } else if (status == VerificationStatus.REJECTED) {
            binding.tvPortalStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_emergency));
        } else {
            binding.tvPortalStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_warning));
        }
    }

    private void setupBottomNavigationForRole(@NonNull UserRole role) {
        binding.bottomNavigation.getMenu().clear();
        int defaultTabId;

        switch (role) {
            case DOCTOR:
                binding.bottomNavigation.inflateMenu(R.menu.menu_bottom_doctor);
                defaultTabId = R.id.nav_doctor_dashboard;
                break;
            case NURSE:
                binding.bottomNavigation.inflateMenu(R.menu.menu_bottom_nurse);
                defaultTabId = R.id.nav_nurse_dashboard;
                break;
            case ADMIN:
                binding.bottomNavigation.inflateMenu(R.menu.menu_bottom_admin);
                defaultTabId = R.id.nav_admin_overview;
                break;
            case PATIENT:
            default:
                binding.bottomNavigation.inflateMenu(R.menu.menu_bottom_patient);
                defaultTabId = R.id.nav_patient_home;
                break;
        }

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == currentTabId) {
                return true;
            }
            switchToTabInternal(itemId);
            return true;
        });

        binding.bottomNavigation.setSelectedItemId(defaultTabId);
    }

    private void switchToTabInternal(@IdRes int itemId) {
        String targetTag = "tab_" + itemId;
        String currentTag = currentTabId != -1 ? "tab_" + currentTabId : null;

        androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
        androidx.fragment.app.FragmentTransaction ft = fm.beginTransaction();

        if (currentTag != null) {
            Fragment currentFragment = fm.findFragmentByTag(currentTag);
            if (currentFragment != null) {
                ft.hide(currentFragment);
            }
        }

        Fragment targetFragment = fm.findFragmentByTag(targetTag);
        if (targetFragment == null) {
            targetFragment = createFragmentForMenuItem(itemId);
            if (targetFragment != null) {
                ft.add(R.id.fragmentContainer, targetFragment, targetTag);
            }
        } else {
            ft.show(targetFragment);
        }

        ft.commitAllowingStateLoss();
        currentTabId = itemId;
    }

    private Fragment createFragmentForMenuItem(@IdRes int itemId) {
        // PATIENT TABS
        if (itemId == R.id.nav_patient_home) {
            return new PatientDashboardFragment();
        } else if (itemId == R.id.nav_patient_doctors) {
            return new PatientDoctorsFragment();
        } else if (itemId == R.id.nav_patient_records) {
            return new PatientRecordsFragment();
        } else if (itemId == R.id.nav_patient_calendar) {
            return UnifiedCalendarFragment.newInstance(false);
        } else if (itemId == R.id.nav_patient_profile) {
            return new PatientProfileFragment();
        }

        // DOCTOR TABS
        else if (itemId == R.id.nav_doctor_dashboard) {
            return new DoctorDashboardFragment();
        } else if (itemId == R.id.nav_doctor_patients) {
            return new DoctorPatientsFragment();
        } else if (itemId == R.id.nav_doctor_schedule) {
            return UnifiedCalendarFragment.newInstance(true);
        } else if (itemId == R.id.nav_doctor_messages) {
            return new ConversationsListFragment();
        } else if (itemId == R.id.nav_doctor_profile) {
            return new DoctorProfileFragment();
        }

        // NURSE TABS
        else if (itemId == R.id.nav_nurse_dashboard) {
            return new NurseDashboardFragment();
        } else if (itemId == R.id.nav_nurse_patients) {
            return RoleSectionInfoFragment.newInstance(
                    "Assigned Ward Patients",
                    "Connected to Cloud Firestore /nurseAssignments & /wards.",
                    "NURSE_PATIENTS",
                    "Phase 5 Inpatient Ward Care: View patients admitted to your assigned ward and monitor bedside care requirements."
            );
        } else if (itemId == R.id.nav_nurse_tasks) {
            return RoleSectionInfoFragment.newInstance(
                    "Nursing Care & Medication Tasks",
                    "Connected to Cloud Firestore /nurseAssignments.",
                    "NURSE_TASKS",
                    "Phase 5 Nursing Task Execution: Mark scheduled medication doses, vitals checks, and post-operative care tasks as completed."
            );
        } else if (itemId == R.id.nav_nurse_schedule) {
            return RoleSectionInfoFragment.newInstance(
                    "Nursing Shift & OT Assistance Roster",
                    "Connected to Cloud Firestore /nurses & /operations.",
                    "NURSE_SCHEDULE",
                    "Phase 5 Duty Roster & OT Assistance: View your Morning/Evening/Night ward shift assignments and Operation Theatre assistance schedule."
            );
        } else if (itemId == R.id.nav_nurse_profile) {
            return new NurseProfileFragment();
        }

        // ADMIN TABS
        else if (itemId == R.id.nav_admin_overview) {
            return new AdminDashboardFragment();
        } else if (itemId == R.id.nav_admin_departments) {
            return RoleSectionInfoFragment.newInstance(
                    "Hospital Departments & Wards",
                    "Connected to Cloud Firestore /hospitalDepartments, /wards, & /beds.",
                    "ADMIN_DEPARTMENTS",
                    "Phase 5 Hospital Infrastructure Management: Configure clinical departments, inpatient wards, and real-time bed occupancy."
            );
        } else if (itemId == R.id.nav_admin_operations) {
            return RoleSectionInfoFragment.newInstance(
                    "Operation Theatre (OT) Management",
                    "Connected to Cloud Firestore /operationRooms & /operations.",
                    "ADMIN_OPERATIONS",
                    "Phase 5 Operation Theatre Scheduling: Manage OT rooms, assign surgical teams, and coordinate scheduled operations."
            );
        } else if (itemId == R.id.nav_admin_staff) {
            return new AdminStaffFragment();
        } else if (itemId == R.id.nav_admin_settings) {
            return new AdminProfileFragment();
        }

        return new PatientDashboardFragment();
    }

    public void switchToTab(@IdRes int menuItemId) {
        if (binding != null) {
            binding.bottomNavigation.setSelectedItemId(menuItemId);
        }
    }

    public void switchToDefaultDashboardTab() {
        if (currentAccount == null) return;
        switch (currentAccount.getUserRoleEnum()) {
            case DOCTOR:
                switchToTab(R.id.nav_doctor_dashboard);
                break;
            case NURSE:
                switchToTab(R.id.nav_nurse_dashboard);
                break;
            case ADMIN:
                switchToTab(R.id.nav_admin_overview);
                break;
            case PATIENT:
            default:
                switchToTab(R.id.nav_patient_home);
                break;
        }
    }

    public void refreshHeaderAndCurrentTab(boolean showFeedback) {
        profileRepository.refreshCurrentAccount(new AuthCallback<UserAccount>() {
            @Override
            public void onSuccess(UserAccount result) {
                if (isFinishing() || isDestroyed()) return;
                setupTopHeader(result);
                if (currentTabId != -1) {
                    Fragment refreshed = createFragmentForMenuItem(currentTabId);
                    if (refreshed != null) {
                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragmentContainer, refreshed)
                                .commitAllowingStateLoss();
                    }
                }
                if (showFeedback) {
                    UiStateHelper.showSuccess(binding.getRoot(),
                            "Synced with Cloud Firestore • Status: " + result.getVerificationStatusEnum().getKey());
                }
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                if (showFeedback) {
                    UiStateHelper.showError(binding.getRoot(), message);
                }
            }
        });
    }

    public void performLogout() {
        authRepository.logout();
        navigateToRoleSelection();
    }

    private void navigateToRoleSelection() {
        Intent intent = new Intent(this, RoleSelectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
