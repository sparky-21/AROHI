package com.arohi.healthcare.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.arohi.healthcare.R;
import com.arohi.healthcare.activities.HospitalDiscoveryActivity;
import com.arohi.healthcare.activities.RolePortalActivity;
import com.arohi.healthcare.databinding.DialogChangePasswordBinding;
import com.arohi.healthcare.databinding.FragmentSettingsBinding;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.utils.LocaleHelper;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.ValidationUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Settings Fragment implementing Phase 10 Requirements:
 * 1. Appearance (Light, Dark, System Default) with immediate consistent persistence.
 * 2. Language Localization (English, বাংলা) with session preservation.
 * 3. Granular Notification Categories toggles.
 * 4. Account & Privacy Management (Edit profile, change password, manage hospitals, privacy options, permissions, logout, deletion request).
 */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SessionManager sessionManager;
    private UserAccount currentAccount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        currentAccount = sessionManager.getCurrentSessionAccount();

        initAppearanceSection();
        initLanguageSection();
        initNotificationSection();
        initAccountAndPrivacySection();
    }

    // =========================================================================
    // 1. APPEARANCE & THEME (Light / Dark / System Default)
    // =========================================================================
    private void initAppearanceSection() {
        int currentTheme = sessionManager.getThemeMode();
        if (currentTheme == SessionManager.THEME_MODE_LIGHT) {
            binding.rbThemeLight.setChecked(true);
        } else if (currentTheme == SessionManager.THEME_MODE_DARK) {
            binding.rbThemeDark.setChecked(true);
        } else {
            binding.rbThemeSystem.setChecked(true);
        }

        binding.rgThemeMode.setOnCheckedChangeListener((group, checkedId) -> {
            int newMode;
            if (checkedId == R.id.rbThemeLight) {
                newMode = SessionManager.THEME_MODE_LIGHT;
            } else if (checkedId == R.id.rbThemeDark) {
                newMode = SessionManager.THEME_MODE_DARK;
            } else {
                newMode = SessionManager.THEME_MODE_SYSTEM;
            }

            if (newMode != sessionManager.getThemeMode()) {
                sessionManager.setThemeMode(newMode);
            }
        });
    }

    // =========================================================================
    // 2. LANGUAGE LOCALIZATION (English / বাংলা)
    // =========================================================================
    private void initLanguageSection() {
        String currentLang = sessionManager.getAppLanguage();
        if (LocaleHelper.LANGUAGE_BANGLA.equalsIgnoreCase(currentLang)) {
            binding.rbLangBangla.setChecked(true);
        } else {
            binding.rbLangEnglish.setChecked(true);
        }

        binding.rgLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            String targetLang = (checkedId == R.id.rbLangBangla)
                    ? LocaleHelper.LANGUAGE_BANGLA
                    : LocaleHelper.LANGUAGE_ENGLISH;

            if (!targetLang.equalsIgnoreCase(sessionManager.getAppLanguage())) {
                sessionManager.setAppLanguage(targetLang);
                LocaleHelper.setLocale(requireContext(), targetLang);

                // Seamlessly restart activity preserving session state
                if (getActivity() != null) {
                    getActivity().recreate();
                }
            }
        });
    }

    // =========================================================================
    // 3. NOTIFICATION PREFERENCES
    // =========================================================================
    private void initNotificationSection() {
        binding.switchNotifAppointments.setChecked(sessionManager.isAppointmentsNotifEnabled());
        binding.switchNotifMessages.setChecked(sessionManager.isMessagesNotifEnabled());
        binding.switchNotifPrescriptions.setChecked(sessionManager.isPrescriptionsNotifEnabled());
        binding.switchNotifLabReports.setChecked(sessionManager.isLabReportsNotifEnabled());
        binding.switchNotifAnnouncements.setChecked(sessionManager.isAnnouncementsNotifEnabled());
        binding.switchNotifEmergency.setChecked(sessionManager.isEmergencyNotifEnabled());

        binding.switchNotifAppointments.setOnCheckedChangeListener((v, isChecked) ->
                sessionManager.setNotificationEnabled(SessionManager.KEY_NOTIF_APPOINTMENTS, isChecked));

        binding.switchNotifMessages.setOnCheckedChangeListener((v, isChecked) ->
                sessionManager.setNotificationEnabled(SessionManager.KEY_NOTIF_MESSAGES, isChecked));

        binding.switchNotifPrescriptions.setOnCheckedChangeListener((v, isChecked) ->
                sessionManager.setNotificationEnabled(SessionManager.KEY_NOTIF_PRESCRIPTIONS, isChecked));

        binding.switchNotifLabReports.setOnCheckedChangeListener((v, isChecked) ->
                sessionManager.setNotificationEnabled(SessionManager.KEY_NOTIF_LAB_REPORTS, isChecked));

        binding.switchNotifAnnouncements.setOnCheckedChangeListener((v, isChecked) ->
                sessionManager.setNotificationEnabled(SessionManager.KEY_NOTIF_ANNOUNCEMENTS, isChecked));

        binding.switchNotifEmergency.setOnCheckedChangeListener((v, isChecked) ->
                sessionManager.setNotificationEnabled(SessionManager.KEY_NOTIF_EMERGENCY, isChecked));
    }

    // =========================================================================
    // 4. ACCOUNT, PRIVACY & DANGER ZONE
    // =========================================================================
    private void initAccountAndPrivacySection() {
        binding.switchPrivacyEmergencyProfile.setChecked(sessionManager.isEmergencyProfileSharingEnabled());
        binding.switchPrivacyRecordSharing.setChecked(sessionManager.isMedicalRecordSharingEnabled());

        binding.switchPrivacyEmergencyProfile.setOnCheckedChangeListener((v, isChecked) ->
                sessionManager.setPrivacyOptionEnabled(SessionManager.KEY_PRIVACY_SHARE_EMERGENCY, isChecked));

        binding.switchPrivacyRecordSharing.setOnCheckedChangeListener((v, isChecked) ->
                sessionManager.setPrivacyOptionEnabled(SessionManager.KEY_PRIVACY_RECORD_SHARING, isChecked));

        binding.btnSettingsEditProfile.setOnClickListener(v -> {
            if (getActivity() != null) {
                // Navigate back to the Profile tab in portal
                getActivity().onBackPressed();
            }
        });

        binding.btnSettingsChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        binding.btnSettingsManageHospitals.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), HospitalDiscoveryActivity.class);
            startActivity(intent);
        });

        binding.btnSettingsLogout.setOnClickListener(v -> {
            if (getActivity() instanceof RolePortalActivity) {
                ((RolePortalActivity) getActivity()).performLogout();
            }
        });

        binding.btnSettingsDeleteAccount.setOnClickListener(v -> showAccountDeletionDialog());
    }

    private void showChangePasswordDialog() {
        if (currentAccount == null || currentAccount.getEmail() == null) {
            Toast.makeText(requireContext(), getString(R.string.error_no_active_session), Toast.LENGTH_SHORT).show();
            return;
        }

        DialogChangePasswordBinding dBinding = DialogChangePasswordBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dBinding.getRoot())
                .setCancelable(true)
                .create();

        dBinding.btnCancelPasswordChange.setOnClickListener(v -> dialog.dismiss());
        dBinding.btnSubmitPasswordChange.setOnClickListener(v -> {
            String currentPwd = dBinding.etCurrentPassword.getText() != null ? dBinding.etCurrentPassword.getText().toString() : "";
            String newPwd = dBinding.etNewPassword.getText() != null ? dBinding.etNewPassword.getText().toString() : "";
            String confirmPwd = dBinding.etConfirmNewPassword.getText() != null ? dBinding.etConfirmNewPassword.getText().toString() : "";

            dBinding.tilCurrentPassword.setError(null);
            dBinding.tilNewPassword.setError(null);
            dBinding.tilConfirmNewPassword.setError(null);

            if (!ValidationUtils.isValidPassword(newPwd)) {
                dBinding.tilNewPassword.setError(getString(R.string.error_password_policy));
                return;
            }

            if (!newPwd.equals(confirmPwd)) {
                dBinding.tilConfirmNewPassword.setError(getString(R.string.error_password_mismatch));
                return;
            }

            boolean success = sessionManager.changeLocalPassword(currentAccount.getEmail(), currentPwd, newPwd);
            if (success) {
                dialog.dismiss();
                Toast.makeText(requireContext(), getString(R.string.password_changed_success), Toast.LENGTH_SHORT).show();
            } else {
                dBinding.tilCurrentPassword.setError(getString(R.string.error_current_password_invalid));
            }
        });

        dialog.show();
    }

    private void showAccountDeletionDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialog_delete_account_title))
                .setMessage(getString(R.string.dialog_delete_account_msg))
                .setPositiveButton(getString(R.string.action_confirm_deletion), (dialog, which) -> {
                    if (currentAccount != null) {
                        sessionManager.requestAccountDeletion(currentAccount.getUid(), "User requested account removal in Settings");
                        Toast.makeText(requireContext(), getString(R.string.toast_account_deletion_requested), Toast.LENGTH_LONG).show();
                        if (getActivity() instanceof RolePortalActivity) {
                            ((RolePortalActivity) getActivity()).performLogout();
                        }
                    }
                })
                .setNegativeButton(getString(R.string.action_back), (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
