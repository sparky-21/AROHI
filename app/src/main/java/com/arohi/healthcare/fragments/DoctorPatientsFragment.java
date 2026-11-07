package com.arohi.healthcare.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.activities.SecureChatActivity;
import com.arohi.healthcare.adapters.ConsultationRequestAdapter;
import com.arohi.healthcare.databinding.DialogBookAppointmentBinding;
import com.arohi.healthcare.databinding.DialogCreateLabRequestBinding;
import com.arohi.healthcare.databinding.DialogCreatePrescriptionBinding;
import com.arohi.healthcare.databinding.FragmentDoctorPatientsBinding;
import com.arohi.healthcare.models.Appointment;
import com.arohi.healthcare.models.ConsultationRequest;
import com.arohi.healthcare.models.DigitalPrescription;
import com.arohi.healthcare.models.DoctorProfile;
import com.arohi.healthcare.models.LaboratoryRequest;
import com.arohi.healthcare.models.MedicalRecordEntry;
import com.arohi.healthcare.models.PatientProfile;
import com.arohi.healthcare.models.PrescriptionMedicine;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.ClinicalRepository;
import com.arohi.healthcare.repositories.DoctorDiscoveryAndRequestRepository;
import com.arohi.healthcare.utils.DatePickerHelper;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Phase 3 Doctor Patients & Consultation Requests Fragment:
 * - View incoming patient consultation requests
 * - Inspect patient request details
 * - Accept or Decline requests (gated by Doctor VERIFIED status)
 * - View accepted patients and inspect their full connected Patient Health Profile
 * - Launch 1-to-1 Secure Chat with accepted patients
 */
public class DoctorPatientsFragment extends Fragment {

    private FragmentDoctorPatientsBinding binding;
    private DoctorDiscoveryAndRequestRepository discoveryRepository;
    private ClinicalRepository clinicalRepository;
    private SessionManager sessionManager;
    private ConsultationRequestAdapter adapter;

    private final List<ConsultationRequest> allRequests = new ArrayList<>();
    private boolean showAcceptedOnly = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDoctorPatientsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        discoveryRepository = new DoctorDiscoveryAndRequestRepository(requireContext());
        clinicalRepository = new ClinicalRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        UserAccount account = sessionManager.getCurrentSessionAccount();
        if (account != null) {
            if (account.getVerificationStatusEnum() == VerificationStatus.VERIFIED) {
                binding.bannerDocPatientsVerification.setVisibility(View.GONE);
            } else {
                binding.bannerDocPatientsVerification.setVisibility(View.VISIBLE);
            }
        }

        adapter = new ConsultationRequestAdapter(true, new ConsultationRequestAdapter.OnConsultationRequestListener() {
            @Override
            public void onInspectDetails(@NonNull ConsultationRequest request) {
                if (request.isAccepted()) {
                    inspectConnectedPatientProfile(request);
                } else {
                    showIncomingRequestDetailsDialog(request);
                }
            }

            @Override
            public void onPrimaryAction(@NonNull ConsultationRequest request) {
                if (request.isPending()) {
                    showAcceptOrDeclineDialog(request, true);
                } else if (request.isAccepted()) {
                    openSecureChatWithPatient(request);
                }
            }

            @Override
            public void onDeclineOrCancel(@NonNull ConsultationRequest request) {
                if (request.isPending()) {
                    showAcceptOrDeclineDialog(request, false);
                }
            }
        });

        binding.rvDoctorRequestsAndPatients.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvDoctorRequestsAndPatients.setAdapter(adapter);

        binding.btnDocTabIncomingRequests.setOnClickListener(v -> {
            showAcceptedOnly = false;
            applyModeFilter();
        });

        binding.btnDocTabAcceptedPatients.setOnClickListener(v -> {
            showAcceptedOnly = true;
            applyModeFilter();
        });

        loadRequests();
    }

    private void loadRequests() {
        UserAccount account = sessionManager.getCurrentSessionAccount();
        if (account == null) return;

        discoveryRepository.loadDoctorRequests(account.getUid(), new AuthCallback<List<ConsultationRequest>>() {
            @Override
            public void onSuccess(List<ConsultationRequest> result) {
                if (binding == null) return;
                allRequests.clear();
                allRequests.addAll(result);
                applyModeFilter();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    private void applyModeFilter() {
        if (binding == null) return;
        UiStateHelper.updateToggleButtons(
                showAcceptedOnly ? binding.btnDocTabAcceptedPatients : binding.btnDocTabIncomingRequests,
                R.color.pale_mint,
                binding.btnDocTabIncomingRequests,
                binding.btnDocTabAcceptedPatients
        );
        List<ConsultationRequest> filtered = new ArrayList<>();
        for (ConsultationRequest r : allRequests) {
            if (showAcceptedOnly) {
                if (r.isAccepted()) {
                    filtered.add(r);
                }
            } else {
                filtered.add(r);
            }
        }

        adapter.submitList(filtered);
        if (showAcceptedOnly) {
            binding.tvDocPatientsSummary.setText(
                    "Showing " + filtered.size() + " Accepted Patient Connection" + (filtered.size() == 1 ? "" : "s")
                            + " • Tap 'Patient Details' for full Health Profile or 'Open Secure Chat'");
            binding.tvEmptyDocPatients.setText(
                    "No accepted patients yet. Accept a Pending consultation request in 'Incoming Requests' to connect.");
        } else {
            binding.tvDocPatientsSummary.setText(
                    "Showing " + filtered.size() + " Patient Consultation Request" + (filtered.size() == 1 ? "" : "s"));
            binding.tvEmptyDocPatients.setText(
                    "No incoming consultation requests yet. Requests sent by patients appear here in real time.");
        }

        if (filtered.isEmpty()) {
            binding.tvEmptyDocPatients.setVisibility(View.VISIBLE);
            binding.rvDoctorRequestsAndPatients.setVisibility(View.GONE);
        } else {
            binding.tvEmptyDocPatients.setVisibility(View.GONE);
            binding.rvDoctorRequestsAndPatients.setVisibility(View.VISIBLE);
        }
    }

    private void showIncomingRequestDetailsDialog(@NonNull ConsultationRequest req) {
        String timeStr = req.getCreatedAt() > 0
                ? DateFormat.format("yyyy/MM/dd hh:mm a", new Date(req.getCreatedAt())).toString()
                : "N/A";
        String info = "Patient Name: " + notEmpty(req.getPatientName(), "Patient") + "\n"
                + "Patient AROHI ID: " + notEmpty(req.getPatientId(), "AROHI-PAT") + "\n"
                + "Gender: " + notEmpty(req.getPatientGender(), "Not Specified") + "\n"
                + "Date of Birth: " + notEmpty(req.getPatientDob(), "Not Specified") + "\n"
                + "Blood Group: " + notEmpty(req.getPatientBloodGroup(), "Not Specified") + "\n"
                + "Known Allergies: " + notEmpty(req.getPatientAllergies(), "None") + "\n"
                + "Mobile: " + notEmpty(req.getPatientPhone(), "N/A") + "\n\n"
                + "Consultation Reason: " + notEmpty(req.getConsultationReason(), "General Consultation") + "\n"
                + "Patient Note: " + notEmpty(req.getPatientNote(), "No additional note") + "\n"
                + "Current Status: " + notEmpty(req.getStatus(), "Pending") + "\n"
                + "Submitted At: " + timeStr;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle("Patient Consultation Request")
                .setMessage(info)
                .setNeutralButton("Close", null);

        if (req.isPending()) {
            builder.setPositiveButton("Accept Request", (d, w) -> showAcceptOrDeclineDialog(req, true));
            builder.setNegativeButton("Decline", (d, w) -> showAcceptOrDeclineDialog(req, false));
        } else if (req.isAccepted()) {
            builder.setPositiveButton("Open Secure Chat", (d, w) -> openSecureChatWithPatient(req));
        }
        builder.show();
    }

    private void inspectConnectedPatientProfile(@NonNull ConsultationRequest req) {
        UserAccount account = sessionManager.getCurrentSessionAccount();
        if (account == null) return;

        discoveryRepository.loadConnectedPatientProfile(
                account.getUid(),
                req.getPatientUid(),
                new AuthCallback<PatientProfile>() {
                    @Override
                    public void onSuccess(PatientProfile p) {
                        if (binding == null) return;
                        String[] clinicalActions = new String[]{
                                "✍  Issue Digital Prescription (Rx)",
                                "🔬  Order Diagnostic Laboratory Test",
                                "📅  Schedule Consultation Appointment",
                                "📋  View Patient Medical Record Timeline",
                                "💬  Open 1-to-1 Secure Chat",
                                "👤  Inspect Full Health Demographics"
                        };

                        new AlertDialog.Builder(requireContext())
                                .setTitle("Patient: " + notEmpty(p.getFullName(), req.getPatientName()))
                                .setItems(clinicalActions, (dialog, which) -> {
                                    switch (which) {
                                        case 0: // Issue Prescription
                                            showCreatePrescriptionDialog(req, p);
                                            break;
                                        case 1: // Order Lab Test
                                            showCreateLabRequestDialog(req, p);
                                            break;
                                        case 2: // Schedule Appointment
                                            showScheduleAppointmentDialog(req, p);
                                            break;
                                        case 3: // View Medical Record
                                            showPatientMedicalRecordDialog(req, p);
                                            break;
                                        case 4: // Secure Chat
                                            openSecureChatWithPatient(req);
                                            break;
                                        case 5: // Full Demographics
                                        default:
                                            showFullDemographicsDialog(req, p);
                                            break;
                                    }
                                })
                                .setNegativeButton("Close", null)
                                .show();
                    }

                    @Override
                    public void onError(String message) {
                        if (binding == null) return;
                        showIncomingRequestDetailsDialog(req);
                    }
                }
        );
    }

    private void showFullDemographicsDialog(@NonNull ConsultationRequest req, @NonNull PatientProfile p) {
        String info = "Patient Full Name: " + notEmpty(p.getFullName(), req.getPatientName()) + "\n"
                + "AROHI Patient ID: " + notEmpty(p.getArohiId(), req.getPatientId()) + "\n"
                + "Gender: " + notEmpty(p.getGender(), "N/A") + "\n"
                + "Date of Birth: " + notEmpty(p.getDateOfBirth(), "N/A") + "\n"
                + "Blood Group: " + notEmpty(p.getBloodGroup(), "N/A") + "\n"
                + "Known Allergies: " + notEmpty(p.getKnownAllergies(), "None") + "\n"
                + "Chronic Conditions: " + notEmpty(p.getChronicConditions(), "None") + "\n"
                + "Mobile Number: " + notEmpty(p.getPhone(), "N/A") + "\n"
                + "Emergency Contact: " + notEmpty(p.getEmergencyContactName(), "N/A")
                + " (" + notEmpty(p.getEmergencyContactPhone(), "N/A") + ")\n"
                + "Address: " + notEmpty(p.getAddress(), "N/A") + "\n"
                + "Health Notes: " + notEmpty(p.getBioNotes(), "None") + "\n\n"
                + "Consultation Reason: " + notEmpty(req.getConsultationReason(), "Consultation");

        new AlertDialog.Builder(requireContext())
                .setTitle("Connected Patient Profile")
                .setMessage(info)
                .setPositiveButton("Open Secure Chat", (d, w) -> openSecureChatWithPatient(req))
                .setNegativeButton("Close", null)
                .show();
    }

    private void showCreatePrescriptionDialog(@NonNull ConsultationRequest req, @NonNull PatientProfile p) {
        UserAccount currentDoctor = sessionManager.getCurrentSessionAccount();
        if (currentDoctor == null) return;

        DialogCreatePrescriptionBinding dBinding = DialogCreatePrescriptionBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dBinding.getRoot())
                .create();

        dBinding.tvPrescriptionPatientInfo.setText("Patient: " + notEmpty(p.getFullName(), req.getPatientName())
                + " • ID: " + notEmpty(p.getArohiId(), req.getPatientId())
                + " • Blood: " + notEmpty(p.getBloodGroup(), req.getPatientBloodGroup()));

        // Pre-fill initial medicine row
        addMedicineRowToContainer(dBinding.layoutMedicinesContainer, "Amoxicillin", "500 mg", "1+1+1", "7 Days", "After Meal");

        dBinding.btnAddMedicineRow.setOnClickListener(v -> {
            addMedicineRowToContainer(dBinding.layoutMedicinesContainer, "", "", "1+0+1", "5 Days", "After Meal");
        });

        dBinding.etRxFollowUpDate.setOnClickListener(v -> {
            DatePickerHelper.showDatePicker(requireContext(), (y, m, d) -> {
                dBinding.etRxFollowUpDate.setText(String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, d));
            });
        });

        dBinding.btnCancelCreateRx.setOnClickListener(v -> dialog.dismiss());

        dBinding.btnSubmitCreateRx.setOnClickListener(v -> {
            String diagnosis = dBinding.etRxDiagnosis.getText() != null
                    ? dBinding.etRxDiagnosis.getText().toString().trim() : "";
            if (diagnosis.isEmpty()) {
                UiStateHelper.showError(dBinding.getRoot(), "Please enter the clinical diagnosis.");
                return;
            }

            String vitals = dBinding.etRxVitals.getText() != null
                    ? dBinding.etRxVitals.getText().toString().trim() : "Normal";
            String advice = dBinding.etRxAdvice.getText() != null
                    ? dBinding.etRxAdvice.getText().toString().trim() : "Rest, drink plenty of warm fluids.";
            String followUp = dBinding.etRxFollowUpDate.getText() != null
                    ? dBinding.etRxFollowUpDate.getText().toString().trim() : "";

            // Collect medicines
            List<PrescriptionMedicine> medicines = collectMedicinesFromContainer(dBinding.layoutMedicinesContainer);

            DigitalPrescription rx = new DigitalPrescription();
            rx.setPatientUid(req.getPatientUid());
            rx.setPatientId(notEmpty(p.getArohiId(), req.getPatientId()));
            rx.setPatientName(notEmpty(p.getFullName(), req.getPatientName()));
            rx.setPatientAgeOrDob(notEmpty(p.getDateOfBirth(), "N/A"));
            rx.setPatientGender(notEmpty(p.getGender(), "N/A"));
            rx.setPatientBloodGroup(notEmpty(p.getBloodGroup(), req.getPatientBloodGroup()));

            rx.setDoctorUid(currentDoctor.getUid());
            rx.setDoctorId(currentDoctor.getArohiId());
            rx.setDoctorName(currentDoctor.getFullName());
            rx.setDoctorSpecialty(notEmpty(req.getDoctorSpecialty(), "General Medicine"));
            rx.setDoctorDepartment(notEmpty(req.getDoctorDepartment(), "Medicine"));

            rx.setDiagnosis(diagnosis);
            rx.setClinicalObservations(vitals);
            rx.setMedicines(medicines);
            rx.setAdviceInstructions(advice);
            rx.setFollowUpDate(followUp);

            dBinding.btnSubmitCreateRx.setEnabled(false);
            clinicalRepository.createPrescription(rx, new AuthCallback<DigitalPrescription>() {
                @Override
                public void onSuccess(DigitalPrescription result) {
                    dialog.dismiss();
                    if (binding != null) {
                        UiStateHelper.showSuccess(binding.getRoot(),
                                "Digital prescription issued for " + result.getPatientName() + "!");
                    }
                }

                @Override
                public void onError(String message) {
                    dBinding.btnSubmitCreateRx.setEnabled(true);
                    UiStateHelper.showError(dBinding.getRoot(), message);
                }
            });
        });

        dialog.show();
    }

    private void addMedicineRowToContainer(LinearLayout container, String name, String dosage,
                                           String freq, String dur, String meal) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundResource(R.drawable.bg_white_pill);
        int pad = (int) (8 * getResources().getDisplayMetrics().density);
        row.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));
        row.setLayoutParams(lp);

        EditText etName = new EditText(requireContext());
        etName.setHint("Medicine Name (e.g. Paracetamol)");
        etName.setText(name);
        etName.setTextSize(12f);
        row.addView(etName);

        LinearLayout rowDetails = new LinearLayout(requireContext());
        rowDetails.setOrientation(LinearLayout.HORIZONTAL);

        EditText etDosage = new EditText(requireContext());
        etDosage.setHint("Dosage (e.g. 500 mg)");
        etDosage.setText(dosage);
        etDosage.setTextSize(11f);
        etDosage.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        rowDetails.addView(etDosage);

        EditText etFreq = new EditText(requireContext());
        etFreq.setHint("Frequency (1+0+1)");
        etFreq.setText(freq);
        etFreq.setTextSize(11f);
        etFreq.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        rowDetails.addView(etFreq);

        EditText etDur = new EditText(requireContext());
        etDur.setHint("Duration (7 Days)");
        etDur.setText(dur);
        etDur.setTextSize(11f);
        etDur.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        rowDetails.addView(etDur);

        row.addView(rowDetails);
        container.addView(row);
    }

    private List<PrescriptionMedicine> collectMedicinesFromContainer(LinearLayout container) {
        List<PrescriptionMedicine> list = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) child;
                if (row.getChildCount() >= 2) {
                    EditText etName = (EditText) row.getChildAt(0);
                    LinearLayout sub = (LinearLayout) row.getChildAt(1);
                    EditText etDosage = (EditText) sub.getChildAt(0);
                    EditText etFreq = (EditText) sub.getChildAt(1);
                    EditText etDur = (EditText) sub.getChildAt(2);

                    String name = etName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        list.add(new PrescriptionMedicine(
                                name,
                                etDosage.getText().toString().trim(),
                                etFreq.getText().toString().trim(),
                                etDur.getText().toString().trim(),
                                "After Meal",
                                ""
                        ));
                    }
                }
            }
        }
        return list;
    }

    private void showCreateLabRequestDialog(@NonNull ConsultationRequest req, @NonNull PatientProfile p) {
        UserAccount currentDoctor = sessionManager.getCurrentSessionAccount();
        if (currentDoctor == null) return;

        DialogCreateLabRequestBinding dBinding = DialogCreateLabRequestBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dBinding.getRoot())
                .create();

        dBinding.tvLabPatientHeader.setText("Patient: " + notEmpty(p.getFullName(), req.getPatientName())
                + " • ID: " + notEmpty(p.getArohiId(), req.getPatientId()));

        String[] testOptions = new String[]{
                "Complete Blood Count (CBC)",
                "Liver Function Test (LFT)",
                "Lipid Profile",
                "Serum Creatinine",
                "Fasting Blood Sugar (FBS)",
                "Chest X-Ray (PA View)",
                "12-Lead Electrocardiogram (ECG)",
                "Urine Routine Examination (R/M/E)",
                "Serum Electrolytes",
                "Thyroid Stimulating Hormone (TSH)"
        };
        ArrayAdapter<String> testAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, testOptions);
        dBinding.actvTestName.setAdapter(testAdapter);
        dBinding.actvTestName.setText(testOptions[0], false);

        String[] categories = new String[]{"Hematology", "Biochemistry", "Radiology", "Cardiology", "Microbiology", "Pathology"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, categories);
        dBinding.actvTestCategory.setAdapter(catAdapter);
        dBinding.actvTestCategory.setText(categories[0], false);

        String[] urgencies = new String[]{LaboratoryRequest.URGENCY_ROUTINE, LaboratoryRequest.URGENCY_URGENT, LaboratoryRequest.URGENCY_EMERGENCY};
        ArrayAdapter<String> urgAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, urgencies);
        dBinding.actvLabUrgency.setAdapter(urgAdapter);
        dBinding.actvLabUrgency.setText(urgencies[0], false);

        dBinding.btnCancelCreateLab.setOnClickListener(v -> dialog.dismiss());

        dBinding.btnSubmitCreateLab.setOnClickListener(v -> {
            String testName = dBinding.actvTestName.getText().toString().trim();
            String category = dBinding.actvTestCategory.getText().toString().trim();
            String indication = dBinding.etLabIndication.getText() != null
                    ? dBinding.etLabIndication.getText().toString().trim() : "";
            String urgency = dBinding.actvLabUrgency.getText().toString().trim();

            if (testName.isEmpty()) {
                UiStateHelper.showError(dBinding.getRoot(), "Please enter or select a diagnostic test name.");
                return;
            }

            LaboratoryRequest labReq = new LaboratoryRequest();
            labReq.setPatientUid(req.getPatientUid());
            labReq.setPatientId(notEmpty(p.getArohiId(), req.getPatientId()));
            labReq.setPatientName(notEmpty(p.getFullName(), req.getPatientName()));
            labReq.setPatientPhone(notEmpty(p.getPhone(), req.getPatientPhone()));
            labReq.setPatientBloodGroup(notEmpty(p.getBloodGroup(), req.getPatientBloodGroup()));

            labReq.setDoctorUid(currentDoctor.getUid());
            labReq.setDoctorId(currentDoctor.getArohiId());
            labReq.setDoctorName(currentDoctor.getFullName());
            labReq.setDoctorSpecialty(notEmpty(req.getDoctorSpecialty(), "General Medicine"));

            labReq.setTestName(testName);
            labReq.setTestCategory(category.isEmpty() ? "Clinical Laboratory" : category);
            labReq.setClinicalIndication(indication.isEmpty() ? "Diagnostic Assessment" : indication);
            labReq.setUrgency(urgency.isEmpty() ? LaboratoryRequest.URGENCY_ROUTINE : urgency);

            dBinding.btnSubmitCreateLab.setEnabled(false);
            clinicalRepository.createLaboratoryRequest(labReq, new AuthCallback<LaboratoryRequest>() {
                @Override
                public void onSuccess(LaboratoryRequest result) {
                    dialog.dismiss();
                    if (binding != null) {
                        UiStateHelper.showSuccess(binding.getRoot(),
                                "Diagnostic test " + result.getTestName() + " ordered successfully!");
                    }
                }

                @Override
                public void onError(String message) {
                    dBinding.btnSubmitCreateLab.setEnabled(true);
                    UiStateHelper.showError(dBinding.getRoot(), message);
                }
            });
        });

        dialog.show();
    }

    private void showScheduleAppointmentDialog(@NonNull ConsultationRequest req, @NonNull PatientProfile p) {
        UserAccount currentDoctor = sessionManager.getCurrentSessionAccount();
        if (currentDoctor == null) return;

        DialogBookAppointmentBinding dBinding = DialogBookAppointmentBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dBinding.getRoot())
                .create();

        dBinding.tvBookApptDialogTitle.setText("Schedule Consultation with " + notEmpty(p.getFullName(), req.getPatientName()));
        dBinding.tvBookApptDoctorSummary.setText("Patient ID: " + notEmpty(p.getArohiId(), req.getPatientId())
                + " • Doctor: Dr. " + currentDoctor.getFullName());
        dBinding.tilSelectDoctor.setVisibility(View.GONE);

        String[] timeSlots = new String[]{
                "09:00 AM", "09:30 AM", "10:00 AM", "10:30 AM", "11:00 AM", "11:30 AM",
                "04:00 PM", "04:30 PM", "05:00 PM", "05:30 PM", "06:00 PM", "06:30 PM",
                "07:00 PM", "07:30 PM", "08:00 PM"
        };
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, timeSlots);
        dBinding.actvAppointmentTime.setAdapter(timeAdapter);
        dBinding.actvAppointmentTime.setText(timeSlots[2], false);

        String[] types = new String[]{
                Appointment.TYPE_IN_PERSON,
                Appointment.TYPE_VIDEO_CALL,
                Appointment.TYPE_FOLLOW_UP
        };
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, types);
        dBinding.actvConsultationType.setAdapter(typeAdapter);
        dBinding.actvConsultationType.setText(types[0], false);

        String today = DateFormat.format("yyyy-MM-dd", new Date()).toString();
        dBinding.etAppointmentDate.setText(today);
        dBinding.etAppointmentDate.setOnClickListener(v -> {
            DatePickerHelper.showDatePicker(requireContext(), (y, m, d) -> {
                dBinding.etAppointmentDate.setText(String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, d));
            });
        });

        dBinding.btnCancelBookAppt.setOnClickListener(v -> dialog.dismiss());

        dBinding.btnSubmitBookAppt.setOnClickListener(v -> {
            String date = dBinding.etAppointmentDate.getText().toString().trim();
            String time = dBinding.actvAppointmentTime.getText().toString().trim();
            String type = dBinding.actvConsultationType.getText().toString().trim();
            String reason = dBinding.etAppointmentReason.getText() != null
                    ? dBinding.etAppointmentReason.getText().toString().trim() : "Scheduled Clinical Consultation";
            String notes = dBinding.etAppointmentNotes.getText() != null
                    ? dBinding.etAppointmentNotes.getText().toString().trim() : "";

            Appointment newAppt = new Appointment(
                    "",
                    req.getPatientUid(),
                    notEmpty(p.getArohiId(), req.getPatientId()),
                    notEmpty(p.getFullName(), req.getPatientName()),
                    notEmpty(p.getPhone(), req.getPatientPhone()),
                    currentDoctor.getUid(),
                    currentDoctor.getArohiId(),
                    currentDoctor.getFullName(),
                    notEmpty(req.getDoctorSpecialty(), "General Medicine"),
                    req.getConsultationFee(),
                    date,
                    time,
                    type,
                    reason,
                    notes
            );
            newAppt.setStatus(Appointment.STATUS_CONFIRMED); // Pre-confirmed when scheduled by doctor

            dBinding.btnSubmitBookAppt.setEnabled(false);
            clinicalRepository.requestAppointment(newAppt, new AuthCallback<Appointment>() {
                @Override
                public void onSuccess(Appointment result) {
                    dialog.dismiss();
                    if (binding != null) {
                        UiStateHelper.showSuccess(binding.getRoot(),
                                "Appointment confirmed for " + result.getPatientName() + " on " + date + " at " + time + "!");
                    }
                }

                @Override
                public void onError(String message) {
                    dBinding.btnSubmitBookAppt.setEnabled(true);
                    UiStateHelper.showError(dBinding.getRoot(), message);
                }
            });
        });

        dialog.show();
    }

    private void showPatientMedicalRecordDialog(@NonNull ConsultationRequest req, @NonNull PatientProfile p) {
        clinicalRepository.loadPatientMedicalRecord(req.getPatientUid(), new AuthCallback<List<MedicalRecordEntry>>() {
            @Override
            public void onSuccess(List<MedicalRecordEntry> entries) {
                if (binding == null) return;
                if (entries.isEmpty()) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Medical History: " + notEmpty(p.getFullName(), req.getPatientName()))
                            .setMessage("No historical prescriptions, laboratory reports, or appointments recorded yet for this patient.")
                            .setPositiveButton("Close", null)
                            .show();
                    return;
                }

                StringBuilder history = new StringBuilder();
                for (MedicalRecordEntry e : entries) {
                    history.append("• [").append(e.getDateString()).append("] ").append(e.getTitle()).append("\n");
                    history.append("   ").append(e.getSubtitle()).append("\n");
                    history.append("   Summary: ").append(e.getSummary()).append("\n\n");
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("Medical Record: " + notEmpty(p.getFullName(), req.getPatientName()))
                        .setMessage(history.toString().trim())
                        .setPositiveButton("Close", null)
                        .show();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    private void showAcceptOrDeclineDialog(@NonNull ConsultationRequest req, boolean accept) {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);

        EditText etNote = new EditText(requireContext());
        etNote.setHint(accept
                ? "Welcome note or consultation slot instruction (optional)"
                : "Brief reason for declining (optional)");
        etNote.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        container.addView(etNote);

        String actionTitle = accept ? "Accept Consultation Request" : "Decline Consultation Request";
        String actionSummary = accept
                ? "Accepting " + req.getPatientName() + "'s request will establish an authorized Doctor-Patient connection and unlock 1-to-1 secure messaging."
                : "Declining " + req.getPatientName() + "'s request will mark it as Declined. Messaging will remain locked.";

        new AlertDialog.Builder(requireContext())
                .setTitle(actionTitle)
                .setMessage(actionSummary)
                .setView(container)
                .setPositiveButton(accept ? "Confirm Accept" : "Confirm Decline", (d, w) -> {
                    String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";
                    discoveryRepository.respondToConsultationRequest(req, accept, note, new AuthCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            if (binding == null) return;
                            UiStateHelper.showSuccess(binding.getRoot(),
                                    accept
                                            ? "Accepted " + req.getPatientName() + "! Secure chat is now unlocked."
                                            : "Declined request from " + req.getPatientName() + ".");
                            loadRequests();
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

    private void openSecureChatWithPatient(@NonNull ConsultationRequest req) {
        Intent intent = SecureChatActivity.createIntent(
                requireContext(),
                req.getRequestId(),
                req.getPatientUid(),
                notEmpty(req.getPatientName(), "Patient"),
                "Patient ID: " + notEmpty(req.getPatientId(), "AROHI-PAT")
                        + " • Blood: " + notEmpty(req.getPatientBloodGroup(), "N/A")
        );
        startActivity(intent);
    }

    private String notEmpty(String val, String fallback) {
        return val != null && !val.trim().isEmpty() ? val.trim() : fallback;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            loadRequests();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRequests();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
