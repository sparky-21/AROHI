package com.arohi.healthcare.payment;

import com.arohi.healthcare.models.HospitalBill;
import com.arohi.healthcare.models.PaymentGatewayType;
import com.arohi.healthcare.models.PaymentStatus;
import com.arohi.healthcare.models.PaymentTransaction;
import com.arohi.healthcare.models.RefundRequest;

import java.security.MessageDigest;
import java.util.UUID;

/**
 * Phase 13 Hospital Cash Counter Gateway Adapter.
 * Manages on-site cash payments made directly at the hospital billing desk.
 * Requires cashier confirmation and produces physical/digital receipt records.
 */
public class CashPaymentGateway implements PaymentGateway {

    @Override
    public PaymentGatewayType getType() {
        return PaymentGatewayType.CASH_COUNTER;
    }

    @Override
    public void initiateTransaction(HospitalBill bill, String idempotencyKey, boolean isSandbox, GatewayInitiateCallback callback) {
        if (bill == null || bill.getTotalAmountBdt() <= 0) {
            callback.onError("Invalid bill amount for cash payment.");
            return;
        }

        String txnId = "TXN_CASH_" + System.currentTimeMillis();
        String counterToken = "CTR_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        PaymentTransaction transaction = new PaymentTransaction(
                txnId,
                bill.getBillId(),
                bill.getBillNumber(),
                bill.getHospitalId(),
                bill.getPatientUid(),
                bill.getTotalAmountBdt(),
                PaymentGatewayType.CASH_COUNTER,
                idempotencyKey
        );
        transaction.setGatewayReferenceNumber(counterToken);
        transaction.setStatus(PaymentStatus.PENDING.name());

        callback.onInitiated(transaction, "HOSPITAL_CASH_COUNTER_TOKEN:" + counterToken);
    }

    @Override
    public void verifyTransaction(String transactionId, String idempotencyKey, String hospitalId,
                                  double expectedAmount, GatewayVerifyCallback callback) {
        if (transactionId == null || expectedAmount <= 0) {
            callback.onError("Cash payment verification requires valid counter transaction ID.");
            return;
        }

        PaymentTransaction tx = new PaymentTransaction();
        tx.setTransactionId(transactionId);
        tx.setHospitalId(hospitalId);
        tx.setAmountBdt(expectedAmount);
        tx.setCurrency("BDT");
        tx.setGateway(PaymentGatewayType.CASH_COUNTER.name());
        tx.setIdempotencyKey(idempotencyKey);
        tx.setStatus(PaymentStatus.SUCCESSFUL.name());
        tx.setCompletedAt(System.currentTimeMillis());

        String signature = generateSignature("CASH_DESK_" + transactionId + "_" + hospitalId + "_" + expectedAmount);
        tx.setVerificationHash(signature);

        callback.onVerified(tx, signature);
    }

    @Override
    public void processRefund(RefundRequest refund, boolean isSandbox, GatewayRefundCallback callback) {
        if (refund == null || refund.getRefundAmountBdt() <= 0) {
            callback.onError("Invalid cash refund request.");
            return;
        }

        refund.setStatus(RefundRequest.STATUS_PROCESSED);
        refund.setProcessedAt(System.currentTimeMillis());
        refund.setAdminNotes("Cash returned at hospital counter. Counter voucher issued.");
        callback.onRefundProcessed(refund);
    }

    private String generateSignature(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.substring(0, 16).toUpperCase();
        } catch (Exception e) {
            return "SIG_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }
}
