package com.arohi.healthcare.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.adapters.HospitalBillAdapter;
import com.arohi.healthcare.databinding.ActivityHospitalBillingDashboardBinding;
import com.arohi.healthcare.models.HospitalBill;
import com.arohi.healthcare.models.HospitalFinancialReport;
import com.arohi.healthcare.models.PaymentStatus;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.BillingRepository;
import com.arohi.healthcare.utils.SessionManager;

import java.util.List;
import java.util.Locale;

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
        if (PaymentStatus.SUCCESSFUL.name().equalsIgnoreCase(bill.getStatus())) {
            Intent intent = DigitalReceiptActivity.newIntent(this, bill.getBillId(), hospitalId);
            startActivity(intent);
        } else {
            Intent intent = BillingCheckoutActivity.newIntent(this, bill.getBillId(), hospitalId);
            startActivity(intent);
        }
    }
}
