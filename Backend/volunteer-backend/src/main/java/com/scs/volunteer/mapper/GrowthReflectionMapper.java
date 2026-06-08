package com.scs.volunteer.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class GrowthReflectionMapper {
    private final JdbcTemplate jdbcTemplate;

    public GrowthReflectionMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean exists(Long activityId, Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from volunteer_growth_reflection where activity_id=? and user_id=?",
                Integer.class, activityId, userId);
        return count != null && count > 0;
    }

    public Long insert(Long activityId, Long userId, String content, boolean anonymous) {
        jdbcTemplate.update("""
                insert into volunteer_growth_reflection(activity_id,user_id,content,anonymous)
                values(?,?,?,?)
                """, activityId, userId, content, anonymous);
        return jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
    }

    public void saveAnalysis(Long id, Map<String, String> parsed) {
        jdbcTemplate.update("""
                update volunteer_growth_reflection
                set parsed_gain=?,parsed_ability=?,parsed_experience=?,parsed_advice=?,analysis_status='DONE'
                where id=?
                """, parsed.get("gain"), parsed.get("ability"), parsed.get("experience"), parsed.get("advice"), id);
    }

    public void saveFromEvaluation(Long activityId, Long userId, String content, boolean anonymous, Map<String, String> parsed) {
        jdbcTemplate.update("""
                insert into volunteer_growth_reflection(activity_id,user_id,content,anonymous,
                    parsed_gain,parsed_ability,parsed_experience,parsed_advice,analysis_status)
                values(?,?,?,?,?,?,?,?, 'DONE')
                on duplicate key update
                    content=values(content),
                    anonymous=values(anonymous),
                    parsed_gain=values(parsed_gain),
                    parsed_ability=values(parsed_ability),
                    parsed_experience=values(parsed_experience),
                    parsed_advice=values(parsed_advice),
                    analysis_status='DONE'
                """, activityId, userId, content, anonymous,
                parsed.get("gain"), parsed.get("ability"), parsed.get("experience"), parsed.get("advice"));
    }

    public List<Map<String, Object>> my(Long userId) {
        return jdbcTemplate.queryForList("""
                select g.id,g.activity_id as activityId,g.content,g.anonymous,
                       g.parsed_gain as parsedGain,g.parsed_ability as parsedAbility,
                       g.parsed_experience as parsedExperience,g.parsed_advice as parsedAdvice,
                       g.created_at as createdAt,a.name as activityName,a.category
                from volunteer_growth_reflection g join activity a on a.id=g.activity_id
                where g.user_id=? order by g.created_at desc
                """, userId);
    }

    public List<Map<String, Object>> recommended(Long activityId, String category, String matchToken, int limit) {
        return jdbcTemplate.queryForList("""
                select g.id,g.content,g.parsed_gain as parsedGain,g.parsed_ability as parsedAbility,
                       g.parsed_experience as parsedExperience,g.parsed_advice as parsedAdvice,
                       a.name as activityName,a.category,a.skill_requirements as skillRequirements
                from volunteer_growth_reflection g join activity a on a.id=g.activity_id
                where (? is null or g.activity_id<>?)
                  and ((? is not null and a.category=?)
                       or (? is not null and (a.name like concat('%',?,'%')
                            or a.category like concat('%',?,'%')
                            or a.skill_requirements like concat('%',?,'%'))))
                  and coalesce(g.content,'')<>''
                order by g.created_at desc
                limit ?
                """, activityId, activityId, n(category), n(category),
                n(matchToken), n(matchToken), n(matchToken), n(matchToken), limit);
    }

    public List<Map<String, Object>> recent(Long activityId, int limit) {
        return jdbcTemplate.queryForList("""
                select g.id,g.content,g.parsed_gain as parsedGain,g.parsed_ability as parsedAbility,
                       g.parsed_experience as parsedExperience,g.parsed_advice as parsedAdvice,
                       a.name as activityName,a.category,a.skill_requirements as skillRequirements
                from volunteer_growth_reflection g join activity a on a.id=g.activity_id
                where (? is null or g.activity_id<>?)
                  and coalesce(g.content,'')<>''
                order by g.created_at desc
                limit ?
                """, activityId, activityId, limit);
    }

    public Map<String, Object> profileStats(Long userId) {
        return jdbcTemplate.queryForMap("""
                select count(distinct case when r.status='已完成' then r.activity_id end) as activityCount,
                       coalesce(p.total_hours,0) as totalHours,coalesce(p.service_count,0) as serviceCount,
                       coalesce(p.credit_score,0) as volunteerPoints,
                       case when p.total_hours>=100 then 'Lv5'
                            when p.total_hours>=60 then 'Lv4'
                            when p.total_hours>=30 then 'Lv3'
                            when p.total_hours>=10 then 'Lv2'
                            else 'Lv1' end as volunteerLevel,
                       case when p.total_hours>=100 then '卓越志愿者'
                            when p.total_hours>=60 then '先锋志愿者'
                            when p.total_hours>=30 then '骨干志愿者'
                            when p.total_hours>=10 then '成长志愿者'
                            else '新星志愿者' end as levelName
                from volunteer_profile p
                left join registration r on r.user_id=p.user_id
                where p.user_id=?
                group by p.id
                """, userId);
    }

    public List<Map<String, Object>> categoryStats(Long userId) {
        return jdbcTemplate.queryForList("""
                select a.category,count(*) as count
                from registration r join activity a on a.id=r.activity_id
                where r.user_id=? and r.status='已完成'
                group by a.category order by count desc
                """, userId);
    }

    private String n(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
