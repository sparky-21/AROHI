package com.arohi.healthcare.models;

import java.io.Serializable;

/**
 * Phase 7 Hospital Analytics Model.
 * Represents genuine, dynamically calculated hospital metrics derived exclusively
 * from real application data across all departments and workflows.
 */
public class HospitalAnalytics implements Serializable {

    // Patient & Population
    private int totalRegisteredPatients;
    private int activePatientsCount;

    // Clinical Staff & Ratios
    private int totalDoctorsCount;
    private int verifiedDoctorsCount;
    private int totalNursesCount;
    private int totalDepartmentsCount;
    private String doctorPatientRatioString; // e.g. "1:15"

    // Inpatient Wards & Beds
    private int totalBedsCount;
    private int occupiedBedsCount;
    private int availableBedsCount;
    private int cleaningBedsCount;
    private int maintenanceBedsCount;
    private double bedOccupancyRatePercent;

    // Operation Theatres & Surgeries
    private int totalOtRoomsCount;
    private int totalOperationsCount;
    private int scheduledOperationsCount;
    private int inProgressOperationsCount;
    private int completedOperationsCount;
    private double otUtilizationRatePercent;

    // Consultations & Pharmacy
    private int totalAppointmentsCount;
    private int completedAppointmentsCount;
    private int cancelledAppointmentsCount;
    private int totalPrescriptionsCount;
    private int activePrescriptionsCount;

    // Laboratory Diagnostics
    private int totalLabRequestsCount;
    private int pendingLabRequestsCount;
    private int completedLabRequestsCount;

    // Community & Compliance
    private int totalCommunityPostsCount;
    private int reportedPostsCount;
    private int pendingStaffVerificationsCount;

    private long computedAt;

    public HospitalAnalytics() {
        this.computedAt = System.currentTimeMillis();
        this.doctorPatientRatioString = "N/A";
    }

    public void recalculateRatios() {
        if (totalBedsCount > 0) {
            this.bedOccupancyRatePercent = Math.round(((double) occupiedBedsCount / totalBedsCount * 100.0) * 10.0) / 10.0;
        } else {
            this.bedOccupancyRatePercent = 0.0;
        }

        if (totalOtRoomsCount > 0) {
            this.otUtilizationRatePercent = Math.round(((double) inProgressOperationsCount / totalOtRoomsCount * 100.0) * 10.0) / 10.0;
        } else {
            this.otUtilizationRatePercent = 0.0;
        }

        if (totalDoctorsCount > 0 && totalRegisteredPatients > 0) {
            int ratio = Math.max(1, totalRegisteredPatients / totalDoctorsCount);
            this.doctorPatientRatioString = "1:" + ratio;
        } else {
            this.doctorPatientRatioString = "1:1";
        }
    }

    // Getters and Setters
    public int getTotalRegisteredPatients() { return totalRegisteredPatients; }
    public void setTotalRegisteredPatients(int totalRegisteredPatients) { this.totalRegisteredPatients = totalRegisteredPatients; }

    public int getActivePatientsCount() { return activePatientsCount; }
    public void setActivePatientsCount(int activePatientsCount) { this.activePatientsCount = activePatientsCount; }

    public int getTotalDoctorsCount() { return totalDoctorsCount; }
    public void setTotalDoctorsCount(int totalDoctorsCount) { this.totalDoctorsCount = totalDoctorsCount; }

    public int getVerifiedDoctorsCount() { return verifiedDoctorsCount; }
    public void setVerifiedDoctorsCount(int verifiedDoctorsCount) { this.verifiedDoctorsCount = verifiedDoctorsCount; }

    public int getTotalNursesCount() { return totalNursesCount; }
    public void setTotalNursesCount(int totalNursesCount) { this.totalNursesCount = totalNursesCount; }

    public int getTotalDepartmentsCount() { return totalDepartmentsCount; }
    public void setTotalDepartmentsCount(int totalDepartmentsCount) { this.totalDepartmentsCount = totalDepartmentsCount; }

    public String getDoctorPatientRatioString() { return doctorPatientRatioString; }
    public void setDoctorPatientRatioString(String doctorPatientRatioString) { this.doctorPatientRatioString = doctorPatientRatioString; }

    public int getTotalBedsCount() { return totalBedsCount; }
    public void setTotalBedsCount(int totalBedsCount) { this.totalBedsCount = totalBedsCount; }

    public int getOccupiedBedsCount() { return occupiedBedsCount; }
    public void setOccupiedBedsCount(int occupiedBedsCount) { this.occupiedBedsCount = occupiedBedsCount; }

    public int getAvailableBedsCount() { return availableBedsCount; }
    public void setAvailableBedsCount(int availableBedsCount) { this.availableBedsCount = availableBedsCount; }

    public int getCleaningBedsCount() { return cleaningBedsCount; }
    public void setCleaningBedsCount(int cleaningBedsCount) { this.cleaningBedsCount = cleaningBedsCount; }

    public int getMaintenanceBedsCount() { return maintenanceBedsCount; }
    public void setMaintenanceBedsCount(int maintenanceBedsCount) { this.maintenanceBedsCount = maintenanceBedsCount; }

    public double getBedOccupancyRatePercent() { return bedOccupancyRatePercent; }
    public void setBedOccupancyRatePercent(double bedOccupancyRatePercent) { this.bedOccupancyRatePercent = bedOccupancyRatePercent; }

    public int getTotalOtRoomsCount() { return totalOtRoomsCount; }
    public void setTotalOtRoomsCount(int totalOtRoomsCount) { this.totalOtRoomsCount = totalOtRoomsCount; }

    public int getTotalOperationsCount() { return totalOperationsCount; }
    public void setTotalOperationsCount(int totalOperationsCount) { this.totalOperationsCount = totalOperationsCount; }

    public int getScheduledOperationsCount() { return scheduledOperationsCount; }
    public void setScheduledOperationsCount(int scheduledOperationsCount) { this.scheduledOperationsCount = scheduledOperationsCount; }

    public int getInProgressOperationsCount() { return inProgressOperationsCount; }
    public void setInProgressOperationsCount(int inProgressOperationsCount) { this.inProgressOperationsCount = inProgressOperationsCount; }

    public int getCompletedOperationsCount() { return completedOperationsCount; }
    public void setCompletedOperationsCount(int completedOperationsCount) { this.completedOperationsCount = completedOperationsCount; }

    public double getOtUtilizationRatePercent() { return otUtilizationRatePercent; }
    public void setOtUtilizationRatePercent(double otUtilizationRatePercent) { this.otUtilizationRatePercent = otUtilizationRatePercent; }

    public int getTotalAppointmentsCount() { return totalAppointmentsCount; }
    public void setTotalAppointmentsCount(int totalAppointmentsCount) { this.totalAppointmentsCount = totalAppointmentsCount; }

    public int getCompletedAppointmentsCount() { return completedAppointmentsCount; }
    public void setCompletedAppointmentsCount(int completedAppointmentsCount) { this.completedAppointmentsCount = completedAppointmentsCount; }

    public int getCancelledAppointmentsCount() { return cancelledAppointmentsCount; }
    public void setCancelledAppointmentsCount(int cancelledAppointmentsCount) { this.cancelledAppointmentsCount = cancelledAppointmentsCount; }

    public int getTotalPrescriptionsCount() { return totalPrescriptionsCount; }
    public void setTotalPrescriptionsCount(int totalPrescriptionsCount) { this.totalPrescriptionsCount = totalPrescriptionsCount; }

    public int getActivePrescriptionsCount() { return activePrescriptionsCount; }
    public void setActivePrescriptionsCount(int activePrescriptionsCount) { this.activePrescriptionsCount = activePrescriptionsCount; }

    public int getTotalLabRequestsCount() { return totalLabRequestsCount; }
    public void setTotalLabRequestsCount(int totalLabRequestsCount) { this.totalLabRequestsCount = totalLabRequestsCount; }

    public int getPendingLabRequestsCount() { return pendingLabRequestsCount; }
    public void setPendingLabRequestsCount(int pendingLabRequestsCount) { this.pendingLabRequestsCount = pendingLabRequestsCount; }

    public int getCompletedLabRequestsCount() { return completedLabRequestsCount; }
    public void setCompletedLabRequestsCount(int completedLabRequestsCount) { this.completedLabRequestsCount = completedLabRequestsCount; }

    public int getTotalCommunityPostsCount() { return totalCommunityPostsCount; }
    public void setTotalCommunityPostsCount(int totalCommunityPostsCount) { this.totalCommunityPostsCount = totalCommunityPostsCount; }

    public int getReportedPostsCount() { return reportedPostsCount; }
    public void setReportedPostsCount(int reportedPostsCount) { this.reportedPostsCount = reportedPostsCount; }

    public int getPendingStaffVerificationsCount() { return pendingStaffVerificationsCount; }
    public void setPendingStaffVerificationsCount(int pendingStaffVerificationsCount) { this.pendingStaffVerificationsCount = pendingStaffVerificationsCount; }

    public long getComputedAt() { return computedAt; }
    public void setComputedAt(long computedAt) { this.computedAt = computedAt; }
}
