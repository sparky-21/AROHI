package com.arohi.healthcare;

import com.arohi.healthcare.models.AppNotification;
import com.arohi.healthcare.models.Appointment;
import com.arohi.healthcare.models.CalendarEvent;
import com.arohi.healthcare.models.DigitalPrescription;
import com.arohi.healthcare.models.LaboratoryRequest;
import com.arohi.healthcare.models.MedicalRecordEntry;
import com.arohi.healthcare.models.PrescriptionMedicine;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive Unit Tests for AROHI Phase 4:
 * Appointments, Unified Calendar, Digital Prescriptions, and Laboratory Management.
 */
public class Phase4ClinicalWorkflowTest {

    // =========================================================================
    // 1. APPOINTMENT MANAGEMENT & SCHEDULING CONFLICT DETECTION
    // =========================================================================
    @Test
    public void testAppointmentCreationAndSerialization() {
        Appointment appt = new Appointment(
                "appt_101",
                "uid_pat_1", "AROHI-PAT-1001", "Rahim Uddin", "+8801711223344",
                "uid_doc_1", "AROHI-DOC-2001", "Dr. Farhana Rahman", "Cardiology",
                1000.0,
                "2026-11-12", "10:30 AM",
                Appointment.TYPE_IN_PERSON, "Chest pain and hypertension", "Patient prefers morning slot"
        );

        assertTrue(appt.isPending());
        assertTrue(appt.isActive());
        assertFalse(appt.isConfirmed());
        assertFalse(appt.isCancelled());

        Map<String, Object> map = appt.toMap();
        assertEquals("appt_101", map.get("appointmentId"));
        assertEquals("2026-11-12", map.get("appointmentDate"));
        assertEquals("10:30 AM", map.get("appointmentTime"));
        assertEquals("Pending", map.get("status"));
        assertEquals(1000.0, (Double) map.get("consultationFee"), 0.001);

        // Status transition to Confirmed
        appt.setStatus(Appointment.STATUS_CONFIRMED);
        appt.setDoctorNotes("Confirmed consultation in Cardiology Room 302");
        assertTrue(appt.isConfirmed());
        assertTrue(appt.isActive());

        // Status transition to Cancelled
        appt.setStatus(Appointment.STATUS_CANCELLED);
        appt.setCancellationReason("Patient had a scheduling emergency");
        assertTrue(appt.isCancelled());
        assertFalse(appt.isActive());
    }

    @Test
    public void testAppointmentSchedulingConflictLogic() {
        List<Appointment> existingAppointments = new ArrayList<>();

        Appointment existingAppt = new Appointment(
                "appt_existing",
                "uid_pat_1", "AROHI-PAT-1001", "Rahim Uddin", "+8801711223344",
                "uid_doc_1", "AROHI-DOC-2001", "Dr. Farhana Rahman", "Cardiology",
                1000.0,
                "2026-11-15", "11:00 AM",
                Appointment.TYPE_IN_PERSON, "Routine checkup", ""
        );
        existingAppt.setStatus(Appointment.STATUS_CONFIRMED);
        existingAppointments.add(existingAppt);

        // Scenario A: Another patient tries to book Dr. Farhana at the exact same date & time -> CONFLICT!
        Appointment conflictingDoctorAppt = new Appointment(
                "appt_conflict_doc",
                "uid_pat_2", "AROHI-PAT-1002", "Salma Begum", "+8801811223344",
                "uid_doc_1", "AROHI-DOC-2001", "Dr. Farhana Rahman", "Cardiology",
                1000.0,
                "2026-11-15", "11:00 AM",
                Appointment.TYPE_IN_PERSON, "Palpitations", ""
        );

        boolean hasDoctorConflict = false;
        for (Appointment a : existingAppointments) {
            if (a.isActive()
                    && a.getDoctorUid().equals(conflictingDoctorAppt.getDoctorUid())
                    && a.getAppointmentDate().equals(conflictingDoctorAppt.getAppointmentDate())
                    && a.getAppointmentTime().equalsIgnoreCase(conflictingDoctorAppt.getAppointmentTime())) {
                hasDoctorConflict = true;
                break;
            }
        }
        assertTrue("Doctor slot collision must be detected", hasDoctorConflict);

        // Scenario B: Same patient tries to book another doctor at the exact same date & time -> CONFLICT!
        Appointment conflictingPatientAppt = new Appointment(
                "appt_conflict_pat",
                "uid_pat_1", "AROHI-PAT-1001", "Rahim Uddin", "+8801711223344",
                "uid_doc_2", "AROHI-DOC-2002", "Dr. Kamal Specialist", "Neurology",
                800.0,
                "2026-11-15", "11:00 AM",
                Appointment.TYPE_IN_PERSON, "Migraine check", ""
        );

        boolean hasPatientConflict = false;
        for (Appointment a : existingAppointments) {
            if (a.isActive()
                    && a.getPatientUid().equals(conflictingPatientAppt.getPatientUid())
                    && a.getAppointmentDate().equals(conflictingPatientAppt.getAppointmentDate())
                    && a.getAppointmentTime().equalsIgnoreCase(conflictingPatientAppt.getAppointmentTime())) {
                hasPatientConflict = true;
                break;
            }
        }
        assertTrue("Patient simultaneous appointment collision must be detected", hasPatientConflict);

        // Scenario C: Non-conflicting different time slot -> NO CONFLICT
        Appointment validAppt = new Appointment(
                "appt_valid",
                "uid_pat_2", "AROHI-PAT-1002", "Salma Begum", "+8801811223344",
                "uid_doc_1", "AROHI-DOC-2001", "Dr. Farhana Rahman", "Cardiology",
                1000.0,
                "2026-11-15", "04:30 PM",
                Appointment.TYPE_IN_PERSON, "Routine checkup", ""
        );

        boolean hasAnyConflict = false;
        for (Appointment a : existingAppointments) {
            if (a.isActive()) {
                if (a.getDoctorUid().equals(validAppt.getDoctorUid())
                        && a.getAppointmentDate().equals(validAppt.getAppointmentDate())
                        && a.getAppointmentTime().equalsIgnoreCase(validAppt.getAppointmentTime())) {
                    hasAnyConflict = true;
                    break;
                }
            }
        }
        assertFalse("Different time slots must not conflict", hasAnyConflict);
    }

    // =========================================================================
    // 2. DIGITAL PRESCRIPTIONS
    // =========================================================================
    @Test
    public void testDigitalPrescriptionSerializationAndMedicineList() {
        DigitalPrescription rx = new DigitalPrescription();
        rx.setPrescriptionId("rx_778899");
        rx.setPatientUid("uid_pat_1");
        rx.setPatientId("AROHI-PAT-1001");
        rx.setPatientName("Rahim Uddin");
        rx.setPatientAgeOrDob("1992-05-14");
        rx.setPatientGender("Male");
        rx.setPatientBloodGroup("A+");

        rx.setDoctorUid("uid_doc_1");
        rx.setDoctorId("AROHI-DOC-2001");
        rx.setDoctorName("Dr. Farhana Rahman");
        rx.setDoctorSpecialty("Cardiology");
        rx.setDoctorLicense("BMDC-45901");
        rx.setDoctorDepartment("Cardiology");

        rx.setDiagnosis("Hypertension Stage 1 & Mild Dyslipidemia");
        rx.setClinicalObservations("BP: 145/92 mmHg • Heart Rate: 78 bpm • Weight: 74 kg");
        rx.setAdviceInstructions("Low sodium diet, 30 min daily brisk walking, avoid fatty meals.");
        rx.setFollowUpDate("2026-11-30");

        List<PrescriptionMedicine> medicines = new ArrayList<>();
        medicines.add(new PrescriptionMedicine("Amlodipine", "5 mg", "1+0+0", "30 Days", "Morning After Meal", "Monitor BP weekly"));
        medicines.add(new PrescriptionMedicine("Atorvastatin", "10 mg", "0+0+1", "30 Days", "At Bedtime", "Take after dinner"));
        medicines.add(new PrescriptionMedicine("Aspirin (Ecosprin)", "75 mg", "0+1+0", "30 Days", "After Meal", "Take with plenty of water"));
        rx.setMedicines(medicines);

        Map<String, Object> serialized = rx.toMap();
        assertEquals("rx_778899", serialized.get("prescriptionId"));
        assertEquals("Hypertension Stage 1 & Mild Dyslipidemia", serialized.get("diagnosis"));
        assertEquals("BMDC-45901", serialized.get("doctorLicense"));

        // Deserialize back
        DigitalPrescription deserialized = DigitalPrescription.fromMap(serialized);
        assertNotNull(deserialized);
        assertEquals(rx.getPrescriptionId(), deserialized.getPrescriptionId());
        assertEquals(3, deserialized.getMedicines().size());
        assertEquals("Amlodipine", deserialized.getMedicines().get(0).getMedicineName());
        assertEquals("5 mg", deserialized.getMedicines().get(0).getDosage());
        assertEquals("Morning After Meal", deserialized.getMedicines().get(0).getMealInstruction());
        assertEquals("Atorvastatin", deserialized.getMedicines().get(1).getMedicineName());
        assertEquals("2026-11-30", deserialized.getFollowUpDate());
    }

    // =========================================================================
    // 3. LABORATORY MANAGEMENT & STATUS PROGRESSION
    // =========================================================================
    @Test
    public void testLaboratoryRequestWorkflowAndStatusProgression() {
        LaboratoryRequest lab = new LaboratoryRequest();
        lab.setRequestId("lab_303");
        lab.setPatientUid("uid_pat_1");
        lab.setPatientId("AROHI-PAT-1001");
        lab.setPatientName("Rahim Uddin");
        lab.setDoctorUid("uid_doc_1");
        lab.setDoctorName("Dr. Farhana Rahman");
        lab.setDoctorSpecialty("Cardiology");

        lab.setTestName("Lipid Profile & Serum Creatinine");
        lab.setTestCategory("Biochemistry");
        lab.setClinicalIndication("Hypertension & Baseline Cardiovascular Workup");
        lab.setUrgency(LaboratoryRequest.URGENCY_ROUTINE);

        // Step 1: Initial state on doctor ordering
        assertEquals(LaboratoryRequest.STATUS_REQUESTED, lab.getStatus());
        assertFalse(lab.isReportReady());
        assertFalse(lab.isReviewed());

        // Step 2: Sample collection
        lab.setStatus(LaboratoryRequest.STATUS_SAMPLE_COLLECTED);
        assertEquals(LaboratoryRequest.STATUS_SAMPLE_COLLECTED, lab.getStatus());

        // Step 3: Processing in lab
        lab.setStatus(LaboratoryRequest.STATUS_PROCESSING);
        assertEquals(LaboratoryRequest.STATUS_PROCESSING, lab.getStatus());

        // Step 4: Report uploaded
        lab.setStatus(LaboratoryRequest.STATUS_REPORT_READY);
        lab.setReportFileName("Lipid_Profile_Report.pdf");
        lab.setReportFileType("PDF");
        lab.setReportFindingsSummary("Total Cholesterol: 210 mg/dL (Borderline High), LDL: 135 mg/dL, HDL: 44 mg/dL, Creatinine: 0.9 mg/dL (Normal).");
        lab.setReportUploadedAt(System.currentTimeMillis());
        assertTrue(lab.isReportReady());
        assertFalse(lab.isReviewed());

        // Step 5: Doctor clinical review
        lab.setStatus(LaboratoryRequest.STATUS_REVIEWED);
        lab.setDoctorReviewNotes("Mild hypercholesterolemia confirmed. Renal function is normal. Continue prescribed Statin and repeat in 3 months.");
        lab.setDoctorReviewedAt(System.currentTimeMillis());
        assertTrue(lab.isReportReady());
        assertTrue(lab.isReviewed());
    }

    // =========================================================================
    // 4. UNIFIED CALENDAR & NURSE/ADMIN PHASE 6 INTEGRATION READINESS
    // =========================================================================
    @Test
    public void testUnifiedCalendarEventSortingAndPhase6Readiness() {
        List<CalendarEvent> events = new ArrayList<>();

        CalendarEvent ev1 = new CalendarEvent(
                "evt_1", "Consultation: Rahim Uddin", "Cardiology OPD",
                CalendarEvent.TYPE_APPOINTMENT, "2026-11-20", "11:00 AM", 1795000000000L,
                "uid_pat_1", "Rahim Uddin", "uid_doc_1", "Dr. Farhana", "Room 302", "appt_1"
        );

        CalendarEvent ev2 = new CalendarEvent(
                "evt_2", "Diagnostic Test: Lipid Profile", "Biochemistry Sample",
                CalendarEvent.TYPE_LAB_TEST, "2026-11-18", "08:30 AM", 1794800000000L,
                "uid_pat_1", "Rahim Uddin", "uid_doc_1", "Dr. Farhana", "Lab Hub", "lab_1"
        );

        // Phase 6 Integration: Nurse Shift & OT Surgery events
        CalendarEvent evNurseShift = new CalendarEvent(
                "evt_nurse_1", "Duty Shift: Ward 4B", "Morning Inpatient Care",
                CalendarEvent.TYPE_NURSE_SHIFT, "2026-11-19", "07:00 AM", 1794900000000L,
                "", "", "", "", "General Ward 4B", "shift_101"
        );
        evNurseShift.setNurseUid("uid_nrs_1");
        evNurseShift.setNurseName("Nadia Islam");
        evNurseShift.setRoleVisibility("NURSE");

        events.add(ev1);
        events.add(ev2);
        events.add(evNurseShift);

        // Sort chronologically ascending
        Collections.sort(events);

        assertEquals("evt_2", events.get(0).getEventId());          // 1794800000000L (Nov 18)
        assertEquals("evt_nurse_1", events.get(1).getEventId());    // 1794900000000L (Nov 19)
        assertEquals("evt_1", events.get(2).getEventId());          // 1795000000000L (Nov 20)
        assertEquals("NURSE", events.get(1).getRoleVisibility());
        assertEquals("Nadia Islam", events.get(1).getNurseName());
    }

    // =========================================================================
    // 5. UNIFIED MEDICAL RECORD CHRONOLOGICAL TIMELINE
    // =========================================================================
    @Test
    public void testMedicalRecordTimelineChronologicalSorting() {
        List<MedicalRecordEntry> timeline = new ArrayList<>();

        MedicalRecordEntry e1 = new MedicalRecordEntry(
                "entry_1", "uid_pat_1", "AROHI-PAT-1001",
                MedicalRecordEntry.CATEGORY_CONSULTATION,
                "Consultation: Dr. Specialist", "General Medicine",
                "Initial consultation for seasonal flu", "2026-10-01", 1000L,
                "Dr. Specialist", "DOCTOR", "Completed", "appt_1"
        );

        MedicalRecordEntry e2 = new MedicalRecordEntry(
                "entry_2", "uid_pat_1", "AROHI-PAT-1001",
                MedicalRecordEntry.CATEGORY_PRESCRIPTION,
                "Prescription: Bronchitis", "Medicine OPD",
                "Amoxicillin, Paracetamol", "2026-10-15", 2000L,
                "Dr. Specialist", "DOCTOR", "Active", "rx_1"
        );

        MedicalRecordEntry e3 = new MedicalRecordEntry(
                "entry_3", "uid_pat_1", "AROHI-PAT-1001",
                MedicalRecordEntry.CATEGORY_LAB_REPORT,
                "Lab: CBC Report", "Hematology",
                "Normal WBC, Hb 14.2 g/dL", "2026-11-01", 3000L,
                "Dr. Specialist", "LABORATORY", "Reviewed by Doctor", "lab_1"
        );

        timeline.add(e1);
        timeline.add(e2);
        timeline.add(e3);

        // Sort chronologically descending (most recent first)
        Collections.sort(timeline);

        assertEquals("entry_3", timeline.get(0).getEntryId()); // Most recent (timestamp 3000)
        assertEquals("entry_2", timeline.get(1).getEntryId()); // timestamp 2000
        assertEquals("entry_1", timeline.get(2).getEntryId()); // timestamp 1000
    }

    // =========================================================================
    // 6. NOTIFICATION TYPE VALIDATION
    // =========================================================================
    @Test
    public void testNotificationTypesIntegrity() {
        AppNotification notif = new AppNotification(
                "notif_rx_1",
                "uid_pat_1",
                "uid_doc_1",
                "Dr. Farhana",
                AppNotification.TYPE_PRESCRIPTION_ISSUED,
                "New Digital Prescription Issued",
                "Dr. Farhana issued an authorized prescription. Tap to view and export PDF.",
                "rx_1"
        );

        assertEquals(AppNotification.TYPE_PRESCRIPTION_ISSUED, notif.getType());
        assertFalse(notif.isRead());
        // Verify privacy: no raw diagnostic or dosage details leaked in body
        assertFalse(notif.getBody().contains("Amlodipine"));
        assertFalse(notif.getBody().contains("5 mg"));
    }
}
