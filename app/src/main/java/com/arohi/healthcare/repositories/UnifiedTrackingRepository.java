package com.arohi.healthcare.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.models.AmbulanceRequest;
import com.arohi.healthcare.models.Appointment;
import com.arohi.healthcare.models.HospitalBill;
import com.arohi.healthcare.models.Operation;
import com.arohi.healthcare.models.PaymentStatus;
import com.arohi.healthcare.models.PharmacyOrder;
import com.arohi.healthcare.models.TrackingCategory;
import com.arohi.healthcare.models.TrackingStatusEvent;
import com.arohi.healthcare.models.UnifiedTrackingItem;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.services.FirebaseProvider;
import com.arohi.healthcare.utils.SessionManager;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Phase 14 Unified Tracking Repository.
 * Aggregates multi-tenant (hospitalId-isolated), role-scoped tracking records across:
 * 1. Pharmacy Delivery Tracking
 * 2. Appointment & Queue Tracking
 * 3. Operation (OT) Progress Tracking (with explicit family authorization privacy protection)
 * 4. Payment & Billing Tracking
 * 5. Ambulance Live Dispatch Tracking (zero fake live GPS data)
 */
public class UnifiedTrackingRepository {

    private static final String PREF_TRACKING = "arohi_phase14_tracking_prefs";
    private static final String KEY_AMBULANCE_PREFIX = "ambulance_requests_";

    private final Context context;
    private final SharedPreferences prefs;
    private final SessionManager sessionManager;
    private final HospitalOperationsRepository operationsRepository;
    private final BillingRepository billingRepository;
    private final FirebaseFirestore firestore;

    public UnifiedTrackingRepository(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_TRACKING, Context.MODE_PRIVATE);
        this.sessionManager = new SessionManager(this.context);
        this.operationsRepository = new HospitalOperationsRepository(this.context);
        this.billingRepository = new BillingRepository(this.context);
        this.firestore = FirebaseProvider.getFirestore(this.context);
    }

    /**
     * Loads unified tracking items scoped strictly to the specified hospitalId and caller's UserRole.
     */
    public void loadUnifiedTrackingItems(@NonNull String hospitalId,
                                         @NonNull UserAccount currentAccount,
                                         @Nullable TrackingCategory categoryFilter,
                                         @NonNull AuthCallback<List<UnifiedTrackingItem>> callback) {
        if (hospitalId.trim().isEmpty()) {
            callback.onError("Valid hospital selection is required for tracking.");
            return;
        }

        List<UnifiedTrackingItem> result = new ArrayList<>();

        // 1. Pharmacy Orders
        if (categoryFilter == null || categoryFilter == TrackingCategory.PHARMACY) {
            for (PharmacyOrder order : sessionManager.getAllPharmacyOrdersLocal()) {
                String orderHosp = order.getHospitalId() != null ? order.getHospitalId() : "hosp_arohi_central";
                if (!hospitalId.equalsIgnoreCase(orderHosp)) continue;
                if (!canViewPharmacyOrder(currentAccount, order)) continue;
                result.add(projectPharmacyOrder(order));
            }
        }

        // 2. Appointments
        if (categoryFilter == null || categoryFilter == TrackingCategory.APPOINTMENT) {
            for (Appointment appt : sessionManager.getAllAppointmentsLocal()) {
                String apptHosp = appt.getHospitalId() != null ? appt.getHospitalId() : "hosp_arohi_central";
                if (!hospitalId.equalsIgnoreCase(apptHosp)) continue;
                if (!canViewAppointment(currentAccount, appt)) continue;
                result.add(projectAppointment(appt));
            }
        }

        // 3. Operations (with family privacy masking)
        if (categoryFilter == null || categoryFilter == TrackingCategory.OPERATION) {
            for (Operation op : operationsRepository.getLocalOperations()) {
                String opHosp = op.getHospitalId() != null ? op.getHospitalId() : "hosp_arohi_central";
                if (!hospitalId.equalsIgnoreCase(opHosp)) continue;
                if (!canViewOperation(currentAccount, op)) continue;
                result.add(projectOperation(op, currentAccount));
            }
        }

        // 4. Ambulance Dispatch
        if (categoryFilter == null || categoryFilter == TrackingCategory.AMBULANCE) {
            for (AmbulanceRequest req : getLocalAmbulanceRequests(hospitalId)) {
                if (!hospitalId.equalsIgnoreCase(req.getHospitalId())) continue;
                if (!canViewAmbulanceRequest(currentAccount, req)) continue;
                result.add(projectAmbulanceRequest(req));
            }
        }

        // 5. Payments (via Phase 13 BillingRepository)
        if (categoryFilter == null || categoryFilter == TrackingCategory.PAYMENT) {
            AuthCallback<List<HospitalBill>> billCallback = new AuthCallback<List<HospitalBill>>() {
                @Override
                public void onSuccess(List<HospitalBill> bills) {
                    if (bills != null) {
                        for (HospitalBill b : bills) {
                            if (hospitalId.equalsIgnoreCase(b.getHospitalId())) {
                                result.add(projectHospitalBill(b));
                            }
                        }
                    }
                    sortTrackingItems(result);
                    callback.onSuccess(result);
                }

                @Override
                public void onError(String message) {
                    sortTrackingItems(result);
                    callback.onSuccess(result);
                }
            };

            if (currentAccount.getUserRoleEnum() == UserRole.ADMIN) {
                billingRepository.getHospitalBills(hospitalId, "ALL", "ALL", billCallback);
            } else {
                billingRepository.getPatientBills(currentAccount.getUid(), billCallback);
            }
            return;
        }

        sortTrackingItems(result);
        callback.onSuccess(result);
    }

    private void sortTrackingItems(List<UnifiedTrackingItem> items) {
        Collections.sort(items, (a, b) -> {
            if (a.isActive() != b.isActive()) {
                return a.isActive() ? -1 : 1;
            }
            return Long.compare(b.getUpdatedAt(), a.getUpdatedAt());
        });
    }

    // =========================================================================
    // ROLE-BASED ACCESS CONTROL FILTERS
    // =========================================================================

    public boolean canViewPharmacyOrder(@NonNull UserAccount user, @NonNull PharmacyOrder order) {
        UserRole role = user.getUserRoleEnum();
        if (role == UserRole.ADMIN || role == UserRole.NURSE) return true;
        if (role == UserRole.DOCTOR) {
            return order.getPrescriptionDoctorName() != null
                    && user.getFullName() != null
                    && order.getPrescriptionDoctorName().equalsIgnoreCase(user.getFullName());
        }
        return user.getUid() != null && user.getUid().equals(order.getPatientUid());
    }

    public boolean canViewAppointment(@NonNull UserAccount user, @NonNull Appointment appt) {
        UserRole role = user.getUserRoleEnum();
        if (role == UserRole.ADMIN) return true;
        if (role == UserRole.DOCTOR) return user.getUid() != null && user.getUid().equals(appt.getDoctorUid());
        if (role == UserRole.NURSE) return true; // Ward/clinic triage nurses assist active outpatient queue
        return user.getUid() != null && user.getUid().equals(appt.getPatientUid());
    }

    public boolean canViewOperation(@NonNull UserAccount user, @NonNull Operation op) {
        UserRole role = user.getUserRoleEnum();
        if (role == UserRole.ADMIN) return true;
        if (role == UserRole.DOCTOR) return user.getUid() != null && user.getUid().equals(op.getDoctorUid());
        if (role == UserRole.NURSE) return user.getUid() != null && user.getUid().equals(op.getNurseUid());
        return op.isFamilyViewerAuthorized(user.getUid());
    }

    public boolean canViewAmbulanceRequest(@NonNull UserAccount user, @NonNull AmbulanceRequest req) {
        UserRole role = user.getUserRoleEnum();
        if (role == UserRole.ADMIN) return true;
        if (role == UserRole.DOCTOR || role == UserRole.NURSE) return true; // ER awareness
        return user.getUid() != null && (user.getUid().equals(req.getPatientUid())
                || user.getUid().equals(req.getDriverUid()));
    }

    // =========================================================================
    // DOMAIN PROJECTIONS
    // =========================================================================

    @NonNull
    public UnifiedTrackingItem projectPharmacyOrder(@NonNull PharmacyOrder order) {
        UnifiedTrackingItem item = new UnifiedTrackingItem();
        item.setItemId(order.getOrderId());
        item.setHospitalId(order.getHospitalId() != null ? order.getHospitalId() : "hosp_arohi_central");
        item.setCategory(TrackingCategory.PHARMACY);
        item.setTitle("Pharmacy Order #" + shortId(order.getOrderId()));
        item.setSubtitle((order.getItems() != null ? order.getItems().size() : 0) + " items • " + order.getDeliveryType());
        item.setCurrentStatus(order.getStatus());
        item.setExpectedStages(order.getExpectedStages());
        item.setCurrentStepIndex(order.getStatusStepIndex());
        item.setTotalSteps(item.getExpectedStages().size());
        boolean terminal = PharmacyOrder.STATUS_DELIVERED.equalsIgnoreCase(order.getStatus())
                || PharmacyOrder.STATUS_COLLECTED.equalsIgnoreCase(order.getStatus())
                || PharmacyOrder.STATUS_CANCELLED.equalsIgnoreCase(order.getStatus());
        item.setTerminal(terminal);
        item.setActive(!terminal);
        item.setHasLiveGps(order.hasActiveLiveLocation());
        String eta = order.getEstimatedDeliveryTime();
        if (eta != null && !eta.trim().isEmpty()) {
            item.setEtaOrScheduleText("ETA: " + eta);
        } else {
            item.setEtaOrScheduleText(order.isHomeDelivery()
                    ? (order.hasActiveLiveLocation() ? "Live partner GPS active" : "Delivery ETA awaiting dispatch telemetry")
                    : "Pickup at hospital pharmacy counter");
        }
        item.setActorSummary(order.getDeliveryPartnerName() != null && !order.getDeliveryPartnerName().isEmpty()
                ? "Partner: " + order.getDeliveryPartnerName()
                : "Patient: " + order.getPatientName());
        item.setDetailSummary(String.format(Locale.US, "Total: ৳ %.2f • %s",
                order.getTotalAmount(),
                order.getDeliveryAddress() != null && !order.getDeliveryAddress().isEmpty()
                        ? order.getDeliveryAddress() : "Counter Pickup"));
        item.setUpdatedAt(order.getUpdatedAtTimestamp() > 0 ? order.getUpdatedAtTimestamp() : order.getCreatedAtTimestamp());

        List<TrackingStatusEvent> history = new ArrayList<>(order.getTrackingHistory());
        if (history.isEmpty()) {
            history.add(new TrackingStatusEvent(
                    order.getStatus(),
                    item.getUpdatedAt(),
                    order.getPatientUid(),
                    order.getPatientName(),
                    "PHARMACY",
                    order.getPharmacistNotes() != null && !order.getPharmacistNotes().isEmpty()
                            ? order.getPharmacistNotes() : "Order status recorded."
            ));
        }
        item.setTimeline(history);
        return item;
    }

    @NonNull
    public UnifiedTrackingItem projectAppointment(@NonNull Appointment appt) {
        UnifiedTrackingItem item = new UnifiedTrackingItem();
        item.setItemId(appt.getAppointmentId());
        item.setHospitalId(appt.getHospitalId() != null ? appt.getHospitalId() : "hosp_arohi_central");
        item.setCategory(TrackingCategory.APPOINTMENT);
        item.setTitle("Dr. " + (appt.getDoctorName() != null ? appt.getDoctorName() : "Specialist"));
        item.setSubtitle(appt.getDoctorSpecialty() + " • " + appt.getConsultationType());
        item.setCurrentStatus(appt.getStatus());
        item.setExpectedStages(Arrays.asList(
                Appointment.STATUS_REQUESTED,
                Appointment.STATUS_CONFIRMED,
                Appointment.STATUS_CHECKED_IN,
                Appointment.STATUS_WAITING,
                Appointment.STATUS_IN_CONSULTATION,
                Appointment.STATUS_COMPLETED
        ));
        item.setCurrentStepIndex(appt.getStatusStepIndex());
        item.setTotalSteps(item.getExpectedStages().size());
        boolean terminal = appt.isCompleted() || appt.isCancelled();
        item.setTerminal(terminal);
        item.setActive(!terminal);
        item.setHasLiveGps(false);

        if (appt.hasLiveQueueData()) {
            String waitText = appt.getEstimatedWaitMinutes() > 0
                    ? " (~" + appt.getEstimatedWaitMinutes() + " min wait)"
                    : "";
            item.setEtaOrScheduleText("Queue #" + appt.getQueuePosition() + waitText);
        } else {
            item.setEtaOrScheduleText("Slot: " + appt.getAppointmentDate() + " at " + appt.getAppointmentTime());
        }
        item.setActorSummary("Patient: " + (appt.getPatientName() != null ? appt.getPatientName() : "Patient"));
        item.setDetailSummary(appt.getReason() != null && !appt.getReason().isEmpty()
                ? appt.getReason() : "Scheduled Clinical Consultation");
        item.setUpdatedAt(appt.getUpdatedAt());

        List<TrackingStatusEvent> history = new ArrayList<>(appt.getTrackingHistory());
        if (history.isEmpty()) {
            history.add(new TrackingStatusEvent(
                    appt.getStatus(),
                    appt.getUpdatedAt(),
                    appt.getDoctorUid(),
                    appt.getDoctorName(),
                    "CLINICAL",
                    "Appointment on " + appt.getAppointmentDate() + " at " + appt.getAppointmentTime()
            ));
        }
        item.setTimeline(history);
        return item;
    }

    @NonNull
    public UnifiedTrackingItem projectOperation(@NonNull Operation op, @NonNull UserAccount viewer) {
        UnifiedTrackingItem item = new UnifiedTrackingItem();
        item.setItemId(op.getOperationId());
        item.setHospitalId(op.getHospitalId() != null ? op.getHospitalId() : "hosp_arohi_central");
        item.setCategory(TrackingCategory.OPERATION);

        boolean isPatientSelf = viewer.getUid() != null && viewer.getUid().equals(op.getPatientUid());
        boolean isClinicalOrAdmin = viewer.getUserRoleEnum() == UserRole.ADMIN
                || viewer.getUserRoleEnum() == UserRole.DOCTOR
                || viewer.getUserRoleEnum() == UserRole.NURSE;

        item.setTitle(op.getProcedureName() != null ? op.getProcedureName() : "OT Procedure");
        item.setSubtitle(op.getOtRoomName() + " • " + op.getDepartment());
        item.setCurrentStatus(op.getStatus());
        item.setExpectedStages(Arrays.asList(
                Operation.STATUS_SCHEDULED,
                Operation.STATUS_PREPARATION,
                Operation.STATUS_SHIFTED_TO_OT,
                Operation.STATUS_IN_PROGRESS,
                Operation.STATUS_COMPLETED,
                Operation.STATUS_RECOVERY,
                Operation.STATUS_FOLLOW_UP
        ));
        item.setCurrentStepIndex(op.getStatusStepIndex());
        item.setTotalSteps(item.getExpectedStages().size());
        boolean terminal = Operation.STATUS_FOLLOW_UP.equalsIgnoreCase(op.getStatus())
                || Operation.STATUS_CANCELLED.equalsIgnoreCase(op.getStatus());
        item.setTerminal(terminal);
        item.setActive(!terminal);
        item.setHasLiveGps(false);
        item.setEtaOrScheduleText(op.getOperationDate() + " • " + op.getOperationTime()
                + " (" + op.getEstimatedDurationMinutes() + " min)");
        item.setActorSummary("Surgeon: " + op.getDoctorName()
                + (op.getNurseName() != null && !op.getNurseName().isEmpty() ? " • Nurse: " + op.getNurseName() : ""));

        // Strictly protect sensitive clinical/surgical notes from unauthorized family viewers
        if (isClinicalOrAdmin || isPatientSelf) {
            String notes = op.getPostOpNotes() != null && !op.getPostOpNotes().isEmpty()
                    ? op.getPostOpNotes() : "OT Checklist & Progress Tracked";
            item.setDetailSummary(notes);
        } else {
            item.setDetailSummary(op.toFamilyPermittedSummary());
        }
        item.setUpdatedAt(op.getUpdatedAt());

        List<TrackingStatusEvent> history = new ArrayList<>();
        if (op.getTrackingHistory() != null && !op.getTrackingHistory().isEmpty()) {
            for (TrackingStatusEvent ev : op.getTrackingHistory()) {
                TrackingStatusEvent copy = new TrackingStatusEvent(
                        ev.getStatus(),
                        ev.getTimestamp(),
                        ev.getUpdatedByUid(),
                        ev.getUpdatedByName(),
                        ev.getUpdatedByRole(),
                        (isClinicalOrAdmin || isPatientSelf)
                                ? ev.getNotes()
                                : "OT Stage updated (clinical notes restricted for privacy)."
                );
                history.add(copy);
            }
        } else {
            history.add(new TrackingStatusEvent(
                    op.getStatus(), op.getUpdatedAt(), op.getDoctorUid(), op.getDoctorName(),
                    "OT_TEAM", "Operation stage: " + op.getStatus()
            ));
        }
        item.setTimeline(history);
        return item;
    }

    @NonNull
    public UnifiedTrackingItem projectHospitalBill(@NonNull HospitalBill bill) {
        UnifiedTrackingItem item = new UnifiedTrackingItem();
        item.setItemId(bill.getBillId());
        item.setHospitalId(bill.getHospitalId());
        item.setCategory(TrackingCategory.PAYMENT);
        item.setTitle(bill.getBillNumber() != null ? bill.getBillNumber() : "Bill " + shortId(bill.getBillId()));
        item.setSubtitle(bill.getCategory() + " • " + String.format(Locale.US, "৳ %.2f", bill.getTotalAmountBdt()));
        item.setCurrentStatus(bill.getStatus());
        item.setExpectedStages(Arrays.asList(
                PaymentStatus.PENDING.name(),
                PaymentStatus.PROCESSING.name(),
                PaymentStatus.SUCCESSFUL.name()
        ));
        PaymentStatus ps = PaymentStatus.fromKey(bill.getStatus());
        int step = ps == PaymentStatus.PENDING ? 0 : (ps == PaymentStatus.PROCESSING ? 1 : 2);
        item.setCurrentStepIndex(step);
        item.setTotalSteps(3);
        boolean terminal = ps.isTerminal();
        item.setTerminal(terminal);
        item.setActive(!terminal);
        item.setHasLiveGps(false);
        item.setEtaOrScheduleText(bill.getReceiptNumber() != null && !bill.getReceiptNumber().isEmpty()
                ? "Receipt: " + bill.getReceiptNumber()
                : "Gateway: " + (bill.getPaymentGateway() != null && !bill.getPaymentGateway().isEmpty()
                ? bill.getPaymentGateway() : "Awaiting checkout"));
        item.setActorSummary("Patient: " + bill.getPatientName());
        item.setDetailSummary(bill.getNotes() != null && !bill.getNotes().isEmpty()
                ? bill.getNotes() : "Hospital Billing Record");
        item.setUpdatedAt(bill.getPaidAt() > 0 ? bill.getPaidAt() : bill.getCreatedAt());

        List<TrackingStatusEvent> timeline = new ArrayList<>();
        timeline.add(new TrackingStatusEvent(
                PaymentStatus.PENDING.name(), bill.getCreatedAt(),
                bill.getDoctorUid(), bill.getDoctorName(), "BILLING", "Invoice generated"
        ));
        if (bill.getPaidAt() > 0) {
            timeline.add(new TrackingStatusEvent(
                    bill.getStatus(), bill.getPaidAt(),
                    bill.getPatientUid(), bill.getPatientName(), "GATEWAY",
                    "Transaction " + bill.getTransactionId()
            ));
        }
        item.setTimeline(timeline);
        return item;
    }

    @NonNull
    public UnifiedTrackingItem projectAmbulanceRequest(@NonNull AmbulanceRequest req) {
        UnifiedTrackingItem item = new UnifiedTrackingItem();
        item.setItemId(req.getRequestId());
        item.setHospitalId(req.getHospitalId());
        item.setCategory(TrackingCategory.AMBULANCE);
        item.setTitle("Ambulance Dispatch (" + req.getPriorityLevel() + ")");
        item.setSubtitle(req.getHospitalName() != null ? req.getHospitalName() : "Emergency Dispatch");
        item.setCurrentStatus(req.getStatus());
        item.setExpectedStages(AmbulanceRequest.WORKFLOW_STAGES);
        item.setCurrentStepIndex(req.getStatusStepIndex());
        item.setTotalSteps(AmbulanceRequest.WORKFLOW_STAGES.size());
        item.setTerminal(req.isTerminal());
        item.setActive(!req.isTerminal());
        item.setHasLiveGps(req.hasActiveLiveTelemetry());

        if (req.hasActiveLiveTelemetry() && req.getEtaMinutes() > 0) {
            item.setEtaOrScheduleText("Live GPS Active • ETA ~" + req.getEtaMinutes() + " min");
        } else if (req.hasActiveLiveTelemetry()) {
            item.setEtaOrScheduleText("Live Ambulance GPS Active");
        } else if (req.isTerminal()) {
            item.setEtaOrScheduleText("Session ended • Live GPS stopped");
        } else {
            item.setEtaOrScheduleText("Awaiting authorized ambulance GPS telemetry");
        }

        item.setActorSummary(req.getDriverName() != null && !req.getDriverName().isEmpty()
                ? "Driver: " + req.getDriverName() + " (" + req.getVehicleNumber() + ")"
                : "Patient: " + req.getPatientName());
        item.setDetailSummary("Pickup: " + (req.getPickupAddress() != null ? req.getPickupAddress() : "Pending location"));
        item.setUpdatedAt(req.getUpdatedAt());
        item.setTimeline(req.getTrackingHistory());
        return item;
    }

    // =========================================================================
    // AMBULANCE REQUEST LOCAL + FIRESTORE PERSISTENCE (Foundation for 14.4)
    // =========================================================================

    @NonNull
    public List<AmbulanceRequest> getLocalAmbulanceRequests(@NonNull String hospitalId) {
        String raw = prefs.getString(KEY_AMBULANCE_PREFIX + hospitalId, "[]");
        List<AmbulanceRequest> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Map<String, Object> map = jsonObjectToMap(obj);
                AmbulanceRequest req = AmbulanceRequest.fromMap(map);
                if (req != null) list.add(req);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public void saveAmbulanceRequest(@NonNull AmbulanceRequest req, @Nullable AuthCallback<AmbulanceRequest> callback) {
        String hospitalId = req.getHospitalId() != null ? req.getHospitalId() : "hosp_arohi_central";
        List<AmbulanceRequest> list = getLocalAmbulanceRequests(hospitalId);
        boolean replaced = false;
        for (int i = 0; i < list.size(); i++) {
            if (req.getRequestId().equals(list.get(i).getRequestId())) {
                list.set(i, req);
                replaced = true;
                break;
            }
        }
        if (!replaced) list.add(0, req);
        saveLocalAmbulanceList(hospitalId, list);

        if (firestore != null) {
            firestore.collection(FirestoreCollections.HOSPITALS)
                    .document(hospitalId)
                    .collection(FirestoreCollections.AMBULANCE_REQUESTS)
                    .document(req.getRequestId())
                    .set(req.toMap());
        }
        if (callback != null) callback.onSuccess(req);
    }

    private void saveLocalAmbulanceList(String hospitalId, List<AmbulanceRequest> list) {
        JSONArray arr = new JSONArray();
        for (AmbulanceRequest r : list) {
            arr.put(new JSONObject(r.toMap()));
        }
        prefs.edit().putString(KEY_AMBULANCE_PREFIX + hospitalId, arr.toString()).apply();
    }

    private Map<String, Object> jsonObjectToMap(JSONObject obj) throws Exception {
        Map<String, Object> map = new HashMap<>();
        java.util.Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object val = obj.get(key);
            if (val instanceof JSONArray) {
                map.put(key, jsonArrayToList((JSONArray) val));
            } else if (val instanceof JSONObject) {
                map.put(key, jsonObjectToMap((JSONObject) val));
            } else {
                map.put(key, val);
            }
        }
        return map;
    }

    private List<Object> jsonArrayToList(JSONArray arr) throws Exception {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            Object val = arr.get(i);
            if (val instanceof JSONArray) {
                list.add(jsonArrayToList((JSONArray) val));
            } else if (val instanceof JSONObject) {
                list.add(jsonObjectToMap((JSONObject) val));
            } else {
                list.add(val);
            }
        }
        return list;
    }

    private String shortId(String raw) {
        if (raw == null) return "N/A";
        return raw.length() > 10 ? raw.substring(raw.length() - 8).toUpperCase(Locale.US) : raw.toUpperCase(Locale.US);
    }
}
