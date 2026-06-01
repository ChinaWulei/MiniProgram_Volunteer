package com.scs.volunteer.service;

import com.scs.volunteer.config.ActivityMailProperties;
import com.scs.volunteer.entity.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class ActivityMailService {
    private static final Logger log = LoggerFactory.getLogger(ActivityMailService.class);
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final ActivityMailProperties activityMailProperties;

    public ActivityMailService(JavaMailSender mailSender, MailProperties mailProperties, ActivityMailProperties activityMailProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.activityMailProperties = activityMailProperties;
    }

    public boolean sendActivityReminder(String email, Activity activity) {
        if (blank(email) || activity == null || !configured()) {
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(first(activityMailProperties.getFrom(), mailProperties.getUsername()));
            message.setTo(email.trim());
            message.setSubject("志愿活动提醒：" + activity.getName());
            message.setText("""
                    你关注的志愿活动已发布。

                    活动名称：%s
                    活动时间：%s
                    活动地点：%s
                    活动类型：%s

                    温馨提示：%s
                    请进入数计志愿服务小程序查看详情并报名。
                    """.formatted(
                    safe(activity.getName()),
                    activity.getStartTime() == null ? "" : DISPLAY_DATE.format(activity.getStartTime()),
                    safe(activity.getLocation()),
                    safe(activity.getCategory()),
                    safe(first(activity.getTips(), "请按时参加志愿活动"))
            ));
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.warn("Activity email reminder failed, email={}, activityId={}", mask(email), activity.getId(), e);
            return false;
        }
    }

    private boolean configured() {
        return activityMailProperties.isEnabled() && !blank(mailProperties.getHost()) && !blank(first(activityMailProperties.getFrom(), mailProperties.getUsername()));
    }

    private String first(String value, String fallback) {
        return blank(value) ? fallback : value;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String mask(String email) {
        if (blank(email) || !email.contains("@")) {
            return "";
        }
        String[] parts = email.split("@", 2);
        String prefix = parts[0].length() <= 2 ? parts[0] : parts[0].substring(0, 2) + "***";
        return prefix + "@" + parts[1];
    }
}
