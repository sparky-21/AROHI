package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 13 Hospital Bill Model.
 * Manages institutional healthcare invoices, itemized calculations,
 * 5% statutory tax/VAT, discounts, payment status, and gateway associations.
 */
public class HospitalBill implements Serializable {

    public static final double STANDARD_VAT_RATE = 0.05; // 5% Healthcare VAT

    private String billId;
    private String billNumber;
    private String hospitalId;
    private String hospitalCode;
    private String patientUid;
    private String patientName;
    private String patientArohiId;
    private String patientPhone;

    private String doctorUid;
    private String doctorName;

    private String category; // CONSULTATION, APPOINTMENT, PHARMACY, LAB_TEST, HOSPITAL_SERVICE, OPERATION
    private List<BillItem> items;

    private double subtotalBdt;
    private double vatBdt;
    private double discountBdt;
    private double totalAmountBdt;

    private String status; // PENDING, PROCESSING, SUCCESSFUL, FAILED, CANCELLED, REFUNDED, PARTIALLY_REFUNDED
    private String paymentGateway; // BKASH, NAGAD, CARD_SSLCOMMERZ, CASH_COUNTER
    private String transactionId;
    private String receiptNumber;

    private long createdAt;
    private long paidAt;
    private String notes;

    public HospitalBill() {
        this.items = new ArrayList<>();
        this.status = PaymentStatus.PENDING.name();
        this.createdAt = System.currentTimeMillis();
    }

    public HospitalBill(String billId, String billNumber, String hospitalId, String hospitalCode,
                        String patientUid, String patientName, String patientArohiId,
                        BillCategory category) {
        this.billId = billId;
        this.billNumber = billNumber;
        this.hospitalId = hospitalId;
        this.hospitalCode = hospitalCode;
        this.patientUid = patientUid;
        this.patientName = patientName;
        this.patientArohiId = patientArohiId;
        this.category = category != null ? category.name() : BillCategory.HOSPITAL_SERVICE.name();
        this.items = new ArrayList<>();
        this.status = PaymentStatus.PENDING.name();
        this.createdAt = System.currentTimeMillis();
    }

    public void addItem(BillItem item) {
        if (items == null) items = new ArrayList<>();
        items.add(item);
        recalculateTotals();
    }

    public void recalculateTotals() {
        double sub = 0;
        if (items != null) {
            for (BillItem it : items) {
                sub += it.getTotalPriceBdt();
            }
        }
        this.subtotalBdt = sub;
        this.vatBdt = Math.round(sub * STANDARD_VAT_RATE * 100.0) / 100.0;
        double net = sub + vatBdt - discountBdt;
        this.totalAmountBdt = Math.max(0, Math.round(net * 100.0) / 100.0);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("billId", billId);
        map.put("billNumber", billNumber);
        map.put("hospitalId", hospitalId);
        map.put("hospitalCode", hospitalCode != null ? hospitalCode : "");
        map.put("patientUid", patientUid);
        map.put("patientName", patientName != null ? patientName : "");
        map.put("patientArohiId", patientArohiId != null ? patientArohiId : "");
        map.put("patientPhone", patientPhone != null ? patientPhone : "");
        map.put("doctorUid", doctorUid != null ? doctorUid : "");
        map.put("doctorName", doctorName != null ? doctorName : "");
        map.put("category", category);

        List<Map<String, Object>> itemsList = new ArrayList<>();
        if (items != null) {
            for (BillItem item : items) {
                itemsList.add(item.toMap());
            }
        }
        map.put("items", itemsList);

        map.put("subtotalBdt", subtotalBdt);
        map.put("vatBdt", vatBdt);
        map.put("discountBdt", discountBdt);
        map.put("totalAmountBdt", totalAmountBdt);
        map.put("status", status);
        map.put("paymentGateway", paymentGateway != null ? paymentGateway : "");
        map.put("transactionId", transactionId != null ? transactionId : "");
        map.put("receiptNumber", receiptNumber != null ? receiptNumber : "");
        map.put("createdAt", createdAt);
        map.put("paidAt", paidAt);
        map.put("notes", notes != null ? notes : "");
        return map;
    }

    // Getters and Setters
    public String getBillId() { return billId; }
    public void setBillId(String billId) { this.billId = billId; }

    public String getBillNumber() { return billNumber; }
    public void setBillNumber(String billNumber) { this.billNumber = billNumber; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getHospitalCode() { return hospitalCode; }
    public void setHospitalCode(String hospitalCode) { this.hospitalCode = hospitalCode; }

    public String getPatientUid() { return patientUid; }
    public void setPatientUid(String patientUid) { this.patientUid = patientUid; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientArohiId() { return patientArohiId; }
    public void setPatientArohiId(String patientArohiId) { this.patientArohiId = patientArohiId; }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public String getDoctorUid() { return doctorUid; }
    public void setDoctorUid(String doctorUid) { this.doctorUid = doctorUid; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<BillItem> getItems() { return items; }
    public void setItems(List<BillItem> items) {
        this.items = items;
        recalculateTotals();
    }

    public double getSubtotalBdt() { return subtotalBdt; }
    public void setSubtotalBdt(double subtotalBdt) { this.subtotalBdt = subtotalBdt; }

    public double getVatBdt() { return vatBdt; }
    public void setVatBdt(double vatBdt) { this.vatBdt = vatBdt; }

    public double getDiscountBdt() { return discountBdt; }
    public void setDiscountBdt(double discountBdt) {
        this.discountBdt = discountBdt;
        recalculateTotals();
    }

    public double getTotalAmountBdt() { return totalAmountBdt; }
    public void setTotalAmountBdt(double totalAmountBdt) { this.totalAmountBdt = totalAmountBdt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentGateway() { return paymentGateway; }
    public void setPaymentGateway(String paymentGateway) { this.paymentGateway = paymentGateway; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getPaidAt() { return paidAt; }
    public void setPaidAt(long paidAt) { this.paidAt = paidAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
