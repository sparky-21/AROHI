package com.arohi.healthcare.models;

import java.io.Serializable;

/**
 * Phase 7 Community Post Comment Model.
 * Represents discussion and user interactions on community posts.
 */
public class PostComment implements Serializable {

    private String commentId;
    private String postId;
    private String authorUid;
    private String authorName;
    private String authorRole; // PATIENT, DOCTOR, NURSE, ADMIN
    private String authorArohiId;
    private String content;
    private long timestamp;

    public PostComment() {
        this.timestamp = System.currentTimeMillis();
    }

    public PostComment(String commentId, String postId, String authorUid, String authorName,
                       String authorRole, String authorArohiId, String content) {
        this.commentId = commentId;
        this.postId = postId;
        this.authorUid = authorUid;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.authorArohiId = authorArohiId;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getAuthorUid() { return authorUid; }
    public void setAuthorUid(String authorUid) { this.authorUid = authorUid; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }

    public String getAuthorArohiId() { return authorArohiId; }
    public void setAuthorArohiId(String authorArohiId) { this.authorArohiId = authorArohiId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
