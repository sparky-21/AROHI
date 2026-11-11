package com.arohi.healthcare.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.adapters.HospitalBillAdapter;
import com.arohi.healthcare.databinding.ActivityHospitalBillingDashboardBinding;
import com.arohi.healthcare.databinding.DialogCreateBillBinding;
import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.models.BillCategory;
import com.arohi.healthcare.models.BillItem;
import com.arohi.healthcare.models.HospitalBill;
import com.arohi.healthcare.models.HospitalFinancialReport;
import com.arohi.healthcare.models.PaymentStatus;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.BillingRepository;
import com.arohi.healthcare.utils.SessionManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Phase 13 Hospital Authority Billing Dashboard Activity.
 * Provides hospital administrators with revenue metrics, collection analytics,
 * itemized ledger view, and payment status filtering.
 */
public class HospitalBillingDashboardActivity extends BaseActivity {

    public static final String EXTRA_HOSPITAL_ID = "extra_hospital_id";

    private ActivityHospitalBillingDashboardBinding binding;
    private BillingRepository billingRepository;
    private SessionManager sessionManager;
    private HospitalBillAdapter billAdapter;

    private String hospitalId;
    private String currentStatusFilter = "ALL";

    public static Intent newIntent(Context context, String hospitalId) {
        Intent intent = new Intent(context, HospitalBillingDashboardActivity.class);
        intent.putExtra(EXTRA_HOSPITAL_ID, hospitalId);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHospitalBillingDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        billingRepository = new BillingRepository(this);

        hospitalId = getIntent().getStringExtra(EXTRA_HOSPITAL_ID);
        if (hospitalId == null || hospitalId.trim().isEmpty()) {
            hospitalId = sessionManager.getSelectedHospitalId();
        }

        setupUI();
        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void setupUI() {
        binding.tvBillingHospitalSubtitle.setText(sessionManager.getSelectedHospitalName()
                + " • Multi-Tenant Financial Ledger");

        binding.btnBillingDashBack.setOnClickListener(v -> finish());
        binding.btnBillingRefresh.setOnClickListener(v -> loadDashboardData());
        binding.btnCreateBill.setOnClickListener(v -> showCreateBillDialog());

        binding.btnFilterAllBills.setOnClickListener(v -> applyFilter("ALL"));
        binding.btnFilterPaidBills.setOnClickListener(v -> applyFilter(PaymentStatus.SUCCESSFUL.name()));
        binding.btnFilterPendingBills.setOnClickListener(v -> applyFilter(PaymentStatus.PENDING.name()));
        binding.btnFilterRefunds.setOnClickListener(v -> applyFilter(PaymentStatus.REFUNDED.name()));
    }

    private void setupRecyclerView() {
        billAdapter = new HospitalBillAdapter(this::onBillClicked);
        binding.rvHospitalBillingList.setLayoutManager(new LinearLayoutManager(this));
        binding.rvHospitalBillingList.setAdapter(billAdapter);
    }

    private void applyFilter(String status) {
        this.currentStatusFilter = status;
        updateFilterButtonStyles();
        loadBillsList();
    }

    private void updateFilterButtonStyles() {
        int activeTint = ContextCompat.getColor(this, R.color.pale_mint);
        int defaultTint = ContextCompat.getColor(this, R.color.surface_card);

        binding.btnFilterAllBills.setBackgroundTintList(
                ContextCompat.getColorStateList(this, "ALL".equals(currentStatusFilter) ? R.color.pale_mint : R.color.surface_card));
        binding.btnFilterPaidBills.setBackgroundTintList(
                ContextCompat.getColorStateList(this, PaymentStatus.SUCCESSFUL.name().equals(currentStatusFilter) ? R.color.pale_mint : R.color.surface_card));
        binding.btnFilterPendingBills.setBackgroundTintList(
                ContextCompat.getColorStateList(this, PaymentStatus.PENDING.name().equals(currentStatusFilter) ? R.color.pale_mint : R.color.surface_card));
        binding.btnFilterRefunds.setBackgroundTintList(
                ContextCompat.getColorStateList(this, PaymentStatus.REFUNDED.name().equals(currentStatusFilter) ? R.color.pale_mint : R.color.surface_card));
    }

    private void loadDashboardData() {
        loadFinancialMetrics();
        loadBillsList();
    }

    private void loadFinancialMetrics() {
        billingRepository.getHospitalFinancialMetrics(hospitalId, new AuthCallback<HospitalFinancialReport>() {
            @Override
            public void onSuccess(HospitalFinancialReport report) {
                binding.tvDashTotalRevenue.setText(String.format(Locale.US, "৳ %.2f", report.getTotalRevenueBdt()));
                binding.tvDashPendingRevenue.setText(String.format(Locale.US, "৳ %.2f", report.getPendingRevenueBdt()));
                binding.tvDashTodayRevenue.setText(String.format(Locale.US, "৳ %.2f", report.getTodayRevenueBdt()));
                binding.tvDashPaidCount.setText(report.getPaidBillsCount() + " / " + report.getTotalBillsCount() + " Bills");
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(HospitalBillingDashboardActivity.this, "Metrics error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadBillsList() {
        billingRepository.getHospitalBills(hospitalId, "ALL", currentStatusFilter, new AuthCallback<List<HospitalBill>>() {
            @Override
            public void onSuccess(List<HospitalBill> bills) {
                if (bills == null || bills.isEmpty()) {
                    binding.layoutEmptyBilling.setVisibility(View.VISIBLE);
                    binding.rvHospitalBillingList.setVisibility(View.GONE);
                } else {
                    binding.layoutEmptyBilling.setVisibility(View.GONE);
                    binding.rvHospitalBillingList.setVisibility(View.VISIBLE);
                    billAdapter.submitList(bills);
                }
            }

            @Override
            public void onError(String errorMessage) {
                binding.layoutEmptyBilling.setVisibility(View.VISIBLE);
            }
        });
    }

    private void onBillClicked(@NonNull HospitalBill bill) {
        // Admin can only view receipts for paid bills — no payment/checkout access
        if (PaymentStatus.SUCCESSFUL.name().equalsIgnoreCase(bill.getStatus())) {
            Intent intent = DigitalReceiptActivity.newIntent(this, bill.getBillId(), hospitalId);
            startActivity(intent);
        } else {
            // Admin sees bill details but cannot pay — show info toast
            Toast.makeText(this,
                    "Bill " + bill.getBillNumber() + " • Status: " + bill.getStatus()
                            + " • Patient: " + (bill.getPatientName() != null ? bill.getPatientName() : "N/A")
                            + " • ৳ " + String.format(Locale.US, "%.2f", bill.getTotalAmountBdt()),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showCreateBillDialog() {
        DialogCreateBillBinding dBinding = DialogCreateBillBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dBinding.getRoot())
                .create();

        dBinding.btnCancelCreateBill.setOnClickListener(v -> dialog.dismiss());
        dBinding.btnSubmitCreateBill.setOnClickListener(v -> {
            String patientUid = dBinding.etBillPatientUid.getText() != null
                    ? dBinding.etBillPatientUid.getText().toString().trim() : "";
            String patientName = dBinding.etBillPatientName.getText() != null
                    ? dBinding.etBillPatientName.getText().toString().trim() : "";

            if (patientUid.isEmpty()) {
                dBinding.tilBillPatientUid.setError("Patient UID or AROHI ID required");
                return;
            }
            dBinding.tilBillPatientUid.setError(null);

            if (patientName.isEmpty()) {
                patientName = "Patient";
            }

            BillCategory category = BillCategory.HOSPITAL_SERVICE;
            if (dBinding.rbCatConsultation.isChecked()) {
                category = BillCategory.CONSULTATION;
            } else if (dBinding.rbCatPharmacy.isChecked()) {
                category = BillCategory.PHARMACY;
            } else if (dBinding.rbCatLabTest.isChecked()) {
                category = BillCategory.LAB_TEST;
            } else if (dBinding.rbCatOperation.isChecked()) {
                category = BillCategory.OPERATION;
            } else if (dBinding.rbCatHospitalService.isChecked()) {
                category = BillCategory.HOSPITAL_SERVICE;
            }

            // Item 1
            String desc1 = dBinding.etBillItem1Desc.getText() != null
                    ? dBinding.etBillItem1Desc.getText().toString().trim() : "";
            String qty1Str = dBinding.etBillItem1Qty.getText() != null
                    ? dBinding.etBillItem1Qty.getText().toString().trim() : "1";
            String price1Str = dBinding.etBillItem1Price.getText() != null
                    ? dBinding.etBillItem1Price.getText().toString().trim() : "";

            if (desc1.isEmpty() || price1Str.isEmpty()) {
                Toast.makeText(this, "Please enter at least one item description and price.", Toast.LENGTH_SHORT).show();
                return;
            }

            double price1;
            int qty1;
            try {
                price1 = Double.parseDouble(price1Str);
                qty1 = Integer.parseInt(qty1Str);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid price or quantity for Item 1.", Toast.LENGTH_SHORT).show();
                return;
            }

            String code = sessionManager.getSelectedHospitalCode() != null ? sessionManager.getSelectedHospitalCode() : "HOSP";
            String billId = "bill_" + UUID.randomUUID().toString().substring(0, 8);

            // Determine if admin entered an AROHI ID or a Firebase UID
            final String enteredId = patientUid;
            final String finalPatientName = patientName;
            final BillCategory finalCategory = category;
            final String finalDesc1 = desc1;
            final int finalQty1 = qty1;
            final double finalPrice1 = price1;

            // Collect optional items before async call
            final String fDesc2 = dBinding.etBillItem2Desc.getText() != null
                    ? dBinding.etBillItem2Desc.getText().toString().trim() : "";
            final String fPrice2Str = dBinding.etBillItem2Price.getText() != null
                    ? dBinding.etBillItem2Price.getText().toString().trim() : "";
            final String fQty2Str = dBinding.etBillItem2Qty.getText() != null
                    ? dBinding.etBillItem2Qty.getText().toString().trim() : "1";
            final String fDesc3 = dBinding.etBillItem3Desc.getText() != null
                    ? dBinding.etBillItem3Desc.getText().toString().trim() : "";
            final String fPrice3Str = dBinding.etBillItem3Price.getText() != null
                    ? dBinding.etBillItem3Price.getText().toString().trim() : "";
            final String fQty3Str = dBinding.etBillItem3Qty.getText() != null
                    ? dBinding.etBillItem3Qty.getText().toString().trim() : "1";
            final String fNotes = dBinding.etBillNotes.getText() != null
                    ? dBinding.etBillNotes.getText().toString().trim() : "";

            // If the entered ID looks like an AROHI ID, resolve the Firebase UID
            if (enteredId.toUpperCase(Locale.US).startsWith("AROHI-")) {
                FirebaseFirestore.getInstance().collection(FirestoreCollections.USERS)
                        .whereEqualTo("arohiId", enteredId)
                        .limit(1)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            String resolvedUid = enteredId; // fallback to entered ID
                            String resolvedArohiId = enteredId;
                            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                resolvedUid = querySnapshot.getDocuments().get(0).getId();
                            }
                            buildAndSubmitBill(dialog, billId, hospitalId, code,
                                    resolvedUid, finalPatientName, resolvedArohiId, finalCategory,
                                    finalDesc1, finalQty1, finalPrice1,
                                    fDesc2, fPrice2Str, fQty2Str,
                                    fDesc3, fPrice3Str, fQty3Str, fNotes);
                        })
                        .addOnFailureListener(e -> {
                            // Firestore lookup failed; proceed with entered ID
                            buildAndSubmitBill(dialog, billId, hospitalId, code,
                                    enteredId, finalPatientName, enteredId, finalCategory,
                                    finalDesc1, finalQty1, finalPrice1,
                                    fDesc2, fPrice2Str, fQty2Str,
                                    fDesc3, fPrice3Str, fQty3Str, fNotes);
                        });
            } else {
                // Admin entered a Firebase UID directly — look up their AROHI ID
                String resolvedArohiId = enteredId; // default same
                buildAndSubmitBill(dialog, billId, hospitalId, code,
                        enteredId, finalPatientName, resolvedArohiId, finalCategory,
                        finalDesc1, finalQty1, finalPrice1,
                        fDesc2, fPrice2Str, fQty2Str,
                        fDesc3, fPrice3Str, fQty3Str, fNotes);
            }
        });

        dialog.show();
    }

    /**
     * Builds a HospitalBill with resolved patient identifiers and submits it.
     */
    private void buildAndSubmitBill(AlertDialog dialog, String billId, String hospitalId,
                                     String code, String firebaseUid, String patientName,
                                     String arohiId, BillCategory category,
                                     String desc1, int qty1, double price1,
                                     String desc2, String price2Str, String qty2Str,
                                     String desc3, String price3Str, String qty3Str,
                                     String notes) {

        HospitalBill bill = new HospitalBill(
                billId, "", hospitalId, code,
                firebaseUid, patientName, arohiId, category
        );
        bill.setStatus(PaymentStatus.PENDING.name());
        bill.addItem(new BillItem("itm_" + UUID.randomUUID().toString().substring(0, 6), desc1, qty1, price1));

        // Item 2 (Optional)
        if (!desc2.isEmpty() && !price2Str.isEmpty()) {
            try {
                double price2 = Double.parseDouble(price2Str);
                int qty2 = Integer.parseInt(qty2Str);
                bill.addItem(new BillItem("itm_" + UUID.randomUUID().toString().substring(0, 6), desc2, qty2, price2));
            } catch (Exception ignored) {}
        }

        // Item 3 (Optional)
        if (!desc3.isEmpty() && !price3Str.isEmpty()) {
            try {
                double price3 = Double.parseDouble(price3Str);
                int qty3 = Integer.parseInt(qty3Str);
                bill.addItem(new BillItem("itm_" + UUID.randomUUID().toString().substring(0, 6), desc3, qty3, price3));
            } catch (Exception ignored) {}
        }

        if (!notes.isEmpty()) {
            bill.setNotes(notes);
        }

        billingRepository.createBill(bill, new AuthCallback<HospitalBill>() {
            @Override
            public void onSuccess(HospitalBill created) {
                dialog.dismiss();
                Toast.makeText(HospitalBillingDashboardActivity.this,
                        "Bill " + created.getBillNumber() + " created successfully!", Toast.LENGTH_LONG).show();
                loadDashboardData();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(HospitalBillingDashboardActivity.this,
                        "Failed to create bill: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
