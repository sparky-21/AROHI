package com.arohi.healthcare.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.adapters.TrackingTimelineAdapter;
import com.arohi.healthcare.adapters.UnifiedTrackingAdapter;
import com.arohi.healthcare.databinding.ActivityUnifiedTrackingBinding;
import com.arohi.healthcare.databinding.DialogTrackingTimelineBinding;
import com.arohi.healthcare.models.AmbulanceRequest;
import com.arohi.healthcare.models.Appointment;
import com.arohi.healthcare.models.Operation;
import com.arohi.healthcare.models.PaymentStatus;
import com.arohi.healthcare.models.PharmacyOrder;
import com.arohi.healthcare.models.TrackingCategory;
import com.arohi.healthcare.models.UnifiedTrackingItem;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.repositories.AmbulanceTrackingRepository;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.ClinicalRepository;
import com.arohi.healthcare.repositories.HospitalOperationsRepository;
import com.arohi.healthcare.repositories.PharmacyRepository;
import com.arohi.healthcare.repositories.UnifiedTrackingRepository;
import com.arohi.healthcare.utils.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

/**
 * Phase 14 Unified Tracking Dashboard Activity.
 * Provides hospital-scoped, role-filtered tracking and workflow controls across
 * Pharmacy Delivery, Appointments, Operations (OT), Payments, and Ambulance Dispatch.
 */
public class UnifiedTrackingActivity extends BaseActivity {

    public static final String EXTRA_INITIAL_CATEGORY = "extra_initial_category";
    public static final String EXTRA_HIGHLIGHT_ITEM_ID = "extra_highlight_item_id";

    private ActivityUnifiedTrackingBinding binding;
    private UnifiedTrackingRepository trackingRepository;
    private AmbulanceTrackingRepository ambulanceRepository;
    private PharmacyRepository pharmacyRepository;
    private ClinicalRepository clinicalRepository;
    private HospitalOperationsRepository operationsRepository;
    private SessionManager sessionManager;
    private UnifiedTrackingAdapter adapter;
    private UserAccount currentAccount;
    private String hospitalId;
    @Nullable
    private TrackingCategory selectedCategory = null;

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, UnifiedTrackingActivity.class);
    }

    public static Intent newIntent(@NonNull Context context, @Nullable TrackingCategory category) {
        Intent intent = new Intent(context, UnifiedTrackingActivity.class);
        if (category != null) {
            intent.putExtra(EXTRA_INITIAL_CATEGORY, category.getKey());
        }
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUnifiedTrackingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        trackingRepository = new UnifiedTrackingRepository(this);
        ambulanceRepository = new AmbulanceTrackingRepository(this);
        pharmacyRepository = new PharmacyRepository(this);
        clinicalRepository = new ClinicalRepository(this);
        operationsRepository = new HospitalOperationsRepository(this);

        currentAccount = sessionManager.getCurrentSessionAccount();
        hospitalId = sessionManager.getSelectedHospitalId();

        if (currentAccount == null || hospitalId == null || hospitalId.trim().isEmpty()) {
            Toast.makeText(this, "Active hospital session required.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String initCat = getIntent().getStringExtra(EXTRA_INITIAL_CATEGORY);
        if (initCat != null && !initCat.trim().isEmpty()) {
            selectedCategory = TrackingCategory.fromKey(initCat);
        }

        setupUI();
        loadTrackingFeed();
    }

    private void setupUI() {
        String hospName = sessionManager.getSelectedHospitalName() != null
                ? sessionManager.getSelectedHospitalName() : "Hospital";
        String hospCode = sessionManager.getSelectedHospitalCode() != null
                ? sessionManager.getSelectedHospitalCode() : "AROHI";
        binding.tvTrackingHospitalSubtitle.setText(hospName + " • " + hospCode);
        binding.tvTrackingRoleBadge.setText(currentAccount.getUserRoleEnum().getKey());

        binding.btnTrackingBack.setOnClickListener(v -> finish());
        binding.btnTrackingRefresh.setOnClickListener(v -> loadTrackingFeed());

        boolean isPatient = currentAccount.getUserRoleEnum() == UserRole.PATIENT;
        binding.btnRequestEmergencyAmbulance.setVisibility(isPatient ? View.VISIBLE : View.GONE);
        binding.btnRequestEmergencyAmbulance.setOnClickListener(v -> showRequestAmbulanceDialog());

        adapter = new UnifiedTrackingAdapter(this::showTimelineSheet);
        binding.rvUnifiedTracking.setLayoutManager(new LinearLayoutManager(this));
        binding.rvUnifiedTracking.setAdapter(adapter);

        preselectFilterChip();
        binding.chipGroupTrackingFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipFilterAll) selectedCategory = null;
            else if (id == R.id.chipFilterPharmacy) selectedCategory = TrackingCategory.PHARMACY;
            else if (id == R.id.chipFilterAppointment) selectedCategory = TrackingCategory.APPOINTMENT;
            else if (id == R.id.chipFilterOperation) selectedCategory = TrackingCategory.OPERATION;
            else if (id == R.id.chipFilterPayment) selectedCategory = TrackingCategory.PAYMENT;
            else if (id == R.id.chipFilterAmbulance) selectedCategory = TrackingCategory.AMBULANCE;
            loadTrackingFeed();
        });
    }

    private void preselectFilterChip() {
        if (selectedCategory == null) {
            binding.chipFilterAll.setChecked(true);
            return;
        }
        switch (selectedCategory) {
            case PHARMACY:
                binding.chipFilterPharmacy.setChecked(true);
                break;
            case APPOINTMENT:
                binding.chipFilterAppointment.setChecked(true);
                break;
            case OPERATION:
                binding.chipFilterOperation.setChecked(true);
                break;
            case PAYMENT:
                binding.chipFilterPayment.setChecked(true);
                break;
            case AMBULANCE:
                binding.chipFilterAmbulance.setChecked(true);
                break;
            default:
                binding.chipFilterAll.setChecked(true);
        }
    }

    private void loadTrackingFeed() {
        trackingRepository.loadUnifiedTrackingItems(hospitalId, currentAccount, selectedCategory,
                new AuthCallback<List<UnifiedTrackingItem>>() {
                    @Override
                    public void onSuccess(List<UnifiedTrackingItem> items) {
                        adapter.submitList(items);

                        int activeCount = 0;
                        int completedCount = 0;
                        for (UnifiedTrackingItem it : items) {
                            if (it.isActive()) activeCount++;
                            else completedCount++;
                        }
                        binding.tvTrackingActiveCount.setText(String.valueOf(activeCount));
                        binding.tvTrackingCompletedCount.setText(String.valueOf(completedCount));

                        boolean isEmpty = items.isEmpty();
                        binding.layoutTrackingEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                        binding.rvUnifiedTracking.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(UnifiedTrackingActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showTimelineSheet(@NonNull UnifiedTrackingItem item) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        DialogTrackingTimelineBinding sheet = DialogTrackingTimelineBinding.inflate(LayoutInflater.from(this));
        dialog.setContentView(sheet.getRoot());

        sheet.tvDialogTimelineTitle.setText(item.getTitle());
        sheet.tvDialogTimelineSubtitle.setText(item.getCategory().getDisplayName()
                + " • Status: " + item.getCurrentStatus()
                + "\n" + item.getEtaOrScheduleText());

        TrackingTimelineAdapter timelineAdapter = new TrackingTimelineAdapter();
        sheet.rvDialogTimelineSteps.setLayoutManager(new LinearLayoutManager(this));
        sheet.rvDialogTimelineSteps.setAdapter(timelineAdapter);
        timelineAdapter.submitList(item.getTimeline());

        configureSheetActions(sheet, dialog, item);

        sheet.btnDialogTimelineClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void configureSheetActions(@NonNull DialogTrackingTimelineBinding sheet,
                                       @NonNull BottomSheetDialog dialog,
                                       @NonNull UnifiedTrackingItem item) {
        UserRole role = currentAccount.getUserRoleEnum();
        boolean isStaffOrAdmin = role == UserRole.ADMIN || role == UserRole.DOCTOR || role == UserRole.NURSE;

        switch (item.getCategory()) {
            case PHARMACY:
                sheet.btnDialogLiveMap.setVisibility(View.VISIBLE);
                sheet.btnDialogLiveMap.setText(R.string.tracking_btn_live_map);
                sheet.btnDialogLiveMap.setOnClickListener(v -> {
                    dialog.dismiss();
                    startActivity(LiveTrackingMapActivity.newPharmacyMapIntent(this, hospitalId, item.getItemId()));
                });
                if (isStaffOrAdmin && !item.isTerminal()) {
                    sheet.btnDialogAdvanceStage.setVisibility(View.VISIBLE);
                    sheet.btnDialogAdvanceStage.setText("Update Delivery Stage");
                    sheet.btnDialogAdvanceStage.setOnClickListener(v -> {
                        dialog.dismiss();
                        showPharmacyStaffAdvanceDialog(item.getItemId());
                    });
                }
                break;

            case APPOINTMENT:
                if (!item.isTerminal()) {
                    sheet.btnDialogAdvanceStage.setVisibility(View.VISIBLE);
                    if (isStaffOrAdmin) {
                        sheet.btnDialogAdvanceStage.setText("Update Queue / Stage");
                        sheet.btnDialogAdvanceStage.setOnClickListener(v -> {
                            dialog.dismiss();
                            showAppointmentQueueDialog(item.getItemId());
                        });
                    } else {
                        sheet.btnDialogAdvanceStage.setText("Cancel Appointment");
                        sheet.btnDialogAdvanceStage.setOnClickListener(v -> {
                            dialog.dismiss();
                            cancelPatientAppointment(item.getItemId());
                        });
                    }
                }
                break;

            case OPERATION:
                if (isStaffOrAdmin && !item.isTerminal()) {
                    sheet.btnDialogAdvanceStage.setVisibility(View.VISIBLE);
                    sheet.btnDialogAdvanceStage.setText("Update OT Stage");
                    sheet.btnDialogAdvanceStage.setOnClickListener(v -> {
                        dialog.dismiss();
                        showOperationStageDialog(item.getItemId());
                    });
                } else if (role == UserRole.PATIENT) {
                    sheet.btnDialogAdvanceStage.setVisibility(View.VISIBLE);
                    sheet.btnDialogAdvanceStage.setText("Authorize Family View");
                    sheet.btnDialogAdvanceStage.setOnClickListener(v -> {
                        dialog.dismiss();
                        showAuthorizeFamilyDialog(item.getItemId());
                    });
                }
                break;

            case PAYMENT:
                sheet.btnDialogLiveMap.setVisibility(View.VISIBLE);
                if (PaymentStatus.SUCCESSFUL.name().equalsIgnoreCase(item.getCurrentStatus())) {
                    sheet.btnDialogLiveMap.setText("Open Digital Receipt");
                    sheet.btnDialogLiveMap.setOnClickListener(v -> {
                        dialog.dismiss();
                        startActivity(DigitalReceiptActivity.newIntent(this, item.getItemId(), hospitalId));
                    });
                } else {
                    sheet.btnDialogLiveMap.setText("Proceed to Checkout");
                    sheet.btnDialogLiveMap.setOnClickListener(v -> {
                        dialog.dismiss();
                        startActivity(BillingCheckoutActivity.newIntent(this, item.getItemId(), hospitalId));
                    });
                }
                break;

            case AMBULANCE:
                sheet.btnDialogLiveMap.setVisibility(View.VISIBLE);
                sheet.btnDialogLiveMap.setText(R.string.tracking_btn_live_map);
                sheet.btnDialogLiveMap.setOnClickListener(v -> {
                    dialog.dismiss();
                    startActivity(LiveTrackingMapActivity.newAmbulanceMapIntent(this, hospitalId, item.getItemId()));
                });
                if (!item.isTerminal() && role == UserRole.ADMIN) {
                    sheet.btnDialogAdvanceStage.setVisibility(View.VISIBLE);
                    sheet.btnDialogAdvanceStage.setText("Dispatch / Advance");
                    sheet.btnDialogAdvanceStage.setOnClickListener(v -> {
                        dialog.dismiss();
                        showAmbulanceAuthorityDialog(item.getItemId());
                    });
                }
                break;
        }
    }

    private void showPharmacyStaffAdvanceDialog(String orderId) {
        PharmacyOrder order = sessionManager.getPharmacyOrderLocal(orderId);
        if (order == null) return;
        List<String> stages = order.getExpectedStages();
        int nextIdx = Math.min(order.getStatusStepIndex() + 1, stages.size() - 1);
        String nextStatus = stages.get(nextIdx);

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(48, 24, 48, 8);
        EditText etPartner = new EditText(this);
        etPartner.setHint("Delivery Partner Name (optional)");
        etPartner.setText(order.getDeliveryPartnerName());
        form.addView(etPartner);
        EditText etEta = new EditText(this);
        etEta.setHint("ETA (e.g. 25 min)");
        etEta.setText(order.getEstimatedDeliveryTime());
        form.addView(etEta);

        new AlertDialog.Builder(this)
                .setTitle("Advance Order to: " + nextStatus)
                .setView(form)
                .setPositiveButton("Confirm", (d, w) ->
                        pharmacyRepository.updateDeliveryDispatchDetails(
                                orderId, nextStatus, etPartner.getText().toString().trim(),
                                order.getDeliveryPartnerPhone(), etEta.getText().toString().trim(),
                                null, null, "Updated via Unified Tracking", currentAccount.getUid(),
                                new AuthCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        Toast.makeText(UnifiedTrackingActivity.this,
                                                "Order updated to " + nextStatus, Toast.LENGTH_SHORT).show();
                                        loadTrackingFeed();
                                    }

                                    @Override
                                    public void onError(String message) {
                                        Toast.makeText(UnifiedTrackingActivity.this, message, Toast.LENGTH_SHORT).show();
                                    }
                                }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAppointmentQueueDialog(String appointmentId) {
        Appointment appt = sessionManager.getAppointmentLocal(appointmentId);
        if (appt == null) return;

        String[] stages = {
                Appointment.STATUS_CONFIRMED,
                Appointment.STATUS_CHECKED_IN,
                Appointment.STATUS_WAITING,
                Appointment.STATUS_IN_CONSULTATION,
                Appointment.STATUS_COMPLETED,
                Appointment.STATUS_CANCELLED
        };
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(48, 24, 48, 8);
        Spinner sp = new Spinner(this);
        sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, stages));
        form.addView(sp);

        EditText etQueue = new EditText(this);
        etQueue.setHint("Queue Position (optional, leave empty if N/A)");
        if (appt.getQueuePosition() > 0) etQueue.setText(String.valueOf(appt.getQueuePosition()));
        form.addView(etQueue);

        EditText etWait = new EditText(this);
        etWait.setHint("Est. Wait Minutes (optional)");
        if (appt.getEstimatedWaitMinutes() > 0) etWait.setText(String.valueOf(appt.getEstimatedWaitMinutes()));
        form.addView(etWait);

        new AlertDialog.Builder(this)
                .setTitle("Appointment Queue & Stage")
                .setView(form)
                .setPositiveButton("Update", (d, w) -> {
                    String next = sp.getSelectedItem().toString();
                    int qPos = parsePositiveIntOrMinusOne(etQueue.getText().toString());
                    int waitMin = parsePositiveIntOrMinusOne(etWait.getText().toString());
                    clinicalRepository.updateAppointmentQueueAndStage(appt, next, qPos, waitMin,
                            null, null, new AuthCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    loadTrackingFeed();
                                }

                                @Override
                                public void onError(String message) {
                                    Toast.makeText(UnifiedTrackingActivity.this, message, Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void cancelPatientAppointment(String appointmentId) {
        Appointment appt = sessionManager.getAppointmentLocal(appointmentId);
        if (appt == null) return;
        clinicalRepository.updateAppointmentStatus(appt, Appointment.STATUS_CANCELLED,
                null, "Cancelled from Unified Tracking", new AuthCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        loadTrackingFeed();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(UnifiedTrackingActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showOperationStageDialog(String operationId) {
        String[] stages = {
                Operation.STATUS_PREPARATION,
                Operation.STATUS_SHIFTED_TO_OT,
                Operation.STATUS_IN_PROGRESS,
                Operation.STATUS_COMPLETED,
                Operation.STATUS_RECOVERY,
                Operation.STATUS_FOLLOW_UP
        };
        Spinner sp = new Spinner(this);
        sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, stages));

        new AlertDialog.Builder(this)
                .setTitle("Update OT Operation Progress")
                .setView(sp)
                .setPositiveButton("Update", (d, w) -> operationsRepository.updateOperationStatus(
                        operationId, sp.getSelectedItem().toString(), null, null,
                        new AuthCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                loadTrackingFeed();
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(UnifiedTrackingActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAuthorizeFamilyDialog(String operationId) {
        EditText etFamilyUid = new EditText(this);
        etFamilyUid.setHint("Authorized Family Member AROHI / User ID");
        new AlertDialog.Builder(this)
                .setTitle("Authorize Family OT Status View")
                .setMessage("Only stage status, OT room, and lead surgeon will be visible — surgical notes remain private.")
                .setView(etFamilyUid)
                .setPositiveButton("Authorize", (d, w) -> {
                    String famId = etFamilyUid.getText().toString().trim();
                    if (!famId.isEmpty()) {
                        operationsRepository.authorizeFamilyViewer(operationId, famId, true,
                                new AuthCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        Toast.makeText(UnifiedTrackingActivity.this,
                                                "Family member authorized for privacy-safe OT status.", Toast.LENGTH_SHORT).show();
                                        loadTrackingFeed();
                                    }

                                    @Override
                                    public void onError(String message) {
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAmbulanceAuthorityDialog(String requestId) {
        AmbulanceRequest req = ambulanceRepository.getRequestById(hospitalId, requestId);
        if (req == null) return;

        if (AmbulanceRequest.STATUS_REQUESTED.equalsIgnoreCase(req.getStatus())
                || AmbulanceRequest.STATUS_ACCEPTED.equalsIgnoreCase(req.getStatus())) {
            LinearLayout form = new LinearLayout(this);
            form.setOrientation(LinearLayout.VERTICAL);
            form.setPadding(48, 24, 48, 8);
            EditText etVehicle = new EditText(this);
            etVehicle.setHint("Ambulance Vehicle No. (e.g. DHAKA-AMB-04)");
            form.addView(etVehicle);
            EditText etDriverName = new EditText(this);
            etDriverName.setHint("Driver Full Name");
            form.addView(etDriverName);
            EditText etDriverPhone = new EditText(this);
            etDriverPhone.setHint("Driver Phone");
            form.addView(etDriverPhone);

            new AlertDialog.Builder(this)
                    .setTitle("Accept & Assign Ambulance")
                    .setView(form)
                    .setPositiveButton("Assign Ambulance", (d, w) -> {
                        String vNum = etVehicle.getText().toString().trim();
                        String dName = etDriverName.getText().toString().trim();
                        String dPhone = etDriverPhone.getText().toString().trim();
                        if (vNum.isEmpty() || dName.isEmpty()) {
                            Toast.makeText(this, "Vehicle and driver name required.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        ambulanceRepository.acceptAndAssignAmbulance(
                                hospitalId, requestId, currentAccount, vNum, "ACLS",
                                "drv_" + System.currentTimeMillis(), dName, dPhone,
                                new AuthCallback<AmbulanceRequest>() {
                                    @Override
                                    public void onSuccess(AmbulanceRequest result) {
                                        loadTrackingFeed();
                                    }

                                    @Override
                                    public void onError(String message) {
                                        Toast.makeText(UnifiedTrackingActivity.this, message, Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        int nextIdx = Math.min(req.getStatusStepIndex() + 1, AmbulanceRequest.WORKFLOW_STAGES.size() - 1);
        String nextStatus = AmbulanceRequest.WORKFLOW_STAGES.get(nextIdx);
        ambulanceRepository.advanceAmbulanceStatus(hospitalId, requestId, nextStatus, currentAccount,
                "Updated via Hospital Dispatch", -1, new AuthCallback<AmbulanceRequest>() {
                    @Override
                    public void onSuccess(AmbulanceRequest result) {
                        loadTrackingFeed();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(UnifiedTrackingActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showRequestAmbulanceDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(48, 24, 48, 8);
        EditText etPickup = new EditText(this);
        etPickup.setHint("Pickup Street Address / Area");
        form.addView(etPickup);
        Spinner spPriority = new Spinner(this);
        spPriority.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"CRITICAL", "HIGH", "STANDARD"}));
        form.addView(spPriority);
        EditText etNotes = new EditText(this);
        etNotes.setHint("Emergency details");
        form.addView(etNotes);

        new AlertDialog.Builder(this)
                .setTitle(R.string.tracking_btn_request_ambulance)
                .setView(form)
                .setPositiveButton("Submit Request", (d, w) -> {
                    String pickup = etPickup.getText().toString().trim();
                    if (pickup.isEmpty()) {
                        Toast.makeText(this, "Enter pickup address.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String hospName = sessionManager.getSelectedHospitalName() != null
                            ? sessionManager.getSelectedHospitalName() : "Selected Hospital";
                    ambulanceRepository.createAmbulanceRequest(
                            hospitalId, hospName, currentAccount, pickup, null, null,
                            spPriority.getSelectedItem().toString(), etNotes.getText().toString().trim(),
                            new AuthCallback<AmbulanceRequest>() {
                                @Override
                                public void onSuccess(AmbulanceRequest saved) {
                                    Toast.makeText(UnifiedTrackingActivity.this,
                                            "Ambulance requested (" + saved.getRequestId() + ")", Toast.LENGTH_SHORT).show();
                                    selectedCategory = TrackingCategory.AMBULANCE;
                                    binding.chipFilterAmbulance.setChecked(true);
                                    loadTrackingFeed();
                                }

                                @Override
                                public void onError(String message) {
                                    Toast.makeText(UnifiedTrackingActivity.this, message, Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int parsePositiveIntOrMinusOne(String raw) {
        if (raw == null || raw.trim().isEmpty()) return -1;
        try {
            int val = Integer.parseInt(raw.trim());
            return val > 0 ? val : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
