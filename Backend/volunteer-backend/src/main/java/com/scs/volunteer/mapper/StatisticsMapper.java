package com.scs.volunteer.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class StatisticsMapper {
    private final JdbcTemplate jdbcTemplate;
    private final RegistrationMapper registrationMapper;

    public StatisticsMapper(JdbcTemplate jdbcTemplate, RegistrationMapper registrationMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.registrationMapper = registrationMapper;
    }

    public Map<String, Object> overview() {
        Map<String, Object> data = new HashMap<>();
        data.put("activityTotal", jdbcTemplate.queryForObject("select count(*) from activity", Integer.class));
        data.put("volunteerTotal", jdbcTemplate.queryForObject("select count(*) from user where role='VOLUNTEER'", Integer.class));
        data.put("registrationTotal", jdbcTemplate.queryForObject("select count(*) from registration", Integer.class));
        data.put("totalHours", jdbcTemplate.queryForObject("select coalesce(sum(total_hours),0) from volunteer_profile", Double.class));
        data.put("categoryStats", jdbcTemplate.queryForList("select category,count(*) as count from activity group by category"));
        data.put("recentRegistrations", registrationMapper.recent());
        return data;
    }
}
