package com.scs.volunteer.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class TimeZoneConfig {
    public static final String ZONE_ID = "Asia/Shanghai";

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZONE_ID));
    }
}
