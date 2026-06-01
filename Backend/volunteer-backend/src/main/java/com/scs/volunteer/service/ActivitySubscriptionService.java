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
    private static final double SEMANTIC_THRESHOLD = 0.72;

    private final ActivitySubscriptionMapper subscriptionMapper;
    private final NotificationMapper notificationMapper;
    private final WechatMiniProgramService wechatMiniProgramService;
    private final ActivityMailService activityMailService;
    private final EmbeddingService embeddingService;

    public ActivitySubscriptionService(ActivitySubscriptionMapper subscriptionMapper,
                                       NotificationMapper notificationMapper,
                                       WechatMiniProgramService wechatMiniProgramService,
                                       ActivityMailService activityMailService,
                                       EmbeddingService embeddingService) {
        this.subscriptionMapper = subscriptionMapper;
        this.notificationMapper = notificationMapper;
        this.wechatMiniProgramService = wechatMiniProgramService;
        this.activityMailService = activityMailService;
        this.embeddingService = embeddingService;
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
        List<Map<String, Object>> users = matchedUsers(activity);
        log.info("Activity subscription matched, activityId={}, category={}, userCount={}", activity.getId(), activity.getCategory(), users.size());
        for (Map<String, Object> user : users) {
            Long userId = ((Number) user.get("userId")).longValue();
            String content = activity.getLocation() + "，" + DISPLAY_DATE.format(activity.getStartTime());
            notificationMapper.insert(userId, "ACTIVITY_SUBSCRIBE", activity.getName(), content, "ACTIVITY", activity.getId());
            Object openid = user.get("openid");
            log.info("Activity subscription channels, activityId={}, userId={}, matchMode={}, matchCategory={}, matchScore={}, wechatEnabled={}, hasOpenid={}, emailEnabled={}, email={}",
                    activity.getId(), userId, user.get("matchMode"), user.get("matchCategory"), user.get("matchScore"),
                    asBoolean(user.get("wechatEnabled")), openid != null,
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

    private List<Map<String, Object>> matchedUsers(Activity activity) {
        java.util.Map<Long, Map<String, Object>> matched = new java.util.LinkedHashMap<>();
        for (Map<String, Object> user : subscriptionMapper.subscribedUsers(activity.getCategory())) {
            Long userId = ((Number) user.get("userId")).longValue();
            user.put("matchMode", "EXACT");
            user.put("matchScore", 1.0);
            user.put("matchCategory", activity.getCategory());
            matched.put(userId, user);
        }

        try {
            float[] activityEmbedding = embeddingService.embed(activitySemanticText(activity));
            for (Map<String, Object> user : subscriptionMapper.enabledSubscriptions()) {
                Long userId = ((Number) user.get("userId")).longValue();
                if (matched.containsKey(userId)) {
                    continue;
                }
                String category = String.valueOf(user.get("category"));
                double score = cosine(activityEmbedding, embeddingService.embed(category));
                if (score >= SEMANTIC_THRESHOLD) {
                    user.put("matchMode", "SEMANTIC");
                    user.put("matchScore", score);
                    user.put("matchCategory", category);
                    matched.put(userId, user);
                }
            }
        } catch (Exception e) {
            log.warn("Activity semantic subscription matching skipped, activityId={}", activity.getId(), e);
        }

        return new java.util.ArrayList<>(matched.values());
    }

    private String activitySemanticText(Activity activity) {
        return String.join("\n",
                safe(activity.getName()),
                safe(activity.getCategory()),
                safe(activity.getDescription()),
                safe(activity.getSkillRequirements()),
                safe(activity.getSignupRequirement())
        );
    }

    private double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0) {
            return 0;
        }
        int length = Math.min(a.length, b.length);
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
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

    private String safe(String value) {
        return value == null ? "" : value;
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
