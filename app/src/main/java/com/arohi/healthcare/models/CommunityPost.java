package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 7 Community and Hospital Feed Post Model.
 * Authored exclusively by verified Doctors and Hospital Administrators.
 * Supports announcements, health awareness, vaccination campaigns, hospital events,
 * likes, comments, user saves, reporting, and admin moderation.
 */
public class CommunityPost implements Serializable {

    public static final String CATEGORY_ANNOUNCEMENT = "ANNOUNCEMENT";
    public static final String CATEGORY_HEALTH_AWARENESS = "HEALTH_AWARENESS";
    public static final String CATEGORY_VACCINATION = "VACCINATION";
    public static final String CATEGORY_HOSPITAL_EVENT = "HOSPITAL_EVENT";

    public static final String MODERATION_ACTIVE = "ACTIVE";
    public static final String MODERATION_REPORTED = "REPORTED";
    public static final String MODERATION_REMOVED = "REMOVED";

    private String postId;
    private String authorUid;
    private String authorName;
    private String authorRole; // DOCTOR or ADMIN
    private String authorArohiId;
    private String authorSpecialty; // e.g. "Cardiologist" or "Chief Medical Administrator"
    private boolean authorVerified;

    private String category; // ANNOUNCEMENT, HEALTH_AWARENESS, VACCINATION, HOSPITAL_EVENT
    private String title;
    private String content;
    private String imageUrl;
    private long timestamp;

    private int likeCount;
    private List<String> likedUserUids;

    private int commentCount;

    private int saveCount;
    private List<String> savedUserUids;

    private int reportCount;
    private List<String> reportedUserUids;

    private String moderationStatus; // ACTIVE, REPORTED, REMOVED
    private String moderationNote;

    public CommunityPost() {
        this.timestamp = System.currentTimeMillis();
        this.likedUserUids = new ArrayList<>();
        this.savedUserUids = new ArrayList<>();
        this.reportedUserUids = new ArrayList<>();
        this.moderationStatus = MODERATION_ACTIVE;
        this.authorVerified = true;
    }

    public CommunityPost(String postId, String authorUid, String authorName, String authorRole,
                         String authorArohiId, String authorSpecialty, String category,
                         String title, String content) {
        this.postId = postId;
        this.authorUid = authorUid;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.authorArohiId = authorArohiId;
        this.authorSpecialty = authorSpecialty;
        this.authorVerified = true;
        this.category = category;
        this.title = title;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.likedUserUids = new ArrayList<>();
        this.savedUserUids = new ArrayList<>();
        this.reportedUserUids = new ArrayList<>();
        this.moderationStatus = MODERATION_ACTIVE;
        this.likeCount = 0;
        this.commentCount = 0;
        this.saveCount = 0;
        this.reportCount = 0;
    }

    public boolean isLikedBy(String userUid) {
        return likedUserUids != null && userUid != null && likedUserUids.contains(userUid);
    }

    public boolean isSavedBy(String userUid) {
        return savedUserUids != null && userUid != null && savedUserUids.contains(userUid);
    }

    public boolean isReportedBy(String userUid) {
        return reportedUserUids != null && userUid != null && reportedUserUids.contains(userUid);
    }

    // Getters and Setters
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

    public String getAuthorSpecialty() { return authorSpecialty; }
    public void setAuthorSpecialty(String authorSpecialty) { this.authorSpecialty = authorSpecialty; }

    public boolean isAuthorVerified() { return authorVerified; }
    public void setAuthorVerified(boolean authorVerified) { this.authorVerified = authorVerified; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public List<String> getLikedUserUids() { return likedUserUids; }
    public void setLikedUserUids(List<String> likedUserUids) { this.likedUserUids = likedUserUids; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public int getSaveCount() { return saveCount; }
    public void setSaveCount(int saveCount) { this.saveCount = saveCount; }

    public List<String> getSavedUserUids() { return savedUserUids; }
    public void setSavedUserUids(List<String> savedUserUids) { this.savedUserUids = savedUserUids; }

    public int getReportCount() { return reportCount; }
    public void setReportCount(int reportCount) { this.reportCount = reportCount; }

    public List<String> getReportedUserUids() { return reportedUserUids; }
    public void setReportedUserUids(List<String> reportedUserUids) { this.reportedUserUids = reportedUserUids; }

    public String getModerationStatus() { return moderationStatus; }
    public void setModerationStatus(String moderationStatus) { this.moderationStatus = moderationStatus; }

    public String getModerationNote() { return moderationNote; }
    public void setModerationNote(String moderationNote) { this.moderationNote = moderationNote; }
}
