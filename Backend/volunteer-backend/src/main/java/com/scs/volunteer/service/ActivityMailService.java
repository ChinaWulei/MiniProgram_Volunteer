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
        if (blank(email) || activity == null) {
            log.warn("Activity email reminder skipped, emailBlank={}, activityNull={}", blank(email), activity == null);
            return false;
        }
        if (!configured()) {
            log.warn("Activity email reminder skipped, mail not configured: enabled={}, hostBlank={}, fromBlank={}",
                    activityMailProperties.isEnabled(), blank(mailProperties.getHost()), blank(fromAddress()));
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress());
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
            log.info("Activity email reminder sent, email={}, activityId={}", mask(email), activity.getId());
            return true;
        } catch (Exception e) {
            log.warn("Activity email reminder failed, email={}, activityId={}", mask(email), activity.getId(), e);
            return false;
        }
    }

    public boolean sendTestEmail(String email) {
        if (blank(email)) {
            log.warn("Activity test email skipped, email is blank");
            return false;
        }
        if (!configured()) {
            log.warn("Activity test email skipped, mail not configured: enabled={}, hostBlank={}, fromBlank={}",
                    activityMailProperties.isEnabled(), blank(mailProperties.getHost()), blank(fromAddress()));
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress());
            message.setTo(email.trim());
            message.setSubject("志愿活动邮箱提醒测试");
            message.setText("这是一封测试邮件。收到后说明志愿活动邮箱提醒通道已配置成功。");
            mailSender.send(message);
            log.info("Activity test email sent, email={}", mask(email));
            return true;
        } catch (Exception e) {
            log.warn("Activity test email failed, email={}", mask(email), e);
            return false;
        }
    }

    private boolean configured() {
        return activityMailProperties.isEnabled() && !blank(mailProperties.getHost()) && !blank(fromAddress());
    }

    private String fromAddress() {
        return first(activityMailProperties.getFrom(), mailProperties.getUsername());
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
