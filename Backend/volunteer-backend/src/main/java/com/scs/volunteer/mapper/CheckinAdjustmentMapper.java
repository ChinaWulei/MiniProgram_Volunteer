package com.scs.volunteer.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class CheckinAdjustmentMapper {
    private final JdbcTemplate jdbcTemplate;

    public CheckinAdjustmentMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean hasOpenApplication(Long activityId, Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from checkin_adjustment
                where activity_id=? and user_id=? and audit_status='PENDING'
                """, Integer.class, activityId, userId);
        return count != null && count > 0;
    }

    public Long insertApplication(Long activityId, Long userId, String originalStatus, LocalDateTime originalCheckinTime,
                                  String reason, String description, String proofImageUrl, Double originalServiceHours) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    insert into checkin_adjustment(activity_id,user_id,original_status,original_checkin_time,reason,description,
                        proof_image_url,original_service_hours,audit_status)
                    values(?,?,?,?,?,?,?,?, 'PENDING')
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, activityId);
            ps.setLong(2, userId);
            ps.setString(3, originalStatus);
            ps.setObject(4, originalCheckinTime);
            ps.setString(5, reason);
            ps.setString(6, description);
            ps.setString(7, proofImageUrl);
            ps.setObject(8, originalServiceHours);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void createMissingAnomalyRows() {
        jdbcTemplate.update("""
                insert into checkin_adjustment(activity_id,user_id,original_status,original_checkin_time,reason,
                    original_service_hours,audit_status)
                select r.activity_id,r.user_id,
                       coalesce(c.status, 'ABSENT') as original_status,
                       c.checkin_time,
                       case when c.status='LATE_CHECKED_IN' then '系统识别为迟到签到' else '系统识别为活动结束后未签到' end,
                       a.service_hours,
                       'SYSTEM'
                from registration r
                join activity a on a.id=r.activity_id
                left join activity_checkin c on c.activity_id=r.activity_id and c.user_id=r.user_id
                where r.status in ('已通过','已完成')
                  and (c.status='LATE_CHECKED_IN' or (c.id is null and now() > a.end_time))
                  and not exists (
                    select 1 from checkin_adjustment ca
                    where ca.activity_id=r.activity_id and ca.user_id=r.user_id
                      and ca.original_status=coalesce(c.status, 'ABSENT')
                  )
                """);
    }

    public List<Map<String, Object>> my(Long userId) {
        return jdbcTemplate.queryForList("""
                select ca.*,a.name as activityName,a.start_time as startTime,a.end_time as endTime
                from checkin_adjustment ca
                join activity a on a.id=ca.activity_id
                where ca.user_id=?
                order by ca.created_at desc
                """, userId);
    }

    public List<Map<String, Object>> adminList(String auditStatus, Long activityId, String keyword) {
        String k = keyword == null || keyword.isBlank() ? null : keyword;
        return jdbcTemplate.queryForList("""
                select ca.*,a.name as activityName,a.start_time as startTime,a.end_time as endTime,
                       u.name as userName,u.nickname,u.identity_no as identityNo,
                       p.college,p.major_class as majorClass,
                       c.method,c.distance_meters as distanceMeters,c.manual_reason as manualReason
                from checkin_adjustment ca
                join activity a on a.id=ca.activity_id
                join user u on u.id=ca.user_id
                left join volunteer_profile p on p.user_id=u.id
                left join activity_checkin c on c.activity_id=ca.activity_id and c.user_id=ca.user_id
                where (? is null or ca.audit_status=?)
                  and (? is null or ca.activity_id=?)
                  and (? is null or u.name like concat('%',?,'%') or u.nickname like concat('%',?,'%')
                       or u.identity_no like concat('%',?,'%') or a.name like concat('%',?,'%'))
                order by case ca.audit_status when 'PENDING' then 0 when 'SYSTEM' then 1 else 2 end, ca.created_at desc
                """, n(auditStatus), n(auditStatus), activityId, activityId, k, k, k, k, k);
    }

    public Map<String, Object> find(Long id) {
        return jdbcTemplate.queryForMap("""
                select ca.*,a.service_hours,a.end_time
                from checkin_adjustment ca join activity a on a.id=ca.activity_id
                where ca.id=?
                """, id);
    }

    public void audit(Long id, String auditStatus, String newStatus, LocalDateTime newCheckinTime,
                      Double newServiceHours, String hoursReason, String adminRemark, Long adminId) {
        jdbcTemplate.update("""
                update checkin_adjustment
                set audit_status=?,new_status=?,new_checkin_time=?,new_service_hours=?,hours_reason=?,
                    admin_remark=?,admin_id=?,updated_at=now()
                where id=?
                """, auditStatus, newStatus, newCheckinTime, newServiceHours, hoursReason, adminRemark, adminId, id);
    }

    public Map<String, Object> latestApproved(Long activityId, Long userId) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList("""
                select * from checkin_adjustment
                where activity_id=? and user_id=? and audit_status='APPROVED'
                order by updated_at desc,id desc
                limit 1
                """, activityId, userId);
        return list.stream().findFirst().orElse(null);
    }

    private String n(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
