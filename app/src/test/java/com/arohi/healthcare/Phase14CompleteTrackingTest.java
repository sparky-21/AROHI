package com.arohi.healthcare;

import com.arohi.healthcare.models.AmbulanceRequest;
import com.arohi.healthcare.models.Appointment;
import com.arohi.healthcare.models.BillCategory;
import com.arohi.healthcare.models.HospitalBill;
import com.arohi.healthcare.models.Operation;
import com.arohi.healthcare.models.PaymentStatus;
import com.arohi.healthcare.models.PharmacyOrder;
import com.arohi.healthcare.models.TrackingCategory;
import com.arohi.healthcare.models.UnifiedTrackingItem;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VerificationStatus;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Unit Tests for Phase 14 Sub-Phases 14.2 - 14.4:
 * - Pharmacy Home Delivery & Partner GPS Telemetry Lifecycle
 * - Appointment Live Queue & Consultation Progression
 * - Operation OT Privacy & Family Authorization
 * - Ambulance Dispatch, Telemetry Authorization & Auto-Stop
 * - Payment-Linked Entity Metadata & Multi-Tenant Hospital Isolation
 */
public class Phase14CompleteTrackingTest {

    @Test
    public void testPharmacyHomeDeliveryPartnerAssignmentAndAutoStop() {
        PharmacyOrder order = new PharmacyOrder();
        order.setHospitalId("hosp_square_dhk");
        order.setDeliveryType(PharmacyOrder.DELIVERY_HOME);
        order.setStatus(PharmacyOrder.STATUS_READY);

        // Ready -> Dispatched
        assertTrue(order.isValidTransition(PharmacyOrder.STATUS_DISPATCHED));
        order.setStatus(PharmacyOrder.STATUS_DISPATCHED);
        order.setDeliveryPartnerUid("rider_01");
        order.setDeliveryPartnerName("Kamal Hasan");
        order.setDeliveryPartnerPhone("+8801711223344");
        order.setEstimatedDeliveryTime("25 min");

        // Dispatched -> Out for Delivery with live telemetry
        assertTrue(order.isValidTransition(PharmacyOrder.STATUS_OUT_FOR_DELIVERY));
        order.setStatus(PharmacyOrder.STATUS_OUT_FOR_DELIVERY);
        order.setLiveLat(23.7525);
        order.setLiveLng(90.3812);
        order.setLocationSharingActive(true);
        assertTrue(order.hasActiveLiveLocation());

        // Delivered terminates GPS sharing
        assertTrue(order.isValidTransition(PharmacyOrder.STATUS_DELIVERED));
        order.setStatus(PharmacyOrder.STATUS_DELIVERED);
        order.recordTrackingEvent(PharmacyOrder.STATUS_DELIVERED, "rider_01", "Kamal Hasan", "DELIVERY", "Delivered");
        assertFalse(order.hasActiveLiveLocation());
        assertFalse(order.isLocationSharingActive());
    }

    @Test
    public void testAppointmentRescheduleCancellationAndLiveQueueData() {
        Appointment appt = new Appointment();
        appt.setHospitalId("hosp_united_dhk");
        appt.setStatus(Appointment.STATUS_REQUESTED);

        assertTrue(appt.isValidTransition(Appointment.STATUS_CONFIRMED));
        appt.setStatus(Appointment.STATUS_CONFIRMED);
        assertTrue(appt.isValidTransition(Appointment.STATUS_RESCHEDULED));
        assertTrue(appt.isValidTransition(Appointment.STATUS_CANCELLED));
        assertFalse("Queue data must not exist prior to check-in queue assignment", appt.hasLiveQueueData());

        appt.setStatus(Appointment.STATUS_CHECKED_IN);
        assertTrue(appt.isValidTransition(Appointment.STATUS_WAITING));
        appt.setStatus(Appointment.STATUS_WAITING);
        appt.setQueuePosition(2);
        appt.setEstimatedWaitMinutes(15);
        assertTrue(appt.hasLiveQueueData());

        // Cancellation is terminal and clears active queue visibility
        appt.setStatus(Appointment.STATUS_CANCELLED);
        assertFalse(appt.hasLiveQueueData());
    }

    @Test
    public void testOperationPrivacyAcrossAuthorizedAndUnauthorizedFamily() {
        Operation op = new Operation(
                "op_77", "Appendectomy", "Surgery",
                "ot_2", "OT Room 2", "2026-11-15", "02:00 PM", 60,
                "pat_99", "AROHI-PAT-99", "Tania Akter",
                "doc_55", "AROHI-DOC-55", "Dr. Kabir", "General Surgery",
                "nurse_12", "Nurse Lima"
        );
        op.setHospitalId("hosp_evercare_dhk");
        op.setSurgicalNotes("PRIVATE_CLINICAL_NOTE_DO_NOT_SHOW_FAMILY");
        op.setPostOpNotes("PRIVATE_POST_OP_SENSITIVE_DETAIL");
        op.setAuthorizedFamilyUids(Collections.singletonList("fam_husband_uid"));

        assertTrue(op.isFamilyViewerAuthorized("pat_99"));
        assertTrue(op.isFamilyViewerAuthorized("fam_husband_uid"));
        assertFalse(op.isFamilyViewerAuthorized("stranger_uid"));

        String familySummary = op.toFamilyPermittedSummary();
        assertFalse(familySummary.contains("PRIVATE_CLINICAL_NOTE"));
        assertFalse(familySummary.contains("PRIVATE_POST_OP"));
        assertTrue(familySummary.contains("Dr. Kabir"));
        assertTrue(familySummary.contains("OT Room 2"));
    }

    @Test
    public void testAmbulanceTelemetryAuthorizationAndCancellationRevocation() {
        AmbulanceRequest req = new AmbulanceRequest(
                "amb_500", "hosp_square_dhk", "Square Hospital",
                "pat_10", "AROHI-PAT-10", "Nabil Rahman", "+8801811223344",
                "Panthapath", 23.7529, 90.3816, "Trauma response", "CRITICAL"
        );
        req.setDriverUid("drv_authorized_1");
        req.updateStatusWithAudit(AmbulanceRequest.STATUS_ACCEPTED, "adm_1", "Dispatch", "ADMIN", "Accepted");
        req.updateStatusWithAudit(AmbulanceRequest.STATUS_AMBULANCE_ASSIGNED, "adm_1", "Dispatch", "ADMIN", "Assigned");
        req.updateStatusWithAudit(AmbulanceRequest.STATUS_ON_THE_WAY, "drv_authorized_1", "Driver", "DRIVER", "En route");

        // Still false until actual coordinates arrive from driver app
        assertFalse(req.hasActiveLiveTelemetry());

        req.setCurrentLat(23.7550);
        req.setCurrentLng(90.3840);
        assertTrue(req.hasActiveLiveTelemetry());

        // Cancel or Complete revokes telemetry
        req.updateStatusWithAudit(AmbulanceRequest.STATUS_COMPLETED, "drv_authorized_1", "Driver", "DRIVER", "Completed");
        assertTrue(req.isTerminal());
        assertFalse(req.isLocationSharingAuthorized());
        assertFalse(req.hasActiveLiveTelemetry());
    }

    @Test
    public void testPaymentLinkedEntityAndMultiTenantHospitalBoundary() {
        HospitalBill bill = new HospitalBill(
                "bill_linked_1", "BILL-SQH-202611-7777",
                "hosp_square_dhk", "SQH",
                "pat_1", "Patient One", "AROHI-PAT-1", BillCategory.PHARMACY
        );
        bill.setLinkedEntityType("PHARMACY_ORDER");
        bill.setLinkedEntityId("ord_999");
        bill.setStatus(PaymentStatus.SUCCESSFUL.name());

        assertEquals("PHARMACY_ORDER", bill.getLinkedEntityType());
        assertEquals("ord_999", bill.getLinkedEntityId());

        UnifiedTrackingItem item = new UnifiedTrackingItem();
        item.setHospitalId("hosp_square_dhk");
        item.setCategory(TrackingCategory.PAYMENT);
        assertNotEquals("hosp_united_dhk", item.getHospitalId());

        UserAccount patient = new UserAccount(
                "pat_1", "AROHI-PAT-1", "p@arohi.bd", "Patient One", "017",
                UserRole.PATIENT, VerificationStatus.VERIFIED, true, true
        );
        patient.setHospitalId("hosp_square_dhk");
        assertEquals("hosp_square_dhk", patient.getHospitalId());
    }
}
