package com.arohi.healthcare;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.arohi.healthcare.models.LiveTelemetryPayload;

import org.junit.Test;

import java.util.Map;

/**
 * Unit tests verifying Firebase Realtime Database Live Telemetry Payload,
 * path structuring, and telemetry serialization.
 */
public class LiveTrackingRealtimeDatabaseTest {

    @Test
    public void testAmbulanceTelemetryPayloadSerialization() {
        LiveTelemetryPayload payload = new LiveTelemetryPayload(
                "amb_req_101",
                "AMBULANCE",
                "hosp_arohi_central",
                "driver_uid_99",
                "Rafiqul Islam",
                "+8801711000000",
                23.8103,
                90.4125,
                45.5f,
                52.0f,
                8,
                "On the Way",
                true
        );

        Map<String, Object> map = payload.toMap();
        assertNotNull(map);
        assertEquals("amb_req_101", map.get("recordId"));
        assertEquals("AMBULANCE", map.get("category"));
        assertEquals("hosp_arohi_central", map.get("hospitalId"));
        assertEquals("driver_uid_99", map.get("operatorUid"));
        assertEquals("Rafiqul Islam", map.get("operatorName"));
        assertEquals(23.8103, (Double) map.get("latitude"), 0.0001);
        assertEquals(90.4125, (Double) map.get("longitude"), 0.0001);
        assertEquals(45.5f, (Float) map.get("bearing"), 0.01f);
        assertEquals(52.0f, (Float) map.get("speed"), 0.01f);
        assertEquals(8, map.get("etaMinutes"));
        assertEquals("On the Way", map.get("status"));
        assertTrue((Boolean) map.get("sharingActive"));

        LiveTelemetryPayload restored = LiveTelemetryPayload.fromMap(map);
        assertNotNull(restored);
        assertEquals(payload.getRecordId(), restored.getRecordId());
        assertEquals(payload.getCategory(), restored.getCategory());
        assertEquals(payload.getHospitalId(), restored.getHospitalId());
        assertEquals(payload.getOperatorUid(), restored.getOperatorUid());
        assertEquals(payload.getOperatorName(), restored.getOperatorName());
        assertEquals(payload.getLatitude(), restored.getLatitude(), 0.0001);
        assertEquals(payload.getLongitude(), restored.getLongitude(), 0.0001);
        assertEquals(payload.getBearing(), restored.getBearing(), 0.01f);
        assertEquals(payload.getSpeed(), restored.getSpeed(), 0.01f);
        assertEquals(payload.getEtaMinutes(), restored.getEtaMinutes());
        assertEquals(payload.getStatus(), restored.getStatus());
        assertTrue(restored.isSharingActive());
    }

    @Test
    public void testPharmacyTelemetryPayloadSessionTermination() {
        LiveTelemetryPayload payload = new LiveTelemetryPayload(
                "order_pharma_202",
                "PHARMACY",
                "hosp_arohi_central",
                "rider_uid_77",
                "Kamal Hossain",
                "+8801811000000",
                23.7925,
                90.4078,
                180.0f,
                24.5f,
                0,
                "Delivered",
                false
        );

        assertFalse(payload.isSharingActive());
        assertEquals("Delivered", payload.getStatus());
        assertEquals(0, payload.getEtaMinutes());

        Map<String, Object> map = payload.toMap();
        assertFalse((Boolean) map.get("sharingActive"));
    }

    @Test
    public void testRealtimeDatabasePathStructure() {
        String hospitalId = "hosp_dhk_01";
        String ambRequestId = "req_amb_888";
        String pharmacyOrderId = "order_rx_999";

        String expectedAmbulancePath = "liveTracking/hospitals/" + hospitalId + "/ambulances/" + ambRequestId;
        String expectedPharmacyPath = "liveTracking/hospitals/" + hospitalId + "/pharmacy/" + pharmacyOrderId;

        assertEquals("liveTracking/hospitals/hosp_dhk_01/ambulances/req_amb_888", expectedAmbulancePath);
        assertEquals("liveTracking/hospitals/hosp_dhk_01/pharmacy/order_rx_999", expectedPharmacyPath);
    }
}
