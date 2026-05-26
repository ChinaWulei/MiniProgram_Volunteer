package com.scs.volunteer.vo;

import java.util.List;

public class UserProfileVO {
    private Long userId;
    private String avatarUrl;
    private String nickname;
    private String name;
    private String volunteerNo;
    private String college;
    private String majorClass;
    private String phone;
    private String skillTags;
    private String availableTime;
    private String bio;
    private Double totalHours;
    private Integer creditScore;
    private String creditLevel;
    private List<java.util.Map<String, Object>> creditRecords;
    private Integer serviceCount;
    private Integer volunteerPoints;
    private String volunteerLevel;
    private String levelName;
    private Double levelMinHours;
    private Double nextLevelHours;
    private Double nextLevelRemainingHours;
    private Integer levelProgress;
    private Integer campusRank;
    private List<String> badges;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVolunteerNo() { return volunteerNo; }
    public void setVolunteerNo(String volunteerNo) { this.volunteerNo = volunteerNo; }
    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }
    public String getMajorClass() { return majorClass; }
    public void setMajorClass(String majorClass) { this.majorClass = majorClass; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
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
    public String getCreditLevel() { return creditLevel; }
    public void setCreditLevel(String creditLevel) { this.creditLevel = creditLevel; }
    public List<java.util.Map<String, Object>> getCreditRecords() { return creditRecords; }
    public void setCreditRecords(List<java.util.Map<String, Object>> creditRecords) { this.creditRecords = creditRecords; }
    public Integer getServiceCount() { return serviceCount; }
    public void setServiceCount(Integer serviceCount) { this.serviceCount = serviceCount; }
    public Integer getVolunteerPoints() { return volunteerPoints; }
    public void setVolunteerPoints(Integer volunteerPoints) { this.volunteerPoints = volunteerPoints; }
    public String getVolunteerLevel() { return volunteerLevel; }
    public void setVolunteerLevel(String volunteerLevel) { this.volunteerLevel = volunteerLevel; }
    public String getLevelName() { return levelName; }
    public void setLevelName(String levelName) { this.levelName = levelName; }
    public Double getLevelMinHours() { return levelMinHours; }
    public void setLevelMinHours(Double levelMinHours) { this.levelMinHours = levelMinHours; }
    public Double getNextLevelHours() { return nextLevelHours; }
    public void setNextLevelHours(Double nextLevelHours) { this.nextLevelHours = nextLevelHours; }
    public Double getNextLevelRemainingHours() { return nextLevelRemainingHours; }
    public void setNextLevelRemainingHours(Double nextLevelRemainingHours) { this.nextLevelRemainingHours = nextLevelRemainingHours; }
    public Integer getLevelProgress() { return levelProgress; }
    public void setLevelProgress(Integer levelProgress) { this.levelProgress = levelProgress; }
    public Integer getCampusRank() { return campusRank; }
    public void setCampusRank(Integer campusRank) { this.campusRank = campusRank; }
    public List<String> getBadges() { return badges; }
    public void setBadges(List<String> badges) { this.badges = badges; }
}
