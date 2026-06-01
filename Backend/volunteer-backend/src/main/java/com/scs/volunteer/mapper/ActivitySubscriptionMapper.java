package com.scs.volunteer.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class ActivitySubscriptionMapper {
    private final JdbcTemplate jdbcTemplate;

    public ActivitySubscriptionMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> enabledCategories(Long userId) {
        return jdbcTemplate.queryForList("""
                select category
                from user_activity_subscription
                where user_id=? and enabled=1 and (wechat_enabled=1 or email_enabled=1)
                order by category
                """, String.class, userId);
    }

    public Map<String, Object> settings(Long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select email, max(wechat_enabled) as wechatEnabled, max(email_enabled) as emailEnabled
                from user_activity_subscription
                where user_id=? and enabled=1
                group by email
                order by email_enabled desc, wechat_enabled desc
                limit 1
                """, userId);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    public void replace(Long userId, List<String> categories, boolean enabled, boolean wechatEnabled, boolean emailEnabled, String email) {
        jdbcTemplate.update("delete from user_activity_subscription where user_id=?", userId);
        if (!enabled || categories == null || categories.isEmpty()) {
            return;
        }
        String normalizedEmail = email == null ? null : email.trim();
        for (String category : categories) {
            if (category == null || category.isBlank()) {
                continue;
            }
            jdbcTemplate.update("""
                    insert into user_activity_subscription(user_id, category, enabled, wechat_enabled, email_enabled, email)
                    values(?,?,?,?,?,?)
                    """, userId, category.trim(), 1, wechatEnabled, emailEnabled, normalizedEmail);
        }
    }

    public List<Map<String, Object>> subscribedUsers(String category) {
        return jdbcTemplate.queryForList("""
                select distinct u.id as userId, u.openid as openid, s.wechat_enabled as wechatEnabled,
                       s.email_enabled as emailEnabled, s.email as email
                from user_activity_subscription s
                join user u on u.id=s.user_id
                where s.enabled=1
                  and (s.wechat_enabled=1 or s.email_enabled=1)
                  and s.category=?
                  and u.role='VOLUNTEER'
                """, category);
    }
}
