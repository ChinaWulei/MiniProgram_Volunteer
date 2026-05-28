package com.scs.volunteer.mapper;

import com.scs.volunteer.vo.CheckinStatusVO;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class CheckinMapper {
    private final JdbcTemplate jdbcTemplate;

    public CheckinMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean approved(Long activityId, Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from registration where activity_id=? and user_id=? and status in ('已通过','已完成')",
                Integer.class, activityId, userId);
        return count != null && count > 0;
    }

    public Optional<CheckinStatusVO> find(Long activityId, Long userId) {
        List<CheckinStatusVO> list = jdbcTemplate.query("""
                select coalesce(e.new_status,c.status) as status,
                       coalesce(e.new_checkin_time,c.checkin_time) as checkin_time,
                       c.method,c.distance_meters,c.manual_reason
                from (select ? as activity_id, ? as user_id) x
                left join activity_checkin c on c.activity_id=x.activity_id and c.user_id=x.user_id
                left join (
                    select activity_id,user_id,new_status,new_checkin_time
                    from checkin_adjustment
                    where activity_id=? and user_id=? and audit_status='APPROVED'
                    order by updated_at desc,id desc
                    limit 1
                ) e on e.activity_id=x.activity_id and e.user_id=x.user_id
                where c.id is not null or e.new_status is not null
                """, new BeanPropertyRowMapper<>(CheckinStatusVO.class), activityId, userId, activityId, userId);
        return list.stream().findFirst();
    }

    public void insertLocation(Long activityId, Long userId, String status, LocalDateTime time, Double lat, Double lng, double distance) {
        jdbcTemplate.update("""
                insert into activity_checkin(activity_id,user_id,status,checkin_time,method,latitude,longitude,distance_meters)
                values(?,?,?,?,?,?,?,?)
                """, activityId, userId, status, time, "定位签到", lat, lng, distance);
    }

    public void insertManual(Long activityId, Long userId, Long adminId, String reason) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                insert into activity_checkin(activity_id,user_id,status,checkin_time,method,manual_admin_id,manual_time,manual_reason)
                values(?,?,?,?,?,?,?,?)
                """, activityId, userId, "MANUAL_CHECKED_IN", now, "管理员补签", adminId, now, reason);
    }

    public Map<String, Object> statistics(Long activityId) {
        return jdbcTemplate.queryForMap("""
                select
                  a.id as activityId,a.name as activityName,a.location,a.start_time as startTime,a.end_time as endTime,
                  count(r.id) as approvedCount,
                  sum(case when coalesce(e.new_status,c.status) in ('CHECKED_IN','LATE_CHECKED_IN') then 1 else 0 end) as checkedCount,
                  sum(case when coalesce(e.new_status,c.status)='MANUAL_CHECKED_IN' then 1 else 0 end) as manualCount,
                  sum(case when coalesce(e.new_status,c.status)='LATE_CHECKED_IN' then 1 else 0 end) as lateCount,
                  sum(case when coalesce(e.new_status,c.status) is null or coalesce(e.new_status,c.status)='ABSENT' then 1 else 0 end) as notCheckedCount
                from activity a
                left join registration r on r.activity_id=a.id and r.status in ('已通过','已完成')
                left join activity_checkin c on c.activity_id=a.id and c.user_id=r.user_id
                left join (
                    select ca.*
                    from checkin_adjustment ca
                    join (
                        select activity_id,user_id,max(id) as id
                        from checkin_adjustment
                        where audit_status='APPROVED'
                        group by activity_id,user_id
                    ) latest on latest.id=ca.id
                ) e on e.activity_id=a.id and e.user_id=r.user_id
                where a.id=?
                group by a.id
                """, activityId);
    }

    public List<Map<String, Object>> list(Long activityId, String status, String keyword) {
        return jdbcTemplate.queryForList("""
                select r.id as registrationId,u.id as userId,u.name,u.nickname,u.avatar_url as avatarUrl,u.identity_no as identityNo,
                       p.college,p.major_class as majorClass,
                       coalesce(e.new_status,c.status, if(now() > a.end_time, 'ABSENT', 'NOT_CHECKED_IN')) as status,
                       coalesce(e.new_checkin_time,c.checkin_time) as checkinTime,c.method,c.manual_reason as manualReason
                from registration r
                join user u on r.user_id=u.id
                left join volunteer_profile p on p.user_id=u.id
                join activity a on a.id=r.activity_id
                left join activity_checkin c on c.activity_id=r.activity_id and c.user_id=r.user_id
                left join (
                    select ca.*
                    from checkin_adjustment ca
                    join (
                        select activity_id,user_id,max(id) as id
                        from checkin_adjustment
                        where audit_status='APPROVED'
                        group by activity_id,user_id
                    ) latest on latest.id=ca.id
                ) e on e.activity_id=r.activity_id and e.user_id=r.user_id
                where r.activity_id=? and r.status in ('已通过','已完成')
                  and (? is null or coalesce(e.new_status,c.status, if(now() > a.end_time, 'ABSENT', 'NOT_CHECKED_IN'))=?)
                  and (? is null or u.name like concat('%',?,'%') or u.nickname like concat('%',?,'%') or u.identity_no like concat('%',?,'%'))
                order by coalesce(e.new_checkin_time,c.checkin_time) desc,r.created_at desc
                """, activityId, n(status), n(status), n(keyword), n(keyword), n(keyword), n(keyword));
    }

    public Map<String, Object> volunteerStatistics(Long userId) {
        return jdbcTemplate.queryForMap("""
                select
                  count(r.id) as approvedTotal,
                  count(r.id) as expectedTotal,
                  sum(case when coalesce(e.new_status,c.status)='CHECKED_IN' then 1 else 0 end) as normalCount,
                  sum(case when coalesce(e.new_status,c.status)='MANUAL_CHECKED_IN' then 1 else 0 end) as manualCount,
                  sum(case when coalesce(e.new_status,c.status)='LATE_CHECKED_IN' then 1 else 0 end) as lateCount,
                  sum(case when (coalesce(e.new_status,c.status)='ABSENT' or (e.new_status is null and c.id is null and now() > a.end_time)) then 1 else 0 end) as absentCount
                from registration r
                join activity a on r.activity_id=a.id
                left join activity_checkin c on c.activity_id=r.activity_id and c.user_id=r.user_id
                left join (
                    select ca.*
                    from checkin_adjustment ca
                    join (
                        select activity_id,user_id,max(id) as id
                        from checkin_adjustment
                        where audit_status='APPROVED'
                        group by activity_id,user_id
                    ) latest on latest.id=ca.id
                ) e on e.activity_id=r.activity_id and e.user_id=r.user_id
                where r.user_id=? and r.status in ('已通过','已完成')
                """, userId);
    }

    public List<Map<String, Object>> recentByVolunteer(Long userId) {
        return jdbcTemplate.queryForList("""
                select a.id as activityId,a.name as activityName,
                       coalesce(e.new_status,c.status, if(now() > a.end_time, 'ABSENT', 'NOT_CHECKED_IN')) as status,
                       coalesce(e.new_checkin_time,c.checkin_time) as checkinTime,c.method
                from registration r
                join activity a on r.activity_id=a.id
                left join activity_checkin c on c.activity_id=r.activity_id and c.user_id=r.user_id
                left join (
                    select ca.*
                    from checkin_adjustment ca
                    join (
                        select activity_id,user_id,max(id) as id
                        from checkin_adjustment
                        where audit_status='APPROVED'
                        group by activity_id,user_id
                    ) latest on latest.id=ca.id
                ) e on e.activity_id=r.activity_id and e.user_id=r.user_id
                where r.user_id=? and r.status in ('已通过','已完成')
                order by a.start_time desc limit 5
                """, userId);
    }

    private String n(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
