package com.scs.volunteer.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
        data.put("activityTrend", jdbcTemplate.queryForList("""
                select date_format(created_at,'%m-%d') as label,count(*) as count
                from activity
                where created_at >= date_sub(curdate(), interval 13 day)
                group by date_format(created_at,'%m-%d')
                order by min(created_at)
                """));
        data.put("skillStats", skillStats());
        data.put("checkinStats", jdbcTemplate.queryForList("""
                select a.id,a.name,
                       count(distinct r.user_id) as approvedCount,
                       count(distinct c.user_id) as checkedCount,
                       if(count(distinct r.user_id)=0,0,round(count(distinct c.user_id)*100/count(distinct r.user_id),1)) as checkinRate
                from activity a
                left join registration r on r.activity_id=a.id
                left join activity_checkin c on c.activity_id=a.id and c.user_id=r.user_id
                group by a.id,a.name
                order by a.start_time desc
                limit 8
                """));
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

    private List<Map<String, Object>> skillStats() {
        Map<String, Integer> counter = new LinkedHashMap<>();
        List<String> rows = jdbcTemplate.queryForList("select skill_tags from volunteer_profile where skill_tags is not null and skill_tags<>''", String.class);
        for (String row : rows) {
            for (String tag : row.split("[,;|\\s]+")) {
                if (!tag.isBlank()) counter.put(tag, counter.getOrDefault(tag, 0) + 1);
            }
        }
        return counter.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(8)
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("skill", e.getKey());
                    item.put("count", e.getValue());
                    return item;
                })
                .toList();
    }
}
