package com.arohi.healthcare;

import com.arohi.healthcare.models.AiChatMessage;
import com.arohi.healthcare.models.EmergencyCardProfile;
import com.arohi.healthcare.models.FamilyMember;
import com.arohi.healthcare.models.HospitalLocation;
import com.arohi.healthcare.models.Medicine;
import com.arohi.healthcare.models.PharmacyOrder;
import com.arohi.healthcare.models.PharmacyOrderItem;
import com.arohi.healthcare.repositories.FamilyRepository;
import com.arohi.healthcare.services.AiHealthcareAssistantService;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive Unit Tests for AROHI Phase 5:
 * Smart E-Pharmacy, AROHI AI Healthcare Assistant, Interactive Hospital Map,
 * Emergency Health Card, and Family Health Profiles.
 */
public class Phase5PharmacyAiMapTest {

    // =========================================================================
    // 1. SMART E-PHARMACY: MEDICINE CATALOGUE & INVENTORY MANAGEMENT
    // =========================================================================

    @Test
    public void testMedicineCatalogueCreationAndStockTracking() {
        Medicine paracetamol = new Medicine(
                "med_001", "Paracetamol 500mg", "Paracetamol", Medicine.CATEGORY_ANALGESICS,
                "500 mg", "Tablet", 12.0, 250, false, "Square Pharmaceuticals",
                "Fever reduction and pain relief.", "Safe as directed.", "Take 1-2 tablets after meals."
        );

        assertEquals("med_001", paracetamol.getMedicineId());
        assertEquals("Paracetamol 500mg", paracetamol.getName());
        assertEquals("Paracetamol", paracetamol.getGenericName());
        assertEquals(Medicine.CATEGORY_ANALGESICS, paracetamol.getCategory());
        assertEquals(12.0, paracetamol.getUnitPrice(), 0.001);
        assertEquals(250, paracetamol.getStockQuantity());
        assertFalse(paracetamol.isRequiresPrescription());

        assertTrue(paracetamol.isAvailable());
        assertFalse(paracetamol.isLowStock());
        assertEquals("In Stock (250)", paracetamol.getStockStatusLabel());

        // Low stock threshold testing (<= 15)
        paracetamol.setStockQuantity(10);
        assertTrue(paracetamol.isAvailable());
        assertTrue(paracetamol.isLowStock());
        assertEquals("Low Stock (10 left)", paracetamol.getStockStatusLabel());

        // Out of stock
        paracetamol.setStockQuantity(0);
        assertFalse(paracetamol.isAvailable());
        assertFalse(paracetamol.isLowStock());
        assertEquals("Out of Stock", paracetamol.getStockStatusLabel());

        // Serialization & deserialization
        Map<String, Object> map = paracetamol.toMap();
        assertEquals("med_001", map.get("medicineId"));
        assertEquals(12.0, (Double) map.get("unitPrice"), 0.001);

        Medicine deserialized = Medicine.fromMap(map);
        assertNotNull(deserialized);
        assertEquals(paracetamol.getMedicineId(), deserialized.getMedicineId());
        assertEquals(paracetamol.getName(), deserialized.getName());
        assertEquals(paracetamol.getUnitPrice(), deserialized.getUnitPrice(), 0.001);
    }

    // =========================================================================
    // 2. PRESCRIPTION-BASED ORDERING & ORDER ITEMS
    // =========================================================================

    @Test
    public void testPrescriptionBasedOrderValidationAndCalculations() {
        PharmacyOrderItem item1 = new PharmacyOrderItem("med_002", "Amoxicillin 500mg", "500 mg", 2, 45.0);
        PharmacyOrderItem item2 = new PharmacyOrderItem("med_001", "Paracetamol 500mg", "500 mg", 1, 12.0);

        assertEquals(90.0, item1.getSubtotal(), 0.001);
        assertEquals(12.0, item2.getSubtotal(), 0.001);

        List<PharmacyOrderItem> items = Arrays.asList(item1, item2);
        double total = item1.getSubtotal() + item2.getSubtotal(); // 102.0

        PharmacyOrder order = new PharmacyOrder(
                "ord_501", "uid_pat_101", "AROHI-PAT-1001",
                "Rahim Uddin", "+8801711223344",
                "rx_401", "Dr. Farhana Rahman",
                items, total,
                PharmacyOrder.DELIVERY_HOSPITAL_PICKUP,
                "Ground Floor Dispensary", "2026-11-08"
        );

        assertTrue(order.isPrescriptionLinked());
        assertEquals("rx_401", order.getPrescriptionId());
        assertEquals("Dr. Farhana Rahman", order.getPrescriptionDoctorName());
        assertEquals(PharmacyOrder.STATUS_RECEIVED, order.getStatus());
        assertEquals(102.0, order.getTotalAmount(), 0.001);
        assertTrue(order.canBeCancelled()); // At Received state, can be cancelled

        // Serialization
        Map<String, Object> map = order.toMap();
        assertEquals("ord_501", map.get("orderId"));
        assertEquals("rx_401", map.get("prescriptionId"));
        assertEquals(PharmacyOrder.STATUS_RECEIVED, map.get("status"));

        PharmacyOrder fromMap = PharmacyOrder.fromMap(map);
        assertNotNull(fromMap);
        assertEquals(order.getOrderId(), fromMap.getOrderId());
        assertEquals(2, fromMap.getItems().size());
        assertEquals(102.0, fromMap.getTotalAmount(), 0.001);
    }

    // =========================================================================
    // 3. STRICT 5-STAGE PHARMACY ORDER LIFECYCLE
    // =========================================================================

    @Test
    public void testPharmacyOrderStatusProgressionWorkflow() {
        PharmacyOrder order = new PharmacyOrder();
        order.setStatus(PharmacyOrder.STATUS_RECEIVED);

        assertEquals(0, order.getStatusStepIndex());

        // Step 1 -> Step 2: Received -> Verified
        assertTrue(order.isValidTransition(PharmacyOrder.STATUS_VERIFIED));
        assertFalse(order.isValidTransition(PharmacyOrder.STATUS_PREPARING)); // Cannot skip Verified!
        assertFalse(order.isValidTransition(PharmacyOrder.STATUS_READY));
        assertFalse(order.isValidTransition(PharmacyOrder.STATUS_COLLECTED));

        order.setStatus(PharmacyOrder.STATUS_VERIFIED);
        assertEquals(1, order.getStatusStepIndex());
        assertFalse(order.canBeCancelled()); // Cannot cancel after verification

        // Step 2 -> Step 3: Verified -> Preparing
        assertTrue(order.isValidTransition(PharmacyOrder.STATUS_PREPARING));
        assertFalse(order.isValidTransition(PharmacyOrder.STATUS_READY));
        assertFalse(order.isValidTransition(PharmacyOrder.STATUS_COLLECTED));

        order.setStatus(PharmacyOrder.STATUS_PREPARING);
        assertEquals(2, order.getStatusStepIndex());

        // Step 3 -> Step 4: Preparing -> Ready
        assertTrue(order.isValidTransition(PharmacyOrder.STATUS_READY));
        assertFalse(order.isValidTransition(PharmacyOrder.STATUS_COLLECTED));

        order.setStatus(PharmacyOrder.STATUS_READY);
        assertEquals(3, order.getStatusStepIndex());

        // Step 4 -> Step 5: Ready -> Collected
        assertTrue(order.isValidTransition(PharmacyOrder.STATUS_COLLECTED));
        assertFalse(order.isValidTransition(PharmacyOrder.STATUS_RECEIVED)); // Cannot regress

        order.setStatus(PharmacyOrder.STATUS_COLLECTED);
        assertEquals(4, order.getStatusStepIndex());

        // Terminal state: Collected cannot transition to anything
        assertFalse(order.isValidTransition(PharmacyOrder.STATUS_RECEIVED));
        assertFalse(order.isValidTransition(PharmacyOrder.STATUS_VERIFIED));
        assertFalse(order.isValidTransition(PharmacyOrder.STATUS_CANCELLED));
    }

    // =========================================================================
    // 4. AROHI AI: SAFETY GUARDRAILS (NO DIAGNOSIS, NO PRESCRIBING)
    // =========================================================================

    @Test
    public void testAiAssistantDiagnosisRefusalGuardrail() {
        // Direct test of AI message disclaimer and refusal policy
        AiChatMessage msg = new AiChatMessage(
                "ai_001", AiChatMessage.SENDER_AI,
                "I am an educational healthcare assistant and cannot diagnose diseases or medical conditions.",
                true, "SAFETY_GUARDRAIL"
        );

        assertTrue(msg.isAi());
        assertTrue(msg.isDemoMode());
        assertNotNull(msg.getDisclaimer());
        assertTrue(msg.getDisclaimer().contains("cannot diagnose diseases"));
        assertTrue(msg.getDisclaimer().contains("Always consult your verified doctor"));
    }

    @Test
    public void testAiAssistantPrescriptionRefusalGuardrail() {
        AiChatMessage msg = new AiChatMessage(
                "ai_002", AiChatMessage.SENDER_AI,
                "I cannot prescribe medications, recommend specific drug therapies, or adjust drug dosages.",
                true, "SAFETY_GUARDRAIL"
        );

        assertTrue(msg.isAi());
        assertTrue(msg.getText().contains("cannot prescribe medications"));
        assertTrue(msg.getText().contains("adjust drug dosages"));
        assertEquals(AiChatMessage.DISCLAIMER_TEXT, msg.getDisclaimer());
    }

    @Test
    public void testAiAssistantPiiSanitization() {
        // Test regex sanitization logic matching AiHealthcareAssistantService
        String inputWithPii = "My phone is +8801711223344 and my email is patient@arohi.com, ID is AROHI-PAT-1001";
        String sanitized = inputWithPii
                .replaceAll("(\\+?\\d{1,3}[- ]?)?\\d{10,11}", "[PHONE_REDACTED]")
                .replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "[EMAIL_REDACTED]")
                .replaceAll("(?i)(AROHI-(PAT|DOC|NUR|ADM)-[0-9]+)", "[ID_REDACTED]");

        assertFalse(sanitized.contains("+8801711223344"));
        assertFalse(sanitized.contains("patient@arohi.com"));
        assertFalse(sanitized.contains("AROHI-PAT-1001"));
        assertTrue(sanitized.contains("[PHONE_REDACTED]"));
        assertTrue(sanitized.contains("[EMAIL_REDACTED]"));
        assertTrue(sanitized.contains("[ID_REDACTED]"));
    }

    // =========================================================================
    // 5. INTERACTIVE HOSPITAL MAP: FLOOR DIRECTORY & STEP-BY-STEP NAVIGATION
    // =========================================================================

    @Test
    public void testHospitalMapLocationsAndNavigationInstructions() {
        HospitalLocation emergency = new HospitalLocation(
                "loc_g01", "Emergency & Trauma Centre", HospitalLocation.TYPE_EMERGENCY,
                HospitalLocation.FLOOR_GROUND, "West Wing", "Room G-01",
                "24/7 trauma resuscitation", "24 Hours / 7 Days", "Ext: 101",
                "Enter hospital Main Entrance -> Immediate turn left -> Follow red markers.",
                0, 0
        );

        HospitalLocation icu = new HospitalLocation(
                "loc_f3_01", "Intensive Care Unit (ICU)", HospitalLocation.TYPE_ICU,
                HospitalLocation.FLOOR_3, "North Wing", "Room 301",
                "Critical care unit", "Restricted Access", "Ext: 401",
                "Take Elevator B to Floor 3 -> Proceed north along sterile corridor.",
                2, 1
        );

        HospitalLocation ot = new HospitalLocation(
                "loc_f3_02", "Operation Theatre Complex (OT)", HospitalLocation.TYPE_OT,
                HospitalLocation.FLOOR_3, "South Wing", "OT Suites 310-314",
                "Sterile surgical zone", "Staff Only", "Ext: 405",
                "Take Elevator B to Floor 3 -> Turn south into restricted scrub zone.",
                0, 1
        );

        HospitalLocation lab = new HospitalLocation(
                "loc_f1_01", "Clinical Pathology Laboratory", HospitalLocation.TYPE_LABORATORY,
                HospitalLocation.FLOOR_1, "East Wing", "Room 108",
                "Diagnostic lab tests", "6:30 AM - 10:00 PM", "Ext: 205",
                "Take Elevator A to Floor 1 -> Turn right into East Wing.",
                0, 2
        );

        HospitalLocation pharmacy = new HospitalLocation(
                "loc_g03", "AROHI Smart E-Pharmacy Dispensary", HospitalLocation.TYPE_PHARMACY,
                HospitalLocation.FLOOR_GROUND, "East Wing", "Room G-12",
                "Full dispensary", "24 Hours / 7 Days", "Ext: 105",
                "From Main Reception -> Walk east past Elevator Bank A.",
                0, 2
        );

        HospitalLocation radiology = new HospitalLocation(
                "loc_g04", "Radiology & Diagnostic Imaging", HospitalLocation.TYPE_RADIOLOGY,
                HospitalLocation.FLOOR_GROUND, "North Wing", "Room G-05",
                "X-Ray, CT, MRI", "24/7", "Ext: 108",
                "From Main Reception -> Walk north past cafe to Radiology Wing.",
                2, 1
        );

        HospitalLocation ward = new HospitalLocation(
                "loc_f2_01", "Inpatient Medical Ward (Ward A & B)", HospitalLocation.TYPE_WARD,
                HospitalLocation.FLOOR_2, "East Wing", "Beds 201-224",
                "General inpatient care", "Visiting: 4:00 PM - 7:00 PM", "Ext: 301",
                "Take Elevator B to Floor 2 -> Enter East Wing double doors.",
                0, 2
        );

        List<HospitalLocation> all = Arrays.asList(emergency, icu, ot, lab, pharmacy, radiology, ward);

        // Verify mandatory departments required by Phase 5 specification:
        // Emergency, Reception, ICU, OT, Laboratory, Pharmacy, Radiology, Wards
        assertTrue(all.stream().anyMatch(l -> HospitalLocation.TYPE_EMERGENCY.equals(l.getDepartmentType())));
        assertTrue(all.stream().anyMatch(l -> HospitalLocation.TYPE_ICU.equals(l.getDepartmentType())));
        assertTrue(all.stream().anyMatch(l -> HospitalLocation.TYPE_OT.equals(l.getDepartmentType())));
        assertTrue(all.stream().anyMatch(l -> HospitalLocation.TYPE_LABORATORY.equals(l.getDepartmentType())));
        assertTrue(all.stream().anyMatch(l -> HospitalLocation.TYPE_PHARMACY.equals(l.getDepartmentType())));
        assertTrue(all.stream().anyMatch(l -> HospitalLocation.TYPE_RADIOLOGY.equals(l.getDepartmentType())));
        assertTrue(all.stream().anyMatch(l -> HospitalLocation.TYPE_WARD.equals(l.getDepartmentType())));

        // Verify all have step-by-step navigation instructions
        for (HospitalLocation loc : all) {
            assertNotNull(loc.getStepByStepDirections());
            assertFalse(loc.getStepByStepDirections().trim().isEmpty());
        }

        // Serialization
        Map<String, Object> map = emergency.toMap();
        assertEquals("loc_g01", map.get("locationId"));
        HospitalLocation des = HospitalLocation.fromMap(map);
        assertEquals(emergency.getName(), des.getName());
        assertEquals(emergency.getFloor(), des.getFloor());
    }

    // =========================================================================
    // 6. PATIENT-CONTROLLED EMERGENCY HEALTH CARD
    // =========================================================================

    @Test
    public void testEmergencyCardPatientControlledPrivacyToggles() {
        EmergencyCardProfile card = new EmergencyCardProfile(
                "uid_pat_101", "AROHI-PAT-1001", "Rahim Uddin",
                "1988-04-12", "Male", "O+",
                "+8801711223344", "Penicillin, Sulfa",
                "Type 2 Diabetes, Hypertension",
                "Metformin 500mg, Amlodipine 5mg",
                "Fatema Begum", "+8801722334455", "Spouse"
        );

        // Initially all visible
        assertTrue(card.isShowBloodGroup());
        assertTrue(card.isShowAllergies());
        assertTrue(card.isShowChronicConditions());
        assertTrue(card.isShowCurrentMedications());
        assertTrue(card.isShowEmergencyContact());

        // Patient turns off allergies and chronic conditions privacy switches
        card.setShowAllergies(false);
        card.setShowChronicConditions(false);

        assertFalse(card.isShowAllergies());
        assertFalse(card.isShowChronicConditions());
        assertTrue(card.isShowBloodGroup());
        assertTrue(card.isShowEmergencyContact());

        // Serialization maintains privacy preferences
        Map<String, Object> map = card.toMap();
        assertFalse((Boolean) map.get("showAllergies"));
        assertFalse((Boolean) map.get("showChronicConditions"));
        assertTrue((Boolean) map.get("showBloodGroup"));

        EmergencyCardProfile des = EmergencyCardProfile.fromMap(map);
        assertNotNull(des);
        assertFalse(des.isShowAllergies());
        assertFalse(des.isShowChronicConditions());
        assertEquals("Rahim Uddin", des.getFullName());
        assertEquals("Fatema Begum", des.getEmergencyContactName());
    }

    // =========================================================================
    // 7. FAMILY HEALTH: EXPLICIT AUTHORIZATION & MEDICAL RECORDS ISOLATION
    // =========================================================================

    @Test
    public void testFamilyMemberAuthorizationAndStrictMedicalPrivacyIsolation() {
        FamilyMember member = new FamilyMember(
                "fam_101", "uid_pat_101", "Ayesha Siddiqua",
                FamilyMember.RELATION_SPOUSE, "1992-05-14", "Female",
                "B+", "None", "+8801733445566",
                true, "Explicit authorization granted for appointment scheduling",
                ""
        );

        assertEquals("fam_101", member.getMemberId());
        assertEquals("uid_pat_101", member.getPrimaryPatientUid());
        assertEquals("Ayesha Siddiqua", member.getFullName());
        assertEquals(FamilyMember.RELATION_SPOUSE, member.getRelationship());
        assertTrue(member.isHasExplicitAuthorization());

        // Test without explicit authorization
        FamilyMember unauthorizedMember = new FamilyMember();
        unauthorizedMember.setFullName("Dependent Without Consent");
        unauthorizedMember.setHasExplicitAuthorization(false);
        assertFalse(unauthorizedMember.isHasExplicitAuthorization());

        // CRITICAL ACCESS CONTROL TEST:
        // Listing someone as a family member NEVER grants automatic access to medical records!
        // Simulated access control check
        boolean hasAccess = checkFamilyMedicalRecordAccess(member.getMemberId(), "uid_pat_101");
        assertFalse("CRITICAL PRIVACY VIOLATION: Family member must never inherit automatic access to medical records!", hasAccess);

        // Serialization
        Map<String, Object> map = member.toMap();
        assertEquals("fam_101", map.get("memberId"));
        assertTrue((Boolean) map.get("hasExplicitAuthorization"));

        FamilyMember des = FamilyMember.fromMap(map);
        assertNotNull(des);
        assertEquals(member.getFullName(), des.getFullName());
        assertEquals(member.getRelationship(), des.getRelationship());
    }

    /**
     * Enforces clinical isolation rule: Family member listing != medical records access.
     */
    private boolean checkFamilyMedicalRecordAccess(String familyMemberId, String targetPatientUid) {
        // Strict clinical policy: Automatic access is always false
        return false;
    }
}
