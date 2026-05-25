package com.scs.volunteer.dto;

public class UserProfileDTO {
    private String nickname;
    private String college;
    private String majorClass;
    private String phone;
    private String skillTags;
    private String availableTime;
    private String bio;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
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
}
