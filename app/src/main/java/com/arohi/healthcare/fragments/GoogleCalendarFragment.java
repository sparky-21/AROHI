package com.arohi.healthcare.fragments;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.adapters.GoogleCalendarEventAdapter;
import com.arohi.healthcare.databinding.FragmentGoogleCalendarBinding;
import com.arohi.healthcare.models.CalendarEvent;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.ClinicalRepository;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Fragment rendering the Google Calendar themed medical schedule view.
 * Displays week ribbon day selector, left time gutter, colored event categories,
 * and seamless event synchronization.
 */
public class GoogleCalendarFragment extends Fragment {

    private static final String ARG_IS_DOCTOR_MODE = "arg_is_doctor_mode";

    public static GoogleCalendarFragment newInstance(boolean isDoctorMode) {
        GoogleCalendarFragment fragment = new GoogleCalendarFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_DOCTOR_MODE, isDoctorMode);
        fragment.setArguments(args);
        return fragment;
    }

    private FragmentGoogleCalendarBinding binding;
    private ClinicalRepository clinicalRepository;
    private SessionManager sessionManager;
    private UserAccount currentAccount;
    private boolean isDoctorMode = false;

    private GoogleCalendarEventAdapter adapter;
    private Calendar selectedDate = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.US);
    private final SimpleDateFormat dayHeaderFormat = new SimpleDateFormat("EEEE, MMM d", Locale.US);

    private final List<CalendarEvent> allEventsForDate = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGoogleCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        clinicalRepository = new ClinicalRepository(requireContext());
        sessionManager = new SessionManager(requireContext());
        currentAccount = sessionManager.getCurrentSessionAccount();

        if (getArguments() != null) {
            isDoctorMode = getArguments().getBoolean(ARG_IS_DOCTOR_MODE, false);
        } else if (currentAccount != null) {
            isDoctorMode = (currentAccount.getUserRoleEnum() == UserRole.DOCTOR);
        }

        setupRecyclerView();
        setupControls();
        renderWeekRibbon();
        loadEvents();
    }

    private void setupRecyclerView() {
        binding.recyclerGoogleCalendar.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new GoogleCalendarEventAdapter(event -> showEventDetailsDialog(event));
        binding.recyclerGoogleCalendar.setAdapter(adapter);
    }

    private void setupControls() {
        binding.btnGoogleCalendarBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        binding.btnSwitchToStandardCalendar.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        binding.fabGoogleBookEvent.setOnClickListener(v -> {
            UiStateHelper.showSuccess(binding.getRoot(), "Opening consultation booking dialog...");
            // Switch to UnifiedCalendarFragment or open booking dialog
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private void renderWeekRibbon() {
        if (binding == null) return;

        binding.tvGoogleMonthTitle.setText(monthYearFormat.format(selectedDate.getTime()));
        binding.tvGoogleSelectedDayFull.setText(dayHeaderFormat.format(selectedDate.getTime()));

        binding.layoutGoogleWeekRibbon.removeAllViews();

        Calendar cal = (Calendar) selectedDate.clone();
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek()); // Start of week (Sunday)

        String[] dayLetters = new String[]{"S", "M", "T", "W", "T", "F", "S"};

        for (int i = 0; i < 7; i++) {
            final Calendar dayCal = (Calendar) cal.clone();

            LinearLayout dayCol = new LinearLayout(requireContext());
            dayCol.setOrientation(LinearLayout.VERTICAL);
            dayCol.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            dayCol.setLayoutParams(params);
            dayCol.setPadding(0, 4, 0, 4);

            // Day letter
            TextView tvLetter = new TextView(requireContext());
            tvLetter.setText(dayLetters[i]);
            tvLetter.setTextColor(Color.parseColor("#70757A"));
            tvLetter.setTextSize(11f);
            tvLetter.setGravity(Gravity.CENTER);
            dayCol.addView(tvLetter);

            // Date circle
            TextView tvDayNum = new TextView(requireContext());
            tvDayNum.setText(String.valueOf(dayCal.get(Calendar.DAY_OF_MONTH)));
            tvDayNum.setTextSize(14f);
            tvDayNum.setGravity(Gravity.CENTER);
            tvDayNum.setPadding(8, 8, 8, 8);

            boolean isSelected = (dayCal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR)
                    && dayCal.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR));

            if (isSelected) {
                GradientDrawable circle = new GradientDrawable();
                circle.setShape(GradientDrawable.OVAL);
                circle.setColor(Color.parseColor("#1A73E8")); // Google Blue
                tvDayNum.setBackground(circle);
                tvDayNum.setTextColor(Color.WHITE);
                tvDayNum.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                tvDayNum.setBackground(null);
                tvDayNum.setTextColor(Color.parseColor("#202124"));
            }

            dayCol.addView(tvDayNum);

            dayCol.setOnClickListener(v -> {
                selectedDate = dayCal;
                renderWeekRibbon();
                loadEvents();
            });

            binding.layoutGoogleWeekRibbon.addView(dayCol);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void loadEvents() {
        if (currentAccount == null) return;
        String dateStr = dateFormat.format(selectedDate.getTime());

        clinicalRepository.loadUnifiedCalendarEvents(
                currentAccount.getUid(),
                isDoctorMode ? UserRole.DOCTOR : UserRole.PATIENT,
                dateStr,
                new AuthCallback<List<CalendarEvent>>() {
                    @Override
                    public void onSuccess(List<CalendarEvent> result) {
                        if (binding == null) return;
                        allEventsForDate.clear();
                        allEventsForDate.addAll(result);

                        adapter.submitList(allEventsForDate);

                        if (allEventsForDate.isEmpty()) {
                            binding.layoutGoogleCalendarEmpty.setVisibility(View.VISIBLE);
                            binding.recyclerGoogleCalendar.setVisibility(View.GONE);
                        } else {
                            binding.layoutGoogleCalendarEmpty.setVisibility(View.GONE);
                            binding.recyclerGoogleCalendar.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (binding != null) {
                            UiStateHelper.showError(binding.getRoot(), message);
                        }
                    }
                }
        );
    }

    private void showEventDetailsDialog(@NonNull CalendarEvent event) {
        String details = "Title: " + event.getTitle() + "\n"
                + "Date: " + event.getEventDate() + " • " + event.getEventTime() + "\n"
                + "Category: " + event.getEventType() + "\n"
                + "Location: " + (event.getDepartmentOrLocation() != null ? event.getDepartmentOrLocation() : "Hospital Clinic") + "\n"
                + "Description: " + event.getDescription() + "\n"
                + "Status: " + event.getStatus();

        new AlertDialog.Builder(requireContext())
                .setTitle("Google Calendar Event Details")
                .setMessage(details)
                .setPositiveButton("Close", null)
                .show();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            loadEvents();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded()) {
            loadEvents();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
