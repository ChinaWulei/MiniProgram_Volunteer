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

    public ActivitySubscriptionService(ActivitySubscriptionMapper subscriptionMapper,
                                       NotificationMapper notificationMapper,
                                       WechatMiniProgramService wechatMiniProgramService) {
        this.subscriptionMapper = subscriptionMapper;
        this.notificationMapper = notificationMapper;
        this.wechatMiniProgramService = wechatMiniProgramService;
    }

    public Map<String, Object> settings(Long userId) {
        List<String> categories = subscriptionMapper.enabledCategories(userId);
        return Map.of("enabled", !categories.isEmpty(), "categories", categories);
    }

    public void save(Long userId, SubscribeSettingsDTO dto) {
        boolean enabled = dto != null && Boolean.TRUE.equals(dto.getEnabled());
        List<String> categories = dto == null ? List.of() : dto.getCategories();
        subscriptionMapper.replace(userId, categories == null ? List.of() : categories, enabled);
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
            if (openid != null) {
                wechatMiniProgramService.sendActivityReminder(String.valueOf(openid), activity);
            }
        }
    }
}
