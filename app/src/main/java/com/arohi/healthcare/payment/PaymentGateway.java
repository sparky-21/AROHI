package com.arohi.healthcare.payment;

import com.arohi.healthcare.models.HospitalBill;
import com.arohi.healthcare.models.PaymentGatewayType;
import com.arohi.healthcare.models.PaymentTransaction;
import com.arohi.healthcare.models.RefundRequest;

/**
 * Phase 13 Provider-Independent Payment Gateway Interface.
 * Strategy pattern decoupling billing business logic from underlying gateway implementations
 * (bKash, Nagad, Card/SSLCommerz, Cash Counter).
 */
public interface PaymentGateway {

    interface GatewayInitiateCallback {
        void onInitiated(PaymentTransaction transaction, String redirectOrCheckoutUrl);
        void onError(String errorMessage);
    }

    interface GatewayVerifyCallback {
        void onVerified(PaymentTransaction verifiedTransaction, String digitalSignature);
        void onError(String errorMessage);
    }

    interface GatewayRefundCallback {
        void onRefundProcessed(RefundRequest processedRefund);
        void onError(String errorMessage);
    }

    PaymentGatewayType getType();

    void initiateTransaction(HospitalBill bill, String idempotencyKey, boolean isSandbox, GatewayInitiateCallback callback);

    void verifyTransaction(String transactionId, String idempotencyKey, String hospitalId,
                           double expectedAmount, GatewayVerifyCallback callback);

    void processRefund(RefundRequest refund, boolean isSandbox, GatewayRefundCallback callback);
}
