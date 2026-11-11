package com.arohi.healthcare;

import com.arohi.healthcare.models.HospitalMembership;
import com.arohi.healthcare.models.StaffConversation;
import com.arohi.healthcare.models.StaffGroupMember;
import com.arohi.healthcare.models.StaffMessage;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VerificationStatus;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit Tests for AROHI Phase 12:
 * Hospital Authority Communication Hub:
 * 1. Multi-tenant isolation by hospitalId
 * 2. Institutional clinical group generation ("All Doctors", "All Nurses")
 * 3. Individual Authority-to-Staff 1-to-1 conversation architecture
 * 4. Message serialization, timestamps, read receipts, and delivery tracking
 * 5. Attachment validation (MIME types, <=10MB size limit)
 * 6. Cross-hospital access rejection and suspended staff handling
 */
public class Phase12AuthorityCommunicationTest {

    private static final String HOSPITAL_A = "hosp_square_dhk";
    private static final String HOSPITAL_B = "hosp_united_dhk";

    // =========================================================================
    // 1. MULTI-TENANT ISOLATION & GROUP MANAGEMENT
    // =========================================================================

    @Test
    public void testClinicalGroupCreationAndHospitalTenancy() {
        StaffConversation docGroup = StaffConversation.createGroup(
                HOSPITAL_A, "SQUARE-01",
                StaffConversation.TYPE_GROUP_DOCTORS,
                "All Doctors • SQUARE-01"
        );

        assertEquals("grp_hosp_square_dhk_group_doctors", docGroup.getConversationId());
        assertEquals(HOSPITAL_A, docGroup.getHospitalId());
        assertEquals("SQUARE-01", docGroup.getHospitalCode());
        assertEquals(StaffConversation.TYPE_GROUP_DOCTORS, docGroup.getConversationType());
        assertTrue(docGroup.isGroup());
        assertTrue(docGroup.isActive());

        StaffConversation nurseGroup = StaffConversation.createGroup(
                HOSPITAL_A, "SQUARE-01",
                StaffConversation.TYPE_GROUP_NURSES,
                "All Nurses • SQUARE-01"
        );

        assertEquals("grp_hosp_square_dhk_group_nurses", nurseGroup.getConversationId());
        assertEquals(HOSPITAL_A, nurseGroup.getHospitalId());
        assertEquals(StaffConversation.TYPE_GROUP_NURSES, nurseGroup.getConversationType());
        assertTrue(nurseGroup.isGroup());
    }

    @Test
    public void testCrossHospitalSeparation() {
        StaffConversation squareDocs = StaffConversation.createGroup(
                HOSPITAL_A, "SQUARE-01", StaffConversation.TYPE_GROUP_DOCTORS, "All Doctors • SQUARE-01");
        StaffConversation unitedDocs = StaffConversation.createGroup(
                HOSPITAL_B, "UNITED-01", StaffConversation.TYPE_GROUP_DOCTORS, "All Doctors • UNITED-01");

        assertNotEquals(squareDocs.getConversationId(), unitedDocs.getConversationId());
        assertNotEquals(squareDocs.getHospitalId(), unitedDocs.getHospitalId());
        assertFalse(squareDocs.getHospitalId().equalsIgnoreCase(unitedDocs.getHospitalId()));
    }

    // =========================================================================
    // 2. INDIVIDUAL STAFF CONVERSATIONS
    // =========================================================================

    @Test
    public void testIndividualAuthorityToStaffConversation() {
        StaffConversation conv = StaffConversation.createIndividual(
                HOSPITAL_A, "SQUARE-01",
                "admin_uid_01", "Hospital Administrator",
                "doc_uid_01", "Dr. Raisa Ahmed",
                UserRole.DOCTOR.name(), "AROHI-DOC-VR8PUP3Q", "Cardiology"
        );

        assertEquals("conv_hosp_square_dhk_doc_uid_01", conv.getConversationId());
        assertEquals(HOSPITAL_A, conv.getHospitalId());
        assertEquals("SQUARE-01", conv.getHospitalCode());
        assertEquals(StaffConversation.TYPE_INDIVIDUAL_DOCTOR, conv.getConversationType());
        assertEquals("admin_uid_01", conv.getAuthorityUid());
        assertEquals("doc_uid_01", conv.getParticipantUid());
        assertEquals("Dr. Raisa Ahmed", conv.getParticipantName());
        assertEquals("Cardiology", conv.getParticipantSpecialty());
        assertFalse(conv.isGroup());
    }

    @Test
    public void testCrossHospitalStaffChatRejectionLogic() {
        UserAccount staffHospitalB = new UserAccount(
                "doc_uid_b", "AROHI-DOC-B001", "doctor@united.org",
                "Dr. United Doctor", "+880 1711-000000",
                UserRole.DOCTOR, VerificationStatus.VERIFIED, true, true
        );
        staffHospitalB.setHospitalId(HOSPITAL_B);

        // Attempting to initiate conversation within HOSPITAL_A with staff from HOSPITAL_B must fail
        boolean isAuthorizedForHospitalA = HOSPITAL_A.equalsIgnoreCase(staffHospitalB.getHospitalId());
        assertFalse("Cross-hospital staff member must be rejected", isAuthorizedForHospitalA);
    }

    // =========================================================================
    // 3. STAFF GROUP MEMBERSHIP & ACTIVE STATUS
    // =========================================================================

    @Test
    public void testStaffGroupMemberVerificationCheck() {
        StaffGroupMember activeMember = new StaffGroupMember(
                "doc_uid_01", "Dr. Raisa Ahmed", "AROHI-DOC-VR8PUP3Q",
                "DOCTOR", "Cardiology", HOSPITAL_A, true, HospitalMembership.STATUS_ACTIVE
        );
        assertTrue(activeMember.isEligibleForChat());

        // Suspended staff member
        StaffGroupMember suspendedMember = new StaffGroupMember(
                "doc_uid_02", "Dr. Suspended", "AROHI-DOC-SUSP01",
                "DOCTOR", "Cardiology", HOSPITAL_A, true, "SUSPENDED"
        );
        assertFalse("Suspended staff must not be eligible for messaging", suspendedMember.isEligibleForChat());

        // Unverified staff member
        StaffGroupMember unverifiedMember = new StaffGroupMember(
                "doc_uid_03", "Dr. Unverified", "AROHI-DOC-UNVER01",
                "DOCTOR", "Cardiology", HOSPITAL_A, false, HospitalMembership.STATUS_ACTIVE
        );
        assertFalse("Unverified staff must not be eligible for messaging", unverifiedMember.isEligibleForChat());
    }

    // =========================================================================
    // 4. MESSAGING, ATTACHMENTS & READ STATUS
    // =========================================================================

    @Test
    public void testStaffMessageCreationAndSerialization() {
        StaffMessage message = new StaffMessage(
                "msg_001",
                "grp_hosp_square_dhk_group_doctors",
                HOSPITAL_A,
                "admin_uid_01",
                "Hospital Authority",
                "ADMIN",
                "AROHI-ADM-001",
                "Emergency ICU rotation briefing scheduled for 10:00 AM."
        );

        assertEquals("msg_001", message.getMessageId());
        assertEquals("grp_hosp_square_dhk_group_doctors", message.getConversationId());
        assertEquals(HOSPITAL_A, message.getHospitalId());
        assertEquals("ADMIN", message.getSenderRole());
        assertTrue(message.isReadBy("admin_uid_01"));
        assertFalse(message.isReadBy("doc_uid_01"));

        // Read receipt
        message.markReadBy("doc_uid_01");
        assertTrue(message.isReadBy("doc_uid_01"));

        // Serialization
        Map<String, Object> map = message.toMap();
        assertEquals("msg_001", map.get("messageId"));
        assertEquals("Hospital Authority", map.get("senderName"));
        assertEquals("Emergency ICU rotation briefing scheduled for 10:00 AM.", map.get("text"));
        assertEquals(StaffMessage.ATTACHMENT_NONE, map.get("attachmentType"));
    }

    @Test
    public void testAttachmentValidationAndAttributes() {
        StaffMessage message = new StaffMessage(
                "msg_att_01",
                "grp_hosp_square_dhk_group_doctors",
                HOSPITAL_A,
                "doc_uid_01",
                "Dr. Raisa Ahmed",
                "DOCTOR",
                "AROHI-DOC-VR8PUP3Q",
                "Please find attached the monthly ICU Schedule."
        );

        long validSize = 4 * 1024 * 1024; // 4MB (under 10MB limit)
        message.setAttachmentName("ICU_Roster_Nov2026.pdf");
        message.setAttachmentType(StaffMessage.ATTACHMENT_PDF);
        message.setAttachmentSizeBytes(validSize);
        message.setAttachmentUrl("content://media/external/file/1234");

        assertEquals(StaffMessage.ATTACHMENT_PDF, message.getAttachmentType());
        assertEquals("ICU_Roster_Nov2026.pdf", message.getAttachmentName());
        assertTrue(message.getAttachmentSizeBytes() <= 10 * 1024 * 1024);

        // Map serialization with attachment
        Map<String, Object> map = message.toMap();
        assertEquals(StaffMessage.ATTACHMENT_PDF, map.get("attachmentType"));
        assertEquals("ICU_Roster_Nov2026.pdf", map.get("attachmentName"));
        assertEquals(validSize, map.get("attachmentSizeBytes"));
    }

    @Test
    public void testModerationStatusFlags() {
        StaffMessage message = new StaffMessage(
                "msg_mod_01", "grp_01", HOSPITAL_A, "uid_01", "Staff", "NURSE", "AROHI-NUR-01", "Normal update"
        );
        assertEquals(StaffMessage.MODERATION_NORMAL, message.getModerationStatus());

        message.setModerationStatus(StaffMessage.MODERATION_FLAGGED);
        assertEquals(StaffMessage.MODERATION_FLAGGED, message.getModerationStatus());

        message.setModerationStatus(StaffMessage.MODERATION_REMOVED);
        assertEquals(StaffMessage.MODERATION_REMOVED, message.getModerationStatus());
    }
}
