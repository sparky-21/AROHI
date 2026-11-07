package com.arohi.healthcare.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.adapters.AiChatAdapter;
import com.arohi.healthcare.databinding.FragmentAiAssistantBinding;
import com.arohi.healthcare.models.AiChatMessage;
import com.arohi.healthcare.services.AiHealthcareAssistantService;
import com.arohi.healthcare.utils.UiStateHelper;

/**
 * Fragment hosting the AROHI AI Healthcare Assistant.
 * Provides educational guidance, medical terminology explanations, hospital indoor navigation,
 * and appointment booking instructions.
 * Strictly enforces Demo Mode fallback and safety disclaimers.
 */
public class AiAssistantFragment extends Fragment {

    private FragmentAiAssistantBinding binding;
    private AiHealthcareAssistantService aiService;
    private AiChatAdapter chatAdapter;

    public static AiAssistantFragment newInstance() {
        return new AiAssistantFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAiAssistantBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        aiService = new AiHealthcareAssistantService(requireContext());
        setupRecyclerView();
        setupListeners();
        updateDemoBannerState();

        // Initial Assistant Welcome Message
        if (chatAdapter.getItemCount() == 0) {
            String welcome = "Hello! I am your AROHI AI Healthcare Assistant.\n\n"
                    + "I can help you with:\n"
                    + "• Explaining medical terms (e.g., 'What is NPO?', 'Explain CBC')\n"
                    + "• Hospital Navigation (e.g., 'Directions to ICU or OT')\n"
                    + "• How to book appointments and find medical records\n"
                    + "• Preventative wellness and healthy lifestyle education.\n\n"
                    + "⚠️ Note: I am an educational assistant and cannot diagnose diseases or prescribe medications.";
            AiChatMessage welcomeMsg = new AiChatMessage("msg_welcome",
                    AiChatMessage.SENDER_AI, welcome, aiService.isDemoMode(), "WELCOME");
            chatAdapter.addMessage(welcomeMsg);
        }
    }

    private void setupRecyclerView() {
        chatAdapter = new AiChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        binding.recyclerAiChat.setLayoutManager(layoutManager);
        binding.recyclerAiChat.setAdapter(chatAdapter);
    }

    private void setupListeners() {
        binding.btnAiBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Quick prompt chips
        binding.chipPromptNpo.setOnClickListener(v -> sendUserPrompt("What does NPO mean?"));
        binding.chipPromptIcu.setOnClickListener(v -> sendUserPrompt("How do I get to the ICU?"));
        binding.chipPromptBp.setOnClickListener(v -> sendUserPrompt("Explain blood pressure readings and hypertension."));
        binding.chipPromptBook.setOnClickListener(v -> sendUserPrompt("How can I book a doctor appointment?"));
        binding.chipPromptRecords.setOnClickListener(v -> sendUserPrompt("Where can I find my digital prescriptions and lab reports?"));

        // Send query button
        binding.btnSendAiQuery.setOnClickListener(v -> {
            String query = binding.etAiQueryInput.getText() != null
                    ? binding.etAiQueryInput.getText().toString().trim() : "";
            if (!query.isEmpty()) {
                sendUserPrompt(query);
                binding.etAiQueryInput.setText("");
            }
        });

        // Configure AI API Settings Dialog
        binding.btnConfigureAiApi.setOnClickListener(v -> showConfigureApiDialog());
    }

    private void sendUserPrompt(String query) {
        // 1. Add User Message
        AiChatMessage userMsg = new AiChatMessage("user_" + System.currentTimeMillis(),
                AiChatMessage.SENDER_USER, query, false, "QUERY");
        chatAdapter.addMessage(userMsg);
        binding.recyclerAiChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);

        // 2. Query AI Service
        aiService.processUserQuery(query, new com.arohi.healthcare.repositories.AuthCallback<AiChatMessage>() {
            @Override
            public void onSuccess(AiChatMessage aiResponse) {
                if (binding == null) return;
                chatAdapter.addMessage(aiResponse);
                binding.recyclerAiChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
            }

            @Override
            public void onError(String message) {
                if (binding != null) {
                    UiStateHelper.showError(binding.getRoot(), message);
                }
            }
        });
    }

    private void updateDemoBannerState() {
        if (binding == null) return;
        boolean demo = aiService.isDemoMode();
        if (demo) {
            binding.cardDemoBanner.setVisibility(View.VISIBLE);
            binding.tvDemoBannerText.setText("Demo Mode Active: Live AI API key is not configured. Operating in educational mode with local medical knowledge and hospital navigation. Cannot diagnose or prescribe.");
            binding.tvAiHeaderStatus.setText("Educational Companion (Demo Mode)");
        } else {
            binding.cardDemoBanner.setVisibility(View.GONE);
            binding.tvAiHeaderStatus.setText("Connected to Configured AI Endpoint");
        }
    }

    private void showConfigureApiDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 24, 32, 16);

        final EditText etKey = new EditText(requireContext());
        etKey.setHint("AI API Key (Leave blank for Demo Mode)");
        etKey.setText(aiService.getApiKey());
        layout.addView(etKey);

        new AlertDialog.Builder(requireContext())
                .setTitle("Configure AI Service")
                .setMessage("Enter an API Key to enable live assistant queries, or leave blank to use the built-in Educational Demo Mode with medical terminology and navigation.")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    String key = etKey.getText().toString().trim();
                    aiService.configureAiApi(key, null);
                    updateDemoBannerState();
                    UiStateHelper.showSuccess(binding.getRoot(),
                            key.isEmpty() ? "Switched to Demo Mode." : "API Key saved for live queries.");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
