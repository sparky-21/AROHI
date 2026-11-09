package com.arohi.healthcare.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.adapters.CommunityPostAdapter;
import com.arohi.healthcare.adapters.PostCommentAdapter;
import com.arohi.healthcare.databinding.DialogCreateCommunityPostBinding;
import com.arohi.healthcare.databinding.DialogPostCommentsBinding;
import com.arohi.healthcare.databinding.FragmentCommunityFeedBinding;
import com.arohi.healthcare.models.CommunityPost;
import com.arohi.healthcare.models.PostComment;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.EcosystemIntegrationRepository;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.UiStateHelper;

import java.util.List;

/**
 * Phase 7 Community and Hospital Feed Fragment.
 * Features verified doctor and administrator posts, health awareness, vaccination campaigns,
 * hospital events, comments, reactions, saving, reporting, and administrative moderation.
 */
public class CommunityFeedFragment extends Fragment {

    private FragmentCommunityFeedBinding binding;
    private EcosystemIntegrationRepository ecosystemRepository;
    private SessionManager sessionManager;
    private UserAccount currentAccount;
    private CommunityPostAdapter postAdapter;
    private String selectedCategory = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCommunityFeedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ecosystemRepository = new EcosystemIntegrationRepository(requireContext());
        sessionManager = new SessionManager(requireContext());
        currentAccount = sessionManager.getCurrentSessionAccount();

        setupRecyclerView();
        setupFilterChips();
        setupCreateButton();

        loadFeed();
    }

    private void setupRecyclerView() {
        boolean isAdmin = currentAccount != null && currentAccount.getUserRoleEnum() == UserRole.ADMIN;
        String uid = currentAccount != null ? currentAccount.getUid() : "guest";

        postAdapter = new CommunityPostAdapter(uid, isAdmin, new CommunityPostAdapter.OnPostInteractionListener() {
            @Override
            public void onLikePost(@NonNull CommunityPost post) {
                ecosystemRepository.togglePostLike(post.getPostId(), uid, new AuthCallback<CommunityPost>() {
                    @Override
                    public void onSuccess(CommunityPost result) {
                        loadFeed();
                    }

                    @Override
                    public void onError(String message) {}
                });
            }

            @Override
            public void onCommentPost(@NonNull CommunityPost post) {
                showCommentsDialog(post);
            }

            @Override
            public void onSavePost(@NonNull CommunityPost post) {
                ecosystemRepository.togglePostSave(post.getPostId(), uid, new AuthCallback<CommunityPost>() {
                    @Override
                    public void onSuccess(CommunityPost result) {
                        String msg = result.isSavedBy(uid) ? "Post saved to your bookmarks." : "Post removed from bookmarks.";
                        UiStateHelper.showSuccess(binding.getRoot(), msg);
                        loadFeed();
                    }

                    @Override
                    public void onError(String message) {}
                });
            }

            @Override
            public void onReportPost(@NonNull CommunityPost post) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Report Community Post")
                        .setMessage("Report this post to hospital administrators for medical or policy review?")
                        .setPositiveButton("Report", (dialog, which) -> {
                            ecosystemRepository.reportPost(post.getPostId(), uid, "User reported", new AuthCallback<CommunityPost>() {
                                @Override
                                public void onSuccess(CommunityPost result) {
                                    UiStateHelper.showSuccess(binding.getRoot(), "Report submitted to Hospital Administration.");
                                    loadFeed();
                                }

                                @Override
                                public void onError(String message) {}
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            @Override
            public void onModeratePost(@NonNull CommunityPost post) {
                showAdminModerationDialog(post);
            }
        });

        binding.rvCommunityPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCommunityPosts.setAdapter(postAdapter);
    }

    private void setupFilterChips() {
        binding.chipFilterAll.setOnClickListener(v -> { selectedCategory = "ALL"; loadFeed(); });
        binding.chipFilterAnnouncements.setOnClickListener(v -> { selectedCategory = CommunityPost.CATEGORY_ANNOUNCEMENT; loadFeed(); });
        binding.chipFilterAwareness.setOnClickListener(v -> { selectedCategory = CommunityPost.CATEGORY_HEALTH_AWARENESS; loadFeed(); });
        binding.chipFilterVaccination.setOnClickListener(v -> { selectedCategory = CommunityPost.CATEGORY_VACCINATION; loadFeed(); });
        binding.chipFilterEvents.setOnClickListener(v -> { selectedCategory = CommunityPost.CATEGORY_HOSPITAL_EVENT; loadFeed(); });
    }

    private void setupCreateButton() {
        // Only DOCTOR or ADMIN can publish
        if (currentAccount != null &&
                (currentAccount.getUserRoleEnum() == UserRole.DOCTOR || currentAccount.getUserRoleEnum() == UserRole.ADMIN)) {
            binding.btnCreateCommunityPost.setVisibility(View.VISIBLE);
            binding.btnCreateCommunityPost.setOnClickListener(v -> showCreatePostDialog());
        } else {
            binding.btnCreateCommunityPost.setVisibility(View.GONE);
        }
    }

    private void loadFeed() {
        if (binding == null) return;
        boolean isAdmin = currentAccount != null && currentAccount.getUserRoleEnum() == UserRole.ADMIN;

        ecosystemRepository.fetchCommunityFeed(selectedCategory, isAdmin, new AuthCallback<List<CommunityPost>>() {
            @Override
            public void onSuccess(List<CommunityPost> result) {
                if (binding == null) return;
                postAdapter.submitList(result);
                binding.tvEmptyFeed.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    private void showCreatePostDialog() {
        DialogCreateCommunityPostBinding dBinding = DialogCreateCommunityPostBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dBinding.getRoot())
                .setCancelable(true)
                .create();

        String[] categories = new String[]{
                "Health Awareness",
                "Vaccination Campaign",
                "Hospital Announcement",
                "Hospital Event"
        };
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories);
        dBinding.spinnerPostCategory.setAdapter(catAdapter);

        dBinding.btnCancelPost.setOnClickListener(v -> dialog.dismiss());
        dBinding.btnPublishPost.setOnClickListener(v -> {
            String title = dBinding.etPostTitle.getText() != null ? dBinding.etPostTitle.getText().toString().trim() : "";
            String content = dBinding.etPostContent.getText() != null ? dBinding.etPostContent.getText().toString().trim() : "";

            if (title.isEmpty()) {
                dBinding.etPostTitle.setError("Title is required");
                return;
            }
            if (content.isEmpty()) {
                dBinding.etPostContent.setError("Content is required");
                return;
            }

            int sel = dBinding.spinnerPostCategory.getSelectedItemPosition();
            String catKey = CommunityPost.CATEGORY_HEALTH_AWARENESS;
            if (sel == 1) catKey = CommunityPost.CATEGORY_VACCINATION;
            else if (sel == 2) catKey = CommunityPost.CATEGORY_ANNOUNCEMENT;
            else if (sel == 3) catKey = CommunityPost.CATEGORY_HOSPITAL_EVENT;

            CommunityPost post = new CommunityPost(
                    "",
                    "",
                    "",
                    "",
                    "",
                    currentAccount.getUserRoleEnum() == UserRole.DOCTOR ? "Physician & Specialist" : "Hospital Administration",
                    catKey,
                    title,
                    content
            );

            dBinding.btnPublishPost.setEnabled(false);
            ecosystemRepository.publishCommunityPost(post, currentAccount, new AuthCallback<CommunityPost>() {
                @Override
                public void onSuccess(CommunityPost result) {
                    dialog.dismiss();
                    if (binding != null) {
                        UiStateHelper.showSuccess(binding.getRoot(), "Post published to AROHI Community Feed.");
                        loadFeed();
                    }
                }

                @Override
                public void onError(String message) {
                    dBinding.btnPublishPost.setEnabled(true);
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void showCommentsDialog(@NonNull CommunityPost post) {
        DialogPostCommentsBinding cBinding = DialogPostCommentsBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(cBinding.getRoot())
                .create();

        cBinding.tvPostTitleReference.setText("Regarding: " + post.getTitle());
        cBinding.btnCloseComments.setOnClickListener(v -> dialog.dismiss());

        PostCommentAdapter commentAdapter = new PostCommentAdapter();
        cBinding.rvPostComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        cBinding.rvPostComments.setAdapter(commentAdapter);

        // Load comments
        ecosystemRepository.fetchPostComments(post.getPostId(), new AuthCallback<List<PostComment>>() {
            @Override
            public void onSuccess(List<PostComment> result) {
                commentAdapter.submitList(result);
                cBinding.tvEmptyComments.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {}
        });

        // Submit comment
        cBinding.btnSubmitComment.setOnClickListener(v -> {
            String text = cBinding.etCommentInput.getText() != null ? cBinding.etCommentInput.getText().toString().trim() : "";
            if (text.isEmpty()) return;

            String authorName = currentAccount != null ? currentAccount.getFullName() : "Community Member";
            String authorUid = currentAccount != null ? currentAccount.getUid() : "uid_guest";
            String role = currentAccount != null ? currentAccount.getRole() : "PATIENT";
            String arohiId = currentAccount != null ? currentAccount.getArohiId() : "";

            PostComment newComment = new PostComment("", post.getPostId(), authorUid, authorName, role, arohiId, text);
            cBinding.btnSubmitComment.setEnabled(false);

            ecosystemRepository.addPostComment(post.getPostId(), newComment, new AuthCallback<PostComment>() {
                @Override
                public void onSuccess(PostComment result) {
                    cBinding.btnSubmitComment.setEnabled(true);
                    cBinding.etCommentInput.setText("");
                    cBinding.tvEmptyComments.setVisibility(View.GONE);

                    ecosystemRepository.fetchPostComments(post.getPostId(), new AuthCallback<List<PostComment>>() {
                        @Override
                        public void onSuccess(List<PostComment> list) {
                            commentAdapter.submitList(list);
                            cBinding.rvPostComments.scrollToPosition(list.size() - 1);
                        }

                        @Override
                        public void onError(String message) {}
                    });
                    loadFeed();
                }

                @Override
                public void onError(String message) {
                    cBinding.btnSubmitComment.setEnabled(true);
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void showAdminModerationDialog(@NonNull CommunityPost post) {
        String[] options;
        if (CommunityPost.MODERATION_REMOVED.equalsIgnoreCase(post.getModerationStatus())) {
            options = new String[]{"Reinstate Post (Make Public)", "Dismiss Reports & Keep Active"};
        } else {
            options = new String[]{"Remove Post from Public Feed", "Flag as Under Medical Review", "Clear All Reports"};
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Administrative Content Moderation")
                .setItems(options, (d, which) -> {
                    String newStatus = CommunityPost.MODERATION_ACTIVE;
                    String note = "Admin action";
                    if (CommunityPost.MODERATION_REMOVED.equalsIgnoreCase(post.getModerationStatus())) {
                        newStatus = CommunityPost.MODERATION_ACTIVE;
                        note = "Reinstated by Admin";
                    } else {
                        if (which == 0) {
                            newStatus = CommunityPost.MODERATION_REMOVED;
                            note = "Removed by Admin for policy";
                        } else if (which == 1) {
                            newStatus = CommunityPost.MODERATION_REPORTED;
                            note = "Flagged under clinical review";
                        } else {
                            newStatus = CommunityPost.MODERATION_ACTIVE;
                            note = "Reports cleared";
                        }
                    }

                    ecosystemRepository.moderatePost(post.getPostId(), currentAccount, newStatus, note, new AuthCallback<CommunityPost>() {
                        @Override
                        public void onSuccess(CommunityPost result) {
                            UiStateHelper.showSuccess(binding.getRoot(), "Post moderation status updated.");
                            loadFeed();
                        }

                        @Override
                        public void onError(String message) {
                            UiStateHelper.showError(binding.getRoot(), message);
                        }
                    });
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
