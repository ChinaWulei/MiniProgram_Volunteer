package com.scs.volunteer.vo;

import java.util.List;

public class ActivityAiGenerateVO {
    private String title;
    private String category;
    private String description;
    private String requirements;
    private List<String> skills;
    private Integer recruitCount;
    private Double serviceHours;
    private String tips;
    private String coverUrl;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }
    public Integer getRecruitCount() { return recruitCount; }
    public void setRecruitCount(Integer recruitCount) { this.recruitCount = recruitCount; }
    public Double getServiceHours() { return serviceHours; }
    public void setServiceHours(Double serviceHours) { this.serviceHours = serviceHours; }
    public String getTips() { return tips; }
    public void setTips(String tips) { this.tips = tips; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
}
