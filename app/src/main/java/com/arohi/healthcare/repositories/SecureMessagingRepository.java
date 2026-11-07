package com.arohi.healthcare.repositories;

import android.content.Context;

import androidx.annotation.NonNull;

import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.models.AppNotification;
import com.arohi.healthcare.models.ChatMessage;
import com.arohi.healthcare.models.ConversationThread;
import com.arohi.healthcare.models.DoctorPatientConnection;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.services.FirebaseProvider;
import com.arohi.healthcare.utils.SessionManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Phase 3 Secure Messaging & Notification Repository:
 * - Enforces that ONLY accepted and valid doctor-patient connections (/connections/{pairId} == Accepted)
 *   can list, read, listen to, or send messages in /conversations/{pairId}/messages.
 * - Provides real-time Firestore message streaming + hybrid local persistence, read-receipt updates,
 *   and privacy-safe notifications.
 */
public class SecureMessagingRepository {

    public interface MessageStreamListener {
        void onMessagesUpdated(@NonNull List<ChatMessage> messages);
        void onAccessDenied(@NonNull String reason);
        void onError(@NonNull String error);
    }

    private final Context appContext;
    private final SessionManager sessionManager;

    public SecureMessagingRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.sessionManager = new SessionManager(appContext);
    }

    // =========================================================================
    // 1. CONVERSATION LIST (ONLY ACCEPTED CONNECTIONS)
    // =========================================================================
    public void loadAuthorizedConversations(AuthCallback<List<ConversationThread>> callback) {
        UserAccount current = sessionManager.getCurrentSessionAccount();
        if (current == null) {
            callback.onError("No active authenticated user.");
            return;
        }
        UserRole role = current.getUserRoleEnum();
        String membershipField;
        if (role == UserRole.DOCTOR) {
            membershipField = "doctorUid";
        } else if (role == UserRole.PATIENT) {
            membershipField = "patientUid";
        } else {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(mergeConversationsForUser(current.getUid(), role, null));
            return;
        }

        db.collection(FirestoreCollections.CONVERSATIONS)
                .whereEqualTo(membershipField, current.getUid())
                .get()
                .addOnSuccessListener(snap -> {
                    List<ConversationThread> remoteList = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        ConversationThread thread = doc.toObject(ConversationThread.class);
                        if (thread != null
                                && DoctorPatientConnection.STATUS_ACCEPTED.equalsIgnoreCase(thread.getConnectionStatus())) {
                            remoteList.add(thread);
                        }
                    }
                    callback.onSuccess(mergeConversationsForUser(current.getUid(), role, remoteList));
                })
                .addOnFailureListener(e -> callback.onSuccess(mergeConversationsForUser(current.getUid(), role, null)));
    }

    private List<ConversationThread> mergeConversationsForUser(String uid, UserRole role, List<ConversationThread> remoteList) {
        Map<String, ConversationThread> byId = new LinkedHashMap<>();
        for (ConversationThread local : sessionManager.getAllConversationsLocal()) {
            boolean matchesUser = (role == UserRole.DOCTOR && uid.equals(local.getDoctorUid()))
                    || (role == UserRole.PATIENT && uid.equals(local.getPatientUid()));
            if (matchesUser
                    && DoctorPatientConnection.STATUS_ACCEPTED.equalsIgnoreCase(local.getConnectionStatus())
                    && local.getConversationId() != null) {
                byId.put(local.getConversationId(), local);
            }
        }
        if (remoteList != null) {
            for (ConversationThread remote : remoteList) {
                if (remote.getConversationId() != null) {
                    ConversationThread existing = byId.get(remote.getConversationId());
                    if (existing == null || remote.getLastMessageTimestamp() >= existing.getLastMessageTimestamp()) {
                        byId.put(remote.getConversationId(), remote);
                        sessionManager.saveConversationLocal(remote);
                    }
                }
            }
        }
        List<ConversationThread> threads = new ArrayList<>(byId.values());
        Collections.sort(threads, (a, b) -> Long.compare(b.getLastMessageTimestamp(), a.getLastMessageTimestamp()));
        return threads;
    }

    // =========================================================================
    // 2. CONNECTION GATE + REAL-TIME MESSAGE STREAMING + READ RECEIPTS
    // =========================================================================
    public void verifyConnectionAndAttachMessageListener(@NonNull String conversationId,
                                                         @NonNull MessageStreamListener listener,
                                                         @NonNull AuthCallback<ListenerRegistration> registrationCallback) {
        UserAccount current = sessionManager.getCurrentSessionAccount();
        if (current == null) {
            listener.onAccessDenied("Authentication required to access secure messaging.");
            return;
        }

        DoctorPatientConnection localConn = sessionManager.getConnectionLocal(conversationId);
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);

        if (db == null) {
            if (!validateConnectionAccess(localConn, current.getUid(), listener)) {
                return;
            }
            markLocalThreadAndMessagesRead(conversationId, localConn, current.getUid());
            listener.onMessagesUpdated(sessionManager.getMessagesLocal(conversationId));
            return;
        }

        // Backend + Repository verification: check /connections/{conversationId} and local registry
        db.collection(FirestoreCollections.CONNECTIONS).document(conversationId).get()
                .addOnSuccessListener(connSnap -> {
                    DoctorPatientConnection conn = connSnap.exists() ? connSnap.toObject(DoctorPatientConnection.class) : localConn;
                    if (!validateConnectionAccess(conn, current.getUid(), listener)) {
                        return;
                    }
                    sessionManager.saveConnectionLocal(conn);
                    String uid = current.getUid();

                    markLocalThreadAndMessagesRead(conversationId, conn, uid);
                    List<ChatMessage> initialLocal = sessionManager.getMessagesLocal(conversationId);
                    if (!initialLocal.isEmpty()) {
                        listener.onMessagesUpdated(initialLocal);
                    }

                    // Clear unread flag on the Firestore conversation thread for this participant
                    Map<String, Object> threadReadUpdate = new HashMap<>();
                    if (uid.equals(conn.getDoctorUid())) {
                        threadReadUpdate.put("unreadByDoctor", false);
                    } else {
                        threadReadUpdate.put("unreadByPatient", false);
                    }
                    db.collection(FirestoreCollections.CONVERSATIONS)
                            .document(conversationId)
                            .set(threadReadUpdate, SetOptions.merge());

                    // Attach real-time SnapshotListener on /conversations/{conversationId}/messages
                    ListenerRegistration reg = db.collection(FirestoreCollections.CONVERSATIONS)
                            .document(conversationId)
                            .collection(FirestoreCollections.MESSAGES)
                            .orderBy("timestamp", Query.Direction.ASCENDING)
                            .addSnapshotListener((querySnapshot, error) -> {
                                if (error != null) {
                                    List<ChatMessage> fallback = sessionManager.getMessagesLocal(conversationId);
                                    listener.onMessagesUpdated(fallback);
                                    return;
                                }
                                if (querySnapshot == null) {
                                    listener.onMessagesUpdated(sessionManager.getMessagesLocal(conversationId));
                                    return;
                                }

                                long now = System.currentTimeMillis();
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    ChatMessage msg = doc.toObject(ChatMessage.class);
                                    if (msg != null) {
                                        if (!msg.isRead() && msg.getSenderUid() != null && !msg.getSenderUid().equals(uid)) {
                                            msg.setRead(true);
                                            msg.setReadAt(now);
                                            Map<String, Object> readPatch = new HashMap<>();
                                            readPatch.put("read", true);
                                            readPatch.put("readAt", now);
                                            doc.getReference().update(readPatch);
                                        }
                                        sessionManager.appendMessageLocal(conversationId, msg);
                                    }
                                }
                                listener.onMessagesUpdated(sessionManager.getMessagesLocal(conversationId));
                            });

                    registrationCallback.onSuccess(reg);
                })
                .addOnFailureListener(e -> {
                    if (validateConnectionAccess(localConn, current.getUid(), listener)) {
                        markLocalThreadAndMessagesRead(conversationId, localConn, current.getUid());
                        listener.onMessagesUpdated(sessionManager.getMessagesLocal(conversationId));
                    }
                });
    }

    private boolean validateConnectionAccess(DoctorPatientConnection conn, String uid, MessageStreamListener listener) {
        if (conn == null) {
            listener.onAccessDenied("Access Denied: No authorized connection exists for this conversation.");
            return false;
        }
        if (!conn.isAccepted()) {
            listener.onAccessDenied("Access Denied: Consultation request is not in Accepted state.");
            return false;
        }
        boolean isMember = uid.equals(conn.getPatientUid()) || uid.equals(conn.getDoctorUid());
        if (!isMember) {
            listener.onAccessDenied("Access Denied: You are not a participant in this doctor-patient connection.");
            return false;
        }
        return true;
    }

    private void markLocalThreadAndMessagesRead(String conversationId, DoctorPatientConnection conn, String currentUid) {
        ConversationThread localThread = sessionManager.getConversationLocal(conversationId);
        if (localThread != null) {
            if (currentUid.equals(conn.getDoctorUid())) {
                localThread.setUnreadByDoctor(false);
            } else {
                localThread.setUnreadByPatient(false);
            }
            sessionManager.saveConversationLocal(localThread);
        }
        long now = System.currentTimeMillis();
        for (ChatMessage msg : sessionManager.getMessagesLocal(conversationId)) {
            if (!msg.isRead() && msg.getSenderUid() != null && !msg.getSenderUid().equals(currentUid)) {
                msg.setRead(true);
                msg.setReadAt(now);
                sessionManager.appendMessageLocal(conversationId, msg);
            }
        }
    }

    // =========================================================================
    // 3. SEND MESSAGE (WITH CONNECTION VERIFICATION & PRIVACY-SAFE NOTIFICATION)
    // =========================================================================
    public void sendSecureMessage(@NonNull String conversationId,
                                  @NonNull String receiverUid,
                                  @NonNull String rawText,
                                  AuthCallback<ChatMessage> callback) {
        String trimmed = rawText.trim();
        if (trimmed.isEmpty()) {
            callback.onError("Please enter a message before sending.");
            return;
        }

        UserAccount current = sessionManager.getCurrentSessionAccount();
        if (current == null) {
            callback.onError("You must be signed in to send messages.");
            return;
        }

        DoctorPatientConnection localConn = sessionManager.getConnectionLocal(conversationId);
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);

        if (db == null) {
            if (localConn == null || !localConn.isAccepted()) {
                callback.onError("Security Gate: Messaging is locked unless the connection is Accepted.");
                return;
            }
            String senderUid = current.getUid();
            if (!senderUid.equals(localConn.getPatientUid()) && !senderUid.equals(localConn.getDoctorUid())) {
                callback.onError("Security Gate: Unauthorized conversation participant.");
                return;
            }
            ChatMessage msg = createAndPersistMessageLocally(conversationId, localConn, current, receiverUid, trimmed, null);
            callback.onSuccess(msg);
            return;
        }

        // Verify /connections/{conversationId} is Accepted and caller is a member before writing
        db.collection(FirestoreCollections.CONNECTIONS).document(conversationId).get()
                .addOnSuccessListener(connSnap -> {
                    DoctorPatientConnection conn = connSnap.exists() ? connSnap.toObject(DoctorPatientConnection.class) : localConn;
                    if (conn == null || !conn.isAccepted()) {
                        callback.onError("Security Gate: Messaging is locked unless the connection is Accepted.");
                        return;
                    }
                    String senderUid = current.getUid();
                    if (!senderUid.equals(conn.getPatientUid()) && !senderUid.equals(conn.getDoctorUid())) {
                        callback.onError("Security Gate: Unauthorized conversation participant.");
                        return;
                    }

                    WriteBatch batch = db.batch();
                    ChatMessage message = createAndPersistMessageLocally(conversationId, conn, current, receiverUid, trimmed, batch);

                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess(message))
                            .addOnFailureListener(e -> callback.onSuccess(message));
                })
                .addOnFailureListener(e -> {
                    if (localConn != null && localConn.isAccepted()) {
                        String senderUid = current.getUid();
                        if (senderUid.equals(localConn.getPatientUid()) || senderUid.equals(localConn.getDoctorUid())) {
                            ChatMessage msg = createAndPersistMessageLocally(conversationId, localConn, current, receiverUid, trimmed, null);
                            callback.onSuccess(msg);
                            return;
                        }
                    }
                    callback.onError("Could not verify connection: " + e.getMessage());
                });
    }

    private ChatMessage createAndPersistMessageLocally(String conversationId,
                                                       DoctorPatientConnection conn,
                                                       UserAccount current,
                                                       String receiverUid,
                                                       String trimmed,
                                                       WriteBatch batch) {
        String senderUid = current.getUid();
        String messageId = "msg_" + UUID.randomUUID().toString().substring(0, 12);
        ChatMessage message = new ChatMessage(
                messageId,
                conversationId,
                senderUid,
                current.getFullName(),
                current.getRole(),
                receiverUid,
                trimmed
        );
        sessionManager.appendMessageLocal(conversationId, message);

        String preview = trimmed.length() > 60 ? trimmed.substring(0, 60) + "…" : trimmed;
        ConversationThread localThread = sessionManager.getConversationLocal(conversationId);
        if (localThread == null) {
            localThread = new ConversationThread();
            localThread.setConversationId(conversationId);
            localThread.setConnectionId(conversationId);
            localThread.setDoctorUid(conn.getDoctorUid());
            localThread.setDoctorName(conn.getDoctorName());
            localThread.setDoctorSpecialty(conn.getDoctorSpecialty());
            localThread.setPatientUid(conn.getPatientUid());
            localThread.setPatientName(conn.getPatientName());
            localThread.setPatientId(conn.getPatientId());
            localThread.setConnectionStatus(DoctorPatientConnection.STATUS_ACCEPTED);
        }
        localThread.setLastMessagePreview(preview);
        localThread.setLastMessageSenderUid(senderUid);
        localThread.setLastMessageTimestamp(message.getTimestamp());
        if (senderUid.equals(conn.getDoctorUid())) {
            localThread.setUnreadByPatient(true);
            localThread.setUnreadByDoctor(false);
        } else {
            localThread.setUnreadByDoctor(true);
            localThread.setUnreadByPatient(false);
        }
        sessionManager.saveConversationLocal(localThread);

        // Privacy-safe notification: never includes clinical message text in notification preview
        String notifId = "notif_" + UUID.randomUUID().toString().substring(0, 10);
        AppNotification msgNotif = new AppNotification(
                notifId,
                receiverUid,
                senderUid,
                current.getFullName(),
                AppNotification.TYPE_NEW_MESSAGE,
                "New Secure Clinical Message",
                current.getFullName() + " sent you a secure message. Open AROHI to view safely.",
                conversationId
        );
        sessionManager.saveNotificationLocal(msgNotif);

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (batch != null && db != null) {
            Map<String, Object> convUpdates = new HashMap<>();
            convUpdates.put("lastMessagePreview", preview);
            convUpdates.put("lastMessageSenderUid", senderUid);
            convUpdates.put("lastMessageTimestamp", message.getTimestamp());
            convUpdates.put("unreadByPatient", localThread.isUnreadByPatient());
            convUpdates.put("unreadByDoctor", localThread.isUnreadByDoctor());

            DocumentReference msgRef = db.collection(FirestoreCollections.CONVERSATIONS)
                    .document(conversationId)
                    .collection(FirestoreCollections.MESSAGES)
                    .document(messageId);
            batch.set(msgRef, message.toMap());
            batch.set(db.collection(FirestoreCollections.CONVERSATIONS).document(conversationId),
                    convUpdates, SetOptions.merge());
            batch.set(db.collection(FirestoreCollections.NOTIFICATIONS).document(notifId),
                    msgNotif.toMap());
        }
        return message;
    }

    // =========================================================================
    // 4. USER NOTIFICATIONS LIST & MARK AS READ
    // =========================================================================
    public void loadUserNotifications(String recipientUid, AuthCallback<List<AppNotification>> callback) {
        if (recipientUid == null) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(sessionManager.getNotificationsForRecipientLocal(recipientUid));
            return;
        }

        db.collection(FirestoreCollections.NOTIFICATIONS)
                .whereEqualTo("recipientUid", recipientUid)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        AppNotification item = doc.toObject(AppNotification.class);
                        if (item != null) {
                            sessionManager.saveNotificationLocal(item);
                        }
                    }
                    callback.onSuccess(sessionManager.getNotificationsForRecipientLocal(recipientUid));
                })
                .addOnFailureListener(e -> callback.onSuccess(sessionManager.getNotificationsForRecipientLocal(recipientUid)));
    }

    public void markAllNotificationsRead(String recipientUid) {
        if (recipientUid == null) return;
        sessionManager.markNotificationsReadLocal(recipientUid);

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) return;

        db.collection(FirestoreCollections.NOTIFICATIONS)
                .whereEqualTo("recipientUid", recipientUid)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Boolean read = doc.getBoolean("read");
                        if (read == null || !read) {
                            doc.getReference().update("read", true);
                        }
                    }
                });
    }
}
