package com.scs.volunteer.dto;

public class CheckinAdjustmentAuditDTO {
    private String auditStatus;
    private String adminRemark;
    private String newStatus;
    private String newCheckinTime;
    private Double newServiceHours;
    private String hoursReason;

    public String getAuditStatus() { return auditStatus; }
    public void setAuditStatus(String auditStatus) { this.auditStatus = auditStatus; }
    public String getAdminRemark() { return adminRemark; }
    public void setAdminRemark(String adminRemark) { this.adminRemark = adminRemark; }
    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
    public String getNewCheckinTime() { return newCheckinTime; }
    public void setNewCheckinTime(String newCheckinTime) { this.newCheckinTime = newCheckinTime; }
    public Double getNewServiceHours() { return newServiceHours; }
    public void setNewServiceHours(Double newServiceHours) { this.newServiceHours = newServiceHours; }
    public String getHoursReason() { return hoursReason; }
    public void setHoursReason(String hoursReason) { this.hoursReason = hoursReason; }
}
