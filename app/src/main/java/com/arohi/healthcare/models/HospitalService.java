package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 6 Hospital Service model.
 * Emergency, ICU, Ambulance, Pathology, Blood Bank, Dialysis, Pharmacy.
 */
public class HospitalService implements Serializable {

    private String serviceId;
    private String serviceName;
    private String department;
    private boolean active;
    private String operatingHours;
    private String leadDoctorOrHead;
    private String contactExtension;
    private String description;

    public HospitalService() {
        this.active = true;
    }

    public HospitalService(String serviceId, String serviceName, String department,
                           boolean active, String operatingHours, String leadDoctorOrHead,
                           String contactExtension, String description) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.department = department;
        this.active = active;
        this.operatingHours = operatingHours;
        this.leadDoctorOrHead = leadDoctorOrHead;
        this.contactExtension = contactExtension;
        this.description = description;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("serviceId", serviceId);
        map.put("serviceName", serviceName);
        map.put("department", department);
        map.put("active", active);
        map.put("operatingHours", operatingHours);
        map.put("leadDoctorOrHead", leadDoctorOrHead);
        map.put("contactExtension", contactExtension);
        map.put("description", description);
        return map;
    }

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getOperatingHours() { return operatingHours; }
    public void setOperatingHours(String operatingHours) { this.operatingHours = operatingHours; }

    public String getLeadDoctorOrHead() { return leadDoctorOrHead; }
    public void setLeadDoctorOrHead(String leadDoctorOrHead) { this.leadDoctorOrHead = leadDoctorOrHead; }

    public String getContactExtension() { return contactExtension; }
    public void setContactExtension(String contactExtension) { this.contactExtension = contactExtension; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
