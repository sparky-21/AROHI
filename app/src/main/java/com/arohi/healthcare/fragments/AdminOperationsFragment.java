package com.arohi.healthcare.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.adapters.HospitalServiceAdapter;
import com.arohi.healthcare.adapters.NurseBedAdapter;
import com.arohi.healthcare.adapters.OTRoomAdapter;
import com.arohi.healthcare.adapters.OperationAdapter;
import com.arohi.healthcare.databinding.DialogAssignBedBinding;
import com.arohi.healthcare.databinding.DialogScheduleOperationBinding;
import com.arohi.healthcare.databinding.DialogUpdateOperationStatusBinding;
import com.arohi.healthcare.databinding.FragmentAdminOperationsBinding;
import com.arohi.healthcare.models.Bed;
import com.arohi.healthcare.models.DoctorProfile;
import com.arohi.healthcare.models.HospitalService;
import com.arohi.healthcare.models.NurseProfile;
import com.arohi.healthcare.models.OTRoom;
import com.arohi.healthcare.models.Operation;
import com.arohi.healthcare.models.PatientProfile;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.DoctorDiscoveryAndRequestRepository;
import com.arohi.healthcare.repositories.HospitalOperationsRepository;
import com.arohi.healthcare.repositories.ProfileRepository;
import com.arohi.healthcare.utils.DatePickerHelper;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Hospital Administrator Operations & Facility Management Fragment.
 * Tab 1: Operation Theatres (OT), live surgeries, scheduling with conflict prevention, status lifecycle.
 * Tab 2: Inpatient Wards & Beds, admission, occupancy validation, and discharge.
 * Tab 3: Hospital Services & Clinical Departments.
 */
public class AdminOperationsFragment extends Fragment {

    private FragmentAdminOperationsBinding binding;
    private HospitalOperationsRepository operationsRepository;
    private DoctorDiscoveryAndRequestRepository doctorRepository;
    private ProfileRepository profileRepository;
    private SessionManager sessionManager;

    private String currentTab = "OT"; // OT, BEDS, SERVICES

    // Adapters
    private OperationAdapter operationAdapter;
    private NurseBedAdapter bedAdapter;
    private HospitalServiceAdapter serviceAdapter;

    private final List<OTRoom> allOtRooms = new ArrayList<>();
    private final List<Operation> allOperations = new ArrayList<>();
    private final List<Bed> allBeds = new ArrayList<>();
    private final List<HospitalService> allServices = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminOperationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        operationsRepository = new HospitalOperationsRepository(requireContext());
        doctorRepository = new DoctorDiscoveryAndRequestRepository(requireContext());
        profileRepository = new ProfileRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        setupAdapters();
        setupTabButtons();

        binding.btnAdminScheduleOpQuick.setOnClickListener(v -> showScheduleOperationDialog(null));

        switchTab("OT");
    }

    private void setupAdapters() {
        operationAdapter = new OperationAdapter(new OperationAdapter.OnOperationActionListener() {
            @Override
            public void onUpdateStatus(@NonNull Operation operation) {
                showUpdateOperationStatusDialog(operation);
            }

            @Override
            public void onViewChecklist(@NonNull Operation operation) {
                showChecklistDialog(operation);
            }
        });

        bedAdapter = new NurseBedAdapter(false, new NurseBedAdapter.OnBedActionListener() {
            @Override
            public void onRecordVitals(@NonNull Bed bed) {
                // Not used in Admin mode
            }

            @Override
            public void onBedAction(@NonNull Bed bed) {
                if (bed.isAvailableForAdmission()) {
                    showAssignBedDialog(bed);
                } else if (Bed.STATUS_OCCUPIED.equalsIgnoreCase(bed.getStatus())) {
                    showDischargeBedDialog(bed);
                } else {
                    operationsRepository.updateBedStatus(bed.getBedId(), Bed.STATUS_AVAILABLE, new AuthCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            if (binding == null) return;
                            UiStateHelper.showSuccess(binding.getRoot(), bed.getBedNumber() + " is now Available.");
                            loadBedsData();
                        }

                        @Override
                        public void onError(String message) {}
                    });
                }
            }

            @Override
            public void onChangeStatus(@NonNull Bed bed) {
                showChangeBedStatusDialog(bed);
            }
        });

        serviceAdapter = new HospitalServiceAdapter((service, active) -> {
            operationsRepository.toggleServiceStatus(service.getServiceId(), active, new AuthCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    if (binding == null) return;
                    String state = active ? "Activated" : "Deactivated";
                    UiStateHelper.showSuccess(binding.getRoot(), service.getServiceName() + ": " + state);
                }

                @Override
                public void onError(String message) {}
            });
        });

        binding.rvAdminOperations.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void setupTabButtons() {
        binding.btnTabOtOperations.setOnClickListener(v -> switchTab("OT"));
        binding.btnTabWardsBeds.setOnClickListener(v -> switchTab("BEDS"));
        binding.btnTabServices.setOnClickListener(v -> switchTab("SERVICES"));
    }

    private void switchTab(String tabKey) {
        if (binding == null) return;
        this.currentTab = tabKey;

        UiStateHelper.updateToggleButtons(
                "OT".equals(tabKey) ? binding.btnTabOtOperations
                        : ("BEDS".equals(tabKey) ? binding.btnTabWardsBeds : binding.btnTabServices),
                R.color.primary_moss_green,
                binding.btnTabOtOperations,
                binding.btnTabWardsBeds,
                binding.btnTabServices
        );

        if ("OT".equals(tabKey)) {
            binding.layoutOtActionsRow.setVisibility(View.VISIBLE);
            binding.btnAdminScheduleOpQuick.setVisibility(View.VISIBLE);
            binding.tvAdminOpsSubtitle.setText("Operation Theatres (OT), Surgical Schedules & Conflict Detection");
            binding.rvAdminOperations.setAdapter(operationAdapter);
            loadOtAndOperationsData();
        } else if ("BEDS".equals(tabKey)) {
            binding.layoutOtActionsRow.setVisibility(View.VISIBLE);
            binding.btnAdminScheduleOpQuick.setVisibility(View.GONE);
            binding.tvAdminOpsSubtitle.setText("Inpatient Wards, Bed Status (Available, Occupied, Cleaning) & Patient Admissions");
            binding.rvAdminOperations.setAdapter(bedAdapter);
            loadBedsData();
        } else {
            binding.layoutOtActionsRow.setVisibility(View.GONE);
            binding.tvAdminOpsSubtitle.setText("Hospital Services (Emergency, ICU, Blood Bank, Dialysis, Ambulance) & Clinical Leads");
            binding.rvAdminOperations.setAdapter(serviceAdapter);
            loadServicesData();
        }
    }

    private void loadOtAndOperationsData() {
        operationsRepository.loadOperations(new AuthCallback<List<Operation>>() {
            @Override
            public void onSuccess(List<Operation> operations) {
                if (binding == null) return;
                allOperations.clear();
                allOperations.addAll(operations);
                operationAdapter.submitList(allOperations);

                int scheduled = 0;
                for (Operation op : operations) {
                    if (!Operation.STATUS_COMPLETED.equalsIgnoreCase(op.getStatus())
                            && !Operation.STATUS_CANCELLED.equalsIgnoreCase(op.getStatus())) {
                        scheduled++;
                    }
                }
                binding.tvAdminOpsSummary.setText("4 OT Suites • " + scheduled + " Active/Scheduled Surgeries");
                binding.tvEmptyOpsState.setVisibility(operations.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    private void loadBedsData() {
        operationsRepository.loadBeds(new AuthCallback<List<Bed>>() {
            @Override
            public void onSuccess(List<Bed> beds) {
                if (binding == null) return;
                allBeds.clear();
                allBeds.addAll(beds);
                bedAdapter.submitList(allBeds);

                int total = beds.size();
                int available = 0;
                for (Bed b : beds) {
                    if (b.isAvailableForAdmission()) available++;
                }
                binding.tvAdminOpsSummary.setText(
                        total + " Total Beds • " + available + " Available for Admission • " + (total - available) + " Occupied/Maintenance");
                binding.tvEmptyOpsState.setVisibility(beds.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    private void loadServicesData() {
        operationsRepository.loadHospitalServices(new AuthCallback<List<HospitalService>>() {
            @Override
            public void onSuccess(List<HospitalService> services) {
                if (binding == null) return;
                allServices.clear();
                allServices.addAll(services);
                serviceAdapter.submitList(allServices);
                binding.tvEmptyOpsState.setVisibility(services.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    // =========================================================================
    // DIALOGS: SCHEDULE OPERATION WITH CONFLICT DETECTION
    // =========================================================================

    public void showScheduleOperationDialog(@Nullable PatientProfile preselectedPatient) {
        DialogScheduleOperationBinding dBinding = DialogScheduleOperationBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dBinding.getRoot())
                .create();

        // 1. OT Room dropdown
        String[] otRooms = new String[]{
                "OT Suite 1 - Cardiac Surgery (Floor 3)",
                "OT Suite 2 - General & Laparoscopic (Floor 3)",
                "OT Suite 3 - Orthopedic & Trauma (Floor 3)",
                "OT Suite 4 - Neuro & Micro-Surgery (Floor 3)"
        };
        ArrayAdapter<String> otAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, otRooms);
        dBinding.actvSelectOtRoom.setAdapter(otAdapter);
        dBinding.actvSelectOtRoom.setText(otRooms[1], false);

        // 2. Date picker
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        dBinding.etOpDate.setText(df.format(new Date()));
        dBinding.etOpDate.setOnClickListener(v -> {
            DatePickerHelper.showDatePicker(requireContext(), (y, m, d) -> {
                dBinding.etOpDate.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d));
            });
        });

        // 3. Time slots
        String[] timeSlots = new String[]{
                "09:00 AM", "10:00 AM", "11:30 AM", "02:00 PM", "04:00 PM", "06:00 PM"
        };
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, timeSlots);
        dBinding.actvOpTime.setAdapter(timeAdapter);
        dBinding.actvOpTime.setText(timeSlots[1], false);

        // 4. Assisting Nurse dropdown
        String[] nurses = new String[]{
                "Sister Fatima (RN) • Floor 2 & 3 Scrub Specialist",
                "Sister Nasreen (RN) • OT Certified Scrub Specialist"
        };
        ArrayAdapter<String> nurseAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, nurses);
        dBinding.actvSelectNurse.setAdapter(nurseAdapter);
        dBinding.actvSelectNurse.setText(nurses[0], false);

        // 5. Follow-up Date default
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 7);
        dBinding.etFollowUpDate.setText(df.format(cal.getTime()));
        dBinding.etFollowUpDate.setOnClickListener(v -> {
            DatePickerHelper.showDatePicker(requireContext(), (y, m, d) -> {
                dBinding.etFollowUpDate.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d));
            });
        });

        dBinding.btnCancelScheduleOp.setOnClickListener(v -> dialog.dismiss());

        dBinding.btnSubmitScheduleOp.setOnClickListener(v -> {
            String proc = dBinding.etProcedureName.getText() != null
                    ? dBinding.etProcedureName.getText().toString().trim() : "";
            if (proc.isEmpty()) {
                dBinding.tilProcedureName.setError("Please enter surgical procedure name");
                return;
            }
            dBinding.tilProcedureName.setError(null);

            String otRoom = dBinding.actvSelectOtRoom.getText().toString();
            String date = dBinding.etOpDate.getText().toString();
            String time = dBinding.actvOpTime.getText().toString();
            String nurse = dBinding.actvSelectNurse.getText().toString();
            String followUp = dBinding.etFollowUpDate.getText() != null
                    ? dBinding.etFollowUpDate.getText().toString().trim() : "";

            UserAccount cur = sessionManager.getCurrentSessionAccount();
            String surgeonName = cur != null ? cur.getFullName() : "Dr. Raisa";
            String surgeonUid = cur != null ? cur.getUid() : "doc_demo_01";

            String patName = preselectedPatient != null ? preselectedPatient.getFullName() : "Karim Ahmed";
            String patUid = preselectedPatient != null ? preselectedPatient.getUid() : "pat_demo_01";
            String patId = preselectedPatient != null ? preselectedPatient.getArohiId() : "AROHI-PAT-KRM1";

            Operation op = new Operation(
                    "",
                    proc,
                    "General & Laparoscopic Surgery",
                    "ot_02",
                    otRoom,
                    date,
                    time,
                    90,
                    patUid,
                    patId,
                    patName,
                    surgeonUid,
                    "AROHI-DOC-01",
                    surgeonName,
                    "General Surgery",
                    "nurse_demo_01",
                    nurse
            );
            op.setNpoConfirmed(dBinding.cbNpo.isChecked());
            op.setAnesthesiaCleared(dBinding.cbAnesthesia.isChecked());
            op.setConsentSigned(dBinding.cbConsent.isChecked());
            op.setBloodArranged(dBinding.cbBlood.isChecked());
            op.setPreOpAntibioticsGiven(dBinding.cbAntibiotics.isChecked());
            op.setFollowUpDate(followUp);

            dBinding.btnSubmitScheduleOp.setEnabled(false);
            operationsRepository.scheduleOperation(op, new AuthCallback<Operation>() {
                @Override
                public void onSuccess(Operation result) {
                    dialog.dismiss();
                    if (binding != null) {
                        UiStateHelper.showSuccess(binding.getRoot(),
                                "Operation scheduled: " + result.getProcedureName() + " on " + result.getOperationDate() + "!");
                        loadOtAndOperationsData();
                    }
                }

                @Override
                public void onError(String message) {
                    dBinding.btnSubmitScheduleOp.setEnabled(true);
                    UiStateHelper.showError(dBinding.getRoot(), message);
                }
            });
        });

        dialog.show();
    }

    private void showUpdateOperationStatusDialog(@NonNull Operation op) {
        DialogUpdateOperationStatusBinding dBinding = DialogUpdateOperationStatusBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dBinding.getRoot())
                .create();

        dBinding.tvUpdateOpProcedureSub.setText(op.getProcedureName() + " • " + op.getPatientName());

        String[] statuses = new String[]{
                Operation.STATUS_SCHEDULED,
                Operation.STATUS_PREPARATION,
                Operation.STATUS_IN_PROGRESS,
                Operation.STATUS_COMPLETED,
                Operation.STATUS_FOLLOW_UP,
                Operation.STATUS_CANCELLED
        };
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, statuses);
        dBinding.actvSelectOpStatus.setAdapter(statusAdapter);
        dBinding.actvSelectOpStatus.setText(op.getStatus(), false);

        if (op.getPostOpNotes() != null && !op.getPostOpNotes().isEmpty()) {
            dBinding.etPostOpNotes.setText(op.getPostOpNotes());
        }
        if (op.getFollowUpDate() != null && !op.getFollowUpDate().isEmpty()) {
            dBinding.etOpFollowUpDate.setText(op.getFollowUpDate());
        }

        dBinding.btnCancelUpdateStatus.setOnClickListener(v -> dialog.dismiss());

        dBinding.btnSubmitUpdateStatus.setOnClickListener(v -> {
            String newStatus = dBinding.actvSelectOpStatus.getText().toString();
            String notes = dBinding.etPostOpNotes.getText() != null ? dBinding.etPostOpNotes.getText().toString().trim() : "";
            String fDate = dBinding.etOpFollowUpDate.getText() != null ? dBinding.etOpFollowUpDate.getText().toString().trim() : "";

            dBinding.btnSubmitUpdateStatus.setEnabled(false);
            operationsRepository.updateOperationStatus(op.getOperationId(), newStatus, notes, fDate, new AuthCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    dialog.dismiss();
                    if (binding != null) {
                        UiStateHelper.showSuccess(binding.getRoot(),
                                "Status updated: " + op.getProcedureName() + " -> " + newStatus);
                        loadOtAndOperationsData();
                    }
                }

                @Override
                public void onError(String message) {
                    dBinding.btnSubmitUpdateStatus.setEnabled(true);
                    UiStateHelper.showError(dBinding.getRoot(), message);
                }
            });
        });

        dialog.show();
    }

    private void showChecklistDialog(@NonNull Operation op) {
        String checklist = "Mandatory Pre-Op Surgical Checklist:\n\n"
                + (op.isNpoConfirmed() ? "✓" : "✗") + " NPO Fasting Verified (6-8 hrs)\n"
                + (op.isAnesthesiaCleared() ? "✓" : "✗") + " Anesthesia Fitness Cleared\n"
                + (op.isConsentSigned() ? "✓" : "✗") + " Informed Consent Signed\n"
                + (op.isBloodArranged() ? "✓" : "✗") + " Blood Cross-matched & Reserved\n"
                + (op.isPreOpAntibioticsGiven() ? "✓" : "✗") + " Prophylactic Antibiotics Given\n\n"
                + "Surgeon: Dr. " + op.getDoctorName() + "\n"
                + "Scrub Nurse: " + op.getNurseName() + "\n"
                + "Follow-up Date: " + (op.getFollowUpDate() != null ? op.getFollowUpDate() : "TBD");

        new AlertDialog.Builder(requireContext())
                .setTitle("Checklist: " + op.getProcedureName())
                .setMessage(checklist)
                .setPositiveButton("Close", null)
                .show();
    }

    // =========================================================================
    // DIALOGS: WARD & BED ASSIGNMENT
    // =========================================================================

    private void showAssignBedDialog(@NonNull Bed bed) {
        DialogAssignBedBinding dBinding = DialogAssignBedBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dBinding.getRoot())
                .create();

        dBinding.tvAssignBedSub.setText(bed.getBedNumber() + " • " + bed.getWardName());

        String[] samplePatients = new String[]{
                "Karim Ahmed (AROHI-PAT-KRM1)",
                "Tanvir Hossain (AROHI-PAT-TNV2)",
                "Salma Begum (AROHI-PAT-SLM3)",
                "Rafiqul Islam (AROHI-PAT-RFQ4)"
        };
        ArrayAdapter<String> patAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, samplePatients);
        dBinding.actvSelectPatient.setAdapter(patAdapter);
        dBinding.actvSelectPatient.setText(samplePatients[0], false);

        String[] sampleDoctors = new String[]{
                "Dr. Raisa (General & Laparoscopic Surgery)",
                "Dr. Kamal (Orthopedics & Joint Replacement)"
        };
        ArrayAdapter<String> docAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, sampleDoctors);
        dBinding.actvSelectDoctor.setAdapter(docAdapter);
        dBinding.actvSelectDoctor.setText(sampleDoctors[0], false);

        String[] sampleNurses = new String[]{
                "Sister Fatima (RN) • In-charge Nurse",
                "Sister Nasreen (RN) • Ward Staff Nurse"
        };
        ArrayAdapter<String> nurseAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, sampleNurses);
        dBinding.actvSelectNurse.setAdapter(nurseAdapter);
        dBinding.actvSelectNurse.setText(sampleNurses[0], false);

        dBinding.btnCancelAssignBed.setOnClickListener(v -> dialog.dismiss());

        dBinding.btnSubmitAssignBed.setOnClickListener(v -> {
            String pat = dBinding.actvSelectPatient.getText().toString();
            String doc = dBinding.actvSelectDoctor.getText().toString();
            String nurse = dBinding.actvSelectNurse.getText().toString();
            String notes = dBinding.etAdmissionNotes.getText() != null
                    ? dBinding.etAdmissionNotes.getText().toString().trim() : "";

            String patName = pat.contains("(") ? pat.substring(0, pat.indexOf("(")).trim() : pat;
            String patId = pat.contains("(") ? pat.substring(pat.indexOf("(") + 1, pat.indexOf(")")).trim() : "AROHI-PAT";

            dBinding.btnSubmitAssignBed.setEnabled(false);
            operationsRepository.assignPatientToBed(
                    bed.getBedId(),
                    "pat_assigned_" + System.currentTimeMillis(),
                    patId,
                    patName,
                    "doc_assigned_01",
                    doc.contains("(") ? doc.substring(0, doc.indexOf("(")).trim() : doc,
                    "nurse_assigned_01",
                    nurse.contains("•") ? nurse.substring(0, nurse.indexOf("•")).trim() : nurse,
                    notes,
                    new AuthCallback<Bed>() {
                        @Override
                        public void onSuccess(Bed result) {
                            dialog.dismiss();
                            if (binding != null) {
                                UiStateHelper.showSuccess(binding.getRoot(),
                                        patName + " successfully admitted to " + result.getBedNumber() + "!");
                                loadBedsData();
                            }
                        }

                        @Override
                        public void onError(String message) {
                            dBinding.btnSubmitAssignBed.setEnabled(true);
                            UiStateHelper.showError(dBinding.getRoot(), message);
                        }
                    }
            );
        });

        dialog.show();
    }

    private void showDischargeBedDialog(@NonNull Bed bed) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Discharge Patient")
                .setMessage("Discharge " + bed.getPatientName() + " from " + bed.getBedNumber()
                        + " (" + bed.getWardName() + ")?\n\nBed will transition to Cleaning status.")
                .setPositiveButton("Confirm Discharge", (d, w) -> {
                    operationsRepository.dischargePatientFromBed(bed.getBedId(), new AuthCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            if (binding == null) return;
                            UiStateHelper.showSuccess(binding.getRoot(),
                                    bed.getBedNumber() + " discharged. Bed is now in Cleaning status.");
                            loadBedsData();
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

    private void showChangeBedStatusDialog(@NonNull Bed bed) {
        String[] statuses = new String[]{
                Bed.STATUS_AVAILABLE,
                Bed.STATUS_OCCUPIED,
                Bed.STATUS_CLEANING,
                Bed.STATUS_MAINTENANCE
        };
        new AlertDialog.Builder(requireContext())
                .setTitle("Change " + bed.getBedNumber() + " Status")
                .setItems(statuses, (dialog, which) -> {
                    operationsRepository.updateBedStatus(bed.getBedId(), statuses[which], new AuthCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            if (binding == null) return;
                            UiStateHelper.showSuccess(binding.getRoot(),
                                    bed.getBedNumber() + " status set to " + statuses[which] + ".");
                            loadBedsData();
                        }

                        @Override
                        public void onError(String message) {}
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && binding != null && isAdded()) {
            switchTab(currentTab);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && isAdded()) {
            switchTab(currentTab);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
