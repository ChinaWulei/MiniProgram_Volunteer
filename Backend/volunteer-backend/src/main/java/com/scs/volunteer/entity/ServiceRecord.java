package com.scs.volunteer.entity;

import java.time.LocalDateTime;

public class ServiceRecord {
    private Long id;
    private Long userId;
    private Long activityId;
    private Double hours;
    private String comment;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public Double getHours() { return hours; }
    public void setHours(Double hours) { this.hours = hours; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
