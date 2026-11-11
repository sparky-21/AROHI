package com.arohi.healthcare.repositories;

import android.content.Context;
import androidx.annotation.NonNull;

import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.models.Hospital;
import com.arohi.healthcare.models.HospitalMembership;
import com.arohi.healthcare.models.SuperAdminAuthorization;
import com.arohi.healthcare.services.FirebaseProvider;
import com.arohi.healthcare.utils.SessionManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-tenant repository coordinating hospital discovery, hospital code lookups,
 * institutional registrations, multi-hospital membership, and platform Super Admin operations.
 */
public class HospitalNetworkRepository {

    private final Context context;
    private final SessionManager sessionManager;
    private final FirebaseFirestore firestore;

    public HospitalNetworkRepository(Context context) {
        this.context = context.getApplicationContext();
        this.sessionManager = new SessionManager(this.context);
        this.firestore = FirebaseProvider.getFirestore(this.context);
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public static void ensureHospitalCoordinates(Hospital h) {
        if (h == null) return;
        if (h.getLatitude() != 0.0 && h.getLongitude() != 0.0) return;

        String code = h.getHospitalCode() != null ? h.getHospitalCode().toUpperCase().trim() : "";
        String name = h.getHospitalName() != null ? h.getHospitalName().toLowerCase() : "";
        String city = h.getCity() != null ? h.getCity().toLowerCase() : "";

        if (code.contains("SQUARE") || name.contains("square")) {
            h.setLatitude(23.7531);
            h.setLongitude(90.3816);
        } else if (code.contains("UNITED") || name.contains("united")) {
            h.setLatitude(23.7978);
            h.setLongitude(90.4184);
        } else if (code.contains("EVERCARE") || name.contains("evercare") || name.contains("apollo dhaka")) {
            h.setLatitude(23.8103);
            h.setLongitude(90.4312);
        } else if (code.contains("APOLLO") || name.contains("apollo") || city.contains("chittagong") || city.contains("ctg")) {
            h.setLatitude(22.3601);
            h.setLongitude(91.8016);
        } else if (code.contains("DMCH") || name.contains("dhaka medical")) {
            h.setLatitude(23.7258);
            h.setLongitude(90.3976);
        } else if (city.contains("chittagong")) {
            h.setLatitude(22.3587);
            h.setLongitude(91.8214);
        } else if (city.contains("sylhet")) {
            h.setLatitude(24.8949);
            h.setLongitude(91.8687);
        } else if (city.contains("rajshahi")) {
            h.setLatitude(24.3745);
            h.setLongitude(88.6042);
        } else {
            // Default to Central Dhaka health corridor
            h.setLatitude(23.7937);
            h.setLongitude(90.4043);
        }
    }

    /**
     * Retrieves all hospitals registered in the AROHI Global Network.
     * Synchronizes from Firestore /hospitals if available, with robust local caching.
     */
    public void getHospitals(AuthCallback<List<Hospital>> callback) {
        if (firestore == null) {
            List<Hospital> cached = sessionManager.getRegisteredHospitals();
            for (Hospital h : cached) ensureHospitalCoordinates(h);
            callback.onSuccess(cached);
            return;
        }

        firestore.collection(FirestoreCollections.HOSPITALS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Hospital> list = new ArrayList<>();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Hospital h = doc.toObject(Hospital.class);
                            if (h != null) {
                                if (h.getHospitalId() == null || h.getHospitalId().isEmpty()) {
                                    h.setHospitalId(doc.getId());
                                }
                                ensureHospitalCoordinates(h);
                                list.add(h);
                                sessionManager.saveHospital(h);
                            }
                        }
                    }
                    if (list.isEmpty()) {
                        list = sessionManager.getRegisteredHospitals();
                    }
                    for (Hospital h : list) ensureHospitalCoordinates(h);
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    // Safe fallback to local registry
                    List<Hospital> cached = sessionManager.getRegisteredHospitals();
                    for (Hospital h : cached) ensureHospitalCoordinates(h);
                    callback.onSuccess(cached);
                });
    }

    /**
     * Searches hospitals by name, code, city, or supported department.
     */
    public void searchHospitals(String query, String category, AuthCallback<List<Hospital>> callback) {
        List<Hospital> results = sessionManager.searchHospitals(query, category);
        callback.onSuccess(results);
    }

    /**
     * Looks up a hospital by its unique code (case-insensitive).
     */
    public void findHospitalByCode(String hospitalCode, AuthCallback<Hospital> callback) {
        if (hospitalCode == null || hospitalCode.trim().isEmpty()) {
            callback.onError("Hospital code cannot be empty.");
            return;
        }
        String normalizedCode = hospitalCode.trim().toUpperCase();

        if (firestore != null) {
            firestore.collection(FirestoreCollections.HOSPITALS)
                    .whereEqualTo("hospitalCode", normalizedCode)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                            Hospital h = doc.toObject(Hospital.class);
                            if (h != null) {
                                if (h.getHospitalId() == null || h.getHospitalId().isEmpty()) {
                                    h.setHospitalId(doc.getId());
                                }
                                sessionManager.saveHospital(h);
                                callback.onSuccess(h);
                                return;
                            }
                        }
                        // Fallback to local
                        Hospital local = sessionManager.findHospitalByCode(normalizedCode);
                        if (local != null) {
                            callback.onSuccess(local);
                        } else {
                            callback.onError("No hospital found with code: " + normalizedCode);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Hospital local = sessionManager.findHospitalByCode(normalizedCode);
                        if (local != null) {
                            callback.onSuccess(local);
                        } else {
                            callback.onError("No hospital found with code: " + normalizedCode);
                        }
                    });
        } else {
            Hospital local = sessionManager.findHospitalByCode(normalizedCode);
            if (local != null) {
                callback.onSuccess(local);
            } else {
                callback.onError("No hospital found with code: " + normalizedCode);
            }
        }
    }

    /**
     * Registers a new institutional applicant into the AROHI Global Network.
     * New applications require Super Admin verification before activation.
     */
    public void registerHospital(Hospital hospital, AuthCallback<Hospital> callback) {
        if (hospital == null) {
            callback.onError("Hospital data cannot be null.");
            return;
        }
        if (hospital.getHospitalName() == null || hospital.getHospitalName().trim().isEmpty()) {
            callback.onError("Hospital name is required.");
            return;
        }
        if (hospital.getHospitalCode() == null || hospital.getHospitalCode().trim().isEmpty()) {
            callback.onError("Hospital unique code is required.");
            return;
        }

        String normalizedCode = hospital.getHospitalCode().trim().toUpperCase();
        hospital.setHospitalCode(normalizedCode);

        // Check code uniqueness
        Hospital existing = sessionManager.findHospitalByCode(normalizedCode);
        if (existing != null && !existing.getHospitalId().equals(hospital.getHospitalId())) {
            callback.onError("Hospital code '" + normalizedCode + "' is already in use by " + existing.getHospitalName());
            return;
        }

        if (hospital.getHospitalId() == null || hospital.getHospitalId().trim().isEmpty()) {
            hospital.setHospitalId("hosp_" + System.currentTimeMillis());
        }
        hospital.setStatus(Hospital.STATUS_PENDING);
        hospital.setVerified(false);

        sessionManager.saveHospital(hospital);

        if (firestore != null) {
            firestore.collection(FirestoreCollections.HOSPITALS)
                    .document(hospital.getHospitalId())
                    .set(hospital.toMap())
                    .addOnSuccessListener(aVoid -> callback.onSuccess(hospital))
                    .addOnFailureListener(e -> {
                        // Local write succeeded
                        callback.onSuccess(hospital);
                    });
        } else {
            callback.onSuccess(hospital);
        }
    }

    /**
     * Platform Super Admin: Approves or rejects a hospital application.
     */
    public void verifyHospital(String hospitalId, boolean approved, AuthCallback<Hospital> callback) {
        Hospital h = sessionManager.findHospitalById(hospitalId);
        if (h == null) {
            callback.onError("Hospital not found.");
            return;
        }
        h.setVerified(approved);
        h.setStatus(approved ? Hospital.STATUS_ACTIVE : Hospital.STATUS_INACTIVE);
        h.setUpdatedAt(System.currentTimeMillis());

        sessionManager.saveHospital(h);

        if (firestore != null) {
            firestore.collection(FirestoreCollections.HOSPITALS)
                    .document(hospitalId)
                    .update("verified", approved, "status", h.getStatus(), "updatedAt", h.getUpdatedAt())
                    .addOnSuccessListener(aVoid -> callback.onSuccess(h))
                    .addOnFailureListener(e -> callback.onSuccess(h));
        } else {
            callback.onSuccess(h);
        }
    }

    /**
     * Platform Super Admin: Activates or deactivates a hospital.
     */
    public void toggleHospitalActivation(String hospitalId, boolean active, AuthCallback<Hospital> callback) {
        Hospital h = sessionManager.findHospitalById(hospitalId);
        if (h == null) {
            callback.onError("Hospital not found.");
            return;
        }
        h.setStatus(active ? Hospital.STATUS_ACTIVE : Hospital.STATUS_INACTIVE);
        h.setUpdatedAt(System.currentTimeMillis());

        sessionManager.saveHospital(h);

        if (firestore != null) {
            firestore.collection(FirestoreCollections.HOSPITALS)
                    .document(hospitalId)
                    .update("status", h.getStatus(), "updatedAt", h.getUpdatedAt())
                    .addOnSuccessListener(aVoid -> callback.onSuccess(h))
                    .addOnFailureListener(e -> callback.onSuccess(h));
        } else {
            callback.onSuccess(h);
        }
    }

    /**
     * Platform Super Admin: Updates hospital unique code.
     */
    public void updateHospitalCode(String hospitalId, String newCode, AuthCallback<Hospital> callback) {
        if (newCode == null || newCode.trim().isEmpty()) {
            callback.onError("New hospital code cannot be empty.");
            return;
        }
        String normalized = newCode.trim().toUpperCase();
        Hospital existing = sessionManager.findHospitalByCode(normalized);
        if (existing != null && !existing.getHospitalId().equals(hospitalId)) {
            callback.onError("Hospital code '" + normalized + "' is already assigned to " + existing.getHospitalName());
            return;
        }

        Hospital h = sessionManager.findHospitalById(hospitalId);
        if (h == null) {
            callback.onError("Hospital not found.");
            return;
        }
        h.setHospitalCode(normalized);
        h.setUpdatedAt(System.currentTimeMillis());

        sessionManager.saveHospital(h);

        if (firestore != null) {
            firestore.collection(FirestoreCollections.HOSPITALS)
                    .document(hospitalId)
                    .update("hospitalCode", normalized, "updatedAt", h.getUpdatedAt())
                    .addOnSuccessListener(aVoid -> callback.onSuccess(h))
                    .addOnFailureListener(e -> callback.onSuccess(h));
        } else {
            callback.onSuccess(h);
        }
    }

    /**
     * Retrieves all hospital memberships held by a user.
     */
    public void fetchUserMemberships(String userId, AuthCallback<List<HospitalMembership>> callback) {
        List<HospitalMembership> memberships = sessionManager.getUserMemberships(userId);
        callback.onSuccess(memberships);
    }

    /**
     * Enrolls or updates a user's membership in a specific hospital.
     */
    public void saveMembership(HospitalMembership membership, AuthCallback<HospitalMembership> callback) {
        if (membership == null) {
            callback.onError("Membership cannot be null.");
            return;
        }
        sessionManager.saveHospitalMembership(membership);

        if (firestore != null) {
            firestore.collection(FirestoreCollections.HOSPITAL_MEMBERSHIPS)
                    .document(membership.getMembershipId())
                    .set(membership.toMap())
                    .addOnSuccessListener(aVoid -> callback.onSuccess(membership))
                    .addOnFailureListener(e -> callback.onSuccess(membership));
        } else {
            callback.onSuccess(membership);
        }
    }

    /**
     * Switches the active session's hospital context.
     */
    public void selectHospital(Hospital hospital) {
        if (hospital == null) return;
        sessionManager.setSelectedHospital(hospital.getHospitalId(), hospital.getHospitalCode(), hospital.getHospitalName());
    }

    /**
     * Checks if a user is an authorized Platform Super Admin.
     */
    public void checkSuperAdmin(String uid, AuthCallback<Boolean> callback) {
        if (sessionManager.isSuperAdmin(uid)) {
            callback.onSuccess(true);
            return;
        }

        if (firestore != null) {
            firestore.collection(FirestoreCollections.SUPER_ADMINS)
                    .document(uid)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        boolean isSa = snapshot != null && snapshot.exists() && Boolean.TRUE.equals(snapshot.getBoolean("active"));
                        callback.onSuccess(isSa);
                    })
                    .addOnFailureListener(e -> callback.onSuccess(false));
        } else {
            callback.onSuccess(false);
        }
    }
}
