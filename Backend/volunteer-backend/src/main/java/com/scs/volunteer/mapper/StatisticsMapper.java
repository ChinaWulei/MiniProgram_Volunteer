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
        data.put("pendingRegistrationTotal", jdbcTemplate.queryForObject("select count(*) from registration where status='待审核'", Integer.class));
        data.put("totalHours", jdbcTemplate.queryForObject("select coalesce(sum(total_hours),0) from volunteer_profile", Double.class));
        data.put("categoryStats", jdbcTemplate.queryForList("select category,count(*) as count from activity group by category"));
        data.put("recentRegistrations", registrationMapper.recent());
        data.put("hoursRank", jdbcTemplate.queryForList("""
                select u.id as userId,coalesce(u.nickname,u.name) as name,p.major_class as majorClass,p.total_hours as totalHours
                from user u join volunteer_profile p on u.id=p.user_id
                where u.role='VOLUNTEER'
                order by p.total_hours desc,p.credit_score desc
                limit 8
                """));
        data.put("pointsRank", jdbcTemplate.queryForList("""
                select u.id as userId,coalesce(u.nickname,u.name) as name,p.major_class as majorClass,p.credit_score as creditScore
                from user u join volunteer_profile p on u.id=p.user_id
                where u.role='VOLUNTEER'
                order by p.credit_score desc,p.total_hours desc
                limit 8
                """));
        return data;
    }
}
