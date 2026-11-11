package com.arohi.healthcare.services;

import androidx.annotation.NonNull;

import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.models.TrackingCategory;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.utils.SessionManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Firebase Cloud Messaging (FCM) receiver for Phase 14 real-time tracking pushes
 * (Pharmacy Dispatch, Appointment Queue/Delays, OT Progress, Verified Payments, Ambulance ETA).
 */
public class ArohiFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        SessionManager sessionManager = new SessionManager(this);
        UserAccount current = sessionManager.getCurrentSessionAccount();
        if (current == null || current.getUid() == null) return;

        FirebaseFirestore db = FirebaseProvider.getFirestore(this);
        if (db != null) {
            Map<String, Object> tokenUpdate = new HashMap<>();
            tokenUpdate.put("fcmToken", token);
            tokenUpdate.put("fcmTokenUpdatedAt", System.currentTimeMillis());
            db.collection(FirestoreCollections.USERS)
                    .document(current.getUid())
                    .set(tokenUpdate, SetOptions.merge());
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();
        String title = remoteMessage.getNotification() != null && remoteMessage.getNotification().getTitle() != null
                ? remoteMessage.getNotification().getTitle()
                : data.getOrDefault("title", "AROHI Tracking Update");
        String body = remoteMessage.getNotification() != null && remoteMessage.getNotification().getBody() != null
                ? remoteMessage.getNotification().getBody()
                : data.getOrDefault("body", "You have a new healthcare care-tracking update.");

        TrackingCategory category = TrackingCategory.fromKey(data.get("trackingCategory"));
        String referenceId = data.get("referenceId");

        TrackingNotificationDispatcher.showLocalBanner(this, title, body, category, referenceId);
    }
}
