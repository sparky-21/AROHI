package com.arohi.healthcare.activities;

import android.content.Intent;
import android.os.Bundle;
import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ActivityRoleSelectionBinding;
import com.arohi.healthcare.utils.SessionManager;

/**
 * 3. Role Selection Screen
 * Directs users to the appropriate role-specific registration or verification flow
 * (Patient, Doctor, Nurse, Authorized Admin Setup) or to the Login screen.
 * Displays active Hospital tenant context and allows switching hospitals.
 */
public class RoleSelectionActivity extends BaseActivity {

    private ActivityRoleSelectionBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRoleSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        updateActiveHospitalBanner();
        setupRoleListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateActiveHospitalBanner();
    }

    private void updateActiveHospitalBanner() {
        String hospName = sessionManager.getSelectedHospitalName();
        String hospCode = sessionManager.getSelectedHospitalCode();
        binding.tvActiveHospitalName.setText(hospName != null ? hospName : SessionManager.DEFAULT_HOSPITAL_NAME);
        binding.tvActiveHospitalCode.setText(getString(R.string.label_hospital_code_verified,
                (hospCode != null ? hospCode : SessionManager.DEFAULT_HOSPITAL_CODE)));
    }

    private void setupRoleListeners() {
        binding.btnChangeHospital.setOnClickListener(v -> {
            Intent intent = new Intent(this, HospitalDiscoveryActivity.class);
            startActivity(intent);
        });

        binding.cardRolePatient.setOnClickListener(v ->
                startActivity(new Intent(this, PatientRegistrationActivity.class))
        );

        binding.cardRoleDoctor.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorRegistrationActivity.class))
        );

        binding.cardRoleNurse.setOnClickListener(v ->
                startActivity(new Intent(this, NurseRegistrationActivity.class))
        );

        binding.cardRoleAdmin.setOnClickListener(v ->
                startActivity(new Intent(this, AdminSetupActivity.class))
        );

        binding.btnGoToLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class))
        );
    }
}
