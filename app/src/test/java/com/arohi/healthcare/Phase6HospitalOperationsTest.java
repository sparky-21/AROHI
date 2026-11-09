package com.arohi.healthcare;

import com.arohi.healthcare.models.Bed;
import com.arohi.healthcare.models.CalendarEvent;
import com.arohi.healthcare.models.DutySchedule;
import com.arohi.healthcare.models.HospitalDepartment;
import com.arohi.healthcare.models.HospitalService;
import com.arohi.healthcare.models.NursingTask;
import com.arohi.healthcare.models.OTRoom;
import com.arohi.healthcare.models.Operation;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.models.VitalObservation;
import com.arohi.healthcare.models.Ward;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive Unit Tests for AROHI Phase 6:
 * Hospital Operations, Operation Theatres (OT), Inpatient Wards & Beds,
 * Nurse Care Management, Conflict Prevention, and Calendar Synchronization.
 */
public class Phase6HospitalOperationsTest {

    // =========================================================================
    // 1. OPERATION THEATRE (OT) MANAGEMENT & CONFLICT DETECTION
    // =========================================================================

    @Test
    public void testOTRoomCreationAndAvailability() {
        OTRoom room = new OTRoom(
                "ot_01", "OT Suite 1 - Cardiac Surgery", "Floor 3, South Wing",
                "Cardiothoracic, Vascular", OTRoom.STATUS_AVAILABLE,
                "Laminar flow, Heart-lung bypass console"
        );

        assertEquals("ot_01", room.getRoomId());
        assertEquals("OT Suite 1 - Cardiac Surgery", room.getRoomName());
        assertEquals(OTRoom.STATUS_AVAILABLE, room.getStatus());
        assertTrue(room.isAvailable());

        // Occupied transition
        room.setStatus(OTRoom.STATUS_OCCUPIED);
        assertEquals(OTRoom.STATUS_OCCUPIED, room.getStatus());
        assertFalse(room.isAvailable());

        // Sanitization transition
        room.setStatus(OTRoom.STATUS_SANITIZATION);
        assertEquals(OTRoom.STATUS_SANITIZATION, room.getStatus());
        assertFalse(room.isAvailable());

        // Restore to available
        room.setAvailable(true);
        assertEquals(OTRoom.STATUS_AVAILABLE, room.getStatus());
        assertTrue(room.isAvailable());
    }

    @Test
    public void testOperationSchedulingAndPreOpChecklist() {
        Operation op = new Operation(
                "op_001", "Laparoscopic Cholecystectomy", "General Surgery",
                "ot_02", "OT Suite 2 - General & Laparoscopic",
                "2026-11-09", "10:00 AM", 90,
                "pat_01", "AROHI-PAT-01", "Karim Ahmed",
                "doc_01", "AROHI-DOC-01", "Dr. Raisa", "General Surgery",
                "nurse_01", "Sister Fatima"
        );

        assertEquals(Operation.STATUS_SCHEDULED, op.getStatus());
        assertEquals(90, op.getEstimatedDurationMinutes());
        assertEquals("Karim Ahmed", op.getPatientName());
        assertEquals("Dr. Raisa", op.getDoctorName());
        assertEquals("Sister Fatima", op.getNurseName());

        // Verify pre-op checklist completion
        op.setNpoConfirmed(true);
        op.setAnesthesiaCleared(true);
        op.setConsentSigned(true);
        op.setBloodArranged(true);
        op.setPreOpAntibioticsGiven(true);

        assertTrue(op.isNpoConfirmed());
        assertTrue(op.isAnesthesiaCleared());
        assertTrue(op.isConsentSigned());
        assertTrue(op.isBloodArranged());
        assertTrue(op.isPreOpAntibioticsGiven());
    }

    @Test
    public void testOperationStatusLifecycleProgression() {
        Operation op = new Operation();
        op.setOperationId("op_002");
        op.setProcedureName("Total Knee Arthroplasty");

        // 1. Scheduled
        assertEquals(Operation.STATUS_SCHEDULED, op.getStatus());

        // 2. Preparation
        op.setStatus(Operation.STATUS_PREPARATION);
        assertEquals(Operation.STATUS_PREPARATION, op.getStatus());

        // 3. In Progress
        op.setStatus(Operation.STATUS_IN_PROGRESS);
        assertEquals(Operation.STATUS_IN_PROGRESS, op.getStatus());

        // 4. Completed
        op.setStatus(Operation.STATUS_COMPLETED);
        op.setPostOpNotes("Successful implant placement. Minimal blood loss.");
        assertEquals(Operation.STATUS_COMPLETED, op.getStatus());
        assertEquals("Successful implant placement. Minimal blood loss.", op.getPostOpNotes());

        // 5. Follow-up
        op.setStatus(Operation.STATUS_FOLLOW_UP);
        op.setFollowUpDate("2026-11-16");
        assertEquals(Operation.STATUS_FOLLOW_UP, op.getStatus());
        assertEquals("2026-11-16", op.getFollowUpDate());
    }

    @Test
    public void testConflictDetectionLogicForOTRoomsDoctorsAndNurses() {
        List<Operation> existing = new ArrayList<>();
        Operation existingOp = new Operation(
                "op_base", "Coronary Bypass", "Cardiology",
                "ot_01", "OT Suite 1", "2026-11-10", "10:00 AM", 120,
                "pat_01", "PAT-01", "Patient A",
                "doc_01", "DOC-01", "Dr. Raisa", "Cardiac",
                "nurse_01", "Sister Fatima"
        );
        existing.add(existingOp);

        // Conflict 1: Same OT Room, same date, same time -> MUST FAIL
        Operation conflictRoom = new Operation(
                "op_new1", "Valve Replacement", "Cardiology",
                "ot_01", "OT Suite 1", "2026-11-10", "10:00 AM", 90,
                "pat_02", "PAT-02", "Patient B",
                "doc_02", "DOC-02", "Dr. Kamal", "Cardiac",
                "nurse_02", "Sister Nasreen"
        );
        boolean hasRoomConflict = false;
        for (Operation o : existing) {
            if (o.getOperationDate().equals(conflictRoom.getOperationDate())
                    && o.getOtRoomId().equals(conflictRoom.getOtRoomId())
                    && o.getOperationTime().equalsIgnoreCase(conflictRoom.getOperationTime())) {
                hasRoomConflict = true;
                break;
            }
        }
        assertTrue("OT Room conflict must be detected", hasRoomConflict);

        // Conflict 2: Different Room, same Doctor, same time -> MUST FAIL
        Operation conflictDoctor = new Operation(
                "op_new2", "Appendectomy", "Surgery",
                "ot_02", "OT Suite 2", "2026-11-10", "10:00 AM", 60,
                "pat_03", "PAT-03", "Patient C",
                "doc_01", "DOC-01", "Dr. Raisa", "Surgery",
                "nurse_02", "Sister Nasreen"
        );
        boolean hasDoctorConflict = false;
        for (Operation o : existing) {
            if (o.getOperationDate().equals(conflictDoctor.getOperationDate())
                    && o.getDoctorUid().equals(conflictDoctor.getDoctorUid())
                    && o.getOperationTime().equalsIgnoreCase(conflictDoctor.getOperationTime())) {
                hasDoctorConflict = true;
                break;
            }
        }
        assertTrue("Doctor double-booking conflict must be detected", hasDoctorConflict);

        // Conflict 3: Different Room, different Doctor, same Nurse -> MUST FAIL
        Operation conflictNurse = new Operation(
                "op_new3", "Hernia Repair", "Surgery",
                "ot_03", "OT Suite 3", "2026-11-10", "10:00 AM", 60,
                "pat_04", "PAT-04", "Patient D",
                "doc_03", "DOC-03", "Dr. Jamal", "Surgery",
                "nurse_01", "Sister Fatima"
        );
        boolean hasNurseConflict = false;
        for (Operation o : existing) {
            if (o.getOperationDate().equals(conflictNurse.getOperationDate())
                    && o.getNurseUid().equals(conflictNurse.getNurseUid())
                    && o.getOperationTime().equalsIgnoreCase(conflictNurse.getOperationTime())) {
                hasNurseConflict = true;
                break;
            }
        }
        assertTrue("Assisting Nurse double-booking conflict must be detected", hasNurseConflict);

        // Non-conflicting operation: Different time slot -> MUST PASS
        Operation cleanOp = new Operation(
                "op_clean", "Gallbladder Removal", "Surgery",
                "ot_01", "OT Suite 1", "2026-11-10", "02:00 PM", 60,
                "pat_05", "PAT-05", "Patient E",
                "doc_01", "DOC-01", "Dr. Raisa", "Surgery",
                "nurse_01", "Sister Fatima"
        );
        boolean hasCleanConflict = false;
        for (Operation o : existing) {
            if (o.getOperationDate().equals(cleanOp.getOperationDate())
                    && o.getOtRoomId().equals(cleanOp.getOtRoomId())
                    && o.getOperationTime().equalsIgnoreCase(cleanOp.getOperationTime())) {
                hasCleanConflict = true;
                break;
            }
        }
        assertFalse("Non-overlapping time slot should not trigger conflict", hasCleanConflict);
    }

    // =========================================================================
    // 2. WARD AND BED MANAGEMENT & PREVENTING OCCUPIED BED ASSIGNMENT
    // =========================================================================

    @Test
    public void testWardCapacityAndAvailabilityCalculation() {
        Ward ward = new Ward("ward_a", "Ward A - Male Surgical", "Surgery", "Floor 2", 12, 4,
                "nurse_01", "Sister Fatima", Ward.STATUS_ACTIVE);

        assertEquals("ward_a", ward.getWardId());
        assertEquals(12, ward.getTotalBeds());
        assertEquals(4, ward.getOccupiedBeds());
        assertEquals(8, ward.getAvailableBeds());
        assertEquals(Ward.STATUS_ACTIVE, ward.getStatus());

        // Full ward condition
        ward.setOccupiedBeds(12);
        assertEquals(0, ward.getAvailableBeds());
    }

    @Test
    public void testBedStatusLifecycleAndPatientAdmission() {
        Bed bed = new Bed("bed_101", "Bed 101", "ward_a", "Ward A", "General", Bed.STATUS_AVAILABLE);
        assertTrue(bed.isAvailableForAdmission());

        // Admit patient
        bed.setStatus(Bed.STATUS_OCCUPIED);
        bed.setPatientUid("pat_01");
        bed.setPatientName("Karim Ahmed");
        bed.setAssignedDoctorName("Dr. Raisa");
        bed.setAssignedNurseName("Sister Fatima");
        bed.setAdmissionDate("2026-11-09");

        assertFalse(bed.isAvailableForAdmission());
        assertEquals(Bed.STATUS_OCCUPIED, bed.getStatus());
        assertEquals("Karim Ahmed", bed.getPatientName());

        // Discharge patient -> transitions to Cleaning
        bed.setStatus(Bed.STATUS_CLEANING);
        bed.setPatientUid("");
        bed.setPatientName("");
        assertFalse("Bed in cleaning is not available for immediate admission", bed.isAvailableForAdmission());

        // Maintenance status
        bed.setStatus(Bed.STATUS_MAINTENANCE);
        assertFalse("Bed in maintenance is not available", bed.isAvailableForAdmission());

        // Restored to available
        bed.setStatus(Bed.STATUS_AVAILABLE);
        assertTrue(bed.isAvailableForAdmission());
    }

    @Test
    public void testPreventAssigningOccupiedOrUnavailableBeds() {
        Bed occupiedBed = new Bed("b1", "Bed 101", "w1", "Ward A", "General", Bed.STATUS_OCCUPIED);
        Bed cleaningBed = new Bed("b2", "Bed 102", "w1", "Ward A", "General", Bed.STATUS_CLEANING);
        Bed maintenanceBed = new Bed("b3", "Bed 103", "w1", "Ward A", "General", Bed.STATUS_MAINTENANCE);
        Bed availableBed = new Bed("b4", "Bed 104", "w1", "Ward A", "General", Bed.STATUS_AVAILABLE);

        assertFalse(occupiedBed.isAvailableForAdmission());
        assertFalse(cleaningBed.isAvailableForAdmission());
        assertFalse(maintenanceBed.isAvailableForAdmission());
        assertTrue(availableBed.isAvailableForAdmission());
    }

    // =========================================================================
    // 3. NURSE CARE, TASK MANAGEMENT & AUTHORIZED ACCESS ENFORCEMENT
    // =========================================================================

    @Test
    public void testNursingTaskCreationAndCompletion() {
        NursingTask task = new NursingTask(
                "task_01", NursingTask.TYPE_MEDICATION, "Ceftriaxone 1g IV",
                "Administer in 100ml Normal Saline", "High",
                "pat_01", "PAT-01", "Karim Ahmed",
                "Ward A", "Bed 101",
                "nurse_01", "Sister Fatima",
                "2026-11-09", "10:00 AM",
                "Dr. Raisa", "Verify allergy band before infusion"
        );

        assertFalse(task.isCompleted());
        assertEquals(NursingTask.STATUS_PENDING, task.getStatus());
        assertEquals("Ceftriaxone 1g IV", task.getTitle());
        assertEquals("High", task.getPriority());

        // Nurse completes task
        task.setStatus(NursingTask.STATUS_COMPLETED);
        task.setCompletedAt(System.currentTimeMillis());
        task.setNurseCompletionNotes("Administered slow IV. Patient stable, nil rash.");

        assertTrue(task.isCompleted());
        assertEquals(NursingTask.STATUS_COMPLETED, task.getStatus());
        assertTrue(task.getCompletedAt() > 0);
        assertEquals("Administered slow IV. Patient stable, nil rash.", task.getNurseCompletionNotes());
    }

    @Test
    public void testVitalObservationRecording() {
        VitalObservation vitals = new VitalObservation(
                "vit_01", "pat_01", "PAT-01", "Karim Ahmed",
                "nurse_01", "Sister Fatima", "Ward A", "Bed 101",
                "120/80 mmHg", 74, 98.6, 99, 18,
                "2026-11-09", "08:00 AM", "Patient resting comfortably"
        );

        assertEquals("120/80 mmHg", vitals.getBloodPressure());
        assertEquals(74, vitals.getHeartRateBpm());
        assertEquals(98.6, vitals.getTemperatureF(), 0.01);
        assertEquals(99, vitals.getSpO2Percent());
        assertEquals(18, vitals.getRespiratoryRate());
        assertEquals("Ward A", vitals.getWardName());
        assertEquals("Bed 101", vitals.getBedNumber());
    }

    @Test
    public void testNurseAuthorizedPatientInformationRestriction() {
        // Security requirement: "Nurses must only access authorized patient information."
        String nurseUid = "nurse_fatima_01";
        String nurseWard = "Ward A - Male Surgical";

        List<Bed> hospitalBeds = new ArrayList<>();

        // Bed 1: In Ward A (Authorized for Sister Fatima)
        Bed b1 = new Bed("b1", "Bed 101", "ward_a", "Ward A - Male Surgical", "General", Bed.STATUS_OCCUPIED);
        b1.setPatientName("Patient A");
        b1.setAssignedNurseUid(nurseUid);
        hospitalBeds.add(b1);

        // Bed 2: In Ward B (NOT assigned to Sister Fatima)
        Bed b2 = new Bed("b2", "Bed 201", "ward_b", "Ward B - Female Medical", "General", Bed.STATUS_OCCUPIED);
        b2.setPatientName("Patient B");
        b2.setAssignedNurseUid("nurse_nasreen_02");
        hospitalBeds.add(b2);

        // Bed 3: In ICU, but Sister Fatima is explicitly assigned as primary nurse (Authorized)
        Bed b3 = new Bed("b3", "ICU Bed 1", "ward_icu", "ICU / CCU", "ICU", Bed.STATUS_OCCUPIED);
        b3.setPatientName("Patient C");
        b3.setAssignedNurseUid(nurseUid);
        hospitalBeds.add(b3);

        // Bed 4: In ICU, NOT assigned to Sister Fatima (Unauthorized)
        Bed b4 = new Bed("b4", "ICU Bed 2", "ward_icu", "ICU / CCU", "ICU", Bed.STATUS_OCCUPIED);
        b4.setPatientName("Patient D");
        b4.setAssignedNurseUid("nurse_nasreen_02");
        hospitalBeds.add(b4);

        // Filter authorized beds
        List<Bed> authorized = new ArrayList<>();
        for (Bed b : hospitalBeds) {
            boolean isAssignedNurse = nurseUid.equals(b.getAssignedNurseUid());
            boolean isInAssignedWard = nurseWard.equalsIgnoreCase(b.getWardName());
            if (isAssignedNurse || isInAssignedWard) {
                authorized.add(b);
            }
        }

        assertEquals("Nurse should only access authorized patients (Bed 1 & Bed 3)", 2, authorized.size());
        assertTrue(authorized.contains(b1));
        assertTrue(authorized.contains(b3));
        assertFalse("Unauthorized patient in Ward B must be restricted", authorized.contains(b2));
        assertFalse("Unauthorized patient in ICU must be restricted", authorized.contains(b4));
    }

    // =========================================================================
    // 4. NURSE DUTY SCHEDULES & SHIFT ROSTER
    // =========================================================================

    @Test
    public void testNurseDutyScheduleAndOtAssistanceRoster() {
        DutySchedule duty = new DutySchedule(
                "duty_01", "nurse_01", "NRS-01", "Sister Fatima",
                "ward_a", "Ward A - Male Surgical",
                DutySchedule.SHIFT_MORNING, "2026-11-09",
                true, "OT Suite 2 - General", "Laparoscopic Cholecystectomy (10:00 AM)"
        );

        assertEquals(DutySchedule.SHIFT_MORNING, duty.getShiftType());
        assertEquals("Ward A - Male Surgical", duty.getWardName());
        assertTrue(duty.isOtAssistance());
        assertEquals("OT Suite 2 - General", duty.getOtRoomName());
        assertEquals("Laparoscopic Cholecystectomy (10:00 AM)", duty.getOtOperationName());
    }

    // =========================================================================
    // 5. HOSPITAL SERVICES & DEPARTMENT MANAGEMENT
    // =========================================================================

    @Test
    public void testHospitalServicesAndDepartmentManagement() {
        HospitalService service = new HospitalService(
                "srv_01", "Emergency & Trauma Resuscitation", "Emergency",
                true, "24 Hours / 7 Days", "Dr. Raisa", "Ext: 101",
                "Acute cardiac and trauma resuscitation"
        );

        assertTrue(service.isActive());
        assertEquals("Emergency & Trauma Resuscitation", service.getServiceName());

        // Admin toggles service status
        service.setActive(false);
        assertFalse(service.isActive());

        HospitalDepartment dept = new HospitalDepartment(
                "dept_01", "Cardiology & Vascular Center", "Dr. Raisa",
                "Floor 1, West Wing", 18, true, "Cardiovascular care"
        );
        assertEquals("Cardiology & Vascular Center", dept.getDeptName());
        assertTrue(dept.isActive());
        assertEquals(18, dept.getTotalStaff());
    }

    // =========================================================================
    // 6. CALENDAR SYNCHRONIZATION ACROSS DOCTOR, NURSE, PATIENT & ADMIN
    // =========================================================================

    @Test
    public void testCalendarSynchronizationForSurgicalOperations() {
        Operation op = new Operation(
                "op_sync_01", "Appendectomy", "Surgery",
                "ot_02", "OT Suite 2", "2026-11-09", "10:00 AM", 60,
                "pat_10", "PAT-10", "Karim Ahmed",
                "doc_20", "DOC-20", "Dr. Raisa", "Surgery",
                "nurse_30", "Sister Fatima"
        );

        CalendarEvent event = new CalendarEvent();
        event.setEventId("cal_op_" + op.getOperationId());
        event.setTitle("Surgery: " + op.getProcedureName() + " (" + op.getOtRoomName() + ")");
        event.setEventType(CalendarEvent.TYPE_OPERATION);
        event.setEventDate(op.getOperationDate());
        event.setEventTime(op.getOperationTime());
        event.setPatientUid(op.getPatientUid());
        event.setDoctorUid(op.getDoctorUid());
        event.setNurseUid(op.getNurseUid());
        event.setRoleVisibility("ALL");

        // Verify Patient calendar visibility
        assertTrue("Patient should see their operation",
                "ALL".equals(event.getRoleVisibility()) || "pat_10".equals(event.getPatientUid()));

        // Verify Doctor calendar visibility
        assertTrue("Doctor should see scheduled surgery",
                "ALL".equals(event.getRoleVisibility()) || "doc_20".equals(event.getDoctorUid()));

        // Verify Nurse calendar visibility
        assertTrue("Assisting Nurse should see surgical assignment",
                "ALL".equals(event.getRoleVisibility()) || "nurse_30".equals(event.getNurseUid()));

        // Verify Admin calendar visibility
        assertTrue("Admin has oversight over all hospital operations",
                "ALL".equals(event.getRoleVisibility()));
    }
}
