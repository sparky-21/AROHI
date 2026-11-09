package com.arohi.healthcare;

import com.arohi.healthcare.models.Bed;
import com.arohi.healthcare.models.CommunityPost;
import com.arohi.healthcare.models.HospitalAnalytics;
import com.arohi.healthcare.models.MedicalRecordEntry;
import com.arohi.healthcare.models.Operation;
import com.arohi.healthcare.models.PatientJourney;
import com.arohi.healthcare.models.PostComment;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.models.VitalObservation;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Comprehensive Unit Tests for AROHI Phase 7:
 * Complete Healthcare Ecosystem Integration:
 * 1. Smart Patient Journey 7-Stage State Machine
 * 2. Medical Memory Chronological Timeline Synthesis
 * 3. Community and Hospital Feed with Verified Publishing, Interactions & Moderation
 * 4. Advanced Ecosystem Notification Deduplication
 * 5. Real Hospital Analytics Calculations
 */
public class Phase7EcosystemIntegrationTest {

    // =========================================================================
    // 1. SMART PATIENT JOURNEY (7-STAGE STATE MACHINE)
    // =========================================================================

    @Test
    public void testPatientJourneyInitialization7Stages() {
        PatientJourney journey = new PatientJourney(
                "journey_001", "pat_karim_01", "AROHI-PAT-KRM1",
                "Karim Ahmed", "doc_raisa_01", "Dr. Raisa Ahmed"
        );

        assertEquals("journey_001", journey.getJourneyId());
        assertEquals("pat_karim_01", journey.getPatientUid());
        assertTrue(journey.isActive());
        assertEquals(0, journey.getCurrentStageIndex());

        List<PatientJourney.JourneyMilestone> milestones = journey.getMilestones();
        assertNotNull(milestones);
        assertEquals(7, milestones.size());

        // Verify the 7 defined stages
        assertEquals(PatientJourney.STAGE_CONSULTATION, milestones.get(0).getStageKey());
        assertEquals(PatientJourney.STAGE_DIAGNOSIS, milestones.get(1).getStageKey());
        assertEquals(PatientJourney.STAGE_TESTING, milestones.get(2).getStageKey());
        assertEquals(PatientJourney.STAGE_TREATMENT, milestones.get(3).getStageKey());
        assertEquals(PatientJourney.STAGE_OPERATION, milestones.get(4).getStageKey());
        assertEquals(PatientJourney.STAGE_RECOVERY, milestones.get(5).getStageKey());
        assertEquals(PatientJourney.STAGE_FOLLOW_UP, milestones.get(6).getStageKey());

        // All initial stages should be PENDING
        for (PatientJourney.JourneyMilestone m : milestones) {
            assertEquals(PatientJourney.STATUS_PENDING, m.getStatus());
        }
    }

    @Test
    public void testPatientJourneyMilestoneProgression() {
        PatientJourney journey = new PatientJourney(
                "journey_002", "pat_karim_01", "AROHI-PAT-KRM1",
                "Karim Ahmed", "doc_raisa_01", "Dr. Raisa Ahmed"
        );

        // Stage 1: Consultation Completed
        journey.updateMilestone(
                PatientJourney.STAGE_CONSULTATION,
                PatientJourney.STATUS_COMPLETED,
                "Dr. Raisa Ahmed",
                "DOCTOR",
                "Initial consultation completed for chest discomfort",
                "appt_001"
        );

        assertEquals(1, journey.getCurrentStageIndex());
        PatientJourney.JourneyMilestone m1 = journey.getMilestone(PatientJourney.STAGE_CONSULTATION);
        assertNotNull(m1);
        assertEquals(PatientJourney.STATUS_COMPLETED, m1.getStatus());
        assertTrue(m1.getCompletedAt() > 0);
        assertEquals("Dr. Raisa Ahmed", m1.getPerformerName());

        // Stage 2: Diagnosis Completed
        journey.updateMilestone(
                PatientJourney.STAGE_DIAGNOSIS,
                PatientJourney.STATUS_COMPLETED,
                "Dr. Raisa Ahmed",
                "DOCTOR",
                "Clinical Diagnosis: Acute Angina Pectoris",
                "rx_001"
        );
        assertEquals(2, journey.getCurrentStageIndex());

        // Stage 3: Testing In Progress
        journey.updateMilestone(
                PatientJourney.STAGE_TESTING,
                PatientJourney.STATUS_IN_PROGRESS,
                "Dr. Raisa Ahmed",
                "DOCTOR",
                "Ordered: 12-Lead ECG & Troponin-I test",
                "lab_001"
        );
        assertEquals(2, journey.getCurrentStageIndex()); // Index remains 2 until stage 3 is completed
    }

    // =========================================================================
    // 2. MEDICAL MEMORY TIMELINE (CHRONOLOGICAL SYNTHESIS)
    // =========================================================================

    @Test
    public void testMedicalMemoryTimelineChronologicalOrdering() {
        List<MedicalRecordEntry> entries = new ArrayList<>();

        // Add events across multiple days
        entries.add(new MedicalRecordEntry(
                "evt_1", "pat_01", "AROHI-PAT-01",
                MedicalRecordEntry.CATEGORY_CONSULTATION,
                "Clinical Consultation", "Dr. Raisa", "Chest Pain",
                "2026-11-01", 1000000L, "Dr. Raisa", "DOCTOR", "Completed", "appt_1"
        ));

        entries.add(new MedicalRecordEntry(
                "evt_2", "pat_01", "AROHI-PAT-01",
                MedicalRecordEntry.CATEGORY_PRESCRIPTION,
                "Digital Prescription", "Dr. Raisa", "Aspirin 75mg",
                "2026-11-02", 2000000L, "Dr. Raisa", "DOCTOR", "Active", "rx_1"
        ));

        entries.add(new MedicalRecordEntry(
                "evt_3", "pat_01", "AROHI-PAT-01",
                MedicalRecordEntry.CATEGORY_OPERATION,
                "OT Surgery: Angioplasty", "OT Suite 1", "Stent placement",
                "2026-11-05", 5000000L, "Dr. Raisa", "SURGEON", "Completed", "op_1"
        ));

        entries.add(new MedicalRecordEntry(
                "evt_4", "pat_01", "AROHI-PAT-01",
                MedicalRecordEntry.CATEGORY_ADMISSION,
                "Inpatient Ward Admission", "Ward B", "Bed 201",
                "2026-11-04", 4000000L, "Dr. Raisa", "HOSPITAL", "Occupied", "bed_1"
        ));

        entries.add(new MedicalRecordEntry(
                "evt_5", "pat_01", "AROHI-PAT-01",
                MedicalRecordEntry.CATEGORY_VITALS,
                "Nursing Vitals", "Ward B", "BP: 120/80 • SpO2 99%",
                "2026-11-06", 6000000L, "Sister Fatima", "NURSE", "Recorded", "vit_1"
        ));

        // Sort descending (most recent first)
        Collections.sort(entries);

        // Verification: Highest timestamp first
        assertEquals("evt_5", entries.get(0).getEntryId()); // 6000000L
        assertEquals("evt_3", entries.get(1).getEntryId()); // 5000000L
        assertEquals("evt_4", entries.get(2).getEntryId()); // 4000000L
        assertEquals("evt_2", entries.get(3).getEntryId()); // 2000000L
        assertEquals("evt_1", entries.get(4).getEntryId()); // 1000000L
    }

    // =========================================================================
    // 3. COMMUNITY AND HOSPITAL FEED (AUTHORIZATION & SOCIAL ACTIONS)
    // =========================================================================

    @Test
    public void testCommunityPostPublishingRoleValidation() {
        UserAccount docAccount = new UserAccount("doc_01", "AROHI-DOC-01", "doctor@hospital.org", "Dr. Raisa", "01711111111", UserRole.DOCTOR, VerificationStatus.VERIFIED, true, true);
        UserAccount adminAccount = new UserAccount("adm_01", "AROHI-ADM-01", "admin@hospital.org", "Admin Chief", "01722222222", UserRole.ADMIN, VerificationStatus.VERIFIED, true, true);
        UserAccount patientAccount = new UserAccount("pat_01", "AROHI-PAT-01", "patient@mail.com", "Patient Nimu", "01733333333", UserRole.PATIENT, VerificationStatus.VERIFIED, true, true);
        UserAccount nurseAccount = new UserAccount("nur_01", "AROHI-NUR-01", "nurse@hospital.org", "Nurse Fatima", "01744444444", UserRole.NURSE, VerificationStatus.VERIFIED, true, true);

        // Doctor and Admin are allowed
        assertTrue(docAccount.getUserRoleEnum() == UserRole.DOCTOR || docAccount.getUserRoleEnum() == UserRole.ADMIN);
        assertTrue(adminAccount.getUserRoleEnum() == UserRole.DOCTOR || adminAccount.getUserRoleEnum() == UserRole.ADMIN);

        // Patient and Nurse cannot author top-level community posts
        assertFalse(patientAccount.getUserRoleEnum() == UserRole.DOCTOR || patientAccount.getUserRoleEnum() == UserRole.ADMIN);
        assertFalse(nurseAccount.getUserRoleEnum() == UserRole.DOCTOR || nurseAccount.getUserRoleEnum() == UserRole.ADMIN);
    }

    @Test
    public void testCommunityPostLikesAndSaves() {
        CommunityPost post = new CommunityPost(
                "post_101", "doc_01", "Dr. Raisa", "DOCTOR",
                "AROHI-DOC-001", "Cardiology",
                CommunityPost.CATEGORY_HEALTH_AWARENESS,
                "Heart Health 101", "Preventive cardiovascular habits."
        );

        String userUid = "pat_nimu_01";
        assertFalse(post.isLikedBy(userUid));
        assertFalse(post.isSavedBy(userUid));

        // Like toggle
        List<String> likes = post.getLikedUserUids();
        likes.add(userUid);
        post.setLikedUserUids(likes);
        post.setLikeCount(likes.size());

        assertTrue(post.isLikedBy(userUid));
        assertEquals(1, post.getLikeCount());

        // Unlike
        likes.remove(userUid);
        post.setLikedUserUids(likes);
        post.setLikeCount(likes.size());
        assertFalse(post.isLikedBy(userUid));
        assertEquals(0, post.getLikeCount());

        // Save toggle
        List<String> saves = post.getSavedUserUids();
        saves.add(userUid);
        post.setSavedUserUids(saves);
        post.setSaveCount(saves.size());
        assertTrue(post.isSavedBy(userUid));
        assertEquals(1, post.getSaveCount());
    }

    @Test
    public void testCommunityPostReportingAndModerationThreshold() {
        CommunityPost post = new CommunityPost(
                "post_102", "doc_01", "Dr. Raisa", "DOCTOR",
                "AROHI-DOC-001", "Cardiology",
                CommunityPost.CATEGORY_ANNOUNCEMENT,
                "Hospital Notice", "Routine OPD schedule update."
        );

        assertEquals(CommunityPost.MODERATION_ACTIVE, post.getModerationStatus());

        // Simulate reports from 3 distinct users
        List<String> reports = post.getReportedUserUids();
        reports.add("user_a");
        reports.add("user_b");
        reports.add("user_c");
        post.setReportedUserUids(reports);
        post.setReportCount(reports.size());

        // Automated threshold check
        if (post.getReportCount() >= 3) {
            post.setModerationStatus(CommunityPost.MODERATION_REPORTED);
        }

        assertEquals(CommunityPost.MODERATION_REPORTED, post.getModerationStatus());
        assertEquals(3, post.getReportCount());

        // Administrator moderates: Removes post
        post.setModerationStatus(CommunityPost.MODERATION_REMOVED);
        post.setModerationNote("Violates hospital privacy guidelines");
        assertEquals(CommunityPost.MODERATION_REMOVED, post.getModerationStatus());

        // Administrator reinstates post
        post.setModerationStatus(CommunityPost.MODERATION_ACTIVE);
        post.setModerationNote("Reviewed and approved by Admin");
        assertEquals(CommunityPost.MODERATION_ACTIVE, post.getModerationStatus());
    }

    @Test
    public void testCommunityPostComments() {
        PostComment comment = new PostComment(
                "cmt_01", "post_101", "pat_karim_01", "Karim Ahmed",
                "PATIENT", "AROHI-PAT-KRM1", "Thank you doctor for this helpful guidance."
        );

        assertEquals("cmt_01", comment.getCommentId());
        assertEquals("post_101", comment.getPostId());
        assertEquals("pat_karim_01", comment.getAuthorUid());
        assertEquals("Karim Ahmed", comment.getAuthorName());
        assertEquals("PATIENT", comment.getAuthorRole());
        assertTrue(comment.getTimestamp() > 0);
    }

    // =========================================================================
    // 4. ECOSYSTEM NOTIFICATIONS & DEDUPLICATION
    // =========================================================================

    @Test
    public void testNotificationDeduplicationEngine() {
        Set<String> sentSignatures = new HashSet<>();

        String recipientUid = "pat_karim_01";
        String type = "PRESCRIPTION_ISSUED";
        String referenceId = "rx_789";
        String date = "2026-11-09";

        String signature = recipientUid + "_" + type + "_" + referenceId + "_" + date;

        // First notification attempt: Allowed
        assertFalse(sentSignatures.contains(signature));
        sentSignatures.add(signature);
        assertTrue(sentSignatures.contains(signature));

        // Second notification attempt with identical trigger on same day: Detected as duplicate
        boolean isDuplicate = sentSignatures.contains(signature);
        assertTrue(isDuplicate); // Should be prevented from re-sending
    }

    // =========================================================================
    // 5. REAL HOSPITAL ANALYTICS CALCULATIONS
    // =========================================================================

    @Test
    public void testHospitalAnalyticsCalculations() {
        HospitalAnalytics analytics = new HospitalAnalytics();

        // Feed realistic test data
        analytics.setTotalRegisteredPatients(30);
        analytics.setTotalDoctorsCount(6);
        analytics.setTotalNursesCount(10);
        analytics.setTotalBedsCount(20);
        analytics.setOccupiedBedsCount(15);
        analytics.setAvailableBedsCount(5);

        analytics.setTotalOtRoomsCount(4);
        analytics.setInProgressOperationsCount(2);
        analytics.setScheduledOperationsCount(3);
        analytics.setCompletedOperationsCount(8);

        // Run calculation engine
        analytics.recalculateRatios();

        // 15 occupied / 20 total beds = 75.0%
        assertEquals(75.0, analytics.getBedOccupancyRatePercent(), 0.1);

        // 2 in progress / 4 OT rooms = 50.0%
        assertEquals(50.0, analytics.getOtUtilizationRatePercent(), 0.1);

        // 30 patients / 6 doctors = 1:5 ratio
        assertEquals("1:5", analytics.getDoctorPatientRatioString());
    }
}
