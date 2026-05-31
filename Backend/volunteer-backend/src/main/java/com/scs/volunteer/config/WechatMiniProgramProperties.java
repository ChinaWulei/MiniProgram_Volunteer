package com.scs.volunteer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.wechat")
public class WechatMiniProgramProperties {
    private String appid;
    private String secret;
    private String activityReminderTemplateId;

    public String getAppid() { return appid; }
    public void setAppid(String appid) { this.appid = appid; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public String getActivityReminderTemplateId() { return activityReminderTemplateId; }
    public void setActivityReminderTemplateId(String activityReminderTemplateId) { this.activityReminderTemplateId = activityReminderTemplateId; }
}
