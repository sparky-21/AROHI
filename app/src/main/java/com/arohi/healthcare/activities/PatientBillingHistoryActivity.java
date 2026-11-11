package com.arohi.healthcare.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.adapters.HospitalBillAdapter;
import com.arohi.healthcare.databinding.ActivityPatientBillingHistoryBinding;
import com.arohi.healthcare.models.HospitalBill;
import com.arohi.healthcare.models.PaymentStatus;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.BillingRepository;
import com.arohi.healthcare.utils.SessionManager;

import java.util.List;

/**
 * Phase 13 Patient Billing History Activity.
 * Allows patients to view their medical bills, pharmacy charges, consultation invoices,
 * make payments, and retrieve official digital receipts.
 */
public class PatientBillingHistoryActivity extends BaseActivity {

    private ActivityPatientBillingHistoryBinding binding;
    private BillingRepository billingRepository;
    private SessionManager sessionManager;
    private HospitalBillAdapter adapter;

    public static Intent newIntent(Context context) {
        return new Intent(context, PatientBillingHistoryActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPatientBillingHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        billingRepository = new BillingRepository(this);

        setupUI();
        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBills();
    }

    private void setupUI() {
        UserAccount current = sessionManager.getCurrentSessionAccount();
        if (current != null) {
            binding.tvPatientBillingSubtitle.setText(current.getFullName() + " • " + (current.getArohiId() != null ? current.getArohiId() : "Patient"));
        }

        binding.btnPatientBillingBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new HospitalBillAdapter(this::onBillClicked);
        binding.rvPatientBillsList.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPatientBillsList.setAdapter(adapter);
    }

    private void loadBills() {
        UserAccount current = sessionManager.getCurrentSessionAccount();
        String uid = current != null ? current.getUid() : "current_user";

        billingRepository.getPatientBills(uid, new AuthCallback<List<HospitalBill>>() {
            @Override
            public void onSuccess(List<HospitalBill> bills) {
                if (bills == null || bills.isEmpty()) {
                    binding.layoutEmptyPatientBills.setVisibility(View.VISIBLE);
                    binding.rvPatientBillsList.setVisibility(View.GONE);
                } else {
                    binding.layoutEmptyPatientBills.setVisibility(View.GONE);
                    binding.rvPatientBillsList.setVisibility(View.VISIBLE);
                    adapter.submitList(bills);
                }
            }

            @Override
            public void onError(String errorMessage) {
                binding.layoutEmptyPatientBills.setVisibility(View.VISIBLE);
            }
        });
    }

    private void onBillClicked(@NonNull HospitalBill bill) {
        String hospId = bill.getHospitalId() != null ? bill.getHospitalId() : sessionManager.getSelectedHospitalId();
        if (PaymentStatus.SUCCESSFUL.name().equalsIgnoreCase(bill.getStatus())) {
            Intent intent = DigitalReceiptActivity.newIntent(this, bill.getBillId(), hospId);
            startActivity(intent);
        } else {
            Intent intent = BillingCheckoutActivity.newIntent(this, bill.getBillId(), hospId);
            startActivity(intent);
        }
    }
}
