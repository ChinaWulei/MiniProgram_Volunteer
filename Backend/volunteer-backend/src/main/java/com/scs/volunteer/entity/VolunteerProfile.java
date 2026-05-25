package com.scs.volunteer.entity;

public class VolunteerProfile {
    private Long id;
    private Long userId;
    private String college;
    private String majorClass;
    private String skillTags;
    private String availableTime;
    private String bio;
    private Double totalHours;
    private Integer creditScore;
    private Integer serviceCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }
    public String getMajorClass() { return majorClass; }
    public void setMajorClass(String majorClass) { this.majorClass = majorClass; }
    public String getSkillTags() { return skillTags; }
    public void setSkillTags(String skillTags) { this.skillTags = skillTags; }
    public String getAvailableTime() { return availableTime; }
    public void setAvailableTime(String availableTime) { this.availableTime = availableTime; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public Double getTotalHours() { return totalHours; }
    public void setTotalHours(Double totalHours) { this.totalHours = totalHours; }
    public Integer getCreditScore() { return creditScore; }
    public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }
    public Integer getServiceCount() { return serviceCount; }
    public void setServiceCount(Integer serviceCount) { this.serviceCount = serviceCount; }
}
