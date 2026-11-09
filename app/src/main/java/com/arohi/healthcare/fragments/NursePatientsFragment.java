package com.arohi.healthcare.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.adapters.NurseBedAdapter;
import com.arohi.healthcare.databinding.DialogRecordVitalsBinding;
import com.arohi.healthcare.databinding.FragmentNursePatientsBinding;
import com.arohi.healthcare.models.Bed;
import com.arohi.healthcare.models.NurseProfile;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.VitalObservation;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.HospitalOperationsRepository;
import com.arohi.healthcare.repositories.ProfileRepository;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Nurse Assigned Ward Patients Fragment.
 * Enforces Security & Authorization: Nurses can only access authorized patient information
 * within their assigned ward or assigned patient care roster.
 */
public class NursePatientsFragment extends Fragment {

    private FragmentNursePatientsBinding binding;
    private HospitalOperationsRepository operationsRepository;
    private ProfileRepository profileRepository;
    private SessionManager sessionManager;
    private NurseBedAdapter adapter;

    private UserAccount currentAccount;
    private NurseProfile nurseProfile;
    private final List<Bed> authorizedBeds = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNursePatientsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        operationsRepository = new HospitalOperationsRepository(requireContext());
        profileRepository = new ProfileRepository(requireContext());
        sessionManager = new SessionManager(requireContext());
        currentAccount = sessionManager.getCurrentSessionAccount();

        adapter = new NurseBedAdapter(true, new NurseBedAdapter.OnBedActionListener() {
            @Override
            public void onRecordVitals(@NonNull Bed bed) {
                showRecordVitalsDialog(bed);
            }

            @Override
            public void onBedAction(@NonNull Bed bed) {
                showRecordVitalsDialog(bed);
            }

            @Override
            public void onChangeStatus(@NonNull Bed bed) {
                showPatientDetailsDialog(bed);
            }
        });

        binding.rvNursePatients.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNursePatients.setAdapter(adapter);

        loadNurseAndAuthorizedPatients();
    }

    private void loadNurseAndAuthorizedPatients() {
        if (currentAccount == null) return;

        profileRepository.loadNurseProfile(currentAccount.getUid(), new AuthCallback<NurseProfile>() {
            @Override
            public void onSuccess(NurseProfile profile) {
                if (binding == null) return;
                nurseProfile = profile;
                String ward = profile != null && profile.getAssignedWardId() != null
                        ? profile.getAssignedWardId() : "Ward A - Male Surgical";
                binding.tvNurseAssignedWardBadge.setText("Assigned Ward: " + ward);

                loadAuthorizedPatients(ward);
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                loadAuthorizedPatients("Ward A - Male Surgical");
            }
        });
    }

    private void loadAuthorizedPatients(String wardName) {
        String uid = currentAccount != null ? currentAccount.getUid() : null;

        operationsRepository.loadAuthorizedPatientsForNurse(uid, wardName, new AuthCallback<List<Bed>>() {
            @Override
            public void onSuccess(List<Bed> beds) {
                if (binding == null) return;
                authorizedBeds.clear();
                authorizedBeds.addAll(beds);
                adapter.submitList(authorizedBeds);

                binding.tvNursePatientCountSummary.setText(beds.size() + " Authorized Patient" + (beds.size() == 1 ? "" : "s"));
                binding.tvEmptyNursePatients.setVisibility(beds.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    public void showRecordVitalsDialog(@NonNull Bed bed) {
        DialogRecordVitalsBinding dBinding = DialogRecordVitalsBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dBinding.getRoot())
                .create();

        dBinding.tvRecordVitalsPatientSub.setText(
                "Patient: " + bed.getPatientName() + " • " + bed.getWardName() + " (" + bed.getBedNumber() + ")");

        dBinding.btnCancelVitals.setOnClickListener(v -> dialog.dismiss());

        dBinding.btnSaveVitals.setOnClickListener(v -> {
            String bp = dBinding.etVitalsBp.getText() != null
                    ? dBinding.etVitalsBp.getText().toString().trim() : "120/80";
            String hrStr = dBinding.etVitalsHr.getText() != null
                    ? dBinding.etVitalsHr.getText().toString().trim() : "75";
            String tempStr = dBinding.etVitalsTemp.getText() != null
                    ? dBinding.etVitalsTemp.getText().toString().trim() : "98.6";
            String spO2Str = dBinding.etVitalsSpO2.getText() != null
                    ? dBinding.etVitalsSpO2.getText().toString().trim() : "98";
            String rrStr = dBinding.etVitalsRr.getText() != null
                    ? dBinding.etVitalsRr.getText().toString().trim() : "18";
            String notes = dBinding.etVitalsNotes.getText() != null
                    ? dBinding.etVitalsNotes.getText().toString().trim() : "Vitals stable.";

            int hr = 75;
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
                    bed.getPatientUid(),
                    bed.getPatientId(),
                    bed.getPatientName(),
                    nurseUid,
                    nurseName,
                    bed.getWardName(),
                    bed.getBedNumber(),
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
                                "Vitals saved for " + bed.getPatientName() + ": " + result.getBloodPressure() + " • SpO2 " + result.getSpO2Percent() + "%");
                        loadNurseAndAuthorizedPatients();
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

    private void showPatientDetailsDialog(@NonNull Bed bed) {
        String info = "Inpatient Ward Chart:\n\n"
                + "Patient: " + bed.getPatientName() + "\n"
                + "AROHI ID: " + bed.getPatientId() + "\n"
                + "Admission Date: " + (bed.getAdmissionDate() != null ? bed.getAdmissionDate() : "N/A") + "\n"
                + "Location: " + bed.getWardName() + " (" + bed.getBedNumber() + ")\n"
                + "Attending Physician: " + bed.getAssignedDoctorName() + "\n"
                + "Primary Care Nurse: " + bed.getAssignedNurseName() + "\n\n"
                + "Clinical Orders:\n" + bed.getClinicalNotes();

        new AlertDialog.Builder(requireContext())
                .setTitle("Inpatient: " + bed.getPatientName())
                .setMessage(info)
                .setPositiveButton("Record Vitals", (d, w) -> showRecordVitalsDialog(bed))
                .setNegativeButton("Close", null)
                .show();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && binding != null && isAdded()) {
            loadNurseAndAuthorizedPatients();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && isAdded()) {
            loadNurseAndAuthorizedPatients();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
