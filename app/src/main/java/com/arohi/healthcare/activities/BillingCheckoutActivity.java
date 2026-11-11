package com.arohi.healthcare.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.adapters.BillItemAdapter;
import com.arohi.healthcare.databinding.ActivityBillingCheckoutBinding;
import com.arohi.healthcare.models.BillCategory;
import com.arohi.healthcare.models.HospitalBill;
import com.arohi.healthcare.models.PaymentGatewayType;
import com.arohi.healthcare.models.PaymentReceipt;
import com.arohi.healthcare.models.PaymentStatus;
import com.arohi.healthcare.models.PaymentTransaction;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.BillingRepository;
import com.arohi.healthcare.utils.SessionManager;

import java.util.Locale;

/**
 * Phase 13 Billing Checkout Activity.
 * Supports multi-gateway selection (bKash, Nagad, Card/SSLCommerz, Cash Counter),
 * itemized charge breakdown, 5% statutory VAT calculation, sandbox test mode,
 * and trusted backend payment verification.
 */
public class BillingCheckoutActivity extends BaseActivity {

    public static final String EXTRA_BILL_ID = "extra_bill_id";
    public static final String EXTRA_HOSPITAL_ID = "extra_hospital_id";

    private ActivityBillingCheckoutBinding binding;
    private BillingRepository billingRepository;
    private SessionManager sessionManager;
    private BillItemAdapter itemAdapter;

    private String billId;
    private String hospitalId;
    private HospitalBill currentBill;
    private PaymentGatewayType selectedGateway = PaymentGatewayType.BKASH;

    public static Intent newIntent(Context context, String billId, String hospitalId) {
        Intent intent = new Intent(context, BillingCheckoutActivity.class);
        intent.putExtra(EXTRA_BILL_ID, billId);
        intent.putExtra(EXTRA_HOSPITAL_ID, hospitalId);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBillingCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        billingRepository = new BillingRepository(this);

        billId = getIntent().getStringExtra(EXTRA_BILL_ID);
        hospitalId = getIntent().getStringExtra(EXTRA_HOSPITAL_ID);

        if (hospitalId == null || hospitalId.trim().isEmpty()) {
            hospitalId = sessionManager.getSelectedHospitalId();
        }

        if (billId == null || hospitalId == null) {
            Toast.makeText(this, "Invalid bill reference.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        loadBillDetails();
    }

    private void setupUI() {
        binding.tvCheckoutHospitalSubtitle.setText(sessionManager.getSelectedHospitalName()
                + " • " + (sessionManager.getSelectedHospitalCode() != null ? sessionManager.getSelectedHospitalCode() : "Hospital"));

        binding.btnCheckoutBack.setOnClickListener(v -> finish());

        // Setup Item Recycler
        itemAdapter = new BillItemAdapter();
        binding.rvCheckoutBillItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCheckoutBillItems.setAdapter(itemAdapter);

        // Gateway Selection
        binding.cardGatewayBkash.setOnClickListener(v -> selectGateway(PaymentGatewayType.BKASH));
        binding.cardGatewayNagad.setOnClickListener(v -> selectGateway(PaymentGatewayType.NAGAD));
        binding.cardGatewayCard.setOnClickListener(v -> selectGateway(PaymentGatewayType.CARD_SSLCOMMERZ));
        binding.cardGatewayCash.setOnClickListener(v -> selectGateway(PaymentGatewayType.CASH_COUNTER));

        binding.btnExecutePayment.setOnClickListener(v -> executeCheckoutPayment());
    }

    private void selectGateway(PaymentGatewayType type) {
        this.selectedGateway = type;

        binding.rbGatewayBkash.setChecked(type == PaymentGatewayType.BKASH);
        binding.rbGatewayNagad.setChecked(type == PaymentGatewayType.NAGAD);
        binding.rbGatewayCard.setChecked(type == PaymentGatewayType.CARD_SSLCOMMERZ);
        binding.rbGatewayCash.setChecked(type == PaymentGatewayType.CASH_COUNTER);

        int activeColor = ContextCompat.getColor(this, R.color.primary_moss_green);
        int defaultColor = ContextCompat.getColor(this, R.color.border_muted);

        binding.cardGatewayBkash.setStrokeColor(type == PaymentGatewayType.BKASH ? activeColor : defaultColor);
        binding.cardGatewayNagad.setStrokeColor(type == PaymentGatewayType.NAGAD ? activeColor : defaultColor);
        binding.cardGatewayCard.setStrokeColor(type == PaymentGatewayType.CARD_SSLCOMMERZ ? activeColor : defaultColor);
        binding.cardGatewayCash.setStrokeColor(type == PaymentGatewayType.CASH_COUNTER ? activeColor : defaultColor);

        binding.cardGatewayBkash.setStrokeWidth(type == PaymentGatewayType.BKASH ? 4 : 2);
        binding.cardGatewayNagad.setStrokeWidth(type == PaymentGatewayType.NAGAD ? 4 : 2);
        binding.cardGatewayCard.setStrokeWidth(type == PaymentGatewayType.CARD_SSLCOMMERZ ? 4 : 2);
        binding.cardGatewayCash.setStrokeWidth(type == PaymentGatewayType.CASH_COUNTER ? 4 : 2);
    }

    private void loadBillDetails() {
        billingRepository.getBillById(hospitalId, billId, new AuthCallback<HospitalBill>() {
            @Override
            public void onSuccess(HospitalBill bill) {
                currentBill = bill;
                bindBillData(bill);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(BillingCheckoutActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void bindBillData(@NonNull HospitalBill bill) {
        binding.tvCheckoutBillNumber.setText(bill.getBillNumber());
        binding.tvCheckoutCategoryBadge.setText(BillCategory.fromKey(bill.getCategory()).getDisplayName().toUpperCase());
        binding.tvCheckoutPatientDetails.setText("Patient: " + bill.getPatientName()
                + " (" + (bill.getPatientArohiId() != null ? bill.getPatientArohiId() : "AROHI-PAT") + ")");

        if (bill.getItems() != null) {
            itemAdapter.submitList(bill.getItems());
        }

        binding.tvCheckoutSubtotal.setText(String.format(Locale.US, "৳ %.2f", bill.getSubtotalBdt()));
        binding.tvCheckoutVat.setText(String.format(Locale.US, "৳ %.2f", bill.getVatBdt()));
        binding.tvCheckoutTotal.setText(String.format(Locale.US, "৳ %.2f", bill.getTotalAmountBdt()));
        binding.btnExecutePayment.setText(String.format(Locale.US, "Pay ৳ %.2f via %s",
                bill.getTotalAmountBdt(), selectedGateway.getShortName()));

        if (PaymentStatus.SUCCESSFUL.name().equalsIgnoreCase(bill.getStatus())) {
            binding.btnExecutePayment.setText("View Digital Receipt (Already Paid)");
            binding.btnExecutePayment.setOnClickListener(v -> openReceiptScreen(null));
        }
    }

    private void executeCheckoutPayment() {
        if (currentBill == null) return;

        if (PaymentStatus.SUCCESSFUL.name().equalsIgnoreCase(currentBill.getStatus())) {
            openReceiptScreen(null);
            return;
        }

        binding.btnExecutePayment.setEnabled(false);
        binding.progressBarPayment.setVisibility(View.VISIBLE);
        binding.btnExecutePayment.setText("Initiating " + selectedGateway.getShortName() + " Checkout…");

        // 1. Initiate Gateway Transaction
        billingRepository.initiateCheckout(hospitalId, billId, selectedGateway, true, new AuthCallback<PaymentTransaction>() {
            @Override
            public void onSuccess(PaymentTransaction transaction) {
                binding.btnExecutePayment.setText("Verifying Transaction on Secure Server…");

                // Simulate realistic secure gateway callback & backend webhook verification
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    billingRepository.verifyAndCompletePayment(
                            hospitalId,
                            billId,
                            transaction.getTransactionId(),
                            transaction.getIdempotencyKey(),
                            selectedGateway,
                            new AuthCallback<PaymentReceipt>() {
                                @Override
                                public void onSuccess(PaymentReceipt receipt) {
                                    binding.progressBarPayment.setVisibility(View.GONE);
                                    binding.btnExecutePayment.setEnabled(true);
                                    Toast.makeText(BillingCheckoutActivity.this,
                                            "Payment Verified Successfully! Receipt Generated.", Toast.LENGTH_LONG).show();
                                    openReceiptScreen(receipt);
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    binding.progressBarPayment.setVisibility(View.GONE);
                                    binding.btnExecutePayment.setEnabled(true);
                                    binding.btnExecutePayment.setText("Retry Payment");
                                    Toast.makeText(BillingCheckoutActivity.this,
                                            "Payment Verification Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                                }
                            }
                    );
                }, 1200);
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBarPayment.setVisibility(View.GONE);
                binding.btnExecutePayment.setEnabled(true);
                Toast.makeText(BillingCheckoutActivity.this, "Checkout Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openReceiptScreen(@Nullable PaymentReceipt receipt) {
        Intent intent = DigitalReceiptActivity.newIntent(this, billId, hospitalId);
        if (receipt != null) {
            intent.putExtra(DigitalReceiptActivity.EXTRA_RECEIPT, receipt);
        }
        startActivity(intent);
        finish();
    }
}
