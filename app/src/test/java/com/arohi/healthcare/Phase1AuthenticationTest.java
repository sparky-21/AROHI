package com.arohi.healthcare;

import com.arohi.healthcare.models.DoctorProfile;
import com.arohi.healthcare.models.NurseProfile;
import com.arohi.healthcare.models.PatientProfile;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.utils.IdGenerator;
import com.arohi.healthcare.utils.ValidationUtils;
import org.junit.Test;
import java.util.Map;
import static org.junit.Assert.*;

/**
 * Unit tests for AROHI Phase 1: Foundation & Authentication.
 * Validates input validation, role identification, unique ID generation,
 * and strict PENDING verification enforcement for Doctors, Nurses, and Admins.
 */
public class Phase1AuthenticationTest {

    @Test
    public void testEmailAndPasswordValidation() {
        assertTrue(ValidationUtils.isValidEmail("patient.rahim@arohi.health"));
        assertFalse(ValidationUtils.isValidEmail("invalid-email"));
        assertFalse(ValidationUtils.isValidEmail(""));

        assertTrue(ValidationUtils.isValidPassword("ArohiCare2026"));
        assertFalse(ValidationUtils.isValidPassword("short1"));
        assertFalse(ValidationUtils.isValidPassword("onlyletters"));
        assertFalse(ValidationUtils.isValidPassword("123456789"));
        assertTrue(ValidationUtils.doPasswordsMatch("ArohiCare2026", "ArohiCare2026"));
        assertFalse(ValidationUtils.doPasswordsMatch("ArohiCare2026", "Mismatch2026"));
    }

    @Test
    public void testPatientMedicalInputsValidation() {
        assertTrue(ValidationUtils.isValidDateOfBirth("1995/08/24"));
        assertTrue(ValidationUtils.isValidDateOfBirth("1995-08-24"));
        assertFalse(ValidationUtils.isValidDateOfBirth("24/08/1995"));

        assertTrue(ValidationUtils.isValidBloodGroup("O+"));
        assertTrue(ValidationUtils.isValidBloodGroup("ab-"));
        assertFalse(ValidationUtils.isValidBloodGroup("C+"));
    }

    @Test
    public void testDoctorAndNurseCredentialValidation() {
        assertTrue(ValidationUtils.isValidProfessionalLicense("BMDC-84920"));
        assertFalse(ValidationUtils.isValidProfessionalLicense("12"));

        assertTrue(ValidationUtils.isValidExperienceYears("12"));
        assertFalse(ValidationUtils.isValidExperienceYears("-1"));
        assertFalse(ValidationUtils.isValidExperienceYears("90"));

        assertTrue(ValidationUtils.isValidConsultationFee("800"));
        assertFalse(ValidationUtils.isValidConsultationFee("-50"));
    }

    @Test
    public void testUnauthorizedAdminTokenRejected() {
        assertFalse(ValidationUtils.isValidAdminTokenFormat("admin123"));
        assertFalse(ValidationUtils.isValidAdminTokenFormat(""));
        assertTrue(ValidationUtils.isValidAdminTokenFormat("AROHI-AUTH-9842AB1C"));
    }

    @Test
    public void testDoctorAndNurseDefaultToPendingVerification() {
        String docUid = "uid_doc_991234";
        String docId = IdGenerator.generateRoleIdentifier(UserRole.DOCTOR, docUid);
        assertTrue(docId.startsWith("AROHI-DOC-"));

        DoctorProfile doctor = new DoctorProfile(
                docUid, docId, "Dr. Farhana Rahman", "dr.farhana@arohi.health",
                "+8801711223344", "BMDC-45901", "Cardiology", "MBBS, FCPS",
                "Cardiology", 10, 1000.0
        );
        assertEquals(VerificationStatus.PENDING.getKey(), doctor.getVerificationStatus());

        String nurseUid = "uid_nrs_771234";
        String nurseId = IdGenerator.generateRoleIdentifier(UserRole.NURSE, nurseUid);
        assertTrue(nurseId.startsWith("AROHI-NRS-"));

        NurseProfile nurse = new NurseProfile(
                nurseUid, nurseId, "Nadia Islam", "nadia.nurse@arohi.health",
                "+8801811223344", "BNMC-33812", "BSc in Nursing",
                "ICU Ward A", "Morning"
        );
        assertEquals(VerificationStatus.PENDING.getKey(), nurse.getVerificationStatus());
    }

    @Test
    public void testUserAccountSerializationAndRoleResolution() {
        UserAccount doctorAccount = new UserAccount(
                "uid_doc_1", "AROHI-DOC-UIDDOC1", "doc@arohi.health",
                "Dr. Kamal", "+8801700000000",
                UserRole.DOCTOR, VerificationStatus.PENDING, false, false
        );

        assertEquals(UserRole.DOCTOR, doctorAccount.getUserRoleEnum());
        assertEquals(VerificationStatus.PENDING, doctorAccount.getVerificationStatusEnum());
        assertFalse(doctorAccount.isVerifiedProfessionalOrPatient());

        Map<String, Object> serialized = doctorAccount.toMap();
        assertEquals("DOCTOR", serialized.get("role"));
        assertEquals("PENDING", serialized.get("verificationStatus"));

        PatientProfile patientProfile = new PatientProfile(
                "uid_pat_1", "AROHI-PAT-UIDPAT1", "Tanvir Ahmed", "tanvir@arohi.health",
                "+8801900000000", "1998-04-12", "Male", "B+",
                "Karim Ahmed", "+8801911111111", "Penicillin"
        );
        assertEquals("AROHI-PAT-UIDPAT1", patientProfile.toMap().get("patientId"));
        assertEquals(VerificationStatus.PENDING.getKey(), patientProfile.getVerificationStatus());
    }
}
