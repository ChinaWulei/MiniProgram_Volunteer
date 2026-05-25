package com.scs.volunteer.dto;

import java.util.List;

public class ActivityNewsDTO {
    private Long activityId;
    private String title;
    private String content;
    private String resultSummary;
    private List<String> imageUrls;
    private String status;

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
