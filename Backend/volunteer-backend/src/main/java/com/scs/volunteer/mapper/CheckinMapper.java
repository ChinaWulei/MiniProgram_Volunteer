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
        Integer count = jdbcTemplate.queryForObject("select count(*) from registration where activity_id=? and user_id=? and status in ('已通过','已完成')",
                Integer.class, activityId, userId);
        return count != null && count > 0;
    }

    public Optional<CheckinStatusVO> find(Long activityId, Long userId) {
        List<CheckinStatusVO> list = jdbcTemplate.query("""
                select status,checkin_time,method,distance_meters,manual_reason
                from activity_checkin where activity_id=? and user_id=?
                """, new BeanPropertyRowMapper<>(CheckinStatusVO.class), activityId, userId);
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
                  sum(case when c.status in ('CHECKED_IN','LATE_CHECKED_IN') then 1 else 0 end) as checkedCount,
                  sum(case when c.status='MANUAL_CHECKED_IN' then 1 else 0 end) as manualCount,
                  sum(case when c.status='LATE_CHECKED_IN' then 1 else 0 end) as lateCount,
                  sum(case when c.id is null then 1 else 0 end) as notCheckedCount
                from activity a
                left join registration r on r.activity_id=a.id and r.status in ('已通过','已完成')
                left join activity_checkin c on c.activity_id=a.id and c.user_id=r.user_id
                where a.id=?
                group by a.id
                """, activityId);
    }

    public List<Map<String, Object>> list(Long activityId, String status, String keyword) {
        return jdbcTemplate.queryForList("""
                select r.id as registrationId,u.id as userId,u.name,u.nickname,u.avatar_url as avatarUrl,u.identity_no as identityNo,
                       p.college,p.major_class as majorClass,
                       coalesce(c.status, if(now() > a.end_time, 'ABSENT', 'NOT_CHECKED_IN')) as status,
                       c.checkin_time as checkinTime,c.method,c.manual_reason as manualReason
                from registration r
                join user u on r.user_id=u.id
                left join volunteer_profile p on p.user_id=u.id
                join activity a on a.id=r.activity_id
                left join activity_checkin c on c.activity_id=r.activity_id and c.user_id=r.user_id
                where r.activity_id=? and r.status in ('已通过','已完成')
                  and (? is null or coalesce(c.status, if(now() > a.end_time, 'ABSENT', 'NOT_CHECKED_IN'))=?)
                  and (? is null or u.name like concat('%',?,'%') or u.nickname like concat('%',?,'%') or u.identity_no like concat('%',?,'%'))
                order by c.checkin_time desc,r.created_at desc
                """, activityId, n(status), n(status), n(keyword), n(keyword), n(keyword), n(keyword));
    }

    public Map<String, Object> volunteerStatistics(Long userId) {
        return jdbcTemplate.queryForMap("""
                select
                  count(r.id) as approvedTotal,
                  count(r.id) as expectedTotal,
                  sum(case when c.status='CHECKED_IN' then 1 else 0 end) as normalCount,
                  sum(case when c.status='MANUAL_CHECKED_IN' then 1 else 0 end) as manualCount,
                  sum(case when c.status='LATE_CHECKED_IN' then 1 else 0 end) as lateCount,
                  sum(case when c.id is null and now() > a.end_time then 1 else 0 end) as absentCount
                from registration r
                join activity a on r.activity_id=a.id
                left join activity_checkin c on c.activity_id=r.activity_id and c.user_id=r.user_id
                where r.user_id=? and r.status in ('已通过','已完成')
                """, userId);
    }

    public List<Map<String, Object>> recentByVolunteer(Long userId) {
        return jdbcTemplate.queryForList("""
                select a.id as activityId,a.name as activityName,
                       coalesce(c.status, if(now() > a.end_time, 'ABSENT', 'NOT_CHECKED_IN')) as status,
                       c.checkin_time as checkinTime,c.method
                from registration r
                join activity a on r.activity_id=a.id
                left join activity_checkin c on c.activity_id=r.activity_id and c.user_id=r.user_id
                where r.user_id=? and r.status in ('已通过','已完成')
                order by a.start_time desc limit 5
                """, userId);
    }

    private String n(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
