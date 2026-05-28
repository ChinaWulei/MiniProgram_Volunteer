package com.scs.volunteer.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class RegistrationMapper {
    private final JdbcTemplate jdbcTemplate;

    public RegistrationMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean exists(Long activityId, Long userId) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from registration where activity_id=? and user_id=?", Integer.class, activityId, userId);
        return count != null && count > 0;
    }

    public boolean hasTimeConflict(Long userId, Long activityId, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from registration r join activity a on r.activity_id=a.id
                where r.user_id=?
                  and r.activity_id<>?
                  and r.status in ('待审核','已通过')
                  and a.start_time < ?
                  and a.end_time > ?
                """, Integer.class, userId, activityId, endTime, startTime);
        return count != null && count > 0;
    }

    public String findStatus(Long activityId, Long userId) {
        List<String> list = jdbcTemplate.queryForList(
                "select status from registration where activity_id=? and user_id=?",
                String.class, activityId, userId);
        return list.stream().findFirst().orElse(null);
    }

    public void insert(Long activityId, Long userId, String status) {
        jdbcTemplate.update("insert into registration(activity_id,user_id,status) values(?,?,?)", activityId, userId, status);
    }

    public List<Map<String, Object>> my(Long userId) {
        return jdbcTemplate.queryForList("""
                select r.*,a.name as activity_name,a.category,a.location,a.start_time,a.end_time,
                       a.service_hours,a.contact_name,a.contact_phone,
                       coalesce(e.new_status,c.status, if(now() > a.end_time and r.status in ('已通过','已完成'), 'ABSENT', 'NOT_CHECKED_IN')) as checkin_status,
                       coalesce(e.new_checkin_time,c.checkin_time) as checkin_time,
                       ca.audit_status as adjustment_status,
                       ca.admin_remark as adjustment_admin_remark,
                       ca.reason as adjustment_reason
                from registration r join activity a on r.activity_id=a.id
                left join activity_checkin c on c.activity_id=r.activity_id and c.user_id=r.user_id
                left join (
                    select x.*
                    from checkin_adjustment x
                    join (
                        select activity_id,user_id,max(id) as id
                        from checkin_adjustment
                        group by activity_id,user_id
                    ) latest on latest.id=x.id
                ) ca on ca.activity_id=r.activity_id and ca.user_id=r.user_id
                left join (
                    select x.*
                    from checkin_adjustment x
                    join (
                        select activity_id,user_id,max(id) as id
                        from checkin_adjustment
                        where audit_status='APPROVED'
                        group by activity_id,user_id
                    ) latest on latest.id=x.id
                ) e on e.activity_id=r.activity_id and e.user_id=r.user_id
                where r.user_id=? order by r.created_at desc
                """, userId);
    }

    public List<Map<String, Object>> aiHistory(Long userId) {
        return jdbcTemplate.queryForList("""
                select a.name as activity_name,a.category,r.status,
                       case when r.status='已完成' then 1 else 0 end as completed,
                       a.service_hours
                from registration r join activity a on r.activity_id=a.id
                where r.user_id=?
                order by r.created_at desc
                limit 20
                """, userId);
    }

    public Map<String, Object> monthlyStats(Long userId, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        return jdbcTemplate.queryForMap("""
                select
                  count(r.id) as activityCount,
                  coalesce(sum(case when r.status='已完成' then coalesce(a.service_hours,0) else 0 end),0) as completedHours,
                  sum(case when r.status='已完成' then 1 else 0 end) as completedCount,
                  sum(case when r.status in ('已通过','已完成') then 1 else 0 end) as approvedCount,
                  sum(case when c.status in ('CHECKED_IN','LATE_CHECKED_IN','MANUAL_CHECKED_IN') then 1 else 0 end) as checkedCount,
                  sum(case when c.status='MANUAL_CHECKED_IN' then 1 else 0 end) as manualCount,
                  sum(case when c.status='LATE_CHECKED_IN' then 1 else 0 end) as lateCount,
                  sum(case when c.id is null and now() > a.end_time and r.status in ('已通过','已完成') then 1 else 0 end) as absentCount
                from registration r
                join activity a on a.id=r.activity_id
                left join activity_checkin c on c.activity_id=r.activity_id and c.user_id=r.user_id
                where r.user_id=? and a.start_time>=? and a.start_time<?
                """, userId, start, end);
    }

    public List<Map<String, Object>> monthlyCategoryStats(Long userId, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        return jdbcTemplate.queryForList("""
                select a.category,count(*) as count
                from registration r join activity a on a.id=r.activity_id
                where r.user_id=? and a.start_time>=? and a.start_time<?
                group by a.category
                order by count desc
                """, userId, start, end);
    }

    public List<Map<String, Object>> recent() {
        return jdbcTemplate.queryForList("""
                select r.id,u.name as user_name,a.name as activity_name,r.status,r.created_at
                from registration r join user u on r.user_id=u.id join activity a on r.activity_id=a.id
                order by r.created_at desc limit 8
                """);
    }

    public List<Map<String, Object>> adminList(String keyword, String status, Long activityId) {
        String k = keyword == null || keyword.isBlank() ? null : keyword;
        return jdbcTemplate.queryForList("""
                select r.id,r.activity_id,r.user_id,r.status,r.review_remark,r.created_at,
                       u.name as userName,u.nickname,u.identity_no as identityNo,u.avatar_url as avatarUrl,
                       p.college,p.major_class as majorClass,p.skill_tags as skillTags,p.available_time as availableTime,
                       p.credit_score as creditScore,p.total_hours as totalHours,p.service_count as serviceCount,
                       a.name as activityName,a.category,a.location,a.start_time as startTime,a.end_time as endTime,
                       a.skill_requirements as skillRequirements
                from registration r
                join user u on r.user_id=u.id
                left join volunteer_profile p on p.user_id=u.id
                join activity a on r.activity_id=a.id
                where (? is null or r.status=?)
                  and (? is null or r.activity_id=?)
                  and (? is null or u.name like concat('%',?,'%') or u.nickname like concat('%',?,'%')
                       or u.identity_no like concat('%',?,'%') or a.name like concat('%',?,'%')
                       or a.category like concat('%',?,'%') or a.location like concat('%',?,'%'))
            order by r.created_at desc
            """, n(status), n(status), activityId, activityId,
            k, k, k, k, k, k, k);
}

    public List<Map<String, Object>> byActivity(Long activityId) {
        return adminList(null, null, activityId);
    }

    public Map<String, Object> findMap(Long id) {
        return jdbcTemplate.queryForMap("select * from registration where id=?", id);
    }

    public void review(Long id, String status, String remark) {
        jdbcTemplate.update("update registration set status=?,review_remark=? where id=?", status, remark, id);
    }

    public void markCompleted(Long activityId, Long userId, String remark) {
        jdbcTemplate.update("""
                update registration
                set status='已完成',review_remark=coalesce(?,review_remark)
                where activity_id=? and user_id=? and status in ('已通过','已完成')
                """, remark, activityId, userId);
    }

    public void delete(Long id) {
        jdbcTemplate.update("delete from registration where id=?", id);
    }

    private String n(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
