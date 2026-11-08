package com.arohi.healthcare.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.activities.SecureChatActivity;
import com.arohi.healthcare.adapters.ConsultationRequestAdapter;
import com.arohi.healthcare.adapters.ConversationThreadAdapter;
import com.arohi.healthcare.adapters.DoctorDiscoveryAdapter;
import com.arohi.healthcare.databinding.FragmentPatientDoctorsBinding;
import com.arohi.healthcare.models.AppNotification;
import com.arohi.healthcare.models.ConsultationRequest;
import com.arohi.healthcare.models.ConversationThread;
import com.arohi.healthcare.models.DoctorProfile;
import com.arohi.healthcare.models.PatientProfile;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.DoctorDiscoveryAndRequestRepository;
import com.arohi.healthcare.repositories.ProfileRepository;
import com.arohi.healthcare.repositories.SecureMessagingRepository;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Phase 3 Patient Doctor Discovery, Consultation Requests & Secure Messaging Hub:
 * - Search verified doctors by name, qualification, or department
 * - Filter by specialty
 * - Inspect full doctor profile (qualifications, experience, availability, fee)
 * - Select consultation reason & submit consultation request (preventing duplicates)
 * - View pending/accepted/declined/cancelled requests & cancel eligible Pending requests
 * - Access 1-to-1 secure chat for Accepted connections & view privacy-safe notifications
 */
public class PatientDoctorsFragment extends Fragment {

    private static final String[] CONSULTATION_REASONS = new String[]{
            "New Symptom Consultation",
            "Follow-up Consultation",
            "Lab Report & Diagnostic Review",
            "Chronic Condition Management",
            "Prescription Renewal / Dosage Inquiry",
            "Second Medical Opinion"
    };

    private FragmentPatientDoctorsBinding binding;
    private DoctorDiscoveryAndRequestRepository discoveryRepository;
    private ProfileRepository profileRepository;
    private SecureMessagingRepository messagingRepository;
    private SessionManager sessionManager;

    private DoctorDiscoveryAdapter doctorAdapter;
    private ConsultationRequestAdapter requestAdapter;
    private ConversationThreadAdapter conversationAdapter;

    private final List<DoctorProfile> allVerifiedDoctors = new ArrayList<>();
    private final List<ConsultationRequest> allPatientRequests = new ArrayList<>();
    private final Map<String, ConsultationRequest> requestMapByDoctorUid = new HashMap<>();

    private PatientProfile currentPatientProfile;
    private String selectedSpecialtyFilter = "ALL";
    private String selectedRequestFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPatientDoctorsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        discoveryRepository = new DoctorDiscoveryAndRequestRepository(requireContext());
        profileRepository = new ProfileRepository(requireContext());
        messagingRepository = new SecureMessagingRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        setupAdapters();
        setupModeSwitcherAndFilters();
        loadPatientProfileAndData();
    }

    private void setupAdapters() {
        doctorAdapter = new DoctorDiscoveryAdapter(new DoctorDiscoveryAdapter.OnDoctorActionListener() {
            @Override
            public void onViewDoctorProfile(@NonNull DoctorProfile doctor,
                                            @Nullable ConsultationRequest existingRequest) {
                showDoctorProfileDialog(doctor, existingRequest);
            }

            @Override
            public void onRequestOrChatClicked(@NonNull DoctorProfile doctor,
                                               @Nullable ConsultationRequest existingRequest) {
                if (existingRequest != null && existingRequest.isAccepted()) {
                    openChatWithDoctor(existingRequest.getRequestId(),
                            doctor.getUid(),
                            doctor.getFullName(),
                            "Verified Connection • " + notEmpty(doctor.getSpecialty(), "Medicine"));
                } else {
                    showSendRequestDialog(doctor);
                }
            }
        });
        binding.rvVerifiedDoctors.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvVerifiedDoctors.setAdapter(doctorAdapter);

        requestAdapter = new ConsultationRequestAdapter(false, new ConsultationRequestAdapter.OnConsultationRequestListener() {
            @Override
            public void onInspectDetails(@NonNull ConsultationRequest request) {
                showRequestDetailsDialog(request);
            }

            @Override
            public void onPrimaryAction(@NonNull ConsultationRequest request) {
                if (request.isAccepted()) {
                    openChatWithDoctor(request.getRequestId(),
                            request.getDoctorUid(),
                            request.getDoctorName(),
                            "Verified Connection • " + notEmpty(request.getDoctorSpecialty(), "Medicine"));
                }
            }

            @Override
            public void onDeclineOrCancel(@NonNull ConsultationRequest request) {
                confirmCancelPendingRequest(request);
            }
        });
        binding.rvPatientRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPatientRequests.setAdapter(requestAdapter);

        conversationAdapter = new ConversationThreadAdapter(false, thread ->
                openChatWithDoctor(thread.getConversationId(),
                        thread.getDoctorUid(),
                        thread.getDoctorName(),
                        "Verified Connection • " + notEmpty(thread.getDoctorSpecialty(), "Medicine"))
        );
        binding.rvPatientConversations.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPatientConversations.setAdapter(conversationAdapter);
    }

    private void setupModeSwitcherAndFilters() {
        binding.btnModeFindDoctors.setOnClickListener(v -> switchMode(0));
        binding.btnModeMyRequests.setOnClickListener(v -> switchMode(1));
        binding.btnModeMyMessages.setOnClickListener(v -> switchMode(2));

        binding.etSearchDoctor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyDoctorSearchAndFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.btnSpecAll.setOnClickListener(v -> setSpecialtyFilter("ALL"));
        binding.btnSpecMedicine.setOnClickListener(v -> setSpecialtyFilter("Medicine"));
        binding.btnSpecCardiology.setOnClickListener(v -> setSpecialtyFilter("Cardiology"));
        binding.btnSpecNeurology.setOnClickListener(v -> setSpecialtyFilter("Neurology"));
        binding.btnSpecPediatrics.setOnClickListener(v -> setSpecialtyFilter("Pediatrics"));
        binding.btnSpecOrthopedics.setOnClickListener(v -> setSpecialtyFilter("Orthopedics"));
        binding.btnSpecGynecology.setOnClickListener(v -> setSpecialtyFilter("Gynecology"));
        binding.btnSpecSurgery.setOnClickListener(v -> setSpecialtyFilter("Surgery"));

        binding.btnReqFilterAll.setOnClickListener(v -> setRequestFilter("ALL"));
        binding.btnReqFilterPending.setOnClickListener(v -> setRequestFilter(ConsultationRequest.STATUS_PENDING));
        binding.btnReqFilterAccepted.setOnClickListener(v -> setRequestFilter(ConsultationRequest.STATUS_ACCEPTED));

        binding.btnPatientNotificationsAlert.setOnClickListener(v -> showPatientNotificationsDialog());
    }

    private void switchMode(int mode) {
        if (binding == null) return;
        MaterialButton selectedBtn = mode == 0
                ? binding.btnModeFindDoctors
                : (mode == 1 ? binding.btnModeMyRequests : binding.btnModeMyMessages);
        UiStateHelper.updateToggleButtons(
                selectedBtn,
                R.color.pale_mint,
                binding.btnModeFindDoctors,
                binding.btnModeMyRequests,
                binding.btnModeMyMessages
        );

        binding.layoutFindDoctorsSection.setVisibility(mode == 0 ? View.VISIBLE : View.GONE);
        binding.layoutMyRequestsSection.setVisibility(mode == 1 ? View.VISIBLE : View.GONE);
        binding.layoutPatientConversationsSection.setVisibility(mode == 2 ? View.VISIBLE : View.GONE);
        if (mode == 2) {
            loadConversations();
        }
    }

    private void loadPatientProfileAndData() {
        UserAccount account = sessionManager.getCurrentSessionAccount();
        if (account == null) return;

        profileRepository.loadPatientProfile(account.getUid(), new AuthCallback<PatientProfile>() {
            @Override
            public void onSuccess(PatientProfile result) {
                currentPatientProfile = result;
            }

            @Override
            public void onError(String message) {
                // Keep fallback
            }
        });

        refreshDoctorsAndRequests();
        loadConversations();
    }

    private void refreshDoctorsAndRequests() {
        UserAccount account = sessionManager.getCurrentSessionAccount();
        if (account == null) return;

        discoveryRepository.loadPatientRequests(account.getUid(), new AuthCallback<List<ConsultationRequest>>() {
            @Override
            public void onSuccess(List<ConsultationRequest> reqs) {
                if (binding == null) return;
                allPatientRequests.clear();
                allPatientRequests.addAll(reqs);
                requestMapByDoctorUid.clear();
                for (ConsultationRequest r : reqs) {
                    if (r.getDoctorUid() != null) {
                        requestMapByDoctorUid.put(r.getDoctorUid(), r);
                    }
                }
                applyRequestFilter();

                discoveryRepository.loadVerifiedDoctors(new AuthCallback<List<DoctorProfile>>() {
                    @Override
                    public void onSuccess(List<DoctorProfile> doctors) {
                        if (binding == null) return;
                        allVerifiedDoctors.clear();
                        allVerifiedDoctors.addAll(doctors);
                        applyDoctorSearchAndFilter();
                    }

                    @Override
                    public void onError(String message) {
                        if (binding == null) return;
                        UiStateHelper.showError(binding.getRoot(), message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    private void loadConversations() {
        messagingRepository.loadAuthorizedConversations(new AuthCallback<List<ConversationThread>>() {
            @Override
            public void onSuccess(List<ConversationThread> result) {
                if (binding == null) return;
                conversationAdapter.submitList(result);
                if (result.isEmpty()) {
                    binding.tvEmptyPatientConversations.setVisibility(View.VISIBLE);
                    binding.rvPatientConversations.setVisibility(View.GONE);
                } else {
                    binding.tvEmptyPatientConversations.setVisibility(View.GONE);
                    binding.rvPatientConversations.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.tvEmptyPatientConversations.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setSpecialtyFilter(String specialty) {
        this.selectedSpecialtyFilter = specialty;
        if (binding != null) {
            MaterialButton selectedBtn = binding.btnSpecAll;
            if ("Medicine".equalsIgnoreCase(specialty)) selectedBtn = binding.btnSpecMedicine;
            else if ("Cardiology".equalsIgnoreCase(specialty)) selectedBtn = binding.btnSpecCardiology;
            else if ("Neurology".equalsIgnoreCase(specialty)) selectedBtn = binding.btnSpecNeurology;
            else if ("Pediatrics".equalsIgnoreCase(specialty)) selectedBtn = binding.btnSpecPediatrics;
            else if ("Orthopedics".equalsIgnoreCase(specialty)) selectedBtn = binding.btnSpecOrthopedics;
            else if ("Gynecology".equalsIgnoreCase(specialty)) selectedBtn = binding.btnSpecGynecology;
            else if ("Surgery".equalsIgnoreCase(specialty)) selectedBtn = binding.btnSpecSurgery;

            UiStateHelper.updateToggleButtons(
                    selectedBtn,
                    R.color.white,
                    binding.btnSpecAll,
                    binding.btnSpecMedicine,
                    binding.btnSpecCardiology,
                    binding.btnSpecNeurology,
                    binding.btnSpecPediatrics,
                    binding.btnSpecOrthopedics,
                    binding.btnSpecGynecology,
                    binding.btnSpecSurgery
            );
        }
        applyDoctorSearchAndFilter();
    }

    private void applyDoctorSearchAndFilter() {
        if (binding == null) return;
        String query = binding.etSearchDoctor.getText() != null
                ? binding.etSearchDoctor.getText().toString().trim().toLowerCase(Locale.US)
                : "";

        List<DoctorProfile> filtered = new ArrayList<>();
        for (DoctorProfile d : allVerifiedDoctors) {
            String name = d.getFullName() != null ? d.getFullName().toLowerCase(Locale.US) : "";
            String spec = d.getSpecialty() != null ? d.getSpecialty().toLowerCase(Locale.US) : "";
            String dept = d.getDepartment() != null ? d.getDepartment().toLowerCase(Locale.US) : "";
            String quals = d.getQualifications() != null ? d.getQualifications().toLowerCase(Locale.US) : "";

            boolean matchesSpecialty = "ALL".equalsIgnoreCase(selectedSpecialtyFilter)
                    || spec.contains(selectedSpecialtyFilter.toLowerCase(Locale.US))
                    || dept.contains(selectedSpecialtyFilter.toLowerCase(Locale.US));

            boolean matchesQuery = query.isEmpty()
                    || name.contains(query)
                    || spec.contains(query)
                    || dept.contains(query)
                    || quals.contains(query);

            if (matchesSpecialty && matchesQuery) {
                filtered.add(d);
            }
        }

        doctorAdapter.submitData(filtered, requestMapByDoctorUid);
        binding.tvDoctorsResultSummary.setText(
                "Showing " + filtered.size() + " Registered Specialist" + (filtered.size() == 1 ? "" : "s")
                        + ("ALL".equals(selectedSpecialtyFilter) ? "" : " in " + selectedSpecialtyFilter));

        if (filtered.isEmpty()) {
            binding.tvEmptyDoctors.setVisibility(View.VISIBLE);
            binding.rvVerifiedDoctors.setVisibility(View.GONE);
        } else {
            binding.tvEmptyDoctors.setVisibility(View.GONE);
            binding.rvVerifiedDoctors.setVisibility(View.VISIBLE);
        }
    }

    private void setRequestFilter(String filter) {
        this.selectedRequestFilter = filter;
        if (binding != null) {
            MaterialButton selectedBtn = binding.btnReqFilterAll;
            if (ConsultationRequest.STATUS_PENDING.equalsIgnoreCase(filter)) {
                selectedBtn = binding.btnReqFilterPending;
            } else if (ConsultationRequest.STATUS_ACCEPTED.equalsIgnoreCase(filter)) {
                selectedBtn = binding.btnReqFilterAccepted;
            }
            UiStateHelper.updateToggleButtons(
                    selectedBtn,
                    R.color.white,
                    binding.btnReqFilterAll,
                    binding.btnReqFilterPending,
                    binding.btnReqFilterAccepted
            );
        }
        applyRequestFilter();
    }

    private void applyRequestFilter() {
        if (binding == null) return;
        List<ConsultationRequest> filtered = new ArrayList<>();
        for (ConsultationRequest r : allPatientRequests) {
            if ("ALL".equalsIgnoreCase(selectedRequestFilter)
                    || selectedRequestFilter.equalsIgnoreCase(r.getStatus())) {
                filtered.add(r);
            }
        }
        requestAdapter.submitList(filtered);
        if (filtered.isEmpty()) {
            binding.tvEmptyPatientRequests.setVisibility(View.VISIBLE);
            binding.rvPatientRequests.setVisibility(View.GONE);
        } else {
            binding.tvEmptyPatientRequests.setVisibility(View.GONE);
            binding.rvPatientRequests.setVisibility(View.VISIBLE);
        }
    }

    private void showDoctorProfileDialog(@NonNull DoctorProfile doctor,
                                         @Nullable ConsultationRequest existing) {
        String stateText = existing != null ? existing.getStatus() : "Not Requested";
        String details = "Doctor Name: " + notEmpty(doctor.getFullName(), "Dr. Specialist") + "\n"
                + "AROHI ID: " + notEmpty(doctor.getArohiId(), "AROHI-DOC") + "\n"
                + "BMDC License: " + notEmpty(doctor.getMedicalLicenseNumber(), "Verified") + " (Verified)\n"
                + "Specialty: " + notEmpty(doctor.getSpecialty(), "General Medicine") + "\n"
                + "Department: " + notEmpty(doctor.getDepartment(), "Medicine") + "\n"
                + "Qualifications: " + notEmpty(doctor.getQualifications(), "MBBS") + "\n"
                + "Clinical Experience: " + doctor.getExperienceYears() + " Years\n"
                + "Consultation Fee: " + String.format("%.0f", doctor.getConsultationFeeBdt()) + " BDT\n"
                + "Availability Schedule: " + notEmpty(doctor.getAvailabilitySchedule(), "Sun - Thu, 5:00 PM - 9:00 PM") + "\n"
                + "Chamber Address: " + notEmpty(doctor.getAddress(), "AROHI Hospital") + "\n"
                + "Bio: " + notEmpty(doctor.getBioNotes(), "Verified Specialist at AROHI") + "\n\n"
                + "Your Request Status: " + stateText;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle("Specialist Profile")
                .setMessage(details)
                .setNegativeButton("Close", null);

        if (existing != null && existing.isAccepted()) {
            builder.setPositiveButton("Open Secure Chat", (d, w) ->
                    openChatWithDoctor(existing.getRequestId(),
                            doctor.getUid(),
                            doctor.getFullName(),
                            "Verified Connection • " + notEmpty(doctor.getSpecialty(), "Medicine")));
        } else if (existing == null || existing.isDeclined() || existing.isCancelled()) {
            builder.setPositiveButton("Request Consultation", (d, w) -> showSendRequestDialog(doctor));
        }
        builder.show();
    }

    private void showSendRequestDialog(@NonNull DoctorProfile doctor) {
        UserAccount account = sessionManager.getCurrentSessionAccount();
        if (account == null) return;

        if (currentPatientProfile == null) {
            currentPatientProfile = sessionManager.getPatientProfileLocal(account.getUid());
        }
        if (currentPatientProfile == null) {
            currentPatientProfile = new PatientProfile(
                    account.getUid(), account.getArohiId(), account.getFullName(),
                    account.getEmail(), account.getPhone(),
                    "Not Specified", "Not Specified", "Not Set",
                    "Not Set", account.getPhone(), "None"
            );
        }

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);

        Spinner reasonSpinner = new Spinner(requireContext());
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                CONSULTATION_REASONS
        );
        reasonSpinner.setAdapter(spinnerAdapter);
        container.addView(reasonSpinner);

        EditText etNote = new EditText(requireContext());
        etNote.setHint("Brief symptoms or consultation note (optional)");
        etNote.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        etNote.setMinLines(2);
        container.addView(etNote);

        new AlertDialog.Builder(requireContext())
                .setTitle("Request Consultation with " + notEmpty(doctor.getFullName(), "Doctor"))
                .setMessage("Select your consultation reason below. Once the doctor accepts your request, secure 1-to-1 messaging will unlock.")
                .setView(container)
                .setPositiveButton("Submit Request", (dialog, which) -> {
                    String selectedReason = reasonSpinner.getSelectedItem() != null
                            ? reasonSpinner.getSelectedItem().toString()
                            : CONSULTATION_REASONS[0];
                    String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";

                    discoveryRepository.sendConsultationRequest(
                            doctor,
                            currentPatientProfile,
                            selectedReason,
                            note,
                            new AuthCallback<ConsultationRequest>() {
                                @Override
                                public void onSuccess(ConsultationRequest result) {
                                    if (binding == null) return;
                                    UiStateHelper.showSuccess(binding.getRoot(),
                                            "Consultation request sent to " + doctor.getFullName() + " (Status: Pending)");
                                    refreshDoctorsAndRequests();
                                }

                                @Override
                                public void onError(String message) {
                                    if (binding == null) return;
                                    UiStateHelper.showError(binding.getRoot(), message);
                                }
                            }
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRequestDetailsDialog(@NonNull ConsultationRequest req) {
        String timeStr = req.getUpdatedAt() > 0
                ? DateFormat.format("yyyy/MM/dd hh:mm a", new Date(req.getUpdatedAt())).toString()
                : "N/A";
        String info = "Doctor: " + notEmpty(req.getDoctorName(), "Doctor") + "\n"
                + "Doctor ID: " + notEmpty(req.getDoctorId(), "AROHI-DOC") + "\n"
                + "Specialty: " + notEmpty(req.getDoctorSpecialty(), "Medicine") + "\n"
                + "Consultation Reason: " + notEmpty(req.getConsultationReason(), "Consultation") + "\n"
                + "Your Note: " + notEmpty(req.getPatientNote(), "None") + "\n"
                + "Doctor Response: " + notEmpty(req.getDoctorResponseNote(), "Awaiting doctor response") + "\n"
                + "Request Status: " + notEmpty(req.getStatus(), "Pending") + "\n"
                + "Last Updated: " + timeStr;

        new AlertDialog.Builder(requireContext())
                .setTitle("Consultation Request Details")
                .setMessage(info)
                .setPositiveButton("Close", null)
                .show();
    }

    private void confirmCancelPendingRequest(@NonNull ConsultationRequest req) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cancel Consultation Request?")
                .setMessage("Are you sure you want to cancel your Pending request to " + req.getDoctorName() + "?")
                .setPositiveButton("Yes, Cancel Request", (d, w) ->
                        discoveryRepository.cancelConsultationRequest(req, new AuthCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                if (binding == null) return;
                                UiStateHelper.showSuccess(binding.getRoot(), "Consultation request cancelled.");
                                refreshDoctorsAndRequests();
                            }

                            @Override
                            public void onError(String message) {
                                if (binding == null) return;
                                UiStateHelper.showError(binding.getRoot(), message);
                            }
                        }))
                .setNegativeButton("Keep Pending", null)
                .show();
    }

    private void showPatientNotificationsDialog() {
        UserAccount account = sessionManager.getCurrentSessionAccount();
        if (account == null) return;

        messagingRepository.loadUserNotifications(account.getUid(), new AuthCallback<List<AppNotification>>() {
            @Override
            public void onSuccess(List<AppNotification> list) {
                if (binding == null) return;
                messagingRepository.markAllNotificationsRead(account.getUid());

                if (list.isEmpty()) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Notifications")
                            .setMessage("No notifications yet. Request updates and secure message alerts appear here.")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }

                StringBuilder sb = new StringBuilder();
                int count = Math.min(list.size(), 10);
                for (int i = 0; i < count; i++) {
                    AppNotification n = list.get(i);
                    String t = n.getCreatedAt() > 0
                            ? DateFormat.format("MMM dd, hh:mm a", new Date(n.getCreatedAt())).toString()
                            : "";
                    sb.append("• ").append(n.getTitle()).append(" (").append(t).append(")\n")
                            .append("  ").append(n.getBody()).append("\n\n");
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("Recent Privacy-Safe Notifications")
                        .setMessage(sb.toString().trim())
                        .setPositiveButton("Done", null)
                        .show();
            }

            @Override
            public void onError(String message) {
                // Ignore
            }
        });
    }

    private void openChatWithDoctor(String conversationId,
                                    String doctorUid,
                                    String doctorName,
                                    String subtitle) {
        Intent intent = SecureChatActivity.createIntent(
                requireContext(),
                conversationId,
                doctorUid,
                doctorName != null ? doctorName : "Dr. Specialist",
                subtitle
        );
        startActivity(intent);
    }

    private String notEmpty(String val, String fallback) {
        return val != null && !val.trim().isEmpty() ? val.trim() : fallback;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && binding != null && isAdded()) {
            refreshDoctorsAndRequests();
            loadConversations();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && isAdded()) {
            refreshDoctorsAndRequests();
            loadConversations();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
