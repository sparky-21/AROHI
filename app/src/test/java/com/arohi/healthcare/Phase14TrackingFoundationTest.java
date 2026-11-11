package com.arohi.healthcare;

import com.arohi.healthcare.models.AmbulanceRequest;
import com.arohi.healthcare.models.AppNotification;
import com.arohi.healthcare.models.Appointment;
import com.arohi.healthcare.models.HospitalBill;
import com.arohi.healthcare.models.Operation;
import com.arohi.healthcare.models.PharmacyOrder;
import com.arohi.healthcare.models.TrackingCategory;
import com.arohi.healthcare.models.TrackingStatusEvent;
import com.arohi.healthcare.models.UnifiedTrackingItem;
import com.arohi.healthcare.services.TrackingNotificationDispatcher;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Phase 14 Sub-Phase 14.1 Unit Tests:
 * Complete Tracking System Foundation (Models, Transitions, Privacy, Telemetry Safety).
 */
public class Phase14TrackingFoundationTest {

    @Test
    public void testPharmacyHomeDeliverySevenStageProgression() {
        PharmacyOrder order = new PharmacyOrder();
        order.setDeliveryType(PharmacyOrder.DELIVERY_HOME);
        order.setStatus(PharmacyOrder.STATUS_PLACED);

        assertTrue(order.isHomeDelivery());
        assertEquals(7, order.getExpectedStages().size());
        assertEquals(0, order.getStatusStepIndex());

        assertTrue(order.isValidTransition(PharmacyOrder.STATUS_VERIFIED));
        order.setStatus(PharmacyOrder.STATUS_VERIFIED);
        assertTrue(order.isValidTransition(PharmacyOrder.STATUS_PREPARING));
        order.setStatus(PharmacyOrder.STATUS_PREPARING);
        assertTrue(order.isValidTransition(PharmacyOrder.STATUS_READY));
        order.setStatus(PharmacyOrder.STATUS_READY);
        assertTrue(order.isValidTransition(PharmacyOrder.STATUS_DISPATCHED));
        order.setStatus(PharmacyOrder.STATUS_DISPATCHED);
        assertEquals(4, order.getStatusStepIndex());

        assertTrue(order.isValidTransition(PharmacyOrder.STATUS_OUT_FOR_DELIVERY));
        order.setStatus(PharmacyOrder.STATUS_OUT_FOR_DELIVERY);
        assertEquals(5, order.getStatusStepIndex());

        assertTrue(order.isValidTransition(PharmacyOrder.STATUS_DELIVERED));
        order.setStatus(PharmacyOrder.STATUS_DELIVERED);
        assertEquals(6, order.getStatusStepIndex());

        // Terminal state cannot advance or revert
        assertFalse(order.isValidTransition(PharmacyOrder.STATUS_DISPATCHED));
        assertFalse(order.isValidTransition(PharmacyOrder.STATUS_CANCELLED));
    }

    @Test
    public void testPharmacyLocationSharingAutoStopsOnDelivery() {
        PharmacyOrder order = new PharmacyOrder();
        order.setDeliveryType(PharmacyOrder.DELIVERY_HOME);
        order.setStatus(PharmacyOrder.STATUS_OUT_FOR_DELIVERY);
        order.setLiveLat(23.7509);
        order.setLiveLng(90.3935);
        order.setLocationSharingActive(true);
        assertTrue(order.hasActiveLiveLocation());

        order.setStatus(PharmacyOrder.STATUS_DELIVERED);
        order.recordTrackingEvent(PharmacyOrder.STATUS_DELIVERED, "drv_1", "Rider", "DELIVERY", "Handed to patient");
        assertFalse("Live GPS must stop after delivery", order.isLocationSharingActive());
        assertFalse(order.hasActiveLiveLocation());
    }

    @Test
    public void testAppointmentQueueAndSixStageTransitions() {
        Appointment appt = new Appointment();
        appt.setHospitalId("hosp_square_dhk");
        appt.setStatus(Appointment.STATUS_REQUESTED);

        assertFalse("Queue data should not be reported before check-in/queue assignment", appt.hasLiveQueueData());

        assertTrue(appt.isValidTransition(Appointment.STATUS_CONFIRMED));
        appt.setStatus(Appointment.STATUS_CONFIRMED);
        assertTrue(appt.isValidTransition(Appointment.STATUS_CHECKED_IN));
        appt.setStatus(Appointment.STATUS_CHECKED_IN);
        assertTrue(appt.isValidTransition(Appointment.STATUS_WAITING));
        appt.setStatus(Appointment.STATUS_WAITING);
        appt.setQueuePosition(3);
        appt.setEstimatedWaitMinutes(25);
        assertTrue(appt.hasLiveQueueData());
        assertEquals(3, appt.getStatusStepIndex());

        assertTrue(appt.isValidTransition(Appointment.STATUS_IN_CONSULTATION));
        appt.setStatus(Appointment.STATUS_IN_CONSULTATION);
        assertTrue(appt.isValidTransition(Appointment.STATUS_COMPLETED));
        appt.setStatus(Appointment.STATUS_COMPLETED);
        assertEquals(5, appt.getStatusStepIndex());
        assertFalse(appt.hasLiveQueueData());
    }

    @Test
    public void testOperationSevenStagesAndFamilyPrivacyMasking() {
        Operation op = new Operation(
                "op_901", "Laparoscopic Cholecystectomy", "General Surgery",
                "ot_1", "OT Room 1", "2026-11-12", "10:00 AM", 90,
                "pat_uid_1", "AROHI-PAT-1", "Sadia Islam",
                "doc_uid_1", "AROHI-DOC-1", "Dr. Rahman", "Surgery",
                "nurse_uid_1", "Nurse Ayesha"
        );
        op.setHospitalId("hosp_square_dhk");
        op.setSurgicalNotes("CONFIDENTIAL: Intra-op arterial clip detail");
        op.setPostOpNotes("CONFIDENTIAL: Internal anesthesia sensitivity");
        op.setAuthorizedFamilyUids(Collections.singletonList("fam_uid_spouse"));

        assertTrue(op.isValidTransition(Operation.STATUS_PREPARATION));
        op.setStatus(Operation.STATUS_PREPARATION);
        assertTrue(op.isValidTransition(Operation.STATUS_SHIFTED_TO_OT));
        op.setStatus(Operation.STATUS_SHIFTED_TO_OT);
        assertEquals(2, op.getStatusStepIndex());
        assertTrue(op.isValidTransition(Operation.STATUS_IN_PROGRESS));
        op.setStatus(Operation.STATUS_IN_PROGRESS);
        assertTrue(op.isValidTransition(Operation.STATUS_COMPLETED));
        op.setStatus(Operation.STATUS_COMPLETED);
        assertTrue(op.isValidTransition(Operation.STATUS_RECOVERY));
        op.setStatus(Operation.STATUS_RECOVERY);
        assertTrue(op.isValidTransition(Operation.STATUS_FOLLOW_UP));
        op.setStatus(Operation.STATUS_FOLLOW_UP);
        assertEquals(6, op.getStatusStepIndex());

        // Family authorization & privacy check
        assertTrue(op.isFamilyViewerAuthorized("pat_uid_1"));
        assertTrue(op.isFamilyViewerAuthorized("fam_uid_spouse"));
        assertFalse("Unauthorized third-party user must be denied", op.isFamilyViewerAuthorized("other_uid"));

        String safeSummary = op.toFamilyPermittedSummary();
        assertFalse(safeSummary.contains("CONFIDENTIAL"));
        assertTrue(safeSummary.contains("OT Room 1"));
    }

    @Test
    public void testAmbulanceEightStagesTelemetryGuardAndSerialization() {
        AmbulanceRequest req = new AmbulanceRequest(
                "amb_001", "hosp_united_dhk", "United Hospital",
                "pat_1", "AROHI-PAT-1", "Karim Ahmed", "+8801711000000",
                "House 12, Road 5, Gulshan 2", 23.7925, 90.4078,
                "Acute chest pain", "CRITICAL"
        );

        // Zero fake GPS telemetry by default
        assertFalse(req.hasActiveLiveTelemetry());
        assertNull(req.getCurrentLat());
        assertNull(req.getCurrentLng());
        assertEquals(0, req.getStatusStepIndex());

        String[] steps = {
                AmbulanceRequest.STATUS_ACCEPTED,
                AmbulanceRequest.STATUS_AMBULANCE_ASSIGNED,
                AmbulanceRequest.STATUS_ON_THE_WAY,
                AmbulanceRequest.STATUS_ARRIVED,
                AmbulanceRequest.STATUS_PATIENT_PICKED_UP,
                AmbulanceRequest.STATUS_HOSPITAL_REACHED,
                AmbulanceRequest.STATUS_COMPLETED
        };

        for (String next : steps) {
            assertTrue("Expected valid transition from " + req.getStatus() + " to " + next,
                    req.isValidTransition(next));
            req.updateStatusWithAudit(next, "auth_1", "Dispatch Desk", "ADMIN", "Progressed to " + next);
            if (AmbulanceRequest.STATUS_ON_THE_WAY.equals(next)) {
                // Simulate authorized GPS coordinate update while en route
                req.setCurrentLat(23.7980);
                req.setCurrentLng(90.4120);
                assertTrue(req.hasActiveLiveTelemetry());
            }
        }

        // Terminal state strictly terminates live GPS authorization
        assertTrue(req.isTerminal());
        assertFalse(req.isLocationSharingAuthorized());
        assertFalse(req.hasActiveLiveTelemetry());
        assertEquals(7, req.getStatusStepIndex());
        assertEquals(8, req.getTrackingHistory().size());

        Map<String, Object> map = req.toMap();
        AmbulanceRequest restored = AmbulanceRequest.fromMap(map);
        assertNotNull(restored);
        assertEquals("amb_001", restored.getRequestId());
        assertEquals("hosp_united_dhk", restored.getHospitalId());
        assertEquals(8, restored.getTrackingHistory().size());
        assertFalse(restored.isLocationSharingAuthorized());
    }

    @Test
    public void testUnifiedTrackingProjectionAndNotificationTypeMapping() {
        UnifiedTrackingItem item = new UnifiedTrackingItem();
        item.setCategory(TrackingCategory.AMBULANCE);
        item.setCurrentStepIndex(3);
        item.setTotalSteps(8);

        assertEquals(43, item.getProgressPercent());

        TrackingStatusEvent ev = new TrackingStatusEvent("On the Way", 1700000000L, "drv_1", "Driver", "DRIVER", "Departed bay");
        ev.setLatitude(23.78);
        ev.setLongitude(90.41);
        TrackingStatusEvent restored = TrackingStatusEvent.fromMap(ev.toMap());
        assertNotNull(restored);
        assertEquals(23.78, restored.getLatitude(), 0.0001);

        HospitalBill bill = new HospitalBill();
        bill.setLinkedEntityType("PHARMACY_ORDER");
        bill.setLinkedEntityId("ord_555");
        Map<String, Object> billMap = bill.toMap();
        assertEquals("PHARMACY_ORDER", billMap.get("linkedEntityType"));
        assertEquals("ord_555", billMap.get("linkedEntityId"));

        assertEquals(AppNotification.TYPE_PHARMACY_TRACKING,
                TrackingNotificationDispatcher.mapCategoryToNotificationType(TrackingCategory.PHARMACY));
        assertEquals(AppNotification.TYPE_AMBULANCE_TRACKING,
                TrackingNotificationDispatcher.mapCategoryToNotificationType(TrackingCategory.AMBULANCE));
    }
}
