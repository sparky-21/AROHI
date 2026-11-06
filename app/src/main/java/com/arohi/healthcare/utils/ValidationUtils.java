package com.arohi.healthcare.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Pure Java validation utility for AROHI authentication, registration, and profile forms.
 */
public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[0-9]{10,15}$"
    );

    // Accepts recommended YYYY/MM/DD (as well as YYYY-MM-DD)
    private static final Pattern DOB_PATTERN = Pattern.compile(
            "^\\d{4}[/\\-](0[1-9]|1[0-2])[/\\-](0[1-9]|[12]\\d|3[01])$"
    );

    private static final Pattern LICENSE_PATTERN = Pattern.compile(
            "^[A-Za-z0-9\\-]{5,24}$"
    );

    private static final Set<String> VALID_BLOOD_GROUPS = new HashSet<>(Arrays.asList(
            "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
    ));

    // Institutional authorization format required to request Admin setup (never hardcoded as a bypass;
    // verified against /adminAuthorizations/{uid} in Firestore when online)
    private static final Pattern ADMIN_TOKEN_FORMAT = Pattern.compile(
            "^AROHI-AUTH-[A-Z0-9]{6,16}$"
    );

    private ValidationUtils() {
        // Prevent instantiation
    }

    public static boolean isValidFullName(String name) {
        return name != null && name.trim().length() >= 3 && name.trim().length() <= 80;
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) {
            return false;
        }
        String normalized = phone.replaceAll("[\\s\\-()]", "");
        return PHONE_PATTERN.matcher(normalized).matches();
    }

    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        return hasLetter && hasDigit;
    }

    public static boolean doPasswordsMatch(String password, String confirmPassword) {
        return password != null && password.equals(confirmPassword);
    }

    public static boolean isValidDateOfBirth(String dob) {
        return dob != null && DOB_PATTERN.matcher(dob.trim()).matches();
    }

    public static boolean isValidBloodGroup(String bloodGroup) {
        return bloodGroup != null && VALID_BLOOD_GROUPS.contains(bloodGroup.trim().toUpperCase());
    }

    public static boolean isValidProfessionalLicense(String license) {
        return license != null && LICENSE_PATTERN.matcher(license.trim()).matches();
    }

    public static boolean isValidExperienceYears(String yearsStr) {
        if (yearsStr == null || yearsStr.trim().isEmpty()) {
            return false;
        }
        try {
            int years = Integer.parseInt(yearsStr.trim());
            return years >= 0 && years <= 65;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidConsultationFee(String feeStr) {
        if (feeStr == null || feeStr.trim().isEmpty()) {
            return false;
        }
        try {
            double fee = Double.parseDouble(feeStr.trim());
            return fee >= 0.0 && fee <= 100000.0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidAdminTokenFormat(String token) {
        return token != null && ADMIN_TOKEN_FORMAT.matcher(token.trim()).matches();
    }
}
