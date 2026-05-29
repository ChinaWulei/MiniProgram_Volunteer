package com.scs.volunteer.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ReportStatsMapper {
    private final JdbcTemplate jdbcTemplate;

    public ReportStatsMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> volunteerStats(Long userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("overview", jdbcTemplate.queryForMap("""
                select
                  coalesce(max(p.total_hours),0) as totalServiceHours,
                  count(distinct r.activity_id) as participatedActivityCount,
                  sum(case when a.end_time < now() and r.status in ('已通过','已完成') then 1 else 0 end) as completedActivityCount,
                  sum(case when coalesce(e.new_status,c.status) in ('CHECKED_IN','LATE_CHECKED_IN','MANUAL_CHECKED_IN') then 1 else 0 end) as checkedCount,
                  sum(case when coalesce(e.new_status,c.status)='LATE_CHECKED_IN' then 1 else 0 end) as lateCount,
                  sum(case when coalesce(e.new_status,c.status)='ABSENT' or (c.id is null and e.id is null and a.end_time < now()) then 1 else 0 end) as absentCount,
                  if(count(distinct r.id)=0,0,round(sum(case when coalesce(e.new_status,c.status) in ('CHECKED_IN','LATE_CHECKED_IN','MANUAL_CHECKED_IN') then 1 else 0 end)*100/count(distinct r.id),1)) as attendanceRate
                from user u
                left join volunteer_profile p on p.user_id=u.id
                left join registration r on r.user_id=u.id and r.status in ('已通过','已完成')
                left join activity a on a.id=r.activity_id
                left join activity_checkin c on c.activity_id=r.activity_id and c.user_id=r.user_id
                left join checkin_adjustment e on e.activity_id=r.activity_id and e.user_id=r.user_id and e.audit_status='APPROVED'
                where u.id=?
                group by u.id
                """, userId));
        data.put("adjustments", jdbcTemplate.queryForMap("""
                select count(*) as adjustmentCount,
                       sum(case when audit_status='APPROVED' then 1 else 0 end) as adjustmentApprovedCount
                from checkin_adjustment
                where user_id=?
                """, userId));
        data.put("monthlyTrend", jdbcTemplate.queryForList("""
                select date_format(a.start_time,'%Y-%m') as label,
                       coalesce(sum(a.service_hours),0) as serviceHours,
                       count(distinct a.id) as activityCount
                from registration r
                join activity a on a.id=r.activity_id
                where r.user_id=? and r.status in ('已通过','已完成')
                  and a.start_time >= date_sub(curdate(), interval 11 month)
                group by date_format(a.start_time,'%Y-%m')
                order by label
                """, userId));
        data.put("categoryStats", jdbcTemplate.queryForList("""
                select coalesce(a.category,'其他') as category,count(distinct a.id) as count
                from registration r
                join activity a on a.id=r.activity_id
                where r.user_id=? and r.status in ('已通过','已完成')
                group by coalesce(a.category,'其他')
                order by count desc
                """, userId));
        data.put("recentActivities", jdbcTemplate.queryForList("""
                select a.name,a.category,a.start_time as startTime,a.service_hours as serviceHours,
                       coalesce(e.new_status,c.status,'NOT_CHECKED_IN') as checkinStatus
                from registration r
                join activity a on a.id=r.activity_id
                left join activity_checkin c on c.activity_id=r.activity_id and c.user_id=r.user_id
                left join checkin_adjustment e on e.activity_id=r.activity_id and e.user_id=r.user_id and e.audit_status='APPROVED'
                where r.user_id=? and r.status in ('已通过','已完成')
                order by a.start_time desc
                limit 8
                """, userId));
        return data;
    }

    public Map<String, Object> adminStats() {
        Map<String, Object> data = new HashMap<>();
        data.put("overview", jdbcTemplate.queryForMap("""
                select
                  (select count(*) from activity) as activityCount,
                  (select count(*) from user where role='VOLUNTEER') as volunteerCount,
                  (select count(*) from registration) as registrationCount,
                  (select count(distinct user_id) from registration where status in ('已通过','已完成')) as actualParticipantCount,
                  (select coalesce(sum(total_hours),0) from volunteer_profile) as totalServiceHours,
                  (select count(*) from checkin_adjustment) as adjustmentCount,
                  (select if(count(*)=0,0,round(sum(case when audit_status='APPROVED' then 1 else 0 end)*100/count(*),1)) from checkin_adjustment) as adjustmentPassRate,
                  if(count(r.id)=0,0,round(sum(case when coalesce(e.new_status,c.status) in ('CHECKED_IN','LATE_CHECKED_IN','MANUAL_CHECKED_IN') then 1 else 0 end)*100/count(r.id),1)) as averageAttendanceRate,
                  if(count(r.id)=0,0,round(sum(case when coalesce(e.new_status,c.status)='ABSENT' or (c.id is null and e.id is null and a.end_time < now()) then 1 else 0 end)*100/count(r.id),1)) as absenceRate
                from registration r
                join activity a on a.id=r.activity_id
                left join activity_checkin c on c.activity_id=r.activity_id and c.user_id=r.user_id
                left join checkin_adjustment e on e.activity_id=r.activity_id and e.user_id=r.user_id and e.audit_status='APPROVED'
                where r.status in ('已通过','已完成')
                """));
        data.put("activityTypeDistribution", jdbcTemplate.queryForList("""
                select coalesce(category,'其他') as category,count(*) as count
                from activity
                group by coalesce(category,'其他')
                order by count desc
                """));
        data.put("hotActivities", jdbcTemplate.queryForList("""
                select a.id,a.name,count(r.id) as registrationCount
                from activity a left join registration r on r.activity_id=a.id
                group by a.id,a.name
                order by registrationCount desc,a.created_at desc
                limit 8
                """));
        data.put("activityParticipationRank", jdbcTemplate.queryForList("""
                select a.id,a.name,a.recruit_number as recruitNumber,count(r.id) as approvedCount,
                       if(a.recruit_number=0,0,round(count(r.id)*100/a.recruit_number,1)) as participationRate
                from activity a left join registration r on r.activity_id=a.id and r.status in ('已通过','已完成')
                group by a.id,a.name,a.recruit_number
                order by participationRate desc,approvedCount desc
                limit 8
                """));
        data.put("serviceHoursRank", jdbcTemplate.queryForList("""
                select u.id as userId,coalesce(u.nickname,u.name) as name,p.total_hours as totalHours
                from user u join volunteer_profile p on p.user_id=u.id
                where u.role='VOLUNTEER'
                order by p.total_hours desc
                limit 8
                """));
        data.put("activeVolunteerRank", jdbcTemplate.queryForList("""
                select u.id as userId,coalesce(u.nickname,u.name) as name,count(r.id) as activityCount
                from user u left join registration r on r.user_id=u.id and r.status in ('已通过','已完成')
                where u.role='VOLUNTEER'
                group by u.id,name
                order by activityCount desc
                limit 8
                """));
        data.put("attendanceRank", jdbcTemplate.queryForList("""
                select u.id as userId,coalesce(u.nickname,u.name) as name,
                       if(count(r.id)=0,0,round(sum(case when coalesce(e.new_status,c.status) in ('CHECKED_IN','LATE_CHECKED_IN','MANUAL_CHECKED_IN') then 1 else 0 end)*100/count(r.id),1)) as attendanceRate
                from user u
                left join registration r on r.user_id=u.id and r.status in ('已通过','已完成')
                left join activity_checkin c on c.activity_id=r.activity_id and c.user_id=r.user_id
                left join checkin_adjustment e on e.activity_id=r.activity_id and e.user_id=r.user_id and e.audit_status='APPROVED'
                where u.role='VOLUNTEER'
                group by u.id,name
                order by attendanceRate desc,count(r.id) desc
                limit 8
                """));
        data.put("abnormalCheckinRank", jdbcTemplate.queryForList("""
                select u.id as userId,coalesce(u.nickname,u.name) as name,
                       sum(case when coalesce(e.new_status,c.status) in ('LATE_CHECKED_IN','ABSENT') or (c.id is null and e.id is null and a.end_time < now()) then 1 else 0 end) as abnormalCount
                from user u
                left join registration r on r.user_id=u.id and r.status in ('已通过','已完成')
                left join activity a on a.id=r.activity_id
                left join activity_checkin c on c.activity_id=r.activity_id and c.user_id=r.user_id
                left join checkin_adjustment e on e.activity_id=r.activity_id and e.user_id=r.user_id and e.audit_status='APPROVED'
                where u.role='VOLUNTEER'
                group by u.id,name
                order by abnormalCount desc
                limit 8
                """));
        return data;
    }
}
