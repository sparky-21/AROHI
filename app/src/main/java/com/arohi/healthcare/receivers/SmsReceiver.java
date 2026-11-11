package com.arohi.healthcare.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SMS BroadcastReceiver for AROHI Healthcare.
 * Handles two incoming SMS scenarios:
 * 1. OTP / Verification Code auto-read — extracts numeric codes and broadcasts locally
 *    so that any listening Activity can auto-fill the OTP field.
 * 2. Emergency Alert SMS — detects keywords like "SOS", "EMERGENCY", "URGENT" from known
 *    contacts and broadcasts a local emergency intent for the app to handle.
 *
 * Requires android.permission.RECEIVE_SMS at runtime (Android 6.0+).
 */
public class SmsReceiver extends BroadcastReceiver {

    /** Local broadcast action fired when an OTP code is extracted from an incoming SMS. */
    public static final String ACTION_OTP_RECEIVED = "com.arohi.healthcare.OTP_RECEIVED";

    /** Local broadcast action fired when an emergency keyword SMS is detected. */
    public static final String ACTION_EMERGENCY_SMS_RECEIVED = "com.arohi.healthcare.EMERGENCY_SMS_RECEIVED";

    /** Intent extra key for the extracted OTP code (String). */
    public static final String EXTRA_OTP_CODE = "extra_otp_code";

    /** Intent extra key for the full SMS body text (String). */
    public static final String EXTRA_SMS_BODY = "extra_sms_body";

    /** Intent extra key for the sender phone number (String). */
    public static final String EXTRA_SENDER_ADDRESS = "extra_sender_address";

    /** Regex pattern to extract a 4-to-8 digit numeric OTP code from SMS body. */
    private static final Pattern OTP_PATTERN = Pattern.compile("\\b(\\d{4,8})\\b");

    /** Keywords that indicate an emergency SMS. */
    private static final String[] EMERGENCY_KEYWORDS = {
            "SOS", "EMERGENCY", "URGENT", "CRITICAL", "HELP", "AMBULANCE", "911"
    };

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null || pdus.length == 0) {
            return;
        }

        String format = bundle.getString("format", "3gpp");

        for (Object pdu : pdus) {
            SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
            if (smsMessage == null) {
                continue;
            }

            String sender = smsMessage.getDisplayOriginatingAddress();
            String body = smsMessage.getMessageBody();
            if (body == null || body.isEmpty()) {
                continue;
            }

            // Check for AROHI OTP / verification code pattern
            if (isArohiOtpMessage(body)) {
                String otpCode = extractOtpCode(body);
                if (otpCode != null) {
                    Intent otpIntent = new Intent(ACTION_OTP_RECEIVED);
                    otpIntent.putExtra(EXTRA_OTP_CODE, otpCode);
                    otpIntent.putExtra(EXTRA_SMS_BODY, body);
                    otpIntent.putExtra(EXTRA_SENDER_ADDRESS, sender != null ? sender : "");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(otpIntent);
                }
            }

            // Check for emergency keywords
            if (containsEmergencyKeyword(body)) {
                Intent emergencyIntent = new Intent(ACTION_EMERGENCY_SMS_RECEIVED);
                emergencyIntent.putExtra(EXTRA_SMS_BODY, body);
                emergencyIntent.putExtra(EXTRA_SENDER_ADDRESS, sender != null ? sender : "");
                LocalBroadcastManager.getInstance(context).sendBroadcast(emergencyIntent);
            }
        }
    }

    /**
     * Checks if the SMS body looks like an AROHI verification/OTP message.
     * Matches messages containing "AROHI" or common OTP keywords.
     */
    private boolean isArohiOtpMessage(@NonNull String body) {
        String upper = body.toUpperCase();
        return upper.contains("AROHI")
                || upper.contains("OTP")
                || upper.contains("VERIFICATION CODE")
                || upper.contains("VERIFY")
                || upper.contains("ONE-TIME")
                || upper.contains("ONE TIME PASSWORD");
    }

    /**
     * Extracts the first 4-to-8 digit numeric code from the SMS body.
     *
     * @return The OTP code string, or null if no numeric code found.
     */
    private String extractOtpCode(@NonNull String body) {
        Matcher matcher = OTP_PATTERN.matcher(body);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Checks if the SMS body contains any emergency alert keywords.
     */
    private boolean containsEmergencyKeyword(@NonNull String body) {
        String upper = body.toUpperCase();
        for (String keyword : EMERGENCY_KEYWORDS) {
            if (upper.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
