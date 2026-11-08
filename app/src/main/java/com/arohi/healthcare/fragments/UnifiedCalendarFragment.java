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
import com.arohi.healthcare.databinding.FragmentUnifiedCalendarBinding;
import com.arohi.healthcare.models.Appointment;
import com.arohi.healthcare.models.CalendarEvent;
import com.arohi.healthcare.models.DoctorProfile;
import com.arohi.healthcare.models.PatientProfile;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.ClinicalRepository;
import com.arohi.healthcare.repositories.DoctorDiscoveryAndRequestRepository;
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
            binding.btnBookAppointmentAction.setText("+ Schedule Appt");
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
        if (!hidden) {
            loadEventsForSelectedDate();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadEventsForSelectedDate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
