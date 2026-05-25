package com.scs.volunteer.dto;

public class ManualCheckinRequest {
    private Long userId;
    private String reason;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
