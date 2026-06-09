package com.scs.volunteer.dto;

public class ActivityPriorityRuleDTO {
    private Long id;
    private String ruleType;
    private String ruleValue;
    private Integer weight;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    public String getRuleValue() { return ruleValue; }
    public void setRuleValue(String ruleValue) { this.ruleValue = ruleValue; }
    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }
}
