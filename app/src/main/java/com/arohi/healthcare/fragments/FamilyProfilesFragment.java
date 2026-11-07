package com.arohi.healthcare.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.activities.RolePortalActivity;
import com.arohi.healthcare.adapters.FamilyMemberAdapter;
import com.arohi.healthcare.databinding.DialogAddFamilyMemberBinding;
import com.arohi.healthcare.databinding.FragmentFamilyHealthBinding;
import com.arohi.healthcare.models.FamilyMember;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.FamilyRepository;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment managing Patient Family Health Profiles.
 * Supports adding dependents, appointment booking on their behalf, and reminders.
 * Strictly guarantees that family profiles do not grant automatic access to medical records.
 */
public class FamilyProfilesFragment extends Fragment {

    private FragmentFamilyHealthBinding binding;
    private FamilyRepository familyRepository;
    private SessionManager sessionManager;
    private UserAccount currentAccount;
    private FamilyMemberAdapter adapter;

    public static FamilyProfilesFragment newInstance() {
        return new FamilyProfilesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFamilyHealthBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        familyRepository = new FamilyRepository(requireContext());
        sessionManager = new SessionManager(requireContext());
        currentAccount = sessionManager.getCurrentSessionAccount();

        setupRecyclerView();
        setupListeners();
        loadFamilyMembers();
    }

    private void setupRecyclerView() {
        binding.recyclerFamilyMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FamilyMemberAdapter(new FamilyMemberAdapter.OnFamilyMemberActionListener() {
            @Override
            public void onBookAppointmentForMember(@NonNull FamilyMember member) {
                // Switch to Doctors tab to book an appointment
                UiStateHelper.showSuccess(binding.getRoot(),
                        "Booking consultation for dependent: " + member.getFullName() + " (" + member.getRelationship() + ")");
                if (getActivity() instanceof RolePortalActivity) {
                    ((RolePortalActivity) getActivity()).switchToTab(R.id.nav_patient_doctors);
                }
            }

            @Override
            public void onRemoveFamilyMember(@NonNull FamilyMember member) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Remove Family Member")
                        .setMessage("Are you sure you want to remove " + member.getFullName() + " from your family profiles?")
                        .setPositiveButton("Remove", (d, w) -> {
                            familyRepository.deleteFamilyMember(member.getMemberId(), new AuthCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    if (binding != null) {
                                        UiStateHelper.showSuccess(binding.getRoot(), "Family member removed.");
                                        loadFamilyMembers();
                                    }
                                }

                                @Override
                                public void onError(String message) {
                                    if (binding != null) UiStateHelper.showError(binding.getRoot(), message);
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        binding.recyclerFamilyMembers.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnFamilyBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        binding.btnAddFamilyMember.setOnClickListener(v -> showAddFamilyMemberDialog());
        binding.btnEmptyAddFamily.setOnClickListener(v -> showAddFamilyMemberDialog());
    }

    private void loadFamilyMembers() {
        if (currentAccount == null) return;
        familyRepository.loadFamilyMembers(currentAccount.getUid(), new AuthCallback<List<FamilyMember>>() {
            @Override
            public void onSuccess(List<FamilyMember> result) {
                if (binding == null) return;
                adapter.submitList(result);
                if (result.isEmpty()) {
                    binding.layoutEmptyFamily.setVisibility(View.VISIBLE);
                    binding.recyclerFamilyMembers.setVisibility(View.GONE);
                } else {
                    binding.layoutEmptyFamily.setVisibility(View.GONE);
                    binding.recyclerFamilyMembers.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String message) {
                if (binding != null) UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    private void showAddFamilyMemberDialog() {
        DialogAddFamilyMemberBinding dBinding = DialogAddFamilyMemberBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dBinding.getRoot())
                .create();

        // Relationship Spinner
        String[] relationships = new String[]{
                FamilyMember.RELATION_SPOUSE,
                FamilyMember.RELATION_CHILD,
                FamilyMember.RELATION_PARENT,
                FamilyMember.RELATION_SIBLING,
                FamilyMember.RELATION_GRANDPARENT,
                FamilyMember.RELATION_OTHER
        };
        ArrayAdapter<String> relAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, relationships);
        dBinding.spinnerRelationship.setAdapter(relAdapter);

        dBinding.btnCancelFamilyDialog.setOnClickListener(v -> dialog.dismiss());

        dBinding.btnSaveFamilyMember.setOnClickListener(v -> {
            String name = dBinding.etFamilyFullName.getText() != null
                    ? dBinding.etFamilyFullName.getText().toString().trim() : "";
            if (name.isEmpty()) {
                dBinding.etFamilyFullName.setError("Full name is required");
                return;
            }

            if (!dBinding.cbExplicitAuthorization.isChecked()) {
                UiStateHelper.showError(dBinding.getRoot(),
                        "You must check the authorization consent box to add a family profile.");
                return;
            }

            String rel = dBinding.spinnerRelationship.getSelectedItem().toString();
            String dob = dBinding.etFamilyDob.getText() != null ? dBinding.etFamilyDob.getText().toString().trim() : "";
            String gender = dBinding.etFamilyGender.getText() != null ? dBinding.etFamilyGender.getText().toString().trim() : "";
            String blood = dBinding.etFamilyBloodGroup.getText() != null ? dBinding.etFamilyBloodGroup.getText().toString().trim() : "";
            String phone = dBinding.etFamilyPhone.getText() != null ? dBinding.etFamilyPhone.getText().toString().trim() : "";
            String allergies = dBinding.etFamilyAllergies.getText() != null ? dBinding.etFamilyAllergies.getText().toString().trim() : "";

            FamilyMember newMember = new FamilyMember(
                    "fam_" + System.currentTimeMillis(),
                    currentAccount.getUid(),
                    name,
                    rel,
                    dob,
                    gender,
                    blood,
                    allergies,
                    phone,
                    true,
                    "Authorized by primary patient for care scheduling",
                    ""
            );

            familyRepository.addFamilyMember(newMember, new AuthCallback<FamilyMember>() {
                @Override
                public void onSuccess(FamilyMember added) {
                    dialog.dismiss();
                    if (binding != null) {
                        UiStateHelper.showSuccess(binding.getRoot(), "Family member profile added successfully!");
                        loadFamilyMembers();
                    }
                }

                @Override
                public void onError(String message) {
                    UiStateHelper.showError(dBinding.getRoot(), message);
                }
            });
        });

        dialog.show();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            loadFamilyMembers();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded()) {
            loadFamilyMembers();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
