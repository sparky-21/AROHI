package com.arohi.healthcare.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.adapters.HospitalAdapter;
import com.arohi.healthcare.databinding.ActivityHospitalDiscoveryBinding;
import com.arohi.healthcare.databinding.DialogHospitalDetailsBinding;
import com.arohi.healthcare.databinding.DialogManageHospitalBinding;
import com.arohi.healthcare.models.Hospital;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.HospitalNetworkRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pre-Login Public Hospital Discovery Screen.
 * Allows users to discover hospitals, lookup by unique code, explore services,
 * view emergency hotlines without logging in, and select an active hospital before authentication.
 */
public class HospitalDiscoveryActivity extends BaseActivity implements HospitalAdapter.OnHospitalActionListener {

    private ActivityHospitalDiscoveryBinding binding;
    private HospitalNetworkRepository repository;
    private HospitalAdapter adapter;
    private String currentCategoryFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHospitalDiscoveryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new HospitalNetworkRepository(this);

        initViews();
        setupFilters();
        setupSearchAndLookup();
        loadHospitals();
    }

    private void initViews() {
        adapter = new HospitalAdapter(this);
        binding.rvHospitals.setLayoutManager(new LinearLayoutManager(this));
        binding.rvHospitals.setAdapter(adapter);

        binding.btnDirectSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        });

        binding.btnRegisterHospitalApp.setOnClickListener(v -> showRegisterHospitalDialog());

        binding.btnSuperAdminAccess.setOnClickListener(v -> showSuperAdminAuthDialog());
    }

    private void setupFilters() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentCategoryFilter = "ALL";
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chipVerified) {
                    currentCategoryFilter = "VERIFIED";
                } else if (id == R.id.chipEmergency) {
                    currentCategoryFilter = "EMERGENCY";
                } else if (id == R.id.chipDhaka) {
                    currentCategoryFilter = "DHAKA";
                } else if (id == R.id.chipChittagong) {
                    currentCategoryFilter = "CHITTAGONG";
                } else {
                    currentCategoryFilter = "ALL";
                }
            }
            performSearch(binding.etSearchHospitals.getText() != null ? binding.etSearchHospitals.getText().toString() : "");
        });
    }

    private void setupSearchAndLookup() {
        binding.etSearchHospitals.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnLookupCode.setOnClickListener(v -> {
            String code = binding.etHospitalCodeInput.getText() != null ? binding.etHospitalCodeInput.getText().toString().trim() : "";
            if (code.isEmpty()) {
                Toast.makeText(this, "Please enter a hospital code (e.g. AROHI-HQ)", Toast.LENGTH_SHORT).show();
                return;
            }

            repository.findHospitalByCode(code, new AuthCallback<Hospital>() {
                @Override
                public void onSuccess(Hospital hospital) {
                    onExploreHospital(hospital);
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(HospitalDiscoveryActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void loadHospitals() {
        repository.getHospitals(new AuthCallback<List<Hospital>>() {
            @Override
            public void onSuccess(List<Hospital> hospitals) {
                renderHospitalList(hospitals);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(HospitalDiscoveryActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performSearch(String query) {
        repository.searchHospitals(query, currentCategoryFilter, new AuthCallback<List<Hospital>>() {
            @Override
            public void onSuccess(List<Hospital> hospitals) {
                renderHospitalList(hospitals);
            }

            @Override
            public void onError(String errorMessage) {
                // Silently fallback
            }
        });
    }

    private void renderHospitalList(List<Hospital> hospitals) {
        if (hospitals == null || hospitals.isEmpty()) {
            binding.rvHospitals.setVisibility(View.GONE);
            binding.layoutEmptyHospitals.setVisibility(View.VISIBLE);
            binding.tvDiscoveryResultsCount.setText(getString(R.string.discovery_available_count, 0));
        } else {
            binding.rvHospitals.setVisibility(View.VISIBLE);
            binding.layoutEmptyHospitals.setVisibility(View.GONE);
            binding.tvDiscoveryResultsCount.setText(getString(R.string.discovery_available_count, hospitals.size()));
            adapter.submitList(hospitals);
        }
    }

    @Override
    public void onExploreHospital(@NonNull Hospital hospital) {
        DialogHospitalDetailsBinding dialogBinding = DialogHospitalDetailsBinding.inflate(LayoutInflater.from(this));

        dialogBinding.dialogHospitalName.setText(hospital.getHospitalName());
        dialogBinding.dialogHospitalCity.setText(hospital.getCity() != null ? hospital.getCity() : "Bangladesh");
        dialogBinding.dialogHospitalCode.setText(hospital.getHospitalCode());
        dialogBinding.dialogBedCapacity.setText(getString(R.string.label_operational_beds, hospital.getTotalBeds()));
        dialogBinding.dialogDescription.setText(hospital.getDescription() != null ? hospital.getDescription() : "Comprehensive healthcare services within the AROHI Network.");
        dialogBinding.dialogAddress.setText(hospital.getAddress() != null ? hospital.getAddress() : "Address on file.");

        if (hospital.isVerified()) {
            dialogBinding.dialogVerificationBadge.setText(getString(R.string.badge_accredited_institution));
            dialogBinding.dialogVerificationBadge.setVisibility(View.VISIBLE);
        } else {
            dialogBinding.dialogVerificationBadge.setText(getString(R.string.badge_pending_accreditation));
            dialogBinding.dialogVerificationBadge.setVisibility(View.VISIBLE);
        }

        if (hospital.getEmergencyHotline() != null && !hospital.getEmergencyHotline().isEmpty()) {
            dialogBinding.dialogHotline.setText(hospital.getEmergencyHotline());
            dialogBinding.btnCallHotline.setOnClickListener(v -> {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + hospital.getEmergencyHotline().replaceAll("[^0-9+]", "")));
                startActivity(dialIntent);
            });
        } else {
            dialogBinding.dialogHotline.setText(getString(R.string.label_hotline_missing));
            dialogBinding.btnCallHotline.setEnabled(false);
        }

        if (hospital.getSupportedDepartments() != null && !hospital.getSupportedDepartments().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String dept : hospital.getSupportedDepartments()) {
                sb.append("• ").append(dept).append("\n");
            }
            dialogBinding.dialogDepartmentsList.setText(sb.toString().trim());
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.getRoot())
                .setCancelable(true)
                .create();

        dialogBinding.dialogBtnClose.setOnClickListener(v -> dialog.dismiss());
        dialogBinding.dialogBtnSelectAndProceed.setOnClickListener(v -> {
            dialog.dismiss();
            onSelectHospital(hospital);
        });

        dialog.show();
    }

    @Override
    public void onSelectHospital(@NonNull Hospital hospital) {
        repository.selectHospital(hospital);
        Toast.makeText(this, "Selected: " + hospital.getHospitalName() + " (" + hospital.getHospitalCode() + ")", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, RoleSelectionActivity.class);
        startActivity(intent);
    }

    private void showRegisterHospitalDialog() {
        DialogManageHospitalBinding dBinding = DialogManageHospitalBinding.inflate(LayoutInflater.from(this));
        dBinding.manageHospitalSubtitle.setText("Institutional Onboarding Application");
        dBinding.switchVerification.setChecked(false);
        dBinding.switchVerification.setEnabled(false); // Only Super Admin can verify
        dBinding.switchActivation.setChecked(false);
        dBinding.switchActivation.setEnabled(false);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dBinding.getRoot())
                .setCancelable(true)
                .create();

        dBinding.btnManageCancel.setOnClickListener(v -> dialog.dismiss());
        dBinding.btnManageSave.setOnClickListener(v -> {
            String name = dBinding.etManageHospitalName.getText() != null ? dBinding.etManageHospitalName.getText().toString().trim() : "";
            String code = dBinding.etManageHospitalCode.getText() != null ? dBinding.etManageHospitalCode.getText().toString().trim().toUpperCase() : "";
            String city = dBinding.etManageCity.getText() != null ? dBinding.etManageCity.getText().toString().trim() : "";
            String bedsStr = dBinding.etManageTotalBeds.getText() != null ? dBinding.etManageTotalBeds.getText().toString().trim() : "100";

            if (name.isEmpty() || code.isEmpty()) {
                Toast.makeText(this, "Hospital name and code are required.", Toast.LENGTH_SHORT).show();
                return;
            }

            int beds = 100;
            try {
                beds = Integer.parseInt(bedsStr);
            } catch (Exception ignored) {}

            List<String> defaultDepts = Arrays.asList("Emergency", "General Medicine", "Surgery");
            Hospital newHosp = new Hospital(
                    "hosp_" + System.currentTimeMillis(),
                    code,
                    name,
                    city + ", Bangladesh",
                    city,
                    "+880 1700-000000",
                    "admin@" + code.toLowerCase() + ".org",
                    "+880 1700-000000",
                    Hospital.STATUS_PENDING,
                    beds,
                    defaultDepts,
                    false,
                    "Institutional applicant awaiting platform Super Admin accreditation."
            );

            repository.registerHospital(newHosp, new AuthCallback<Hospital>() {
                @Override
                public void onSuccess(Hospital result) {
                    dialog.dismiss();
                    Toast.makeText(HospitalDiscoveryActivity.this, "Application submitted for " + result.getHospitalName() + ". Awaiting Super Admin verification.", Toast.LENGTH_LONG).show();
                    loadHospitals();
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(HospitalDiscoveryActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog.show();
    }

    private void showSuperAdminAuthDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter Platform Super Admin Master Key");
        input.setSingleLine(true);
        input.setPadding(40, 30, 40, 30);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Platform Super Admin Gateway")
                .setMessage("Enter the authorization master key to review, verify and activate hospitals across the AROHI Network.")
                .setView(input)
                .setPositiveButton("Authenticate", (dialog, which) -> {
                    String key = input.getText().toString().trim();
                    if ("AROHI-SUPER-2026".equals(key) || "superadmin".equalsIgnoreCase(key)) {
                        dialog.dismiss();
                        showSuperAdminHospitalSelector();
                    } else {
                        Toast.makeText(this, "Unauthorized Super Admin key.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showSuperAdminHospitalSelector() {
        List<Hospital> all = repository.getSessionManager().getRegisteredHospitals();
        String[] hospitalNames = new String[all.size()];
        for (int i = 0; i < all.size(); i++) {
            Hospital h = all.get(i);
            hospitalNames[i] = h.getHospitalName() + " [" + h.getHospitalCode() + "] " + (h.isVerified() ? "✓" : "⏳");
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Super Admin Governance: Select Hospital")
                .setItems(hospitalNames, (dialog, which) -> {
                    Hospital selected = all.get(which);
                    openSuperAdminHospitalEditor(selected);
                })
                .setNegativeButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void openSuperAdminHospitalEditor(Hospital hospital) {
        DialogManageHospitalBinding dBinding = DialogManageHospitalBinding.inflate(LayoutInflater.from(this));
        dBinding.etManageHospitalName.setText(hospital.getHospitalName());
        dBinding.etManageHospitalCode.setText(hospital.getHospitalCode());
        dBinding.etManageCity.setText(hospital.getCity());
        dBinding.etManageTotalBeds.setText(String.valueOf(hospital.getTotalBeds()));
        dBinding.switchVerification.setChecked(hospital.isVerified());
        dBinding.switchActivation.setChecked(hospital.isActive());

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dBinding.getRoot())
                .create();

        dBinding.btnManageCancel.setOnClickListener(v -> dialog.dismiss());
        dBinding.btnManageSave.setOnClickListener(v -> {
            String newCode = dBinding.etManageHospitalCode.getText() != null ? dBinding.etManageHospitalCode.getText().toString().trim().toUpperCase() : hospital.getHospitalCode();
            boolean verified = dBinding.switchVerification.isChecked();
            boolean active = dBinding.switchActivation.isChecked();

            hospital.setHospitalName(dBinding.etManageHospitalName.getText().toString().trim());
            hospital.setHospitalCode(newCode);
            hospital.setCity(dBinding.etManageCity.getText().toString().trim());
            try {
                hospital.setTotalBeds(Integer.parseInt(dBinding.etManageTotalBeds.getText().toString().trim()));
            } catch (Exception ignored) {}
            hospital.setVerified(verified);
            hospital.setStatus(active ? Hospital.STATUS_ACTIVE : Hospital.STATUS_INACTIVE);
            hospital.setUpdatedAt(System.currentTimeMillis());

            repository.getSessionManager().saveHospital(hospital);
            dialog.dismiss();
            Toast.makeText(this, "Updated " + hospital.getHospitalName() + " (Verified: " + verified + ", Active: " + active + ")", Toast.LENGTH_SHORT).show();
            loadHospitals();
        });

        dialog.show();
    }
}
