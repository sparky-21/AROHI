package com.arohi.healthcare;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.arohi.healthcare.models.EmergencyHospitalAlert;
import com.arohi.healthcare.utils.EmergencyAlertSoundHelper;

import org.junit.After;
import org.junit.Test;

import java.util.Map;

/**
 * Unit tests verifying Emergency Alert System, sound alert state management,
 * and emergency hospital alert payload serialization.
 */
public class EmergencyAlertSystemTest {

    @After
    public void tearDown() {
        EmergencyAlertSoundHelper.stopEmergencyBeepAlert();
    }

    @Test
    public void testEmergencyHospitalAlertSerialization() {
        EmergencyHospitalAlert alert = new EmergencyHospitalAlert(
                "alert_code_blue_01",
                "hosp_arohi_central",
                "doc_uid_123",
                "Dr. Anisur Rahman",
                "DOCTOR",
                "Code Blue",
                "Cardiac arrest resuscitation needed",
                "Ward 4B, Bed 12"
        );

        Map<String, Object> map = alert.toMap();
        assertNotNull(map);
        assertEquals("alert_code_blue_01", map.get("alertId"));
        assertEquals("hosp_arohi_central", map.get("hospitalId"));
        assertEquals("doc_uid_123", map.get("senderUid"));
        assertEquals("Dr. Anisur Rahman", map.get("senderName"));
        assertEquals("DOCTOR", map.get("senderRole"));
        assertEquals("Code Blue", map.get("emergencyType"));
        assertEquals("Ward 4B, Bed 12", map.get("wardOrRoom"));
        assertFalse((Boolean) map.get("acknowledged"));

        EmergencyHospitalAlert restored = EmergencyHospitalAlert.fromMap(map);
        assertNotNull(restored);
        assertEquals(alert.getAlertId(), restored.getAlertId());
        assertEquals(alert.getHospitalId(), restored.getHospitalId());
        assertEquals(alert.getSenderUid(), restored.getSenderUid());
        assertEquals(alert.getSenderName(), restored.getSenderName());
        assertEquals(alert.getSenderRole(), restored.getSenderRole());
        assertEquals(alert.getEmergencyType(), restored.getEmergencyType());
        assertEquals(alert.getWardOrRoom(), restored.getWardOrRoom());
        assertFalse(restored.isAcknowledged());
    }

    @Test
    public void testEmergencyAlertAcknowledgement() {
        EmergencyHospitalAlert alert = new EmergencyHospitalAlert(
                "alert_trauma_02",
                "hosp_arohi_central",
                "nurse_uid_456",
                "Fatema Begum",
                "NURSE",
                "Critical Trauma",
                "Immediate surgical team needed in OT 3",
                "OT Room 3"
        );

        assertFalse(alert.isAcknowledged());

        // Authority acknowledges
        alert.setAcknowledged(true);
        alert.setAcknowledgedByUid("admin_uid_hq");
        alert.setAcknowledgedByName("Director of Hospital Services");
        alert.setAcknowledgedAt(System.currentTimeMillis());

        Map<String, Object> map = alert.toMap();
        assertTrue((Boolean) map.get("acknowledged"));
        assertEquals("admin_uid_hq", map.get("acknowledgedByUid"));
        assertEquals("Director of Hospital Services", map.get("acknowledgedByName"));
        assertTrue(((Number) map.get("acknowledgedAt")).longValue() > 0);
    }

    @Test
    public void testSoundHelperStateSafety() {
        assertFalse(EmergencyAlertSoundHelper.isBeeping());

        EmergencyAlertSoundHelper.stopEmergencyBeepAlert();
        assertFalse(EmergencyAlertSoundHelper.isBeeping());

        EmergencyAlertSoundHelper.playShortConfirmationBeep();
        // Short beep should not lock continuous beeping
        assertFalse(EmergencyAlertSoundHelper.isBeeping());
    }
}
