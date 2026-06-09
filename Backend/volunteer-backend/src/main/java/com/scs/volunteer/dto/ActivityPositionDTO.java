package com.scs.volunteer.dto;

public class ActivityPositionDTO {
    private Long id;
    private String name;
    private String startTime;
    private String endTime;
    private Integer recruitNumber;
    private String requirements;
    private Boolean requiresRehearsal;
    private String rehearsalStartTime;
    private String rehearsalEndTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public Integer getRecruitNumber() { return recruitNumber; }
    public void setRecruitNumber(Integer recruitNumber) { this.recruitNumber = recruitNumber; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
    public Boolean getRequiresRehearsal() { return requiresRehearsal; }
    public void setRequiresRehearsal(Boolean requiresRehearsal) { this.requiresRehearsal = requiresRehearsal; }
    public String getRehearsalStartTime() { return rehearsalStartTime; }
    public void setRehearsalStartTime(String rehearsalStartTime) { this.rehearsalStartTime = rehearsalStartTime; }
    public String getRehearsalEndTime() { return rehearsalEndTime; }
    public void setRehearsalEndTime(String rehearsalEndTime) { this.rehearsalEndTime = rehearsalEndTime; }
}
