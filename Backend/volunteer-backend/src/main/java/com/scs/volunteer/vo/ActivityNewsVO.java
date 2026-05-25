package com.scs.volunteer.vo;

import java.time.LocalDateTime;
import java.util.List;

public class ActivityNewsVO {
    private Long id;
    private Long activityId;
    private String activityName;
    private String activityTime;
    private String location;
    private Integer participantCount;
    private Double totalServiceHours;
    private String title;
    private String content;
    private String resultSummary;
    private String status;
    private Integer readCount;
    private LocalDateTime publishedAt;
    private List<String> imageUrls;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }
    public String getActivityTime() { return activityTime; }
    public void setActivityTime(String activityTime) { this.activityTime = activityTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Integer getParticipantCount() { return participantCount; }
    public void setParticipantCount(Integer participantCount) { this.participantCount = participantCount; }
    public Double getTotalServiceHours() { return totalServiceHours; }
    public void setTotalServiceHours(Double totalServiceHours) { this.totalServiceHours = totalServiceHours; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getReadCount() { return readCount; }
    public void setReadCount(Integer readCount) { this.readCount = readCount; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
}
