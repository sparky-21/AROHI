package com.arohi.healthcare.fragments;

import android.os.Bundle;
import android.text.InputType;
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
import com.arohi.healthcare.adapters.NurseTaskAdapter;
import com.arohi.healthcare.databinding.DialogRecordVitalsBinding;
import com.arohi.healthcare.databinding.FragmentNurseTasksBinding;
import com.arohi.healthcare.models.NursingTask;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.VitalObservation;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.HospitalOperationsRepository;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Nurse Bedside Tasks, Medication Schedules, Procedures & Doctor Instructions Fragment.
 */
public class NurseTasksFragment extends Fragment {

    private FragmentNurseTasksBinding binding;
    private HospitalOperationsRepository operationsRepository;
    private SessionManager sessionManager;
    private NurseTaskAdapter adapter;

    private UserAccount currentAccount;
    private final List<NursingTask> allTasks = new ArrayList<>();
    private String currentFilter = "ALL"; // ALL, MEDS, VITALS, PROCEDURES, PENDING

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNurseTasksBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        operationsRepository = new HospitalOperationsRepository(requireContext());
        sessionManager = new SessionManager(requireContext());
        currentAccount = sessionManager.getCurrentSessionAccount();

        adapter = new NurseTaskAdapter(this::showCompleteTaskDialog);
        binding.rvNurseTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNurseTasks.setAdapter(adapter);

        setupFilterButtons();

        binding.btnQuickRecordVitals.setOnClickListener(v -> showQuickVitalsDialog());

        loadTasks();
    }

    private void setupFilterButtons() {
        binding.btnFilterTasksAll.setOnClickListener(v -> applyFilter("ALL"));
        binding.btnFilterTasksMeds.setOnClickListener(v -> applyFilter("MEDS"));
        binding.btnFilterTasksVitals.setOnClickListener(v -> applyFilter("VITALS"));
        binding.btnFilterTasksProcedures.setOnClickListener(v -> applyFilter("PROCEDURES"));
        binding.btnFilterTasksPending.setOnClickListener(v -> applyFilter("PENDING"));
    }

    private void loadTasks() {
        String uid = currentAccount != null ? currentAccount.getUid() : null;

        operationsRepository.loadTasksForNurse(uid, new AuthCallback<List<NursingTask>>() {
            @Override
            public void onSuccess(List<NursingTask> tasks) {
                if (binding == null) return;
                allTasks.clear();
                allTasks.addAll(tasks);
                applyFilter(currentFilter);
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    private void applyFilter(String filterKey) {
        if (binding == null) return;
        this.currentFilter = filterKey;

        MaterialButton selectedBtn = binding.btnFilterTasksAll;
        if ("MEDS".equals(filterKey)) selectedBtn = binding.btnFilterTasksMeds;
        else if ("VITALS".equals(filterKey)) selectedBtn = binding.btnFilterTasksVitals;
        else if ("PROCEDURES".equals(filterKey)) selectedBtn = binding.btnFilterTasksProcedures;
        else if ("PENDING".equals(filterKey)) selectedBtn = binding.btnFilterTasksPending;

        UiStateHelper.updateToggleButtons(
                selectedBtn,
                R.color.primary_moss_green,
                binding.btnFilterTasksAll,
                binding.btnFilterTasksMeds,
                binding.btnFilterTasksVitals,
                binding.btnFilterTasksProcedures,
                binding.btnFilterTasksPending
        );

        List<NursingTask> filtered = new ArrayList<>();
        for (NursingTask t : allTasks) {
            if ("MEDS".equals(filterKey)) {
                if (NursingTask.TYPE_MEDICATION.equalsIgnoreCase(t.getTaskType())) filtered.add(t);
            } else if ("VITALS".equals(filterKey)) {
                if (NursingTask.TYPE_VITALS.equalsIgnoreCase(t.getTaskType())) filtered.add(t);
            } else if ("PROCEDURES".equals(filterKey)) {
                if (NursingTask.TYPE_PROCEDURE.equalsIgnoreCase(t.getTaskType())
                        || NursingTask.TYPE_DRESSING.equalsIgnoreCase(t.getTaskType())
                        || NursingTask.TYPE_IV_FLUID.equalsIgnoreCase(t.getTaskType())) filtered.add(t);
            } else if ("PENDING".equals(filterKey)) {
                if (!t.isCompleted()) filtered.add(t);
            } else {
                filtered.add(t);
            }
        }

        adapter.submitList(filtered);
        binding.tvEmptyNurseTasks.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showCompleteTaskDialog(@NonNull NursingTask task) {
        EditText etNotes = new EditText(requireContext());
        etNotes.setHint("Nurse notes (e.g. Administered as prescribed. Vitals normal.)");
        etNotes.setText("Administered as prescribed. Patient tolerated well.");
        etNotes.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, pad / 2, pad, 0);
        layout.addView(etNotes);

        new AlertDialog.Builder(requireContext())
                .setTitle("Mark Task Completed")
                .setMessage("Task: " + task.getTitle() + "\nPatient: " + task.getPatientName()
                        + " (" + task.getWardName() + " " + task.getBedNumber() + ")")
                .setView(layout)
                .setPositiveButton("Confirm Completion", (d, w) -> {
                    String notes = etNotes.getText() != null ? etNotes.getText().toString().trim() : "Completed";
                    operationsRepository.completeNursingTask(task.getTaskId(), notes, new AuthCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            if (binding == null) return;
                            UiStateHelper.showSuccess(binding.getRoot(),
                                    "Task marked completed: " + task.getTitle());
                            loadTasks();
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

    private void showQuickVitalsDialog() {
        DialogRecordVitalsBinding dBinding = DialogRecordVitalsBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dBinding.getRoot())
                .create();

        dBinding.tvRecordVitalsPatientSub.setText("Ward A - Male Surgical • Inpatient Telemetry");

        dBinding.btnCancelVitals.setOnClickListener(v -> dialog.dismiss());

        dBinding.btnSaveVitals.setOnClickListener(v -> {
            String bp = dBinding.etVitalsBp.getText() != null ? dBinding.etVitalsBp.getText().toString().trim() : "120/80";
            String hrStr = dBinding.etVitalsHr.getText() != null ? dBinding.etVitalsHr.getText().toString().trim() : "76";
            String tempStr = dBinding.etVitalsTemp.getText() != null ? dBinding.etVitalsTemp.getText().toString().trim() : "98.6";
            String spO2Str = dBinding.etVitalsSpO2.getText() != null ? dBinding.etVitalsSpO2.getText().toString().trim() : "98";
            String rrStr = dBinding.etVitalsRr.getText() != null ? dBinding.etVitalsRr.getText().toString().trim() : "18";
            String notes = dBinding.etVitalsNotes.getText() != null ? dBinding.etVitalsNotes.getText().toString().trim() : "Vitals stable.";

            int hr = 76;
            double temp = 98.6;
            int spO2 = 98;
            int rr = 18;
            try { hr = Integer.parseInt(hrStr); } catch (Exception ignored) {}
            try { temp = Double.parseDouble(tempStr); } catch (Exception ignored) {}
            try { spO2 = Integer.parseInt(spO2Str); } catch (Exception ignored) {}
            try { rr = Integer.parseInt(rrStr); } catch (Exception ignored) {}

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat tf = new SimpleDateFormat("hh:mm a", Locale.US);
            Date now = new Date();

            String nurseName = currentAccount != null ? currentAccount.getFullName() : "Sister Fatima";
            String nurseUid = currentAccount != null ? currentAccount.getUid() : "nurse_demo_01";

            VitalObservation vital = new VitalObservation(
                    "",
                    "pat_demo_01",
                    "AROHI-PAT-KRM1",
                    "Karim Ahmed",
                    nurseUid,
                    nurseName,
                    "Ward A - Male Surgical",
                    "Bed 101",
                    bp,
                    hr,
                    temp,
                    spO2,
                    rr,
                    df.format(now),
                    tf.format(now),
                    notes
            );

            dBinding.btnSaveVitals.setEnabled(false);
            operationsRepository.recordPatientVitals(vital, new AuthCallback<VitalObservation>() {
                @Override
                public void onSuccess(VitalObservation result) {
                    dialog.dismiss();
                    if (binding != null) {
                        UiStateHelper.showSuccess(binding.getRoot(),
                                "Vitals observation recorded: " + result.getBloodPressure() + " • HR " + result.getHeartRateBpm() + " bpm");
                        loadTasks();
                    }
                }

                @Override
                public void onError(String message) {
                    dBinding.btnSaveVitals.setEnabled(true);
                    UiStateHelper.showError(dBinding.getRoot(), message);
                }
            });
        });

        dialog.show();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && binding != null && isAdded()) {
            loadTasks();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && isAdded()) {
            loadTasks();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
