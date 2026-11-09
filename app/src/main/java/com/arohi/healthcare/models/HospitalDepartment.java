package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 6 Hospital Department model.
 * Cardiology, General Surgery, Orthopedics, Critical Care, Pediatrics, Neurology, etc.
 */
public class HospitalDepartment implements Serializable {

    private String deptId;
    private String deptName;
    private String headOfDepartment;
    private String location;
    private int totalStaff;
    private boolean active;
    private String description;

    public HospitalDepartment() {
        this.active = true;
    }

    public HospitalDepartment(String deptId, String deptName, String headOfDepartment,
                              String location, int totalStaff, boolean active, String description) {
        this.deptId = deptId;
        this.deptName = deptName;
        this.headOfDepartment = headOfDepartment;
        this.location = location;
        this.totalStaff = totalStaff;
        this.active = active;
        this.description = description;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("deptId", deptId);
        map.put("deptName", deptName);
        map.put("headOfDepartment", headOfDepartment);
        map.put("location", location);
        map.put("totalStaff", totalStaff);
        map.put("active", active);
        map.put("description", description);
        return map;
    }

    public String getDeptId() { return deptId; }
    public void setDeptId(String deptId) { this.deptId = deptId; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public String getHeadOfDepartment() { return headOfDepartment; }
    public void setHeadOfDepartment(String headOfDepartment) { this.headOfDepartment = headOfDepartment; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getTotalStaff() { return totalStaff; }
    public void setTotalStaff(int totalStaff) { this.totalStaff = totalStaff; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
