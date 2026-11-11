package com.arohi.healthcare.services;

import android.content.Context;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import java.util.List;

/**
 * Provides safe, centralized access to Firebase services and detects whether
 * google-services.json is configured in the build.
 */
public final class FirebaseProvider {

    private FirebaseProvider() {
        // Prevent instantiation
    }

    /**
     * Returns true if Firebase has been configured via google-services.json.
     */
    public static boolean isFirebaseConfigured(Context context) {
        try {
            List<FirebaseApp> apps = FirebaseApp.getApps(context.getApplicationContext());
            if (apps != null && !apps.isEmpty()) {
                return true;
            }
            FirebaseApp initialized = FirebaseApp.initializeApp(context.getApplicationContext());
            return initialized != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static FirebaseAuth getAuth(Context context) {
        if (!isFirebaseConfigured(context)) {
            return null;
        }
        return FirebaseAuth.getInstance();
    }

    public static FirebaseFirestore getFirestore(Context context) {
        if (!isFirebaseConfigured(context)) {
            return null;
        }
        return FirebaseFirestore.getInstance();
    }

    public static FirebaseStorage getStorage(Context context) {
        if (!isFirebaseConfigured(context)) {
            return null;
        }
        return FirebaseStorage.getInstance();
    }

    private static boolean rtdbPersistenceConfigured = false;
    public static final String DEFAULT_RTDB_URL = "https://arohi-694a5-default-rtdb.firebaseio.com";

    public static com.google.firebase.database.FirebaseDatabase getDatabase(Context context) {
        if (!isFirebaseConfigured(context)) {
            return null;
        }
        try {
            com.google.firebase.database.FirebaseDatabase db;
            try {
                db = com.google.firebase.database.FirebaseDatabase.getInstance();
            } catch (Exception noUrlEx) {
                // Fallback to project default RTDB instance URL
                db = com.google.firebase.database.FirebaseDatabase.getInstance(DEFAULT_RTDB_URL);
            }
            if (!rtdbPersistenceConfigured && db != null) {
                try {
                    db.setPersistenceEnabled(true);
                } catch (Exception ignored) {
                    // Persistence must be set before any other database reference usage
                }
                rtdbPersistenceConfigured = true;
            }
            return db;
        } catch (Exception e) {
            return null;
        }
    }
}
