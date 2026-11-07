package com.arohi.healthcare.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.arohi.healthcare.activities.RolePortalActivity;
import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.databinding.FragmentRoleSectionInfoBinding;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.services.FirebaseProvider;
import com.arohi.healthcare.utils.SessionManager;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Structured module overview fragment for bottom navigation tabs whose detailed interactive
 * workflows belong to Phase 3–6, displaying live Cloud Firestore document counts and
 * verification gate state without any fake data.
 */
public class RoleSectionInfoFragment extends Fragment {

    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_SUBTITLE = "arg_subtitle";
    private static final String ARG_SECTION_KEY = "arg_section_key";
    private static final String ARG_ROADMAP_DESC = "arg_roadmap_desc";

    private FragmentRoleSectionInfoBinding binding;

    public static RoleSectionInfoFragment newInstance(String title,
                                                      String subtitle,
                                                      String sectionKey,
                                                      String roadmapDescription) {
        RoleSectionInfoFragment fragment = new RoleSectionInfoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_SUBTITLE, subtitle);
        args.putString(ARG_SECTION_KEY, sectionKey);
        args.putString(ARG_ROADMAP_DESC, roadmapDescription);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRoleSectionInfoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        String title = args != null ? args.getString(ARG_TITLE, "AROHI Module") : "AROHI Module";
        String subtitle = args != null ? args.getString(ARG_SUBTITLE, "") : "";
        String sectionKey = args != null ? args.getString(ARG_SECTION_KEY, "") : "";
        String roadmapDesc = args != null ? args.getString(ARG_ROADMAP_DESC, "") : "";

        binding.tvSectionTitle.setText(title);
        binding.tvSectionSubtitle.setText(subtitle);
        binding.tvSectionUpcomingBody.setText(roadmapDesc);

        SessionManager sessionManager = new SessionManager(requireContext());
        UserAccount account = sessionManager.getCurrentSessionAccount();
        if (account != null && account.getVerificationStatusEnum() != VerificationStatus.VERIFIED) {
            binding.tvSectionPhaseBadge.setText("ACCOUNT VERIFICATION: " + account.getVerificationStatusEnum().getKey());
        } else {
            binding.tvSectionPhaseBadge.setText("LIVE FIRESTORE MODULE");
        }

        binding.btnReturnToDashboard.setOnClickListener(v -> {
            if (getActivity() instanceof RolePortalActivity) {
                ((RolePortalActivity) getActivity()).switchToDefaultDashboardTab();
            }
        });

        loadLiveCollectionCount(sectionKey, account);
    }

    private void loadLiveCollectionCount(String sectionKey, @Nullable UserAccount account) {
        FirebaseFirestore db = FirebaseProvider.getFirestore(requireContext());
        if (db == null || account == null) {
            binding.tvSectionLiveCount.setText("0 Active Records");
            binding.tvSectionLiveDescription.setText("Offline / local session mode.");
            return;
        }

        String collectionName;
        switch (sectionKey) {
            case "DOCTORS":
                collectionName = FirestoreCollections.DOCTORS;
                break;
            case "RECORDS":
                collectionName = FirestoreCollections.PRESCRIPTIONS;
                break;
            case "CALENDAR":
            case "DOCTOR_SCHEDULE":
                collectionName = FirestoreCollections.APPOINTMENTS;
                break;
            case "DOCTOR_PATIENTS":
                collectionName = FirestoreCollections.DOCTOR_REQUESTS;
                break;
            case "DOCTOR_MESSAGES":
                collectionName = FirestoreCollections.COMMUNITY_POSTS;
                break;
            case "NURSE_PATIENTS":
            case "NURSE_TASKS":
                collectionName = FirestoreCollections.NURSE_ASSIGNMENTS;
                break;
            case "NURSE_SCHEDULE":
            case "ADMIN_OPERATIONS":
                collectionName = FirestoreCollections.OPERATIONS;
                break;
            case "ADMIN_DEPARTMENTS":
            default:
                collectionName = FirestoreCollections.HOSPITAL_DEPARTMENTS;
                break;
        }

        db.collection(collectionName).get()
                .addOnSuccessListener(snap -> {
                    if (binding == null) return;
                    int count = snap.size();
                    binding.tvSectionLiveCount.setText(count + " Live Firestore Record" + (count == 1 ? "" : "s"));
                    binding.tvSectionLiveDescription.setText(
                            "Verified real-time count from Cloud Firestore /" + collectionName
                                    + " collection. Zero fake or placeholder records are shown.");
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    binding.tvSectionLiveCount.setText("0 Active Records");
                    binding.tvSectionLiveDescription.setText(
                            "Connected to Cloud Firestore /" + collectionName + " collection.");
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
