package com.scs.volunteer.dto;

import java.util.List;

public class SubscribeSettingsDTO {
    private Boolean enabled;
    private Boolean wechatEnabled;
    private List<String> categories;
    private Boolean emailEnabled;
    private String email;

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getWechatEnabled() { return wechatEnabled; }
    public void setWechatEnabled(Boolean wechatEnabled) { this.wechatEnabled = wechatEnabled; }
    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }
    public Boolean getEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(Boolean emailEnabled) { this.emailEnabled = emailEnabled; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
