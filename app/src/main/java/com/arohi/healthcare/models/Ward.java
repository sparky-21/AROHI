package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 6 Inpatient Ward model.
 * Managed by Administrators, supervised by Nurses, admitting Patients.
 */
public class Ward implements Serializable {

    public static final String STATUS_ACTIVE = "Active";
    public static final String STATUS_FULL = "Full";
    public static final String STATUS_MAINTENANCE = "Maintenance";

    private String wardId;
    private String wardName;
    private String department;
    private String floor;
    private int totalBeds;
    private int occupiedBeds;
    private String nurseSupervisorUid;
    private String nurseSupervisorName;
    private String status;

    public Ward() {
        this.status = STATUS_ACTIVE;
    }

    public Ward(String wardId, String wardName, String department, String floor,
                int totalBeds, int occupiedBeds, String nurseSupervisorUid,
                String nurseSupervisorName, String status) {
        this.wardId = wardId;
        this.wardName = wardName;
        this.department = department;
        this.floor = floor;
        this.totalBeds = totalBeds;
        this.occupiedBeds = occupiedBeds;
        this.nurseSupervisorUid = nurseSupervisorUid;
        this.nurseSupervisorName = nurseSupervisorName;
        this.status = status;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("wardId", wardId);
        map.put("wardName", wardName);
        map.put("department", department);
        map.put("floor", floor);
        map.put("totalBeds", totalBeds);
        map.put("occupiedBeds", occupiedBeds);
        map.put("nurseSupervisorUid", nurseSupervisorUid != null ? nurseSupervisorUid : "");
        map.put("nurseSupervisorName", nurseSupervisorName != null ? nurseSupervisorName : "");
        map.put("status", status);
        return map;
    }

    public int getAvailableBeds() {
        return Math.max(0, totalBeds - occupiedBeds);
    }

    public String getWardId() { return wardId; }
    public void setWardId(String wardId) { this.wardId = wardId; }

    public String getWardName() { return wardName; }
    public void setWardName(String wardName) { this.wardName = wardName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }

    public int getTotalBeds() { return totalBeds; }
    public void setTotalBeds(int totalBeds) { this.totalBeds = totalBeds; }

    public int getOccupiedBeds() { return occupiedBeds; }
    public void setOccupiedBeds(int occupiedBeds) { this.occupiedBeds = occupiedBeds; }

    public String getNurseSupervisorUid() { return nurseSupervisorUid; }
    public void setNurseSupervisorUid(String nurseSupervisorUid) { this.nurseSupervisorUid = nurseSupervisorUid; }

    public String getNurseSupervisorName() { return nurseSupervisorName; }
    public void setNurseSupervisorName(String nurseSupervisorName) { this.nurseSupervisorName = nurseSupervisorName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
