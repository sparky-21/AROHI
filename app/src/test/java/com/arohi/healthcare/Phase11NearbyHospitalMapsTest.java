package com.arohi.healthcare;

import com.arohi.healthcare.models.Hospital;

import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * Unit Tests for AROHI Phase 11:
 * Google Maps SDK, Geographic Coordinates & Nearby Hospital Discovery:
 * 1. Geographic Coordinates Model & Serialization
 * 2. Precise Haversine Distance Calculation & Zero-Coordinate Guard
 * 3. Hospital Sorting by User Proximity
 * 4. Multi-Attribute Area / City / Specialty Filtering
 * 5. Verification Status Distinction (AROHI Verified vs External Facility)
 * 6. Google Maps Directions Navigation URI Construction
 */
public class Phase11NearbyHospitalMapsTest {

    // =========================================================================
    // 1. GEOGRAPHIC COORDINATES & SERIALIZATION
    // =========================================================================

    @Test
    public void testHospitalCoordinatesInitializationAndSerialization() throws Exception {
        Hospital hospital = new Hospital(
                "hosp_square_dhk",
                "SQUARE-01",
                "Square Hospital Dhaka",
                "18/F, West Panthapath, Dhaka-1205",
                "Dhaka",
                "+880 2 8159457",
                "info@squarehospital.com",
                "+880 1713-377775",
                Hospital.STATUS_ACTIVE,
                400,
                Arrays.asList("Cardiology", "Critical Care", "General Surgery"),
                true,
                "Leading JCI-accredited tertiary care hospital in Dhaka.",
                23.7531,
                90.3816
        );

        assertEquals(23.7531, hospital.getLatitude(), 0.0001);
        assertEquals(90.3816, hospital.getLongitude(), 0.0001);

        // Map Serialization
        java.util.Map<String, Object> map = hospital.toMap();
        assertEquals(23.7531, (Double) map.get("latitude"), 0.0001);
        assertEquals(90.3816, (Double) map.get("longitude"), 0.0001);
    }

    // =========================================================================
    // 2. HAVERSINE DISTANCE CALCULATION
    // =========================================================================

    @Test
    public void testDistanceCalculationAccuracy() {
        // Square Hospital Dhaka: 23.7531, 90.3816
        Hospital squareHospital = new Hospital(
                "hosp_square", "SQUARE-01", "Square Hospital",
                "Panthapath", "Dhaka", "01700", "sq@test.com", "999",
                Hospital.STATUS_ACTIVE, 400, null, true, "Desc",
                23.7531, 90.3816
        );

        // Reference point: Karwan Bazar Intersection (approx 23.7508, 90.3934) ~1.2 km away
        double userLat = 23.7508;
        double userLng = 90.3934;
        double distanceKm = squareHospital.calculateDistanceKm(userLat, userLng);

        assertTrue("Distance should be realistic (~1.2 km)", distanceKm > 0.5 && distanceKm < 3.0);

        // Distance between identical points should be 0.0
        double selfDistance = squareHospital.calculateDistanceKm(23.7531, 90.3816);
        assertEquals(0.0, selfDistance, 0.01);
    }

    @Test
    public void testUnconfiguredCoordinatesReturnNegativeDistance() {
        Hospital unconfiguredHospital = new Hospital(
                "hosp_pending", "PENDING-01", "Pending Clinic",
                "Remote Location", "Sylhet", "01700", "pen@test.com", "999",
                Hospital.STATUS_PENDING, 50, null, false, "Desc",
                0.0, 0.0
        );

        double dist = unconfiguredHospital.calculateDistanceKm(23.7937, 90.4043);
        assertEquals(-1.0, dist, 0.001);
    }

    // =========================================================================
    // 3. PROXIMITY SORTING
    // =========================================================================

    @Test
    public void testNearbyHospitalsProximitySorting() {
        // User Location: Banani / Gulshan-2 (23.7937, 90.4043)
        double userLat = 23.7937;
        double userLng = 90.4043;

        Hospital h1 = new Hospital("h1", "CENTRAL", "AROHI Central", "Dhaka", "Dhaka", "1", "a", "1",
                Hospital.STATUS_ACTIVE, 500, null, true, "Desc", 23.7937, 90.4043); // 0.0 km

        Hospital h2 = new Hospital("h2", "UNITED", "United Hospital", "Gulshan", "Dhaka", "2", "b", "2",
                Hospital.STATUS_ACTIVE, 450, null, true, "Desc", 23.7978, 90.4184); // ~1.5 km

        Hospital h3 = new Hospital("h3", "SQUARE", "Square Hospital", "Panthapath", "Dhaka", "3", "c", "3",
                Hospital.STATUS_ACTIVE, 400, null, true, "Desc", 23.7531, 90.3816); // ~5.0 km

        Hospital h4 = new Hospital("h4", "APOLLO", "Apollo Chittagong", "Pahartali", "Chittagong", "4", "d", "4",
                Hospital.STATUS_ACTIVE, 375, null, true, "Desc", 22.3601, 91.8016); // ~200 km

        List<Hospital> hospitals = new ArrayList<>(Arrays.asList(h4, h3, h1, h2));

        Collections.sort(hospitals, (a, b) -> {
            double d1 = a.calculateDistanceKm(userLat, userLng);
            double d2 = b.calculateDistanceKm(userLat, userLng);
            return Double.compare(d1, d2);
        });

        assertEquals("h1", hospitals.get(0).getHospitalId()); // Nearest (0 km)
        assertEquals("h2", hospitals.get(1).getHospitalId()); // Second (~1.5 km)
        assertEquals("h3", hospitals.get(2).getHospitalId()); // Third (~5 km)
        assertEquals("h4", hospitals.get(3).getHospitalId()); // Furthest (~200 km)
    }

    // =========================================================================
    // 4. MULTI-ATTRIBUTE AREA / CITY / SPECIALTY FILTERING
    // =========================================================================

    @Test
    public void testManualAreaAndSpecialtyFiltering() {
        Hospital dhakaCardio = new Hospital("h1", "DHK-1", "Central Dhaka Heart Institute",
                "Health Sciences Ave", "Dhaka", "1", "a", "1",
                Hospital.STATUS_ACTIVE, 300, Arrays.asList("Cardiology", "Emergency"), true, "Desc", 23.79, 90.40);

        Hospital ctgGeneral = new Hospital("h2", "CTG-1", "Chittagong Coastal Clinic",
                "Zakir Hossain Rd", "Chittagong", "2", "b", "2",
                Hospital.STATUS_ACTIVE, 150, Arrays.asList("General Medicine", "Pediatrics"), true, "Desc", 22.36, 91.80);

        List<Hospital> list = Arrays.asList(dhakaCardio, ctgGeneral);

        // Filter by City: "Chittagong"
        List<Hospital> ctgResults = filterList(list, "Chittagong");
        assertEquals(1, ctgResults.size());
        assertEquals("CTG-1", ctgResults.get(0).getHospitalCode());

        // Filter by Specialty: "Cardiology"
        List<Hospital> cardioResults = filterList(list, "Cardiology");
        assertEquals(1, cardioResults.size());
        assertEquals("DHK-1", cardioResults.get(0).getHospitalCode());

        // Filter by Area: "Health Sciences"
        List<Hospital> areaResults = filterList(list, "Health Sciences");
        assertEquals(1, areaResults.size());
        assertEquals("DHK-1", areaResults.get(0).getHospitalCode());
    }

    private List<Hospital> filterList(List<Hospital> list, String query) {
        String q = query.toLowerCase(Locale.getDefault());
        List<Hospital> results = new ArrayList<>();
        for (Hospital h : list) {
            boolean match = (h.getHospitalName() != null && h.getHospitalName().toLowerCase().contains(q))
                    || (h.getCity() != null && h.getCity().toLowerCase().contains(q))
                    || (h.getAddress() != null && h.getAddress().toLowerCase().contains(q));
            if (!match && h.getSupportedDepartments() != null) {
                for (String d : h.getSupportedDepartments()) {
                    if (d.toLowerCase().contains(q)) {
                        match = true;
                        break;
                    }
                }
            }
            if (match) results.add(h);
        }
        return results;
    }

    // =========================================================================
    // 5. VERIFICATION STATUS DISTINCTION
    // =========================================================================

    @Test
    public void testVerificationStatusDistinction() {
        Hospital verifiedArohi = new Hospital("h1", "AROHI-01", "Verified Hospital",
                "Panthapath", "Dhaka", "1", "a", "1",
                Hospital.STATUS_ACTIVE, 200, null, true, "Desc", 23.75, 90.38);

        Hospital externalFacility = new Hospital("h2", "EXT-01", "External Nursing Home",
                "Mirpur", "Dhaka", "2", "b", "2",
                Hospital.STATUS_ACTIVE, 50, null, false, "Desc", 23.80, 90.36);

        assertTrue("AROHI verified hospital must have verified=true", verifiedArohi.isVerified());
        assertFalse("External unverified facility must have verified=false", externalFacility.isVerified());
    }

    // =========================================================================
    // 6. GOOGLE MAPS DIRECTIONS URI
    // =========================================================================

    @Test
    public void testDirectionsUriGeneration() {
        double lat = 23.7531;
        double lng = 90.3816;
        String navUri = String.format(Locale.ENGLISH, "google.navigation:q=%f,%f&mode=d", lat, lng);
        assertEquals("google.navigation:q=23.753100,90.381600&mode=d", navUri);

        String webUri = String.format(Locale.ENGLISH, "https://www.google.com/maps/dir/?api=1&destination=%f,%f", lat, lng);
        assertEquals("https://www.google.com/maps/dir/?api=1&destination=23.753100,90.381600", webUri);
    }
}
