package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 6 Nurse Duty Schedule & Shift Roster model.
 * Morning, Evening, Night ward shifts and Operation Theatre assistance rosters.
 */
public class DutySchedule implements Serializable {

    public static final String SHIFT_MORNING = "Morning (08:00 AM - 04:00 PM)";
    public static final String SHIFT_EVENING = "Evening (04:00 PM - 12:00 AM)";
    public static final String SHIFT_NIGHT = "Night (12:00 AM - 08:00 AM)";

    public static final String STATUS_SCHEDULED = "Scheduled";
    public static final String STATUS_ON_DUTY = "On Duty";
    public static final String STATUS_COMPLETED = "Completed";

    private String scheduleId;
    private String nurseUid;
    private String nurseId;
    private String nurseName;
    private String wardId;
    private String wardName;
    private String shiftType; // Morning, Evening, Night
    private String shiftDate; // YYYY-MM-DD
    private boolean otAssistance;
    private String otRoomName;
    private String otOperationName;
    private String status;
    private String notes;

    public DutySchedule() {
        this.status = STATUS_SCHEDULED;
        this.shiftType = SHIFT_MORNING;
    }

    public DutySchedule(String scheduleId, String nurseUid, String nurseId, String nurseName,
                        String wardId, String wardName, String shiftType, String shiftDate,
                        boolean otAssistance, String otRoomName, String otOperationName) {
        this.scheduleId = scheduleId;
        this.nurseUid = nurseUid;
        this.nurseId = nurseId;
        this.nurseName = nurseName;
        this.wardId = wardId;
        this.wardName = wardName;
        this.shiftType = shiftType;
        this.shiftDate = shiftDate;
        this.otAssistance = otAssistance;
        this.otRoomName = otRoomName;
        this.otOperationName = otOperationName;
        this.status = STATUS_SCHEDULED;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("scheduleId", scheduleId);
        map.put("nurseUid", nurseUid);
        map.put("nurseId", nurseId);
        map.put("nurseName", nurseName);
        map.put("wardId", wardId);
        map.put("wardName", wardName);
        map.put("shiftType", shiftType);
        map.put("shiftDate", shiftDate);
        map.put("otAssistance", otAssistance);
        map.put("otRoomName", otRoomName != null ? otRoomName : "");
        map.put("otOperationName", otOperationName != null ? otOperationName : "");
        map.put("status", status);
        map.put("notes", notes != null ? notes : "");
        return map;
    }

    public String getScheduleId() { return scheduleId; }
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }

    public String getNurseUid() { return nurseUid; }
    public void setNurseUid(String nurseUid) { this.nurseUid = nurseUid; }

    public String getNurseId() { return nurseId; }
    public void setNurseId(String nurseId) { this.nurseId = nurseId; }

    public String getNurseName() { return nurseName; }
    public void setNurseName(String nurseName) { this.nurseName = nurseName; }

    public String getWardId() { return wardId; }
    public void setWardId(String wardId) { this.wardId = wardId; }

    public String getWardName() { return wardName; }
    public void setWardName(String wardName) { this.wardName = wardName; }

    public String getShiftType() { return shiftType; }
    public void setShiftType(String shiftType) { this.shiftType = shiftType; }

    public String getShiftDate() { return shiftDate; }
    public void setShiftDate(String shiftDate) { this.shiftDate = shiftDate; }

    public boolean isOtAssistance() { return otAssistance; }
    public void setOtAssistance(boolean otAssistance) { this.otAssistance = otAssistance; }

    public String getOtRoomName() { return otRoomName; }
    public void setOtRoomName(String otRoomName) { this.otRoomName = otRoomName; }

    public String getOtOperationName() { return otOperationName; }
    public void setOtOperationName(String otOperationName) { this.otOperationName = otOperationName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
