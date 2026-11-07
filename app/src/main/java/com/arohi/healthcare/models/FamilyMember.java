package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Model representing an authorized family profile linked to a primary patient.
 * Supports care coordination, appointment booking, and medication reminders.
 * CRITICAL PRIVACY RULE: Family profiles NEVER grant automatic access to medical records.
 */
public class FamilyMember implements Serializable {

    public static final String RELATION_SPOUSE = "Spouse";
    public static final String RELATION_CHILD = "Child";
    public static final String RELATION_PARENT = "Parent";
    public static final String RELATION_SIBLING = "Sibling";
    public static final String RELATION_GRANDPARENT = "Grandparent";
    public static final String RELATION_OTHER = "Other Dependent";

    private String memberId;
    private String primaryPatientUid; // The patient who registered and manages this family profile
    private String fullName;
    private String relationship;
    private String dateOfBirth;
    private String gender;
    private String bloodGroup;
    private String knownAllergies;
    private String emergencyPhone;
    private boolean hasExplicitAuthorization; // Must be true with explicit consent
    private String authorizationNotes;
    private String linkedPatientArohiId; // Optional if member has their own AROHI account
    private long createdAtTimestamp;

    public FamilyMember() {
        this.createdAtTimestamp = System.currentTimeMillis();
        this.hasExplicitAuthorization = true;
    }

    public FamilyMember(String memberId, String primaryPatientUid, String fullName,
                        String relationship, String dateOfBirth, String gender,
                        String bloodGroup, String knownAllergies, String emergencyPhone,
                        boolean hasExplicitAuthorization, String authorizationNotes,
                        String linkedPatientArohiId) {
        this.memberId = memberId;
        this.primaryPatientUid = primaryPatientUid;
        this.fullName = fullName;
        this.relationship = relationship;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.bloodGroup = bloodGroup;
        this.knownAllergies = knownAllergies;
        this.emergencyPhone = emergencyPhone;
        this.hasExplicitAuthorization = hasExplicitAuthorization;
        this.authorizationNotes = authorizationNotes;
        this.linkedPatientArohiId = linkedPatientArohiId;
        this.createdAtTimestamp = System.currentTimeMillis();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("memberId", memberId);
        map.put("primaryPatientUid", primaryPatientUid);
        map.put("fullName", fullName);
        map.put("relationship", relationship);
        map.put("dateOfBirth", dateOfBirth);
        map.put("gender", gender);
        map.put("bloodGroup", bloodGroup);
        map.put("knownAllergies", knownAllergies);
        map.put("emergencyPhone", emergencyPhone);
        map.put("hasExplicitAuthorization", hasExplicitAuthorization);
        map.put("authorizationNotes", authorizationNotes);
        map.put("linkedPatientArohiId", linkedPatientArohiId);
        map.put("createdAtTimestamp", createdAtTimestamp);
        return map;
    }

    public static FamilyMember fromMap(Map<String, Object> map) {
        if (map == null) return null;
        FamilyMember f = new FamilyMember();
        f.setMemberId((String) map.get("memberId"));
        f.setPrimaryPatientUid((String) map.get("primaryPatientUid"));
        f.setFullName((String) map.get("fullName"));
        f.setRelationship((String) map.get("relationship"));
        f.setDateOfBirth((String) map.get("dateOfBirth"));
        f.setGender((String) map.get("gender"));
        f.setBloodGroup((String) map.get("bloodGroup"));
        f.setKnownAllergies((String) map.get("knownAllergies"));
        f.setEmergencyPhone((String) map.get("emergencyPhone"));
        if (map.get("hasExplicitAuthorization") instanceof Boolean) {
            f.setHasExplicitAuthorization((Boolean) map.get("hasExplicitAuthorization"));
        }
        f.setAuthorizationNotes((String) map.get("authorizationNotes"));
        f.setLinkedPatientArohiId((String) map.get("linkedPatientArohiId"));
        if (map.get("createdAtTimestamp") instanceof Number) {
            f.setCreatedAtTimestamp(((Number) map.get("createdAtTimestamp")).longValue());
        }
        return f;
    }

    // Getters and Setters
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getPrimaryPatientUid() { return primaryPatientUid; }
    public void setPrimaryPatientUid(String primaryPatientUid) { this.primaryPatientUid = primaryPatientUid; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getKnownAllergies() { return knownAllergies; }
    public void setKnownAllergies(String knownAllergies) { this.knownAllergies = knownAllergies; }

    public String getEmergencyPhone() { return emergencyPhone; }
    public void setEmergencyPhone(String emergencyPhone) { this.emergencyPhone = emergencyPhone; }

    public boolean isHasExplicitAuthorization() { return hasExplicitAuthorization; }
    public void setHasExplicitAuthorization(boolean hasExplicitAuthorization) { this.hasExplicitAuthorization = hasExplicitAuthorization; }

    public String getAuthorizationNotes() { return authorizationNotes; }
    public void setAuthorizationNotes(String authorizationNotes) { this.authorizationNotes = authorizationNotes; }

    public String getLinkedPatientArohiId() { return linkedPatientArohiId; }
    public void setLinkedPatientArohiId(String linkedPatientArohiId) { this.linkedPatientArohiId = linkedPatientArohiId; }

    public long getCreatedAtTimestamp() { return createdAtTimestamp; }
    public void setCreatedAtTimestamp(long createdAtTimestamp) { this.createdAtTimestamp = createdAtTimestamp; }
}
