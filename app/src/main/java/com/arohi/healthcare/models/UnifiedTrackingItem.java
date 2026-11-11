package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain-agnostic tracking projection rendered inside the Unified Tracking Dashboard.
 * Synthesizes active & historical records from Pharmacy Orders, Appointments, Operations,
 * Payments, and Ambulance Dispatch under a strict hospitalId multi-tenant boundary.
 */
public class UnifiedTrackingItem implements Serializable {

    private String itemId;
    private String hospitalId;
    private TrackingCategory category;
    private String title;
    private String subtitle;
    private String detailSummary;
    private String currentStatus;
    private int currentStepIndex;
    private int totalSteps;
    private boolean terminal;
    private boolean active;
    private boolean hasLiveGps;
    private String etaOrScheduleText;
    private String actorSummary;
    private long updatedAt;
    private List<TrackingStatusEvent> timeline;
    private List<String> expectedStages;

    public UnifiedTrackingItem() {
        this.timeline = new ArrayList<>();
        this.expectedStages = new ArrayList<>();
    }

    public int getProgressPercent() {
        if (totalSteps <= 1) return terminal ? 100 : 0;
        if (currentStepIndex < 0) return 0;
        int pct = (int) Math.round(((double) currentStepIndex / (totalSteps - 1)) * 100.0);
        return Math.max(0, Math.min(100, pct));
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public TrackingCategory getCategory() { return category; }
    public void setCategory(TrackingCategory category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getDetailSummary() { return detailSummary; }
    public void setDetailSummary(String detailSummary) { this.detailSummary = detailSummary; }

    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }

    public int getCurrentStepIndex() { return currentStepIndex; }
    public void setCurrentStepIndex(int currentStepIndex) { this.currentStepIndex = currentStepIndex; }

    public int getTotalSteps() { return totalSteps; }
    public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }

    public boolean isTerminal() { return terminal; }
    public void setTerminal(boolean terminal) { this.terminal = terminal; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isHasLiveGps() { return hasLiveGps; }
    public void setHasLiveGps(boolean hasLiveGps) { this.hasLiveGps = hasLiveGps; }

    public String getEtaOrScheduleText() { return etaOrScheduleText; }
    public void setEtaOrScheduleText(String etaOrScheduleText) { this.etaOrScheduleText = etaOrScheduleText; }

    public String getActorSummary() { return actorSummary; }
    public void setActorSummary(String actorSummary) { this.actorSummary = actorSummary; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public List<TrackingStatusEvent> getTimeline() { return timeline; }
    public void setTimeline(List<TrackingStatusEvent> timeline) { this.timeline = timeline != null ? timeline : new ArrayList<>(); }

    public List<String> getExpectedStages() { return expectedStages; }
    public void setExpectedStages(List<String> expectedStages) { this.expectedStages = expectedStages != null ? expectedStages : new ArrayList<>(); }
}
