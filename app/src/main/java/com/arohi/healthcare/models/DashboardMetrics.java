package com.arohi.healthcare.models;

/**
 * Aggregated live dashboard metrics retrieved from Cloud Firestore for each role.
 * All counts reflect real Firestore documents (0 when no documents exist yet).
 */
public class DashboardMetrics {

    // Patient Dashboard Metrics
    private int upcomingAppointmentsCount;
    private int recentPrescriptionsCount;
    private int pendingReportsCount;
    private int activeMedicationsCount;
    private int connectedDoctorsCount;
    private int unreadNotificationsCount;

    // Doctor Dashboard Metrics
    private int todayAppointmentsCount;
    private int pendingRequestsCount;
    private int acceptedPatientsCount;
    private int upcomingOperationsCount;
    private int pendingReportReviewsCount;
    private int followUpRemindersCount;

    // Nurse Dashboard Metrics
    private int assignedPatientsCount;
    private String assignedWardName = "UNASSIGNED";
    private int todayTasksCount;
    private int completedTasksCount;
    private int medicationSchedulesCount;
    private int upcomingProceduresCount;
    private int operationAssistanceCount;
    private String dutyShift = "Not Scheduled";

    // Admin Dashboard Operational Metrics
    private int totalPatientsCount;
    private int verifiedPatientsCount;
    private int totalDoctorsCount;
    private int verifiedDoctorsCount;
    private int totalNursesCount;
    private int verifiedNursesCount;
    private int pendingVerificationsCount;
    private int departmentsCount;
    private int wardsCount;
    private int availableBedsCount;
    private int totalBedsCount;
    private int otRoomsCount;
    private int scheduledSurgeriesCount;
    private int announcementsCount;

    private String latestAnnouncementTitle = "No hospital announcements published yet.";
    private String latestAnnouncementBody = "Verified Hospital Authorities and Doctors can publish updates here.";

    public int getUpcomingAppointmentsCount() {
        return upcomingAppointmentsCount;
    }

    public void setUpcomingAppointmentsCount(int upcomingAppointmentsCount) {
        this.upcomingAppointmentsCount = upcomingAppointmentsCount;
    }

    public int getRecentPrescriptionsCount() {
        return recentPrescriptionsCount;
    }

    public void setRecentPrescriptionsCount(int recentPrescriptionsCount) {
        this.recentPrescriptionsCount = recentPrescriptionsCount;
    }

    public int getPendingReportsCount() {
        return pendingReportsCount;
    }

    public void setPendingReportsCount(int pendingReportsCount) {
        this.pendingReportsCount = pendingReportsCount;
    }

    public int getActiveMedicationsCount() {
        return activeMedicationsCount;
    }

    public void setActiveMedicationsCount(int activeMedicationsCount) {
        this.activeMedicationsCount = activeMedicationsCount;
    }

    public int getConnectedDoctorsCount() {
        return connectedDoctorsCount;
    }

    public void setConnectedDoctorsCount(int connectedDoctorsCount) {
        this.connectedDoctorsCount = connectedDoctorsCount;
    }

    public int getUnreadNotificationsCount() {
        return unreadNotificationsCount;
    }

    public void setUnreadNotificationsCount(int unreadNotificationsCount) {
        this.unreadNotificationsCount = unreadNotificationsCount;
    }

    public int getTodayAppointmentsCount() {
        return todayAppointmentsCount;
    }

    public void setTodayAppointmentsCount(int todayAppointmentsCount) {
        this.todayAppointmentsCount = todayAppointmentsCount;
    }

    public int getPendingRequestsCount() {
        return pendingRequestsCount;
    }

    public void setPendingRequestsCount(int pendingRequestsCount) {
        this.pendingRequestsCount = pendingRequestsCount;
    }

    public int getAcceptedPatientsCount() {
        return acceptedPatientsCount;
    }

    public void setAcceptedPatientsCount(int acceptedPatientsCount) {
        this.acceptedPatientsCount = acceptedPatientsCount;
    }

    public int getUpcomingOperationsCount() {
        return upcomingOperationsCount;
    }

    public void setUpcomingOperationsCount(int upcomingOperationsCount) {
        this.upcomingOperationsCount = upcomingOperationsCount;
    }

    public int getPendingReportReviewsCount() {
        return pendingReportReviewsCount;
    }

    public void setPendingReportReviewsCount(int pendingReportReviewsCount) {
        this.pendingReportReviewsCount = pendingReportReviewsCount;
    }

    public int getFollowUpRemindersCount() {
        return followUpRemindersCount;
    }

    public void setFollowUpRemindersCount(int followUpRemindersCount) {
        this.followUpRemindersCount = followUpRemindersCount;
    }

    public int getAssignedPatientsCount() {
        return assignedPatientsCount;
    }

    public void setAssignedPatientsCount(int assignedPatientsCount) {
        this.assignedPatientsCount = assignedPatientsCount;
    }

    public String getAssignedWardName() {
        return assignedWardName;
    }

    public void setAssignedWardName(String assignedWardName) {
        this.assignedWardName = assignedWardName;
    }

    public int getTodayTasksCount() {
        return todayTasksCount;
    }

    public void setTodayTasksCount(int todayTasksCount) {
        this.todayTasksCount = todayTasksCount;
    }

    public int getCompletedTasksCount() {
        return completedTasksCount;
    }

    public void setCompletedTasksCount(int completedTasksCount) {
        this.completedTasksCount = completedTasksCount;
    }

    public int getMedicationSchedulesCount() {
        return medicationSchedulesCount;
    }

    public void setMedicationSchedulesCount(int medicationSchedulesCount) {
        this.medicationSchedulesCount = medicationSchedulesCount;
    }

    public int getUpcomingProceduresCount() {
        return upcomingProceduresCount;
    }

    public void setUpcomingProceduresCount(int upcomingProceduresCount) {
        this.upcomingProceduresCount = upcomingProceduresCount;
    }

    public int getOperationAssistanceCount() {
        return operationAssistanceCount;
    }

    public void setOperationAssistanceCount(int operationAssistanceCount) {
        this.operationAssistanceCount = operationAssistanceCount;
    }

    public String getDutyShift() {
        return dutyShift;
    }

    public void setDutyShift(String dutyShift) {
        this.dutyShift = dutyShift;
    }

    public int getTotalPatientsCount() {
        return totalPatientsCount;
    }

    public void setTotalPatientsCount(int totalPatientsCount) {
        this.totalPatientsCount = totalPatientsCount;
    }

    public int getVerifiedPatientsCount() {
        return verifiedPatientsCount;
    }

    public void setVerifiedPatientsCount(int verifiedPatientsCount) {
        this.verifiedPatientsCount = verifiedPatientsCount;
    }

    public int getTotalDoctorsCount() {
        return totalDoctorsCount;
    }

    public void setTotalDoctorsCount(int totalDoctorsCount) {
        this.totalDoctorsCount = totalDoctorsCount;
    }

    public int getVerifiedDoctorsCount() {
        return verifiedDoctorsCount;
    }

    public void setVerifiedDoctorsCount(int verifiedDoctorsCount) {
        this.verifiedDoctorsCount = verifiedDoctorsCount;
    }

    public int getTotalNursesCount() {
        return totalNursesCount;
    }

    public void setTotalNursesCount(int totalNursesCount) {
        this.totalNursesCount = totalNursesCount;
    }

    public int getVerifiedNursesCount() {
        return verifiedNursesCount;
    }

    public void setVerifiedNursesCount(int verifiedNursesCount) {
        this.verifiedNursesCount = verifiedNursesCount;
    }

    public int getPendingVerificationsCount() {
        return pendingVerificationsCount;
    }

    public void setPendingVerificationsCount(int pendingVerificationsCount) {
        this.pendingVerificationsCount = pendingVerificationsCount;
    }

    public int getDepartmentsCount() {
        return departmentsCount;
    }

    public void setDepartmentsCount(int departmentsCount) {
        this.departmentsCount = departmentsCount;
    }

    public int getWardsCount() {
        return wardsCount;
    }

    public void setWardsCount(int wardsCount) {
        this.wardsCount = wardsCount;
    }

    public int getAvailableBedsCount() {
        return availableBedsCount;
    }

    public void setAvailableBedsCount(int availableBedsCount) {
        this.availableBedsCount = availableBedsCount;
    }

    public int getOtRoomsCount() {
        return otRoomsCount;
    }

    public void setOtRoomsCount(int otRoomsCount) {
        this.otRoomsCount = otRoomsCount;
    }

    public int getTotalBedsCount() {
        return totalBedsCount;
    }

    public void setTotalBedsCount(int totalBedsCount) {
        this.totalBedsCount = totalBedsCount;
    }

    public int getScheduledSurgeriesCount() {
        return scheduledSurgeriesCount;
    }

    public void setScheduledSurgeriesCount(int scheduledSurgeriesCount) {
        this.scheduledSurgeriesCount = scheduledSurgeriesCount;
    }

    public int getAnnouncementsCount() {
        return announcementsCount;
    }

    public void setAnnouncementsCount(int announcementsCount) {
        this.announcementsCount = announcementsCount;
    }

    public String getLatestAnnouncementTitle() {
        return latestAnnouncementTitle;
    }

    public void setLatestAnnouncementTitle(String latestAnnouncementTitle) {
        this.latestAnnouncementTitle = latestAnnouncementTitle;
    }

    public String getLatestAnnouncementBody() {
        return latestAnnouncementBody;
    }

    public void setLatestAnnouncementBody(String latestAnnouncementBody) {
        this.latestAnnouncementBody = latestAnnouncementBody;
    }
}
