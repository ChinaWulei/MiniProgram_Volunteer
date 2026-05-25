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
}
