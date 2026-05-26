package com.scs.volunteer.entity;

import java.time.LocalDateTime;

public class Activity {
    private Long id;
    private String name;
    private String coverImageUrl;
    private String category;
    private String location;
    private Double latitude;
    private Double longitude;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime signupStartTime;
    private LocalDateTime signupDeadline;
    private LocalDateTime checkinStartTime;
    private LocalDateTime checkinEndTime;
    private Integer recruitNumber;
    private Integer registeredNumber;
    private String skillRequirements;
    private String description;
    private String signupRequirement;
    private String contactName;
    private String contactPhone;
    private Double serviceHours;
    private String reviewMethod;
    private String status;
    private Long createdBy;
    private LocalDateTime finishedAt;
    private LocalDateTime publishedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
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
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public LocalDateTime getSignupStartTime() { return signupStartTime; }
    public void setSignupStartTime(LocalDateTime signupStartTime) { this.signupStartTime = signupStartTime; }
    public LocalDateTime getSignupDeadline() { return signupDeadline; }
    public void setSignupDeadline(LocalDateTime signupDeadline) { this.signupDeadline = signupDeadline; }
    public LocalDateTime getCheckinStartTime() { return checkinStartTime; }
    public void setCheckinStartTime(LocalDateTime checkinStartTime) { this.checkinStartTime = checkinStartTime; }
    public LocalDateTime getCheckinEndTime() { return checkinEndTime; }
    public void setCheckinEndTime(LocalDateTime checkinEndTime) { this.checkinEndTime = checkinEndTime; }
    public Integer getRecruitNumber() { return recruitNumber; }
    public void setRecruitNumber(Integer recruitNumber) { this.recruitNumber = recruitNumber; }
    public Integer getRegisteredNumber() { return registeredNumber; }
    public void setRegisteredNumber(Integer registeredNumber) { this.registeredNumber = registeredNumber; }
    public String getSkillRequirements() { return skillRequirements; }
    public void setSkillRequirements(String skillRequirements) { this.skillRequirements = skillRequirements; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSignupRequirement() { return signupRequirement; }
    public void setSignupRequirement(String signupRequirement) { this.signupRequirement = signupRequirement; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public Double getServiceHours() { return serviceHours; }
    public void setServiceHours(Double serviceHours) { this.serviceHours = serviceHours; }
    public String getReviewMethod() { return reviewMethod; }
    public void setReviewMethod(String reviewMethod) { this.reviewMethod = reviewMethod; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
}
