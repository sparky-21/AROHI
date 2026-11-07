package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Individual medication item prescribed in a digital prescription.
 */
public class PrescriptionMedicine implements Serializable {

    private String medicineName;
    private String dosage;              // e.g. "500 mg", "1 tablet", "10 ml"
    private String frequency;           // e.g. "1+0+1", "1+1+1", "Once daily"
    private String duration;            // e.g. "5 Days", "7 Days", "1 Month"
    private String mealInstruction;     // e.g. "After Meal", "Before Meal", "With Meal"
    private String instructions;        // e.g. "Drink plenty of water"

    public PrescriptionMedicine() {
        this.mealInstruction = "After Meal";
    }

    public PrescriptionMedicine(String medicineName, String dosage, String frequency,
                                String duration, String mealInstruction, String instructions) {
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.frequency = frequency;
        this.duration = duration;
        this.mealInstruction = mealInstruction != null ? mealInstruction : "After Meal";
        this.instructions = instructions;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("medicineName", medicineName);
        map.put("dosage", dosage);
        map.put("frequency", frequency);
        map.put("duration", duration);
        map.put("mealInstruction", mealInstruction);
        map.put("instructions", instructions);
        return map;
    }

    public static PrescriptionMedicine fromMap(Map<String, Object> map) {
        if (map == null) return null;
        PrescriptionMedicine m = new PrescriptionMedicine();
        m.medicineName = (String) map.get("medicineName");
        m.dosage = (String) map.get("dosage");
        m.frequency = (String) map.get("frequency");
        m.duration = (String) map.get("duration");
        m.mealInstruction = (String) map.get("mealInstruction");
        m.instructions = (String) map.get("instructions");
        return m;
    }

    // Getters and Setters
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getMealInstruction() { return mealInstruction; }
    public void setMealInstruction(String mealInstruction) { this.mealInstruction = mealInstruction; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
}
