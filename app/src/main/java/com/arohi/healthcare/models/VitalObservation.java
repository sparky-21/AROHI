package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 6 Patient Vital Observation record.
 * Recorded by nurses during ward care and procedure monitoring.
 */
public class VitalObservation implements Serializable {

    private String vitalId;
    private String patientUid;
    private String patientId;
    private String patientName;
    private String nurseUid;
    private String nurseName;
    private String wardName;
    private String bedNumber;

    // Vital measurements
    private String bloodPressure;     // e.g. "120/80 mmHg"
    private int heartRateBpm;         // e.g. 76 bpm
    private double temperatureF;      // e.g. 98.6 F
    private int spO2Percent;          // e.g. 98 %
    private int respiratoryRate;      // e.g. 18 breaths/min

    private long recordedTimestamp;
    private String recordedDate;      // YYYY-MM-DD
    private String recordedTime;      // e.g. "10:30 AM"
    private String clinicalNotes;

    public VitalObservation() {
        this.recordedTimestamp = System.currentTimeMillis();
    }

    public VitalObservation(String vitalId, String patientUid, String patientId, String patientName,
                            String nurseUid, String nurseName, String wardName, String bedNumber,
                            String bloodPressure, int heartRateBpm, double temperatureF,
                            int spO2Percent, int respiratoryRate, String recordedDate,
                            String recordedTime, String clinicalNotes) {
        this.vitalId = vitalId;
        this.patientUid = patientUid;
        this.patientId = patientId;
        this.patientName = patientName;
        this.nurseUid = nurseUid;
        this.nurseName = nurseName;
        this.wardName = wardName;
        this.bedNumber = bedNumber;
        this.bloodPressure = bloodPressure;
        this.heartRateBpm = heartRateBpm;
        this.temperatureF = temperatureF;
        this.spO2Percent = spO2Percent;
        this.respiratoryRate = respiratoryRate;
        this.recordedDate = recordedDate;
        this.recordedTime = recordedTime;
        this.clinicalNotes = clinicalNotes;
        this.recordedTimestamp = System.currentTimeMillis();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("vitalId", vitalId);
        map.put("patientUid", patientUid);
        map.put("patientId", patientId);
        map.put("patientName", patientName);
        map.put("nurseUid", nurseUid);
        map.put("nurseName", nurseName);
        map.put("wardName", wardName);
        map.put("bedNumber", bedNumber);
        map.put("bloodPressure", bloodPressure);
        map.put("heartRateBpm", heartRateBpm);
        map.put("temperatureF", temperatureF);
        map.put("spO2Percent", spO2Percent);
        map.put("respiratoryRate", respiratoryRate);
        map.put("recordedTimestamp", recordedTimestamp);
        map.put("recordedDate", recordedDate);
        map.put("recordedTime", recordedTime);
        map.put("clinicalNotes", clinicalNotes != null ? clinicalNotes : "");
        return map;
    }

    public String getVitalId() { return vitalId; }
    public void setVitalId(String vitalId) { this.vitalId = vitalId; }

    public String getPatientUid() { return patientUid; }
    public void setPatientUid(String patientUid) { this.patientUid = patientUid; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getNurseUid() { return nurseUid; }
    public void setNurseUid(String nurseUid) { this.nurseUid = nurseUid; }

    public String getNurseName() { return nurseName; }
    public void setNurseName(String nurseName) { this.nurseName = nurseName; }

    public String getWardName() { return wardName; }
    public void setWardName(String wardName) { this.wardName = wardName; }

    public String getBedNumber() { return bedNumber; }
    public void setBedNumber(String bedNumber) { this.bedNumber = bedNumber; }

    public String getBloodPressure() { return bloodPressure; }
    public void setBloodPressure(String bloodPressure) { this.bloodPressure = bloodPressure; }

    public int getHeartRateBpm() { return heartRateBpm; }
    public void setHeartRateBpm(int heartRateBpm) { this.heartRateBpm = heartRateBpm; }

    public double getTemperatureF() { return temperatureF; }
    public void setTemperatureF(double temperatureF) { this.temperatureF = temperatureF; }

    public int getSpO2Percent() { return spO2Percent; }
    public void setSpO2Percent(int spO2Percent) { this.spO2Percent = spO2Percent; }

    public int getRespiratoryRate() { return respiratoryRate; }
    public void setRespiratoryRate(int respiratoryRate) { this.respiratoryRate = respiratoryRate; }

    public long getRecordedTimestamp() { return recordedTimestamp; }
    public void setRecordedTimestamp(long recordedTimestamp) { this.recordedTimestamp = recordedTimestamp; }

    public String getRecordedDate() { return recordedDate; }
    public void setRecordedDate(String recordedDate) { this.recordedDate = recordedDate; }

    public String getRecordedTime() { return recordedTime; }
    public void setRecordedTime(String recordedTime) { this.recordedTime = recordedTime; }

    public String getClinicalNotes() { return clinicalNotes; }
    public void setClinicalNotes(String clinicalNotes) { this.clinicalNotes = clinicalNotes; }
}
