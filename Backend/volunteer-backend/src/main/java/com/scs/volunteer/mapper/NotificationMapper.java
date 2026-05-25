package com.scs.volunteer.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class NotificationMapper {
    private final JdbcTemplate jdbcTemplate;

    public NotificationMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(Long userId, String type, String title, String content, String targetType, Long targetId) {
        jdbcTemplate.update("""
                insert into notification(user_id,type,title,content,target_type,target_id)
                values(?,?,?,?,?,?)
                """, userId, type, title, content, targetType, targetId);
    }

    public List<Map<String, Object>> list(Long userId) {
        return jdbcTemplate.queryForList("""
                select id,type,title,content,target_type as targetType,target_id as targetId,read_at as readAt,created_at as createdAt
                from notification where user_id=? order by created_at desc limit 50
                """, userId);
    }

    public int unreadCount(Long userId) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from notification where user_id=? and read_at is null", Integer.class, userId);
        return count == null ? 0 : count;
    }
}
