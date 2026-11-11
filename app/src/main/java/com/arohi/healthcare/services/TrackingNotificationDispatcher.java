package com.arohi.healthcare.services;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.arohi.healthcare.R;
import com.arohi.healthcare.activities.UnifiedTrackingActivity;
import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.models.AppNotification;
import com.arohi.healthcare.models.TrackingCategory;
import com.arohi.healthcare.utils.SessionManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.UUID;

/**
 * Centralized Phase 14 Privacy-Safe Tracking Notification Dispatcher.
 * Synchronizes status updates to:
 * 1. Local SessionManager notification center
 * 2. Firestore /notifications collection (triggers Cloud Functions -> FCM push when configured)
 * 3. Local Android Notification tray (when user is signed in & notification permission is granted)
 */
public final class TrackingNotificationDispatcher {

    public static final String CHANNEL_TRACKING = "arohi_tracking_updates";

    private TrackingNotificationDispatcher() {
    }

    public static void ensureNotificationChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null && nm.getNotificationChannel(CHANNEL_TRACKING) == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_TRACKING,
                        "AROHI Care & Delivery Tracking",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Status updates for pharmacy deliveries, appointments, operations, payments, and ambulance dispatch.");
                nm.createNotificationChannel(channel);
            }
        }
    }

    public static void dispatchTrackingUpdate(@NonNull Context context,
                                              @NonNull String recipientUid,
                                              String senderUid,
                                              String senderName,
                                              @NonNull TrackingCategory category,
                                              @NonNull String referenceId,
                                              @NonNull String title,
                                              @NonNull String privacySafeBody) {
        if (recipientUid.trim().isEmpty()) return;

        String notifId = "trk_notif_" + UUID.randomUUID().toString().substring(0, 10);
        String notifType = mapCategoryToNotificationType(category);

        AppNotification notification = new AppNotification(
                notifId,
                recipientUid,
                senderUid != null ? senderUid : "",
                senderName != null ? senderName : "AROHI Tracking",
                notifType,
                title,
                privacySafeBody,
                referenceId
        );

        SessionManager sessionManager = new SessionManager(context);
        sessionManager.saveNotificationLocal(notification);

        FirebaseFirestore db = FirebaseProvider.getFirestore(context);
        if (db != null) {
            db.collection(FirestoreCollections.NOTIFICATIONS)
                    .document(notifId)
                    .set(notification.toMap());
        }

        if (sessionManager.getCurrentSessionAccount() != null
                && recipientUid.equals(sessionManager.getCurrentSessionAccount().getUid())) {
            showLocalBanner(context, title, privacySafeBody, category, referenceId);
        }
    }

    public static void showLocalBanner(@NonNull Context context,
                                       @NonNull String title,
                                       @NonNull String body,
                                       TrackingCategory category,
                                       String referenceId) {
        ensureNotificationChannel(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        Intent intent = new Intent(context, UnifiedTrackingActivity.class);
        if (category != null) {
            intent.putExtra(UnifiedTrackingActivity.EXTRA_INITIAL_CATEGORY, category.getKey());
        }
        if (referenceId != null) {
            intent.putExtra(UnifiedTrackingActivity.EXTRA_HIGHLIGHT_ITEM_ID, referenceId);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) (System.currentTimeMillis() & 0x7fffffff),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_TRACKING)
                .setSmallIcon(R.drawable.ic_arohi_logo)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context)
                    .notify((int) (System.currentTimeMillis() & 0x7fffffff), builder.build());
        } catch (SecurityException ignored) {
            // Safely ignored if notification permission revoked mid-session
        }
    }

    @NonNull
    public static String mapCategoryToNotificationType(@NonNull TrackingCategory category) {
        switch (category) {
            case PHARMACY:
                return AppNotification.TYPE_PHARMACY_TRACKING;
            case APPOINTMENT:
                return AppNotification.TYPE_APPOINTMENT_QUEUE;
            case OPERATION:
                return AppNotification.TYPE_OPERATION_TRACKING;
            case PAYMENT:
                return AppNotification.TYPE_PAYMENT_TRACKING;
            case AMBULANCE:
                return AppNotification.TYPE_AMBULANCE_TRACKING;
            default:
                return AppNotification.TYPE_PHARMACY_TRACKING;
        }
    }
}
