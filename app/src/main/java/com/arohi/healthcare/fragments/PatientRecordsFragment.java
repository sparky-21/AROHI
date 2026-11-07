package com.arohi.healthcare.fragments;

import android.os.Bundle;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.adapters.DigitalPrescriptionAdapter;
import com.arohi.healthcare.adapters.LaboratoryRequestAdapter;
import com.arohi.healthcare.adapters.MedicalTimelineAdapter;
import com.arohi.healthcare.databinding.DialogPrescriptionViewBinding;
import com.arohi.healthcare.databinding.FragmentPatientRecordsBinding;
import com.arohi.healthcare.models.DigitalPrescription;
import com.arohi.healthcare.models.LaboratoryRequest;
import com.arohi.healthcare.models.MedicalRecordEntry;
import com.arohi.healthcare.models.PrescriptionMedicine;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.ClinicalRepository;
import com.arohi.healthcare.utils.PdfGenerator;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Phase 4 Digital Medical Records & Vault Fragment:
 * - Tab 1: Chronological Medical History Timeline (integrated consultations, prescriptions, lab reports, appointments)
 * - Tab 2: Digital Prescriptions (structured medicines, dosage schedules, advice, and A4 PDF export)
 * - Tab 3: Diagnostic Laboratory Reports (test requests, status progress, file uploads, and doctor review findings)
 */
public class PatientRecordsFragment extends Fragment {

    private static final String TAB_TIMELINE = "TIMELINE";
    private static final String TAB_PRESCRIPTIONS = "PRESCRIPTIONS";
    private static final String TAB_LAB_REPORTS = "LAB_REPORTS";

    private FragmentPatientRecordsBinding binding;
    private ClinicalRepository clinicalRepository;
    private SessionManager sessionManager;
    private UserAccount currentAccount;

    private String activeTab = TAB_TIMELINE;

    private MedicalTimelineAdapter timelineAdapter;
    private DigitalPrescriptionAdapter rxAdapter;
    private LaboratoryRequestAdapter labAdapter;

    private final List<MedicalRecordEntry> timelineList = new ArrayList<>();
    private final List<DigitalPrescription> rxList = new ArrayList<>();
    private final List<LaboratoryRequest> labList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPatientRecordsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        clinicalRepository = new ClinicalRepository(requireContext());
        sessionManager = new SessionManager(requireContext());
        currentAccount = sessionManager.getCurrentSessionAccount();

        setupAdapters();
        setupTabs();

        switchTab(TAB_TIMELINE);
    }

    private void setupAdapters() {
        binding.rvMedicalRecords.setLayoutManager(new LinearLayoutManager(requireContext()));

        // 1. Timeline Adapter
        timelineAdapter = new MedicalTimelineAdapter(entry -> {
            if (entry.getRawObject() instanceof DigitalPrescription) {
                showPrescriptionDetailsDialog((DigitalPrescription) entry.getRawObject());
            } else if (entry.getRawObject() instanceof LaboratoryRequest) {
                showLabReportDetailsDialog((LaboratoryRequest) entry.getRawObject());
            } else {
                showSimpleRecordInfoDialog(entry);
            }
        });

        // 2. Prescriptions Adapter
        rxAdapter = new DigitalPrescriptionAdapter(
                currentAccount != null && currentAccount.getUserRoleEnum() == UserRole.DOCTOR,
                new DigitalPrescriptionAdapter.OnPrescriptionActionListener() {
                    @Override
                    public void onViewPrescriptionDetails(@NonNull DigitalPrescription prescription) {
                        showPrescriptionDetailsDialog(prescription);
                    }

                    @Override
                    public void onExportPrescriptionPdf(@NonNull DigitalPrescription prescription) {
                        exportPrescriptionPdf(prescription);
                    }
                }
        );

        // 3. Laboratory Adapter
        labAdapter = new LaboratoryRequestAdapter(
                currentAccount != null && currentAccount.getUserRoleEnum() == UserRole.DOCTOR,
                new LaboratoryRequestAdapter.OnLabActionListener() {
                    @Override
                    public void onUploadReport(@NonNull LaboratoryRequest request) {
                        showUploadReportDialog(request);
                    }

                    @Override
                    public void onDoctorReviewReport(@NonNull LaboratoryRequest request) {
                        showDoctorReviewDialog(request);
                    }

                    @Override
                    public void onViewReportDetails(@NonNull LaboratoryRequest request) {
                        showLabReportDetailsDialog(request);
                    }
                }
        );
    }

    private void setupTabs() {
        binding.btnTabTimeline.setOnClickListener(v -> switchTab(TAB_TIMELINE));
        binding.btnTabPrescriptions.setOnClickListener(v -> switchTab(TAB_PRESCRIPTIONS));
        binding.btnTabLabReports.setOnClickListener(v -> switchTab(TAB_LAB_REPORTS));
    }

    private void switchTab(String tab) {
        this.activeTab = tab;

        com.google.android.material.button.MaterialButton selectedBtn =
                TAB_PRESCRIPTIONS.equals(tab) ? binding.btnTabPrescriptions
                : (TAB_LAB_REPORTS.equals(tab) ? binding.btnTabLabReports : binding.btnTabTimeline);

        UiStateHelper.updateToggleButtons(
                selectedBtn,
                R.color.white,
                binding.btnTabTimeline,
                binding.btnTabPrescriptions,
                binding.btnTabLabReports
        );

        if (TAB_TIMELINE.equals(tab)) {
            binding.rvMedicalRecords.setAdapter(timelineAdapter);
            loadTimeline();
        } else if (TAB_PRESCRIPTIONS.equals(tab)) {
            binding.rvMedicalRecords.setAdapter(rxAdapter);
            loadPrescriptions();
        } else if (TAB_LAB_REPORTS.equals(tab)) {
            binding.rvMedicalRecords.setAdapter(labAdapter);
            loadLabReports();
        }
    }

    // =========================================================================
    // DATA LOADERS
    // =========================================================================
    private void loadTimeline() {
        if (currentAccount == null) return;
        clinicalRepository.loadPatientMedicalRecord(currentAccount.getUid(), new AuthCallback<List<MedicalRecordEntry>>() {
            @Override
            public void onSuccess(List<MedicalRecordEntry> result) {
                if (binding == null) return;
                timelineList.clear();
                timelineList.addAll(result);
                timelineAdapter.submitList(timelineList);

                updateEmptyState(
                        timelineList.isEmpty(),
                        "No Medical Records Recorded",
                        "Prescriptions, diagnostic test orders, and consultation appointments will form your chronological health history here."
                );
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    private void loadPrescriptions() {
        if (currentAccount == null) return;
        clinicalRepository.loadPatientPrescriptions(currentAccount.getUid(), new AuthCallback<List<DigitalPrescription>>() {
            @Override
            public void onSuccess(List<DigitalPrescription> result) {
                if (binding == null) return;
                rxList.clear();
                rxList.addAll(result);
                rxAdapter.submitList(rxList);

                updateEmptyState(
                        rxList.isEmpty(),
                        "No Digital Prescriptions",
                        "Authorized digital prescriptions issued by your specialist doctors will appear here with printable PDF export."
                );
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    private void loadLabReports() {
        if (currentAccount == null) return;
        clinicalRepository.loadPatientLabRequests(currentAccount.getUid(), new AuthCallback<List<LaboratoryRequest>>() {
            @Override
            public void onSuccess(List<LaboratoryRequest> result) {
                if (binding == null) return;
                labList.clear();
                labList.addAll(result);
                labAdapter.submitList(labList);

                updateEmptyState(
                        labList.isEmpty(),
                        "No Diagnostic Lab Reports",
                        "Laboratory test orders, sample collection status, and diagnostic report uploads will appear here."
                );
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    private void updateEmptyState(boolean isEmpty, String title, String subtitle) {
        if (binding == null) return;
        if (isEmpty) {
            binding.layoutRecordsEmpty.setVisibility(View.VISIBLE);
            binding.rvMedicalRecords.setVisibility(View.GONE);
            binding.tvRecordsEmptyTitle.setText(title);
            binding.tvRecordsEmptySub.setText(subtitle);
        } else {
            binding.layoutRecordsEmpty.setVisibility(View.GONE);
            binding.rvMedicalRecords.setVisibility(View.VISIBLE);
        }
    }

    // =========================================================================
    // PRESCRIPTION VIEW & PDF EXPORT
    // =========================================================================
    private void showPrescriptionDetailsDialog(@NonNull DigitalPrescription rx) {
        DialogPrescriptionViewBinding dBinding = DialogPrescriptionViewBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dBinding.getRoot())
                .create();

        dBinding.tvViewRxDoctorName.setText("Dr. " + safe(rx.getDoctorName(), "Specialist"));
        dBinding.tvViewRxDoctorMeta.setText(safe(rx.getDoctorSpecialty(), "General Medicine")
                + " • BMDC: " + safe(rx.getDoctorLicense(), "VERIFIED")
                + (rx.getDoctorDepartment() != null ? " • Dept: " + rx.getDoctorDepartment() : ""));

        dBinding.tvViewRxPatientLine.setText("Patient: " + safe(rx.getPatientName(), "Patient")
                + " • ID: " + safe(rx.getPatientId(), "AROHI-PAT")
                + " • Blood: " + safe(rx.getPatientBloodGroup(), "N/A"));

        dBinding.tvViewRxVitalsLine.setText("Clinical Vitals: " + safe(rx.getClinicalObservations(), "Normal"));
        dBinding.tvViewRxDiagnosis.setText(safe(rx.getDiagnosis(), "General Consultation Assessment"));

        // Format medicines table
        List<PrescriptionMedicine> meds = rx.getMedicines();
        if (meds != null && !meds.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int idx = 1;
            for (PrescriptionMedicine m : meds) {
                if (m == null) continue;
                sb.append(idx).append(". ").append(m.getMedicineName());
                if (m.getDosage() != null && !m.getDosage().isEmpty()) {
                    sb.append(" (").append(m.getDosage()).append(")");
                }
                sb.append("\n   Frequency: ").append(safe(m.getFrequency(), "1+0+1"));
                sb.append(" • Duration: ").append(safe(m.getDuration(), "7 Days"));
                sb.append(" • ").append(safe(m.getMealInstruction(), "After Meal"));
                if (m.getInstructions() != null && !m.getInstructions().trim().isEmpty()) {
                    sb.append("\n   Instructions: ").append(m.getInstructions().trim());
                }
                sb.append("\n\n");
                idx++;
            }
            dBinding.tvViewRxMedicinesList.setText(sb.toString().trim());
        } else {
            dBinding.tvViewRxMedicinesList.setText("No specific medications prescribed.");
        }

        dBinding.tvViewRxAdvice.setText("Advice: " + safe(rx.getAdviceInstructions(), "Rest, warm fluids, follow instructions."));
        dBinding.tvViewRxFollowUp.setText(rx.getFollowUpDate() != null && !rx.getFollowUpDate().isEmpty()
                ? "Next Follow-up Visit: " + rx.getFollowUpDate()
                : "Follow-up: As clinically needed");

        dBinding.btnCloseViewRx.setOnClickListener(v -> dialog.dismiss());
        dBinding.btnDownloadRxPdf.setOnClickListener(v -> {
            dialog.dismiss();
            exportPrescriptionPdf(rx);
        });
        dBinding.btnOrderPrescriptionMedicines.setOnClickListener(v -> {
            dialog.dismiss();
            if (getActivity() instanceof com.arohi.healthcare.activities.RolePortalActivity) {
                ((com.arohi.healthcare.activities.RolePortalActivity) getActivity()).openSubFragment(
                        PharmacyFragment.newInstance(rx.getPrescriptionId()), "pharmacy_rx");
            }
        });

        dialog.show();
    }

    private void exportPrescriptionPdf(@NonNull DigitalPrescription rx) {
        try {
            File pdfFile = PdfGenerator.generatePrescriptionPdf(requireContext(), rx);
            UiStateHelper.showSuccess(binding.getRoot(), "Generated PDF: " + pdfFile.getName());
            PdfGenerator.viewOrSharePdf(requireContext(), pdfFile);
        } catch (Exception e) {
            UiStateHelper.showError(binding.getRoot(), "Failed to generate PDF: " + e.getMessage());
        }
    }

    // =========================================================================
    // LAB REPORT DIALOGS
    // =========================================================================
    private void showLabReportDetailsDialog(@NonNull LaboratoryRequest lab) {
        String info = "Test Name: " + safe(lab.getTestName(), "Diagnostic Test") + "\n"
                + "Category: " + safe(lab.getTestCategory(), "Clinical Lab") + "\n"
                + "Urgency: " + safe(lab.getUrgency(), "Routine") + "\n"
                + "Status: " + safe(lab.getStatus(), "REQUESTED") + "\n\n"
                + "Ordering Doctor: Dr. " + safe(lab.getDoctorName(), "Specialist") + "\n"
                + "Patient: " + safe(lab.getPatientName(), "Patient") + " (" + safe(lab.getPatientId(), "AROHI-PAT") + ")\n"
                + "Clinical Indication: " + safe(lab.getClinicalIndication(), "Diagnostic Assessment") + "\n\n"
                + "Findings Summary:\n" + safe(lab.getReportFindingsSummary(), "Test sample processing.") + "\n\n"
                + "Doctor Review Notes:\n" + safe(lab.getDoctorReviewNotes(), "Awaiting review.") + "\n"
                + (lab.getReportFileUrl() != null && !lab.getReportFileUrl().isEmpty()
                ? "Report Document: " + lab.getReportFileName()
                : "Report Document: Awaiting upload");

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle("Diagnostic Test & Report Details")
                .setMessage(info)
                .setNeutralButton("Close", null);

        if (lab.getReportFileUrl() != null && !lab.getReportFileUrl().isEmpty()) {
            builder.setPositiveButton("Open Report", (d, w) -> {
                File f = new File(lab.getReportFileUrl());
                if (f.exists()) {
                    PdfGenerator.viewOrSharePdf(requireContext(), f);
                } else {
                    UiStateHelper.showSuccess(binding.getRoot(), "Report record: " + lab.getReportFileName());
                }
            });
        } else {
            builder.setPositiveButton("Upload Findings", (d, w) -> showUploadReportDialog(lab));
        }

        builder.show();
    }

    private void showUploadReportDialog(@NonNull LaboratoryRequest lab) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad / 2, pad, 0);

        TextView tvHint = new TextView(requireContext());
        tvHint.setText("Upload or enter laboratory diagnostic findings for " + lab.getTestName() + ":");
        tvHint.setTextColor(getResources().getColor(R.color.text_primary));
        layout.addView(tvHint);

        EditText etSummary = new EditText(requireContext());
        etSummary.setHint("Findings summary (e.g. Hemoglobin: 13.8 g/dL, WBC: 7,500 /cumm, Platelets: 250,000 /cumm)");
        etSummary.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etSummary.setLines(3);
        layout.addView(etSummary);

        EditText etFileName = new EditText(requireContext());
        etFileName.setHint("Report file name (e.g. CBC_Report.pdf)");
        etFileName.setText("Report_" + lab.getTestName().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");
        layout.addView(etFileName);

        new AlertDialog.Builder(requireContext())
                .setTitle("Upload Diagnostic Report")
                .setView(layout)
                .setPositiveButton("Upload to Vault", (d, w) -> {
                    String summary = etSummary.getText() != null ? etSummary.getText().toString().trim() : "Findings uploaded.";
                    String fileName = etFileName.getText() != null ? etFileName.getText().toString().trim() : "Report.pdf";

                    byte[] mockFileBytes = ("AROHI DIAGNOSTIC REPORT: " + lab.getTestName() + "\nFindings: " + summary).getBytes();
                    clinicalRepository.uploadLaboratoryReport(lab, mockFileBytes, fileName, "PDF", summary, new AuthCallback<LaboratoryRequest>() {
                        @Override
                        public void onSuccess(LaboratoryRequest result) {
                            if (binding == null) return;
                            UiStateHelper.showSuccess(binding.getRoot(), "Report for " + lab.getTestName() + " uploaded successfully!");
                            loadLabReports();
                        }

                        @Override
                        public void onError(String message) {
                            if (binding == null) return;
                            UiStateHelper.showError(binding.getRoot(), message);
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDoctorReviewDialog(@NonNull LaboratoryRequest lab) {
        EditText etReview = new EditText(requireContext());
        etReview.setHint("Clinical findings review (e.g. Results normal, continue antibiotics)");
        etReview.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etReview.setLines(3);

        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, pad / 2, pad, 0);
        layout.addView(etReview);

        new AlertDialog.Builder(requireContext())
                .setTitle("Doctor Clinical Report Review")
                .setMessage("Add your professional review notes for " + lab.getTestName() + " (" + lab.getPatientName() + "):")
                .setView(layout)
                .setPositiveButton("Confirm Review", (d, w) -> {
                    String reviewNotes = etReview.getText() != null ? etReview.getText().toString().trim() : "";
                    if (reviewNotes.isEmpty()) {
                        UiStateHelper.showError(binding.getRoot(), "Please enter review comments.");
                        return;
                    }

                    clinicalRepository.reviewLaboratoryReport(lab, reviewNotes, new AuthCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            if (binding == null) return;
                            UiStateHelper.showSuccess(binding.getRoot(), "Report reviewed successfully!");
                            loadLabReports();
                        }

                        @Override
                        public void onError(String message) {
                            if (binding == null) return;
                            UiStateHelper.showError(binding.getRoot(), message);
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSimpleRecordInfoDialog(@NonNull MedicalRecordEntry entry) {
        String info = "Title: " + entry.getTitle() + "\n"
                + "Date: " + entry.getDateString() + "\n"
                + "Provider: " + entry.getSubtitle() + "\n"
                + "Status: " + entry.getStatus() + "\n"
                + "Summary: " + entry.getSummary();

        new AlertDialog.Builder(requireContext())
                .setTitle("Medical Record Entry")
                .setMessage(info)
                .setPositiveButton("Close", null)
                .show();
    }

    private String safe(String s, String fallback) {
        return s != null && !s.trim().isEmpty() ? s.trim() : fallback;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            switchTab(activeTab);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        switchTab(activeTab);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
