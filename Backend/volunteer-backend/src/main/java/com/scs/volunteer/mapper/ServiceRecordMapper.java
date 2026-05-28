package com.scs.volunteer.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ServiceRecordMapper {
    private final JdbcTemplate jdbcTemplate;

    public ServiceRecordMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(Long userId, Long activityId, double hours, String comment) {
        jdbcTemplate.update("insert into service_record(user_id,activity_id,hours,comment) values(?,?,?,?)",
                userId, activityId, hours, comment);
    }

    public double totalHours(Long userId, Long activityId) {
        Double value = jdbcTemplate.queryForObject(
                "select coalesce(sum(hours),0) from service_record where user_id=? and activity_id=?",
                Double.class, userId, activityId);
        return value == null ? 0D : value;
    }

    public void replace(Long userId, Long activityId, double hours, String comment) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from service_record where user_id=? and activity_id=?",
                Integer.class, userId, activityId);
        if (count != null && count > 0) {
            jdbcTemplate.update("""
                    update service_record set hours=?,comment=?,created_at=created_at
                    where user_id=? and activity_id=?
                    """, hours, comment, userId, activityId);
        } else {
            insert(userId, activityId, hours, comment);
        }
    }
}
