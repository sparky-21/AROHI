package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 4 Digital Prescription containing diagnosis, medicines, dosage, meal instructions,
 * advice, and follow-up date. Issued by verified doctors and stored securely in Cloud Firestore /prescriptions.
 */
public class DigitalPrescription implements Serializable {

    private String prescriptionId;
    private String appointmentId; // Optional link to an appointment

    // Patient Demographics
    private String patientUid;
    private String patientId;
    private String patientName;
    private String patientAgeOrDob;
    private String patientGender;
    private String patientBloodGroup;

    // Doctor Demographics
    private String doctorUid;
    private String doctorId;
    private String doctorName;
    private String doctorSpecialty;
    private String doctorLicense;
    private String doctorDepartment;

    // Clinical Information
    private String diagnosis;
    private String clinicalObservations; // e.g. "BP: 120/80 mmHg • Temp: 98.6°F"
    private List<PrescriptionMedicine> medicines = new ArrayList<>();
    private String adviceInstructions;
    private String followUpDate;

    private long createdAt;
    private long updatedAt;

    public DigitalPrescription() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("prescriptionId", prescriptionId);
        map.put("appointmentId", appointmentId);
        map.put("patientUid", patientUid);
        map.put("patientId", patientId);
        map.put("patientName", patientName);
        map.put("patientAgeOrDob", patientAgeOrDob);
        map.put("patientGender", patientGender);
        map.put("patientBloodGroup", patientBloodGroup);

        map.put("doctorUid", doctorUid);
        map.put("doctorId", doctorId);
        map.put("doctorName", doctorName);
        map.put("doctorSpecialty", doctorSpecialty);
        map.put("doctorLicense", doctorLicense);
        map.put("doctorDepartment", doctorDepartment);

        map.put("diagnosis", diagnosis);
        map.put("clinicalObservations", clinicalObservations);

        List<Map<String, Object>> medsList = new ArrayList<>();
        if (medicines != null) {
            for (PrescriptionMedicine m : medicines) {
                if (m != null) medsList.add(m.toMap());
            }
        }
        map.put("medicines", medsList);

        map.put("adviceInstructions", adviceInstructions);
        map.put("followUpDate", followUpDate);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static DigitalPrescription fromMap(Map<String, Object> map) {
        if (map == null) return null;
        DigitalPrescription p = new DigitalPrescription();
        p.prescriptionId = (String) map.get("prescriptionId");
        p.appointmentId = (String) map.get("appointmentId");
        p.patientUid = (String) map.get("patientUid");
        p.patientId = (String) map.get("patientId");
        p.patientName = (String) map.get("patientName");
        p.patientAgeOrDob = (String) map.get("patientAgeOrDob");
        p.patientGender = (String) map.get("patientGender");
        p.patientBloodGroup = (String) map.get("patientBloodGroup");

        p.doctorUid = (String) map.get("doctorUid");
        p.doctorId = (String) map.get("doctorId");
        p.doctorName = (String) map.get("doctorName");
        p.doctorSpecialty = (String) map.get("doctorSpecialty");
        p.doctorLicense = (String) map.get("doctorLicense");
        p.doctorDepartment = (String) map.get("doctorDepartment");

        p.diagnosis = (String) map.get("diagnosis");
        p.clinicalObservations = (String) map.get("clinicalObservations");

        List<Map<String, Object>> medsList = (List<Map<String, Object>>) map.get("medicines");
        if (medsList != null) {
            for (Map<String, Object> mMap : medsList) {
                p.medicines.add(PrescriptionMedicine.fromMap(mMap));
            }
        }

        p.adviceInstructions = (String) map.get("adviceInstructions");
        p.followUpDate = (String) map.get("followUpDate");
        if (map.get("createdAt") instanceof Number) {
            p.createdAt = ((Number) map.get("createdAt")).longValue();
        }
        if (map.get("updatedAt") instanceof Number) {
            p.updatedAt = ((Number) map.get("updatedAt")).longValue();
        }
        return p;
    }

    // Getters and Setters
    public String getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(String prescriptionId) { this.prescriptionId = prescriptionId; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getPatientUid() { return patientUid; }
    public void setPatientUid(String patientUid) { this.patientUid = patientUid; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientAgeOrDob() { return patientAgeOrDob; }
    public void setPatientAgeOrDob(String patientAgeOrDob) { this.patientAgeOrDob = patientAgeOrDob; }

    public String getPatientGender() { return patientGender; }
    public void setPatientGender(String patientGender) { this.patientGender = patientGender; }

    public String getPatientBloodGroup() { return patientBloodGroup; }
    public void setPatientBloodGroup(String patientBloodGroup) { this.patientBloodGroup = patientBloodGroup; }

    public String getDoctorUid() { return doctorUid; }
    public void setDoctorUid(String doctorUid) { this.doctorUid = doctorUid; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorSpecialty() { return doctorSpecialty; }
    public void setDoctorSpecialty(String doctorSpecialty) { this.doctorSpecialty = doctorSpecialty; }

    public String getDoctorLicense() { return doctorLicense; }
    public void setDoctorLicense(String doctorLicense) { this.doctorLicense = doctorLicense; }

    public String getDoctorDepartment() { return doctorDepartment; }
    public void setDoctorDepartment(String doctorDepartment) { this.doctorDepartment = doctorDepartment; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getClinicalObservations() { return clinicalObservations; }
    public void setClinicalObservations(String clinicalObservations) { this.clinicalObservations = clinicalObservations; }

    public List<PrescriptionMedicine> getMedicines() { return medicines; }
    public void setMedicines(List<PrescriptionMedicine> medicines) { this.medicines = medicines; }

    public String getAdviceInstructions() { return adviceInstructions; }
    public void setAdviceInstructions(String adviceInstructions) { this.adviceInstructions = adviceInstructions; }

    public String getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(String followUpDate) { this.followUpDate = followUpDate; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
