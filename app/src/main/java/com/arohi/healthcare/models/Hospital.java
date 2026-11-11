package com.arohi.healthcare.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hospital entity representing an institution within the AROHI Global Network.
 * Supports multi-tenant isolation, unique hospital codes, and verification workflows.
 */
public class Hospital {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_PENDING = "PENDING_VERIFICATION";
    public static final String STATUS_INACTIVE = "INACTIVE";

    private String hospitalId;
    private String hospitalCode;
    private String hospitalName;
    private String address;
    private String city;
    private String phone;
    private String email;
    private String emergencyHotline;
    private String status;
    private int totalBeds;
    private List<String> supportedDepartments;
    private boolean verified;
    private String description;
    private double latitude;
    private double longitude;
    private long createdAt;
    private long updatedAt;

    public Hospital() {
        this.supportedDepartments = new ArrayList<>();
        this.status = STATUS_PENDING;
        this.verified = false;
        this.latitude = 0.0;
        this.longitude = 0.0;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Hospital(String hospitalId, String hospitalCode, String hospitalName,
                    String address, String city, String phone, String email,
                    String emergencyHotline, String status, int totalBeds,
                    List<String> supportedDepartments, boolean verified, String description) {
        this(hospitalId, hospitalCode, hospitalName, address, city, phone, email,
                emergencyHotline, status, totalBeds, supportedDepartments, verified, description, 0.0, 0.0);
    }

    public Hospital(String hospitalId, String hospitalCode, String hospitalName,
                    String address, String city, String phone, String email,
                    String emergencyHotline, String status, int totalBeds,
                    List<String> supportedDepartments, boolean verified, String description,
                    double latitude, double longitude) {
        this.hospitalId = hospitalId;
        this.hospitalCode = hospitalCode != null ? hospitalCode.toUpperCase().trim() : "";
        this.hospitalName = hospitalName;
        this.address = address;
        this.city = city;
        this.phone = phone;
        this.email = email;
        this.emergencyHotline = emergencyHotline;
        this.status = status != null ? status : STATUS_PENDING;
        this.totalBeds = totalBeds;
        this.supportedDepartments = supportedDepartments != null ? supportedDepartments : new ArrayList<>();
        this.verified = verified;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("hospitalId", hospitalId);
        map.put("hospitalCode", hospitalCode);
        map.put("hospitalName", hospitalName);
        map.put("address", address);
        map.put("city", city);
        map.put("phone", phone);
        map.put("email", email);
        map.put("emergencyHotline", emergencyHotline);
        map.put("status", status);
        map.put("totalBeds", totalBeds);
        map.put("supportedDepartments", supportedDepartments);
        map.put("verified", verified);
        map.put("description", description);
        map.put("latitude", latitude);
        map.put("longitude", longitude);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("hospitalId", hospitalId);
            json.put("hospitalCode", hospitalCode);
            json.put("hospitalName", hospitalName);
            json.put("address", address);
            json.put("city", city);
            json.put("phone", phone);
            json.put("email", email);
            json.put("emergencyHotline", emergencyHotline);
            json.put("status", status);
            json.put("totalBeds", totalBeds);
            JSONArray deptArray = new JSONArray();
            if (supportedDepartments != null) {
                for (String dept : supportedDepartments) {
                    deptArray.put(dept);
                }
            }
            json.put("supportedDepartments", deptArray);
            json.put("verified", verified);
            json.put("description", description);
            json.put("latitude", latitude);
            json.put("longitude", longitude);
            json.put("createdAt", createdAt);
            json.put("updatedAt", updatedAt);
        } catch (JSONException e) {
            // Log or fallback
        }
        return json;
    }

    public static Hospital fromJson(JSONObject json) {
        if (json == null) return null;
        try {
            List<String> depts = new ArrayList<>();
            JSONArray arr = json.optJSONArray("supportedDepartments");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    depts.add(arr.optString(i));
                }
            }
            Hospital h = new Hospital(
                    json.optString("hospitalId"),
                    json.optString("hospitalCode"),
                    json.optString("hospitalName"),
                    json.optString("address"),
                    json.optString("city"),
                    json.optString("phone"),
                    json.optString("email"),
                    json.optString("emergencyHotline"),
                    json.optString("status", STATUS_ACTIVE),
                    json.optInt("totalBeds", 100),
                    depts,
                    json.optBoolean("verified", true),
                    json.optString("description"),
                    json.optDouble("latitude", 0.0),
                    json.optDouble("longitude", 0.0)
            );
            h.setCreatedAt(json.optLong("createdAt", System.currentTimeMillis()));
            h.setUpdatedAt(json.optLong("updatedAt", System.currentTimeMillis()));
            return h;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Calculates the distance in kilometers from a reference location using the Haversine formula.
     * Returns -1 if this hospital has unconfigured (0, 0) coordinates.
     */
    public double calculateDistanceKm(double userLat, double userLng) {
        if (latitude == 0.0 && longitude == 0.0) {
            return -1.0;
        }
        final double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(latitude - userLat);
        double dLng = Math.toRadians(longitude - userLng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(latitude))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equalsIgnoreCase(status);
    }

    // Getters and Setters
    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getHospitalCode() {
        return hospitalCode;
    }

    public void setHospitalCode(String hospitalCode) {
        this.hospitalCode = hospitalCode != null ? hospitalCode.toUpperCase().trim() : "";
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmergencyHotline() {
        return emergencyHotline;
    }

    public void setEmergencyHotline(String emergencyHotline) {
        this.emergencyHotline = emergencyHotline;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalBeds() {
        return totalBeds;
    }

    public void setTotalBeds(int totalBeds) {
        this.totalBeds = totalBeds;
    }

    public List<String> getSupportedDepartments() {
        return supportedDepartments;
    }

    public void setSupportedDepartments(List<String> supportedDepartments) {
        this.supportedDepartments = supportedDepartments;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
