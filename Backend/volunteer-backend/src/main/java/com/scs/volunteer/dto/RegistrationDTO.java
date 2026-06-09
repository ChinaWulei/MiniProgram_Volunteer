package com.scs.volunteer.dto;

public class RegistrationDTO {
    private Long activityId;
    private Long positionId;
    private Boolean transportRequired;
    private String boardingPoint;

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public Long getPositionId() { return positionId; }
    public void setPositionId(Long positionId) { this.positionId = positionId; }
    public Boolean getTransportRequired() { return transportRequired; }
    public void setTransportRequired(Boolean transportRequired) { this.transportRequired = transportRequired; }
    public String getBoardingPoint() { return boardingPoint; }
    public void setBoardingPoint(String boardingPoint) { this.boardingPoint = boardingPoint; }
}
