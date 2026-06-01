package com.scs.volunteer.service;

import com.scs.volunteer.dto.SubscribeSettingsDTO;
import com.scs.volunteer.entity.Activity;
import com.scs.volunteer.mapper.ActivitySubscriptionMapper;
import com.scs.volunteer.mapper.NotificationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ActivitySubscriptionService {
    private static final Logger log = LoggerFactory.getLogger(ActivitySubscriptionService.class);
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ActivitySubscriptionMapper subscriptionMapper;
    private final NotificationMapper notificationMapper;
    private final WechatMiniProgramService wechatMiniProgramService;
    private final ActivityMailService activityMailService;

    public ActivitySubscriptionService(ActivitySubscriptionMapper subscriptionMapper,
                                       NotificationMapper notificationMapper,
                                       WechatMiniProgramService wechatMiniProgramService,
                                       ActivityMailService activityMailService) {
        this.subscriptionMapper = subscriptionMapper;
        this.notificationMapper = notificationMapper;
        this.wechatMiniProgramService = wechatMiniProgramService;
        this.activityMailService = activityMailService;
    }

    public Map<String, Object> settings(Long userId) {
        List<String> categories = subscriptionMapper.enabledCategories(userId);
        Map<String, Object> settings = subscriptionMapper.settings(userId);
        return Map.of(
                "enabled", !categories.isEmpty(),
                "wechatEnabled", asBoolean(settings.get("wechatEnabled")),
                "emailEnabled", asBoolean(settings.get("emailEnabled")),
                "email", settings.get("email") == null ? "" : settings.get("email"),
                "categories", categories
        );
    }

    public void save(Long userId, SubscribeSettingsDTO dto) {
        boolean enabled = dto != null && Boolean.TRUE.equals(dto.getEnabled());
        boolean wechatEnabled = enabled && Boolean.TRUE.equals(dto.getWechatEnabled());
        boolean emailEnabled = enabled && Boolean.TRUE.equals(dto.getEmailEnabled()) && validEmail(dto.getEmail());
        List<String> categories = dto == null ? List.of() : dto.getCategories();
        subscriptionMapper.replace(userId, categories == null ? List.of() : categories, enabled, wechatEnabled, emailEnabled, dto == null ? null : dto.getEmail());
        log.info("Activity subscription saved, userId={}, enabled={}, wechatEnabled={}, emailEnabled={}, email={}, categories={}",
                userId, enabled, wechatEnabled, emailEnabled, mask(dto == null ? null : dto.getEmail()), categories);
    }

    public boolean sendTestEmail(Long userId, String email) {
        String target = email == null || email.isBlank() ? String.valueOf(settings(userId).get("email")) : email;
        if (!validEmail(target)) {
            return false;
        }
        return activityMailService.sendTestEmail(target);
    }

    public void notifyActivityPublished(Activity activity) {
        if (activity == null || activity.getCategory() == null || activity.getCategory().isBlank()) {
            return;
        }
        List<Map<String, Object>> users = subscriptionMapper.subscribedUsers(activity.getCategory());
        log.info("Activity subscription matched, activityId={}, category={}, userCount={}", activity.getId(), activity.getCategory(), users.size());
        for (Map<String, Object> user : users) {
            Long userId = ((Number) user.get("userId")).longValue();
            String content = activity.getLocation() + "，" + DISPLAY_DATE.format(activity.getStartTime());
            notificationMapper.insert(userId, "ACTIVITY_SUBSCRIBE", activity.getName(), content, "ACTIVITY", activity.getId());
            Object openid = user.get("openid");
            log.info("Activity subscription channels, activityId={}, userId={}, wechatEnabled={}, hasOpenid={}, emailEnabled={}, email={}",
                    activity.getId(), userId, asBoolean(user.get("wechatEnabled")), openid != null,
                    asBoolean(user.get("emailEnabled")), mask(user.get("email") == null ? null : String.valueOf(user.get("email"))));
            if (asBoolean(user.get("wechatEnabled")) && openid != null) {
                wechatMiniProgramService.sendActivityReminder(String.valueOf(openid), activity);
            }
            Object email = user.get("email");
            if (asBoolean(user.get("emailEnabled")) && email != null) {
                activityMailService.sendActivityReminder(String.valueOf(email), activity);
            }
        }
    }

    private boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private boolean validEmail(String value) {
        return value != null && value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    private String mask(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return "";
        }
        String[] parts = email.split("@", 2);
        String prefix = parts[0].length() <= 2 ? parts[0] : parts[0].substring(0, 2) + "***";
        return prefix + "@" + parts[1];
    }
}
