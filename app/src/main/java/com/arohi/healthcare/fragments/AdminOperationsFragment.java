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

        // 0. Dynamic Patient, Surgeon & Nurse registries (Newly registered + Saved)
        final List<UserAccount> patientOptions = new ArrayList<>();
        final List<UserAccount> doctorOptions = new ArrayList<>();
        final List<UserAccount> nurseOptions = new ArrayList<>();
        final UserAccount[] selectedPatient = new UserAccount[1];
        final UserAccount[] selectedDoctor = new UserAccount[1];
        final UserAccount[] selectedNurse = new UserAccount[1];

        if (preselectedPatient == null) {
            dBinding.tilSelectOpPatient.setVisibility(View.VISIBLE);
        } else {
            dBinding.tilSelectOpPatient.setVisibility(View.GONE);
        }
        dBinding.tilSelectOpDoctor.setVisibility(View.VISIBLE);

        // 1. OT Room dropdown (dynamically loaded OT Suites + defaults)
        final List<OTRoom> roomOptions = new ArrayList<>();
        final List<String> otRoomLabels = new ArrayList<>();
        if (!allOtRooms.isEmpty()) {
            for (OTRoom r : allOtRooms) {
                roomOptions.add(r);
                otRoomLabels.add(r.getRoomName());
            }
        } else {
            String[] defaults = new String[]{
                    "OT Suite 1 - Cardiac Surgery (Floor 3)",
                    "OT Suite 2 - General & Laparoscopic (Floor 3)",
                    "OT Suite 3 - Orthopedic & Trauma (Floor 3)",
                    "OT Suite 4 - Neuro & Micro-Surgery (Floor 3)"
            };
            for (int i = 0; i < defaults.length; i++) {
                otRoomLabels.add(defaults[i]);
            }
        }
        ArrayAdapter<String> otAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, otRoomLabels);
        dBinding.actvSelectOtRoom.setAdapter(otAdapter);
        if (!otRoomLabels.isEmpty()) {
            dBinding.actvSelectOtRoom.setText(otRoomLabels.get(otRoomLabels.size() > 1 ? 1 : 0), false);
        }

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

        // 4. Populate Patient, Doctor, and Assisting Nurse dynamically from registered + saved directory
        Runnable updateSubtitle = () -> {
            String pName = preselectedPatient != null ? preselectedPatient.getFullName()
                    : (selectedPatient[0] != null ? selectedPatient[0].getFullName() : "Select Patient");
            String dName = selectedDoctor[0] != null ? selectedDoctor[0].getFullName() : "Select Surgeon";
            dBinding.tvScheduleOpPatientSubtitle.setText("Patient: " + pName + " • Surgeon: " + dName);
        };

        operationsRepository.loadStaffAndPatientDirectory(new AuthCallback<HospitalOperationsRepository.StaffAndPatientDirectory>() {
            @Override
            public void onSuccess(HospitalOperationsRepository.StaffAndPatientDirectory dir) {
                if (getContext() == null) return;
                patientOptions.clear();
                patientOptions.addAll(dir.patients);
                doctorOptions.clear();
                doctorOptions.addAll(dir.doctors);
                nurseOptions.clear();
                nurseOptions.addAll(dir.nurses);

                List<String> patLabels = new ArrayList<>();
                for (UserAccount acc : patientOptions) patLabels.add(HospitalOperationsRepository.formatAccountOptionLabel(acc));
                ArrayAdapter<String> patAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, patLabels);
                dBinding.actvSelectOpPatient.setAdapter(patAdapter);
                if (!patientOptions.isEmpty() && selectedPatient[0] == null) {
                    selectedPatient[0] = patientOptions.get(0);
                    dBinding.actvSelectOpPatient.setText(patLabels.get(0), false);
                }
                dBinding.actvSelectOpPatient.setOnItemClickListener((p, v, pos, id) -> {
                    if (pos < patientOptions.size()) {
                        selectedPatient[0] = patientOptions.get(pos);
                        updateSubtitle.run();
                    }
                });

                List<String> docLabels = new ArrayList<>();
                for (UserAccount acc : doctorOptions) docLabels.add(HospitalOperationsRepository.formatAccountOptionLabel(acc));
                ArrayAdapter<String> docAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, docLabels);
                dBinding.actvSelectOpDoctor.setAdapter(docAdapter);
                if (!doctorOptions.isEmpty() && selectedDoctor[0] == null) {
                    selectedDoctor[0] = doctorOptions.get(0);
                    dBinding.actvSelectOpDoctor.setText(docLabels.get(0), false);
                }
                dBinding.actvSelectOpDoctor.setOnItemClickListener((p, v, pos, id) -> {
                    if (pos < doctorOptions.size()) {
                        selectedDoctor[0] = doctorOptions.get(pos);
                        updateSubtitle.run();
                    }
                });

                List<String> nurseLabels = new ArrayList<>();
                for (UserAccount acc : nurseOptions) nurseLabels.add(HospitalOperationsRepository.formatAccountOptionLabel(acc));
                ArrayAdapter<String> nurseAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, nurseLabels);
                dBinding.actvSelectNurse.setAdapter(nurseAdapter);
                if (!nurseOptions.isEmpty() && selectedNurse[0] == null) {
                    selectedNurse[0] = nurseOptions.get(0);
                    dBinding.actvSelectNurse.setText(nurseLabels.get(0), false);
                }
                dBinding.actvSelectNurse.setOnItemClickListener((p, v, pos, id) -> {
                    if (pos < nurseOptions.size()) {
                        selectedNurse[0] = nurseOptions.get(pos);
                    }
                });

                updateSubtitle.run();
            }

            @Override
            public void onError(String message) {}
        });

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
            String otRoomId = "ot_02";
            for (OTRoom r : roomOptions) {
                if (otRoom.equals(r.getRoomName())) {
                    otRoomId = r.getRoomId();
                    break;
                }
            }
            String date = dBinding.etOpDate.getText().toString();
            String time = dBinding.actvOpTime.getText().toString();
            String followUp = dBinding.etFollowUpDate.getText() != null
                    ? dBinding.etFollowUpDate.getText().toString().trim() : "";

            UserAccount patAcc = selectedPatient[0];
            String patName = preselectedPatient != null ? preselectedPatient.getFullName()
                    : (patAcc != null ? patAcc.getFullName() : "Karim Ahmed");
            String patUid = preselectedPatient != null ? preselectedPatient.getUid()
                    : (patAcc != null ? patAcc.getUid() : "pat_demo_01");
            String patId = preselectedPatient != null ? preselectedPatient.getArohiId()
                    : (patAcc != null ? patAcc.getArohiId() : "AROHI-PAT-KRM1");

            UserAccount docAcc = selectedDoctor[0];
            String surgeonName = docAcc != null ? docAcc.getFullName() : "Dr. Raisa";
            String surgeonUid = docAcc != null ? docAcc.getUid() : "doc_demo_01";
            String surgeonArohiId = docAcc != null && docAcc.getArohiId() != null ? docAcc.getArohiId() : "AROHI-DOC-01";

            UserAccount nurseAcc = selectedNurse[0];
            String nurseUid = nurseAcc != null ? nurseAcc.getUid() : "nurse_demo_01";
            String nurseName = nurseAcc != null ? nurseAcc.getFullName() : dBinding.actvSelectNurse.getText().toString();

            Operation op = new Operation(
                    "",
                    proc,
                    "General & Laparoscopic Surgery",
                    otRoomId,
                    otRoom,
                    date,
                    time,
                    90,
                    patUid,
                    patId,
                    patName,
                    surgeonUid,
                    surgeonArohiId,
                    surgeonName,
                    "General Surgery",
                    nurseUid,
                    nurseName
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

        final List<UserAccount> patientOptions = new ArrayList<>();
        final List<UserAccount> doctorOptions = new ArrayList<>();
        final List<UserAccount> nurseOptions = new ArrayList<>();
        final UserAccount[] selectedPatient = new UserAccount[1];
        final UserAccount[] selectedDoctor = new UserAccount[1];
        final UserAccount[] selectedNurse = new UserAccount[1];

        operationsRepository.loadStaffAndPatientDirectory(new AuthCallback<HospitalOperationsRepository.StaffAndPatientDirectory>() {
            @Override
            public void onSuccess(HospitalOperationsRepository.StaffAndPatientDirectory dir) {
                if (getContext() == null) return;
                patientOptions.clear();
                patientOptions.addAll(dir.patients);
                doctorOptions.clear();
                doctorOptions.addAll(dir.doctors);
                nurseOptions.clear();
                nurseOptions.addAll(dir.nurses);

                List<String> patLabels = new ArrayList<>();
                for (UserAccount acc : patientOptions) patLabels.add(HospitalOperationsRepository.formatAccountOptionLabel(acc));
                ArrayAdapter<String> patAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, patLabels);
                dBinding.actvSelectPatient.setAdapter(patAdapter);
                if (!patientOptions.isEmpty() && selectedPatient[0] == null) {
                    selectedPatient[0] = patientOptions.get(0);
                    dBinding.actvSelectPatient.setText(patLabels.get(0), false);
                }
                dBinding.actvSelectPatient.setOnItemClickListener((p, v, pos, id) -> {
                    if (pos < patientOptions.size()) selectedPatient[0] = patientOptions.get(pos);
                });

                List<String> docLabels = new ArrayList<>();
                for (UserAccount acc : doctorOptions) docLabels.add(HospitalOperationsRepository.formatAccountOptionLabel(acc));
                ArrayAdapter<String> docAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, docLabels);
                dBinding.actvSelectDoctor.setAdapter(docAdapter);
                if (!doctorOptions.isEmpty() && selectedDoctor[0] == null) {
                    selectedDoctor[0] = doctorOptions.get(0);
                    dBinding.actvSelectDoctor.setText(docLabels.get(0), false);
                }
                dBinding.actvSelectDoctor.setOnItemClickListener((p, v, pos, id) -> {
                    if (pos < doctorOptions.size()) selectedDoctor[0] = doctorOptions.get(pos);
                });

                List<String> nurseLabels = new ArrayList<>();
                for (UserAccount acc : nurseOptions) nurseLabels.add(HospitalOperationsRepository.formatAccountOptionLabel(acc));
                ArrayAdapter<String> nurseAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, nurseLabels);
                dBinding.actvSelectNurse.setAdapter(nurseAdapter);
                if (!nurseOptions.isEmpty() && selectedNurse[0] == null) {
                    selectedNurse[0] = nurseOptions.get(0);
                    dBinding.actvSelectNurse.setText(nurseLabels.get(0), false);
                }
                dBinding.actvSelectNurse.setOnItemClickListener((p, v, pos, id) -> {
                    if (pos < nurseOptions.size()) selectedNurse[0] = nurseOptions.get(pos);
                });
            }

            @Override
            public void onError(String message) {}
        });

        dBinding.btnCancelAssignBed.setOnClickListener(v -> dialog.dismiss());

        dBinding.btnSubmitAssignBed.setOnClickListener(v -> {
            String notes = dBinding.etAdmissionNotes.getText() != null
                    ? dBinding.etAdmissionNotes.getText().toString().trim() : "";

            UserAccount patAcc = selectedPatient[0];
            String patUid = patAcc != null ? patAcc.getUid() : "pat_demo_01";
            String patId = patAcc != null && patAcc.getArohiId() != null ? patAcc.getArohiId() : "AROHI-PAT";
            String patName = patAcc != null ? patAcc.getFullName() : dBinding.actvSelectPatient.getText().toString();

            UserAccount docAcc = selectedDoctor[0];
            String docUid = docAcc != null ? docAcc.getUid() : "doc_demo_01";
            String docName = docAcc != null ? docAcc.getFullName() : dBinding.actvSelectDoctor.getText().toString();

            UserAccount nurseAcc = selectedNurse[0];
            String nurseUid = nurseAcc != null ? nurseAcc.getUid() : "nurse_demo_01";
            String nurseName = nurseAcc != null ? nurseAcc.getFullName() : dBinding.actvSelectNurse.getText().toString();

            dBinding.btnSubmitAssignBed.setEnabled(false);
            operationsRepository.assignPatientToBed(
                    bed.getBedId(),
                    patUid,
                    patId,
                    patName,
                    docUid,
                    docName,
                    nurseUid,
                    nurseName,
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
