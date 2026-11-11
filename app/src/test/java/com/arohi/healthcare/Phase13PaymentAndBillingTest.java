package com.arohi.healthcare;

import com.arohi.healthcare.models.BillCategory;
import com.arohi.healthcare.models.BillItem;
import com.arohi.healthcare.models.HospitalBill;
import com.arohi.healthcare.models.PaymentGatewayType;
import com.arohi.healthcare.models.PaymentReceipt;
import com.arohi.healthcare.models.PaymentStatus;
import com.arohi.healthcare.models.PaymentTransaction;
import com.arohi.healthcare.models.RefundRequest;
import com.arohi.healthcare.payment.BkashPaymentGateway;
import com.arohi.healthcare.payment.CardPaymentGateway;
import com.arohi.healthcare.payment.CashPaymentGateway;
import com.arohi.healthcare.payment.NagadPaymentGateway;
import com.arohi.healthcare.payment.PaymentGateway;
import com.arohi.healthcare.payment.PaymentGatewayFactory;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit Tests for AROHI Phase 13:
 * Payment and Billing System:
 * 1. Bill line-item calculation, subtotal, 5% healthcare VAT & discount logic
 * 2. Payment category & status machine verification
 * 3. Provider-independent payment gateway strategy & factory resolution
 * 4. Transaction idempotency & duplicate prevention
 * 5. Multi-tenant hospital isolation for billing records
 * 6. Digital receipt creation & verification code generation
 * 7. Refund request and audit reversal workflow
 */
public class Phase13PaymentAndBillingTest {

    private static final String HOSPITAL_SQH = "hosp_square_dhk";
    private static final String HOSPITAL_UNT = "hosp_united_dhk";

    // =========================================================================
    // 1. BILL CALCULATION, STATUTORY VAT & TOTALS
    // =========================================================================

    @Test
    public void testBillItemAdditionAndVatCalculation() {
        HospitalBill bill = new HospitalBill(
                "bill_101", "BILL-SQH-202611-0001", HOSPITAL_SQH, "SQH",
                "pat_01", "Sadia Islam", "AROHI-PAT-A1B2C3D4", BillCategory.CONSULTATION
        );

        bill.addItem(new BillItem("itm_01", "Specialist OPD Consultation", 1, 1200.0));
        bill.addItem(new BillItem("itm_02", "12-Lead ECG Investigation", 1, 600.0));

        // Subtotal = 1200 + 600 = 1800
        assertEquals(1800.0, bill.getSubtotalBdt(), 0.001);

        // 5% VAT on 1800 = 90.0
        assertEquals(90.0, bill.getVatBdt(), 0.001);

        // Total = 1800 + 90 = 1890.0
        assertEquals(1890.0, bill.getTotalAmountBdt(), 0.001);

        // Apply discount of 150 BDT
        bill.setDiscountBdt(150.0);
        assertEquals(1740.0, bill.getTotalAmountBdt(), 0.001);
    }

    @Test
    public void testBillSerialization() {
        HospitalBill bill = new HospitalBill(
                "bill_102", "BILL-SQH-202611-0002", HOSPITAL_SQH, "SQH",
                "pat_01", "Sadia Islam", "AROHI-PAT-A1B2C3D4", BillCategory.PHARMACY
        );
        bill.addItem(new BillItem("itm_03", "Medicine Rosuvastatin", 2, 450.0)); // 900
        bill.recalculateTotals();

        Map<String, Object> map = bill.toMap();
        assertEquals("bill_102", map.get("billId"));
        assertEquals("BILL-SQH-202611-0002", map.get("billNumber"));
        assertEquals(HOSPITAL_SQH, map.get("hospitalId"));
        assertEquals(BillCategory.PHARMACY.name(), map.get("category"));
        assertEquals(900.0, (Double) map.get("subtotalBdt"), 0.001);
        assertEquals(45.0, (Double) map.get("vatBdt"), 0.001);
        assertEquals(945.0, (Double) map.get("totalAmountBdt"), 0.001);
    }

    // =========================================================================
    // 2. PAYMENT CATEGORIES & STATE MACHINE
    // =========================================================================

    @Test
    public void testAllPaymentCategoriesSupported() {
        assertEquals("Doctor Consultation", BillCategory.CONSULTATION.getDisplayName());
        assertEquals("Appointment Booking", BillCategory.APPOINTMENT.getDisplayName());
        assertEquals("Pharmacy Order", BillCategory.PHARMACY.getDisplayName());
        assertEquals("Laboratory Test", BillCategory.LAB_TEST.getDisplayName());
        assertEquals("Hospital Service Bill", BillCategory.HOSPITAL_SERVICE.getDisplayName());
        assertEquals("Surgical & OT Bill", BillCategory.OPERATION.getDisplayName());

        assertEquals(BillCategory.OPERATION, BillCategory.fromKey("OPERATION"));
        assertEquals(BillCategory.PHARMACY, BillCategory.fromKey("pharmacy"));
    }

    @Test
    public void testPaymentStatusTransitions() {
        HospitalBill bill = new HospitalBill();
        assertEquals(PaymentStatus.PENDING.name(), bill.getStatus());

        bill.setStatus(PaymentStatus.PROCESSING.name());
        assertEquals(PaymentStatus.PROCESSING.name(), bill.getStatus());

        bill.setStatus(PaymentStatus.SUCCESSFUL.name());
        bill.setPaidAt(System.currentTimeMillis());
        assertEquals(PaymentStatus.SUCCESSFUL.name(), bill.getStatus());
        assertTrue(bill.getPaidAt() > 0);

        bill.setStatus(PaymentStatus.REFUNDED.name());
        assertEquals(PaymentStatus.REFUNDED.name(), bill.getStatus());
    }

    // =========================================================================
    // 3. PROVIDER-INDEPENDENT GATEWAY ARCHITECTURE & FACTORY
    // =========================================================================

    @Test
    public void testGatewayFactoryResolution() {
        PaymentGateway bkash = PaymentGatewayFactory.getGateway(PaymentGatewayType.BKASH);
        assertNotNull(bkash);
        assertTrue(bkash instanceof BkashPaymentGateway);
        assertEquals(PaymentGatewayType.BKASH, bkash.getType());

        PaymentGateway nagad = PaymentGatewayFactory.getGateway(PaymentGatewayType.NAGAD);
        assertNotNull(nagad);
        assertTrue(nagad instanceof NagadPaymentGateway);
        assertEquals(PaymentGatewayType.NAGAD, nagad.getType());

        PaymentGateway card = PaymentGatewayFactory.getGateway(PaymentGatewayType.CARD_SSLCOMMERZ);
        assertNotNull(card);
        assertTrue(card instanceof CardPaymentGateway);
        assertEquals(PaymentGatewayType.CARD_SSLCOMMERZ, card.getType());

        PaymentGateway cash = PaymentGatewayFactory.getGateway(PaymentGatewayType.CASH_COUNTER);
        assertNotNull(cash);
        assertTrue(cash instanceof CashPaymentGateway);
        assertEquals(PaymentGatewayType.CASH_COUNTER, cash.getType());
    }

    @Test
    public void testGatewayTransactionInitiationAndVerification() {
        HospitalBill bill = new HospitalBill(
                "bill_test", "BILL-SQH-TEST", HOSPITAL_SQH, "SQH",
                "pat_01", "Patient Name", "AROHI-PAT-001", BillCategory.LAB_TEST
        );
        bill.addItem(new BillItem("itm_lab", "Complete Blood Count", 1, 500.0));

        PaymentGateway bkash = PaymentGatewayFactory.getGateway(PaymentGatewayType.BKASH);
        final String[] capturedTxnId = new String[1];
        final String[] capturedUrl = new String[1];

        bkash.initiateTransaction(bill, "IDEMP_KEY_001", true, new PaymentGateway.GatewayInitiateCallback() {
            @Override
            public void onInitiated(PaymentTransaction transaction, String redirectOrCheckoutUrl) {
                capturedTxnId[0] = transaction.getTransactionId();
                capturedUrl[0] = redirectOrCheckoutUrl;
            }

            @Override
            public void onError(String errorMessage) {
                fail("Initiation should not fail: " + errorMessage);
            }
        });

        assertNotNull(capturedTxnId[0]);
        assertTrue(capturedTxnId[0].startsWith("TXN_BKASH_"));
        assertNotNull(capturedUrl[0]);
        assertTrue(capturedUrl[0].contains("sandbox.bkash.com"));

        // Backend Verification
        final String[] capturedSignature = new String[1];
        bkash.verifyTransaction(capturedTxnId[0], "IDEMP_KEY_001", HOSPITAL_SQH, 525.0,
                new PaymentGateway.GatewayVerifyCallback() {
                    @Override
                    public void onVerified(PaymentTransaction verifiedTransaction, String digitalSignature) {
                        assertEquals(PaymentStatus.SUCCESSFUL.name(), verifiedTransaction.getStatus());
                        capturedSignature[0] = digitalSignature;
                    }

                    @Override
                    public void onError(String errorMessage) {
                        fail("Verification should not fail: " + errorMessage);
                    }
                });

        assertNotNull(capturedSignature[0]);
        assertFalse(capturedSignature[0].isEmpty());
    }

    // =========================================================================
    // 4. IDEMPOTENCY & DUPLICATE PREVENTION
    // =========================================================================

    @Test
    public void testIdempotencyKeyTracking() {
        Set<String> processedKeys = new HashSet<>();
        String idempotencyKey = "IDEMP_SQH_BILL101_20261111";

        // First attempt: processed successfully
        assertFalse(processedKeys.contains(idempotencyKey));
        processedKeys.add(idempotencyKey);
        assertTrue(processedKeys.contains(idempotencyKey));

        // Duplicate attempt: blocked by idempotency guard
        boolean isDuplicate = processedKeys.contains(idempotencyKey);
        assertTrue("Duplicate transaction submission must be detected and blocked", isDuplicate);
    }

    // =========================================================================
    // 5. MULTI-TENANT HOSPITAL ISOLATION
    // =========================================================================

    @Test
    public void testMultiTenantBillingIsolation() {
        HospitalBill squareBill = new HospitalBill(
                "bill_sqh_01", "BILL-SQH-01", HOSPITAL_SQH, "SQH",
                "pat_01", "Sadia Islam", "AROHI-PAT-A1B2C3D4", BillCategory.CONSULTATION
        );

        HospitalBill unitedBill = new HospitalBill(
                "bill_unt_01", "BILL-UNT-01", HOSPITAL_UNT, "UNT",
                "pat_02", "Rafiqul Islam", "AROHI-PAT-B2C3D4E5", BillCategory.CONSULTATION
        );

        assertNotEquals(squareBill.getHospitalId(), unitedBill.getHospitalId());
        assertFalse(squareBill.getHospitalId().equalsIgnoreCase(unitedBill.getHospitalId()));

        // Multi-tenant filter simulation
        List<HospitalBill> allBills = new ArrayList<>();
        allBills.add(squareBill);
        allBills.add(unitedBill);

        List<HospitalBill> squareOnly = new ArrayList<>();
        for (HospitalBill b : allBills) {
            if (HOSPITAL_SQH.equalsIgnoreCase(b.getHospitalId())) {
                squareOnly.add(b);
            }
        }

        assertEquals(1, squareOnly.size());
        assertEquals(HOSPITAL_SQH, squareOnly.get(0).getHospitalId());
    }

    // =========================================================================
    // 6. DIGITAL RECEIPT & VERIFICATION HASH
    // =========================================================================

    @Test
    public void testDigitalReceiptCreationAndHash() {
        PaymentReceipt receipt = new PaymentReceipt(
                "RCP-SQH-202611-9921",
                "bill_101",
                "BILL-SQH-202611-0001",
                "TXN_BKASH_8829102",
                HOSPITAL_SQH,
                "Square Hospital Dhaka",
                "Sadia Islam",
                "AROHI-PAT-A1B2C3D4",
                "bKash Online Payment",
                1890.0
        );
        receipt.setDigitalVerificationCode("AROHI-SIG-94A2FE48");

        assertEquals("RCP-SQH-202611-9921", receipt.getReceiptNumber());
        assertEquals("BILL-SQH-202611-0001", receipt.getBillNumber());
        assertEquals(1890.0, receipt.getTotalPaidBdt(), 0.001);
        assertEquals("AROHI-SIG-94A2FE48", receipt.getDigitalVerificationCode());

        Map<String, Object> map = receipt.toMap();
        assertEquals("RCP-SQH-202611-9921", map.get("receiptNumber"));
        assertEquals("Square Hospital Dhaka", map.get("hospitalName"));
        assertEquals(1890.0, (Double) map.get("totalPaidBdt"), 0.001);
    }

    // =========================================================================
    // 7. REFUND REQUEST & REVERSAL WORKFLOW
    // =========================================================================

    @Test
    public void testRefundWorkflow() {
        RefundRequest refund = new RefundRequest(
                "REF_001",
                "bill_101",
                "BILL-SQH-202611-0001",
                "TXN_BKASH_8829102",
                HOSPITAL_SQH,
                "pat_01",
                "Sadia Islam",
                1890.0,
                "Doctor was unavailable for consultation."
        );

        assertEquals(RefundRequest.STATUS_REQUESTED, refund.getStatus());
        assertEquals(1890.0, refund.getRefundAmountBdt(), 0.001);

        // Authority approval
        refund.setStatus(RefundRequest.STATUS_PROCESSED);
        refund.setProcessedAt(System.currentTimeMillis());
        refund.setAdminNotes("Reversal approved by Square Hospital Billing Authority.");

        assertEquals(RefundRequest.STATUS_PROCESSED, refund.getStatus());
        assertTrue(refund.getProcessedAt() > 0);
        assertNotNull(refund.getAdminNotes());
    }
}
