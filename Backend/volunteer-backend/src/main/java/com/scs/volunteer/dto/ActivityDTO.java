package com.scs.volunteer.dto;

public class ActivityDTO {
    private String name;
    private String title;
    private String coverImageUrl;
    private String category;
    private String location;
    private Double latitude;
    private Double longitude;
    private String startTime;
    private String endTime;
    private String activityTime;
    private String signupStartTime;
    private String signupDeadline;
    private String checkinStartTime;
    private String checkinEndTime;
    private Integer recruitNumber;
    private Integer recruitCount;
    private String skillRequirements;
    private String[] requiredSkills;
    private String description;
    private String signupRequirement;
    private String requirements;
    private String contactName;
    private String contactPhone;
    private Double serviceHours;
    private String reviewMethod;
    private String auditMode;
    private String status;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getActivityTime() { return activityTime; }
    public void setActivityTime(String activityTime) { this.activityTime = activityTime; }
    public String getSignupStartTime() { return signupStartTime; }
    public void setSignupStartTime(String signupStartTime) { this.signupStartTime = signupStartTime; }
    public String getSignupDeadline() { return signupDeadline; }
    public void setSignupDeadline(String signupDeadline) { this.signupDeadline = signupDeadline; }
    public String getCheckinStartTime() { return checkinStartTime; }
    public void setCheckinStartTime(String checkinStartTime) { this.checkinStartTime = checkinStartTime; }
    public String getCheckinEndTime() { return checkinEndTime; }
    public void setCheckinEndTime(String checkinEndTime) { this.checkinEndTime = checkinEndTime; }
    public Integer getRecruitNumber() { return recruitNumber; }
    public void setRecruitNumber(Integer recruitNumber) { this.recruitNumber = recruitNumber; }
    public Integer getRecruitCount() { return recruitCount; }
    public void setRecruitCount(Integer recruitCount) { this.recruitCount = recruitCount; }
    public String getSkillRequirements() { return skillRequirements; }
    public void setSkillRequirements(String skillRequirements) { this.skillRequirements = skillRequirements; }
    public String[] getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(String[] requiredSkills) { this.requiredSkills = requiredSkills; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSignupRequirement() { return signupRequirement; }
    public void setSignupRequirement(String signupRequirement) { this.signupRequirement = signupRequirement; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public Double getServiceHours() { return serviceHours; }
    public void setServiceHours(Double serviceHours) { this.serviceHours = serviceHours; }
    public String getReviewMethod() { return reviewMethod; }
    public void setReviewMethod(String reviewMethod) { this.reviewMethod = reviewMethod; }
    public String getAuditMode() { return auditMode; }
    public void setAuditMode(String auditMode) { this.auditMode = auditMode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
