package com.scs.volunteer.dto;

public class ActivityInviteRequest {
    private Long receiverId;
    private Long activityId;
    private String reason;

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
