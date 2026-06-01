package com.scs.volunteer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.mail")
public class ActivityMailProperties {
    private boolean enabled = true;
    private String from;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
}
