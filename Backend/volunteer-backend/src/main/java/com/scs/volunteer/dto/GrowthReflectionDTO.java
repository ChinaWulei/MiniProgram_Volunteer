package com.scs.volunteer.dto;

public class GrowthReflectionDTO {
    private Long activityId;
    private String content;
    private Boolean anonymous;

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Boolean getAnonymous() { return anonymous; }
    public void setAnonymous(Boolean anonymous) { this.anonymous = anonymous; }
}
