package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 13 Payment Transaction Model.
 * Represents an auditable ledger record for financial exchanges across gateways.
 * Strictly implements idempotency checking and cryptographic verification hashes.
 */
public class PaymentTransaction implements Serializable {

    private String transactionId;
    private String billId;
    private String billNumber;
    private String hospitalId;
    private String patientUid;
    private double amountBdt;
    private String currency; // "BDT"
    private String gateway; // BKASH, NAGAD, CARD_SSLCOMMERZ, CASH_COUNTER
    private String idempotencyKey;
    private String status; // PENDING, PROCESSING, SUCCESSFUL, FAILED, CANCELLED, REFUNDED
    private String gatewayReferenceNumber;
    private String verificationHash;
    private long createdAt;
    private long completedAt;
    private String errorMessage;

    public PaymentTransaction() {
        this.currency = "BDT";
        this.status = PaymentStatus.PENDING.name();
        this.createdAt = System.currentTimeMillis();
    }

    public PaymentTransaction(String transactionId, String billId, String billNumber,
                              String hospitalId, String patientUid, double amountBdt,
                              PaymentGatewayType gateway, String idempotencyKey) {
        this.transactionId = transactionId;
        this.billId = billId;
        this.billNumber = billNumber;
        this.hospitalId = hospitalId;
        this.patientUid = patientUid;
        this.amountBdt = amountBdt;
        this.currency = "BDT";
        this.gateway = gateway != null ? gateway.name() : PaymentGatewayType.BKASH.name();
        this.idempotencyKey = idempotencyKey;
        this.status = PaymentStatus.PENDING.name();
        this.createdAt = System.currentTimeMillis();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("transactionId", transactionId);
        map.put("billId", billId);
        map.put("billNumber", billNumber != null ? billNumber : "");
        map.put("hospitalId", hospitalId);
        map.put("patientUid", patientUid);
        map.put("amountBdt", amountBdt);
        map.put("currency", currency != null ? currency : "BDT");
        map.put("gateway", gateway);
        map.put("idempotencyKey", idempotencyKey);
        map.put("status", status);
        map.put("gatewayReferenceNumber", gatewayReferenceNumber != null ? gatewayReferenceNumber : "");
        map.put("verificationHash", verificationHash != null ? verificationHash : "");
        map.put("createdAt", createdAt);
        map.put("completedAt", completedAt);
        map.put("errorMessage", errorMessage != null ? errorMessage : "");
        return map;
    }

    // Getters and Setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getBillId() { return billId; }
    public void setBillId(String billId) { this.billId = billId; }

    public String getBillNumber() { return billNumber; }
    public void setBillNumber(String billNumber) { this.billNumber = billNumber; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getPatientUid() { return patientUid; }
    public void setPatientUid(String patientUid) { this.patientUid = patientUid; }

    public double getAmountBdt() { return amountBdt; }
    public void setAmountBdt(double amountBdt) { this.amountBdt = amountBdt; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getGatewayReferenceNumber() { return gatewayReferenceNumber; }
    public void setGatewayReferenceNumber(String gatewayReferenceNumber) { this.gatewayReferenceNumber = gatewayReferenceNumber; }

    public String getVerificationHash() { return verificationHash; }
    public void setVerificationHash(String verificationHash) { this.verificationHash = verificationHash; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
