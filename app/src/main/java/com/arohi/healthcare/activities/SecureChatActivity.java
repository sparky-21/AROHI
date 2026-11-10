package com.arohi.healthcare.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.adapters.ChatMessageAdapter;
import com.arohi.healthcare.databinding.ActivitySecureChatBinding;
import com.arohi.healthcare.models.ChatMessage;
import com.arohi.healthcare.models.DoctorProfile;
import com.arohi.healthcare.models.PatientProfile;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.DoctorDiscoveryAndRequestRepository;
import com.arohi.healthcare.repositories.ProfileRepository;
import com.arohi.healthcare.repositories.SecureMessagingRepository;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * 13. Secure One-to-One Doctor-Patient Chat Activity
 * Enforces both client-side and Firestore-rule connection verification before streaming
 * or sending messages. Only Accepted connections can access this conversation.
 */
public class SecureChatActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "extra_conversation_id";
    public static final String EXTRA_COUNTERPARTY_UID = "extra_counterparty_uid";
    public static final String EXTRA_COUNTERPARTY_NAME = "extra_counterparty_name";
    public static final String EXTRA_COUNTERPARTY_SUBTITLE = "extra_counterparty_subtitle";

    private ActivitySecureChatBinding binding;
    private SecureMessagingRepository messagingRepository;
    private DoctorDiscoveryAndRequestRepository discoveryRepository;
    private ProfileRepository profileRepository;
    private SessionManager sessionManager;
    private ChatMessageAdapter messageAdapter;
    private ListenerRegistration messageListenerRegistration;

    private String conversationId;
    private String counterpartyUid;
    private String counterpartyName;
    private UserAccount currentAccount;

    public static Intent createIntent(@NonNull Context context,
                                      @NonNull String conversationId,
                                      @NonNull String counterpartyUid,
                                      @NonNull String counterpartyName,
                                      @NonNull String counterpartySubtitle) {
        Intent intent = new Intent(context, SecureChatActivity.class);
        intent.putExtra(EXTRA_CONVERSATION_ID, conversationId);
        intent.putExtra(EXTRA_COUNTERPARTY_UID, counterpartyUid);
        intent.putExtra(EXTRA_COUNTERPARTY_NAME, counterpartyName);
        intent.putExtra(EXTRA_COUNTERPARTY_SUBTITLE, counterpartySubtitle);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySecureChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        messagingRepository = new SecureMessagingRepository(this);
        discoveryRepository = new DoctorDiscoveryAndRequestRepository(this);
        profileRepository = new ProfileRepository(this);

        currentAccount = sessionManager.getCurrentSessionAccount();
        if (currentAccount == null) {
            finish();
            return;
        }

        conversationId = getIntent().getStringExtra(EXTRA_CONVERSATION_ID);
        counterpartyUid = getIntent().getStringExtra(EXTRA_COUNTERPARTY_UID);
        counterpartyName = getIntent().getStringExtra(EXTRA_COUNTERPARTY_NAME);
        String subtitle = getIntent().getStringExtra(EXTRA_COUNTERPARTY_SUBTITLE);

        binding.tvChatCounterpartyName.setText(
                counterpartyName != null && !counterpartyName.isEmpty() ? counterpartyName : "Clinical Participant");
        if (subtitle != null && !subtitle.isEmpty()) {
            binding.tvChatConnectionBadge.setText(subtitle);
        }

        binding.btnChatBack.setOnClickListener(v -> finish());
        binding.btnInspectCounterpartyProfile.setOnClickListener(v -> inspectCounterpartyProfile());

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.rvChatMessages.setLayoutManager(layoutManager);
        messageAdapter = new ChatMessageAdapter(currentAccount.getUid());
        binding.rvChatMessages.setAdapter(messageAdapter);

        if (conversationId == null || conversationId.isEmpty()
                || counterpartyUid == null || counterpartyUid.isEmpty()) {
            showAccessDeniedState("Invalid conversation or participant identifier.");
            return;
        }

        binding.btnSendChatMessage.setOnClickListener(v -> sendCurrentMessage());
        startVerifiedMessageStream();
    }

    private void startVerifiedMessageStream() {
        messagingRepository.verifyConnectionAndAttachMessageListener(
                conversationId,
                new SecureMessagingRepository.MessageStreamListener() {
                    @Override
                    public void onMessagesUpdated(@NonNull List<ChatMessage> messages) {
                        if (isFinishing() || isDestroyed()) return;
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            binding.bannerChatAccessDenied.setVisibility(View.GONE);
                            binding.layoutChatComposer.setVisibility(View.VISIBLE);
                            binding.rvChatMessages.setVisibility(View.VISIBLE);

                            messageAdapter.submitMessages(messages);
                            if (messages.isEmpty()) {
                                binding.tvEmptyChatMessages.setVisibility(View.VISIBLE);
                            } else {
                                binding.tvEmptyChatMessages.setVisibility(View.GONE);
                                binding.rvChatMessages.scrollToPosition(messages.size() - 1);
                            }
                        });
                    }

                    @Override
                    public void onAccessDenied(@NonNull String reason) {
                        if (isFinishing() || isDestroyed()) return;
                        showAccessDeniedState(reason);
                    }

                    @Override
                    public void onError(@NonNull String error) {
                        if (isFinishing() || isDestroyed()) return;
                        UiStateHelper.showError(binding.getRoot(), error);
                    }
                },
                new AuthCallback<ListenerRegistration>() {
                    @Override
                    public void onSuccess(ListenerRegistration reg) {
                        messageListenerRegistration = reg;
                    }

                    @Override
                    public void onError(String message) {
                        showAccessDeniedState(message);
                    }
                }
        );
    }

    private void showAccessDeniedState(String reason) {
        binding.bannerChatAccessDenied.setVisibility(View.VISIBLE);
        binding.tvChatAccessDeniedReason.setText(reason);
        binding.layoutChatComposer.setVisibility(View.GONE);
        binding.rvChatMessages.setVisibility(View.GONE);
        binding.tvEmptyChatMessages.setVisibility(View.GONE);
    }

    private void sendCurrentMessage() {
        String text = binding.etChatMessageInput.getText() != null
                ? binding.etChatMessageInput.getText().toString().trim()
                : "";
        if (text.isEmpty()) {
            return;
        }

        binding.btnSendChatMessage.setEnabled(false);
        messagingRepository.sendSecureMessage(conversationId, counterpartyUid, text, new AuthCallback<ChatMessage>() {
            @Override
            public void onSuccess(ChatMessage result) {
                if (isFinishing() || isDestroyed()) return;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    binding.btnSendChatMessage.setEnabled(true);
                    binding.etChatMessageInput.setText("");
                    if (result != null) {
                        binding.bannerChatAccessDenied.setVisibility(View.GONE);
                        binding.rvChatMessages.setVisibility(View.VISIBLE);
                        binding.tvEmptyChatMessages.setVisibility(View.GONE);
                        messageAdapter.addOrUpdateMessage(result);
                        if (messageAdapter.getItemCount() > 0) {
                            binding.rvChatMessages.scrollToPosition(messageAdapter.getItemCount() - 1);
                        }
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    binding.btnSendChatMessage.setEnabled(true);
                    UiStateHelper.showError(binding.getRoot(), message);
                });
            }
        });
    }

    private void inspectCounterpartyProfile() {
        if (currentAccount == null || counterpartyUid == null) return;

        if (currentAccount.getUserRoleEnum() == UserRole.DOCTOR) {
            discoveryRepository.loadConnectedPatientProfile(
                    currentAccount.getUid(),
                    counterpartyUid,
                    new AuthCallback<PatientProfile>() {
                        @Override
                        public void onSuccess(PatientProfile p) {
                            if (isFinishing() || isDestroyed()) return;
                            String info = "Patient Name: " + notEmpty(p.getFullName(), "Patient") + "\n"
                                    + "Patient ID: " + notEmpty(p.getArohiId(), "AROHI-PAT") + "\n"
                                    + "Gender: " + notEmpty(p.getGender(), "N/A") + "\n"
                                    + "Date of Birth: " + notEmpty(p.getDateOfBirth(), "N/A") + "\n"
                                    + "Blood Group: " + notEmpty(p.getBloodGroup(), "N/A") + "\n"
                                    + "Known Allergies: " + notEmpty(p.getKnownAllergies(), "None") + "\n"
                                    + "Chronic Conditions: " + notEmpty(p.getChronicConditions(), "None") + "\n"
                                    + "Phone: " + notEmpty(p.getPhone(), "N/A") + "\n"
                                    + "Emergency Contact: " + notEmpty(p.getEmergencyContactName(), "N/A")
                                    + " (" + notEmpty(p.getEmergencyContactPhone(), "N/A") + ")";
                            new AlertDialog.Builder(SecureChatActivity.this)
                                    .setTitle("Connected Patient Health Profile")
                                    .setMessage(info)
                                    .setPositiveButton("Close", null)
                                    .show();
                        }

                        @Override
                        public void onError(String message) {
                            if (isFinishing() || isDestroyed()) return;
                            UiStateHelper.showError(binding.getRoot(), message);
                        }
                    });
        } else {
            profileRepository.loadDoctorProfile(counterpartyUid, new AuthCallback<DoctorProfile>() {
                @Override
                public void onSuccess(DoctorProfile d) {
                    if (isFinishing() || isDestroyed()) return;
                    String info = "Doctor: " + notEmpty(d.getFullName(), "Dr. Specialist") + "\n"
                            + "Doctor ID: " + notEmpty(d.getArohiId(), "AROHI-DOC") + "\n"
                            + "BMDC License: " + notEmpty(d.getMedicalLicenseNumber(), "Verified") + "\n"
                            + "Specialty: " + notEmpty(d.getSpecialty(), "General Medicine") + "\n"
                            + "Qualifications: " + notEmpty(d.getQualifications(), "MBBS") + "\n"
                            + "Department: " + notEmpty(d.getDepartment(), "Medicine") + "\n"
                            + "Experience: " + d.getExperienceYears() + " Years\n"
                            + "Consultation Fee: " + String.format("%.0f", d.getConsultationFeeBdt()) + " BDT\n"
                            + "Availability: " + notEmpty(d.getAvailabilitySchedule(), "Sun - Thu, 5PM - 9PM");
                    new AlertDialog.Builder(SecureChatActivity.this)
                            .setTitle("Connected Specialist Profile")
                            .setMessage(info)
                            .setPositiveButton("Close", null)
                            .show();
                }

                @Override
                public void onError(String message) {
                    if (isFinishing() || isDestroyed()) return;
                    UiStateHelper.showError(binding.getRoot(), message);
                }
            });
        }
    }

    private String notEmpty(String val, String fallback) {
        return val != null && !val.trim().isEmpty() ? val.trim() : fallback;
    }

    @Override
    protected void onDestroy() {
        if (messageListenerRegistration != null) {
            messageListenerRegistration.remove();
            messageListenerRegistration = null;
        }
        super.onDestroy();
    }
}
