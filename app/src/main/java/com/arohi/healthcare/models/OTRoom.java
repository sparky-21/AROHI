package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 6 Operation Theatre Room model.
 * Managed by Administrators. Available for surgical scheduling by authorized Doctors.
 */
public class OTRoom implements Serializable {

    public static final String STATUS_AVAILABLE = "Available";
    public static final String STATUS_OCCUPIED = "Occupied";
    public static final String STATUS_SANITIZATION = "Sanitization";
    public static final String STATUS_MAINTENANCE = "Maintenance";

    private String roomId;
    private String roomName;
    private String floor;
    private String specialties;
    private String status; // Available, Occupied, Sanitization, Maintenance
    private boolean available;
    private String currentOperationId;
    private String equipmentNotes;

    public OTRoom() {
        this.status = STATUS_AVAILABLE;
        this.available = true;
    }

    public OTRoom(String roomId, String roomName, String floor, String specialties, String status, String equipmentNotes) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.floor = floor;
        this.specialties = specialties;
        this.status = (status != null && !status.trim().isEmpty()) ? status.trim() : STATUS_AVAILABLE;
        this.available = STATUS_AVAILABLE.equalsIgnoreCase(this.status);
        this.equipmentNotes = equipmentNotes;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("roomId", roomId);
        map.put("roomName", roomName);
        map.put("floor", floor);
        map.put("specialties", specialties);
        map.put("status", status);
        map.put("available", available);
        map.put("currentOperationId", currentOperationId != null ? currentOperationId : "");
        map.put("equipmentNotes", equipmentNotes != null ? equipmentNotes : "");
        return map;
    }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }

    public String getSpecialties() { return specialties; }
    public void setSpecialties(String specialties) { this.specialties = specialties; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = (status != null && !status.trim().isEmpty()) ? status.trim() : STATUS_AVAILABLE;
        this.available = STATUS_AVAILABLE.equalsIgnoreCase(this.status);
    }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) {
        this.available = available;
        if (available) {
            this.status = STATUS_AVAILABLE;
        }
    }

    public String getCurrentOperationId() { return currentOperationId; }
    public void setCurrentOperationId(String currentOperationId) { this.currentOperationId = currentOperationId; }

    public String getEquipmentNotes() { return equipmentNotes; }
    public void setEquipmentNotes(String equipmentNotes) { this.equipmentNotes = equipmentNotes; }
}
