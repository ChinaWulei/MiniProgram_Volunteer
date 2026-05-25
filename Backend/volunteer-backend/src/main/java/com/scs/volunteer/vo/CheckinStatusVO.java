package com.scs.volunteer.vo;

import java.time.LocalDateTime;

public class CheckinStatusVO {
    private String status;
    private String statusText;
    private LocalDateTime checkinTime;
    private String method;
    private Double distanceMeters;
    private String manualReason;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public LocalDateTime getCheckinTime() { return checkinTime; }
    public void setCheckinTime(LocalDateTime checkinTime) { this.checkinTime = checkinTime; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public Double getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(Double distanceMeters) { this.distanceMeters = distanceMeters; }
    public String getManualReason() { return manualReason; }
    public void setManualReason(String manualReason) { this.manualReason = manualReason; }
}
