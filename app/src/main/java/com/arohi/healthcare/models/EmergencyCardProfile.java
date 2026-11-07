package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Model representing the Patient-Controlled Emergency Health Card.
 * Allows quick access to critical medical parameters during emergencies
 * with granular patient privacy toggles.
 */
public class EmergencyCardProfile implements Serializable {

    private String patientUid;
    private String arohiId;
    private String fullName;
    private String dateOfBirth;
    private String gender;
    private String bloodGroup;
    private String primaryPhone;
    private String knownAllergies;
    private String chronicConditions;
    private String currentMedications;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;
    private boolean organDonor;
    private String insuranceProvider;
    private String insurancePolicyNumber;

    // Granular Patient Privacy Controls (Patient decides what shows on emergency screen)
    private boolean showBloodGroup;
    private boolean showAllergies;
    private boolean showChronicConditions;
    private boolean showCurrentMedications;
    private boolean showEmergencyContact;
    private boolean showOrganDonorStatus;

    private long lastUpdatedAt;

    public EmergencyCardProfile() {
        this.showBloodGroup = true;
        this.showAllergies = true;
        this.showChronicConditions = true;
        this.showCurrentMedications = true;
        this.showEmergencyContact = true;
        this.showOrganDonorStatus = true;
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public EmergencyCardProfile(String patientUid, String arohiId, String fullName,
                                String dateOfBirth, String gender, String bloodGroup,
                                String primaryPhone, String knownAllergies,
                                String chronicConditions, String currentMedications,
                                String emergencyContactName, String emergencyContactPhone,
                                String emergencyContactRelation) {
        this();
        this.patientUid = patientUid;
        this.arohiId = arohiId;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.bloodGroup = bloodGroup;
        this.primaryPhone = primaryPhone;
        this.knownAllergies = knownAllergies;
        this.chronicConditions = chronicConditions;
        this.currentMedications = currentMedications;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
        this.emergencyContactRelation = emergencyContactRelation;
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("patientUid", patientUid);
        map.put("arohiId", arohiId);
        map.put("fullName", fullName);
        map.put("dateOfBirth", dateOfBirth);
        map.put("gender", gender);
        map.put("bloodGroup", bloodGroup);
        map.put("primaryPhone", primaryPhone);
        map.put("knownAllergies", knownAllergies);
        map.put("chronicConditions", chronicConditions);
        map.put("currentMedications", currentMedications);
        map.put("emergencyContactName", emergencyContactName);
        map.put("emergencyContactPhone", emergencyContactPhone);
        map.put("emergencyContactRelation", emergencyContactRelation);
        map.put("organDonor", organDonor);
        map.put("insuranceProvider", insuranceProvider);
        map.put("insurancePolicyNumber", insurancePolicyNumber);

        map.put("showBloodGroup", showBloodGroup);
        map.put("showAllergies", showAllergies);
        map.put("showChronicConditions", showChronicConditions);
        map.put("showCurrentMedications", showCurrentMedications);
        map.put("showEmergencyContact", showEmergencyContact);
        map.put("showOrganDonorStatus", showOrganDonorStatus);
        map.put("lastUpdatedAt", lastUpdatedAt);
        return map;
    }

    public static EmergencyCardProfile fromMap(Map<String, Object> map) {
        if (map == null) return null;
        EmergencyCardProfile p = new EmergencyCardProfile();
        p.setPatientUid((String) map.get("patientUid"));
        p.setArohiId((String) map.get("arohiId"));
        p.setFullName((String) map.get("fullName"));
        p.setDateOfBirth((String) map.get("dateOfBirth"));
        p.setGender((String) map.get("gender"));
        p.setBloodGroup((String) map.get("bloodGroup"));
        p.setPrimaryPhone((String) map.get("primaryPhone"));
        p.setKnownAllergies((String) map.get("knownAllergies"));
        p.setChronicConditions((String) map.get("chronicConditions"));
        p.setCurrentMedications((String) map.get("currentMedications"));
        p.setEmergencyContactName((String) map.get("emergencyContactName"));
        p.setEmergencyContactPhone((String) map.get("emergencyContactPhone"));
        p.setEmergencyContactRelation((String) map.get("emergencyContactRelation"));
        if (map.get("organDonor") instanceof Boolean) {
            p.setOrganDonor((Boolean) map.get("organDonor"));
        }
        p.setInsuranceProvider((String) map.get("insuranceProvider"));
        p.setInsurancePolicyNumber((String) map.get("insurancePolicyNumber"));

        if (map.get("showBloodGroup") instanceof Boolean) {
            p.setShowBloodGroup((Boolean) map.get("showBloodGroup"));
        }
        if (map.get("showAllergies") instanceof Boolean) {
            p.setShowAllergies((Boolean) map.get("showAllergies"));
        }
        if (map.get("showChronicConditions") instanceof Boolean) {
            p.setShowChronicConditions((Boolean) map.get("showChronicConditions"));
        }
        if (map.get("showCurrentMedications") instanceof Boolean) {
            p.setShowCurrentMedications((Boolean) map.get("showCurrentMedications"));
        }
        if (map.get("showEmergencyContact") instanceof Boolean) {
            p.setShowEmergencyContact((Boolean) map.get("showEmergencyContact"));
        }
        if (map.get("showOrganDonorStatus") instanceof Boolean) {
            p.setShowOrganDonorStatus((Boolean) map.get("showOrganDonorStatus"));
        }
        if (map.get("lastUpdatedAt") instanceof Number) {
            p.setLastUpdatedAt(((Number) map.get("lastUpdatedAt")).longValue());
        }
        return p;
    }

    // Getters and Setters
    public String getPatientUid() { return patientUid; }
    public void setPatientUid(String patientUid) { this.patientUid = patientUid; }

    public String getArohiId() { return arohiId; }
    public void setArohiId(String arohiId) { this.arohiId = arohiId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getPrimaryPhone() { return primaryPhone; }
    public void setPrimaryPhone(String primaryPhone) { this.primaryPhone = primaryPhone; }

    public String getKnownAllergies() { return knownAllergies; }
    public void setKnownAllergies(String knownAllergies) { this.knownAllergies = knownAllergies; }

    public String getChronicConditions() { return chronicConditions; }
    public void setChronicConditions(String chronicConditions) { this.chronicConditions = chronicConditions; }

    public String getCurrentMedications() { return currentMedications; }
    public void setCurrentMedications(String currentMedications) { this.currentMedications = currentMedications; }

    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }

    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }

    public String getEmergencyContactRelation() { return emergencyContactRelation; }
    public void setEmergencyContactRelation(String emergencyContactRelation) { this.emergencyContactRelation = emergencyContactRelation; }

    public boolean isOrganDonor() { return organDonor; }
    public void setOrganDonor(boolean organDonor) { this.organDonor = organDonor; }

    public String getInsuranceProvider() { return insuranceProvider; }
    public void setInsuranceProvider(String insuranceProvider) { this.insuranceProvider = insuranceProvider; }

    public String getInsurancePolicyNumber() { return insurancePolicyNumber; }
    public void setInsurancePolicyNumber(String insurancePolicyNumber) { this.insurancePolicyNumber = insurancePolicyNumber; }

    public boolean isShowBloodGroup() { return showBloodGroup; }
    public void setShowBloodGroup(boolean showBloodGroup) { this.showBloodGroup = showBloodGroup; }

    public boolean isShowAllergies() { return showAllergies; }
    public void setShowAllergies(boolean showAllergies) { this.showAllergies = showAllergies; }

    public boolean isShowChronicConditions() { return showChronicConditions; }
    public void setShowChronicConditions(boolean showChronicConditions) { this.showChronicConditions = showChronicConditions; }

    public boolean isShowCurrentMedications() { return showCurrentMedications; }
    public void setShowCurrentMedications(boolean showCurrentMedications) { this.showCurrentMedications = showCurrentMedications; }

    public boolean isShowEmergencyContact() { return showEmergencyContact; }
    public void setShowEmergencyContact(boolean showEmergencyContact) { this.showEmergencyContact = showEmergencyContact; }

    public boolean isShowOrganDonorStatus() { return showOrganDonorStatus; }
    public void setShowOrganDonorStatus(boolean showOrganDonorStatus) { this.showOrganDonorStatus = showOrganDonorStatus; }

    public long getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(long lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
}
