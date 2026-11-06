package com.arohi.healthcare.utils;

import com.arohi.healthcare.models.UserRole;
import java.util.Locale;
import java.util.UUID;

/**
 * Generates structured, human-readable AROHI healthcare identifiers for users, patients,
 * doctors, nurses, and administrators.
 */
public final class IdGenerator {

    private IdGenerator() {
        // Prevent instantiation
    }

    public static String generateRoleIdentifier(UserRole role, String uid) {
        String prefix;
        switch (role) {
            case DOCTOR:
                prefix = "AROHI-DOC-";
                break;
            case NURSE:
                prefix = "AROHI-NRS-";
                break;
            case ADMIN:
                prefix = "AROHI-ADM-";
                break;
            case PATIENT:
            default:
                prefix = "AROHI-PAT-";
                break;
        }
        String suffix;
        if (uid != null && uid.trim().length() >= 6) {
            suffix = uid.trim().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.US);
            if (suffix.length() > 8) {
                suffix = suffix.substring(0, 8);
            }
        } else {
            suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.US);
        }
        return prefix + suffix;
    }
}
