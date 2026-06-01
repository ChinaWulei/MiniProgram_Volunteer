package com.scs.volunteer.service;

import com.scs.volunteer.dto.SubscribeSettingsDTO;
import com.scs.volunteer.entity.Activity;
import com.scs.volunteer.mapper.ActivitySubscriptionMapper;
import com.scs.volunteer.mapper.NotificationMapper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ActivitySubscriptionService {
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
    }

    public void notifyActivityPublished(Activity activity) {
        if (activity == null || activity.getCategory() == null || activity.getCategory().isBlank()) {
            return;
        }
        List<Map<String, Object>> users = subscriptionMapper.subscribedUsers(activity.getCategory());
        for (Map<String, Object> user : users) {
            Long userId = ((Number) user.get("userId")).longValue();
            String content = activity.getLocation() + "，" + DISPLAY_DATE.format(activity.getStartTime());
            notificationMapper.insert(userId, "ACTIVITY_SUBSCRIBE", activity.getName(), content, "ACTIVITY", activity.getId());
            Object openid = user.get("openid");
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
}
