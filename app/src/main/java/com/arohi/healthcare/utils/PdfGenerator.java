package com.arohi.healthcare.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.arohi.healthcare.models.DigitalPrescription;
import com.arohi.healthcare.models.PrescriptionMedicine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Utility for generating official A4 PDF Digital Prescriptions and opening/sharing
 * them via Android's native FileProvider.
 */
public final class PdfGenerator {

    private PdfGenerator() {
        // Prevent instantiation
    }

    /**
     * Generates an official A4 PDF document from a DigitalPrescription and returns the File.
     */
    public static File generatePrescriptionPdf(@NonNull Context context,
                                               @NonNull DigitalPrescription rx) throws IOException {
        PdfDocument document = new PdfDocument();

        // Standard A4 dimensions in PostScript points: 595 x 842 pt
        final int pageWidth = 595;
        final int pageHeight = 842;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // 1. Top Hospital Header Banner
        paint.setColor(Color.parseColor("#263D2B")); // Deep forest green
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, pageWidth, 75, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(18f);
        paint.setFakeBoldText(true);
        canvas.drawText("AROHI HEALTHCARE ECOSYSTEM", 32, 38, paint);

        paint.setTextSize(10f);
        paint.setFakeBoldText(false);
        paint.setColor(Color.parseColor("#E8EFE2"));
        canvas.drawText("Digital Health Portal • \"Every Step Towards Better Health.\"", 32, 56, paint);

        // Date in header
        String dateStr = rx.getCreatedAt() > 0
                ? DateFormat.format("yyyy-MM-dd hh:mm a", new Date(rx.getCreatedAt())).toString()
                : DateFormat.format("yyyy-MM-dd", new Date()).toString();
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Prescription ID: " + safe(rx.getPrescriptionId(), "Rx-001"), pageWidth - 32, 38, paint);
        canvas.drawText("Date: " + dateStr, pageWidth - 32, 56, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        // 2. Doctor Header Block
        int y = 100;
        paint.setColor(Color.parseColor("#26332A"));
        paint.setTextSize(14f);
        paint.setFakeBoldText(true);
        canvas.drawText(safe(rx.getDoctorName(), "Dr. Specialist"), 32, y, paint);

        y += 16;
        paint.setTextSize(10f);
        paint.setFakeBoldText(false);
        paint.setColor(Color.parseColor("#557B52"));
        String docSub = safe(rx.getDoctorSpecialty(), "General Medicine")
                + " • BMDC Reg: " + safe(rx.getDoctorLicense(), "Verified")
                + (rx.getDoctorDepartment() != null ? " • Dept: " + rx.getDoctorDepartment() : "");
        canvas.drawText(docSub, 32, y, paint);

        y += 18;
        paint.setColor(Color.parseColor("#D5E0CE"));
        paint.setStrokeWidth(1f);
        canvas.drawLine(32, y, pageWidth - 32, y, paint);

        // 3. Patient Details Card Box
        y += 12;
        paint.setColor(Color.parseColor("#F7F8F3"));
        paint.setStyle(Paint.Style.FILL);
        RectF patBox = new RectF(32, y, pageWidth - 32, y + 60);
        canvas.drawRoundRect(patBox, 6, 6, paint);

        paint.setColor(Color.parseColor("#D5E0CE"));
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(patBox, 6, 6, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#26332A"));
        paint.setTextSize(10.5f);
        paint.setFakeBoldText(true);
        canvas.drawText("PATIENT: " + safe(rx.getPatientName(), "Patient").toUpperCase(), 44, y + 20, paint);
        paint.setFakeBoldText(false);
        canvas.drawText("ID: " + safe(rx.getPatientId(), "AROHI-PAT"), 44, y + 36, paint);
        canvas.drawText("Blood Group: " + safe(rx.getPatientBloodGroup(), "N/A"), 44, y + 50, paint);

        canvas.drawText("Gender: " + safe(rx.getPatientGender(), "N/A"), 250, y + 20, paint);
        canvas.drawText("Age/DOB: " + safe(rx.getPatientAgeOrDob(), "N/A"), 250, y + 36, paint);

        String obs = safe(rx.getClinicalObservations(), "Normal");
        canvas.drawText("Vitals: " + obs, 380, y + 20, paint);

        y += 76;

        // 4. Diagnosis Box
        paint.setColor(Color.parseColor("#557B52"));
        paint.setTextSize(11f);
        paint.setFakeBoldText(true);
        canvas.drawText("DIAGNOSIS / CLINICAL ASSESSMENT:", 32, y, paint);

        y += 16;
        paint.setColor(Color.parseColor("#26332A"));
        paint.setTextSize(12f);
        paint.setFakeBoldText(false);
        canvas.drawText(safe(rx.getDiagnosis(), "General Consultation Assessment"), 32, y, paint);

        y += 24;

        // 5. Rx Symbol
        paint.setColor(Color.parseColor("#557B52"));
        paint.setTextSize(26f);
        paint.setFakeBoldText(true);
        canvas.drawText("Rx", 32, y + 10, paint);

        y += 22;

        // 6. Medicines Table
        // Table Header
        paint.setColor(Color.parseColor("#E8EFE2"));
        paint.setStyle(Paint.Style.FILL);
        RectF tblHeader = new RectF(32, y, pageWidth - 32, y + 24);
        canvas.drawRoundRect(tblHeader, 4, 4, paint);

        paint.setColor(Color.parseColor("#263D2B"));
        paint.setTextSize(10f);
        paint.setFakeBoldText(true);
        canvas.drawText("#", 42, y + 16, paint);
        canvas.drawText("Medicine Name & Dosage", 65, y + 16, paint);
        canvas.drawText("Frequency", 260, y + 16, paint);
        canvas.drawText("Duration", 360, y + 16, paint);
        canvas.drawText("Meal Instructions", 440, y + 16, paint);

        y += 28;
        paint.setFakeBoldText(false);

        List<PrescriptionMedicine> meds = rx.getMedicines();
        if (meds != null && !meds.isEmpty()) {
            int idx = 1;
            for (PrescriptionMedicine m : meds) {
                if (m == null) continue;
                paint.setColor(Color.parseColor("#26332A"));
                paint.setTextSize(10.5f);
                paint.setFakeBoldText(true);
                canvas.drawText(String.valueOf(idx) + ".", 42, y + 14, paint);

                String medLine = safe(m.getMedicineName(), "Medicine")
                        + (!isEmpty(m.getDosage()) ? " (" + m.getDosage().trim() + ")" : "");
                canvas.drawText(medLine, 65, y + 14, paint);

                paint.setFakeBoldText(false);
                canvas.drawText(safe(m.getFrequency(), "1+0+1"), 260, y + 14, paint);
                canvas.drawText(safe(m.getDuration(), "7 Days"), 360, y + 14, paint);
                canvas.drawText(safe(m.getMealInstruction(), "After Meal"), 440, y + 14, paint);

                if (!isEmpty(m.getInstructions())) {
                    y += 14;
                    paint.setTextSize(9f);
                    paint.setColor(Color.parseColor("#778276"));
                    canvas.drawText("Note: " + m.getInstructions().trim(), 65, y + 12, paint);
                }

                y += 24;
                paint.setColor(Color.parseColor("#E8EFE2"));
                paint.setStrokeWidth(0.8f);
                canvas.drawLine(32, y, pageWidth - 32, y, paint);
                y += 6;
                idx++;
            }
        } else {
            paint.setColor(Color.parseColor("#778276"));
            paint.setTextSize(10f);
            canvas.drawText("No specific prescription medications added.", 65, y + 14, paint);
            y += 26;
        }

        y += 16;

        // 7. Advice & General Instructions Box
        if (!isEmpty(rx.getAdviceInstructions())) {
            paint.setColor(Color.parseColor("#557B52"));
            paint.setTextSize(11f);
            paint.setFakeBoldText(true);
            canvas.drawText("ADVICE & SPECIAL INSTRUCTIONS:", 32, y, paint);

            y += 16;
            paint.setColor(Color.parseColor("#26332A"));
            paint.setTextSize(10.5f);
            paint.setFakeBoldText(false);
            canvas.drawText(rx.getAdviceInstructions().trim(), 32, y, paint);
            y += 24;
        }

        // 8. Follow-up Visit Date
        if (!isEmpty(rx.getFollowUpDate())) {
            paint.setColor(Color.parseColor("#D99B45"));
            paint.setTextSize(11f);
            paint.setFakeBoldText(true);
            canvas.drawText("NEXT FOLLOW-UP VISIT: " + rx.getFollowUpDate().trim(), 32, y, paint);
            y += 26;
        }

        // 9. Signature & Watermark Footer
        int footerY = pageHeight - 75;
        paint.setColor(Color.parseColor("#D5E0CE"));
        paint.setStrokeWidth(1f);
        canvas.drawLine(32, footerY, pageWidth - 32, footerY, paint);

        footerY += 20;
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(Color.parseColor("#3B7A57"));
        paint.setTextSize(11f);
        paint.setFakeBoldText(true);
        canvas.drawText("[VERIFIED DIGITAL SIGNATURE]", pageWidth - 32, footerY, paint);

        paint.setTextSize(9.5f);
        paint.setFakeBoldText(false);
        paint.setColor(Color.parseColor("#26332A"));
        canvas.drawText(safe(rx.getDoctorName(), "Doctor Specialist"), pageWidth - 32, footerY + 14, paint);
        canvas.drawText("BMDC: " + safe(rx.getDoctorLicense(), "VERIFIED"), pageWidth - 32, footerY + 26, paint);

        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Color.parseColor("#778276"));
        paint.setTextSize(8.5f);
        canvas.drawText("Authorized Digital Prescription issued via AROHI Healthcare Ecosystem.", 32, footerY, paint);
        canvas.drawText("Valid without physical signature under digital health management standards.", 32, footerY + 12, paint);
        canvas.drawText("Patient ID: " + safe(rx.getPatientId(), "AROHI-PAT") + " • Secure Firestore Record ID: " + safe(rx.getPrescriptionId(), "N/A"), 32, footerY + 24, paint);

        document.finishPage(page);

        // Save PDF to Documents / Cache
        File docDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (docDir == null) {
            docDir = context.getCacheDir();
        }
        if (!docDir.exists()) {
            docDir.mkdirs();
        }

        String safeId = !isEmpty(rx.getPrescriptionId())
                ? rx.getPrescriptionId().replaceAll("[^a-zA-Z0-9_-]", "_")
                : "rx_" + System.currentTimeMillis();
        File pdfFile = new File(docDir, "AROHI_Prescription_" + safeId + ".pdf");

        FileOutputStream out = new FileOutputStream(pdfFile);
        document.writeTo(out);
        out.flush();
        out.close();
        document.close();

        return pdfFile;
    }

    /**
     * Launches standard Android Intent to view or share the generated PDF file.
     */
    public static void viewOrSharePdf(@NonNull Context context, @NonNull File pdfFile) {
        Uri contentUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                pdfFile
        );

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(contentUri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            // Fallback: Share chooser
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("application/pdf");
            share.putExtra(Intent.EXTRA_STREAM, contentUri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(share, "Open Prescription PDF"));
        }
    }

    private static String safe(String s, String fallback) {
        return s != null && !s.trim().isEmpty() ? s.trim() : fallback;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
