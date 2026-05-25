package com.scs.volunteer.dto;

public class ReviewDTO {
    private String status;
    private String reviewRemark;
    private String reason;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReviewRemark() { return reviewRemark; }
    public void setReviewRemark(String reviewRemark) { this.reviewRemark = reviewRemark; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
