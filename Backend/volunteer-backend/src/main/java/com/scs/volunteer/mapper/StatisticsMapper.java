package com.scs.volunteer.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class StatisticsMapper {
    private final JdbcTemplate jdbcTemplate;

    public StatisticsMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> overview() {
        return overview(null, null);
    }

    public Map<String, Object> overview(String startDate, String endDate) {
        LocalDateTime start = parseStart(startDate);
        LocalDateTime end = parseEnd(endDate);
        Map<String, Object> data = new HashMap<>();
        data.put("activityTotal", jdbcTemplate.queryForObject("""
                select count(*)
                from activity
                where (? is null or start_time >= ?)
                  and (? is null or start_time < ?)
                """, Integer.class, start, start, end, end));
        data.put("volunteerTotal", jdbcTemplate.queryForObject(volunteerTotalSql(start, end), Integer.class, rangeArgs(start, end)));
        data.put("registrationTotal", jdbcTemplate.queryForObject("""
                select count(*)
                from registration
                where (? is null or created_at >= ?)
                  and (? is null or created_at < ?)
                """, Integer.class, start, start, end, end));
        data.put("pendingRegistrationTotal", jdbcTemplate.queryForObject("""
                select count(*)
                from registration
                where status='待审核'
                  and (? is null or created_at >= ?)
                  and (? is null or created_at < ?)
                """, Integer.class, start, start, end, end));
        data.put("totalHours", totalHours(start, end));
        data.put("categoryStats", categoryStats(start, end));
        data.put("activityTrend", activityTrend(start, end));
        data.put("skillStats", skillStats());
        data.put("checkinStats", jdbcTemplate.queryForList("""
                select a.id,a.name,
                       count(distinct r.user_id) as approvedCount,
                       count(distinct c.user_id) as checkedCount,
                       if(count(distinct r.user_id)=0,0,round(count(distinct c.user_id)*100/count(distinct r.user_id),1)) as checkinRate
                from activity a
                left join registration r on r.activity_id=a.id
                left join activity_checkin c on c.activity_id=a.id and c.user_id=r.user_id
                where (? is null or a.start_time >= ?)
                  and (? is null or a.start_time < ?)
                group by a.id,a.name
                order by a.start_time desc
                limit 8
                """, start, start, end, end));
        data.put("recentRegistrations", recentRegistrations(start, end));
        data.put("hoursRank", hoursRank(start, end));
        data.put("pointsRank", jdbcTemplate.queryForList("""
                select u.id as userId,coalesce(u.nickname,u.name) as name,p.major_class as majorClass,p.credit_score as creditScore
                from user u join volunteer_profile p on u.id=p.user_id
                where u.role='VOLUNTEER'
                order by p.credit_score desc,p.total_hours desc
                limit 8
                """));
        return data;
    }

    private List<Map<String, Object>> hoursRank(LocalDateTime start, LocalDateTime end) {
        if (start == null && end == null) {
            return jdbcTemplate.queryForList("""
                select u.id as userId,coalesce(u.nickname,u.name) as name,p.major_class as majorClass,p.total_hours as totalHours
                from user u join volunteer_profile p on u.id=p.user_id
                where u.role='VOLUNTEER'
                order by p.total_hours desc,p.credit_score desc
                limit 8
                """));
        }
        return jdbcTemplate.queryForList("""
                select u.id as userId,coalesce(u.nickname,u.name) as name,p.major_class as majorClass,
                       coalesce(sum(sr.hours),0) as totalHours
                from user u
                join volunteer_profile p on u.id=p.user_id
                left join service_record sr on sr.user_id=u.id
                    and (? is null or sr.created_at >= ?)
                    and (? is null or sr.created_at < ?)
                where u.role='VOLUNTEER'
                group by u.id,coalesce(u.nickname,u.name),p.major_class
                order by totalHours desc
                limit 8
                """, start, start, end, end);
    }

    private List<Map<String, Object>> recentRegistrations(LocalDateTime start, LocalDateTime end) {
        return jdbcTemplate.queryForList("""
                select r.id,u.name as user_name,a.name as activity_name,r.status,r.created_at
                from registration r join user u on r.user_id=u.id join activity a on r.activity_id=a.id
                where (? is null or r.created_at >= ?)
                  and (? is null or r.created_at < ?)
                order by r.created_at desc limit 8
                """, start, start, end, end);
    }

    private Double totalHours(LocalDateTime start, LocalDateTime end) {
        if (start == null && end == null) {
            return jdbcTemplate.queryForObject("select coalesce(sum(total_hours),0) from volunteer_profile", Double.class);
        }
        return jdbcTemplate.queryForObject("""
                select coalesce(sum(hours),0)
                from service_record
                where (? is null or created_at >= ?)
                  and (? is null or created_at < ?)
                """, Double.class, start, start, end, end);
    }

    private List<Map<String, Object>> categoryStats(LocalDateTime start, LocalDateTime end) {
        return jdbcTemplate.queryForList("""
                select category,count(*) as count
                from activity
                where (? is null or start_time >= ?)
                  and (? is null or start_time < ?)
                group by category
                order by count desc
                """, start, start, end, end);
    }

    private List<Map<String, Object>> activityTrend(LocalDateTime start, LocalDateTime end) {
        if (start == null && end == null) {
            return jdbcTemplate.queryForList("""
                    select date_format(created_at,'%m-%d') as label,count(*) as count
                    from activity
                    where created_at >= date_sub(curdate(), interval 13 day)
                    group by date_format(created_at,'%m-%d')
                    order by min(created_at)
                    """);
        }
        return jdbcTemplate.queryForList("""
                select date_format(start_time,'%m-%d') as label,count(*) as count
                from activity
                where (? is null or start_time >= ?)
                  and (? is null or start_time < ?)
                group by date_format(start_time,'%m-%d')
                order by min(start_time)
                """, start, start, end, end);
    }

    private String volunteerTotalSql(LocalDateTime start, LocalDateTime end) {
        if (start == null && end == null) {
            return "select count(*) from user where role='VOLUNTEER'";
        }
        return """
                select count(distinct r.user_id)
                from registration r
                join user u on u.id=r.user_id
                join activity a on a.id=r.activity_id
                where u.role='VOLUNTEER'
                  and r.status in ('已通过','已完成')
                  and (? is null or a.start_time >= ?)
                  and (? is null or a.start_time < ?)
                """;
    }

    private Object[] rangeArgs(LocalDateTime start, LocalDateTime end) {
        if (start == null && end == null) return new Object[]{};
        return new Object[]{start, start, end, end};
    }

    private LocalDateTime parseStart(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value).atStartOfDay();
    }

    private LocalDateTime parseEnd(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value).plusDays(1).atStartOfDay();
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
