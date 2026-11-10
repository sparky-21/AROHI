package com.arohi.healthcare.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ActivityRolePortalBinding;
import com.arohi.healthcare.fragments.AdminDashboardFragment;
import com.arohi.healthcare.fragments.AdminOperationsFragment;
import com.arohi.healthcare.fragments.AdminProfileFragment;
import com.arohi.healthcare.fragments.AdminStaffFragment;
import com.arohi.healthcare.fragments.AiAssistantFragment;
import com.arohi.healthcare.fragments.ConversationsListFragment;
import com.arohi.healthcare.fragments.DoctorDashboardFragment;
import com.arohi.healthcare.fragments.DoctorPatientsFragment;
import com.arohi.healthcare.fragments.DoctorProfileFragment;
import com.arohi.healthcare.fragments.FamilyProfilesFragment;
import com.arohi.healthcare.fragments.GoogleCalendarFragment;
import com.arohi.healthcare.fragments.GoogleHospitalMapFragment;
import com.arohi.healthcare.fragments.HospitalMapFragment;
import com.arohi.healthcare.fragments.NurseDashboardFragment;
import com.arohi.healthcare.fragments.NursePatientsFragment;
import com.arohi.healthcare.fragments.NurseProfileFragment;
import com.arohi.healthcare.fragments.NurseScheduleFragment;
import com.arohi.healthcare.fragments.NurseTasksFragment;
import com.arohi.healthcare.fragments.PatientDashboardFragment;
import com.arohi.healthcare.fragments.PatientDoctorsFragment;
import com.arohi.healthcare.fragments.PatientProfileFragment;
import com.arohi.healthcare.fragments.PatientRecordsFragment;
import com.arohi.healthcare.fragments.PharmacyFragment;
import com.arohi.healthcare.fragments.RoleSectionInfoFragment;
import com.arohi.healthcare.fragments.UnifiedCalendarFragment;
import com.arohi.healthcare.models.ConversationThread;
import com.arohi.healthcare.models.Hospital;
import com.arohi.healthcare.models.HospitalMembership;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.AuthRepository;
import com.arohi.healthcare.repositories.ProfileRepository;
import com.arohi.healthcare.repositories.SecureMessagingRepository;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;
import com.google.android.material.badge.BadgeDrawable;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * Main Role-Specific Portal Activity hosting the 5-tab BottomNavigationView
 * and switching fragments for Patient, Doctor, Nurse, and Hospital Administrator roles.
 */
public class RolePortalActivity extends BaseActivity {

    private ActivityRolePortalBinding binding;
    private SessionManager sessionManager;
    private ProfileRepository profileRepository;
    private AuthRepository authRepository;
    private SecureMessagingRepository messagingRepository;
    private ListenerRegistration unreadConversationsListener;
    private UserAccount currentAccount;
    private int currentTabId = -1;
    private int defaultTabId = -1;
    private boolean restoredState = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRolePortalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState != null) {
            currentTabId = savedInstanceState.getInt("saved_current_tab_id", -1);
            restoredState = true;
        }

        sessionManager = new SessionManager(this);
        profileRepository = new ProfileRepository(this);
        authRepository = new AuthRepository(this);
        messagingRepository = new SecureMessagingRepository(this);

        currentAccount = sessionManager.getCurrentSessionAccount();
        if (currentAccount == null) {
            navigateToRoleSelection();
            return;
        }

        setupTopHeader(currentAccount);
        setupBottomNavigationForRole(currentAccount.getUserRoleEnum());
        attachUnreadMessagesMonitor();

        binding.btnHeaderRefresh.setOnClickListener(v -> refreshHeaderAndCurrentTab(true));
        binding.btnHeaderCommunityFeed.setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new com.arohi.healthcare.fragments.CommunityFeedFragment())
                    .addToBackStack("community_feed")
                    .commit();
        });
        binding.btnHeaderSettings.setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new com.arohi.healthcare.fragments.SettingsFragment())
                    .addToBackStack("settings")
                    .commit();
        });

        // Silently sync latest verification status from Firestore on portal launch
        refreshHeaderAndCurrentTab(false);

        // Safe Back Navigation: unwinds subfragments or returns to main dashboard tab
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                    return;
                }
                if (defaultTabId != -1 && currentTabId != defaultTabId) {
                    switchToTab(defaultTabId);
                    return;
                }
                finish();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("saved_current_tab_id", currentTabId);
    }

    public void setupTopHeader(@NonNull UserAccount account) {
        this.currentAccount = account;
        UserRole role = account.getUserRoleEnum();
        VerificationStatus status = account.getVerificationStatusEnum();

        String portalTitle;
        switch (role) {
            case DOCTOR:
                portalTitle = getString(R.string.portal_title_doctor);
                break;
            case NURSE:
                portalTitle = getString(R.string.portal_title_nurse);
                break;
            case ADMIN:
                portalTitle = getString(R.string.portal_title_admin);
                break;
            case PATIENT:
            default:
                portalTitle = getString(R.string.portal_title_patient);
                break;
        }

        binding.tvPortalHeaderTitle.setText(portalTitle);
        String arohiId = account.getArohiId() != null ? account.getArohiId() : "AROHI-USER";
        String name = account.getFullName() != null ? account.getFullName() : "";
        String hospCode = sessionManager.getSelectedHospitalCode();
        binding.tvPortalHeaderSub.setText("[" + (hospCode != null ? hospCode : "AROHI-HQ") + "] " + arohiId + (name.isEmpty() ? "" : " • " + name));

        binding.topPortalBar.setOnClickListener(v -> showHospitalSwitcherDialog());

        String statusLabel;
        if (status == VerificationStatus.VERIFIED) {
            statusLabel = getString(R.string.status_verified);
            binding.tvPortalStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_verified));
        } else if (status == VerificationStatus.REJECTED) {
            statusLabel = getString(R.string.status_rejected);
            binding.tvPortalStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_emergency));
        } else {
            statusLabel = getString(R.string.status_pending);
            binding.tvPortalStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_warning));
        }
        binding.tvPortalStatusBadge.setText(statusLabel);
    }

    private void setupBottomNavigationForRole(@NonNull UserRole role) {
        binding.bottomNavigation.getMenu().clear();

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
            if (itemId == currentTabId && getSupportFragmentManager().getBackStackEntryCount() == 0) {
                return true;
            }
            switchToTabInternal(itemId);
            return true;
        });

        boolean preserveBackStack = restoredState && getSupportFragmentManager().getBackStackEntryCount() > 0;
        if (preserveBackStack) {
            int tabToHighlight = currentTabId != -1 ? currentTabId : defaultTabId;
            if (binding.bottomNavigation.getMenu().findItem(tabToHighlight) != null) {
                binding.bottomNavigation.getMenu().findItem(tabToHighlight).setChecked(true);
            }
        } else if (restoredState && currentTabId != -1 && binding.bottomNavigation.getMenu().findItem(currentTabId) != null) {
            int target = currentTabId;
            currentTabId = -1;
            binding.bottomNavigation.setSelectedItemId(target);
        } else {
            binding.bottomNavigation.setSelectedItemId(defaultTabId);
        }
    }

    private void switchToTabInternal(@IdRes int itemId) {
        androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();

        // Safely clear any backstack subfragments before switching tabs
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        String targetTag = "tab_" + itemId;
        String currentTag = currentTabId != -1 ? "tab_" + currentTabId : null;

        androidx.fragment.app.FragmentTransaction ft = fm.beginTransaction();

        if (currentTag != null) {
            Fragment currentFragment = fm.findFragmentByTag(currentTag);
            if (currentFragment != null) {
                ft.hide(currentFragment);
            }
        }

        Fragment targetFragment = fm.findFragmentByTag(targetTag);
        // If target fragment exists but view is destroyed, remove so it recreates view cleanly
        if (targetFragment != null && targetFragment.getView() == null) {
            ft.remove(targetFragment);
            targetFragment = null;
        }

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
            return new NursePatientsFragment();
        } else if (itemId == R.id.nav_nurse_tasks) {
            return new NurseTasksFragment();
        } else if (itemId == R.id.nav_nurse_schedule) {
            return new NurseScheduleFragment();
        } else if (itemId == R.id.nav_nurse_profile) {
            return new NurseProfileFragment();
        }

        // ADMIN TABS
        else if (itemId == R.id.nav_admin_overview) {
            return new AdminDashboardFragment();
        } else if (itemId == R.id.nav_admin_departments) {
            return HospitalMapFragment.newInstance();
        } else if (itemId == R.id.nav_admin_operations) {
            return new AdminOperationsFragment();
        } else if (itemId == R.id.nav_admin_staff) {
            return new AdminStaffFragment();
        } else if (itemId == R.id.nav_admin_settings) {
            return new AdminProfileFragment();
        }

        return new PatientDashboardFragment();
    }

    public void openSubFragment(Fragment fragment, String tag) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(tag)
                .commitAllowingStateLoss();
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
                if (currentTabId != -1 && getSupportFragmentManager().getBackStackEntryCount() == 0) {
                    androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
                    String tag = "tab_" + currentTabId;
                    Fragment existing = fm.findFragmentByTag(tag);
                    androidx.fragment.app.FragmentTransaction ft = fm.beginTransaction();
                    if (existing != null) {
                        ft.remove(existing);
                    }
                    Fragment refreshed = createFragmentForMenuItem(currentTabId);
                    if (refreshed != null) {
                        ft.add(R.id.fragmentContainer, refreshed, tag).commitAllowingStateLoss();
                    }
                }
                if (showFeedback && binding != null) {
                    UiStateHelper.showSuccess(binding.getRoot(),
                            getString(R.string.sync_firestore_status, result.getVerificationStatusEnum().getKey()));
                }
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                if (showFeedback && binding != null) {
                    UiStateHelper.showError(binding.getRoot(), message);
                }
            }
        });
    }

    private void attachUnreadMessagesMonitor() {
        if (currentAccount == null || messagingRepository == null) return;
        UserRole role = currentAccount.getUserRoleEnum();
        if (role != UserRole.DOCTOR && role != UserRole.PATIENT) return;

        if (unreadConversationsListener != null) {
            unreadConversationsListener.remove();
            unreadConversationsListener = null;
        }
        unreadConversationsListener = messagingRepository.attachConversationsListener(new AuthCallback<List<ConversationThread>>() {
            @Override
            public void onSuccess(List<ConversationThread> result) {
                if (isFinishing() || isDestroyed() || binding == null || result == null) return;
                int unreadCount = messagingRepository.countUnreadConversations(result, role);
                updateUnreadNavBadge(unreadCount);
            }

            @Override
            public void onError(String message) {
                // Keep existing state gracefully
            }
        });
    }

    public void updateUnreadNavBadge(int unreadCount) {
        if (binding == null || currentAccount == null) return;
        UserRole role = currentAccount.getUserRoleEnum();
        int targetItemId = -1;
        if (role == UserRole.DOCTOR) {
            targetItemId = R.id.nav_doctor_messages;
        } else if (role == UserRole.PATIENT) {
            targetItemId = R.id.nav_patient_doctors;
        }
        if (targetItemId == -1) return;

        if (unreadCount > 0) {
            BadgeDrawable badge = binding.bottomNavigation.getOrCreateBadge(targetItemId);
            badge.setVisible(true);
            badge.setNumber(unreadCount);
            badge.setBackgroundColor(ContextCompat.getColor(this, R.color.status_emergency));
            badge.setBadgeTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            binding.bottomNavigation.removeBadge(targetItemId);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentAccount != null && messagingRepository != null) {
            attachUnreadMessagesMonitor();
        }
    }

    @Override
    protected void onDestroy() {
        if (unreadConversationsListener != null) {
            unreadConversationsListener.remove();
            unreadConversationsListener = null;
        }
        super.onDestroy();
    }

    private void showHospitalSwitcherDialog() {
        if (currentAccount == null) return;
        List<HospitalMembership> memberships = sessionManager.getUserMemberships(currentAccount.getUid());
        String currentHospId = sessionManager.getSelectedHospitalId();

        List<String> options = new java.util.ArrayList<>();
        List<Hospital> matchingHospitals = new java.util.ArrayList<>();

        for (HospitalMembership m : memberships) {
            Hospital h = sessionManager.findHospitalById(m.getHospitalId());
            if (h != null) {
                boolean isCurrent = h.getHospitalId().equalsIgnoreCase(currentHospId);
                options.add((isCurrent ? getString(R.string.hospital_prefix_active) : getString(R.string.hospital_prefix_switch))
                        + h.getHospitalName() + " [" + h.getHospitalCode() + "] • "
                        + getString(R.string.label_role_prefix, m.getRole()));
                matchingHospitals.add(h);
            }
        }

        options.add(getString(R.string.action_explore_network_directory));
        if (sessionManager.isSuperAdmin(currentAccount.getUid())) {
            options.add(getString(R.string.action_super_admin_governance));
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.dialog_hospital_switcher_title))
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    if (which < matchingHospitals.size()) {
                        Hospital selected = matchingHospitals.get(which);
                        sessionManager.setSelectedHospital(selected.getHospitalId(), selected.getHospitalCode(), selected.getHospitalName());
                        HospitalMembership activeMem = sessionManager.getActiveMembership(currentAccount.getUid(), selected.getHospitalId());
                        if (activeMem != null && activeMem.getRole() != null) {
                            currentAccount.setRole(activeMem.getRole());
                            currentAccount.setHospitalId(selected.getHospitalId());
                            currentAccount.setHospitalCode(selected.getHospitalCode());
                            sessionManager.saveUserSession(currentAccount);
                        }
                        setupTopHeader(currentAccount);
                        setupBottomNavigationForRole(currentAccount.getUserRoleEnum());
                        switchToTab(defaultTabId);
                        android.widget.Toast.makeText(this, getString(R.string.toast_switched_facility, selected.getHospitalName()), android.widget.Toast.LENGTH_SHORT).show();
                    } else {
                        Intent intent = new Intent(this, HospitalDiscoveryActivity.class);
                        startActivity(intent);
                    }
                })
                .setNegativeButton(getString(R.string.action_close), (dialog, which) -> dialog.dismiss())
                .show();
    }

    public void performLogout() {
        if (unreadConversationsListener != null) {
            unreadConversationsListener.remove();
            unreadConversationsListener = null;
        }
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
