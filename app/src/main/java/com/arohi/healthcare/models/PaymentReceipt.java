package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 13 Digital Payment Receipt Model.
 * Represents an official institutional receipt suitable for PDF export and digital verification.
 */
public class PaymentReceipt implements Serializable {

    private String receiptNumber;
    private String billId;
    private String billNumber;
    private String transactionId;
    private String hospitalId;
    private String hospitalName;
    private String hospitalAddress;

    private String patientName;
    private String patientArohiId;
    private String patientPhone;

    private String category;
    private String paymentMethod;
    private double subtotalBdt;
    private double vatBdt;
    private double discountBdt;
    private double totalPaidBdt;
    private long timestamp;
    private String digitalVerificationCode;
    private String itemsSummary;

    public PaymentReceipt() {
        this.timestamp = System.currentTimeMillis();
    }

    public PaymentReceipt(String receiptNumber, String billId, String billNumber,
                          String transactionId, String hospitalId, String hospitalName,
                          String patientName, String patientArohiId, String paymentMethod,
                          double totalPaidBdt) {
        this.receiptNumber = receiptNumber;
        this.billId = billId;
        this.billNumber = billNumber;
        this.transactionId = transactionId;
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
        this.patientName = patientName;
        this.patientArohiId = patientArohiId;
        this.paymentMethod = paymentMethod;
        this.totalPaidBdt = totalPaidBdt;
        this.timestamp = System.currentTimeMillis();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("receiptNumber", receiptNumber);
        map.put("billId", billId);
        map.put("billNumber", billNumber != null ? billNumber : "");
        map.put("transactionId", transactionId != null ? transactionId : "");
        map.put("hospitalId", hospitalId);
        map.put("hospitalName", hospitalName != null ? hospitalName : "");
        map.put("hospitalAddress", hospitalAddress != null ? hospitalAddress : "");
        map.put("patientName", patientName != null ? patientName : "");
        map.put("patientArohiId", patientArohiId != null ? patientArohiId : "");
        map.put("patientPhone", patientPhone != null ? patientPhone : "");
        map.put("category", category != null ? category : "");
        map.put("paymentMethod", paymentMethod != null ? paymentMethod : "");
        map.put("subtotalBdt", subtotalBdt);
        map.put("vatBdt", vatBdt);
        map.put("discountBdt", discountBdt);
        map.put("totalPaidBdt", totalPaidBdt);
        map.put("timestamp", timestamp);
        map.put("digitalVerificationCode", digitalVerificationCode != null ? digitalVerificationCode : "");
        map.put("itemsSummary", itemsSummary != null ? itemsSummary : "");
        return map;
    }

    // Getters and Setters
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    public String getBillId() { return billId; }
    public void setBillId(String billId) { this.billId = billId; }

    public String getBillNumber() { return billNumber; }
    public void setBillNumber(String billNumber) { this.billNumber = billNumber; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

    public String getHospitalAddress() { return hospitalAddress; }
    public void setHospitalAddress(String hospitalAddress) { this.hospitalAddress = hospitalAddress; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientArohiId() { return patientArohiId; }
    public void setPatientArohiId(String patientArohiId) { this.patientArohiId = patientArohiId; }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public double getSubtotalBdt() { return subtotalBdt; }
    public void setSubtotalBdt(double subtotalBdt) { this.subtotalBdt = subtotalBdt; }

    public double getVatBdt() { return vatBdt; }
    public void setVatBdt(double vatBdt) { this.vatBdt = vatBdt; }

    public double getDiscountBdt() { return discountBdt; }
    public void setDiscountBdt(double discountBdt) { this.discountBdt = discountBdt; }

    public double getTotalPaidBdt() { return totalPaidBdt; }
    public void setTotalPaidBdt(double totalPaidBdt) { this.totalPaidBdt = totalPaidBdt; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getDigitalVerificationCode() { return digitalVerificationCode; }
    public void setDigitalVerificationCode(String digitalVerificationCode) { this.digitalVerificationCode = digitalVerificationCode; }

    public String getItemsSummary() { return itemsSummary; }
    public void setItemsSummary(String itemsSummary) { this.itemsSummary = itemsSummary; }
}
