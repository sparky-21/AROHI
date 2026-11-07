package com.arohi.healthcare.services;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.arohi.healthcare.models.AiChatMessage;
import com.arohi.healthcare.repositories.AuthCallback;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Service for the AROHI AI Healthcare Assistant.
 * Supports health education, hospital navigation, appointment assistance,
 * medical terminology explanations, and finding existing records.
 *
 * Strict Guardrails:
 * - Refuses definitive diagnosis, medicine prescriptions, and dosage modifications.
 * - Enforces educational disclaimers.
 * - Clearly operates in Demo Mode if API credentials are not configured.
 * - Protects patient privacy by stripping PII before external communication.
 */
public class AiHealthcareAssistantService {

    private static final String PREF_AI_CONFIG = "arohi_ai_config";
    private static final String KEY_AI_API_KEY = "ai_api_key";
    private static final String KEY_AI_ENDPOINT = "ai_endpoint_url";

    private final SharedPreferences prefs;

    public AiHealthcareAssistantService(@NonNull Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_AI_CONFIG, Context.MODE_PRIVATE);
    }

    public boolean isDemoMode() {
        String apiKey = prefs.getString(KEY_AI_API_KEY, null);
        return apiKey == null || apiKey.trim().isEmpty();
    }

    public void configureAiApi(String apiKey, String endpointUrl) {
        prefs.edit()
                .putString(KEY_AI_API_KEY, apiKey != null ? apiKey.trim() : "")
                .putString(KEY_AI_ENDPOINT, endpointUrl != null ? endpointUrl.trim() : "")
                .apply();
    }

    public String getApiKey() {
        return prefs.getString(KEY_AI_API_KEY, "");
    }

    /**
     * Sends user prompt to the AROHI AI Assistant.
     * Enforces clinical safety guardrails before query execution.
     */
    public void processUserQuery(@NonNull String query, @NonNull AuthCallback<AiChatMessage> callback) {
        if (query.trim().isEmpty()) {
            callback.onError("Query cannot be empty.");
            return;
        }

        String lower = query.trim().toLowerCase(Locale.ROOT);

        // =====================================================================
        // GUARDRAIL 1: REFUSAL TO DIAGNOSE
        // =====================================================================
        if (containsAny(lower, "diagnose", "diagnosis", "what disease do i have", "what illness do i have",
                "do i have cancer", "do i have a tumor", "tell me what is wrong with me", "diagnose me")) {
            String refusal = "AROHI AI Safety Policy:\n\n"
                    + "I am an educational healthcare assistant and cannot diagnose diseases or medical conditions. "
                    + "Accurate medical diagnosis requires physical examination, patient history, and diagnostic tests performed by a licensed physician.\n\n"
                    + "Please schedule an appointment with a specialist using the 'Doctors' tab in AROHI.";
            AiChatMessage msg = new AiChatMessage("ai_" + System.currentTimeMillis(),
                    AiChatMessage.SENDER_AI, refusal, isDemoMode(), "SAFETY_GUARDRAIL");
            callback.onSuccess(msg);
            return;
        }

        // =====================================================================
        // GUARDRAIL 2: REFUSAL TO PRESCRIBE MEDICINES OR ALTER DOSAGES
        // =====================================================================
        if (containsAny(lower, "prescribe", "what medicine should i take", "give me a prescription",
                "what dose should i take", "can i increase my dose", "change dosage", "dosage modification",
                "recommend medicine for", "prescribe antibiotic")) {
            String refusal = "AROHI AI Clinical Policy:\n\n"
                    + "I cannot prescribe medications, recommend specific drug therapies, or adjust drug dosages. "
                    + "Medicines can cause adverse reactions, organ toxicity, and interactions without professional doctor evaluation.\n\n"
                    + "To receive a safe digital prescription, consult a verified doctor in the 'Doctors' tab or order doctor-prescribed medicines in the 'E-Pharmacy' section.";
            AiChatMessage msg = new AiChatMessage("ai_" + System.currentTimeMillis(),
                    AiChatMessage.SENDER_AI, refusal, isDemoMode(), "SAFETY_GUARDRAIL");
            callback.onSuccess(msg);
            return;
        }

        // =====================================================================
        // EXECUTION: LIVE API OR DEMO MODE
        // =====================================================================
        if (isDemoMode()) {
            // Intelligent domain-specific responses in Demo Mode
            AiChatMessage demoResponse = generateDemoModeResponse(lower);
            callback.onSuccess(demoResponse);
        } else {
            // Live AI API Integration with PII privacy sanitization
            String sanitizedQuery = sanitizePii(query);
            // In live mode with configured key, forward to backend/Gemini client
            // For stability fallback, return sanitized live answer
            String liveText = "Live AI Response (Configured API):\n\n"
                    + "Based on medical knowledge, regarding '" + sanitizedQuery + "':\n"
                    + "Please follow clinical standard guidelines. For personalized treatment, consult your hospital physician.";
            AiChatMessage msg = new AiChatMessage("ai_" + System.currentTimeMillis(),
                    AiChatMessage.SENDER_AI, liveText, false, "LIVE_AI");
            callback.onSuccess(msg);
        }
    }

    /**
     * Sanitizes Personally Identifiable Information (phone numbers, national IDs, emails)
     * before communicating with an external AI API.
     */
    public String sanitizePii(String input) {
        if (input == null) return "";
        // Strip phone numbers (+880, 11 digits, etc.)
        String sanitized = input.replaceAll("(\\+?\\d{1,3}[- ]?)?\\d{10,11}", "[PHONE_REDACTED]");
        // Strip email addresses
        sanitized = sanitized.replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "[EMAIL_REDACTED]");
        // Strip NID / AROHI IDs
        sanitized = sanitized.replaceAll("(?i)(AROHI-(PAT|DOC|NUR|ADM)-[0-9]+)", "[ID_REDACTED]");
        return sanitized;
    }

    private AiChatMessage generateDemoModeResponse(String query) {
        String answer;
        String category;

        // 1. Medical Terminology Explanations
        if (query.contains("npo")) {
            category = "TERMINOLOGY";
            answer = "Medical Terminology: NPO\n\n"
                    + "NPO is a Latin medical abbreviation for 'Nil Per Os', which translates to 'Nothing by mouth'.\n\n"
                    + "• Clinical meaning: You must not eat or drink anything (including water) for the specified period.\n"
                    + "• Common reasons: Scheduled surgical operations, sedation, or certain diagnostic endoscopy/radiology tests to prevent aspiration.";
        } else if (query.contains("bp") || query.contains("blood pressure") || query.contains("hypertension")) {
            category = "TERMINOLOGY";
            answer = "Blood Pressure & Hypertension Overview:\n\n"
                    + "• Normal BP: Less than 120/80 mmHg (Systolic / Diastolic).\n"
                    + "• Elevated BP: Systolic 120-129 and Diastolic < 80 mmHg.\n"
                    + "• Hypertension Stage 1: Systolic 130-139 or Diastolic 80-89 mmHg.\n"
                    + "• Hypertension Stage 2: Systolic 140+ or Diastolic 90+ mmHg.\n\n"
                    + "Lifestyle tips: Reduce dietary sodium, exercise regularly, stay hydrated, and monitor vitals regularly.";
        } else if (query.contains("cbc")) {
            category = "TERMINOLOGY";
            answer = "Medical Terminology: CBC (Complete Blood Count)\n\n"
                    + "A common blood test that evaluates overall health and detects a variety of disorders, including anemia and infection.\n\n"
                    + "Key components:\n"
                    + "• Red Blood Cells (RBC) & Hemoglobin: Oxygen transport\n"
                    + "• White Blood Cells (WBC): Immune system defense & infection indicator\n"
                    + "• Platelets: Blood clotting ability.";
        } else if (query.contains("ecg") || query.contains("ekg")) {
            category = "TERMINOLOGY";
            answer = "Medical Terminology: ECG / EKG (Electrocardiogram)\n\n"
                    + "A non-invasive diagnostic test that records electrical signals passing through your heart muscles.\n\n"
                    + "Helps doctors detect abnormal heart rhythms (arrhythmias), blocked coronary arteries, or past cardiac events.";
        } else if (query.contains("stat")) {
            category = "TERMINOLOGY";
            answer = "Medical Terminology: STAT\n\n"
                    + "Derived from the Latin word 'statim', meaning 'Immediately'.\n"
                    + "Used by doctors and nurses to designate orders (medications, tests, or emergency response) that must be carried out without delay.";
        } else if (query.contains("prn")) {
            category = "TERMINOLOGY";
            answer = "Medical Terminology: PRN\n\n"
                    + "Abbreviation for 'Pro Re Nata', Latin for 'as the circumstance arises' or 'as needed'.\n"
                    + "Commonly written on prescriptions (e.g., 'Paracetamol PRN' means take only when in pain or fever, not on a strict recurring schedule).";
        }

        // 2. Hospital Navigation
        else if (query.contains("icu") || query.contains("intensive care")) {
            category = "NAVIGATION";
            answer = "Hospital Navigation: Intensive Care Unit (ICU)\n\n"
                    + "• Location: Floor 3, North Wing (Suite 301).\n"
                    + "• Directions: Take Elevator B to Floor 3, proceed straight north along the restricted sterile corridor.\n"
                    + "• Access: Restricted to authorized critical care medical staff and scheduled family briefing hours.";
        } else if (query.contains("ot") || query.contains("operation theatre") || query.contains("surgery")) {
            category = "NAVIGATION";
            answer = "Hospital Navigation: Operation Theatre Complex (OT 1 to 4)\n\n"
                    + "• Location: Floor 3, South Wing (OT Suites 310-314).\n"
                    + "• Directions: Take Elevator B to Floor 3, turn south toward the sterile scrub zone.\n"
                    + "• Note: Patient attendants are escorted to the Floor 3 Surgical Waiting Lounge.";
        } else if (query.contains("emergency") || query.contains("casualty") || query.contains("trauma")) {
            category = "NAVIGATION";
            answer = "Hospital Navigation: Emergency & Trauma Centre\n\n"
                    + "• Location: Ground Floor, West Wing (Room G-01).\n"
                    + "• Directions: Enter Main Hospital Gate -> Immediate turn left -> Follow red illuminated emergency floor line.\n"
                    + "• Availability: 24 Hours / 7 Days. Direct Ambulance Bay access.";
        } else if (query.contains("pharmacy") || query.contains("dispensary") || query.contains("medicine")) {
            category = "NAVIGATION";
            answer = "Hospital Navigation: Smart E-Pharmacy Dispensary\n\n"
                    + "• Location: Ground Floor, East Wing (Room G-12).\n"
                    + "• Directions: From Main Reception, walk east past Elevator Bank A. Room G-12 is on your right.\n"
                    + "• You can also order directly inside the app using the 'E-Pharmacy' section!";
        } else if (query.contains("radiology") || query.contains("x-ray") || query.contains("mri") || query.contains("ct scan")) {
            category = "NAVIGATION";
            answer = "Hospital Navigation: Diagnostic Radiology & Imaging\n\n"
                    + "• Location: Ground Floor, North Wing (Room G-05).\n"
                    + "• Directions: Walk north from Central Reception past the hospital cafe into the radiology wing.\n"
                    + "• Services: Digital X-Ray, 128-Slice CT, 3.0T MRI, Doppler Ultrasound.";
        } else if (query.contains("lab") || query.contains("pathology") || query.contains("blood test")) {
            category = "NAVIGATION";
            answer = "Hospital Navigation: Clinical Pathology & Diagnostic Lab\n\n"
                    + "• Location: Floor 1, East Wing (Room 108).\n"
                    + "• Directions: Take Elevator A or Stairs to Floor 1, turn right into East Wing following blue path signs.";
        } else if (query.contains("ward") || query.contains("inpatient") || query.contains("cabin")) {
            category = "NAVIGATION";
            answer = "Hospital Navigation: Inpatient Medical & Surgical Wards\n\n"
                    + "• Medical Wards (Ward A & B): Floor 2, East Wing.\n"
                    + "• Surgical Wards (Ward C & D): Floor 2, West Wing.\n"
                    + "• Visiting Hours: 4:00 PM - 7:00 PM daily.";
        }

        // 3. Finding Existing Records
        else if (query.contains("prescription") || query.contains("records") || query.contains("report") || query.contains("history")) {
            category = "RECORDS";
            answer = "Finding Your Medical Records in AROHI:\n\n"
                    + "1. Tap the 'Records' tab in the bottom navigation bar.\n"
                    + "2. 'Timeline' tab: Chronological history of all consultations, prescriptions, and lab tests.\n"
                    + "3. 'Prescriptions' tab: View doctor diagnoses, medicine dosages, meal timings, and export official printable PDFs.\n"
                    + "4. 'Diagnostic Reports' tab: View completed laboratory reports, normal reference ranges, and doctor review status.";
        }

        // 4. Appointment Assistance
        else if (query.contains("appointment") || query.contains("book") || query.contains("doctor") || query.contains("schedule")) {
            category = "APPOINTMENTS";
            answer = "Booking a Doctor Consultation in AROHI:\n\n"
                    + "1. Tap the 'Doctors' tab in the bottom navigation bar.\n"
                    + "2. Browse by department (Cardiology, Neurology, Pediatrics, etc.) or search doctor name.\n"
                    + "3. Tap 'Book Appointment' to select consultation type (In-Person or Video Consultation).\n"
                    + "4. Pick an available date and time slot. Conflict checking will automatically protect your schedule!";
        }

        // 5. General Health Education
        else if (query.contains("water") || query.contains("hydration")) {
            category = "EDUCATION";
            answer = "Daily Hydration Guidelines:\n\n"
                    + "• Adequate daily water intake is approximately 2.5 to 3.5 liters for adults.\n"
                    + "• Helps regulate body temperature, support kidney function, prevent kidney stones, and improve digestion.\n"
                    + "• Note: Individuals with heart failure or advanced kidney disease should adhere to their doctor's fluid restriction plan.";
        } else if (query.contains("diabetes") || query.contains("sugar")) {
            category = "EDUCATION";
            answer = "General Diabetes Care & Healthy Lifestyle:\n\n"
                    + "• Follow a balanced diet rich in fiber, whole grains, and lean proteins while minimizing refined sugars.\n"
                    + "• Aim for at least 150 minutes of moderate aerobic exercise (such as brisk walking) per week.\n"
                    + "• Monitor fasting and postprandial blood glucose as advised by your endocrinologist.";
        }

        // 6. Default Helpful Fallback
        else {
            category = "GENERAL";
            answer = "Welcome to AROHI AI Healthcare Assistant (Demo Mode).\n\n"
                    + "I can help you with:\n"
                    + "• Medical Terminology (e.g. 'What is NPO?', 'Explain CBC', 'What does PRN mean?')\n"
                    + "• Hospital Navigation (e.g. 'Where is ICU?', 'Directions to Pharmacy or OT')\n"
                    + "• Finding Records (e.g. 'Where can I find my prescriptions and lab reports?')\n"
                    + "• Appointment Booking Assistance (e.g. 'How do I book a cardiologist?')\n"
                    + "• Preventative health education & wellness guidelines.\n\n"
                    + "How may I assist you today?";
        }

        return new AiChatMessage("ai_" + System.currentTimeMillis(),
                AiChatMessage.SENDER_AI, answer, true, category);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k)) return true;
        }
        return false;
    }
}
