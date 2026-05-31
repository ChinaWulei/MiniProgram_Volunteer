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
                where user_id=? and enabled=1
                order by category
                """, String.class, userId);
    }

    public void replace(Long userId, List<String> categories, boolean enabled) {
        jdbcTemplate.update("delete from user_activity_subscription where user_id=?", userId);
        if (!enabled || categories == null || categories.isEmpty()) {
            return;
        }
        for (String category : categories) {
            if (category == null || category.isBlank()) {
                continue;
            }
            jdbcTemplate.update("""
                    insert into user_activity_subscription(user_id, category, enabled)
                    values(?,?,1)
                    """, userId, category.trim());
        }
    }

    public List<Map<String, Object>> subscribedUsers(String category) {
        return jdbcTemplate.queryForList("""
                select distinct u.id as userId, u.openid as openid
                from user_activity_subscription s
                join user u on u.id=s.user_id
                where s.enabled=1
                  and s.category=?
                  and u.role='VOLUNTEER'
                """, category);
    }
}
