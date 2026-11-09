package com.arohi.healthcare.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.google.android.material.button.MaterialButton;
import com.arohi.healthcare.adapters.MedicineAdapter;
import com.arohi.healthcare.adapters.PharmacyOrderAdapter;
import com.arohi.healthcare.databinding.DialogCreatePharmacyOrderBinding;
import com.arohi.healthcare.databinding.DialogManageOrderStatusBinding;
import com.arohi.healthcare.databinding.FragmentPharmacyBinding;
import com.arohi.healthcare.models.DigitalPrescription;
import com.arohi.healthcare.models.Medicine;
import com.arohi.healthcare.models.PharmacyOrder;
import com.arohi.healthcare.models.PharmacyOrderItem;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.ClinicalRepository;
import com.arohi.healthcare.repositories.PharmacyRepository;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fragment managing AROHI Smart E-Pharmacy:
 * - Medicine catalogue browsing, search, and category filtering
 * - Product details & inventory availability
 * - Prescription-based ordering & OTC ordering
 * - Pharmacy order tracking (Received -> Verified -> Preparing -> Ready -> Collected)
 * - Staff order management permissions
 */
public class PharmacyFragment extends Fragment {

    private static final String ARG_PRESELECTED_RX_ID = "arg_preselected_rx_id";

    private FragmentPharmacyBinding binding;
    private PharmacyRepository pharmacyRepository;
    private ClinicalRepository clinicalRepository;
    private SessionManager sessionManager;
    private UserAccount currentAccount;

    private MedicineAdapter medicineAdapter;
    private PharmacyOrderAdapter orderAdapter;

    private boolean isOrdersTab = false;
    private boolean isStaff = false;
    private String currentCategory = "All";
    private String currentSearchQuery = "";
    private String preselectedPrescriptionId = null;

    private final List<DigitalPrescription> patientPrescriptions = new ArrayList<>();

    public static PharmacyFragment newInstance(@Nullable String preselectedPrescriptionId) {
        PharmacyFragment fragment = new PharmacyFragment();
        Bundle args = new Bundle();
        if (preselectedPrescriptionId != null) {
            args.putString(ARG_PRESELECTED_RX_ID, preselectedPrescriptionId);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPharmacyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pharmacyRepository = new PharmacyRepository(requireContext());
        clinicalRepository = new ClinicalRepository(requireContext());
        sessionManager = new SessionManager(requireContext());
        currentAccount = sessionManager.getCurrentSessionAccount();

        if (getArguments() != null) {
            preselectedPrescriptionId = getArguments().getString(ARG_PRESELECTED_RX_ID, null);
        }

        isStaff = currentAccount != null && (currentAccount.getUserRoleEnum() == UserRole.ADMIN
                || currentAccount.getUserRoleEnum() == UserRole.NURSE);

        if (isStaff) {
            binding.btnTabOrders.setText("All Hospital Orders");
            binding.btnManageCatalogue.setVisibility(View.VISIBLE);
        } else {
            binding.btnTabOrders.setText("My Orders");
            binding.btnManageCatalogue.setVisibility(View.GONE);
        }

        setupRecyclerViews();
        setupListeners();
        loadPrescriptionsCache();
        loadCurrentTab();

        // If pre-selected prescription passed from Medical Records, open quick order dialog
        if (preselectedPrescriptionId != null) {
            showPrescriptionOrderDirectDialog(preselectedPrescriptionId);
        }
    }

    private void setupRecyclerViews() {
        binding.recyclerPharmacy.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Medicine catalogue adapter
        medicineAdapter = new MedicineAdapter(new MedicineAdapter.OnMedicineActionListener() {
            @Override
            public void onShowDetails(@NonNull Medicine medicine) {
                showMedicineDetailsDialog(medicine);
            }

            @Override
            public void onPlaceOrder(@NonNull Medicine medicine) {
                showPlaceOrderDialog(medicine, null);
            }
        });

        // Pharmacy orders adapter
        orderAdapter = new PharmacyOrderAdapter(isStaff, new PharmacyOrderAdapter.OnOrderActionListener() {
            @Override
            public void onViewOrderDetails(@NonNull PharmacyOrder order) {
                showOrderDetailsDialog(order);
            }

            @Override
            public void onManageOrderStatus(@NonNull PharmacyOrder order) {
                showManageOrderStatusDialog(order);
            }
        });
    }

    private void setupListeners() {
        binding.btnPharmacyBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Tab buttons
        binding.btnTabCatalogue.setOnClickListener(v -> switchTab(false));
        binding.btnTabOrders.setOnClickListener(v -> switchTab(true));

        // Quick Prescription Order Banner
        binding.btnQuickOrderPrescription.setOnClickListener(v -> showPrescriptionSelectionDialog());

        // Search text watcher / action
        binding.etSearchMedicine.setOnEditorActionListener((v, actionId, event) -> {
            currentSearchQuery = binding.etSearchMedicine.getText() != null
                    ? binding.etSearchMedicine.getText().toString().trim() : "";
            loadMedicinesCatalogue();
            return true;
        });

        // Category Chips
        binding.chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentCategory = "All";
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chipAnalgesics) currentCategory = Medicine.CATEGORY_ANALGESICS;
                else if (id == R.id.chipAntibiotics) currentCategory = Medicine.CATEGORY_ANTIBIOTICS;
                else if (id == R.id.chipCardio) currentCategory = Medicine.CATEGORY_CARDIOVASCULAR;
                else if (id == R.id.chipGI) currentCategory = Medicine.CATEGORY_GASTROINTESTINAL;
                else if (id == R.id.chipRespiratory) currentCategory = Medicine.CATEGORY_RESPIRATORY;
                else if (id == R.id.chipDiabetes) currentCategory = Medicine.CATEGORY_DIABETES;
                else if (id == R.id.chipVitamins) currentCategory = Medicine.CATEGORY_VITAMINS;
                else currentCategory = "All";
            }
            loadMedicinesCatalogue();
        });
    }

    private void switchTab(boolean toOrders) {
        isOrdersTab = toOrders;
        if (binding == null || getContext() == null || !isAdded()) return;

        int forestColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.deep_forest_green);
        int whiteColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white);

        if (isOrdersTab) {
            binding.btnTabCatalogue.setBackgroundTintList(null);
            binding.btnTabCatalogue.setTextColor(forestColor);
            binding.btnTabOrders.setBackgroundTintList(android.content.res.ColorStateList.valueOf(forestColor));
            binding.btnTabOrders.setTextColor(whiteColor);

            binding.layoutSearch.setVisibility(View.GONE);
            binding.scrollCategories.setVisibility(View.GONE);
            binding.cardPrescriptionQuickOrder.setVisibility(View.GONE);
            binding.recyclerPharmacy.setAdapter(orderAdapter);
            loadOrders();
        } else {
            binding.btnTabOrders.setBackgroundTintList(null);
            binding.btnTabOrders.setTextColor(forestColor);
            binding.btnTabCatalogue.setBackgroundTintList(android.content.res.ColorStateList.valueOf(forestColor));
            binding.btnTabCatalogue.setTextColor(whiteColor);

            binding.layoutSearch.setVisibility(View.VISIBLE);
            binding.scrollCategories.setVisibility(View.VISIBLE);
            binding.cardPrescriptionQuickOrder.setVisibility(View.VISIBLE);
            binding.recyclerPharmacy.setAdapter(medicineAdapter);
            loadMedicinesCatalogue();
        }
    }

    private void loadCurrentTab() {
        if (binding == null || !isAdded()) return;
        if (isOrdersTab) {
            loadOrders();
        } else {
            binding.recyclerPharmacy.setAdapter(medicineAdapter);
            loadMedicinesCatalogue();
        }
    }

    private void loadMedicinesCatalogue() {
        pharmacyRepository.loadMedicines(currentCategory, currentSearchQuery, new AuthCallback<List<Medicine>>() {
            @Override
            public void onSuccess(List<Medicine> result) {
                if (binding == null) return;
                medicineAdapter.submitList(result);
                if (result.isEmpty()) {
                    binding.layoutEmptyPharmacy.setVisibility(View.VISIBLE);
                    binding.tvEmptyTitle.setText("No Medicines Found");
                    binding.tvEmptySubtitle.setText("No medicines matched your search or category filter.");
                } else {
                    binding.layoutEmptyPharmacy.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String message) {
                if (binding != null) {
                    UiStateHelper.showError(binding.getRoot(), message);
                }
            }
        });
    }

    private void loadOrders() {
        if (currentAccount == null) return;
        if (isStaff) {
            pharmacyRepository.loadAllOrders(new AuthCallback<List<PharmacyOrder>>() {
                @Override
                public void onSuccess(List<PharmacyOrder> result) {
                    if (binding == null) return;
                    orderAdapter.submitList(result);
                    if (result.isEmpty()) {
                        binding.layoutEmptyPharmacy.setVisibility(View.VISIBLE);
                        binding.tvEmptyTitle.setText("No Pharmacy Orders");
                        binding.tvEmptySubtitle.setText("No orders have been received by the hospital pharmacy.");
                    } else {
                        binding.layoutEmptyPharmacy.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onError(String message) {
                    if (binding != null) UiStateHelper.showError(binding.getRoot(), message);
                }
            });
        } else {
            pharmacyRepository.loadPatientOrders(currentAccount.getUid(), new AuthCallback<List<PharmacyOrder>>() {
                @Override
                public void onSuccess(List<PharmacyOrder> result) {
                    if (binding == null) return;
                    orderAdapter.submitList(result);
                    if (result.isEmpty()) {
                        binding.layoutEmptyPharmacy.setVisibility(View.VISIBLE);
                        binding.tvEmptyTitle.setText("No Orders Placed");
                        binding.tvEmptySubtitle.setText("You have not placed any medicine orders yet.");
                    } else {
                        binding.layoutEmptyPharmacy.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onError(String message) {
                    if (binding != null) UiStateHelper.showError(binding.getRoot(), message);
                }
            });
        }
    }

    private void loadPrescriptionsCache() {
        if (currentAccount == null) return;
        clinicalRepository.loadPatientPrescriptions(currentAccount.getUid(), new AuthCallback<List<DigitalPrescription>>() {
            @Override
            public void onSuccess(List<DigitalPrescription> result) {
                patientPrescriptions.clear();
                patientPrescriptions.addAll(result);
            }

            @Override
            public void onError(String message) {}
        });
    }

    // =========================================================================
    // DIALOGS: DETAILS, ORDER CREATION, STATUS MANAGEMENT
    // =========================================================================

    private void showMedicineDetailsDialog(@NonNull Medicine m) {
        String msg = "Generic Name: " + m.getGenericName() + "\n"
                + "Dosage Form: " + m.getDosageForm() + " (" + m.getStrength() + ")\n"
                + "Category: " + m.getCategory() + "\n"
                + "Manufacturer: " + m.getManufacturer() + "\n"
                + "Unit Price: ৳ " + String.format(Locale.ROOT, "%.2f", m.getUnitPrice()) + "\n"
                + "Stock Status: " + m.getStockStatusLabel() + "\n"
                + "Prescription Required: " + (m.isRequiresPrescription() ? "Yes (Rx Only)" : "No (Over The Counter)") + "\n\n"
                + "Clinical Description:\n" + m.getDescription() + "\n\n"
                + "Dosage Instructions:\n" + m.getDosageInstructions() + "\n\n"
                + "Side Effects & Precautions:\n" + m.getSideEffects();

        new AlertDialog.Builder(requireContext())
                .setTitle(m.getName())
                .setMessage(msg)
                .setPositiveButton("Order Now", (d, w) -> showPlaceOrderDialog(m, null))
                .setNegativeButton("Close", null)
                .show();
    }

    private void showPlaceOrderDialog(@NonNull Medicine medicine, @Nullable DigitalPrescription linkedRx) {
        if (currentAccount == null) return;

        DialogCreatePharmacyOrderBinding dBinding = DialogCreatePharmacyOrderBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dBinding.getRoot())
                .create();

        dBinding.tvOrderMedicineName.setText(medicine.getName() + " • " + medicine.getDosageForm());

        // Quantity logic
        final int[] qty = {1};
        final double unitPrice = medicine.getUnitPrice();
        updateDialogTotal(dBinding, qty[0], unitPrice);

        dBinding.btnQtyMinus.setOnClickListener(v -> {
            if (qty[0] > 1) {
                qty[0]--;
                dBinding.tvOrderQuantity.setText(String.valueOf(qty[0]));
                updateDialogTotal(dBinding, qty[0], unitPrice);
            }
        });

        dBinding.btnQtyPlus.setOnClickListener(v -> {
            if (qty[0] < medicine.getStockQuantity()) {
                qty[0]++;
                dBinding.tvOrderQuantity.setText(String.valueOf(qty[0]));
                updateDialogTotal(dBinding, qty[0], unitPrice);
            } else {
                UiStateHelper.showWarning(dBinding.getRoot(), "Maximum stock available: " + medicine.getStockQuantity());
            }
        });

        // Prescription Spinner Setup
        List<String> rxOptions = new ArrayList<>();
        rxOptions.add("None (Over The Counter)");
        int preselectedIndex = 0;

        for (int i = 0; i < patientPrescriptions.size(); i++) {
            DigitalPrescription p = patientPrescriptions.get(i);
            String label = "Rx #" + p.getPrescriptionId() + " (Dr. " + p.getDoctorName() + " • " + p.getDiagnosis() + ")";
            rxOptions.add(label);
            if (linkedRx != null && linkedRx.getPrescriptionId().equals(p.getPrescriptionId())) {
                preselectedIndex = i + 1;
            }
        }

        ArrayAdapter<String> rxAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, rxOptions);
        dBinding.spinnerPrescriptions.setAdapter(rxAdapter);
        if (preselectedIndex > 0) {
            dBinding.spinnerPrescriptions.setSelection(preselectedIndex);
        }

        if (medicine.isRequiresPrescription()) {
            dBinding.tvRxNotice.setText("⚠️ Prescription Required: You must link a verified prescription to fulfill this medicine.");
            dBinding.tvRxNotice.setTextColor(requireContext().getColor(R.color.status_emergency));
        }

        dBinding.btnCancelOrderDialog.setOnClickListener(v -> dialog.dismiss());

        dBinding.btnSubmitOrder.setOnClickListener(v -> {
            int selectedRxPos = dBinding.spinnerPrescriptions.getSelectedItemPosition();
            String attachedRxId = null;
            String attachedDocName = null;

            if (selectedRxPos > 0 && selectedRxPos <= patientPrescriptions.size()) {
                DigitalPrescription chosen = patientPrescriptions.get(selectedRxPos - 1);
                attachedRxId = chosen.getPrescriptionId();
                attachedDocName = chosen.getDoctorName();
            }

            if (medicine.isRequiresPrescription() && (attachedRxId == null || attachedRxId.isEmpty())) {
                UiStateHelper.showError(dBinding.getRoot(), "Prescription-only medicine requires an attached digital prescription.");
                return;
            }

            // Determine Delivery Type
            String delivery = PharmacyOrder.DELIVERY_HOSPITAL_PICKUP;
            if (dBinding.rbWard.isChecked()) delivery = PharmacyOrder.DELIVERY_WARD_DELIVERY;
            else if (dBinding.rbHome.isChecked()) delivery = PharmacyOrder.DELIVERY_HOME;

            String address = dBinding.etDeliveryAddress.getText() != null
                    ? dBinding.etDeliveryAddress.getText().toString().trim() : "";

            // Create Order
            PharmacyOrderItem item = new PharmacyOrderItem(
                    medicine.getMedicineId(),
                    medicine.getName(),
                    medicine.getStrength(),
                    qty[0],
                    unitPrice
            );

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = sdf.format(new Date());

            PharmacyOrder newOrder = new PharmacyOrder(
                    "ord_" + System.currentTimeMillis(),
                    currentAccount.getUid(),
                    currentAccount.getArohiId(),
                    currentAccount.getFullName(),
                    currentAccount.getPhone(),
                    attachedRxId,
                    attachedDocName,
                    Collections.singletonList(item),
                    qty[0] * unitPrice,
                    delivery,
                    address,
                    today
            );

            pharmacyRepository.createOrder(newOrder, new AuthCallback<PharmacyOrder>() {
                @Override
                public void onSuccess(PharmacyOrder created) {
                    dialog.dismiss();
                    if (binding != null) {
                        UiStateHelper.showSuccess(binding.getRoot(),
                                "Order #" + created.getOrderId() + " submitted! Track progress in Orders tab.");
                        switchTab(true);
                    }
                }

                @Override
                public void onError(String message) {
                    UiStateHelper.showError(dBinding.getRoot(), message);
                }
            });
        });

        dialog.show();
    }

    private void updateDialogTotal(DialogCreatePharmacyOrderBinding dBinding, int quantity, double price) {
        double total = quantity * price;
        dBinding.tvOrderCalculatedTotal.setText(String.format(Locale.ROOT, "৳ %.2f", total));
    }

    private void showPrescriptionSelectionDialog() {
        if (patientPrescriptions.isEmpty()) {
            UiStateHelper.showWarning(binding.getRoot(),
                    "No active digital prescriptions found on your account. Consult a doctor in 'Doctors' tab first.");
            return;
        }

        // Build a styled dialog with Material buttons for each prescription
        int dp = (int) (requireContext().getResources().getDisplayMetrics().density);

        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int padH = 20 * dp;
        int padV = 8 * dp;
        container.setPadding(padH, padV, padH, padV);

        // Dialog title
        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText("Select Digital Prescription");
        tvTitle.setTextSize(18f);
        tvTitle.setTextColor(0xFF26332A);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 12 * dp, 0, 8 * dp);
        container.addView(tvTitle);

        // Divider under title
        View dividerTop = new View(requireContext());
        dividerTop.setBackgroundColor(0xFFD5E0CE);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        container.addView(dividerTop, dividerParams);

        AlertDialog[] dialogRef = new AlertDialog[1];

        for (int i = 0; i < patientPrescriptions.size(); i++) {
            final DigitalPrescription rx = patientPrescriptions.get(i);
            String label = "Rx #" + rx.getPrescriptionId()
                    + " • Dr. " + rx.getDoctorName()
                    + " (" + rx.getDiagnosis() + ")";

            MaterialButton btn = new MaterialButton(requireContext(),
                    null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btn.setText(label);
            btn.setTextColor(0xFF26332A);
            btn.setTextSize(13f);
            btn.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            btn.setAllCaps(false);
            btn.setInsetTop(0);
            btn.setInsetBottom(0);
            btn.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFFD5E0CE));
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
            btn.setRippleColor(android.content.res.ColorStateList.valueOf(0xFFE8EFE2));
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            btnParams.setMargins(0, 8 * dp, 0, 0);
            btn.setLayoutParams(btnParams);

            btn.setOnClickListener(v -> {
                if (dialogRef[0] != null) dialogRef[0].dismiss();
                showPrescriptionOrderDirectDialog(rx.getPrescriptionId());
            });
            container.addView(btn);
        }

        // Divider above cancel
        View dividerBottom = new View(requireContext());
        dividerBottom.setBackgroundColor(0xFFD5E0CE);
        LinearLayout.LayoutParams dividerBottom2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        dividerBottom2.setMargins(0, 12 * dp, 0, 0);
        container.addView(dividerBottom, dividerBottom2);

        // Cancel button
        MaterialButton btnCancel = new MaterialButton(requireContext(),
                null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnCancel.setText("Cancel");
        btnCancel.setAllCaps(false);
        btnCancel.setInsetTop(0);
        btnCancel.setInsetBottom(0);
        btnCancel.setTextColor(0xFF778276);
        btnCancel.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFFD5E0CE));
        btnCancel.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 44 * dp);
        cancelParams.setMargins(0, 10 * dp, 0, 8 * dp);
        btnCancel.setLayoutParams(cancelParams);
        btnCancel.setOnClickListener(v -> { if (dialogRef[0] != null) dialogRef[0].dismiss(); });
        container.addView(btnCancel);

        scrollView.addView(container);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(scrollView)
                .create();
        dialogRef[0] = dialog;
        dialog.show();
    }

    private void showPrescriptionOrderDirectDialog(String rxId) {
        DigitalPrescription match = null;
        for (DigitalPrescription p : patientPrescriptions) {
            if (p.getPrescriptionId().equals(rxId)) {
                match = p;
                break;
            }
        }
        if (match == null) {
            match = sessionManager.getPrescriptionLocal(rxId);
        }

        if (match != null && !match.getMedicines().isEmpty()) {
            // Find standard stock medicine or create order directly
            Medicine defMed = sessionManager.getAllMedicinesLocal().get(0);
            showPlaceOrderDialog(defMed, match);
        } else {
            UiStateHelper.showWarning(binding.getRoot(), "Prescription #" + rxId + " has no medicine items.");
        }
    }

    private void showOrderDetailsDialog(@NonNull PharmacyOrder order) {
        StringBuilder sb = new StringBuilder();
        sb.append("Order ID: #").append(order.getOrderId()).append("\n")
                .append("Status: ").append(order.getStatus()).append("\n")
                .append("Order Date: ").append(order.getOrderDate()).append("\n")
                .append("Patient: ").append(order.getPatientName()).append(" (ID: ").append(order.getPatientArohiId()).append(")\n")
                .append("Delivery: ").append(order.getDeliveryType()).append("\n");
        if (order.getDeliveryAddress() != null && !order.getDeliveryAddress().isEmpty()) {
            sb.append("Address/Bed: ").append(order.getDeliveryAddress()).append("\n");
        }
        if (order.isPrescriptionLinked()) {
            sb.append("Prescription: #").append(order.getPrescriptionId()).append(" (Dr. ").append(order.getPrescriptionDoctorName()).append(")\n");
        }
        sb.append("\nItems Ordered:\n");
        if (order.getItems() != null) {
            for (PharmacyOrderItem item : order.getItems()) {
                sb.append("• ").append(item.getMedicineName()).append(" (").append(item.getStrength()).append(") x ")
                        .append(item.getQuantity()).append(" = ৳ ").append(String.format(Locale.ROOT, "%.2f", item.getSubtotal())).append("\n");
            }
        }
        sb.append("\nTotal Amount: ৳ ").append(String.format(Locale.ROOT, "%.2f", order.getTotalAmount()));

        if (order.getPharmacistNotes() != null && !order.getPharmacistNotes().isEmpty()) {
            sb.append("\n\nPharmacist Notes:\n").append(order.getPharmacistNotes());
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("AROHI Pharmacy Order Summary")
                .setMessage(sb.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    private void showManageOrderStatusDialog(@NonNull PharmacyOrder order) {
        DialogManageOrderStatusBinding dBinding = DialogManageOrderStatusBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dBinding.getRoot())
                .create();

        dBinding.tvManageOrderId.setText("Order #" + order.getOrderId() + " • " + order.getPatientName());
        dBinding.tvCurrentStatusLabel.setText("Current Status: " + order.getStatus());

        // Valid Next Statuses based on workflow: Received -> Verified -> Preparing -> Ready -> Collected
        List<String> nextStatuses = new ArrayList<>();
        if (PharmacyOrder.STATUS_RECEIVED.equalsIgnoreCase(order.getStatus())) {
            nextStatuses.add(PharmacyOrder.STATUS_VERIFIED);
            nextStatuses.add(PharmacyOrder.STATUS_CANCELLED);
        } else if (PharmacyOrder.STATUS_VERIFIED.equalsIgnoreCase(order.getStatus())) {
            nextStatuses.add(PharmacyOrder.STATUS_PREPARING);
        } else if (PharmacyOrder.STATUS_PREPARING.equalsIgnoreCase(order.getStatus())) {
            nextStatuses.add(PharmacyOrder.STATUS_READY);
        } else if (PharmacyOrder.STATUS_READY.equalsIgnoreCase(order.getStatus())) {
            nextStatuses.add(PharmacyOrder.STATUS_COLLECTED);
        } else {
            nextStatuses.add(order.getStatus() + " (Final)");
        }

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, nextStatuses);
        dBinding.spinnerNextStatus.setAdapter(statusAdapter);

        dBinding.btnCancelStatusDialog.setOnClickListener(v -> dialog.dismiss());

        dBinding.btnConfirmStatusUpdate.setOnClickListener(v -> {
            String selectedNext = dBinding.spinnerNextStatus.getSelectedItem().toString();
            if (selectedNext.contains("Final")) {
                dialog.dismiss();
                return;
            }

            String notes = dBinding.etPharmacistNotes.getText() != null
                    ? dBinding.etPharmacistNotes.getText().toString().trim() : "";

            pharmacyRepository.updateOrderStatus(order.getOrderId(), selectedNext, notes,
                    currentAccount != null ? currentAccount.getUid() : "staff",
                    new AuthCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            dialog.dismiss();
                            if (binding != null) {
                                UiStateHelper.showSuccess(binding.getRoot(),
                                        "Order #" + order.getOrderId() + " updated to " + selectedNext);
                                loadOrders();
                            }
                        }

                        @Override
                        public void onError(String message) {
                            UiStateHelper.showError(dBinding.getRoot(), message);
                        }
                    });
        });

        dialog.show();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && binding != null && isAdded()) {
            loadPrescriptionsCache();
            loadCurrentTab();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && isAdded()) {
            loadPrescriptionsCache();
            loadCurrentTab();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
