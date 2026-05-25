package com.scs.volunteer.vo;

public class VolunteerVO {
    private Long userId;
    private String name;
    private String nickname;
    private String avatarUrl;
    private String identityNo;
    private String phone;
    private String college;
    private String majorClass;
    private String skillTags;
    private String availableTime;
    private String bio;
    private Double totalHours;
    private Integer creditScore;
    private Integer serviceCount;
    private String recentActivity;
    private String volunteerLevel;
    private Integer volunteerPoints;
    private String badges;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getIdentityNo() { return identityNo; }
    public void setIdentityNo(String identityNo) { this.identityNo = identityNo; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
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
    public String getRecentActivity() { return recentActivity; }
    public void setRecentActivity(String recentActivity) { this.recentActivity = recentActivity; }
    public String getVolunteerLevel() { return volunteerLevel; }
    public void setVolunteerLevel(String volunteerLevel) { this.volunteerLevel = volunteerLevel; }
    public Integer getVolunteerPoints() { return volunteerPoints; }
    public void setVolunteerPoints(Integer volunteerPoints) { this.volunteerPoints = volunteerPoints; }
    public String getBadges() { return badges; }
    public void setBadges(String badges) { this.badges = badges; }
}
