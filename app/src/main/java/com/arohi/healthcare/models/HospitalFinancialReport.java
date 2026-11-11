package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 13 Hospital Financial Report Model.
 * Encapsulates revenue, collection, category breakdowns, and audit analytics for hospital authority.
 */
public class HospitalFinancialReport implements Serializable {

    private String hospitalId;
    private double totalRevenueBdt;
    private double pendingRevenueBdt;
    private double refundedAmountBdt;
    private double todayRevenueBdt;

    private int totalBillsCount;
    private int paidBillsCount;
    private int pendingBillsCount;
    private int refundsCount;

    private Map<String, Double> categoryRevenue;
    private Map<String, Double> gatewayRevenue;

    public HospitalFinancialReport() {
        this.categoryRevenue = new HashMap<>();
        this.gatewayRevenue = new HashMap<>();
    }

    public HospitalFinancialReport(String hospitalId) {
        this.hospitalId = hospitalId;
        this.categoryRevenue = new HashMap<>();
        this.gatewayRevenue = new HashMap<>();
    }

    // Getters and Setters
    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public double getTotalRevenueBdt() { return totalRevenueBdt; }
    public void setTotalRevenueBdt(double totalRevenueBdt) { this.totalRevenueBdt = totalRevenueBdt; }

    public double getPendingRevenueBdt() { return pendingRevenueBdt; }
    public void setPendingRevenueBdt(double pendingRevenueBdt) { this.pendingRevenueBdt = pendingRevenueBdt; }

    public double getRefundedAmountBdt() { return refundedAmountBdt; }
    public void setRefundedAmountBdt(double refundedAmountBdt) { this.refundedAmountBdt = refundedAmountBdt; }

    public double getTodayRevenueBdt() { return todayRevenueBdt; }
    public void setTodayRevenueBdt(double todayRevenueBdt) { this.todayRevenueBdt = todayRevenueBdt; }

    public int getTotalBillsCount() { return totalBillsCount; }
    public void setTotalBillsCount(int totalBillsCount) { this.totalBillsCount = totalBillsCount; }

    public int getPaidBillsCount() { return paidBillsCount; }
    public void setPaidBillsCount(int paidBillsCount) { this.paidBillsCount = paidBillsCount; }

    public int getPendingBillsCount() { return pendingBillsCount; }
    public void setPendingBillsCount(int pendingBillsCount) { this.pendingBillsCount = pendingBillsCount; }

    public int getRefundsCount() { return refundsCount; }
    public void setRefundsCount(int refundsCount) { this.refundsCount = refundsCount; }

    public Map<String, Double> getCategoryRevenue() { return categoryRevenue; }
    public void setCategoryRevenue(Map<String, Double> categoryRevenue) { this.categoryRevenue = categoryRevenue; }

    public Map<String, Double> getGatewayRevenue() { return gatewayRevenue; }
    public void setGatewayRevenue(Map<String, Double> gatewayRevenue) { this.gatewayRevenue = gatewayRevenue; }
}
