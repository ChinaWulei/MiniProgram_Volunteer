package com.scs.volunteer.dto;

public class RegisterDTO {
    private String username;
    private String password;
    private String name;
    private String identityNo;
    private String phone;
    private String role;
    private String college;
    private String majorClass;
    private String skillTags;
    private String availableTime;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIdentityNo() { return identityNo; }
    public void setIdentityNo(String identityNo) { this.identityNo = identityNo; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }
    public String getMajorClass() { return majorClass; }
    public void setMajorClass(String majorClass) { this.majorClass = majorClass; }
    public String getSkillTags() { return skillTags; }
    public void setSkillTags(String skillTags) { this.skillTags = skillTags; }
    public String getAvailableTime() { return availableTime; }
    public void setAvailableTime(String availableTime) { this.availableTime = availableTime; }
}
