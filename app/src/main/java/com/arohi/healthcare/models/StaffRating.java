package com.arohi.healthcare.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Rating given by a patient to a doctor or nurse.
 * Stored in Firestore /staffRatings/{ratingId}.
 */
public class StaffRating {

    private String ratingId;
    private String patientUid;
    private String patientName;
    private String staffUid;
    private String staffName;
    private String staffRole; // "DOCTOR" or "NURSE"
    private float rating; // 1.0 to 5.0
    private String comment;
    private long createdAt;

    public StaffRating() {
        // Required empty constructor for Firestore
    }

    public StaffRating(String ratingId, String patientUid, String patientName,
                       String staffUid, String staffName, String staffRole,
                       float rating, String comment) {
        this.ratingId = ratingId;
        this.patientUid = patientUid;
        this.patientName = patientName;
        this.staffUid = staffUid;
        this.staffName = staffName;
        this.staffRole = staffRole;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = System.currentTimeMillis();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("ratingId", ratingId);
        map.put("patientUid", patientUid);
        map.put("patientName", patientName);
        map.put("staffUid", staffUid);
        map.put("staffName", staffName);
        map.put("staffRole", staffRole);
        map.put("rating", rating);
        map.put("comment", comment != null ? comment : "");
        map.put("createdAt", createdAt);
        return map;
    }

    public String getRatingId() { return ratingId; }
    public void setRatingId(String ratingId) { this.ratingId = ratingId; }

    public String getPatientUid() { return patientUid; }
    public void setPatientUid(String patientUid) { this.patientUid = patientUid; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getStaffUid() { return staffUid; }
    public void setStaffUid(String staffUid) { this.staffUid = staffUid; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public String getStaffRole() { return staffRole; }
    public void setStaffRole(String staffRole) { this.staffRole = staffRole; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
