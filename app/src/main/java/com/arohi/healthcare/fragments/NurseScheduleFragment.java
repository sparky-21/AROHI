package com.arohi.healthcare.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.activities.RolePortalActivity;
import com.arohi.healthcare.adapters.DutyScheduleAdapter;
import com.arohi.healthcare.databinding.FragmentNurseScheduleBinding;
import com.arohi.healthcare.models.DutySchedule;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.HospitalOperationsRepository;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Nurse Duty Schedule & Operation Theatre Assistance Roster Fragment.
 */
public class NurseScheduleFragment extends Fragment {

    private FragmentNurseScheduleBinding binding;
    private HospitalOperationsRepository operationsRepository;
    private SessionManager sessionManager;
    private DutyScheduleAdapter adapter;

    private UserAccount currentAccount;
    private final List<DutySchedule> allSchedules = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNurseScheduleBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        operationsRepository = new HospitalOperationsRepository(requireContext());
        sessionManager = new SessionManager(requireContext());
        currentAccount = sessionManager.getCurrentSessionAccount();

        adapter = new DutyScheduleAdapter();
        binding.rvNurseSchedule.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNurseSchedule.setAdapter(adapter);

        binding.btnOpenUnifiedCalendarNurse.setOnClickListener(v -> {
            if (getActivity() instanceof RolePortalActivity) {
                ((RolePortalActivity) getActivity()).openSubFragment(
                        UnifiedCalendarFragment.newInstance(false), "nurse_calendar");
            }
        });

        loadDutySchedules();
    }

    private void loadDutySchedules() {
        String uid = currentAccount != null ? currentAccount.getUid() : null;

        operationsRepository.loadDutySchedulesForNurse(uid, new AuthCallback<List<DutySchedule>>() {
            @Override
            public void onSuccess(List<DutySchedule> list) {
                if (binding == null) return;
                allSchedules.clear();
                allSchedules.addAll(list);
                adapter.submitList(allSchedules);
                binding.tvEmptyNurseSchedule.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && binding != null && isAdded()) {
            loadDutySchedules();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && isAdded()) {
            loadDutySchedules();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
