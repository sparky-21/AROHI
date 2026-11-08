package com.arohi.healthcare.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.activities.SecureChatActivity;
import com.arohi.healthcare.adapters.ConversationThreadAdapter;
import com.arohi.healthcare.databinding.FragmentConversationsListBinding;
import com.arohi.healthcare.models.AppNotification;
import com.arohi.healthcare.models.ConversationThread;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.SecureMessagingRepository;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;

import java.util.Date;
import java.util.List;

/**
 * Phase 3 Conversations List & Notifications Fragment:
 * Displays all authorized 1-to-1 Doctor-Patient conversations where connectionStatus == "Accepted",
 * and provides a privacy-safe Notifications dialog.
 */
public class ConversationsListFragment extends Fragment {

    private FragmentConversationsListBinding binding;
    private SecureMessagingRepository messagingRepository;
    private SessionManager sessionManager;
    private ConversationThreadAdapter adapter;
    private boolean isDoctorViewer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentConversationsListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        messagingRepository = new SecureMessagingRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        UserAccount account = sessionManager.getCurrentSessionAccount();
        isDoctorViewer = account != null && account.getUserRoleEnum() == UserRole.DOCTOR;

        adapter = new ConversationThreadAdapter(isDoctorViewer, this::openConversation);
        binding.rvConversations.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvConversations.setAdapter(adapter);

        binding.btnViewNotifications.setOnClickListener(v -> showNotificationsDialog());

        loadConversations();
    }

    private void loadConversations() {
        messagingRepository.loadAuthorizedConversations(new AuthCallback<List<ConversationThread>>() {
            @Override
            public void onSuccess(List<ConversationThread> result) {
                if (binding == null) return;
                adapter.submitList(result);
                if (result.isEmpty()) {
                    binding.tvEmptyConversations.setVisibility(View.VISIBLE);
                    binding.rvConversations.setVisibility(View.GONE);
                } else {
                    binding.tvEmptyConversations.setVisibility(View.GONE);
                    binding.rvConversations.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    private void openConversation(@NonNull ConversationThread thread) {
        String counterpartyUid = isDoctorViewer ? thread.getPatientUid() : thread.getDoctorUid();
        String counterpartyName = isDoctorViewer ? thread.getPatientName() : thread.getDoctorName();
        String subtitle = isDoctorViewer
                ? "Patient ID: " + notEmpty(thread.getPatientId(), "AROHI-PAT") + " • Accepted Connection"
                : "Specialty: " + notEmpty(thread.getDoctorSpecialty(), "Medicine") + " • Accepted Connection";

        Intent intent = SecureChatActivity.createIntent(
                requireContext(),
                thread.getConversationId(),
                counterpartyUid,
                notEmpty(counterpartyName, "Participant"),
                subtitle
        );
        startActivity(intent);
    }

    private void showNotificationsDialog() {
        UserAccount account = sessionManager.getCurrentSessionAccount();
        if (account == null) return;

        messagingRepository.loadUserNotifications(account.getUid(), new AuthCallback<List<AppNotification>>() {
            @Override
            public void onSuccess(List<AppNotification> list) {
                if (binding == null) return;
                messagingRepository.markAllNotificationsRead(account.getUid());

                if (list.isEmpty()) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Clinical Notifications")
                            .setMessage("No notifications yet. Incoming patient requests and secure message alerts will appear here.")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }

                StringBuilder sb = new StringBuilder();
                int count = Math.min(list.size(), 12);
                for (int i = 0; i < count; i++) {
                    AppNotification n = list.get(i);
                    String timeStr = n.getCreatedAt() > 0
                            ? DateFormat.format("MMM dd, hh:mm a", new Date(n.getCreatedAt())).toString()
                            : "";
                    sb.append("• ").append(n.getTitle()).append(" (").append(timeStr).append(")\n")
                            .append("  ").append(n.getBody()).append("\n\n");
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("Recent Privacy-Safe Notifications")
                        .setMessage(sb.toString().trim())
                        .setPositiveButton("Close", null)
                        .show();
            }

            @Override
            public void onError(String message) {
                // Ignore
            }
        });
    }

    private String notEmpty(String val, String fallback) {
        return val != null && !val.trim().isEmpty() ? val.trim() : fallback;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && binding != null && isAdded()) {
            loadConversations();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && isAdded()) {
            loadConversations();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
