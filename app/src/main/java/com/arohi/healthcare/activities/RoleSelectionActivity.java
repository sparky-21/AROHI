package com.arohi.healthcare.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.arohi.healthcare.databinding.ActivityRoleSelectionBinding;

/**
 * 3. Role Selection Screen
 * Directs users to the appropriate role-specific registration or verification flow
 * (Patient, Doctor, Nurse, Authorized Admin Setup) or to the Login screen.
 */
public class RoleSelectionActivity extends AppCompatActivity {

    private ActivityRoleSelectionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRoleSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRoleListeners();
    }

    private void setupRoleListeners() {
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
