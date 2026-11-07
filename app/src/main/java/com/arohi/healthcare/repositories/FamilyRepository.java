package com.arohi.healthcare.repositories;

import android.content.Context;

import androidx.annotation.NonNull;

import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.models.FamilyMember;
import com.arohi.healthcare.utils.SessionManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Repository for managing authorized Family Health Profiles.
 * Supports family care coordination, appointment booking, and reminders.
 *
 * CRITICAL ACCESS CONTROL:
 * Under no circumstances does a family profile grant automatic access to another patient's
 * confidential medical records, diagnostic reports, or digital prescriptions.
 */
public class FamilyRepository {

    private final SessionManager sessionManager;
    private final FirebaseFirestore firestore;

    public FamilyRepository(@NonNull Context context) {
        this.sessionManager = new SessionManager(context);
        this.firestore = FirebaseFirestore.getInstance();
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public void loadFamilyMembers(@NonNull String primaryPatientUid,
                                  @NonNull AuthCallback<List<FamilyMember>> callback) {
        // Step 1: Cache-First Instant Local Delivery
        List<FamilyMember> localList = sessionManager.getFamilyMembersForPatientLocal(primaryPatientUid);
        callback.onSuccess(localList);

        // Step 2: Background Firestore Synchronization
        firestore.collection(FirestoreCollections.FAMILY_MEMBERS)
                .whereEqualTo("primaryPatientUid", primaryPatientUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            if (doc.getData() != null) {
                                FamilyMember remote = FamilyMember.fromMap(doc.getData());
                                if (remote != null) {
                                    sessionManager.saveFamilyMemberLocal(remote);
                                }
                            }
                        }
                    }
                    callback.onSuccess(sessionManager.getFamilyMembersForPatientLocal(primaryPatientUid));
                })
                .addOnFailureListener(e -> {
                    // Fallback to local cache already delivered
                });
    }

    public void addFamilyMember(@NonNull FamilyMember member,
                                @NonNull AuthCallback<FamilyMember> callback) {
        if (!member.isHasExplicitAuthorization()) {
            callback.onError("Explicit authorization and consent is required to add a family profile.");
            return;
        }

        if (member.getFullName() == null || member.getFullName().trim().isEmpty()) {
            callback.onError("Full name is required.");
            return;
        }

        if (member.getMemberId() == null || member.getMemberId().isEmpty()) {
            member.setMemberId("fam_" + System.currentTimeMillis());
        }

        // Save locally
        sessionManager.saveFamilyMemberLocal(member);

        // Sync Firestore
        firestore.collection(FirestoreCollections.FAMILY_MEMBERS)
                .document(member.getMemberId())
                .set(member.toMap())
                .addOnSuccessListener(aVoid -> callback.onSuccess(member))
                .addOnFailureListener(e -> callback.onSuccess(member)); // local already persisted
    }

    public void deleteFamilyMember(@NonNull String memberId,
                                   @NonNull AuthCallback<Void> callback) {
        sessionManager.deleteFamilyMemberLocal(memberId);

        firestore.collection(FirestoreCollections.FAMILY_MEMBERS)
                .document(memberId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onSuccess(null));
    }

    /**
     * Privacy & Access Validation:
     * Explicitly enforces that adding or listing someone as a family member NEVER grants
     * automatic access to confidential medical records or prescriptions.
     */
    public boolean canAccessMedicalRecords(@NonNull String familyMemberId, @NonNull String targetPatientUid) {
        // Strict HIPAA/GDPR clinical isolation: automatic medical record access is DENIED
        return false;
    }
}
