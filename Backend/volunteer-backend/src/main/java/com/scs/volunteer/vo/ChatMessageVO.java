package com.scs.volunteer.vo;

import java.time.LocalDateTime;

public class ChatMessageVO {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private Long receiverId;
    private String type;
    private String content;
    private Long activityId;
    private String activityName;
    private String activityTime;
    private String location;
    private Integer remainingSlots;
    private String inviteStatus;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }
    public String getActivityTime() { return activityTime; }
    public void setActivityTime(String activityTime) { this.activityTime = activityTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Integer getRemainingSlots() { return remainingSlots; }
    public void setRemainingSlots(Integer remainingSlots) { this.remainingSlots = remainingSlots; }
    public String getInviteStatus() { return inviteStatus; }
    public void setInviteStatus(String inviteStatus) { this.inviteStatus = inviteStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
