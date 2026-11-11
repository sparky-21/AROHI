package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 13 Refund Request Model.
 * Manages patient refund requests, authority review, and transaction reversals.
 */
public class RefundRequest implements Serializable {

    public static final String STATUS_REQUESTED = "REQUESTED";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_PROCESSED = "PROCESSED";
    public static final String STATUS_REJECTED = "REJECTED";

    private String refundId;
    private String billId;
    private String billNumber;
    private String transactionId;
    private String hospitalId;
    private String patientUid;
    private String patientName;
    private double refundAmountBdt;
    private String reason;
    private String status;
    private long requestedAt;
    private long processedAt;
    private String adminNotes;

    public RefundRequest() {
        this.status = STATUS_REQUESTED;
        this.requestedAt = System.currentTimeMillis();
    }

    public RefundRequest(String refundId, String billId, String billNumber,
                         String transactionId, String hospitalId, String patientUid,
                         String patientName, double refundAmountBdt, String reason) {
        this.refundId = refundId;
        this.billId = billId;
        this.billNumber = billNumber;
        this.transactionId = transactionId;
        this.hospitalId = hospitalId;
        this.patientUid = patientUid;
        this.patientName = patientName;
        this.refundAmountBdt = refundAmountBdt;
        this.reason = reason;
        this.status = STATUS_REQUESTED;
        this.requestedAt = System.currentTimeMillis();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("refundId", refundId);
        map.put("billId", billId);
        map.put("billNumber", billNumber != null ? billNumber : "");
        map.put("transactionId", transactionId != null ? transactionId : "");
        map.put("hospitalId", hospitalId);
        map.put("patientUid", patientUid);
        map.put("patientName", patientName != null ? patientName : "");
        map.put("refundAmountBdt", refundAmountBdt);
        map.put("reason", reason != null ? reason : "");
        map.put("status", status);
        map.put("requestedAt", requestedAt);
        map.put("processedAt", processedAt);
        map.put("adminNotes", adminNotes != null ? adminNotes : "");
        return map;
    }

    // Getters and Setters
    public String getRefundId() { return refundId; }
    public void setRefundId(String refundId) { this.refundId = refundId; }

    public String getBillId() { return billId; }
    public void setBillId(String billId) { this.billId = billId; }

    public String getBillNumber() { return billNumber; }
    public void setBillNumber(String billNumber) { this.billNumber = billNumber; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getPatientUid() { return patientUid; }
    public void setPatientUid(String patientUid) { this.patientUid = patientUid; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public double getRefundAmountBdt() { return refundAmountBdt; }
    public void setRefundAmountBdt(double refundAmountBdt) { this.refundAmountBdt = refundAmountBdt; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getRequestedAt() { return requestedAt; }
    public void setRequestedAt(long requestedAt) { this.requestedAt = requestedAt; }

    public long getProcessedAt() { return processedAt; }
    public void setProcessedAt(long processedAt) { this.processedAt = processedAt; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
}
