package com.arohi.healthcare.models;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Model representing a pharmaceutical product in the AROHI Smart E-Pharmacy.
 * Tracks medicine specifications, categories, inventory stock, and prescription requirements.
 */
public class Medicine implements Serializable {

    public static final String CATEGORY_ANALGESICS = "Analgesics & Pain Relief";
    public static final String CATEGORY_ANTIBIOTICS = "Antibiotics & Antivirals";
    public static final String CATEGORY_CARDIOVASCULAR = "Cardiovascular & Hypertension";
    public static final String CATEGORY_GASTROINTESTINAL = "Gastrointestinal & Digestion";
    public static final String CATEGORY_RESPIRATORY = "Respiratory & Allergy";
    public static final String CATEGORY_DIABETES = "Diabetes & Metabolism";
    public static final String CATEGORY_DERMATOLOGY = "Dermatology & Skin Care";
    public static final String CATEGORY_VITAMINS = "Vitamins & Supplements";

    private String medicineId;
    private String name;
    private String genericName;
    private String category;
    private String strength;
    private String dosageForm; // Tablet, Capsule, Syrup, Inhaler, Injection
    private double unitPrice;
    private int stockQuantity;
    private boolean requiresPrescription;
    private String manufacturer;
    private String description;
    private String sideEffects;
    private String dosageInstructions;
    private long createdAtTimestamp;

    public Medicine() {
        this.createdAtTimestamp = System.currentTimeMillis();
    }

    public Medicine(String medicineId, String name, String genericName, String category,
                    String strength, String dosageForm, double unitPrice, int stockQuantity,
                    boolean requiresPrescription, String manufacturer, String description,
                    String sideEffects, String dosageInstructions) {
        this.medicineId = medicineId;
        this.name = name;
        this.genericName = genericName;
        this.category = category;
        this.strength = strength;
        this.dosageForm = dosageForm;
        this.unitPrice = unitPrice;
        this.stockQuantity = stockQuantity;
        this.requiresPrescription = requiresPrescription;
        this.manufacturer = manufacturer;
        this.description = description;
        this.sideEffects = sideEffects;
        this.dosageInstructions = dosageInstructions;
        this.createdAtTimestamp = System.currentTimeMillis();
    }

    public boolean isAvailable() {
        return stockQuantity > 0;
    }

    public boolean isLowStock() {
        return stockQuantity > 0 && stockQuantity <= 15;
    }

    public String getStockStatusLabel() {
        if (stockQuantity <= 0) {
            return "Out of Stock";
        } else if (stockQuantity <= 15) {
            return "Low Stock (" + stockQuantity + " left)";
        } else {
            return "In Stock (" + stockQuantity + ")";
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("medicineId", medicineId);
        map.put("name", name);
        map.put("genericName", genericName);
        map.put("category", category);
        map.put("strength", strength);
        map.put("dosageForm", dosageForm);
        map.put("unitPrice", unitPrice);
        map.put("stockQuantity", stockQuantity);
        map.put("requiresPrescription", requiresPrescription);
        map.put("manufacturer", manufacturer);
        map.put("description", description);
        map.put("sideEffects", sideEffects);
        map.put("dosageInstructions", dosageInstructions);
        map.put("createdAtTimestamp", createdAtTimestamp);
        return map;
    }

    public static Medicine fromMap(Map<String, Object> map) {
        if (map == null) return null;
        Medicine m = new Medicine();
        m.setMedicineId((String) map.get("medicineId"));
        m.setName((String) map.get("name"));
        m.setGenericName((String) map.get("genericName"));
        m.setCategory((String) map.get("category"));
        m.setStrength((String) map.get("strength"));
        m.setDosageForm((String) map.get("dosageForm"));
        if (map.get("unitPrice") instanceof Number) {
            m.setUnitPrice(((Number) map.get("unitPrice")).doubleValue());
        }
        if (map.get("stockQuantity") instanceof Number) {
            m.setStockQuantity(((Number) map.get("stockQuantity")).intValue());
        }
        if (map.get("requiresPrescription") instanceof Boolean) {
            m.setRequiresPrescription((Boolean) map.get("requiresPrescription"));
        }
        m.setManufacturer((String) map.get("manufacturer"));
        m.setDescription((String) map.get("description"));
        m.setSideEffects((String) map.get("sideEffects"));
        m.setDosageInstructions((String) map.get("dosageInstructions"));
        if (map.get("createdAtTimestamp") instanceof Number) {
            m.setCreatedAtTimestamp(((Number) map.get("createdAtTimestamp")).longValue());
        }
        return m;
    }

    // Getters and Setters
    public String getMedicineId() { return medicineId; }
    public void setMedicineId(String medicineId) { this.medicineId = medicineId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }

    public String getDosageForm() { return dosageForm; }
    public void setDosageForm(String dosageForm) { this.dosageForm = dosageForm; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public boolean isRequiresPrescription() { return requiresPrescription; }
    public void setRequiresPrescription(boolean requiresPrescription) { this.requiresPrescription = requiresPrescription; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSideEffects() { return sideEffects; }
    public void setSideEffects(String sideEffects) { this.sideEffects = sideEffects; }

    public String getDosageInstructions() { return dosageInstructions; }
    public void setDosageInstructions(String dosageInstructions) { this.dosageInstructions = dosageInstructions; }

    public long getCreatedAtTimestamp() { return createdAtTimestamp; }
    public void setCreatedAtTimestamp(long createdAtTimestamp) { this.createdAtTimestamp = createdAtTimestamp; }

    @NonNull
    @Override
    public String toString() {
        return name + " (" + strength + ") - " + dosageForm;
    }
}
