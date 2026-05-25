package com.scs.volunteer.vo;

import java.time.LocalDateTime;

public class AiActivityCandidateVO {
    private Long id;
    private String name;
    private String category;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String skillRequirements;
    private String description;
    private Double serviceHours;
    private Integer remainingSlots;
    private String reason;
    private Integer score;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getSkillRequirements() { return skillRequirements; }
    public void setSkillRequirements(String skillRequirements) { this.skillRequirements = skillRequirements; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getServiceHours() { return serviceHours; }
    public void setServiceHours(Double serviceHours) { this.serviceHours = serviceHours; }
    public Integer getRemainingSlots() { return remainingSlots; }
    public void setRemainingSlots(Integer remainingSlots) { this.remainingSlots = remainingSlots; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
}
