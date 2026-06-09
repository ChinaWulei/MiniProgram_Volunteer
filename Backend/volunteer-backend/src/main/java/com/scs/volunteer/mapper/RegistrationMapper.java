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
                left join activity_position p on p.id=r.position_id
                where r.user_id=?
                  and r.activity_id<>?
                  and r.status in ('待审核','已通过')
                  and (
                    (coalesce(p.start_time,a.start_time) < ? and coalesce(p.end_time,a.end_time) > ?)
                    or
                    (p.requires_rehearsal=1 and p.rehearsal_start_time < ? and p.rehearsal_end_time > ?)
                  )
                """, Integer.class, userId, activityId, endTime, startTime, endTime, startTime);
        return count != null && count > 0;
    }

    public String findStatus(Long activityId, Long userId) {
        List<String> list = jdbcTemplate.queryForList(
                "select status from registration where activity_id=? and user_id=?",
                String.class, activityId, userId);
        return list.stream().findFirst().orElse(null);
    }

    public void insert(Long activityId, Long userId, Long positionId, boolean transportRequired, String boardingPoint, String status) {
        jdbcTemplate.update("""
                insert into registration(activity_id,user_id,position_id,transport_required,boarding_point,status)
                values(?,?,?,?,?,?)
                """, activityId, userId, positionId, transportRequired, boardingPoint, status);
    }

    public List<Map<String, Object>> my(Long userId) {
        return jdbcTemplate.queryForList("""
                select r.*,a.name as activity_name,a.category,a.location,a.start_time,a.end_time,
                       a.service_hours,a.contact_name,a.contact_phone,p.name as position_name,
                       p.start_time as position_start_time,p.end_time as position_end_time,
                       coalesce(e.new_status,c.status, if(now() > a.end_time and r.status in ('已通过','已完成'), 'ABSENT', 'NOT_CHECKED_IN')) as checkin_status,
                       coalesce(e.new_checkin_time,c.checkin_time) as checkin_time,
                       ca.audit_status as adjustment_status,
                       ca.admin_remark as adjustment_admin_remark,
                       ca.reason as adjustment_reason
                from registration r join activity a on r.activity_id=a.id
                left join activity_position p on p.id=r.position_id
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

    public boolean hasCompletedActivity(Long userId, String activityKeyword) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from registration r
                join activity a on a.id=r.activity_id
                where r.user_id=? and r.status='已完成'
                  and a.name like concat('%',?,'%')
                """, Integer.class, userId, activityKeyword);
        return count != null && count > 0;
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

    public List<Map<String, Object>> adminList(String keyword, String status, Long activityId, String department,
                                               String priorityDepartment) {
        String k = keyword == null || keyword.isBlank() ? null : keyword;
        return jdbcTemplate.queryForList("""
                select r.id,r.activity_id,r.user_id,r.status,r.review_remark,r.created_at,
                       u.name as userName,u.nickname,u.identity_no as identityNo,u.avatar_url as avatarUrl,
                       p.college,p.campus,p.department,p.major_class as majorClass,p.skill_tags as skillTags,p.available_time as availableTime,
                       p.credit_score as creditScore,p.total_hours as totalHours,p.service_count as serviceCount,
                       a.name as activityName,a.category,a.location,a.start_time as startTime,a.end_time as endTime,
                       a.skill_requirements as skillRequirements,ap.name as positionName,
                       r.transport_required as transportRequired,r.boarding_point as boardingPoint,
                       case when ? is not null and p.department=? then 1 else 0 end as priorityDepartmentMatch
                from registration r
                join user u on r.user_id=u.id
                left join volunteer_profile p on p.user_id=u.id
                join activity a on r.activity_id=a.id
                left join activity_position ap on ap.id=r.position_id
                where (? is null or r.status=?)
                  and (? is null or r.activity_id=?)
                  and (? is null or p.department=?)
                  and (? is null or u.name like concat('%',?,'%') or u.nickname like concat('%',?,'%')
                       or u.identity_no like concat('%',?,'%') or a.name like concat('%',?,'%')
                       or a.category like concat('%',?,'%') or a.location like concat('%',?,'%'))
            order by priorityDepartmentMatch desc,r.created_at desc
            """, n(priorityDepartment), n(priorityDepartment),
            n(status), n(status), activityId, activityId, n(department), n(department),
            k, k, k, k, k, k, k);
}

    public List<Map<String, Object>> byActivity(Long activityId) {
        return adminList(null, null, activityId, null, null);
    }

    public List<Map<String, Object>> approvedExportList(Long activityId) {
        return jdbcTemplate.queryForList("""
                select u.name as userName,u.identity_no as identityNo,u.phone,
                       p.college,p.campus,p.department,p.major_class as majorClass,
                       p.skill_tags as skillTags,p.credit_score as creditScore,
                       p.total_hours as totalHours,ap.name as positionName,
                       r.transport_required as transportRequired,
                       r.boarding_point as boardingPoint,r.status,r.created_at as signupTime
                from registration r
                join user u on u.id=r.user_id
                left join volunteer_profile p on p.user_id=u.id
                left join activity_position ap on ap.id=r.position_id
                where r.activity_id=? and r.status in ('已通过','已完成')
                order by coalesce(ap.sort_order,999),r.created_at
                """, activityId);
    }

    public List<String> departments() {
        return jdbcTemplate.queryForList("""
                select distinct p.department
                from registration r
                join volunteer_profile p on p.user_id=r.user_id
                where p.department is not null and trim(p.department)<>''
                order by p.department
                """, String.class);
    }

    public List<Long> participantUserIds(Long activityId, String scope) {
        String normalized = scope == null || scope.isBlank() ? "APPROVED" : scope.trim().toUpperCase();
        String statusSql = switch (normalized) {
            case "ALL" -> "";
            case "COMPLETED" -> " and status='已完成'";
            default -> " and status in ('已通过','已完成')";
        };
        return jdbcTemplate.queryForList("""
                select distinct user_id
                from registration
                where activity_id=?
                """ + statusSql, Long.class, activityId);
    }

    public Map<String, Object> findMap(Long id) {
        return jdbcTemplate.queryForMap("select * from registration where id=?", id);
    }

    public void review(Long id, String status, String remark) {
        jdbcTemplate.update("update registration set status=?,review_remark=? where id=?", status, remark, id);
    }

    public List<Map<String, Object>> pendingCandidates(Long activityId, Long positionId) {
        return jdbcTemplate.queryForList("""
                select r.id,r.activity_id,r.user_id,r.position_id,r.status,r.created_at,
                       u.name as userName,u.nickname,u.identity_no as identityNo,u.avatar_url as avatarUrl,
                       p.college,p.campus,p.department,p.major_class as majorClass,
                       p.skill_tags as skillTags,p.available_time as availableTime,
                       p.credit_score as creditScore,p.total_hours as totalHours,p.service_count as serviceCount,
                       a.name as activityName,a.category,a.location,a.start_time as startTime,a.end_time as endTime,
                       a.skill_requirements as skillRequirements,ap.name as positionName,
                       ap.start_time as positionStartTime,ap.end_time as positionEndTime,
                       ap.requires_rehearsal as requiresRehearsal,
                       ap.rehearsal_start_time as rehearsalStartTime,
                       ap.rehearsal_end_time as rehearsalEndTime
                from registration r
                join user u on u.id=r.user_id
                left join volunteer_profile p on p.user_id=u.id
                join activity a on a.id=r.activity_id
                left join activity_position ap on ap.id=r.position_id
                where r.activity_id=? and r.status='待审核'
                  and ((? is null and r.position_id is null) or r.position_id=?)
                order by r.created_at
                """, activityId, positionId, positionId);
    }

    public void lockPendingCandidates(Long activityId, Long positionId) {
        jdbcTemplate.queryForList("""
                select id
                from registration
                where activity_id=? and status='待审核'
                  and ((? is null and position_id is null) or position_id=?)
                for update
                """, Long.class, activityId, positionId, positionId);
    }

    public void markCompleted(Long activityId, Long userId, String remark) {
        jdbcTemplate.update("""
                update registration
                set status='已完成',review_remark=coalesce(?,review_remark)
                where activity_id=? and user_id=? and status in ('已通过','已完成')
                """, remark, activityId, userId);
    }

    public void markActivityCompleted(Long activityId, String remark) {
        jdbcTemplate.update("""
                update registration
                set status='已完成',review_remark=coalesce(?,review_remark)
                where activity_id=? and status in ('已通过','已完成')
                """, remark, activityId);
    }

    public void delete(Long id) {
        jdbcTemplate.update("delete from registration where id=?", id);
    }

    private String n(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
