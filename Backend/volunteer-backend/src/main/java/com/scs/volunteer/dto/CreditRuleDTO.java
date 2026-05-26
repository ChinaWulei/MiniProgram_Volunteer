package com.scs.volunteer.dto;

public class CreditRuleDTO {
    private String code;
    private String name;
    private Integer changeValue;
    private Boolean enabled;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getChangeValue() { return changeValue; }
    public void setChangeValue(Integer changeValue) { this.changeValue = changeValue; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
