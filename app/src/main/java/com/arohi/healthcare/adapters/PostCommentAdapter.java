package com.arohi.healthcare.adapters;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arohi.healthcare.databinding.ItemPostCommentBinding;
import com.arohi.healthcare.models.PostComment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Adapter for rendering community post comments.
 */
public class PostCommentAdapter extends RecyclerView.Adapter<PostCommentAdapter.CommentViewHolder> {

    private final List<PostComment> items = new ArrayList<>();

    public void submitList(@NonNull List<PostComment> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPostCommentBinding binding = ItemPostCommentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CommentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        private final ItemPostCommentBinding binding;

        CommentViewHolder(@NonNull ItemPostCommentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull PostComment comment) {
            binding.tvCommentAuthorName.setText(comment.getAuthorName() != null ? comment.getAuthorName() : "Community Member");
            binding.tvCommentAuthorRole.setText(comment.getAuthorRole() != null ? comment.getAuthorRole() : "PATIENT");
            binding.tvCommentContent.setText(comment.getContent() != null ? comment.getContent() : "");
            if (comment.getTimestamp() > 0) {
                binding.tvCommentDate.setText(DateFormat.format("yyyy-MM-dd • hh:mm a", new Date(comment.getTimestamp())));
            }
        }
    }
}
