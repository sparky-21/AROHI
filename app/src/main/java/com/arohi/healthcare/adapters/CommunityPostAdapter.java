package com.arohi.healthcare.adapters;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.ItemCommunityPostBinding;
import com.arohi.healthcare.models.CommunityPost;
import com.arohi.healthcare.models.UserRole;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Adapter for rendering Community and Hospital Feed posts.
 */
public class CommunityPostAdapter extends RecyclerView.Adapter<CommunityPostAdapter.PostViewHolder> {

    public interface OnPostInteractionListener {
        void onLikePost(@NonNull CommunityPost post);
        void onCommentPost(@NonNull CommunityPost post);
        void onSavePost(@NonNull CommunityPost post);
        void onReportPost(@NonNull CommunityPost post);
        void onModeratePost(@NonNull CommunityPost post);
    }

    private final String currentUserId;
    private final boolean isAdmin;
    private final OnPostInteractionListener listener;
    private final List<CommunityPost> items = new ArrayList<>();

    public CommunityPostAdapter(String currentUserId, boolean isAdmin, @NonNull OnPostInteractionListener listener) {
        this.currentUserId = currentUserId;
        this.isAdmin = isAdmin;
        this.listener = listener;
    }

    public void submitList(@NonNull List<CommunityPost> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCommunityPostBinding binding = ItemCommunityPostBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new PostViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        holder.bind(items.get(position), currentUserId, isAdmin, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        private final ItemCommunityPostBinding binding;

        PostViewHolder(@NonNull ItemCommunityPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull CommunityPost post, String currentUserId, boolean isAdmin,
                  @NonNull OnPostInteractionListener listener) {
            Context context = binding.getRoot().getContext();

            // Author Header
            binding.tvAuthorName.setText(post.getAuthorName() != null ? post.getAuthorName() : "Hospital Provider");

            String initials = "DR";
            if (UserRole.ADMIN.name().equalsIgnoreCase(post.getAuthorRole())) {
                initials = "AD";
                binding.tvVerificationBadge.setText("✓ ADMIN");
            } else {
                binding.tvVerificationBadge.setText("✓ VERIFIED");
            }
            if (post.getAuthorName() != null && post.getAuthorName().length() >= 2) {
                String clean = post.getAuthorName().replace("Dr.", "").trim();
                if (clean.length() >= 2) {
                    initials = clean.substring(0, 2).toUpperCase();
                }
            }
            binding.tvAuthorAvatar.setText(initials);

            String sub = (post.getAuthorSpecialty() != null && !post.getAuthorSpecialty().isEmpty())
                    ? post.getAuthorSpecialty() : "Clinical Department";
            if (post.getAuthorArohiId() != null && !post.getAuthorArohiId().isEmpty()) {
                sub += " • " + post.getAuthorArohiId();
            }
            binding.tvAuthorSubtitle.setText(sub);

            // Category badge
            String cat = post.getCategory() != null ? post.getCategory() : CommunityPost.CATEGORY_ANNOUNCEMENT;
            binding.tvPostCategoryBadge.setText(cat.replace("_", " "));

            // Title & Content
            binding.tvPostTitle.setText(post.getTitle() != null ? post.getTitle() : "Hospital Update");
            binding.tvPostContent.setText(post.getContent() != null ? post.getContent() : "");

            // Timestamp
            if (post.getTimestamp() > 0) {
                binding.tvPostDate.setText("Published " + DateFormat.format("yyyy-MM-dd • hh:mm a", new Date(post.getTimestamp())));
            }

            // Likes
            boolean isLiked = post.isLikedBy(currentUserId);
            binding.tvLikeIcon.setText(isLiked ? "♥ " : "♡ ");
            binding.tvLikeIcon.setTextColor(ContextCompat.getColor(context, isLiked ? R.color.status_rejected : R.color.text_secondary));
            binding.tvLikeCount.setText(String.valueOf(post.getLikeCount()));
            binding.btnLikePost.setOnClickListener(v -> listener.onLikePost(post));

            // Comments
            binding.tvCommentCount.setText(String.valueOf(post.getCommentCount()));
            binding.btnCommentPost.setOnClickListener(v -> listener.onCommentPost(post));

            // Saves
            boolean isSaved = post.isSavedBy(currentUserId);
            binding.tvSaveIcon.setText(isSaved ? "★ " : "☆ ");
            binding.tvSaveIcon.setTextColor(ContextCompat.getColor(context, isSaved ? R.color.status_warning : R.color.text_secondary));
            binding.tvSaveLabel.setText(isSaved ? "Saved" : "Save");
            binding.btnSavePost.setOnClickListener(v -> listener.onSavePost(post));

            // Report
            boolean isReported = post.isReportedBy(currentUserId);
            binding.btnReportPost.setText(isReported ? "🚩 Reported" : "🚩 Report");
            binding.btnReportPost.setTextColor(ContextCompat.getColor(context, isReported ? R.color.status_rejected : R.color.text_muted));
            binding.btnReportPost.setOnClickListener(v -> listener.onReportPost(post));

            // Admin Moderation
            if (isAdmin) {
                binding.btnModeratePost.setVisibility(View.VISIBLE);
                if (CommunityPost.MODERATION_REMOVED.equalsIgnoreCase(post.getModerationStatus())) {
                    binding.btnModeratePost.setText("Reinstate (Removed)");
                    binding.btnModeratePost.setTextColor(ContextCompat.getColor(context, R.color.status_rejected));
                } else if (CommunityPost.MODERATION_REPORTED.equalsIgnoreCase(post.getModerationStatus())) {
                    binding.btnModeratePost.setText("Flagged (" + post.getReportCount() + ")");
                    binding.btnModeratePost.setTextColor(ContextCompat.getColor(context, R.color.status_warning));
                } else {
                    binding.btnModeratePost.setText("⚙ Moderate");
                    binding.btnModeratePost.setTextColor(ContextCompat.getColor(context, R.color.deep_forest_green));
                }
                binding.btnModeratePost.setOnClickListener(v -> listener.onModeratePost(post));
            } else {
                binding.btnModeratePost.setVisibility(View.GONE);
            }
        }
    }
}
