package com.arohi.healthcare;

import com.arohi.healthcare.models.Hospital;
import com.arohi.healthcare.models.HospitalMembership;
import com.arohi.healthcare.models.SuperAdminAuthorization;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VerificationStatus;

import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive Unit Tests for AROHI Phase 9:
 * Global Hospital Network & Multi-Tenant Architecture:
 * 1. Hospital Model, Code Normalization & Serialization
 * 2. Multi-Tenant User Identity vs Hospital Membership Separation
 * 3. Hospital Code Uniqueness and Verification Workflow
 * 4. Super Admin Governance & Status Transitions
 * 5. Multi-Tenant Data Isolation by HospitalId
 * 6. Backward Compatibility & Safe Legacy Record Migration
 */
public class Phase9MultiTenantTest {

    // =========================================================================
    // 1. HOSPITAL MODEL & SERIALIZATION
    // =========================================================================

    @Test
    public void testHospitalModelInitializationAndCodeNormalization() {
        List<String> depts = Arrays.asList("Cardiology", "Neurology", "Emergency");
        Hospital hospital = new Hospital(
                "hosp_square_dhk",
                "  square-01  ",
                "Square Hospital Dhaka",
                "18/F West Panthapath",
                "Dhaka",
                "+880 2 8159457",
                "info@square.com",
                "+880 1713-377775",
                Hospital.STATUS_ACTIVE,
                400,
                depts,
                true,
                "Leading tertiary hospital in Dhaka"
        );

        // Code must be trimmed and uppercase
        assertEquals("SQUARE-01", hospital.getHospitalCode());
        assertEquals("Square Hospital Dhaka", hospital.getHospitalName());
        assertTrue(hospital.isActive());
        assertTrue(hospital.isVerified());
        assertEquals(3, hospital.getSupportedDepartments().size());

        // Firestore Map serialization
        Map<String, Object> map = hospital.toMap();
        assertEquals("hosp_square_dhk", map.get("hospitalId"));
        assertEquals("SQUARE-01", map.get("hospitalCode"));
        assertEquals(Hospital.STATUS_ACTIVE, map.get("status"));
        assertEquals(400, map.get("totalBeds"));
        assertTrue((Boolean) map.get("verified"));
    }

    // =========================================================================
    // 2. MULTI-TENANT MEMBERSHIP & ROLE SEPARATION
    // =========================================================================

    @Test
    public void testMultiHospitalMembershipRoleSeparation() {
        String globalUserId = "usr_rahman_99";

        // Global User Account
        UserAccount globalAccount = new UserAccount(
                globalUserId, "AROHI-USR-99", "dr.rahman@gmail.com",
                "Dr. Habibur Rahman", "+880 1711-000000",
                UserRole.DOCTOR, VerificationStatus.VERIFIED, true, true
        );
        assertEquals("hosp_arohi_central", globalAccount.getHospitalId());

        // Hospital Membership 1: DOCTOR at Central Hospital
        HospitalMembership membership1 = new HospitalMembership(
                "mem_01", globalUserId, "hosp_arohi_central", "AROHI-HQ",
                "DOCTOR", HospitalMembership.STATUS_ACTIVE, "Cardiology"
        );

        // Hospital Membership 2: PATIENT at Square Hospital (Independent Role in another institution)
        HospitalMembership membership2 = new HospitalMembership(
                "mem_02", globalUserId, "hosp_square_dhk", "SQUARE-01",
                "PATIENT", HospitalMembership.STATUS_ACTIVE, "General"
        );

        // Hospital Membership 3: ADMIN at Apollo Chittagong
        HospitalMembership membership3 = new HospitalMembership(
                "mem_03", globalUserId, "hosp_apollo_ctg", "APOLLO-CTG",
                "ADMIN", HospitalMembership.STATUS_ACTIVE, "Executive Directorate"
        );

        List<HospitalMembership> userMemberships = Arrays.asList(membership1, membership2, membership3);

        assertEquals(3, userMemberships.size());
        assertEquals("DOCTOR", userMemberships.get(0).getRole());
        assertEquals("AROHI-HQ", userMemberships.get(0).getHospitalCode());

        assertEquals("PATIENT", userMemberships.get(1).getRole());
        assertEquals("SQUARE-01", userMemberships.get(1).getHospitalCode());

        assertEquals("ADMIN", userMemberships.get(2).getRole());
        assertEquals("APOLLO-CTG", userMemberships.get(2).getHospitalCode());

        // Map serialization for membership
        Map<String, Object> memMap = membership2.toMap();
        assertEquals("mem_02", memMap.get("membershipId"));
        assertEquals("PATIENT", memMap.get("role"));
        assertEquals("SQUARE-01", memMap.get("hospitalCode"));
    }

    // =========================================================================
    // 3. HOSPITAL CODE UNIQUENESS & VALIDATION
    // =========================================================================

    @Test
    public void testHospitalCodeUniquenessCheck() {
        Map<String, Hospital> codeRegistry = new HashMap<>();

        Hospital central = new Hospital("hosp_01", "AROHI-HQ", "Central Hospital", "", "", "", "", "", Hospital.STATUS_ACTIVE, 500, null, true, "");
        codeRegistry.put(central.getHospitalCode(), central);

        Hospital square = new Hospital("hosp_02", "SQUARE-01", "Square Hospital", "", "", "", "", "", Hospital.STATUS_ACTIVE, 400, null, true, "");
        codeRegistry.put(square.getHospitalCode(), square);

        // Test collision detection
        String candidateCode1 = "AROHI-HQ";
        assertTrue("Duplicate code must be detected", codeRegistry.containsKey(candidateCode1));

        String candidateCode2 = "UNITED-01";
        assertFalse("Unique code must be accepted", codeRegistry.containsKey(candidateCode2));
    }

    // =========================================================================
    // 4. SUPER ADMIN GOVERNANCE & STATUS TRANSITIONS
    // =========================================================================

    @Test
    public void testSuperAdminHospitalVerificationWorkflow() {
        SuperAdminAuthorization superAdmin = new SuperAdminAuthorization(
                "sa_root_01", "superadmin@arohi.health", "Chief Platform Governance Officer", 1, true, "Platform Root Authority"
        );
        assertTrue(superAdmin.isActive());
        assertEquals(1, superAdmin.getLevel());

        // Newly registered hospital starts PENDING_VERIFICATION
        Hospital newApplicant = new Hospital(
                "hosp_applicant_01", "NEW-CLINIC", "Green Valley Hospital",
                "Sylhet", "Sylhet", "+880 1822-000000", "admin@gv.org",
                "+880 1822-000000", Hospital.STATUS_PENDING, 120,
                Arrays.asList("General Medicine"), false, "Applicant clinic"
        );
        assertEquals(Hospital.STATUS_PENDING, newApplicant.getStatus());
        assertFalse(newApplicant.isVerified());

        // Super Admin approves and activates
        newApplicant.setVerified(true);
        newApplicant.setStatus(Hospital.STATUS_ACTIVE);
        newApplicant.setUpdatedAt(System.currentTimeMillis());

        assertTrue(newApplicant.isVerified());
        assertTrue(newApplicant.isActive());

        // Super Admin deactivates
        newApplicant.setStatus(Hospital.STATUS_INACTIVE);
        assertFalse(newApplicant.isActive());
        assertEquals(Hospital.STATUS_INACTIVE, newApplicant.getStatus());
    }

    // =========================================================================
    // 5. MULTI-TENANT DATA ISOLATION BY HOSPITAL ID
    // =========================================================================

    @Test
    public void testTenantDataIsolation() {
        List<UserAccount> allAccounts = new ArrayList<>();

        UserAccount accCentral = new UserAccount("u1", "ID-1", "doc1@arohi.health", "Doc 1", "017", UserRole.DOCTOR, VerificationStatus.VERIFIED, true, true);
        accCentral.setHospitalId("hosp_arohi_central");
        accCentral.setHospitalCode("AROHI-HQ");
        allAccounts.add(accCentral);

        UserAccount accSquare = new UserAccount("u2", "ID-2", "doc2@square.com", "Doc 2", "018", UserRole.DOCTOR, VerificationStatus.VERIFIED, true, true);
        accSquare.setHospitalId("hosp_square_dhk");
        accSquare.setHospitalCode("SQUARE-01");
        allAccounts.add(accSquare);

        UserAccount accApollo = new UserAccount("u3", "ID-3", "pat3@gmail.com", "Patient 3", "019", UserRole.PATIENT, VerificationStatus.VERIFIED, true, true);
        accApollo.setHospitalId("hosp_apollo_ctg");
        accApollo.setHospitalCode("APOLLO-CTG");
        allAccounts.add(accApollo);

        // Filter for hosp_square_dhk only
        List<UserAccount> squareTenantUsers = new ArrayList<>();
        for (UserAccount a : allAccounts) {
            if ("hosp_square_dhk".equals(a.getHospitalId())) {
                squareTenantUsers.add(a);
            }
        }

        assertEquals(1, squareTenantUsers.size());
        assertEquals("doc2@square.com", squareTenantUsers.get(0).getEmail());
        assertEquals("SQUARE-01", squareTenantUsers.get(0).getHospitalCode());
    }

    // =========================================================================
    // 6. BACKWARD COMPATIBILITY & SAFE LEGACY DATA MIGRATION
    // =========================================================================

    @Test
    public void testLegacyDataMigrationSafety() {
        // Simulate legacy Phase 1-8 user account created before Phase 9 (null hospitalId)
        UserAccount legacyAccount = new UserAccount(
                "legacy_pat_01", "AROHI-PAT-LEGACY", "legacy.pat@gmail.com",
                "Karim Legacy", "+880 1711-222333",
                UserRole.PATIENT, VerificationStatus.VERIFIED, true, true
        );
        legacyAccount.setHospitalId(null);
        legacyAccount.setHospitalCode(null);

        // Verify getter fallback provides default central hospital
        assertEquals("hosp_arohi_central", legacyAccount.getHospitalId());
        assertEquals("AROHI-HQ", legacyAccount.getHospitalCode());

        // Perform migration
        if (legacyAccount.getHospitalId() == null || "hosp_arohi_central".equals(legacyAccount.getHospitalId())) {
            legacyAccount.setHospitalId("hosp_arohi_central");
            legacyAccount.setHospitalCode("AROHI-HQ");
        }

        assertEquals("hosp_arohi_central", legacyAccount.getHospitalId());
        assertEquals("AROHI-HQ", legacyAccount.getHospitalCode());
        assertEquals("Karim Legacy", legacyAccount.getFullName()); // Zero data loss
    }
}
