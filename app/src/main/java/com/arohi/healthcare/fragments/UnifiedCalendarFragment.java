package com.arohi.healthcare.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.adapters.CalendarEventAdapter;
import com.arohi.healthcare.databinding.DialogBookAppointmentBinding;
import com.arohi.healthcare.databinding.DialogScheduleOperationBinding;
import com.arohi.healthcare.databinding.FragmentUnifiedCalendarBinding;
import com.arohi.healthcare.models.Appointment;
import com.arohi.healthcare.models.CalendarEvent;
import com.arohi.healthcare.models.ConsultationRequest;
import com.arohi.healthcare.models.DoctorProfile;
import com.arohi.healthcare.models.Operation;
import com.arohi.healthcare.models.PatientProfile;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.ClinicalRepository;
import com.arohi.healthcare.repositories.DoctorDiscoveryAndRequestRepository;
import com.arohi.healthcare.repositories.HospitalOperationsRepository;
import com.arohi.healthcare.repositories.ProfileRepository;
import com.arohi.healthcare.utils.DatePickerHelper;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Phase 4 Unified Synchronized Medical Calendar Fragment for Patients and Doctors.
 * Supports date browsing, category filtering (Appointments, Lab Tests, Reminders),
 * booking new appointment slots with duplicate prevention & conflict checks,
 * and direct appointment rescheduling/cancellation.
 */
public class UnifiedCalendarFragment extends Fragment {

    private static final String ARG_IS_DOCTOR_MODE = "arg_is_doctor_mode";

    public static UnifiedCalendarFragment newInstance(boolean isDoctorMode) {
        UnifiedCalendarFragment fragment = new UnifiedCalendarFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_DOCTOR_MODE, isDoctorMode);
        fragment.setArguments(args);
        return fragment;
    }

    private FragmentUnifiedCalendarBinding binding;
    private ClinicalRepository clinicalRepository;
    private DoctorDiscoveryAndRequestRepository doctorRepository;
    private HospitalOperationsRepository operationsRepository;
    private ProfileRepository profileRepository;
    private SessionManager sessionManager;
    private CalendarEventAdapter adapter;

    private UserAccount currentAccount;
    private boolean isDoctorMode = false;
    private Calendar currentCalendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private final List<CalendarEvent> allEventsForDate = new ArrayList<>();
    private String selectedFilterType = "ALL"; // ALL, APPOINTMENT, LAB_TEST, REMINDERS

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUnifiedCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        clinicalRepository = new ClinicalRepository(requireContext());
        doctorRepository = new DoctorDiscoveryAndRequestRepository(requireContext());
        operationsRepository = new HospitalOperationsRepository(requireContext());
        profileRepository = new ProfileRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        currentAccount = sessionManager.getCurrentSessionAccount();
        if (getArguments() != null) {
            isDoctorMode = getArguments().getBoolean(ARG_IS_DOCTOR_MODE, false);
        } else if (currentAccount != null) {
            isDoctorMode = (currentAccount.getUserRoleEnum() == UserRole.DOCTOR);
        }

        setupHeaderAndRoleMode();
        setupRecyclerView();
        setupDateControls();
        setupFilterChips();

        binding.btnBookAppointmentAction.setOnClickListener(v -> showBookAppointmentDialog());

        binding.btnOpenGoogleCalendarTheme.setOnClickListener(v -> {
            if (getActivity() instanceof com.arohi.healthcare.activities.RolePortalActivity) {
                ((com.arohi.healthcare.activities.RolePortalActivity) getActivity()).openSubFragment(
                        GoogleCalendarFragment.newInstance(isDoctorMode), "google_calendar_theme");
            } else {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, GoogleCalendarFragment.newInstance(isDoctorMode))
                        .addToBackStack("google_calendar_theme")
                        .commit();
            }
        });

        loadEventsForSelectedDate();
    }

    private void setupHeaderAndRoleMode() {
        if (isDoctorMode) {
            binding.tvCalendarHeaderTitle.setText("Clinical Consultation & Procedure Schedule");
            binding.tvCalendarHeaderSub.setText("Today's Patient Appointments, OT Surgeries & Hospital Rounds");
            binding.btnBookAppointmentAction.setText("+ Schedule");
        } else {
            binding.tvCalendarHeaderTitle.setText("Synchronized Medical Calendar");
            binding.tvCalendarHeaderSub.setText("Appointments, Lab Tests, Reminders & Follow-up Milestones");
            binding.btnBookAppointmentAction.setText("+ Book Slot");
        }
    }

    private void setupRecyclerView() {
        adapter = new CalendarEventAdapter(this::handleEventClicked);
        binding.rvCalendarEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCalendarEvents.setAdapter(adapter);
    }

    private void setupDateControls() {
        updateDateDisplay();

        binding.btnCalendarPrevDay.setOnClickListener(v -> {
            currentCalendar.add(Calendar.DAY_OF_YEAR, -1);
            updateDateDisplay();
            loadEventsForSelectedDate();
        });

        binding.btnCalendarNextDay.setOnClickListener(v -> {
            currentCalendar.add(Calendar.DAY_OF_YEAR, 1);
            updateDateDisplay();
            loadEventsForSelectedDate();
        });

        binding.btnPickSpecificDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        currentCalendar.set(Calendar.YEAR, year);
                        currentCalendar.set(Calendar.MONTH, month);
                        currentCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateDateDisplay();
                        loadEventsForSelectedDate();
                    },
                    currentCalendar.get(Calendar.YEAR),
                    currentCalendar.get(Calendar.MONTH),
                    currentCalendar.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });
    }

    private void updateDateDisplay() {
        Calendar today = Calendar.getInstance();
        boolean isToday = (today.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)
                && today.get(Calendar.DAY_OF_YEAR) == currentCalendar.get(Calendar.DAY_OF_YEAR));

        String dateStr = dateFormat.format(currentCalendar.getTime());
        String display = (isToday ? "Today • " : "") + dateStr;
        binding.tvSelectedDateDisplay.setText(display);
    }

    private void setupFilterChips() {
        binding.btnFilterAllEvents.setOnClickListener(v -> {
            selectedFilterType = "ALL";
            applyFilterSelection();
        });

        binding.btnFilterAppointments.setOnClickListener(v -> {
            selectedFilterType = CalendarEvent.TYPE_APPOINTMENT;
            applyFilterSelection();
        });

        binding.btnFilterLabTests.setOnClickListener(v -> {
            selectedFilterType = CalendarEvent.TYPE_LAB_TEST;
            applyFilterSelection();
        });

        binding.btnFilterReminders.setOnClickListener(v -> {
            selectedFilterType = "REMINDERS";
            applyFilterSelection();
        });
    }

    private void applyFilterSelection() {
        if (binding == null || !isAdded()) return;
        com.google.android.material.button.MaterialButton selectedBtn =
                CalendarEvent.TYPE_APPOINTMENT.equals(selectedFilterType) ? binding.btnFilterAppointments
                : (CalendarEvent.TYPE_LAB_TEST.equals(selectedFilterType) ? binding.btnFilterLabTests
                : ("REMINDERS".equals(selectedFilterType) ? binding.btnFilterReminders : binding.btnFilterAllEvents));

        UiStateHelper.updateToggleButtons(
                selectedBtn,
                R.color.white,
                binding.btnFilterAllEvents,
                binding.btnFilterAppointments,
                binding.btnFilterLabTests,
                binding.btnFilterReminders
        );

        List<CalendarEvent> filtered = new ArrayList<>();
        for (CalendarEvent e : allEventsForDate) {
            if ("ALL".equals(selectedFilterType)) {
                filtered.add(e);
            } else if (CalendarEvent.TYPE_APPOINTMENT.equals(selectedFilterType)
                    && CalendarEvent.TYPE_APPOINTMENT.equals(e.getEventType())) {
                filtered.add(e);
            } else if (CalendarEvent.TYPE_LAB_TEST.equals(selectedFilterType)
                    && CalendarEvent.TYPE_LAB_TEST.equals(e.getEventType())) {
                filtered.add(e);
            } else if ("REMINDERS".equals(selectedFilterType)
                    && (CalendarEvent.TYPE_MED_REMINDER.equals(e.getEventType())
                    || CalendarEvent.TYPE_FOLLOW_UP.equals(e.getEventType())
                    || CalendarEvent.TYPE_OPERATION.equals(e.getEventType()))) {
                filtered.add(e);
            }
        }

        adapter.submitList(filtered);
        if (filtered.isEmpty()) {
            binding.layoutCalendarEmpty.setVisibility(View.VISIBLE);
            binding.rvCalendarEvents.setVisibility(View.GONE);
            binding.tvCalendarEmptySub.setText("No " + selectedFilterType.toLowerCase().replace('_', ' ')
                    + " events scheduled for " + dateFormat.format(currentCalendar.getTime()) + ".");
        } else {
            binding.layoutCalendarEmpty.setVisibility(View.GONE);
            binding.rvCalendarEvents.setVisibility(View.VISIBLE);
        }
    }

    private void loadEventsForSelectedDate() {
        if (currentAccount == null) return;
        String filterDate = dateFormat.format(currentCalendar.getTime());

        clinicalRepository.loadUnifiedCalendarEvents(
                currentAccount.getUid(),
                currentAccount.getUserRoleEnum(),
                filterDate,
                new AuthCallback<List<CalendarEvent>>() {
                    @Override
                    public void onSuccess(List<CalendarEvent> result) {
                        if (binding == null) return;
                        allEventsForDate.clear();
                        allEventsForDate.addAll(result);
                        applyFilterSelection();
                    }

                    @Override
                    public void onError(String message) {
                        if (binding == null) return;
                        UiStateHelper.showError(binding.getRoot(), message);
                    }
                }
        );
    }

    private void handleEventClicked(@NonNull CalendarEvent event) {
        String info = "Title: " + event.getTitle() + "\n"
                + "Time: " + event.getEventDate() + " • " + event.getEventTime() + "\n"
                + "Type: " + event.getEventType() + "\n"
                + "Status: " + event.getStatus() + "\n"
                + "Location/Type: " + event.getDepartmentOrLocation() + "\n"
                + "Details: " + event.getDescription();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle("Calendar Event Details")
                .setMessage(info)
                .setNeutralButton("Close", null);

        // If this is an appointment event, allow direct Reschedule or Cancel actions!
        if (CalendarEvent.TYPE_APPOINTMENT.equals(event.getEventType()) && event.getReferenceId() != null) {
            Appointment appt = sessionManager.getAppointmentLocal(event.getReferenceId());
            if (appt != null && appt.isActive()) {
                builder.setPositiveButton("Reschedule", (d, w) -> showRescheduleDialog(appt));
                builder.setNegativeButton("Cancel Appt", (d, w) -> showCancelAppointmentDialog(appt));
            }
        }

        builder.show();
    }

    // =========================================================================
    // APPOINTMENT BOOKING DIALOG WITH CONFLICT DETECTION
    // =========================================================================
    private void showBookAppointmentDialog() {
        if (isDoctorMode) {
            String[] options = new String[]{
                    "📅 Schedule Patient Consultation / Follow-up",
                    "🏥 Schedule Hospital Operation (OT)"
            };
            new AlertDialog.Builder(requireContext())
                    .setTitle("Doctor Clinical Scheduling")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            showDoctorScheduleDialog();
                        } else {
                            showDoctorScheduleOtDialog();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            showPatientBookingDialog();
        }
    }

    private void showDoctorScheduleOtDialog() {
        if (currentAccount == null) return;
        DialogScheduleOperationBinding dBinding = DialogScheduleOperationBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dBinding.getRoot())
                .create();

        dBinding.tvScheduleOpPatientSubtitle.setText("Attending Surgeon: Dr. " + currentAccount.getFullName());
        dBinding.tilSelectOpPatient.setVisibility(View.VISIBLE);

        final List<ConsultationRequest> acceptedPatients = new ArrayList<>();
        final List<String> patientNames = new ArrayList<>();
        final ConsultationRequest[] chosenPatient = new ConsultationRequest[1];

        doctorRepository.loadDoctorRequests(currentAccount.getUid(), new AuthCallback<List<ConsultationRequest>>() {
            @Override
            public void onSuccess(List<ConsultationRequest> requests) {
                if (requests != null) {
                    for (ConsultationRequest r : requests) {
                        if (r.isAccepted()) {
                            acceptedPatients.add(r);
                            String name = r.getPatientName() != null ? r.getPatientName() : "Patient";
                            if (r.getPatientId() != null && !r.getPatientId().isEmpty()) {
                                name = name + " (" + r.getPatientId() + ")";
                            }
                            patientNames.add(name);
                        }
                    }
                    if (getContext() != null) {
                        ArrayAdapter<String> patAdapter = new ArrayAdapter<>(requireContext(),
                                android.R.layout.simple_dropdown_item_1line, patientNames);
                        dBinding.actvSelectOpPatient.setAdapter(patAdapter);
                        if (!patientNames.isEmpty()) {
                            chosenPatient[0] = acceptedPatients.get(0);
                            dBinding.actvSelectOpPatient.setText(patientNames.get(0), false);
                            dBinding.tvScheduleOpPatientSubtitle.setText("Patient: " + patientNames.get(0)
                                    + " • Attending Surgeon: Dr. " + currentAccount.getFullName());
                        }
                        dBinding.actvSelectOpPatient.setOnItemClickListener((parent, view, pos, id) -> {
                            if (pos < acceptedPatients.size()) {
                                chosenPatient[0] = acceptedPatients.get(pos);
                                dBinding.tvScheduleOpPatientSubtitle.setText("Patient: " + patientNames.get(pos)
                                        + " • Attending Surgeon: Dr. " + currentAccount.getFullName());
                            }
                        });
                    }
                }
            }

            @Override
            public void onError(String message) {}
        });

        final List<com.arohi.healthcare.models.OTRoom> roomOptions = new ArrayList<>();
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

        operationsRepository.loadOTRooms(new AuthCallback<List<com.arohi.healthcare.models.OTRoom>>() {
            @Override
            public void onSuccess(List<com.arohi.healthcare.models.OTRoom> rooms) {
                if (getContext() == null || rooms == null || rooms.isEmpty()) return;
                roomOptions.clear();
                roomOptions.addAll(rooms);
                List<String> names = new ArrayList<>();
                for (com.arohi.healthcare.models.OTRoom r : rooms) names.add(r.getRoomName());
                ArrayAdapter<String> dynamicOt = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, names);
                dBinding.actvSelectOtRoom.setAdapter(dynamicOt);
                dBinding.actvSelectOtRoom.setText(names.get(names.size() > 1 ? 1 : 0), false);
            }
            @Override public void onError(String message) {}
        });

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        dBinding.etOpDate.setText(df.format(currentCalendar.getTime()));
        dBinding.etOpDate.setOnClickListener(v -> {
            DatePickerHelper.showDatePicker(requireContext(), (y, m, d) -> {
                dBinding.etOpDate.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d));
            });
        });

        String[] timeSlots = new String[]{
                "09:00 AM", "10:00 AM", "11:30 AM", "02:00 PM", "04:00 PM", "06:00 PM"
        };
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, timeSlots);
        dBinding.actvOpTime.setAdapter(timeAdapter);
        dBinding.actvOpTime.setText(timeSlots[1], false);

        final List<UserAccount> nurseOptions = new ArrayList<>();
        final UserAccount[] selectedNurse = new UserAccount[1];
        operationsRepository.loadStaffAndPatientDirectory(new AuthCallback<HospitalOperationsRepository.StaffAndPatientDirectory>() {
            @Override
            public void onSuccess(HospitalOperationsRepository.StaffAndPatientDirectory dir) {
                if (getContext() == null) return;
                nurseOptions.clear();
                nurseOptions.addAll(dir.nurses);
                List<String> nurseLabels = new ArrayList<>();
                for (UserAccount acc : nurseOptions) {
                    nurseLabels.add(HospitalOperationsRepository.formatAccountOptionLabel(acc));
                }
                ArrayAdapter<String> nurseAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, nurseLabels);
                dBinding.actvSelectNurse.setAdapter(nurseAdapter);
                if (!nurseOptions.isEmpty() && selectedNurse[0] == null) {
                    selectedNurse[0] = nurseOptions.get(0);
                    dBinding.actvSelectNurse.setText(nurseLabels.get(0), false);
                }
                dBinding.actvSelectNurse.setOnItemClickListener((parent, view, pos, id) -> {
                    if (pos < nurseOptions.size()) selectedNurse[0] = nurseOptions.get(pos);
                });
            }
            @Override public void onError(String message) {}
        });

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

            if (acceptedPatients.isEmpty()) {
                UiStateHelper.showError(dBinding.getRoot(), "No accepted patients available for surgical scheduling.");
                return;
            }

            ConsultationRequest pReq = chosenPatient[0] != null ? chosenPatient[0] : acceptedPatients.get(0);
            String selectedOtText = dBinding.actvSelectOtRoom.getText().toString();
            String otRoomId = "ot_01";
            String otRoom = selectedOtText;

            for (com.arohi.healthcare.models.OTRoom r : roomOptions) {
                if (selectedOtText.contains(r.getRoomName()) || selectedOtText.contains(r.getRoomId())) {
                    otRoomId = r.getRoomId();
                    otRoom = r.getRoomName();
                    break;
                }
            }
            if (selectedOtText.contains("Suite 1") || selectedOtText.contains("Cardiac")) {
                otRoomId = "ot_01";
                otRoom = "OT Suite 1 - Cardiac Surgery";
            } else if (selectedOtText.contains("Suite 2") || selectedOtText.contains("General")) {
                otRoomId = "ot_02";
                otRoom = "OT Suite 2 - General & Laparoscopic";
            } else if (selectedOtText.contains("Suite 3") || selectedOtText.contains("Orthopedic")) {
                otRoomId = "ot_03";
                otRoom = "OT Suite 3 - Orthopedic & Trauma";
            } else if (selectedOtText.contains("Suite 4") || selectedOtText.contains("Neuro")) {
                otRoomId = "ot_04";
                otRoom = "OT Suite 4 - Neuro & Micro-Surgery";
            }
            String date = dBinding.etOpDate.getText().toString().trim();
            String time = dBinding.actvOpTime.getText().toString().trim();
            String nurseUid = selectedNurse[0] != null ? selectedNurse[0].getUid() : "nurse_demo_01";
            String nurseName = selectedNurse[0] != null ? selectedNurse[0].getFullName() : dBinding.actvSelectNurse.getText().toString().trim();
            String followUp = dBinding.etFollowUpDate.getText() != null
                    ? dBinding.etFollowUpDate.getText().toString().trim() : "";

            Operation op = new Operation(
                    "",
                    proc,
                    pReq.getDoctorSpecialty() != null ? pReq.getDoctorSpecialty() : "General & Laparoscopic Surgery",
                    otRoomId,
                    otRoom,
                    date,
                    time,
                    90,
                    pReq.getPatientUid(),
                    pReq.getPatientId() != null ? pReq.getPatientId() : "",
                    pReq.getPatientName() != null ? pReq.getPatientName() : "Patient",
                    currentAccount.getUid(),
                    currentAccount.getArohiId(),
                    currentAccount.getFullName(),
                    pReq.getDoctorSpecialty() != null ? pReq.getDoctorSpecialty() : "General Surgery",
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
                                "Operation '" + result.getProcedureName() + "' scheduled successfully on " + result.getOperationDate());
                        loadEventsForSelectedDate();
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

    /**
     * Doctor mode: Schedule an appointment with one of their accepted patients.
     * Hides "Select Doctor" and shows "Select Patient" dropdown instead.
     */
    private void showDoctorScheduleDialog() {
        DialogBookAppointmentBinding dBinding = DialogBookAppointmentBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dBinding.getRoot())
                .create();

        dBinding.tvBookApptDialogTitle.setText("Schedule Patient Appointment");
        dBinding.tvBookApptDoctorSummary.setText("Doctor: Dr. " + currentAccount.getFullName());

        // Repurpose the doctor selection dropdown as a patient selector
        dBinding.tilSelectDoctor.setHint("Select Patient");

        // Time slots
        String[] timeSlots = new String[]{
                "09:00 AM", "09:30 AM", "10:00 AM", "10:30 AM", "11:00 AM", "11:30 AM",
                "04:00 PM", "04:30 PM", "05:00 PM", "05:30 PM", "06:00 PM", "06:30 PM",
                "07:00 PM", "07:30 PM", "08:00 PM"
        };
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, timeSlots);
        dBinding.actvAppointmentTime.setAdapter(timeAdapter);
        dBinding.actvAppointmentTime.setText(timeSlots[2], false);

        // Consultation types
        String[] types = new String[]{
                Appointment.TYPE_IN_PERSON,
                Appointment.TYPE_VIDEO_CALL,
                Appointment.TYPE_FOLLOW_UP
        };
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, types);
        dBinding.actvConsultationType.setAdapter(typeAdapter);
        dBinding.actvConsultationType.setText(types[0], false);

        // Date
        dBinding.etAppointmentDate.setText(dateFormat.format(currentCalendar.getTime()));
        dBinding.etAppointmentDate.setOnClickListener(v -> {
            DatePickerHelper.showDatePicker(requireContext(), (y, m, d) -> {
                String picked = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);
                dBinding.etAppointmentDate.setText(picked);
            });
        });

        // Load accepted patients for this doctor
        final List<ConsultationRequest> acceptedPatients = new ArrayList<>();
        final List<String> patientNames = new ArrayList<>();

        doctorRepository.loadDoctorRequests(currentAccount.getUid(), new AuthCallback<List<ConsultationRequest>>() {
            @Override
            public void onSuccess(List<ConsultationRequest> requests) {
                if (requests != null) {
                    for (ConsultationRequest r : requests) {
                        if (r.isAccepted()) {
                            acceptedPatients.add(r);
                            String name = r.getPatientName();
                            if (name == null || name.isEmpty()) name = "Patient";
                            String patientId = r.getPatientId();
                            if (patientId != null && !patientId.isEmpty()) {
                                name = name + " (" + patientId + ")";
                            }
                            patientNames.add(name);
                        }
                    }
                    if (getContext() != null) {
                        ArrayAdapter<String> patientAdapter = new ArrayAdapter<>(requireContext(),
                                android.R.layout.simple_dropdown_item_1line, patientNames);
                        dBinding.actvSelectDoctor.setAdapter(patientAdapter);
                        if (!patientNames.isEmpty()) {
                            dBinding.actvSelectDoctor.setText(patientNames.get(0), false);
                            dBinding.tvBookApptDoctorSummary.setText(
                                    "Doctor: Dr. " + currentAccount.getFullName() + " • Patient: " + patientNames.get(0));
                        } else {
                            dBinding.tvBookApptDoctorSummary.setText(
                                    "Doctor: Dr. " + currentAccount.getFullName() + " • No accepted patients");
                        }
                    }
                }
            }

            @Override
            public void onError(String message) {
                // Keep default
            }
        });

        dBinding.actvSelectDoctor.setOnItemClickListener((parent, view, position, id) -> {
            if (position < patientNames.size()) {
                dBinding.tvBookApptDoctorSummary.setText(
                        "Doctor: Dr. " + currentAccount.getFullName() + " • Patient: " + patientNames.get(position));
            }
        });

        dBinding.btnCancelBookAppt.setOnClickListener(v -> dialog.dismiss());
        dBinding.btnSubmitBookAppt.setText("Confirm");

        dBinding.btnSubmitBookAppt.setOnClickListener(v -> {
            String date = dBinding.etAppointmentDate.getText() != null
                    ? dBinding.etAppointmentDate.getText().toString().trim() : "";
            String time = dBinding.actvAppointmentTime.getText() != null
                    ? dBinding.actvAppointmentTime.getText().toString().trim() : "";
            String type = dBinding.actvConsultationType.getText() != null
                    ? dBinding.actvConsultationType.getText().toString().trim() : Appointment.TYPE_IN_PERSON;
            String reason = dBinding.etAppointmentReason.getText() != null
                    ? dBinding.etAppointmentReason.getText().toString().trim() : "";
            String notes = dBinding.etAppointmentNotes.getText() != null
                    ? dBinding.etAppointmentNotes.getText().toString().trim() : "";

            if (reason.isEmpty()) {
                reason = "Scheduled Clinical Consultation";
            }

            // Find chosen patient
            ConsultationRequest chosenPatient = null;
            String patText = dBinding.actvSelectDoctor.getText() != null
                    ? dBinding.actvSelectDoctor.getText().toString() : "";
            for (int i = 0; i < patientNames.size(); i++) {
                if (patientNames.get(i).equals(patText) && i < acceptedPatients.size()) {
                    chosenPatient = acceptedPatients.get(i);
                    break;
                }
            }
            if (chosenPatient == null && !acceptedPatients.isEmpty()) {
                chosenPatient = acceptedPatients.get(0);
            }

            if (chosenPatient == null) {
                UiStateHelper.showError(dBinding.getRoot(), "No accepted patients to schedule. Accept patient requests first.");
                return;
            }

            // Build appointment: patient info from the chosen patient, doctor info from current account
            Appointment newAppt = new Appointment(
                    "",
                    chosenPatient.getPatientUid(),
                    chosenPatient.getPatientId() != null ? chosenPatient.getPatientId() : "",
                    chosenPatient.getPatientName() != null ? chosenPatient.getPatientName() : "Patient",
                    chosenPatient.getPatientPhone() != null ? chosenPatient.getPatientPhone() : "",
                    currentAccount.getUid(),
                    currentAccount.getArohiId(),
                    currentAccount.getFullName(),
                    chosenPatient.getDoctorSpecialty() != null ? chosenPatient.getDoctorSpecialty() : "General Medicine",
                    chosenPatient.getConsultationFee(),
                    date,
                    time,
                    type,
                    reason,
                    notes
            );
            newAppt.setStatus(Appointment.STATUS_CONFIRMED);

            dBinding.btnSubmitBookAppt.setEnabled(false);
            clinicalRepository.requestAppointment(newAppt, new AuthCallback<Appointment>() {
                @Override
                public void onSuccess(Appointment result) {
                    dialog.dismiss();
                    if (binding != null) {
                        UiStateHelper.showSuccess(binding.getRoot(),
                                "Appointment scheduled for " + result.getPatientName() + " on " + result.getAppointmentDate() + " at " + result.getAppointmentTime() + "!");
                        loadEventsForSelectedDate();
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

    /**
     * Patient mode: Book a consultation with a verified doctor.
     */
    private void showPatientBookingDialog() {
        DialogBookAppointmentBinding dBinding = DialogBookAppointmentBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dBinding.getRoot())
                .create();

        // 1. Setup Time Slots
        String[] timeSlots = new String[]{
                "09:00 AM", "09:30 AM", "10:00 AM", "10:30 AM", "11:00 AM", "11:30 AM",
                "04:00 PM", "04:30 PM", "05:00 PM", "05:30 PM", "06:00 PM", "06:30 PM",
                "07:00 PM", "07:30 PM", "08:00 PM"
        };
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, timeSlots);
        dBinding.actvAppointmentTime.setAdapter(timeAdapter);
        dBinding.actvAppointmentTime.setText(timeSlots[3], false); // Default 10:30 AM

        // 2. Setup Consultation Types
        String[] types = new String[]{
                Appointment.TYPE_IN_PERSON,
                Appointment.TYPE_VIDEO_CALL,
                Appointment.TYPE_FOLLOW_UP
        };
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, types);
        dBinding.actvConsultationType.setAdapter(typeAdapter);
        dBinding.actvConsultationType.setText(types[0], false);

        // 3. Pre-fill Date with current calendar date
        dBinding.etAppointmentDate.setText(dateFormat.format(currentCalendar.getTime()));
        dBinding.etAppointmentDate.setOnClickListener(v -> {
            DatePickerHelper.showDatePicker(requireContext(), (y, m, d) -> {
                String picked = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);
                dBinding.etAppointmentDate.setText(picked);
            });
        });

        // 4. Setup Doctor Selection
        final List<DoctorProfile> doctorList = new ArrayList<>();
        final List<String> doctorNames = new ArrayList<>();

        doctorRepository.loadVerifiedDoctors(new AuthCallback<List<DoctorProfile>>() {
            @Override
            public void onSuccess(List<DoctorProfile> docs) {
                if (docs != null) {
                    doctorList.addAll(docs);
                    for (DoctorProfile doc : docs) {
                        doctorNames.add(doc.getFullName() + " (" + doc.getSpecialty() + " • "
                                + String.format("%.0f", doc.getConsultationFeeBdt()) + " BDT)");
                    }
                    if (getContext() != null) {
                        ArrayAdapter<String> docAdapter = new ArrayAdapter<>(requireContext(),
                                android.R.layout.simple_dropdown_item_1line, doctorNames);
                        dBinding.actvSelectDoctor.setAdapter(docAdapter);
                        if (!doctorNames.isEmpty()) {
                            dBinding.actvSelectDoctor.setText(doctorNames.get(0), false);
                            dBinding.tvBookApptDoctorSummary.setText("Selected: " + doctorNames.get(0));
                        }
                    }
                }
            }

            @Override
            public void onError(String message) {
                // Keep default
            }
        });

        dBinding.actvSelectDoctor.setOnItemClickListener((parent, view, position, id) -> {
            if (position < doctorList.size()) {
                DoctorProfile selected = doctorList.get(position);
                dBinding.tvBookApptDoctorSummary.setText("Doctor: " + selected.getFullName()
                        + " • Fee: " + String.format("%.0f", selected.getConsultationFeeBdt()) + " BDT");
            }
        });

        dBinding.btnCancelBookAppt.setOnClickListener(v -> dialog.dismiss());

        dBinding.btnSubmitBookAppt.setOnClickListener(v -> {
            String date = dBinding.etAppointmentDate.getText() != null
                    ? dBinding.etAppointmentDate.getText().toString().trim() : "";
            String time = dBinding.actvAppointmentTime.getText() != null
                    ? dBinding.actvAppointmentTime.getText().toString().trim() : "";
            String type = dBinding.actvConsultationType.getText() != null
                    ? dBinding.actvConsultationType.getText().toString().trim() : Appointment.TYPE_IN_PERSON;
            String reason = dBinding.etAppointmentReason.getText() != null
                    ? dBinding.etAppointmentReason.getText().toString().trim() : "";
            String notes = dBinding.etAppointmentNotes.getText() != null
                    ? dBinding.etAppointmentNotes.getText().toString().trim() : "";

            if (reason.isEmpty()) {
                dBinding.tilAppointmentReason.setError("Please enter the reason for consultation");
                return;
            }
            dBinding.tilAppointmentReason.setError(null);

            DoctorProfile chosenDoctor = null;
            String docText = dBinding.actvSelectDoctor.getText() != null
                    ? dBinding.actvSelectDoctor.getText().toString() : "";
            for (int i = 0; i < doctorNames.size(); i++) {
                if (doctorNames.get(i).equals(docText) && i < doctorList.size()) {
                    chosenDoctor = doctorList.get(i);
                    break;
                }
            }
            if (chosenDoctor == null && !doctorList.isEmpty()) {
                chosenDoctor = doctorList.get(0);
            }

            if (chosenDoctor == null) {
                UiStateHelper.showError(dBinding.getRoot(), "Please select an authorized Doctor.");
                return;
            }

            // Build appointment model
            Appointment newAppt = new Appointment(
                    "",
                    currentAccount.getUid(),
                    currentAccount.getArohiId(),
                    currentAccount.getFullName(),
                    currentAccount.getPhone(),
                    chosenDoctor.getUid(),
                    chosenDoctor.getArohiId(),
                    chosenDoctor.getFullName(),
                    chosenDoctor.getSpecialty(),
                    chosenDoctor.getConsultationFeeBdt(),
                    date,
                    time,
                    type,
                    reason,
                    notes
            );

            dBinding.btnSubmitBookAppt.setEnabled(false);
            clinicalRepository.requestAppointment(newAppt, new AuthCallback<Appointment>() {
                @Override
                public void onSuccess(Appointment result) {
                    dialog.dismiss();
                    if (binding != null) {
                        UiStateHelper.showSuccess(binding.getRoot(),
                                "Appointment booked with Dr. " + result.getDoctorName() + " on " + result.getAppointmentDate() + " at " + result.getAppointmentTime() + "!");
                        loadEventsForSelectedDate();
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

    private void showRescheduleDialog(@NonNull Appointment appt) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad / 2, pad, 0);

        EditText etDate = new EditText(requireContext());
        etDate.setHint("New Date (YYYY-MM-DD)");
        etDate.setText(appt.getAppointmentDate());
        etDate.setFocusable(false);
        etDate.setOnClickListener(v -> DatePickerHelper.showDatePicker(requireContext(), (y, m, d) -> {
            etDate.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d));
        }));
        layout.addView(etDate);

        EditText etTime = new EditText(requireContext());
        etTime.setHint("New Time (e.g. 11:30 AM, 05:00 PM)");
        etTime.setText("11:30 AM");
        layout.addView(etTime);

        EditText etReason = new EditText(requireContext());
        etReason.setHint("Reason for rescheduling (optional)");
        etReason.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        layout.addView(etReason);

        new AlertDialog.Builder(requireContext())
                .setTitle("Reschedule Appointment")
                .setMessage("Select a new date and time slot for your consultation with "
                        + (isDoctorMode ? appt.getPatientName() : "Dr. " + appt.getDoctorName()) + ".")
                .setView(layout)
                .setPositiveButton("Confirm Reschedule", (d, w) -> {
                    String nDate = etDate.getText().toString().trim();
                    String nTime = etTime.getText().toString().trim();
                    String reason = etReason.getText().toString().trim();

                    clinicalRepository.rescheduleAppointment(appt, nDate, nTime, reason, new AuthCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            if (binding == null) return;
                            UiStateHelper.showSuccess(binding.getRoot(), "Appointment rescheduled to " + nDate + " at " + nTime + ".");
                            loadEventsForSelectedDate();
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

    private void showCancelAppointmentDialog(@NonNull Appointment appt) {
        EditText etReason = new EditText(requireContext());
        etReason.setHint("Reason for cancellation (optional)");
        int pad = (int) (18 * getResources().getDisplayMetrics().density);

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, pad / 2, pad, 0);
        layout.addView(etReason);

        new AlertDialog.Builder(requireContext())
                .setTitle("Cancel Appointment")
                .setMessage("Are you sure you want to cancel the scheduled appointment on "
                        + appt.getAppointmentDate() + " at " + appt.getAppointmentTime() + "?")
                .setView(layout)
                .setPositiveButton("Confirm Cancel", (d, w) -> {
                    String reason = etReason.getText() != null ? etReason.getText().toString().trim() : "Cancelled by user";
                    clinicalRepository.updateAppointmentStatus(
                            appt,
                            Appointment.STATUS_CANCELLED,
                            null,
                            reason,
                            new AuthCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    if (binding == null) return;
                                    UiStateHelper.showSuccess(binding.getRoot(), "Appointment cancelled.");
                                    loadEventsForSelectedDate();
                                }

                                @Override
                                public void onError(String message) {
                                    if (binding == null) return;
                                    UiStateHelper.showError(binding.getRoot(), message);
                                }
                            }
                    );
                })
                .setNegativeButton("Keep Appointment", null)
                .show();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && binding != null && isAdded()) {
            loadEventsForSelectedDate();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && isAdded()) {
            loadEventsForSelectedDate();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
