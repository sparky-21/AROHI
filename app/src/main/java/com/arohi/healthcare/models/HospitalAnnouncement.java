package com.arohi.healthcare.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a hospital announcement or health awareness post in /communityPosts.
 */
public class HospitalAnnouncement {
    private String id;
    private String title;
    private String content;
    private String category;
    private String authorUid;
    private String authorName;
    private String authorRole;
    private long createdAt;

    public HospitalAnnouncement() {
        // Required empty constructor for Firestore
    }

    public HospitalAnnouncement(String id, String title, String content, String category,
                                String authorUid, String authorName, String authorRole) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.authorUid = authorUid;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.createdAt = System.currentTimeMillis();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("title", title);
        map.put("content", content);
        map.put("category", category);
        map.put("authorUid", authorUid);
        map.put("authorName", authorName);
        map.put("authorRole", authorRole);
        map.put("createdAt", createdAt);
        return map;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAuthorUid() {
        return authorUid;
    }

    public void setAuthorUid(String authorUid) {
        this.authorUid = authorUid;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorRole() {
        return authorRole;
    }

    public void setAuthorRole(String authorRole) {
        this.authorRole = authorRole;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
