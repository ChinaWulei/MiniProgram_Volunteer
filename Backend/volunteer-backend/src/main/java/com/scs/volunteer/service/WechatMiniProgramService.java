package com.scs.volunteer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.volunteer.config.WechatMiniProgramProperties;
import com.scs.volunteer.entity.Activity;
import com.scs.volunteer.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class WechatMiniProgramService {
    private static final Logger log = LoggerFactory.getLogger(WechatMiniProgramService.class);
    private static final DateTimeFormatter TEMPLATE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final WechatMiniProgramProperties properties;
    private final UserMapper userMapper;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private String accessToken;
    private long accessTokenExpireAt;

    public WechatMiniProgramService(WechatMiniProgramProperties properties, UserMapper userMapper, ObjectMapper objectMapper) {
        this.properties = properties;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    public void bindOpenid(Long userId, String code) {
        if (blank(code) || !configured()) {
            return;
        }
        try {
            String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + enc(properties.getAppid())
                    + "&secret=" + enc(properties.getSecret())
                    + "&js_code=" + enc(code)
                    + "&grant_type=authorization_code";
            String body = restClient.get().uri(url).retrieve().body(String.class);
            Map<String, Object> result = parse(body);
            Object openid = result.get("openid");
            if (openid != null && !String.valueOf(openid).isBlank()) {
                userMapper.bindOpenid(userId, String.valueOf(openid));
            } else {
                log.warn("Wechat jscode2session did not return openid: {}", body);
            }
        } catch (Exception e) {
            log.warn("Wechat openid binding failed for user {}", userId, e);
        }
    }

    public boolean sendActivityReminder(String openid, Activity activity) {
        if (blank(openid) || !configured() || blank(properties.getActivityReminderTemplateId())) {
            return false;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("touser", openid);
            payload.put("template_id", properties.getActivityReminderTemplateId());
            payload.put("page", "pages/activity-detail/activity-detail?id=" + activity.getId());
            payload.put("data", Map.of(
                    "thing4", Map.of("value", limit(activity.getName(), 20)),
                    "date5", Map.of("value", format(activity.getStartTime())),
                    "thing6", Map.of("value", limit(activity.getLocation(), 20)),
                    "thing7", Map.of("value", limit(reminderText(activity), 20))
            ));
            String requestBody = objectMapper.writeValueAsString(payload);
            WechatResponse response = restClient.post()
                    .uri("https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + accessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .exchange((request, clientResponse) -> new WechatResponse(
                            clientResponse.getStatusCode().value(),
                            StreamUtils.copyToString(clientResponse.getBody(), StandardCharsets.UTF_8)
                    ));
            String body = response.body();
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Wechat subscribe HTTP failed, status={}, body={}, templateId={}, openidTail={}, activityId={}, payload={}",
                        response.statusCode(), body, properties.getActivityReminderTemplateId(), tail(openid), activity.getId(), requestBody);
                return false;
            }
            Map<String, Object> result = parse(body);
            Object errcode = result.get("errcode");
            boolean ok = errcode instanceof Number number && number.intValue() == 0;
            if (!ok) {
                log.warn("Wechat subscribe message failed, body={}, templateId={}, openidTail={}, activityId={}, payload={}",
                        body, properties.getActivityReminderTemplateId(), tail(openid), activity.getId(), requestBody);
            }
            return ok;
        } catch (Exception e) {
            log.warn("Wechat subscribe message failed for activity {}", activity.getId(), e);
            return false;
        }
    }

    private String accessToken() {
        long now = System.currentTimeMillis();
        if (!blank(accessToken) && now < accessTokenExpireAt) {
            return accessToken;
        }
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                + enc(properties.getAppid()) + "&secret=" + enc(properties.getSecret());
        String body = restClient.get().uri(url).retrieve().body(String.class);
        Map<String, Object> result = parse(body);
        Object token = result.get("access_token");
        if (token == null || String.valueOf(token).isBlank()) {
            throw new IllegalStateException("Wechat access_token missing: " + body);
        }
        Object expires = result.get("expires_in");
        long expiresIn = expires instanceof Number number ? number.longValue() : 7200;
        accessToken = String.valueOf(token);
        accessTokenExpireAt = now + Math.max(60, expiresIn - 300) * 1000;
        return accessToken;
    }

    private Map<String, Object> parse(String body) {
        try {
            return objectMapper.readValue(body == null ? "{}" : body, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Wechat response parse failed", e);
        }
    }

    private String reminderText(Activity activity) {
        if (!blank(activity.getTips())) {
            return activity.getTips();
        }
        return "请按时参加志愿活动";
    }

    private String format(LocalDateTime time) {
        return time == null ? "" : TEMPLATE_DATE.format(time);
    }

    private String limit(String value, int max) {
        if (value == null) {
            return "";
        }
        String text = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return text.length() <= max ? text : text.substring(0, max);
    }

    private boolean configured() {
        return !blank(properties.getAppid()) && !blank(properties.getSecret());
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String tail(String value) {
        if (blank(value)) {
            return "";
        }
        return value.length() <= 6 ? value : value.substring(value.length() - 6);
    }

    private record WechatResponse(int statusCode, String body) {
    }
}
