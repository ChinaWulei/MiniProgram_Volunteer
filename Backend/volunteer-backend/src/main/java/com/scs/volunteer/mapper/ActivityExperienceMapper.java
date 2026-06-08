package com.scs.volunteer.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class ActivityExperienceMapper {
    private final JdbcTemplate jdbcTemplate;

    public ActivityExperienceMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void adopt(Long activityId, Long evaluationId, String category, String type, String content, Long adminId) {
        jdbcTemplate.update("""
                insert into activity_experience(activity_id,evaluation_id,activity_category,experience_type,content,enabled,adopted_by)
                values(?,?,?,?,?,1,?)
                on duplicate key update content=values(content),enabled=1,adopted_by=values(adopted_by),updated_at=current_timestamp
                """, activityId, evaluationId, category, type, content, adminId);
    }

    public void unadopt(Long evaluationId, String type) {
        jdbcTemplate.update("delete from activity_experience where evaluation_id=? and experience_type=?", evaluationId, type);
    }

    public void setEnabled(Long id, boolean enabled) {
        jdbcTemplate.update("update activity_experience set enabled=? where id=?", enabled, id);
    }

    public List<Map<String, Object>> list(String category, Boolean enabled) {
        return jdbcTemplate.queryForList("""
                select x.id,x.activity_id as activityId,x.evaluation_id as evaluationId,
                       x.activity_category as activityCategory,x.experience_type as experienceType,
                       x.content,x.enabled,x.adopted_at as adoptedAt,a.name as activityName,u.name as adoptedByName
                from activity_experience x
                join activity a on a.id=x.activity_id
                join user u on u.id=x.adopted_by
                where (? is null or x.activity_category=?)
                  and (? is null or x.enabled=?)
                order by x.updated_at desc,x.id desc
                """, n(category), n(category), enabled, enabled);
    }

    public List<Map<String, Object>> enabledByCategory(String category, int limit) {
        return jdbcTemplate.queryForList("""
                select content,experience_type as experienceType,activity_category as activityCategory
                from activity_experience
                where enabled=1 and (? is null or activity_category=?)
                order by updated_at desc,id desc
                limit ?
                """, n(category), n(category), limit);
    }

    private String n(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
