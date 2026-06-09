package com.scs.volunteer.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class NotificationMapper {
    private final JdbcTemplate jdbcTemplate;

    public NotificationMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(Long userId, String type, String title, String content, String targetType, Long targetId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    insert into notification(user_id,type,title,content,target_type,target_id)
                    values(?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, userId);
            statement.setString(2, type);
            statement.setString(3, title);
            statement.setString(4, content);
            statement.setString(5, targetType);
            if (targetId == null) statement.setObject(6, null); else statement.setLong(6, targetId);
            return statement;
        }, keyHolder);
        return keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
    }

    public List<Map<String, Object>> list(Long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select id,type,title,content,target_type as targetType,target_id as targetId,read_at as readAt,created_at as createdAt
                from notification where user_id=? order by created_at desc limit 50
                """, userId);
        for (Map<String, Object> row : rows) {
            row.put("attachments", attachments(((Number) row.get("id")).longValue()));
        }
        return rows;
    }

    public Optional<Map<String, Object>> find(Long userId, Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select id,type,title,content,target_type as targetType,target_id as targetId,
                       read_at as readAt,created_at as createdAt
                from notification
                where id=? and user_id=?
                """, id, userId);
        if (rows.isEmpty()) return Optional.empty();
        Map<String, Object> row = rows.get(0);
        row.put("attachments", attachments(id));
        return Optional.of(row);
    }

    public void addAttachments(Long notificationId, List<Long> ruleFileIds) {
        if (notificationId == null || ruleFileIds == null) return;
        for (Long ruleFileId : ruleFileIds) {
            if (ruleFileId == null) continue;
            jdbcTemplate.update("""
                    insert ignore into notification_attachment(notification_id,rule_file_id)
                    values(?,?)
                    """, notificationId, ruleFileId);
        }
    }

    public List<Map<String, Object>> attachments(Long notificationId) {
        return jdbcTemplate.queryForList("""
                select a.rule_file_id as ruleFileId,f.original_name as fileName,
                       f.file_type as fileType,f.s3_url as url
                from notification_attachment a
                join rule_file f on f.id=a.rule_file_id
                where a.notification_id=?
                order by a.id
                """, notificationId);
    }

    public int unreadCount(Long userId) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from notification where user_id=? and read_at is null", Integer.class, userId);
        return count == null ? 0 : count;
    }

    public void markRead(Long userId, Long id) {
        jdbcTemplate.update("update notification set read_at=coalesce(read_at, now()) where id=? and user_id=?", id, userId);
    }
}
