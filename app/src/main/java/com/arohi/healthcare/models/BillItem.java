package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 13 Bill Line Item.
 * Represents a discrete service, procedure, medicine, or consultation charged on a bill.
 */
public class BillItem implements Serializable {

    private String itemId;
    private String description;
    private int quantity;
    private double unitPriceBdt;
    private double totalPriceBdt;

    public BillItem() {}

    public BillItem(String itemId, String description, int quantity, double unitPriceBdt) {
        this.itemId = itemId;
        this.description = description;
        this.quantity = Math.max(1, quantity);
        this.unitPriceBdt = Math.max(0, unitPriceBdt);
        this.totalPriceBdt = this.quantity * this.unitPriceBdt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("itemId", itemId);
        map.put("description", description);
        map.put("quantity", quantity);
        map.put("unitPriceBdt", unitPriceBdt);
        map.put("totalPriceBdt", totalPriceBdt);
        return map;
    }

    public static BillItem fromMap(Map<String, Object> map) {
        if (map == null) return null;
        BillItem item = new BillItem();
        item.setItemId((String) map.get("itemId"));
        item.setDescription((String) map.get("description"));
        Number q = (Number) map.get("quantity");
        item.setQuantity(q != null ? q.intValue() : 1);
        Number up = (Number) map.get("unitPriceBdt");
        item.setUnitPriceBdt(up != null ? up.doubleValue() : 0.0);
        Number tp = (Number) map.get("totalPriceBdt");
        item.setTotalPriceBdt(tp != null ? tp.doubleValue() : 0.0);
        return item;
    }

    // Getters and Setters
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.totalPriceBdt = this.quantity * this.unitPriceBdt;
    }

    public double getUnitPriceBdt() { return unitPriceBdt; }
    public void setUnitPriceBdt(double unitPriceBdt) {
        this.unitPriceBdt = unitPriceBdt;
        this.totalPriceBdt = this.quantity * this.unitPriceBdt;
    }

    public double getTotalPriceBdt() { return totalPriceBdt; }
    public void setTotalPriceBdt(double totalPriceBdt) { this.totalPriceBdt = totalPriceBdt; }
}
