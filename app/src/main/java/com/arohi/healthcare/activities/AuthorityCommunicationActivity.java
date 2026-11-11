package com.arohi.healthcare.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.adapters.StaffConversationAdapter;
import com.arohi.healthcare.databinding.ActivityAuthorityCommunicationBinding;
import com.arohi.healthcare.models.Hospital;
import com.arohi.healthcare.models.StaffConversation;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.HospitalCommunicationRepository;
import com.arohi.healthcare.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 12 Authority Communication Hub Activity.
 * Hospital administrator center for communicating with clinical staff:
 * - Pinned "All Doctors" group
 * - Pinned "All Nurses" group
 * - Direct 1-to-1 conversations with doctors and nurses
 * - Automated membership based on hospitalId and verified active status.
 */
public class AuthorityCommunicationActivity extends BaseActivity {

    public static final String EXTRA_HOSPITAL_ID = "extra_hospital_id";
    public static final String EXTRA_HOSPITAL_CODE = "extra_hospital_code";

    private ActivityAuthorityCommunicationBinding binding;
    private SessionManager sessionManager;
    private HospitalCommunicationRepository repository;
    private StaffConversationAdapter directConversationAdapter;

    private String hospitalId;
    private String hospitalCode;
    private UserAccount currentAccount;

    private StaffConversation doctorsGroup;
    private StaffConversation nursesGroup;

    public static Intent newIntent(Context context, String hospitalId, String hospitalCode) {
        Intent intent = new Intent(context, AuthorityCommunicationActivity.class);
        intent.putExtra(EXTRA_HOSPITAL_ID, hospitalId);
        intent.putExtra(EXTRA_HOSPITAL_CODE, hospitalCode);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthorityCommunicationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        repository = new HospitalCommunicationRepository(this);
        currentAccount = sessionManager.getCurrentSessionAccount();

        hospitalId = getIntent().getStringExtra(EXTRA_HOSPITAL_ID);
        hospitalCode = getIntent().getStringExtra(EXTRA_HOSPITAL_CODE);

        if (hospitalId == null || hospitalId.trim().isEmpty()) {
            hospitalId = sessionManager.getSelectedHospitalId();
        }
        if (hospitalCode == null || hospitalCode.trim().isEmpty()) {
            hospitalCode = sessionManager.getSelectedHospitalCode();
            if (hospitalCode == null || hospitalCode.trim().isEmpty()) {
                hospitalCode = "Hospital";
            }
        }

        if (currentAccount == null || hospitalId == null) {
            Toast.makeText(this, "Session invalid. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCommunicationHubData();
    }

    private void setupUI() {
        binding.tvHospitalTenancySubtitle.setText("Hospital Authority • " + hospitalCode + " (" + hospitalId + ")");
        binding.tvSecurityBannerDetails.setText("Verified Clinical Staff Isolation: Channel access is strictly limited to staff registered with " + hospitalCode + ".");

        binding.btnAuthorityCommBack.setOnClickListener(v -> finish());
        binding.btnRefreshConversations.setOnClickListener(v -> loadCommunicationHubData());

        binding.cardAllDoctorsGroup.setOnClickListener(v -> {
            if (doctorsGroup != null) {
                openChat(doctorsGroup);
            }
        });

        binding.cardAllNursesGroup.setOnClickListener(v -> {
            if (nursesGroup != null) {
                openChat(nursesGroup);
            }
        });

        binding.btnNewStaffChat.setOnClickListener(v -> openNewDirectChatPicker());
    }

    private void setupRecyclerView() {
        directConversationAdapter = new StaffConversationAdapter(this::openChat);
        binding.rvDirectStaffConversations.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDirectStaffConversations.setAdapter(directConversationAdapter);
    }

    private void loadCommunicationHubData() {
        // 1. Load Clinical Groups
        repository.getHospitalGroups(hospitalId, hospitalCode, new AuthCallback<List<StaffConversation>>() {
            @Override
            public void onSuccess(List<StaffConversation> groups) {
                for (StaffConversation c : groups) {
                    if (StaffConversation.TYPE_GROUP_DOCTORS.equals(c.getConversationType())) {
                        doctorsGroup = c;
                        binding.tvDoctorsGroupTitle.setText(c.getTitle());
                        binding.tvDoctorsMemberCountBadge.setText(c.getMemberCount() + " ACTIVE");
                        binding.tvDoctorsLastMessage.setText(c.getLastMessageText() != null ? c.getLastMessageText() : "Tap to open group");
                    } else if (StaffConversation.TYPE_GROUP_NURSES.equals(c.getConversationType())) {
                        nursesGroup = c;
                        binding.tvNursesGroupTitle.setText(c.getTitle());
                        binding.tvNursesMemberCountBadge.setText(c.getMemberCount() + " ACTIVE");
                        binding.tvNursesLastMessage.setText(c.getLastMessageText() != null ? c.getLastMessageText() : "Tap to open group");
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(AuthorityCommunicationActivity.this, "Failed to load groups: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        // 2. Load Direct Staff Conversations
        repository.getStaffConversations(hospitalId, new AuthCallback<List<StaffConversation>>() {
            @Override
            public void onSuccess(List<StaffConversation> conversations) {
                if (conversations == null || conversations.isEmpty()) {
                    binding.layoutEmptyDirectChats.setVisibility(View.VISIBLE);
                    binding.rvDirectStaffConversations.setVisibility(View.GONE);
                } else {
                    binding.layoutEmptyDirectChats.setVisibility(View.GONE);
                    binding.rvDirectStaffConversations.setVisibility(View.VISIBLE);
                    directConversationAdapter.submitList(conversations);
                }
            }

            @Override
            public void onError(String errorMessage) {
                binding.layoutEmptyDirectChats.setVisibility(View.VISIBLE);
            }
        });
    }

    private void openChat(@NonNull StaffConversation conversation) {
        Intent intent = StaffChatActivity.newIntent(
                this,
                conversation.getConversationId(),
                conversation.getHospitalId(),
                conversation.getHospitalCode(),
                conversation.getTitle(),
                conversation.getConversationType()
        );
        intent.putExtra(StaffChatActivity.EXTRA_PARTICIPANT_NAME, conversation.getParticipantName());
        intent.putExtra(StaffChatActivity.EXTRA_PARTICIPANT_ROLE, conversation.getParticipantRole());
        startActivity(intent);
    }

    private void openNewDirectChatPicker() {
        repository.getHospitalStaffDirectory(hospitalId, new AuthCallback<List<UserAccount>>() {
            @Override
            public void onSuccess(List<UserAccount> staffList) {
                if (staffList == null || staffList.isEmpty()) {
                    new AlertDialog.Builder(AuthorityCommunicationActivity.this)
                            .setTitle("No Verified Staff Found")
                            .setMessage("There are currently no verified active doctors or nurses registered under " + hospitalCode + ".")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }

                String[] staffLabels = new String[staffList.size()];
                for (int i = 0; i < staffList.size(); i++) {
                    UserAccount staff = staffList.get(i);
                    staffLabels[i] = staff.getFullName() + " (" + staff.getUserRoleEnum().name() + " • " + staff.getArohiId() + ")";
                }

                new AlertDialog.Builder(AuthorityCommunicationActivity.this)
                        .setTitle("Select Staff Member")
                        .setItems(staffLabels, (dialog, which) -> {
                            UserAccount selectedStaff = staffList.get(which);
                            startOrOpenIndividualChat(selectedStaff);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(AuthorityCommunicationActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startOrOpenIndividualChat(@NonNull UserAccount staff) {
        repository.getOrCreateIndividualConversation(
                hospitalId, hospitalCode, currentAccount, staff,
                new AuthCallback<StaffConversation>() {
                    @Override
                    public void onSuccess(StaffConversation conversation) {
                        openChat(conversation);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(AuthorityCommunicationActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }
}
