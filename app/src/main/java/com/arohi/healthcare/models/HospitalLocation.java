package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Model representing a physical clinical department, facility, or unit within the hospital.
 * Used for indoor floor-by-floor navigation and interactive map simulation.
 */
public class HospitalLocation implements Serializable {

    public static final String TYPE_EMERGENCY = "Emergency";
    public static final String TYPE_RECEPTION = "Reception";
    public static final String TYPE_ICU = "ICU";
    public static final String TYPE_OT = "Operation Theatre";
    public static final String TYPE_LABORATORY = "Laboratory";
    public static final String TYPE_PHARMACY = "Pharmacy";
    public static final String TYPE_RADIOLOGY = "Radiology & Imaging";
    public static final String TYPE_WARD = "Inpatient Ward";

    public static final String FLOOR_GROUND = "Ground Floor";
    public static final String FLOOR_1 = "Floor 1";
    public static final String FLOOR_2 = "Floor 2";
    public static final String FLOOR_3 = "Floor 3";
    public static final String FLOOR_BASEMENT = "Basement";

    private String locationId;
    private String name;
    private String departmentType;
    private String floor;
    private String wing; // East Wing, West Wing, North Wing, Central Atrium
    private String roomNumber;
    private String description;
    private String workingHours;
    private String contactExtension;
    private String stepByStepDirections;
    private int gridRow; // 0 to 2 for 3x3 interactive floor schematic
    private int gridCol; // 0 to 2

    public HospitalLocation() {}

    public HospitalLocation(String locationId, String name, String departmentType,
                            String floor, String wing, String roomNumber,
                            String description, String workingHours, String contactExtension,
                            String stepByStepDirections, int gridRow, int gridCol) {
        this.locationId = locationId;
        this.name = name;
        this.departmentType = departmentType;
        this.floor = floor;
        this.wing = wing;
        this.roomNumber = roomNumber;
        this.description = description;
        this.workingHours = workingHours;
        this.contactExtension = contactExtension;
        this.stepByStepDirections = stepByStepDirections;
        this.gridRow = gridRow;
        this.gridCol = gridCol;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("locationId", locationId);
        map.put("name", name);
        map.put("departmentType", departmentType);
        map.put("floor", floor);
        map.put("wing", wing);
        map.put("roomNumber", roomNumber);
        map.put("description", description);
        map.put("workingHours", workingHours);
        map.put("contactExtension", contactExtension);
        map.put("stepByStepDirections", stepByStepDirections);
        map.put("gridRow", gridRow);
        map.put("gridCol", gridCol);
        return map;
    }

    public static HospitalLocation fromMap(Map<String, Object> map) {
        if (map == null) return null;
        HospitalLocation l = new HospitalLocation();
        l.setLocationId((String) map.get("locationId"));
        l.setName((String) map.get("name"));
        l.setDepartmentType((String) map.get("departmentType"));
        l.setFloor((String) map.get("floor"));
        l.setWing((String) map.get("wing"));
        l.setRoomNumber((String) map.get("roomNumber"));
        l.setDescription((String) map.get("description"));
        l.setWorkingHours((String) map.get("workingHours"));
        l.setContactExtension((String) map.get("contactExtension"));
        l.setStepByStepDirections((String) map.get("stepByStepDirections"));
        if (map.get("gridRow") instanceof Number) {
            l.setGridRow(((Number) map.get("gridRow")).intValue());
        }
        if (map.get("gridCol") instanceof Number) {
            l.setGridCol(((Number) map.get("gridCol")).intValue());
        }
        return l;
    }

    // Getters and Setters
    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartmentType() { return departmentType; }
    public void setDepartmentType(String departmentType) { this.departmentType = departmentType; }

    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }

    public String getWing() { return wing; }
    public void setWing(String wing) { this.wing = wing; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getWorkingHours() { return workingHours; }
    public void setWorkingHours(String workingHours) { this.workingHours = workingHours; }

    public String getContactExtension() { return contactExtension; }
    public void setContactExtension(String contactExtension) { this.contactExtension = contactExtension; }

    public String getStepByStepDirections() { return stepByStepDirections; }
    public void setStepByStepDirections(String stepByStepDirections) { this.stepByStepDirections = stepByStepDirections; }

    public int getGridRow() { return gridRow; }
    public void setGridRow(int gridRow) { this.gridRow = gridRow; }

    public int getGridCol() { return gridCol; }
    public void setGridCol(int gridCol) { this.gridCol = gridCol; }
}
