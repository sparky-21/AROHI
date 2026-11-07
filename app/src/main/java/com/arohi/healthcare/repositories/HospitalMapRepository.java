package com.arohi.healthcare.repositories;

import android.content.Context;

import androidx.annotation.NonNull;

import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.models.HospitalLocation;
import com.arohi.healthcare.utils.SessionManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Repository for Interactive Hospital Map & Indoor Navigation Simulation.
 * Provides floor-by-floor location directories, department search,
 * and step-by-step route directions.
 */
public class HospitalMapRepository {

    private final SessionManager sessionManager;
    private final FirebaseFirestore firestore;

    public HospitalMapRepository(@NonNull Context context) {
        this.sessionManager = new SessionManager(context);
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void loadLocations(String floorFilter, String searchQuery,
                              @NonNull AuthCallback<List<HospitalLocation>> callback) {
        // Step 1: Cache-First Delivery
        List<HospitalLocation> localList = sessionManager.getAllHospitalLocationsLocal();
        List<HospitalLocation> filtered = filterLocations(localList, floorFilter, searchQuery);
        callback.onSuccess(filtered);

        // Step 2: Background Firestore Synchronization
        firestore.collection(FirestoreCollections.HOSPITAL_DEPARTMENTS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        // Remote updates if configured
                        List<HospitalLocation> remoteList = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            if (doc.getData() != null) {
                                HospitalLocation loc = HospitalLocation.fromMap(doc.getData());
                                if (loc != null) remoteList.add(loc);
                            }
                        }
                        if (!remoteList.isEmpty()) {
                            callback.onSuccess(filterLocations(remoteList, floorFilter, searchQuery));
                            return;
                        }
                    }
                    callback.onSuccess(filterLocations(sessionManager.getAllHospitalLocationsLocal(), floorFilter, searchQuery));
                })
                .addOnFailureListener(e -> {
                    // Fallback to local
                });
    }

    public List<HospitalLocation> filterLocations(List<HospitalLocation> list, String floor, String query) {
        List<HospitalLocation> result = new ArrayList<>();
        String normalizedQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        boolean filterFloor = floor != null && !floor.equalsIgnoreCase("ALL") && !floor.trim().isEmpty();

        for (HospitalLocation loc : list) {
            if (filterFloor && !floor.equalsIgnoreCase(loc.getFloor())) {
                continue;
            }
            if (!normalizedQuery.isEmpty()) {
                String name = loc.getName() != null ? loc.getName().toLowerCase(Locale.ROOT) : "";
                String type = loc.getDepartmentType() != null ? loc.getDepartmentType().toLowerCase(Locale.ROOT) : "";
                String desc = loc.getDescription() != null ? loc.getDescription().toLowerCase(Locale.ROOT) : "";
                String room = loc.getRoomNumber() != null ? loc.getRoomNumber().toLowerCase(Locale.ROOT) : "";
                if (!name.contains(normalizedQuery) && !type.contains(normalizedQuery)
                        && !desc.contains(normalizedQuery) && !room.contains(normalizedQuery)) {
                    continue;
                }
            }
            result.add(loc);
        }
        return result;
    }

    public HospitalLocation getLocationById(String locationId) {
        for (HospitalLocation loc : sessionManager.getAllHospitalLocationsLocal()) {
            if (loc.getLocationId() != null && loc.getLocationId().equals(locationId)) {
                return loc;
            }
        }
        return null;
    }
}
