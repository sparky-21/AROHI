package com.arohi.healthcare.services;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.models.AppNotification;
import com.arohi.healthcare.models.ChatMessage;
import com.arohi.healthcare.models.ConversationThread;
import com.arohi.healthcare.models.DoctorPatientConnection;
import com.arohi.healthcare.models.EmergencyHospitalAlert;
import com.arohi.healthcare.models.StaffConversation;
import com.arohi.healthcare.models.StaffMessage;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.HospitalCommunicationRepository;
import com.arohi.healthcare.utils.EmergencyAlertSoundHelper;
import com.arohi.healthcare.utils.SessionManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Universal Emergency Alert Dispatcher & Real-time Auditor.
 * Orchestrates:
 * 1. Patient SOS -> Automatic urgent message to doctor inbox + dialing emergency contact.
 * 2. Doctor/Nurse Emergency SOS -> Automatic broadcast to Hospital Authority group chat + Realtime DB.
 * 3. Authority Portal Alert -> Triggers loud beep alarms & popups upon critical alerts.
 */
public class EmergencyAlertManager {

    private static final String PREF_EMERGENCY = "arohi_emergency_prefs";
    private static final String RTDB_ROOT = "liveTracking";
    private static final String RTDB_HOSPITALS = "hospitals";
    private static final String RTDB_ALERTS = "emergencyAlerts";

    private final Context context;
    private final SessionManager sessionManager;
    private final SharedPreferences prefs;
    private final FirebaseDatabase realtimeDb;
    private final FirebaseFirestore firestore;
    private final HospitalCommunicationRepository communicationRepo;

    public interface AlertObserver {
        void onAlertReceived(@NonNull EmergencyHospitalAlert alert);
    }

    public EmergencyAlertManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.sessionManager = new SessionManager(this.context);
        this.prefs = this.context.getSharedPreferences(PREF_EMERGENCY, Context.MODE_PRIVATE);
        this.realtimeDb = FirebaseProvider.getDatabase(this.context);
        this.firestore = FirebaseProvider.getFirestore(this.context);
        this.communicationRepo = new HospitalCommunicationRepository(this.context);
    }

    /**
     * Patient Emergency SOS:
     * Dispatches an urgent critical medical message directly into the connected doctor's inbox.
     */
    public void sendPatientEmergencyAlertToDoctor(@NonNull UserAccount patient,
                                                  @Nullable String doctorUidHint,
                                                  @Nullable String note,
                                                  @NonNull AuthCallback<String> callback) {
        String targetDoctorUid = doctorUidHint;
        String targetDoctorName = "Assigned Doctor";
        DoctorPatientConnection matchedConnection = null;

        // Find the accepted connection to use its canonical connectionId
        List<DoctorPatientConnection> connections = sessionManager.getAllConnectionsLocal();
        for (DoctorPatientConnection c : connections) {
            if (patient.getUid().equals(c.getPatientUid()) && c.isAccepted() && c.getDoctorUid() != null) {
                if (targetDoctorUid == null || targetDoctorUid.isEmpty() || targetDoctorUid.equals(c.getDoctorUid())) {
                    targetDoctorUid = c.getDoctorUid();
                    targetDoctorName = c.getDoctorName() != null ? c.getDoctorName() : "Dr. Specialist";
                    matchedConnection = c;
                    break;
                }
            }
        }

        if (targetDoctorUid == null || targetDoctorUid.isEmpty()) {
            // If no connected doctor found in cache, fallback to emergency notice
            targetDoctorUid = "doc_emergency_desk";
            targetDoctorName = "Emergency Department On-Duty Specialist";
        }

        // Use the canonical connection ID so message lands in the same conversation the doctor sees
        String conversationId = (matchedConnection != null && matchedConnection.getConnectionId() != null)
                ? matchedConnection.getConnectionId()
                : "conv_" + patient.getUid() + "_" + targetDoctorUid;

        String alertText = "🚨 CRITICAL EMERGENCY ALERT from Patient " + patient.getFullName()
                + " (AROHI ID: " + patient.getArohiId() + ", Phone: " + patient.getPhone() + ")!\n"
                + "Immediate medical assistance requested. Note: "
                + (note != null && !note.trim().isEmpty() ? note.trim() : "Patient triggered emergency alert button.");

        ChatMessage msg = new ChatMessage(
                "sos_" + UUID.randomUUID().toString().substring(0, 8),
                conversationId,
                patient.getUid(),
                patient.getFullName(),
                "PATIENT",
                targetDoctorUid,
                alertText
        );
        msg.setTimestamp(System.currentTimeMillis());

        // Save locally for instant rendering
        sessionManager.appendMessageLocal(conversationId, msg);

        // Update ConversationThread metadata so the message appears in the doctor's inbox list
        String preview = alertText.length() > 60 ? alertText.substring(0, 60) + "…" : alertText;
        ConversationThread localThread = sessionManager.getConversationLocal(conversationId);
        if (localThread == null) {
            localThread = new ConversationThread();
            localThread.setConversationId(conversationId);
            localThread.setConnectionId(conversationId);
            localThread.setDoctorUid(targetDoctorUid);
            localThread.setDoctorName(targetDoctorName);
            localThread.setDoctorSpecialty(matchedConnection != null ? matchedConnection.getDoctorSpecialty() : "");
            localThread.setPatientUid(patient.getUid());
            localThread.setPatientName(patient.getFullName());
            localThread.setPatientId(patient.getArohiId());
            localThread.setConnectionStatus(DoctorPatientConnection.STATUS_ACCEPTED);
        }
        localThread.setLastMessagePreview(preview);
        localThread.setLastMessageSenderUid(patient.getUid());
        localThread.setLastMessageTimestamp(msg.getTimestamp());
        localThread.setUnreadByDoctor(true);
        localThread.setUnreadByPatient(false);
        sessionManager.saveConversationLocal(localThread);

        // Create notification for the doctor so it appears in their notifications list
        String notifId = "notif_sos_" + UUID.randomUUID().toString().substring(0, 10);
        AppNotification sosNotif = new AppNotification(
                notifId,
                targetDoctorUid,
                patient.getUid(),
                patient.getFullName(),
                AppNotification.TYPE_NEW_MESSAGE,
                "🚨 Emergency SOS Alert",
                patient.getFullName() + " triggered an emergency SOS alert. Open AROHI to respond immediately.",
                conversationId
        );
        sessionManager.saveNotificationLocal(sosNotif);

        // Sync Firestore: message, conversation thread, and notification
        if (firestore != null) {
            Map<String, Object> convUpdates = new HashMap<>();
            convUpdates.put("conversationId", conversationId);
            convUpdates.put("connectionId", conversationId);
            convUpdates.put("doctorUid", targetDoctorUid);
            convUpdates.put("doctorName", targetDoctorName);
            convUpdates.put("patientUid", patient.getUid());
            convUpdates.put("patientName", patient.getFullName());
            convUpdates.put("patientId", patient.getArohiId() != null ? patient.getArohiId() : "");
            convUpdates.put("connectionStatus", DoctorPatientConnection.STATUS_ACCEPTED);
            convUpdates.put("lastMessagePreview", preview);
            convUpdates.put("lastMessageSenderUid", patient.getUid());
            convUpdates.put("lastMessageTimestamp", msg.getTimestamp());
            convUpdates.put("unreadByDoctor", true);
            convUpdates.put("unreadByPatient", false);

            firestore.collection(FirestoreCollections.CONVERSATIONS)
                    .document(conversationId)
                    .set(convUpdates, com.google.firebase.firestore.SetOptions.merge());

            firestore.collection(FirestoreCollections.CONVERSATIONS)
                    .document(conversationId)
                    .collection(FirestoreCollections.MESSAGES)
                    .document(msg.getMessageId())
                    .set(msg);

            firestore.collection(FirestoreCollections.NOTIFICATIONS)
                    .document(notifId)
                    .set(sosNotif.toMap());
        }

        // Auditory confirmation
        EmergencyAlertSoundHelper.playShortConfirmationBeep();
        callback.onSuccess(targetDoctorName);
    }

    /**
     * Doctor or Nurse Emergency SOS:
     * Dispatches urgent message to the Hospital Authority group chat and broadcasts to Realtime Database.
     */
    public void sendStaffEmergencyAlertToAuthority(@NonNull UserAccount staff,
                                                   @NonNull String hospitalId,
                                                   @NonNull String emergencyType,
                                                   @NonNull String wardOrRoom,
                                                   @NonNull AuthCallback<EmergencyHospitalAlert> callback) {
        String alertId = "alert_" + UUID.randomUUID().toString().substring(0, 8);
        String urgentMessage = "🚨 [URGENT EMERGENCY ALERT] " + emergencyType.toUpperCase()
                + " triggered by " + staff.getFullName() + " (" + staff.getUserRoleEnum().getKey() + ")!\n"
                + "Location/Ward: " + wardOrRoom + "\nImmediate authority intervention & clinical team dispatch required!";

        EmergencyHospitalAlert alert = new EmergencyHospitalAlert(
                alertId,
                hospitalId,
                staff.getUid(),
                staff.getFullName(),
                staff.getUserRoleEnum().getKey(),
                emergencyType,
                urgentMessage,
                wardOrRoom
        );

        // 1. Broadcast to Firebase Realtime Database for sub-second beep alarm in Authority portal
        if (realtimeDb != null) {
            DatabaseReference alertRef = realtimeDb.getReference(RTDB_ROOT)
                    .child(RTDB_HOSPITALS)
                    .child(hospitalId)
                    .child(RTDB_ALERTS)
                    .child(alertId);
            alertRef.setValue(alert.toMap());
        }

        // 2. Broadcast message to both clinical hospital group channels (All Doctors & All Nurses)
        String doctorsGroupId = "grp_" + hospitalId + "_" + StaffConversation.TYPE_GROUP_DOCTORS.toLowerCase();
        String nursesGroupId = "grp_" + hospitalId + "_" + StaffConversation.TYPE_GROUP_NURSES.toLowerCase();

        // Send to Doctors group channel
        StaffMessage docMsg = new StaffMessage(
                "msg_sos_doc_" + UUID.randomUUID().toString().substring(0, 8),
                doctorsGroupId,
                hospitalId,
                staff.getUid(),
                staff.getFullName(),
                staff.getUserRoleEnum().getKey(),
                staff.getArohiId() != null ? staff.getArohiId() : "STAFF",
                urgentMessage
        );
        docMsg.setTimestamp(System.currentTimeMillis());
        communicationRepo.sendMessage(hospitalId, doctorsGroupId, docMsg, new AuthCallback<StaffMessage>() {
            @Override public void onSuccess(StaffMessage result) {}
            @Override public void onError(String message) {}
        });

        // Send to Nurses group channel
        StaffMessage nurseMsg = new StaffMessage(
                "msg_sos_nurse_" + UUID.randomUUID().toString().substring(0, 8),
                nursesGroupId,
                hospitalId,
                staff.getUid(),
                staff.getFullName(),
                staff.getUserRoleEnum().getKey(),
                staff.getArohiId() != null ? staff.getArohiId() : "STAFF",
                urgentMessage
        );
        nurseMsg.setTimestamp(System.currentTimeMillis());
        communicationRepo.sendMessage(hospitalId, nursesGroupId, nurseMsg, new AuthCallback<StaffMessage>() {
            @Override public void onSuccess(StaffMessage result) {}
            @Override public void onError(String message) {}
        });

        // 3. Sync Firestore
        if (firestore != null) {
            firestore.collection("hospitals")
                    .document(hospitalId)
                    .collection("emergencyAlerts")
                    .document(alertId)
                    .set(alert.toMap());
        }

        // Auditory confirmation
        EmergencyAlertSoundHelper.playShortConfirmationBeep();
        callback.onSuccess(alert);
    }

    public static class AuthorityMonitorHandle {
        public ListenerRegistration firestoreRegistration;
        public ValueEventListener rtdbListener;
        public DatabaseReference rtdbRef;

        public void detach() {
            if (firestoreRegistration != null) {
                firestoreRegistration.remove();
                firestoreRegistration = null;
            }
            if (rtdbRef != null && rtdbListener != null) {
                try {
                    rtdbRef.removeEventListener(rtdbListener);
                } catch (Exception ignored) {}
                rtdbListener = null;
            }
        }
    }

    /**
     * Authority Portal Real-time Listener:
     * Observes incoming hospital emergency alerts from doctors and nurses to play loud beep alarms.
     * Uses Cloud Firestore Snapshot Listener as the primary rock-solid real-time source,
     * with transparent Realtime Database synchronization if provisioned in Firebase Console.
     */
    @NonNull
    public AuthorityMonitorHandle attachAuthorityAlertMonitor(@NonNull String hospitalId,
                                                              @NonNull AlertObserver observer) {
        AuthorityMonitorHandle handle = new AuthorityMonitorHandle();

        // 1. Primary Cloud Firestore Snapshot Listener (Functions with standard Firestore + Auth setup)
        if (firestore != null) {
            handle.firestoreRegistration = firestore.collection("hospitals")
                    .document(hospitalId)
                    .collection("emergencyAlerts")
                    .whereEqualTo("acknowledged", false)
                    .addSnapshotListener((snapshots, error) -> {
                        if (snapshots != null && !snapshots.isEmpty()) {
                            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                                if (doc.exists() && doc.getData() != null) {
                                    EmergencyHospitalAlert alert = EmergencyHospitalAlert.fromMap(doc.getData());
                                    if (alert != null && !alert.isAcknowledged()) {
                                        observer.onAlertReceived(alert);
                                    }
                                }
                            }
                        }
                    });
        }

        // 2. Optional Realtime Database Listener (Transparently attaches if enabled in Firebase console)
        if (realtimeDb != null) {
            try {
                DatabaseReference alertsRef = realtimeDb.getReference(RTDB_ROOT)
                        .child(RTDB_HOSPITALS)
                        .child(hospitalId)
                        .child(RTDB_ALERTS);
                handle.rtdbRef = alertsRef;
                handle.rtdbListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                Object val = child.getValue();
                                if (val instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> map = (Map<String, Object>) val;
                                    EmergencyHospitalAlert alert = EmergencyHospitalAlert.fromMap(map);
                                    if (alert != null && !alert.isAcknowledged()) {
                                        observer.onAlertReceived(alert);
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                };
                alertsRef.addValueEventListener(handle.rtdbListener);
            } catch (Exception ignored) {}
        }

        return handle;
    }

    /**
     * Acknowledges an emergency alert and silences it.
     */
    public void acknowledgeAlert(@NonNull String hospitalId,
                                 @NonNull String alertId,
                                 @NonNull UserAccount admin) {
        if (realtimeDb != null) {
            DatabaseReference alertRef = realtimeDb.getReference(RTDB_ROOT)
                    .child(RTDB_HOSPITALS)
                    .child(hospitalId)
                    .child(RTDB_ALERTS)
                    .child(alertId);
            alertRef.child("acknowledged").setValue(true);
            alertRef.child("acknowledgedByUid").setValue(admin.getUid());
            alertRef.child("acknowledgedByName").setValue(admin.getFullName());
            alertRef.child("acknowledgedAt").setValue(System.currentTimeMillis());
        }

        if (firestore != null) {
            firestore.collection("hospitals")
                    .document(hospitalId)
                    .collection("emergencyAlerts")
                    .document(alertId)
                    .update("acknowledged", true,
                            "acknowledgedByUid", admin.getUid(),
                            "acknowledgedByName", admin.getFullName(),
                            "acknowledgedAt", System.currentTimeMillis());
        }

        // Silence beep alarm
        EmergencyAlertSoundHelper.stopEmergencyBeepAlert();
    }
}
