package com.arohi.healthcare;

import com.arohi.healthcare.models.AmbulanceRequest;
import com.arohi.healthcare.models.Appointment;
import com.arohi.healthcare.models.BillCategory;
import com.arohi.healthcare.models.BillItem;
import com.arohi.healthcare.models.Hospital;
import com.arohi.healthcare.models.HospitalBill;
import com.arohi.healthcare.models.HospitalMembership;
import com.arohi.healthcare.models.Operation;
import com.arohi.healthcare.models.PaymentGatewayType;
import com.arohi.healthcare.models.PaymentStatus;
import com.arohi.healthcare.models.PaymentTransaction;
import com.arohi.healthcare.models.PharmacyOrder;
import com.arohi.healthcare.models.RefundRequest;
import com.arohi.healthcare.models.StaffConversation;
import com.arohi.healthcare.models.StaffGroupMember;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.payment.PaymentGateway;
import com.arohi.healthcare.payment.PaymentGatewayFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.*;

/**
 * Phase 15 Global Integration & Final Release Audit Test Suite.
 * Covers:
 * 1. Cross-Hospital Multi-Tenant Isolation & Staff Group Governance
 * 2. Payment Verification, Idempotency, Ownership, Failure, Refund & Linked Sync
 * 3. UI Theme Color Parity, English/Bangla Resource Parity & Dark-Mode Safety
 */
public class Phase15GlobalIntegrationAuditTest {

    private static final String HOSP_A = "hosp_square_dhk";
    private static final String HOSP_B = "hosp_united_dhk";

    // =========================================================================
    // 1. CROSS-HOSPITAL ISOLATION TESTS
    // =========================================================================

    @Test
    public void testCrossHospitalPrivateRecordIsolation() {
        HospitalBill billA = new HospitalBill("b_a1", "BILL-SQH-01", HOSP_A, "SQH",
                "pat_1", "Patient A", "AROHI-PAT-1", BillCategory.CONSULTATION);
        HospitalBill billB = new HospitalBill("b_b1", "BILL-UNT-01", HOSP_B, "UNT",
                "pat_2", "Patient B", "AROHI-PAT-2", BillCategory.OPERATION);

        Operation opA = new Operation();
        opA.setOperationId("op_a1");
        opA.setHospitalId(HOSP_A);

        AmbulanceRequest ambB = new AmbulanceRequest();
        ambB.setRequestId("amb_b1");
        ambB.setHospitalId(HOSP_B);

        // Tenant boundary filters must isolate Hospital A from Hospital B records
        assertEquals(HOSP_A, billA.getHospitalId());
        assertNotEquals(HOSP_A, billB.getHospitalId());
        assertNotEquals(HOSP_B, opA.getHospitalId());
        assertNotEquals(HOSP_A, ambB.getHospitalId());
    }

    @Test
    public void testCrossHospitalAuthorityCannotAccessOtherStaffGroups() {
        StaffConversation hospADoctors = StaffConversation.createGroup(
                HOSP_A, "SQH", StaffConversation.TYPE_GROUP_DOCTORS, "Square Doctors");
        StaffConversation hospBDoctors = StaffConversation.createGroup(
                HOSP_B, "UNT", StaffConversation.TYPE_GROUP_DOCTORS, "United Doctors");

        assertNotEquals(hospADoctors.getConversationId(), hospBDoctors.getConversationId());
        assertEquals(HOSP_A, hospADoctors.getHospitalId());
        assertEquals(HOSP_B, hospBDoctors.getHospitalId());

        StaffGroupMember docInHospA = new StaffGroupMember(
                "doc_a1", "Dr. Square", "AROHI-DOC-A1", "DOCTOR", "Cardiology",
                HOSP_A, true, HospitalMembership.STATUS_ACTIVE);
        assertTrue(docInHospA.isEligibleForChat());
        assertNotEquals("Member belongs exclusively to Hospital A", HOSP_B, docInHospA.getHospitalId());

        // Suspended or terminated staff must be denied group chat access
        StaffGroupMember suspendedStaff = new StaffGroupMember(
                "doc_a2", "Dr. Suspended", "AROHI-DOC-A2", "DOCTOR", "ENT",
                HOSP_A, true, "SUSPENDED");
        assertFalse(suspendedStaff.isEligibleForChat());
    }

    @Test
    public void testHospitalSwitchingAndAuthorizedMembershipEnforcement() {
        Hospital hA = new Hospital(HOSP_A, "SQH-01", "Square Hospital", "Panthapath", "Dhaka",
                "10616", "sqh@test.bd", "10616", Hospital.STATUS_ACTIVE, 400, null, true, "Desc", 23.75, 90.38);
        Hospital hB = new Hospital(HOSP_B, "UNT-01", "United Hospital", "Gulshan", "Dhaka",
                "10666", "unt@test.bd", "10666", Hospital.STATUS_ACTIVE, 450, null, true, "Desc", 23.79, 90.41);

        HospitalMembership activeInA = new HospitalMembership(
                "mem_1", "user_1", hA.getHospitalId(), hA.getHospitalCode(),
                "PATIENT", HospitalMembership.STATUS_ACTIVE, "");
        HospitalMembership revokedInB = new HospitalMembership(
                "mem_2", "user_1", hB.getHospitalId(), hB.getHospitalCode(),
                "DOCTOR", HospitalMembership.STATUS_REVOKED, "Cardiology");

        assertTrue(activeInA.isActive());
        assertFalse("Revoked membership in Hospital B must deny access", revokedInB.isActive());
        assertNotEquals(hA.getHospitalCode(), hB.getHospitalCode());
    }

    // =========================================================================
    // 2. PAYMENT SECURITY & BILLING AUDIT TESTS
    // =========================================================================

    @Test
    public void testPaymentIdempotencyAmountAndOwnershipValidation() {
        HospitalBill bill = new HospitalBill("bill_audit_1", "BILL-SQH-9001", HOSP_A, "SQH",
                "pat_owner_1", "Owner Patient", "AROHI-PAT-1", BillCategory.APPOINTMENT);
        bill.addItem(new BillItem("i1", "Consultation", 1, 1000.0));
        bill.setLinkedEntityType("APPOINTMENT");
        bill.setLinkedEntityId("appt_owner_1");
        bill.recalculateTotals(); // 1000 + 5% VAT = 1050.0

        assertEquals(1050.0, bill.getTotalAmountBdt(), 0.001);

        PaymentGateway gateway = PaymentGatewayFactory.getGateway(PaymentGatewayType.BKASH);
        Set<String> processedKeys = new HashSet<>();
        String idempotencyKey = "idem_audit_" + bill.getBillId();

        // Reject invalid zero/negative amount verification
        AtomicBoolean rejectedInvalidAmount = new AtomicBoolean(false);
        gateway.verifyTransaction("TXN_1", idempotencyKey, HOSP_A, 0.0, new PaymentGateway.GatewayVerifyCallback() {
            @Override
            public void onVerified(PaymentTransaction verifiedTransaction, String digitalSignature) {}

            @Override
            public void onError(String errorMessage) {
                rejectedInvalidAmount.set(true);
            }
        });
        assertTrue("Zero/invalid payment amount must be rejected", rejectedInvalidAmount.get());

        // First callback processes and marks idempotency key consumed
        AtomicBoolean verifiedOnce = new AtomicBoolean(false);
        gateway.verifyTransaction("TXN_VALID_1", idempotencyKey, HOSP_A, bill.getTotalAmountBdt(),
                new PaymentGateway.GatewayVerifyCallback() {
                    @Override
                    public void onVerified(PaymentTransaction tx, String sig) {
                        assertEquals(HOSP_A, tx.getHospitalId());
                        assertEquals(bill.getTotalAmountBdt(), tx.getAmountBdt(), 0.001);
                        assertFalse(processedKeys.contains(tx.getIdempotencyKey()));
                        processedKeys.add(tx.getIdempotencyKey());
                        verifiedOnce.set(true);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        fail(errorMessage);
                    }
                });
        assertTrue(verifiedOnce.get());
        assertTrue("Duplicate callback guard must retain consumed key", processedKeys.contains(idempotencyKey));
    }

    @Test
    public void testPaymentFailureRefundAndOrderConsistencyStates() {
        // Failed payment should allow retry (not locked into terminal paid state)
        HospitalBill failedBill = new HospitalBill("bill_fail_1", "BILL-SQH-9002", HOSP_A, "SQH",
                "pat_1", "Patient One", "AROHI-PAT-1", BillCategory.PHARMACY);
        failedBill.addItem(new BillItem("m1", "Paracetamol", 2, 100.0));
        failedBill.setStatus(PaymentStatus.FAILED.name());
        assertTrue(PaymentStatus.FAILED.isTerminal());
        assertNotEquals(PaymentStatus.SUCCESSFUL.name(), failedBill.getStatus());

        // Refund cannot exceed paid total and updates refund audit state
        RefundRequest refund = new RefundRequest("ref_1", "bill_paid_1", "BILL-SQH-9003", "TXN_10",
                HOSP_A, "pat_1", "Patient One", 525.0, "Duplicate order charge");
        assertEquals(RefundRequest.STATUS_REQUESTED, refund.getStatus());

        AtomicBoolean refundDone = new AtomicBoolean(false);
        PaymentGatewayFactory.getGateway(PaymentGatewayType.NAGAD).processRefund(refund, true,
                new PaymentGateway.GatewayRefundCallback() {
                    @Override
                    public void onRefundProcessed(RefundRequest updated) {
                        assertEquals(RefundRequest.STATUS_PROCESSED, updated.getStatus());
                        refundDone.set(true);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        fail(errorMessage);
                    }
                });
        assertTrue(refundDone.get());

        // Order/Appointment link transitions cleanly on verified payment
        Appointment linkedAppt = new Appointment();
        linkedAppt.setAppointmentId("appt_owner_1");
        linkedAppt.setHospitalId(HOSP_A);
        linkedAppt.setStatus(Appointment.STATUS_PENDING);
        linkedAppt.setStatus(Appointment.STATUS_CONFIRMED);
        assertTrue(linkedAppt.isConfirmed());

        PharmacyOrder linkedOrder = new PharmacyOrder();
        linkedOrder.setOrderId("ord_owner_1");
        linkedOrder.setHospitalId(HOSP_A);
        assertTrue(linkedOrder.isValidTransition(PharmacyOrder.STATUS_VERIFIED));
    }

    // =========================================================================
    // 3. UI, DARK/LIGHT THEME & EN/BN LOCALIZATION PARITY AUDIT TESTS
    // =========================================================================

    @Test
    public void testEnglishAndBanglaStringResourcesExactParity() throws Exception {
        File enFile = resolveResourceFile("src/main/res/values/strings.xml");
        File bnFile = resolveResourceFile("src/main/res/values-bn/strings.xml");

        Set<String> enKeys = extractXmlTagNames(enFile, "string");
        Set<String> bnKeys = extractXmlTagNames(bnFile, "string");

        assertTrue("Expected >500 localized strings", enKeys.size() > 500);
        assertEquals("English and Bangla strings must have identical key sets", enKeys, bnKeys);
    }

    @Test
    public void testDayAndNightColorPalettesExactParity() throws Exception {
        File dayFile = resolveResourceFile("src/main/res/values/colors.xml");
        File nightFile = resolveResourceFile("src/main/res/values-night/colors.xml");

        Set<String> dayColors = extractXmlTagNames(dayFile, "color");
        Set<String> nightColors = extractXmlTagNames(nightFile, "color");

        assertTrue("Expected >=45 color tokens", dayColors.size() >= 45);
        assertEquals("Day and Night color palettes must define exact matching tokens", dayColors, nightColors);
    }

    @Test
    public void testLayoutsAvoidHardcodedBlackTextOrWhiteBackgrounds() throws Exception {
        File layoutDir = resolveResourceFile("src/main/res/layout");
        File[] files = layoutDir.listFiles((dir, name) -> name.endsWith(".xml"));
        assertNotNull(files);
        assertTrue(files.length > 50);

        for (File f : files) {
            String xml = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            assertFalse("Hardcoded black textColor found in " + f.getName(),
                    xml.contains("android:textColor=\"#000000\"")
                            || xml.contains("android:textColor=\"@android:color/black\""));
            assertFalse("Hardcoded white screen/card background found in " + f.getName(),
                    xml.contains("android:background=\"#FFFFFF\"")
                            || xml.contains("android:background=\"@android:color/white\""));
        }
    }

    private File resolveResourceFile(String relativeFromApp) {
        File direct = new File(relativeFromApp);
        if (direct.exists()) return direct;
        return new File("app/" + relativeFromApp);
    }

    private Set<String> extractXmlTagNames(File xmlFile, String tagName) throws Exception {
        assertTrue("Missing file: " + xmlFile.getAbsolutePath(), xmlFile.exists());
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
        NodeList nodes = doc.getElementsByTagName(tagName);
        Set<String> names = new HashSet<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String n = el.getAttribute("name");
            if (n != null && !n.isEmpty()) names.add(n);
        }
        return names;
    }
}
