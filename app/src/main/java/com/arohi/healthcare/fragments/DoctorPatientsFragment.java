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
import com.arohi.healthcare.databinding.FragmentDoctorPatientsBinding;
import com.arohi.healthcare.models.ConsultationRequest;
import com.arohi.healthcare.models.PatientProfile;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.DoctorDiscoveryAndRequestRepository;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;

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

                    @Override
                    public void onError(String message) {
                        if (binding == null) return;
                        showIncomingRequestDetailsDialog(req);
                    }
                }
        );
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
