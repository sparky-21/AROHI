package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Item contained within an AROHI Pharmacy order.
 */
public class PharmacyOrderItem implements Serializable {

    private String medicineId;
    private String medicineName;
    private String strength;
    private int quantity;
    private double unitPrice;
    private double subtotal;

    public PharmacyOrderItem() {}

    public PharmacyOrderItem(String medicineId, String medicineName, String strength, int quantity, double unitPrice) {
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.strength = strength;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = quantity * unitPrice;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("medicineId", medicineId);
        map.put("medicineName", medicineName);
        map.put("strength", strength);
        map.put("quantity", quantity);
        map.put("unitPrice", unitPrice);
        map.put("subtotal", subtotal);
        return map;
    }

    public static PharmacyOrderItem fromMap(Map<String, Object> map) {
        if (map == null) return null;
        PharmacyOrderItem item = new PharmacyOrderItem();
        item.setMedicineId((String) map.get("medicineId"));
        item.setMedicineName((String) map.get("medicineName"));
        item.setStrength((String) map.get("strength"));
        if (map.get("quantity") instanceof Number) {
            item.setQuantity(((Number) map.get("quantity")).intValue());
        }
        if (map.get("unitPrice") instanceof Number) {
            item.setUnitPrice(((Number) map.get("unitPrice")).doubleValue());
        }
        if (map.get("subtotal") instanceof Number) {
            item.setSubtotal(((Number) map.get("subtotal")).doubleValue());
        }
        return item;
    }

    // Getters and Setters
    public String getMedicineId() { return medicineId; }
    public void setMedicineId(String medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.subtotal = this.quantity * this.unitPrice;
    }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
        this.subtotal = this.quantity * this.unitPrice;
    }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
}
