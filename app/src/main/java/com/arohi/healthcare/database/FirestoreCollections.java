package com.arohi.healthcare.database;

/**
 * Centralized Firestore collection and field names for the AROHI healthcare ecosystem.
 */
public final class FirestoreCollections {

    private FirestoreCollections() {
        // Prevent instantiation
    }

    // Core Identity & Role Collections (Phase 1 Active)
    public static final String USERS = "users";
    public static final String PATIENTS = "patients";
    public static final String DOCTORS = "doctors";
    public static final String NURSES = "nurses";
    public static final String ADMINISTRATORS = "administrators";
    public static final String ADMIN_AUTHORIZATIONS = "adminAuthorizations";
    public static final String AUDIT_LOGS = "auditLogs";

    // Connected Clinical Workflow Collections (Prepared for Subsequent Phases)
    public static final String DOCTOR_REQUESTS = "doctorRequests";
    public static final String CONNECTIONS = "connections";
    public static final String CONVERSATIONS = "conversations";
    public static final String MESSAGES = "messages";
    public static final String APPOINTMENTS = "appointments";
    public static final String PRESCRIPTIONS = "prescriptions";
    public static final String LABORATORY_REQUESTS = "laboratoryRequests";
    public static final String LABORATORY_REPORTS = "laboratoryReports";
    public static final String PHARMACY_PRODUCTS = "pharmacyProducts";
    public static final String PHARMACY_ORDERS = "pharmacyOrders";
    public static final String OPERATIONS = "operations";
    public static final String OPERATION_ROOMS = "operationRooms";
    public static final String WARDS = "wards";
    public static final String BEDS = "beds";
    public static final String NURSE_ASSIGNMENTS = "nurseAssignments";
    public static final String MEDICAL_RECORDS = "medicalRecords";
    public static final String MEDICAL_TIMELINE = "medicalTimeline";
    public static final String CALENDAR_EVENTS = "calendarEvents";
    public static final String NOTIFICATIONS = "notifications";
    public static final String EMERGENCY_PROFILES = "emergencyProfiles";
    public static final String FAMILY_MEMBERS = "familyMembers";
    public static final String HOSPITAL_DEPARTMENTS = "hospitalDepartments";
    public static final String COMMUNITY_POSTS = "communityPosts";
}
